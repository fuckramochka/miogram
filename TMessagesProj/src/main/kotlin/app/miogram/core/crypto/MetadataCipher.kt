package app.miogram.core.crypto

import java.security.SecureRandom

/**
 * Seals/opens Miogram metadata at rest.
 *
 * The contract deliberately exposes operations, not raw key bytes: production
 * implementations are backed by non-exportable hardware keys (AndroidKeyStore),
 * where `getEncoded()` returns null by design. Tests supply an ephemeral
 * software implementation ([AesGcmMetadataCipher]).
 */
interface MetadataCipher {
    /** Returns `"MIE1"`-layout sealed box bound to [aad]. */
    fun seal(plaintext: ByteArray, aad: ByteArray): ByteArray

    /** Throws [AesGcm.SealedBoxException] on authentication failure. */
    fun open(sealedBox: ByteArray, aad: ByteArray): ByteArray
}

/** Software MetadataCipher over an ephemeral in-memory key. For tests only. */
class AesGcmMetadataCipher(private val key: ByteArray = KeyMaterial.random(SecureRandom(), AesGcm.KEY_LENGTH).bytes()) :
    MetadataCipher {
    override fun seal(plaintext: ByteArray, aad: ByteArray): ByteArray = AesGcm.encrypt(key, plaintext, aad)

    override fun open(sealedBox: ByteArray, aad: ByteArray): ByteArray = AesGcm.decrypt(key, sealedBox, aad)
}
