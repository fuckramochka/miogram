package app.miogram.bridge.plugins

import app.miogram.core.wasm.SandboxConfig
import app.miogram.core.wasm.WasmInstance
import app.miogram.core.wasm.WasmModule
import app.miogram.core.wasm.WasmRuntime
import app.miogram.core.wasm.WasmTrapException
import org.telegram.messenger.NativeLoader

/**
 * WAMR-backed [WasmRuntime] living inside libtmessages*.so
 * (jni/miogram/miogram_wasm.c). The C section is compiled only when the
 * third_party/wasm-micro-runtime submodule is checked out; otherwise
 * [isAvailable] reports false and callers must degrade gracefully.
 */
object WamrWasmRuntime : WasmRuntime {

    @JvmStatic
    fun isAvailable(): Boolean = runCatching { NativeLoader.loaded() }.getOrDefault(false)

    override fun loadModule(wasmBytes: ByteArray, config: SandboxConfig): WasmModule {
        check(isAvailable()) { "native runtime unavailable (submodule not built)" }
        val modulePtr = try {
            nativeLoadModule(wasmBytes)
        } catch (e: UnsatisfiedLinkError) {
            throw WasmTrapException("wasm bridge missing in this build", e)
        }
        return object : WasmModule {
            private val closed = java.util.concurrent.atomic.AtomicBoolean(false)

            init {
                if (modulePtr == 0L) {
                    closed.set(true)
                    throw WasmTrapException("module load returned null")
                }
            }

            override fun instantiate(): WasmInstance {
                check(!closed.get()) { "module closed" }
                val handlePtr = nativeInstantiate(modulePtr)
                return object : WasmInstance {
                    private val instanceClosed = java.util.concurrent.atomic.AtomicBoolean(false)

                    init {
                        if (handlePtr == 0L) {
                            instanceClosed.set(true)
                            throw WasmTrapException("instantiation failed")
                        }
                    }

                    override fun callExport(function: String, payload: ByteArray?): ByteArray? =
                        nativeCallExport(handlePtr, function, payload)

                    // Fuel accounting requires epoch-based preemption (Этап 2.1);
                    // reported as zero until then.
                    override fun consumedFuelMs(): Long = 0

                    override fun close() {
                        if (instanceClosed.compareAndSet(false, true)) {
                            nativeCloseInstance(handlePtr)
                        }
                    }
                }
            }

            override fun close() {
                if (closed.compareAndSet(false, true)) {
                    nativeCloseModule(modulePtr)
                }
            }
        }
    }

    // JNI surface — implemented in jni/miogram/miogram_wasm.c.
    private external fun nativeLoadModule(wasmBytes: ByteArray): Long
    private external fun nativeInstantiate(modulePtr: Long): Long
    private external fun nativeCloseModule(modulePtr: Long)
    private external fun nativeCloseInstance(handlePtr: Long)
    private external fun nativeCallExport(handlePtr: Long, function: String, payload: ByteArray?): ByteArray?
}
