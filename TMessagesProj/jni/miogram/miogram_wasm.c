/*
 * Miogram WASM sandbox — JNI bridge to WAMR (WebAssembly Micro Runtime).
 *
 * Built into libtmessages*.so alongside the rest of jni/; symbols are
 * exported through the existing `Java_*` wildcard in exports.map. The whole
 * section is optional: without third_party/wasm-micro-runtime the CMake guard
 * simply omits it and the Kotlin side reports "runtime unavailable".
 *
 * Call protocol (mirrored by the Rust SDK, sdk/rust/miogram-plugin-sdk):
 *
 * Guest exports required from every plugin:
 *   miogram_abi_version() -> i32
 *   miogram_alloc(len: i32) -> i32              // plugin-allocator owned
 *   miogram_guest_free(ptr: i32, len: i32)
 *   miogram_call(ptr: i32, len: i32) -> i64     // packed resp_ptr<<32 | len,
 *                                                // or -1 on failure
 *
 * ALL guest buffers (request and response) are allocated through the plugin's
 * own allocator via miogram_alloc and released through miogram_guest_free.
 * WAMR's module_malloc is deliberately NOT used for payloads: it manages a
 * runtime-owned heap that the guest allocator knows nothing about, and mixing
 * the two would corrupt memory.
 *
 * Concurrency: one exec_env per instance guarded by a mutex — WAMR exec_envs
 * are not reentrant. Preemption (fuel/epoch) is NOT implemented yet; only
 * Ed25519-signed trusted modules reach this layer until it lands.
 */

#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "iwasm.h"

#define MIOGRAM_ABI_VERSION 1
#define EXEC_STACK_SIZE (256u * 1024u)
#define MAX_PAYLOAD (4u * 1024u * 1024u)

typedef struct {
    wasm_module_t module;
    wasm_module_inst_t instance;
    wasm_exec_env_t exec_env;
    pthread_mutex_t lock;
} miogram_plugin_handle_t;

static int g_runtime_ready = 0;

static void miogram_throw(JNIEnv *env, const char *cls, const char *msg) {
    jclass ex = (*env)->FindClass(env, cls);
    if (ex != NULL) {
        (*env)->ThrowNew(env, ex, msg);
    }
}

static int miogram_init_runtime(void) {
    if (g_runtime_ready) {
        return 1;
    }
    RuntimeInitArgs init_args;
    memset(&init_args, 0, sizeof(init_args));
    init_args.mem_alloc_type = Alloc_With_System_Allocator;
    init_args.max_thread_num = 1;

    if (wasm_runtime_full_init(&init_args) == false) {
        return 0;
    }
    g_runtime_ready = 1;
    return 1;
}

/* --- module lifecycle ----------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_app_miogram_bridge_plugins_WamrWasmRuntime_nativeLoadModule(
        JNIEnv *env, jclass clazz, jbyteArray wasm_bytes)
{
    (void) clazz;
    if (!miogram_init_runtime()) {
        miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "runtime init failed");
        return 0;
    }

    jsize length = (*env)->GetArrayLength(env, wasm_bytes);
    if (length <= 0 || (uint32_t) length > (64u * 1024u * 1024u)) {
        miogram_throw(env, "java/lang/IllegalArgumentException", "module size out of range");
        return 0;
    }

    jbyte *elements = (*env)->GetByteArrayElements(env, wasm_bytes, NULL);
    if (elements == NULL) {
        miogram_throw(env, "java/lang/OutOfMemoryError", "copy failed");
        return 0;
    }

    char error_buf[128];
    wasm_module_t module = wasm_runtime_load((uint8_t *) elements, (uint32_t) length,
                                             error_buf, sizeof(error_buf));
    (*env)->ReleaseByteArrayElements(env, wasm_bytes, elements, JNI_ABORT);

    if (module == NULL) {
        miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", error_buf);
        return 0;
    }
    return (jlong) (uintptr_t) module;
}

JNIEXPORT void JNICALL
Java_app_miogram_bridge_plugins_WamrWasmRuntime_nativeCloseModule(
        JNIEnv *env, jclass clazz, jlong module_ptr)
{
    (void) env;
    (void) clazz;
    if (module_ptr != 0) {
        wasm_runtime_unload((wasm_module_t) (uintptr_t) module_ptr);
    }
}

/* --- instance lifecycle ---------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_app_miogram_bridge_plugins_WamrWasmRuntime_nativeInstantiate(
        JNIEnv *env, jclass clazz, jlong module_ptr)
{
    (void) clazz;
    if (module_ptr == 0) {
        miogram_throw(env, "java/lang/IllegalArgumentException", "null module");
        return 0;
    }
    wasm_module_inst_t instance = wasm_runtime_instantiate(
            (wasm_module_t) (uintptr_t) module_ptr,
            EXEC_STACK_SIZE,
            /* heap_size */ 512u * 1024u,
            NULL, 0);
    if (instance == NULL) {
        miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "instantiate failed");
        return 0;
    }

    miogram_plugin_handle_t *handle = calloc(1, sizeof(miogram_plugin_handle_t));
    if (handle == NULL) {
        wasm_runtime_deinstantiate(instance);
        miogram_throw(env, "java/lang/OutOfMemoryError", "handle alloc failed");
        return 0;
    }
    handle->module = (wasm_module_t) (uintptr_t) module_ptr;
    handle->instance = instance;
    pthread_mutex_init(&handle->lock, NULL);

    handle->exec_env = wasm_runtime_create_exec_env(instance, EXEC_STACK_SIZE);
    if (handle->exec_env == NULL) {
        pthread_mutex_destroy(&handle->lock);
        free(handle);
        wasm_runtime_deinstantiate(instance);
        miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "exec_env failed");
        return 0;
    }
    return (jlong) (uintptr_t) handle;
}

JNIEXPORT void JNICALL
Java_app_miogram_bridge_plugins_WamrWasmRuntime_nativeCloseInstance(
        JNIEnv *env, jclass clazz, jlong handle_ptr)
{
    (void) env;
    (void) clazz;
    miogram_plugin_handle_t *handle = (miogram_plugin_handle_t *) (uintptr_t) handle_ptr;
    if (handle == NULL) {
        return;
    }
    if (handle->exec_env != NULL) {
        wasm_runtime_destroy_exec_env(handle->exec_env);
    }
    if (handle->instance != NULL) {
        wasm_runtime_deinstantiate(handle->instance);
    }
    pthread_mutex_destroy(&handle->lock);
    free(handle);
}

/* --- invocation ------------------------------------------------------------ */

/* Guest-allocator shims: thin calls into the plugin's own exports so every
 * byte crossing the boundary belongs to one consistent heap. */

static int32_t guest_alloc(miogram_plugin_handle_t *h, int32_t len) {
    wasm_function_inst_t f = wasm_runtime_lookup_function(h->instance, "miogram_alloc");
    if (f == NULL) return 0;
    uint32_t argv[1] = { (uint32_t) len };
    if (!wasm_runtime_call_wasm(h->exec_env, f, 1, argv)) return 0;
    if (argv[0] == 0) return 0;
    return (int32_t) argv[0];
}

static void guest_free(miogram_plugin_handle_t *h, int32_t ptr, int32_t len) {
    if (ptr == 0) return;
    wasm_function_inst_t f = wasm_runtime_lookup_function(h->instance, "miogram_guest_free");
    if (f == NULL) return;
    uint32_t argv[2] = { (uint32_t) ptr, (uint32_t) len };
    wasm_runtime_call_wasm(h->exec_env, f, 2, argv);
}

static void *guest_native_addr(miogram_plugin_handle_t *h, int32_t offset) {
    return wasm_runtime_addr_app_to_native(h->instance, (uint64_t) offset);
}

JNIEXPORT jbyteArray JNICALL
Java_app_miogram_bridge_plugins_WamrWasmRuntime_nativeCallExport(
        JNIEnv *env, jclass clazz, jlong handle_ptr,
        jstring function_name, jbyteArray payload)
{
    (void) clazz;
    miogram_plugin_handle_t *handle = (miogram_plugin_handle_t *) (uintptr_t) handle_ptr;
    if (handle == NULL) {
        miogram_throw(env, "java/lang/IllegalArgumentException", "null handle");
        return NULL;
    }

    const char *name = (*env)->GetStringUTFChars(env, function_name, NULL);
    if (name == NULL) {
        return NULL;
    }

    uint32_t payload_len = payload != NULL ? (uint32_t) (*env)->GetArrayLength(env, payload) : 0;
    if (payload_len > MAX_PAYLOAD) {
        (*env)->ReleaseStringUTFChars(env, function_name, name);
        miogram_throw(env, "java/lang/IllegalArgumentException", "payload too large");
        return NULL;
    }

    jbyteArray result = NULL;

    pthread_mutex_lock(&handle->lock);

    do {
        wasm_function_inst_t func =
                wasm_runtime_lookup_function(handle->instance, "miogram_call");
        if (func == NULL) {
            miogram_throw(env, "app/miogram/core/wasm/WasmTrapException",
                           "export miogram_call not found");
            break;
        }

        /* 1. request buffer from the plugin's own allocator */
        int32_t req_offset = guest_alloc(handle, (int32_t) payload_len);
        if (payload_len > 0 && req_offset == 0) {
            miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "guest alloc failed");
            break;
        }
        if (payload_len > 0) {
            void *req_native = guest_native_addr(handle, req_offset);
            if (req_native == NULL) {
                guest_free(handle, req_offset, (int32_t) payload_len);
                miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "bad guest address");
                break;
            }
            (*env)->GetByteArrayRegion(env, payload, 0, (jsize) payload_len, (jbyte *) req_native);
        }

        /* 2. invoke: miogram_call(ptr: i32, len: i32) -> i64 */
        uint32_t argv[2] = { (uint32_t) req_offset, payload_len };
        if (!wasm_runtime_call_wasm(handle->exec_env, func, 2, argv)) {
            guest_free(handle, req_offset, (int32_t) payload_len);
            const char *exception = wasm_runtime_get_exception(handle->instance);
            miogram_throw(env, "app/miogram/core/wasm/WasmTrapException",
                           exception != NULL ? exception : "call trapped");
            break;
        }

        /* classic interpreter returns i64 as two u32 slots: lo | hi */
        uint32_t ret_lo = argv[0];
        uint32_t ret_hi = argv[1];
        if ((int32_t) ret_lo < 0 && ret_hi == 0xFFFFFFFFu) {
            guest_free(handle, req_offset, (int32_t) payload_len);
            break; /* plugin signalled soft failure; empty response */
        }

        uint64_t packed = ((uint64_t) ret_hi << 32) | ret_lo;
        int32_t resp_offset = (int32_t) (packed >> 32);
        uint32_t resp_len = (uint32_t) (packed & 0xFFFFFFFFu);

        if (resp_len > MAX_PAYLOAD) {
            guest_free(handle, resp_offset, (int32_t) resp_len);
            guest_free(handle, req_offset, (int32_t) payload_len);
            miogram_throw(env, "app/miogram/core/wasm/WasmTrapException", "response too large");
            break;
        }

        result = (*env)->NewByteArray(env, (jsize) resp_len);
        if (result != NULL && resp_len > 0) {
            void *resp_native = guest_native_addr(handle, resp_offset);
            if (resp_native != NULL) {
                (*env)->SetByteArrayRegion(env, result, 0, (jsize) resp_len, (jbyte *) resp_native);
            } else {
                (*env)->DeleteLocalRef(env, result);
                result = NULL;
            }
        }

        guest_free(handle, resp_offset, (int32_t) resp_len);
        guest_free(handle, req_offset, (int32_t) payload_len);
    } while (0);

    pthread_mutex_unlock(&handle->lock);
    (*env)->ReleaseStringUTFChars(env, function_name, name);
    return result;
}
