package app.miogram.core.plugins

import app.miogram.core.wasm.SandboxConfig
import app.miogram.core.wasm.WasmInstance
import app.miogram.core.wasm.WasmRuntime
import app.miogram.core.wasm.WasmTrapException
import java.util.concurrent.atomic.AtomicInteger

enum class PluginState { INSTALLED, ENABLED, DISABLED, QUARANTINED }

/**
 * Installed distribution. [code] is retained so the engine can re-instantiate;
 * persistence implementations are responsible for encrypting it at rest.
 */
data class InstalledPlugin(
    val pluginId: String,
    val versionCode: Int,
    val displayName: String,
    val capabilities: CapabilitySet,
    val sandboxConfig: SandboxConfig = SandboxConfig(),
    val state: PluginState = PluginState.INSTALLED,
    val code: ByteArray,
) {
    fun withCode(code: ByteArray): InstalledPlugin = copy(code = code)

    fun withState(state: PluginState): InstalledPlugin = copy(state = state)
}

/**
 * Lifecycle orchestrator: install (verify → persist) → enable (instantiate) →
 * dispatch (capability gate → guest call) with automatic quarantine on traps.
 *
 * Concurrency: coarse-grained synchronization — plugin management is rare and
 * dispatch is serialized per call; swap for per-plugin locks if benchmarks
 * ever demand it.
 *
 * Failure policy:
 *  * a trap during dispatch increments the fault counter; reaching
 *    [faultLimit] quarantines the plugin (instances closed, dispatch fails
 *    fast until an explicit re-enable);
 *  * capability denials are NOT faults — they are expected authorization
 *    outcomes and only audited.
 */
class MiogramPluginEngine(
    private val repository: PluginRepository,
    private val runtime: WasmRuntime,
    private val anchors: TrustAnchors,
    private val auditSink: PluginAuditSink? = null,
    private val maxInstalledPlugins: Int = DEFAULT_MAX_PLUGINS,
    private val faultLimit: Int = DEFAULT_FAULT_LIMIT,
) {

    private class Active(
        val module: app.miogram.core.wasm.WasmModule,
        val instance: WasmInstance,
        val grants: CapabilitySet,
        val faults: AtomicInteger = AtomicInteger(0),
    )

    private val lock = Any()
    private val activeInstances = HashMap<String, Active>()

    // --- install / uninstall ------------------------------------------------

    sealed class InstallResult {
        data class Installed(val pluginId: String, val upgraded: Boolean) : InstallResult()

        data class Rejected(val reason: Reason, val detail: String? = null) : InstallResult()

        enum class Reason {
            MALFORMED,
            UNTRUSTED,
            BAD_SIGNATURE,
            CODE_SIZE_MISMATCH,
            CODE_HASH_MISMATCH,
            VERSION_NOT_NEWER,
            LIMIT_REACHED,
        }
    }

    fun install(manifestBytes: ByteArray, codeBytes: ByteArray): InstallResult {
        val verdict = PluginSignatures.verifyAndAudit(manifestBytes, codeBytes, anchors, auditSink)
        val manifest = (verdict as? PluginSignatures.Verdict.Valid)?.manifest

        synchronized(lock) {
            if (manifest == null) {
                val rejection = verdict as PluginSignatures.Verdict.Rejected
                return@install InstallResult.Rejected(toInstallReason(rejection.reason), rejection.detail)
            }
            val existing = repository.find(manifest.pluginId)
            if (existing != null && manifest.versionCode <= existing.versionCode) {
                return InstallResult.Rejected(
                    InstallResult.Reason.VERSION_NOT_NEWER,
                    "installed=${existing.versionCode} incoming=${manifest.versionCode}",
                )
            }
            if (existing == null && repository.list().size >= maxInstalledPlugins) {
                return InstallResult.Rejected(InstallResult.Reason.LIMIT_REACHED)
            }

            val upgraded = existing != null
            // An upgrade invalidates any running instance of the old version.
            if (upgraded) closeActiveLocked(manifest.pluginId)

            repository.save(
                InstalledPlugin(
                    pluginId = manifest.pluginId,
                    versionCode = manifest.versionCode,
                    displayName = manifest.displayName,
                    capabilities = manifest.capabilities.fold(CapabilitySet()) { acc, c -> acc + c },
                    code = codeBytes.copyOf(),
                )
            )
            return InstallResult.Installed(manifest.pluginId, upgraded)
        }
    }

    fun uninstall(pluginId: String): Boolean {
        synchronized(lock) {
            closeActiveLocked(pluginId)
            val removed = repository.find(pluginId) != null
            repository.delete(pluginId)
            return removed
        }
    }

    // --- enable / disable ---------------------------------------------------

    sealed class EnableResult {
        data object Enabled : EnableResult()
        data class Failed(val reason: String) : EnableResult()
    }

    /** Instantiates the module; the instance stays open until disable/quarantine. */
    fun enable(pluginId: String): EnableResult {
        synchronized(lock) {
            val plugin = repository.find(pluginId) ?: return EnableResult.Failed("not installed")

            closeActiveLocked(pluginId)

            return try {
                // The module stays open for as long as the instance lives —
                // closing it earlier would invalidate guest memory handles.
                val module = runtime.loadModule(plugin.code, plugin.sandboxConfig)
                val instance = try {
                    module.instantiate()
                } catch (e: Throwable) {
                    module.close()
                    throw e
                }
                activeInstances[pluginId] = Active(module, instance, plugin.capabilities)
                repository.save(plugin.withState(PluginState.ENABLED))
                EnableResult.Enabled
            } catch (e: WasmTrapException) {
                auditSink?.onEvent(
                    PluginAuditEvent(pluginId, PluginAuditEvent.Kind.SANDBOX_FAULT, "enable: ${e.message}")
                )
                repository.save(plugin.withState(PluginState.QUARANTINED))
                EnableResult.Failed("trap: ${e.message?.take(120)}")
            } catch (e: IllegalStateException) {
                auditSink?.onEvent(
                    PluginAuditEvent(pluginId, PluginAuditEvent.Kind.SANDBOX_FAULT, "enable: ${e.message}")
                )
                EnableResult.Failed("state: ${e.message?.take(120)}")
            }
        }
    }

    fun disable(pluginId: String) {
        synchronized(lock) {
            closeActiveLocked(pluginId)
            repository.find(pluginId)?.let { plugin ->
                if (plugin.state != PluginState.INSTALLED) {
                    repository.save(plugin.withState(PluginState.DISABLED))
                }
            }
        }
    }

    // --- dispatch ------------------------------------------------------------

    sealed class DispatchOutcome {
        data class Ok(val response: ByteArray?) : DispatchOutcome()
        data class Denied(val reason: String) : DispatchOutcome()
        data class Failed(val reason: String) : DispatchOutcome()
    }

    /**
     * Runs one host operation against an enabled plugin. The required
     * capability is derived from [op]; unknown operations are denied by
     * default before touching guest memory.
     */
    fun dispatch(pluginId: String, op: String, payload: ByteArray?): DispatchOutcome {
        synchronized(lock) {
            val active = activeInstances[pluginId]
                ?: return DispatchOutcome.Denied("plugin not enabled")
            if (repository.find(pluginId)?.state != PluginState.ENABLED) {
                return DispatchOutcome.Denied("plugin not enabled")
            }

            val required = OpPolicy.requiredCapability(op)
                ?: return denied(pluginId, "unknown op '$op'")

            if (!gate.require(pluginId, active.grants, required)) {
                return DispatchOutcome.Denied("capability ${required.name} not granted")
            }

            return try {
                DispatchOutcome.Ok(active.instance.callExport("miogram_call", payload))
            } catch (e: WasmTrapException) {
                registerFault(pluginId, active, e.message)
                DispatchOutcome.Failed("trap: ${e.message?.take(120)}")
            }
        }
    }

    fun quarantinedIds(): List<String> = synchronized(lock) {
        repository.list().filter { it.state == PluginState.QUARANTINED }.map { it.pluginId }
    }

    /** Manual escape from quarantine; re-instantiates immediately. */
    fun reEnable(pluginId: String): EnableResult {
        synchronized(lock) {
            repository.find(pluginId)?.let { repository.save(it.withState(PluginState.INSTALLED)) }
            activeInstances[pluginId]?.faults?.set(0)
        }
        return enable(pluginId)
    }

    /** Closes every live instance; called when the host tears plugins down. */
    fun shutdownAll() {
        synchronized(lock) {
            for (id in activeInstances.keys.toList()) {
                closeActiveLocked(id)
                repository.find(id)?.let { repository.save(it.withState(PluginState.DISABLED)) }
            }
        }
    }

    // --- internals -----------------------------------------------------------

    private val gate = CapabilityGate(auditSink)

    private fun registerFault(pluginId: String, active: Active, message: String?) {
        val count = active.faults.incrementAndGet()
        auditSink?.onEvent(
            PluginAuditEvent(pluginId, PluginAuditEvent.Kind.SANDBOX_FAULT, "fault #$count: $message")
        )
        if (count >= faultLimit) {
            auditSink?.onEvent(
                PluginAuditEvent(pluginId, PluginAuditEvent.Kind.SANDBOX_FAULT, "quarantined after $count faults")
            )
            closeActiveLocked(pluginId)
            repository.find(pluginId)?.let { repository.save(it.withState(PluginState.QUARANTINED)) }
        }
    }

    private fun denied(pluginId: String, reason: String): DispatchOutcome.Denied {
        auditSink?.onEvent(PluginAuditEvent(pluginId, PluginAuditEvent.Kind.CAPABILITY_DENIED, reason))
        return DispatchOutcome.Denied(reason)
    }

    private fun closeActiveLocked(pluginId: String) {
        activeInstances.remove(pluginId)?.let { active ->
            active.instance.close()
            active.module.close()
        }
    }

    private fun toInstallReason(reason: PluginSignatures.Verdict.Reason): InstallResult.Reason =
        when (reason) {
            PluginSignatures.Verdict.Reason.MALFORMED -> InstallResult.Reason.MALFORMED
            PluginSignatures.Verdict.Reason.UNTRUSTED_SIGNER -> InstallResult.Reason.UNTRUSTED
            PluginSignatures.Verdict.Reason.BAD_SIGNATURE -> InstallResult.Reason.BAD_SIGNATURE
            PluginSignatures.Verdict.Reason.CODE_SIZE_MISMATCH -> InstallResult.Reason.CODE_SIZE_MISMATCH
            PluginSignatures.Verdict.Reason.CODE_HASH_MISMATCH -> InstallResult.Reason.CODE_HASH_MISMATCH
        }

    companion object {
        const val DEFAULT_MAX_PLUGINS = 16
        const val DEFAULT_FAULT_LIMIT = 3
    }
}
