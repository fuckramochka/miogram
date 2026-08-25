package app.miogram.bridge.ai

import app.miogram.core.ai.SttBackend
import app.miogram.core.ai.audio.AudioFrontend
import java.io.File
import java.security.MessageDigest

/**
 * On-device speech-to-text engine (Whisper family via ONNX Runtime / NNAPI).
 *
 * Deliberately free of android.* imports: the model directory is injected,
 * which keeps the whole state machine unit-testable on the JVM. Production
 * wiring lives in [MiogramSttFactory].
 *
 * Lifecycle status machine:
 *   NOT_DOWNLOADED -> DOWNLOADING -> READY
 *                                -> FAILED(reason)
 *   READY -> BUSY -> READY | FAILED
 */
class LocalSttEngine(
    /** Directory holding downloaded model files; must exist or be creatable. */
    private val modelDir: File,
) {

    enum class Status { NOT_DOWNLOADED, DOWNLOADING, READY, BUSY, FAILED }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        /** SHA-256 of the downloaded archive; guards against corrupted pulls. */
        val sha256: String?,
    )

    @Volatile
    var status: Status = Status.NOT_DOWNLOADED
        private set

    /** Diagnostic for the last FAILED transition; safe to show in UI. */
    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var selectedModelId: String = ModelCatalog.DEFAULT.id
        private set

    fun models(): List<ModelInfo> = ModelCatalog.ALL

    fun selectModel(modelId: String) {
        require(ModelCatalog.ALL.any { it.id == modelId }) { "unknown model: $modelId" }
        selectedModelId = modelId
        if (status == Status.READY && !isDownloaded(selectedModelId)) {
            status = Status.NOT_DOWNLOADED
        }
    }

    fun modelFile(): File = File(modelDir, "$selectedModelId.onnx")

    fun isDownloaded(modelId: String = selectedModelId): Boolean =
        modelFile().takeIf { it.isFile }?.length() ?: 0L > MIN_VALID_FILE_BYTES

    /**
     * Marks a model file as present after external provisioning (download
     * manager, sideload). Verifies size; hash check runs lazily before first
     * inference to keep this call cheap.
     */
    fun registerDownloadedFile(expected: ModelInfo): Boolean {
        val file = modelFile()
        val ok = file.isFile &&
                file.length() in MIN_VALID_FILE_BYTES..expected.sizeBytes * 2
        status = if (ok) Status.READY else Status.FAILED.also { lastError = "invalid model file" }
        return ok
    }

    /**
     * Attaches the ONNX backend after model provisioning. Until then
     * transcribe() fails with [SttException.ErrorCode.BACKEND_NOT_READY].
     */
    @Volatile
    private var backend: SttBackend? = null

    fun attachBackend(backend: SttBackend) {
        this.backend = backend
        if (isDownloaded() && status != Status.BUSY) status = Status.READY
    }

    fun detachBackend() {
        backend = null
        if (status == Status.READY || status == Status.BUSY) status = Status.NOT_DOWNLOADED
    }

    /**
     * Transcribes 16-bit LE PCM mono audio on-device.
     *
     * @throws SttException with MODEL_NOT_DOWNLOADED / BACKEND_NOT_READY /
     *         BUSY depending on what is missing; AUDIO_INVALID for empty input.
     */
    fun transcribe(wavPcm16: ByteArray, sampleRate: Int = 16_000, onProgress: (Float) -> Unit = {}): String {
        if (!isDownloaded()) throw SttException(ErrorCode.MODEL_NOT_DOWNLOADED)
        val activeBackend = backend ?: throw SttException(ErrorCode.BACKEND_NOT_READY)
        if (status == Status.BUSY) throw SttException(ErrorCode.BUSY)
        if (wavPcm16.size < AudioFrontend.MIN_INPUT_BYTES) {
            throw SttException(ErrorCode.AUDIO_INVALID, "payload too short")
        }

        status = Status.BUSY
        try {
            return activeBackend.transcribe(wavPcm16, sampleRate, onProgress)
        } finally {
            status = if (isDownloaded()) Status.READY else Status.FAILED.also { lastError = "model disappeared" }
        }
    }

    class SttException(val code: ErrorCode, detail: String? = null) :
        Exception(code.name + (detail?.let { ": $it" } ?: ""))

    enum class ErrorCode { MODEL_NOT_DOWNLOADED, BACKEND_NOT_READY, AUDIO_INVALID, BUSY }

    private object ModelCatalog {
        const val DEFAULT_ID = "whisper-small-int8"
        val DEFAULT = ModelInfo(
            id = DEFAULT_ID,
            displayName = "Whisper Small (int8, ~250 МБ)",
            // Pinned release asset; mirrored into the app's own CDN later.
            downloadUrl = "https://github.com/miogram-ai/models/releases/whisper-small-int8.onnx",
            sizeBytes = 262_144_000L,
            sha256 = null,
        )
        val ALL = listOf(DEFAULT)
    }

    companion object {
        private const val MIN_VALID_FILE_BYTES = 1_000_000L

        fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                var read = input.read(buffer)
                while (read >= 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
