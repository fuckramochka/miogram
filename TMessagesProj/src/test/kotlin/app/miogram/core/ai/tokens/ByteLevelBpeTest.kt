package app.miogram.core.ai.tokens

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ByteLevelBpeTest {

    /**
     * Tiny hand-crafted vocabulary:
     *  ids 0..4 — single letters; 5+ — learned merges in rank order.
     */
    private val vocab = mapOf(
        "h" to 0, "e" to 1, "l" to 2, "o" to 3, "!" to 4,
        "he" to 5,    // merge #0: h e
        "ll" to 6,    // merge #1: l l
        "hell" to 8,  // merge #2: he ll
        "hello" to 7, // merge #3: hell o
    )

    private val merges = listOf("h" to "e", "l" to "l", "he" to "ll", "hell" to "o")

    @Test
    fun `greedy merging follows ranks`() {
        val bpe = ByteLevelBpe(vocab, merges)
        assertArrayEquals(intArrayOf(7), bpe.encode("hello"))
    }

    @Test
    fun `partial vocabulary leaves unmergeable singles`() {
        val withoutFinal = ByteLevelBpe(vocab, merges.take(2))
        assertArrayEquals(intArrayOf(5, 6, 3), withoutFinal.encode("hello"))
    }

    @Test
    fun `decode is exact inverse for ascii`() {
        val bpe = ByteLevelBpe(vocab, merges)
        assertEquals("hello", bpe.decode(bpe.encode("hello")))
        assertEquals("hello!", bpe.decode(bpe.encode("hello!")))
        assertEquals("hell", bpe.decode(bpe.encode("hell")))
    }

    @Test
    fun `byte level alphabet survives cyrillic and emoji`() {
        // Full 256-token alphabet with ids equal to unsigned byte value: the
        // construction real byte-level models ship with.
        val alphabet = ByteLevelBpe.byteAlphabet()
            .entries.associate { (byte, char) -> char.toString() to (byte.toInt() and 0xFF) }
        val bpe = ByteLevelBpe(alphabet, emptyList())

        for (sample in listOf("привет мир", "emoji 🚀🔥", "mixed Привет123 !?", "quote \" backslash \\")) {
            assertEquals(sample, bpe.decode(bpe.encode(sample)))
        }
    }

    @Test(expected = TokenizerException::class)
    fun `unknown id on decode throws`() {
        ByteLevelBpe(vocab, merges).decode(intArrayOf(999))
    }

    @Test
    fun `idFor returns null for absent tokens`() {
        val bpe = ByteLevelBpe(vocab, merges)
        assertNull(bpe.idFor("<|never|>"))
        assertEquals(5, bpe.idFor("he"))
    }
}
