package app.miogram.core.ai.tokens

/**
 * Byte-level BPE encoder/decoder, compatible with GPT-2 / Whisper vocabulary
 * files (`vocab.json`, `merges.txt`).
 *
 * Pipeline (encode): text -> UTF-8 bytes -> printable-unicode byte alphabet ->
 * regex word split -> greedy lowest-rank pair merging -> ids.
 * Pipeline (decode) is exact inverse; byte level makes it lossless for any
 * Unicode input including emoji and broken sequences.
 *
 * Pure data structure: no I/O, no android — loaders live elsewhere.
 */
class ByteLevelBpe(
    tokenToId: Map<String, Int>,
    mergesInOrder: List<Pair<String, String>>,
) {

    init {
        require(tokenToId.isNotEmpty()) { "empty vocabulary" }
    }

    private val idByToken: Map<String, Int> = tokenToId
    private val tokenById: Map<Int, String> = tokenToId.entries.associate { (k, v) -> v to k }
    private val mergeRank: Map<Pair<String, String>, Int> =
        mergesInOrder.withIndex().associate { (index, pair) -> pair to index }

    private val byteEncoder: Map<Byte, Char> = Companion.byteAlphabet()
    private val byteDecoder: Map<Char, Byte> = byteEncoder.entries.associate { (k, v) -> v to k }

    private val splitPattern = Regex(
        "'(?:[sdmt]|ll|ve|re)| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
    )

    // --- public API ----------------------------------------------------------

    /** Raw BPE encoding — the inverse of [decode]. */
    fun encode(text: String): IntArray {
        // Step 1: map the UTF-8 byte stream into the reversible alphabet —
        // this is what makes the scheme lossless for any Unicode input.
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val mapped = StringBuilder(rawBytes.size)
        for (b in rawBytes) {
            mapped.append(byteEncoder[b] ?: throw TokenizerException("unmapped byte ${b.toInt()}"))
        }

        // Step 2: word-split and greedy-merge over the mapped representation.
        val ids = ArrayList<Int>(rawBytes.size)
        for (word in splitPattern.findAll(mapped)) {
            for (token in bpe(word.value)) {
                val id = idByToken[token]
                    ?: throw TokenizerException("no vocab entry for '$token'")
                ids.add(id)
            }
        }
        return ids.toIntArray()
    }

    fun decode(tokenIds: IntArray): String {
        val sb = StringBuilder(tokenIds.size * 2)
        for (id in tokenIds) {
            val token = tokenById[id] ?: throw TokenizerException("unknown token id $id")
            sb.append(token)
        }
        val bytes = ByteArray(sb.length)
        for (i in sb.indices) {
            bytes[i] = byteDecoder[sb[i]]
                ?: throw TokenizerException("non-alphabet char at $i")
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun idFor(token: String): Int? = idByToken[token]

    // --- internals -----------------------------------------------------------

    /**
     * Greedy lowest-rank adjacent-pair merging until no known pair remains.
     */
    private fun bpe(word: String): List<String> {
        var symbols = word.map(Char::toString)
        while (symbols.size > 1) {
            var bestRank = Int.MAX_VALUE
            var bestIndex = -1
            for (i in 0 until symbols.size - 1) {
                val rank = mergeRank[symbols[i] to symbols[i + 1]] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestIndex = i
                }
            }
            if (bestIndex < 0) break
            symbols = symbols.subList(0, bestIndex) +
                    listOf(symbols[bestIndex] + symbols[bestIndex + 1]) +
                    symbols.subList(bestIndex + 2, symbols.size)
        }
        return symbols
    }

    companion object {
        /**
         * The GPT-2 reversible byte alphabet: visible Latin-1 characters map
         * to themselves, everything else is shifted above U+0100 so every
         * byte has a distinct printable representation.
         *
         * Public because plugin/tooling authors need the exact mapping to
         * render token strings outside this class.
         */
        fun byteAlphabet(): Map<Byte, Char> {
            val printableRanges = listOf(0x21..0x7E, 0xA1..0xAC, 0xAE..0xFF)

            fun isPrintable(value: Int): Boolean =
                printableRanges.any { range -> value in range }

            val encoder = HashMap<Byte, Char>(256)
            var offset = 0
            for (b in 0 until 256) {
                encoder[b.toByte()] = if (isPrintable(b)) b.toChar() else (256 + offset++).toChar()
            }
            return encoder
        }
    }
}

class TokenizerException(message: String) : Exception(message)
