package app.miogram.core.wasm

/**
 * Runtime-agnostic WASM contract. Core defines the shape; the WAMR-backed
 * implementation lives in the bridge layer and can be faked on the JVM.
 *
 * Marshalling contract: payloads crossing [WasmInstance.callExport] are
 * FlatBuffers-encoded frames agreed in host_api.fbs. The runtime never sees
 * structured data — that keeps the JNI surface to byte arrays only.
 */
interface WasmRuntime {
    /**
     * Compiles [wasmBytes] without instantiating. Must reject modules whose
     * declared imports exceed [config] limits before any JIT work happens.
     */
    fun loadModule(wasmBytes: ByteArray, config: SandboxConfig = SandboxConfig()): WasmModule
}

interface WasmModule : AutoCloseable {
    /** Instantiates with a fresh linear memory bounded by [SandboxConfig.memoryPages]. */
    fun instantiate(): WasmInstance
}

interface WasmInstance : AutoCloseable {
    /**
     * Invokes export [function] with one FlatBuffers frame; returns the
     * FlatBuffers frame produced by the plugin or null for void exports.
     * @throws WasmTrapException on trap (OOM, unreachable, host deny).
     */
    fun callExport(function: String, payload: ByteArray?): ByteArray?

    /** Wall-clock budget consumed so far, milliseconds; informational. */
    fun consumedFuelMs(): Long
}

data class SandboxConfig(
    /** Linear memory ceiling in 64 KiB pages; 256 pages = 16 MiB. */
    val memoryPages: Int = DEFAULT_MEMORY_PAGES,
    /** Hard wall-clock budget per single export call. */
    val callTimeoutMs: Long = 250,
    /** Total lifetime budget for this instance. */
    val totalBudgetMs: Long = 5_000,
) {
    init {
        require(memoryPages in MIN_MEMORY_PAGES..MAX_MEMORY_PAGES) { "memoryPages out of range: $memoryPages" }
        require(callTimeoutMs in 1..10_000) { "callTimeoutMs out of range: $callTimeoutMs" }
        require(totalBudgetMs >= callTimeoutMs) { "totalBudgetMs must cover at least one call" }
    }

    companion object {
        const val MIN_MEMORY_PAGES = 1
        const val MAX_MEMORY_PAGES = 1024
        const val DEFAULT_MEMORY_PAGES = 256
    }
}

class WasmTrapException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Deterministic in-memory runtime for JVM tests and golden-path demos. */
class FakeWasmRuntime(private val handler: (function: String, payload: ByteArray?) -> ByteArray?) : WasmRuntime {

    var loadedModules = 0
        private set
    var closedInstances = 0
        private set

    override fun loadModule(wasmBytes: ByteArray, config: SandboxConfig): WasmModule {
        loadedModules++
        return object : WasmModule {
            private var open = true

            override fun instantiate(): WasmInstance {
                check(open) { "module closed" }
                return object : WasmInstance {
                    private var instanceOpen = true

                    override fun callExport(function: String, payload: ByteArray?): ByteArray? {
                        check(instanceOpen) { "instance closed" }
                        return handler(function, payload)
                    }

                    override fun consumedFuelMs(): Long = 0

                    override fun close() {
                        if (instanceOpen) {
                            instanceOpen = false
                            closedInstances++
                        }
                    }
                }
            }

            override fun close() {
                open = false
            }
        }
    }
}
