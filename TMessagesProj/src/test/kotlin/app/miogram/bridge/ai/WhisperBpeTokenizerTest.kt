package app.miogram.bridge.ai

import app.miogram.core.ai.tokens.ByteLevelBpe
import app.miogram.core.ai.tokens.TokenizerException
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WhisperBpeTokenizerTest {

    /** Full byte-alphabet vocabulary plus Whisper special tokens. */
    private val rawVocab: String by lazy {
        val json = JSONObject()
        ByteLevelBpe.byteAlphabet().forEach { (byte, char) ->
            json.put(char.toString(), byte.toInt() and 0xFF)
        }
        json.put("<|startoftranscript|>", 50258)
        json.put("<|endoftext|>", 50256)
        json.put("<|transcribe|>", 50359)
        json.put("<|ru|>", 50274)
        json.put("<|en|>", 50259)
        json.toString()
    }

    private val tokenizer by lazy { WhisperBpeTokenizer.from(rawVocab, "#version: 1\n") }

    @Test
    fun `endOfText resolved from literal`() {
        assertEquals(50256, tokenizer.endOfTextTokenId)
    }

    @Test
    fun `prompt includes language hint only when known`() {
        assertArrayEquals(intArrayOf(50258, 50274, 50359), tokenizer.encodePrompt("ru"))
        assertArrayEquals(intArrayOf(50258, 50359), tokenizer.encodePrompt("klingon"))
        assertArrayEquals(intArrayOf(50258, 50359), tokenizer.encodePrompt(null))
    }

    @Test
    fun `decode strips special tokens and renders bytes`() {
        val hiIds = "hi".toByteArray(Charsets.UTF_8).map { byte ->
            ByteLevelBpe.byteAlphabet().getValue(byte).code
        }
        val text = tokenizer.decode(intArrayOf(50258, 50359) + hiIds + intArrayOf(50256))
        assertEquals("hi", text)
    }

    @Test
    fun `full encode decode roundtrip through real pipeline`() {
        val text = "Суммаризация треда 🚀"
        assertEquals(text, tokenizer.decode(tokenizer.encodeText(text)))
    }

    @Test
    fun `missing mandatory endoftext fails fast`() {
        assertThrows(TokenizerException::class.java) {
            WhisperBpeTokenizer.from("""{"a":0}""", "")
        }
    }
}
