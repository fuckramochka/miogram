package app.miogram.core.plugins

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

class CapabilitySetTest {

    @Test
    fun `membership and union`() {
        var caps = CapabilitySet.of(PluginCapability.READ_MESSAGE_EVENTS, PluginCapability.NETWORK)
        assertTrue(PluginCapability.READ_MESSAGE_EVENTS in caps)
        assertTrue(PluginCapability.NETWORK in caps)
        assertFalse(PluginCapability.SEND_MESSAGES in caps)

        caps += PluginCapability.SEND_MESSAGES
        assertTrue(PluginCapability.SEND_MESSAGES in caps)
    }

    @Test
    fun `empty set grants nothing`() {
        val gate = CapabilityGate(null)
        for (cap in PluginCapability.entries) {
            assertFalse(gate.require("p", CapabilitySet(), cap))
        }
    }

    @Test
    fun `denials are audited with plugin id`() {
        val events = ArrayList<PluginAuditEvent>()
        val sink = PluginAuditSink { events.add(it) }
        val gate = CapabilityGate(sink)

        assertFalse(gate.require("my-plugin", CapabilitySet(), PluginCapability.NETWORK))
        assertEquals(1, events.size)
        assertEquals("my-plugin", events[0].pluginId)
        assertEquals(PluginAuditEvent.Kind.CAPABILITY_DENIED, events[0].kind)
        assertEquals(PluginCapability.NETWORK.name, events[0].detail)
    }
}

class PluginManifestCodecTest {

    private fun sampleManifest(): PluginManifest {
        val code = "wasm-code-placeholder".toByteArray()
        return PluginManifest(
            pluginId = "com.example.plugin",
            versionCode = 7,
            displayName = "Example Plugin",
            capabilities = setOf(PluginCapability.READ_MESSAGE_EVENTS, PluginCapability.UI_DECORATOR),
            codeSize = code.size.toLong(),
            codeSha256 = sha256(code),
            signerKeyId = "0123456789abcdef",
        )
    }

    @Test
    fun `encode decode roundtrip preserves fields`() {
        val unsigned = PluginManifestCodec.encodeUnsigned(sampleManifest())
        val signed = PluginManifestCodec.attachSignature(unsigned, ByteArray(64) { 0x5A })
        val decoded = PluginManifestCodec.decode(signed)

        assertArrayEquals(unsigned, decoded.signedBytes)
        assertEquals(64, decoded.signature.size)
        val m = decoded.manifest
        assertEquals("com.example.plugin", m.pluginId)
        assertEquals(7, m.versionCode)
        assertEquals("Example Plugin", m.displayName)
        assertEquals(setOf(PluginCapability.READ_MESSAGE_EVENTS, PluginCapability.UI_DECORATOR), m.capabilities)
    }

    @Test(expected = PluginFormatException::class)
    fun `truncated signature rejected`() {
        val signed = PluginManifestCodec.attachSignature(
            PluginManifestCodec.encodeUnsigned(sampleManifest()),
            ByteArray(64)
        )
        PluginManifestCodec.decode(signed.copyOf(signed.size - 10))
    }

    @Test(expected = PluginFormatException::class)
    fun `bad magic rejected`() {
        val bytes = PluginManifestCodec.attachSignature(
            PluginManifestCodec.encodeUnsigned(sampleManifest()),
            ByteArray(64)
        )
        bytes[3] = 'X'.code.toByte()
        PluginManifestCodec.decode(bytes)
    }

    @Test
    fun `oversized code size rejected on decode and encode`() {
        val bad = sampleManifest().copy(codeSize = 65L * 1024 * 1024)
        org.junit.Assert.assertThrows(PluginFormatException::class.java) {
            PluginManifestCodec.encodeUnsigned(bad)
        }
    }
}

class PluginSignaturesTest {

    private val random = SecureRandom()

    private fun distribution(keyPair: PluginSignatures.KeyPair): Pair<ByteArray, ByteArray> {
        val code = "deflated wasm module bytes ${random.nextInt()}".toByteArray()
        val manifest = PluginManifest(
            pluginId = "test.plugin",
            versionCode = 1,
            displayName = "Test",
            capabilities = setOf(PluginCapability.PRIVATE_STORAGE),
            codeSize = code.size.toLong(),
            codeSha256 = sha256(code),
            signerKeyId = keyPair.keyId,
        )
        val unsigned = PluginManifestCodec.encodeUnsigned(manifest)
        val signed = PluginManifestCodec.attachSignature(unsigned, PluginSignatures.sign(unsigned, keyPair.privateKey))
        return signed to code
    }

    @Test
    fun `valid distribution verifies`() {
        val keys = PluginSignatures.generateKeyPair(random)
        val anchors = InMemoryTrustAnchors(keys.publicKey)
        val (manifestBytes, codeBytes) = distribution(keys)

        val verdict = PluginSignatures.verifyAndAudit(manifestBytes, codeBytes, anchors, null)
        assertTrue(verdict is PluginSignatures.Verdict.Valid)
        assertEquals("test.plugin", (verdict as PluginSignatures.Verdict.Valid).manifest.pluginId)
    }

    @Test
    fun `tampered code rejected by hash`() {
        val keys = PluginSignatures.generateKeyPair(random)
        val anchors = InMemoryTrustAnchors(keys.publicKey)
        val (manifestBytes, codeBytes) = distribution(keys)
        codeBytes[0] = (codeBytes[0].toInt() xor 1).toByte()

        val verdict = PluginSignatures.verifyAndAudit(manifestBytes, codeBytes, anchors, null)
        assertEquals(PluginSignatures.Verdict.Reason.CODE_HASH_MISMATCH, (verdict as PluginSignatures.Verdict.Rejected).reason)
    }

    @Test
    fun `unknown signer rejected`() {
        val signerKeys = PluginSignatures.generateKeyPair(random)
        val otherKeys = PluginSignatures.generateKeyPair(random)
        val anchors = InMemoryTrustAnchors(otherKeys.publicKey)
        val (manifestBytes, codeBytes) = distribution(signerKeys)

        val verdict = PluginSignatures.verifyAndAudit(manifestBytes, codeBytes, anchors, null)
        assertEquals(PluginSignatures.Verdict.Reason.UNTRUSTED_SIGNER, (verdict as PluginSignatures.Verdict.Rejected).reason)
    }

    @Test
    fun `signature from different key rejected`() {
        val signerKeys = PluginSignatures.generateKeyPair(random)
        val attackerKeys = PluginSignatures.generateKeyPair(random)
        val anchors = InMemoryTrustAnchors(attackerKeys.publicKey)

        // Manifest claims the attacker's key id but carries a signature made
        // with the honest signer's private key -> Ed25519 must fail.
        val code = "wasm-bytes".toByteArray()
        val manifest = PluginManifest(
            pluginId = "test.plugin",
            versionCode = 1,
            displayName = "Test",
            capabilities = setOf(PluginCapability.PRIVATE_STORAGE),
            codeSize = code.size.toLong(),
            codeSha256 = sha256(code),
            signerKeyId = attackerKeys.keyId,
        )
        val unsigned = PluginManifestCodec.encodeUnsigned(manifest)
        val signed = PluginManifestCodec.attachSignature(
            unsigned,
            PluginSignatures.sign(unsigned, signerKeys.privateKey)
        )

        val verdict = PluginSignatures.verifyAndAudit(signed, code, anchors, null)
        assertEquals(PluginSignatures.Verdict.Reason.BAD_SIGNATURE, (verdict as PluginSignatures.Verdict.Rejected).reason)
    }

    @Test
    fun `verification outcomes audited`() {
        val keys = PluginSignatures.generateKeyPair(random)
        val anchors = InMemoryTrustAnchors(keys.publicKey)
        val events = ArrayList<PluginAuditEvent>()
        val sink = PluginAuditSink { events.add(it) }
        val (manifestBytes, codeBytes) = distribution(keys)

        PluginSignatures.verifyAndAudit(manifestBytes, codeBytes, anchors, sink)
        assertEquals(PluginAuditEvent.Kind.MANIFEST_VERIFIED, events.single().kind)

        PluginSignatures.verifyAndAudit(manifestBytes, "other".toByteArray(), anchors, sink)
        assertEquals(PluginAuditEvent.Kind.MANIFEST_REJECTED, events.last().kind)
    }

    @Test
    fun `key ids stable across recomputation`() {
        val keys = PluginSignatures.generateKeyPair(random)
        assertEquals(keys.keyId, TrustAnchors.keyIdOf(keys.publicKey))
        assertFalse(keys.keyId == PluginSignatures.generateKeyPair(random).keyId)
    }
}
