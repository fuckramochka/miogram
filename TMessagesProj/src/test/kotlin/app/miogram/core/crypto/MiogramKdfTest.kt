package app.miogram.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiogramKdfTest {

    private val kdf = MiogramKdf()
    private val params = MiogramKdf.Params.TEST_FAST

    private fun pin(s: String): CharArray = s.toCharArray()

    @Test
    fun `derivation is deterministic for identical salt`() {
        val salt = kdf.newSalt()
        kdf.deriveChar(pin("correct horse"), salt, params).useWrappingKey { tag1, key1 ->
            kdf.deriveChar(pin("correct horse"), salt, params).useWrappingKey { tag2, key2 ->
                assertArrayEquals(tag1, tag2)
                assertArrayEquals(key1, key2)
            }
        }
    }

    @Test
    fun `different salt yields different material`() {
        val t1 = kdf.deriveChar(pin("hunter2"), kdf.newSalt(), params)
        val t2 = kdf.deriveChar(pin("hunter2"), kdf.newSalt(), params)
        assertFalse(t1.checkTag.contentEquals(t2.checkTag))
        t1.close()
        t2.close()
    }

    @Test
    fun `different passphrase yields different material`() {
        val salt = kdf.newSalt()
        kdf.deriveChar(pin("one"), salt, params).useWrappingKey { a, _ ->
            kdf.deriveChar(pin("two"), salt, params).useWrappingKey { b, _ ->
                assertFalse(a.contentEquals(b))
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty passphrase rejected`() {
        kdf.deriveChar(CharArray(0), kdf.newSalt(), params)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized memory parameter rejected`() {
        MiogramKdf.Params(MiogramKdf.Params.MAX_MEMORY_KIB + 1, 1, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `output length below envelope size rejected`() {
        kdf.derive("x".toByteArray(), kdf.newSalt(), params, outputLength = 32)
    }

    @Test
    fun `close zeroes wrapping key but keeps check tag readable`() {
        val result = kdf.deriveChar(pin("keep-tag"), kdf.newSalt(), params)
        val tagCopy = result.checkTag.copyOf()
        result.close()
        assertArrayEquals(tagCopy, result.checkTag)
    }
}
