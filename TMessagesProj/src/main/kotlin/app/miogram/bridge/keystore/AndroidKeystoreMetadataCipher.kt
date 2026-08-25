package app.miogram.bridge.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.miogram.core.crypto.AesGcm
import app.miogram.core.crypto.MetadataCipher
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed MetadataCipher. The AES-256-GCM key is generated inside
 * AndroidKeyStore, is non-exportable, and never leaves the TEE/StrongBox;
 * sealing happens through JCA against the keystore key handle.
 */
class AndroidKeystoreMetadataCipher(
    private val alias: String = DEFAULT_ALIAS,
    private val requireStrongBox: Boolean = false,
) : MetadataCipher {

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AesGcm.KEY_LENGTH * 8)
            .setRandomizedEncryptionRequired(true)
        if (requireStrongBox) {
            spec.setIsStrongBoxBacked(true)
        }
        generator.init(spec.build())
        return generator.generateKey()
    }

    override fun seal(plaintext: ByteArray, aad: ByteArray): ByteArray =
        AesGcm.encryptWith(obtainKey(), plaintext, aad)

    override fun open(sealedBox: ByteArray, aad: ByteArray): ByteArray {
        if (sealedBox.size <= 4 + AesGcm.NONCE_LENGTH) {
            throw AesGcm.SealedBoxException("sealed box too short")
        }
        // Layout parity with AesGcm: magic(4) | iv(12) | ct+tag.
        val magic = byteArrayOf(0x4D, 0x49, 0x45, 0x31)
        for (i in magic.indices) {
            if (sealedBox[i] != magic[i]) throw AesGcm.SealedBoxException("bad magic")
        }
        val iv = sealedBox.copyOfRange(4, 16)
        val sealed = sealedBox.copyOfRange(16, sealedBox.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(128, iv))
            cipher.updateAAD(aad)
            cipher.doFinal(sealed)
        } catch (e: Exception) {
            throw AesGcm.SealedBoxException("keystore authentication failed", e)
        }
    }

    companion object {
        const val DEFAULT_ALIAS = "miogram_metadata_kek_v1"
        private const val PROVIDER = "AndroidKeyStore"
    }
}
