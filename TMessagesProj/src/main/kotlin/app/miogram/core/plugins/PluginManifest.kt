package app.miogram.core.plugins

import java.security.MessageDigest

data class PluginManifest(
    val pluginId: String,
    val versionCode: Int,
    val displayName: String,
    val capabilities: Set<PluginCapability>,
    val codeSize: Long,
    val codeSha256: ByteArray,
    val signerKeyId: String,
)

class PluginFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Device-level registry of Ed25519 signers allowed to publish installable
 * plugins. Maps a stable key id to the raw public key so verification never
 * needs the private half.
 */
interface TrustAnchors {
    /** @return raw 32-byte Ed25519 public key, or null when untrusted. */
    fun publicKeyFor(keyId: String): ByteArray?

    companion object {
        /** keyId = hex of first 8 bytes of SHA-256(raw public key). */
        fun keyIdOf(publicKey: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(publicKey)
            return digest.take(8).joinToString("") { "%02x".format(it) }
        }
    }
}

class InMemoryTrustAnchors(vararg publicKeys: ByteArray) : TrustAnchors {
    private val byKeyId: Map<String, ByteArray> =
        publicKeys.associate { TrustAnchors.keyIdOf(it) to it.copyOf() }

    override fun publicKeyFor(keyId: String): ByteArray? = byKeyId[keyId]?.copyOf()
}
