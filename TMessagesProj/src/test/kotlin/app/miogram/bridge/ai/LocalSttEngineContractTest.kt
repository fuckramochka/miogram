package app.miogram.bridge.ai

import app.miogram.core.ai.SttBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalSttEngineContractTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var engine: LocalSttEngine
    private lateinit var backend: RecordingBackend

    private class RecordingBackend : SttBackend {
        var calls = 0
        var lastRate = -1
        /** Captured engine status at the moment of the call. */
        var observedStatus: LocalSttEngine.Status? = null
        var transcript = "привет мир"
        var engineRef: LocalSttEngine? = null

        override fun transcribe(wavPcm16: ByteArray, sampleRate: Int, onProgress: (Float) -> Unit): String {
            calls++
            lastRate = sampleRate
            observedStatus = engineRef?.status
            return transcript
        }
    }

    @Before
    fun setUp() {
        engine = LocalSttEngine(tmp.root)
        backend = RecordingBackend().apply { engineRef = engine }
    }

    private fun provisionModelFile() {
        val file = File(tmp.root, "${engine.selectedModelId}.onnx")
        file.writeBytes(ByteArray(1_100_000) { (it % 251).toByte() })
    }

    @Test
    fun `fresh engine reports not downloaded and refuses transcription`() {
        assertEquals(LocalSttEngine.Status.NOT_DOWNLOADED, engine.status)
        val ex = assertThrows(LocalSttEngine.SttException::class.java) {
            engine.transcribe(ByteArray(128))
        }
        assertEquals(LocalSttEngine.ErrorCode.MODEL_NOT_DOWNLOADED, ex.code)
    }

    @Test
    fun `model present without backend reports BACKEND_NOT_READY`() {
        provisionModelFile()
        assertTrue(engine.isDownloaded())
        val ex = assertThrows(LocalSttEngine.SttException::class.java) {
            engine.transcribe(ByteArray(128))
        }
        assertEquals(LocalSttEngine.ErrorCode.BACKEND_NOT_READY, ex.code)
    }

    @Test
    fun `happy path returns transcript and returns to READY`() {
        provisionModelFile()
        engine.attachBackend(backend)
        assertEquals(LocalSttEngine.Status.READY, engine.status)

        val text = engine.transcribe(ByteArray(512) { 3 }, 16_000)

        assertEquals("привет мир", text)
        assertEquals(1, backend.calls)
        assertEquals(16_000, backend.lastRate)
        // The backend observed the BUSY guard during its own invocation.
        assertEquals(LocalSttEngine.Status.BUSY, backend.observedStatus)
        assertEquals(LocalSttEngine.Status.READY, engine.status)
    }

    @Test
    fun `short audio rejected before backend is touched`() {
        provisionModelFile()
        engine.attachBackend(backend)

        val ex = assertThrows(LocalSttEngine.SttException::class.java) {
            engine.transcribe(byteArrayOf(0), 16_000)
        }
        assertEquals(LocalSttEngine.ErrorCode.AUDIO_INVALID, ex.code)
        assertEquals(0, backend.calls)
    }

    @Test
    fun `detach after attach drops back to NOT_DOWNLOADED when no model`() {
        engine.attachBackend(RecordingBackend())
        engine.detachBackend()
        assertEquals(LocalSttEngine.Status.NOT_DOWNLOADED, engine.status)
    }

    @Test
    fun `unknown model selection rejected`() {
        assertThrows(IllegalArgumentException::class.java) { engine.selectModel("nope") }
    }
}
