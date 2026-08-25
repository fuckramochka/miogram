package app.miogram.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class AesGcmTest {

    private val random = SecureRandom()
    private val key = KeyMaterial.random(random, AesGcm.KEY_LENGTH).bytes()

    @Test
    fun `roundtrip preserves plaintext`() {
        val plaintext = "attack at dawn".toByteArray()
        val aad = "context-a".toByteArray()
        val sealed = AesGcm.encrypt(key, plaintext, aad)
        assertArrayEquals(plaintext, AesGcm.decrypt(key, sealed, aad))
    }

    @Test(expected = AesGcm.SealedBoxException::class)
    fun `tampered ciphertext fails authentication`() {
        val sealed = AesGcm.encrypt(key, "payload".toByteArray(), "ctx".toByteArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 0x01).toByte()
        AesGcm.decrypt(key, sealed, "ctx".toByteArray())
    }

    @Test(expected = AesGcm.SealedBoxException::class)
    fun `wrong aad fails authentication`() {
        val sealed = AesGcm.encrypt(key, "payload".toByteArray(), "ctx-a".toByteArray())
        AesGcm.decrypt(key, sealed, "ctx-b".toByteArray())
    }

    @Test(expected = AesGcm.SealedBoxException::class)
    fun `wrong key fails authentication`() {
        val otherKey = KeyMaterial.random(random, AesGcm.KEY_LENGTH).bytes()
        val sealed = AesGcm.encrypt(key, "payload".toByteArray(), "ctx".toByteArray())
        AesGcm.decrypt(otherKey, sealed, "ctx".toByteArray())
    }

    @Test
    fun `nonce reuse impossible across calls`() {
        val s1 = AesGcm.encrypt(key, "same".toByteArray(), ByteArray(0))
        val s2 = AesGcm.encrypt(key, "same".toByteArray(), ByteArray(0))
        assertFalse(s1.contentEquals(s2))
    }

    @Test
    fun `short sealed box rejected cleanly`() {
        try {
            AesGcm.decrypt(key, byteArrayOf(0x48, 0x59), ByteArray(0))
            throw AssertionError("expected SealedBoxException")
        } catch (e: AesGcm.SealedBoxException) {
            assertTrue(e.message!!.contains("too short"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid key size rejected`() {
        AesGcm.encrypt(ByteArray(16), "data".toByteArray(), ByteArray(0))
    }
}

class SecretsTest {

    @Test
    fun `constant time equals matches content only`() {
        assertTrue(Secrets.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(Secrets.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(Secrets.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2)))
    }

    @Test
    fun `zeroize accepts nulls`() {
        Secrets.zeroize(null as ByteArray?)
        Secrets.zeroize(byteArrayOf(9), null as ByteArray?)
    }
}

class KeyMaterialTest {

    @Test(expected = IllegalStateException::class)
    fun `access after close forbidden`() {
        val km = KeyMaterial.of(byteArrayOf(1, 2, 3))
        km.close()
        km.bytes()
    }

    @Test
    fun `close is idempotent`() {
        val km = KeyMaterial.of(byteArrayOf(5, 5))
        km.close()
        km.close()
    }

    @Test
    fun `withRaw sees live buffer`() {
        val data = byteArrayOf(7, 8, 9)
        val km = KeyMaterial.of(data)
        km.withRaw { assertArrayEquals(data, it) }
        km.close()
    }
}
