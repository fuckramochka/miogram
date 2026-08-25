package app.miogram.core.plugins

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Ed25519 signing/verification for plugin distributions.
 *
 * A distribution = (wasm code bytes, signed manifest bytes). Verification
 * binds the exact code bytes into the manifest through SHA-256, so a valid
 * signature is meaningless without byte-identical code.
 */
object PluginSignatures {

    class KeyPair(val privateKey: ByteArray, val publicKey: ByteArray) {
        val keyId: String by lazy { TrustAnchors.keyIdOf(publicKey) }
    }

    fun generateKeyPair(random: SecureRandom = SecureRandom()): KeyPair {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(random))
        val pair = generator.generateKeyPair()
        return KeyPair(
            (pair.private as Ed25519PrivateKeyParameters).encoded,
            (pair.public as Ed25519PublicKeyParameters).encoded,
        )
    }

    /** Signs the canonical unsigned payload produced by [PluginManifestCodec.encodeUnsigned]. */
    fun sign(unsignedManifestPayload: ByteArray, privateKey: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKey, 0))
        signer.update(unsignedManifestPayload, 0, unsignedManifestPayload.size)
        return signer.generateSignature()
    }

    sealed class Verdict {
        data class Valid(val manifest: PluginManifest) : Verdict()

        data class Rejected(val reason: Reason, val detail: String? = null) : Verdict()

        enum class Reason { MALFORMED, UNTRUSTED_SIGNER, BAD_SIGNATURE, CODE_SIZE_MISMATCH, CODE_HASH_MISMATCH }
    }

    /**
     * Full pipeline: parse → trust anchor → Ed25519 → code size → code hash.
     * Every outcome is audited; rejections never leak more than the reason.
     */
    fun verifyAndAudit(manifestBytes: ByteArray, codeBytes: ByteArray, anchors: TrustAnchors, auditSink: PluginAuditSink?): Verdict {
        val decoded = try {
            PluginManifestCodec.decode(manifestBytes)
        } catch (e: PluginFormatException) {
            return reject(null, Verdict.Reason.MALFORMED, e.message, auditSink)
        }
        val manifest = decoded.manifest

        val publicKey = anchors.publicKeyFor(manifest.signerKeyId)
        if (publicKey == null) {
            return reject(manifest, Verdict.Reason.UNTRUSTED_SIGNER, "keyId=${manifest.signerKeyId}", auditSink)
        }

        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        verifier.update(decoded.signedBytes, 0, decoded.signedBytes.size)
        if (!verifier.verifySignature(decoded.signature)) {
            return reject(manifest, Verdict.Reason.BAD_SIGNATURE, null, auditSink)
        }

        if (codeBytes.size.toLong() != manifest.codeSize) {
            return reject(manifest, Verdict.Reason.CODE_SIZE_MISMATCH, "expected=${manifest.codeSize} actual=${codeBytes.size}", auditSink)
        }

        val actualHash = MessageDigest.getInstance("SHA-256").digest(codeBytes)
        if (!MessageDigest.isEqual(actualHash, manifest.codeSha256)) {
            return reject(manifest, Verdict.Reason.CODE_HASH_MISMATCH, null, auditSink)
        }

        auditSink?.onEvent(PluginAuditEvent(manifest.pluginId, PluginAuditEvent.Kind.MANIFEST_VERIFIED, manifest.signerKeyId))
        return Verdict.Valid(manifest)
    }

    private fun reject(
        manifest: PluginManifest?,
        reason: Verdict.Reason,
        detail: String?,
        auditSink: PluginAuditSink?,
    ): Verdict.Rejected {
        auditSink?.onEvent(PluginAuditEvent(manifest?.pluginId ?: "?", PluginAuditEvent.Kind.MANIFEST_REJECTED, "$reason${detail?.let { ": $it" } ?: ""}"))
        return Verdict.Rejected(reason, detail)
    }
}
