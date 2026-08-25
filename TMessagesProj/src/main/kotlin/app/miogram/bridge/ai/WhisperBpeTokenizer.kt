package app.miogram.bridge.ai

import app.miogram.core.ai.tokens.ByteLevelBpe
import app.miogram.core.ai.tokens.TextTokenizer
import app.miogram.core.ai.tokens.TokenizerException
import org.json.JSONObject
import java.io.File

/**
 * TextTokenizer backed by Whisper's GPT-2 style vocabulary.
 *
 * Files (standard distribution layout):
 *  * `vocab.json` — { "token string": id, ... }
 *  * `merges.txt` — first line is a version comment, then "left right" per rank
 *
 * Special tokens are looked up by their literal strings; a missing entry
 * simply drops that prompt position so partial vocabularies keep working in
 * tests and tooling.
 */
class WhisperBpeTokenizer private constructor(
    private val bpe: ByteLevelBpe,
    private val startOfTranscriptId: Int?,
    private val transcribeTaskId: Int?,
    /** ISO-ish language name -> token id ("<|ru|>" style). */
    private val languageTokenIds: Map<String, Int>,
    override val endOfTextTokenId: Int,
) : TextTokenizer {

    override fun encodePrompt(languageHint: String?): IntArray {
        val ids = ArrayList<Int>(3)
        startOfTranscriptId?.let(ids::add)
        if (languageHint != null) {
            languageTokenIds[languageHint.lowercase()]?.let(ids::add)
        }
        transcribeTaskId?.let(ids::add)
        return ids.toIntArray()
    }

    /** Symmetric counterpart of [decode] for tests and tooling. */
    fun encodeText(text: String): IntArray = bpe.encode(text)

    override fun decode(tokenIds: IntArray): String = bpe.decode(stripSpecial(tokenIds))

    /** Special tokens carry no text; drop them before BPE decode. */
    private fun stripSpecial(tokenIds: IntArray): IntArray =
        tokenIds.filterNot { it == endOfTextTokenId || it == startOfTranscriptId || it == transcribeTaskId }
            .toIntArray()

    companion object {

        const val SPECIAL_START_OF_TRANSCRIPT = "<|startoftranscript|>"
        const val SPECIAL_END_OF_TEXT = "<|endoftext|>"
        const val SPECIAL_TASK_TRANSCRIBE = "<|transcribe|>"

        fun load(vocabFile: File, mergesFile: File): WhisperBpeTokenizer =
            from(
                parseVocab(vocabFile.readText(Charsets.UTF_8)),
                parseMerges(mergesFile.readText(Charsets.UTF_8)),
            )

        internal fun from(rawVocab: String, rawMerges: String): WhisperBpeTokenizer {
            val vocabJson = JSONObject(rawVocab)
            val tokenToId = HashMap<String, Int>(vocabJson.length())
            for (key in vocabJson.keys()) {
                tokenToId[key] = vocabJson.getInt(key)
            }

            val merges = rawMerges.lineSequence()
                .dropWhile { it.startsWith("#") || it.isBlank() }
                .mapNotNull { line ->
                    val parts = line.split(" ")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toList()

            val bpe = ByteLevelBpe(tokenToId, merges)

            fun requiredId(token: String): Int =
                bpe.idFor(token) ?: throw TokenizerException("vocabulary lacks $token")

            return WhisperBpeTokenizer(
                bpe = bpe,
                startOfTranscriptId = bpe.idFor(SPECIAL_START_OF_TRANSCRIPT),
                transcribeTaskId = bpe.idFor(SPECIAL_TASK_TRANSCRIBE),
                languageTokenIds = tokenToId.keys
                    .filter { it.startsWith("<|") && it.endsWith("|>") && it != SPECIAL_END_OF_TEXT &&
                            it != SPECIAL_START_OF_TRANSCRIPT && it != SPECIAL_TASK_TRANSCRIBE }
                    .mapNotNull { token ->
                        val lang = token.removeSurrounding("<|", "|>")
                        if (lang.all { it.isLetter() }) lang.lowercase() to tokenToId.getValue(token) else null
                    }
                    .toMap(),
                endOfTextTokenId = requiredId(SPECIAL_END_OF_TEXT),
            )
        }

        private fun parseVocab(text: String): String = text

        private fun parseMerges(text: String): String = text
    }
}
