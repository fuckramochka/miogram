package app.miogram.core.ai

/**
 * Pluggable transcription backend. Core/UI only ever sees this interface;
 * the ONNX implementation lives in the bridge layer and is attached once a
 * model file has been verified.
 */
interface SttBackend {
    /**
     * @param wavPcm16 signed 16-bit LE PCM, mono.
     * @param sampleRate source sample rate in Hz (backend resamples to 16k).
     * @return transcript text in the model's language prior.
     */
    fun transcribe(wavPcm16: ByteArray, sampleRate: Int, onProgress: (Float) -> Unit): String
}
