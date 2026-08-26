package app.miogram.bridge.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.miogram.core.crypto.AesGcm
import app.miogram.core.crypto.MetadataCipher
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed MetadataCipher with seamless Software fallback.
 * Uses AndroidKeyStore when permitted, and transparently falls back to
 * an isolated AES-256 local key if hardware keystore access is restricted by OEM/Knox.
 */
class AndroidKeystoreMetadataCipher(
    private val alias: String = DEFAULT_ALIAS,
    private val requireStrongBox: Boolean = false,
) : MetadataCipher {

    private fun obtainKey(): SecretKey {
        return try {
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
            generator.generateKey()
        } catch (e: Throwable) {
            obtainSoftwareFallbackKey()
        }
    }

    private fun obtainSoftwareFallbackKey(): SecretKey {
        val ctx = org.telegram.messenger.ApplicationLoader.applicationContext
        val filesDir = ctx?.filesDir ?: File("/data/data/com.exteraless.app/files")
        if (!filesDir.exists()) filesDir.mkdirs()
        val seedFile = File(filesDir, "miogram_vault_seed.bin")
        val raw = if (seedFile.exists() && seedFile.length() == 32L) {
            seedFile.readBytes()
        } else {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            seedFile.writeBytes(bytes)
            bytes
        }
        return SecretKeySpec(raw, "AES")
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
