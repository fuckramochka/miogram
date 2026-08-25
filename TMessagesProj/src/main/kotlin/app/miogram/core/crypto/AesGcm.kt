package app.miogram.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM envelope encryption.
 *
 * Sealed layout: `"MIE1"` magic | 12-byte nonce | ciphertext+tag(128-bit).
 * AAD binds the envelope to its purpose so sealed blobs cannot be replayed
 * between contexts (e.g. master-secret slot reused as DB-passphrase slot).
 */
object AesGcm {

    private val MAGIC = byteArrayOf(0x4D, 0x49, 0x45, 0x31)
    const val NONCE_LENGTH = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    const val KEY_LENGTH = 32

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    class SealedBoxException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == KEY_LENGTH) { "key must be $KEY_LENGTH bytes, got ${key.size}" }
        return encryptWith(SecretKeySpec(key, "AES"), plaintext, aad)
    }

    fun decrypt(key: ByteArray, sealedBox: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == KEY_LENGTH) { "key must be $KEY_LENGTH bytes, got ${key.size}" }
        return decryptWith(SecretKeySpec(key, "AES"), sealedBox, aad)
    }

    /**
     * Hardware-key variant sharing the same sealed layout; used by
     * AndroidKeyStore-backed MetadataCipher where raw key bytes are unavailable.
     */
    fun encryptWith(key: javax.crypto.SecretKey, plaintext: ByteArray, aad: ByteArray): ByteArray {
        require(plaintext.isNotEmpty()) { "plaintext must not be empty" }

        val nonce = ByteArray(NONCE_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        val sealed = cipher.doFinal(plaintext)

        val out = ByteArray(MAGIC.size + NONCE_LENGTH + sealed.size)
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.size)
        System.arraycopy(nonce, 0, out, MAGIC.size, NONCE_LENGTH)
        System.arraycopy(sealed, 0, out, MAGIC.size + NONCE_LENGTH, sealed.size)
        return out
    }

    fun decryptWith(key: javax.crypto.SecretKey, sealedBox: ByteArray, aad: ByteArray): ByteArray {
        if (sealedBox.size <= MAGIC.size + NONCE_LENGTH) {
            throw SealedBoxException("sealed box too short: ${sealedBox.size}")
        }
        for (i in MAGIC.indices) {
            if (sealedBox[i] != MAGIC[i]) throw SealedBoxException("bad magic")
        }

        val nonce = sealedBox.copyOfRange(MAGIC.size, MAGIC.size + NONCE_LENGTH)
        val sealed = sealedBox.copyOfRange(MAGIC.size + NONCE_LENGTH, sealedBox.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad)
            cipher.doFinal(sealed)
        } catch (e: Exception) {
            Secrets.zeroize(sealed)
            throw SealedBoxException("authentication failed", e)
        }
    }
}
