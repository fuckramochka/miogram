package app.miogram.core.plugins

import app.miogram.core.wasm.FakeWasmRuntime
import app.miogram.core.wasm.WasmTrapException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

class PluginEngineTest {

    private val random = SecureRandom()
    private lateinit var keys: PluginSignatures.KeyPair
    private lateinit var runtime: FakeWasmRuntime
    private lateinit var repository: InMemoryPluginRepository
    private lateinit var events: MutableList<PluginAuditEvent>
    private lateinit var engine: MiogramPluginEngine

    @Before
    fun setUp() {
        keys = PluginSignatures.generateKeyPair(random)
        runtime = FakeWasmRuntime { _, payload -> payload }
        repository = InMemoryPluginRepository()
        events = mutableListOf()
        engine = MiogramPluginEngine(
            repository, runtime, InMemoryTrustAnchors(keys.publicKey),
            auditSink = PluginAuditSink { events.add(it) },
        )
    }

    /** Builds a signed distribution; handler drives the fake wasm behaviour. */
    private fun distribution(
        pluginId: String = "test.plugin",
        versionCode: Int = 1,
        capabilities: Set<PluginCapability> = setOf(PluginCapability.READ_MESSAGE_EVENTS),
        code: ByteArray = "wasm:${pluginId}:${versionCode}".toByteArray(),
        signer: PluginSignatures.KeyPair = keys,
    ): Pair<ByteArray, ByteArray> {
        val manifest = PluginManifest(
            pluginId = pluginId,
            versionCode = versionCode,
            displayName = "Test Plugin",
            capabilities = capabilities,
            codeSize = code.size.toLong(),
            codeSha256 = sha256(code),
            signerKeyId = signer.keyId,
        )
        val unsigned = PluginManifestCodec.encodeUnsigned(manifest)
        val signed = PluginManifestCodec.attachSignature(
            unsigned,
            PluginSignatures.sign(unsigned, signer.privateKey)
        )
        return signed to code
    }

    @Test
    fun `install persists verified distribution`() {
        val (manifestBytes, codeBytes) = distribution()

        val result = engine.install(manifestBytes, codeBytes)
        assertTrue(result is MiogramPluginEngine.InstallResult.Installed)
        assertEquals(PluginState.INSTALLED, repository.find("test.plugin")?.state)
        assertTrue(events.any { it.kind == PluginAuditEvent.Kind.MANIFEST_VERIFIED })
    }

    @Test
    fun `tampered code is rejected and never stored`() {
        val (manifestBytes, codeBytes) = distribution()
        codeBytes[0] = (codeBytes[0].toInt() xor 1).toByte()

        val result = engine.install(manifestBytes, codeBytes)
        assertEquals(
            MiogramPluginEngine.InstallResult.Reason.CODE_HASH_MISMATCH,
            (result as MiogramPluginEngine.InstallResult.Rejected).reason
        )
        assertEquals(null, repository.find("test.plugin"))
    }

    @Test
    fun `upgrade requires strictly newer version`() {
        val (m1, c1) = distribution(versionCode = 2)
        val (m2, c2) = distribution(versionCode = 3)

        assertTrue(engine.install(m1, c1) is MiogramPluginEngine.InstallResult.Installed)
        assertEquals(
            MiogramPluginEngine.InstallResult.Reason.VERSION_NOT_NEWER,
            (engine.install(m1, c1) as MiogramPluginEngine.InstallResult.Rejected).reason
        )
        assertTrue(engine.install(m2, c2) is MiogramPluginEngine.InstallResult.Installed)
    }

    @Test
    fun `enable then dispatch echoes through sandbox`() {
        val (m, c) = distribution(capabilities = setOf(PluginCapability.READ_MESSAGE_EVENTS))
        engine.install(m, c)
        assertTrue(engine.enable("test.plugin") is MiogramPluginEngine.EnableResult.Enabled)

        val outcome = engine.dispatch("test.plugin", "on_message_receive", "hello".toByteArray())
        assertArrayEquals("hello".toByteArray(), (outcome as MiogramPluginEngine.DispatchOutcome.Ok).response)
    }

    @Test
    fun `dispatch without capability is denied but not quarantined`() {
        val (m, c) = distribution(capabilities = setOf(PluginCapability.READ_MESSAGE_EVENTS))
        engine.install(m, c)
        engine.enable("test.plugin")

        val outcome = engine.dispatch("test.plugin", "send_message", null)
        assertTrue(outcome is MiogramPluginEngine.DispatchOutcome.Denied)

        // Expected denials must not count as faults.
        assertFalse(engine.quarantinedIds().contains("test.plugin"))
        assertTrue(events.any { it.kind == PluginAuditEvent.Kind.CAPABILITY_DENIED })
    }

    @Test
    fun `unknown operations denied by default`() {
        val (m, c) = distribution(capabilities = CapabilitySet.of(*PluginCapability.entries.toTypedArray()).toList().toSet())
        engine.install(m, c)
        engine.enable("test.plugin")

        val outcome = engine.dispatch("test.plugin", "exfiltrate_contacts", null)
        assertTrue((outcome as MiogramPluginEngine.DispatchOutcome.Denied).reason.contains("unknown op"))
    }

    @Test
    fun `dispatch before enable is denied`() {
        val (m, c) = distribution()
        engine.install(m, c)
        assertTrue(engine.dispatch("test.plugin", "on_message_receive", null) is MiogramPluginEngine.DispatchOutcome.Denied)
    }

    @Test
    fun `traps accumulate to quarantine`() {
        var calls = 0
        runtime = FakeWasmRuntime { _, _ ->
            calls++
            throw WasmTrapException("guest panic $calls")
        }
        engine = MiogramPluginEngine(
            repository, runtime, InMemoryTrustAnchors(keys.publicKey),
            auditSink = PluginAuditSink { events.add(it) }, faultLimit = 3,
        )

        val (m, c) = distribution()
        engine.install(m, c)
        engine.enable("test.plugin")

        repeat(3) { index ->
            val outcome = engine.dispatch("test.plugin", "on_message_receive", null)
            if (index < 2) {
                assertTrue(outcome is MiogramPluginEngine.DispatchOutcome.Failed)
            } else {
                // The third fault closes the instance; the call itself still traps first.
                assertTrue(outcome is MiogramPluginEngine.DispatchOutcome.Failed)
            }
        }

        assertTrue(engine.quarantinedIds().contains("test.plugin"))
        assertEquals(PluginState.QUARANTINED, repository.find("test.plugin")?.state)

        // Fail fast after quarantine.
        assertTrue(engine.dispatch("test.plugin", "on_message_receive", null) is MiogramPluginEngine.DispatchOutcome.Denied)
        assertEquals(3, calls)
    }

    @Test
    fun `reEnable resets faults and restores service`() {
        var shouldThrow = true
        runtime = FakeWasmRuntime { _, payload ->
            if (shouldThrow) throw WasmTrapException("boom")
            payload
        }
        engine = MiogramPluginEngine(repository, runtime, InMemoryTrustAnchors(keys.publicKey), faultLimit = 1)

        val (m, c) = distribution()
        engine.install(m, c)
        engine.enable("test.plugin")
        engine.dispatch("test.plugin", "on_message_receive", null)
        assertTrue(engine.quarantinedIds().contains("test.plugin"))

        shouldThrow = false
        assertTrue(engine.reEnable("test.plugin") is MiogramPluginEngine.EnableResult.Enabled)
        assertTrue(engine.dispatch("test.plugin", "on_message_receive", "ok".toByteArray()) is MiogramPluginEngine.DispatchOutcome.Ok)
    }

    @Test
    fun `disable closes the live instance`() {
        val (m, c) = distribution()
        engine.install(m, c)
        engine.enable("test.plugin")

        engine.disable("test.plugin")
        assertEquals(1, runtime.closedInstances)
        assertTrue(engine.dispatch("test.plugin", "on_message_receive", null) is MiogramPluginEngine.DispatchOutcome.Denied)
        assertEquals(PluginState.DISABLED, repository.find("test.plugin")?.state)
    }

    @Test
    fun `uninstall removes everything`() {
        val (m, c) = distribution()
        engine.install(m, c)
        engine.enable("test.plugin")

        assertTrue(engine.uninstall("test.plugin"))
        assertFalse(engine.uninstall("test.plugin"))
        assertEquals(1, runtime.closedInstances)
        assertEquals(null, repository.find("test.plugin"))
    }

    @Test
    fun `upgrade of enabled plugin swaps instance`() {
        val (m1, c1) = distribution(versionCode = 1)
        val (m2, c2) = distribution(versionCode = 2)
        engine.install(m1, c1)
        engine.enable("test.plugin")

        assertTrue(engine.install(m2, c2) is MiogramPluginEngine.InstallResult.Installed)
        assertEquals(1, runtime.closedInstances) // old version torn down

        assertTrue(engine.enable("test.plugin") is MiogramPluginEngine.EnableResult.Enabled)
    }

    @Test
    fun `installed plugin limit enforced`() {
        val tinyRepo = InMemoryPluginRepository()
        engine = MiogramPluginEngine(tinyRepo, runtime, InMemoryTrustAnchors(keys.publicKey), maxInstalledPlugins = 1)

        val (m1, c1) = distribution(pluginId = "a.one")
        val (m2, c2) = distribution(pluginId = "b.two")

        assertTrue(engine.install(m1, c1) is MiogramPluginEngine.InstallResult.Installed)
        assertEquals(
            MiogramPluginEngine.InstallResult.Reason.LIMIT_REACHED,
            (engine.install(m2, c2) as MiogramPluginEngine.InstallResult.Rejected).reason
        )
    }

    @Test
    fun `shutdownAll disables every live instance`() {
        val (ma, ca) = distribution(pluginId = "a")
        val (mb, cb) = distribution(pluginId = "b")
        engine.install(ma, ca)
        engine.install(mb, cb)
        engine.enable("a")
        engine.enable("b")

        engine.shutdownAll()
        assertEquals(2, runtime.closedInstances)
        assertEquals(PluginState.DISABLED, repository.find("a")?.state)
        assertEquals(PluginState.DISABLED, repository.find("b")?.state)
    }
}
