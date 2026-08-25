package app.miogram.core.ai.tokens

/**
 * Minimal token contract consumed by the transcription backend.
 * Implementations live in the bridge layer (BPE over model vocabulary files).
 */
interface TextTokenizer {
    /** Decoder prompt tokens preceding generation (start-of-transcript, language, task). */
    fun encodePrompt(languageHint: String?): IntArray

    /** Renders generated token ids as human text. */
    fun decode(tokenIds: IntArray): String

    /** Sentinel that ends autoregressive generation. */
    val endOfTextTokenId: Int
}
