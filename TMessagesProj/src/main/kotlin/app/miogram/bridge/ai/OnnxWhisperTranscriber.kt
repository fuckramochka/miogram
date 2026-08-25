package app.miogram.bridge.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import app.miogram.core.ai.SttBackend
import app.miogram.core.ai.audio.AudioFrontend
import app.miogram.core.ai.tokens.TextTokenizer
import java.io.File
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * ONNX Runtime implementation of [SttBackend] for Whisper-family exports that
 * embed the mel frontend (encoder consumes raw 16 kHz float32 PCM).
 *
 * Decode strategy v1: greedy autoregressive loop over the decoder with the
 * `<|startoftranscript|>` prompt, early stop on `<|endoftext|>`. The tokenizer
 * is injected — Whisper BPE vocabularies ship separately from the graphs.
 *
 * Resource discipline: every tensor and result is closed in use-blocks; the
 * session is owned by [sessionFactory]'s caller contract and closed here after
 * each transcription (sessions are cheap relative to model load on modern
 * ORT; revisit caching when streaming lands). Not thread-safe — LocalSttEngine
 * serializes through its BUSY state.
 */
class OnnxWhisperTranscriber(
    private val tokenizer: TextTokenizer,
    private val sessionFactory: () -> OrtSession,
) : SttBackend {

    /** Opens the native runtime lazily; NNAPI EP when present, CPU otherwise. */
    companion object {
        private const val MEL_INPUT = "mel"
        private const val TOKENS_INPUT = "tokens"
        private const val ENCODER_OUTPUT = "encoded"
        private const val LOGITS_OUTPUT = "logits"
        private const val MAX_DECODE_STEPS = 224

        @Volatile
        private var environment: OrtEnvironment? = null

        fun environment(): OrtEnvironment =
            environment ?: synchronized(this) {
                environment ?: OrtEnvironment.getEnvironment().also { environment = it }
            }

        /**
         * Standard factory for a single-file export containing both encoder
         * and decoder with KV-cache handling inside the graph.
         */
        fun sessionFactoryFor(modelFile: File): () -> OrtSession = {
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                try {
                    addNnapi()
                } catch (_: Throwable) {
                    // NNAPI unavailable on this device; CPU EP already active.
                }
            }
            environment().createSession(modelFile.absolutePath, options)
        }
    }

    override fun transcribe(wavPcm16: ByteArray, sampleRate: Int, onProgress: (Float) -> Unit): String {
        var samples = AudioFrontend.pcm16ToFloat(wavPcm16)
        samples = AudioFrontend.ensureSampleRate(samples, sampleRate)
        samples = AudioFrontend.normalizePeak(samples)
        val window = AudioFrontend.prepareModelInput(samples)

        val session = sessionFactory()
        try {
            createTensorF(window, longArrayOf(1, window.size.toLong())).use { mel ->
                // Materialize the encoder output as a JVM copy before the
                // result (and its native memory) is released.
                val encoded: Array<Array<FloatArray>> = session.run(mapOf(MEL_INPUT to mel)).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    result.get(0).value as Array<Array<FloatArray>>
                }
                return decodeLoop(session, encoded, onProgress)
            }
        } finally {
            session.close()
        }
    }

    private fun decodeLoop(
        session: OrtSession,
        encoded: Array<Array<FloatArray>>,
        onProgress: (Float) -> Unit,
    ): String {
        val collected = ArrayList<Int>()
        val prompt = tokenizer.encodePrompt(null)

        for (step in 0 until MAX_DECODE_STEPS) {
            onProgress(step / MAX_DECODE_STEPS.toFloat())

            val tokensSoFar: List<Int> = prompt.toList() + collected
            val idsShape = longArrayOf(1, tokensSoFar.size.toLong())
            createTensorI(tokensSoFar.toIntArray(), idsShape).use { ids ->
                createEncoderTensor(encoded).use { encoderTensor ->
                    session.run(
                        mapOf(
                            TOKENS_INPUT to ids,
                            ENCODER_OUTPUT to encoderTensor,
                        )
                    ).use { result ->
                        val logitsOut = result.get(LOGITS_OUTPUT)
                            .orElseThrow { IllegalStateException("decoder produced no $LOGITS_OUTPUT") }
                        @Suppress("UNCHECKED_CAST")
                        val logits = (logitsOut.value as Array<Array<FloatArray>>)[0]
                        val nextToken = argmax(logits[logits.size - 1])
                        if (nextToken == tokenizer.endOfTextTokenId) return tokenizer.decode(collected.toIntArray()).trim()
                        collected.add(nextToken)
                    }
                }
            }
        }
        return tokenizer.decode(collected.toIntArray()).trim()
    }

    private fun createTensorF(data: FloatArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(environment(), FloatBuffer.wrap(data), shape)

    private fun createTensorI(data: IntArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(environment(), IntBuffer.wrap(data), shape)

    private fun createEncoderTensor(encoded: Array<Array<FloatArray>>): OnnxTensor {
        val seqLen = encoded.size
        val headsOrDim1 = encoded[0].size
        val dim = encoded[0][0].size
        val flat = FloatArray(seqLen * headsOrDim1 * dim)
        var o = 0
        for (seq in encoded) for (row in seq) for (v in row) flat[o++] = v
        return OnnxTensor.createTensor(
            environment(),
            FloatBuffer.wrap(flat),
            longArrayOf(1, seqLen.toLong(), headsOrDim1.toLong(), dim.toLong())
        )
    }

    private fun argmax(row: FloatArray): Int {
        var best = 0
        for (i in 1 until row.size) if (row[i] > row[best]) best = i
        return best
    }
}
