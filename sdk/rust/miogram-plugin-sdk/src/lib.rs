//! Miogram WASM plugin SDK.
//!
//! ## Contract with the host (jni/miogram/miogram_wasm.c)
//!
//! Guest exports required from every plugin:
//! ```text
//! miogram_abi_version() -> i32                    // must return 1
//! miogram_alloc(len: i32) -> i32                  // guest-owned buffer
//! miogram_guest_free(ptr: i32, len: i32)          // release buffers given out
//! miogram_call(ptr: i32, len: i32) -> i64         // packed resp_ptr<<32 | len,
//!                                                  // or -1 on failure
//! ```
//!
//! The host allocates the request buffer through `miogram_alloc`, writes one
//! [`envelope`] frame into it and calls `miogram_call`. The plugin encodes a
//! response frame, hands ownership to the host via the packed pointer pair and
//! the host releases both regions through `miogram_guest_free`.
//!
//! All allocations are served by this crate's global allocator — the host
//! never touches plugin heap management directly, which keeps the ABI stable
//! across allocator changes.
//!
//! ## Writing a plugin
//!
//! ```ignore
//! use miogram_plugin_sdk::{register, Plugin};
//!
//! struct Echo;
//!
//! impl Plugin for Echo {
//!     fn handle(&mut self, op: &str, payload: &[u8]) -> Result<Vec<u8>, i32> {
//!         Ok(payload.to_vec())
//!     }
//! }
//!
//! register!(Echo);
//! ```

use std::panic;

pub mod envelope;

/// Semantic error codes surfaced as `-1` responses; details go inside the
/// error frame payload when applicable.
pub const ERR_BAD_FRAME: i32 = 1;
pub const ERR_HANDLER_PANICKED: i32 = 2;
pub const ERR_NO_PLUGIN: i32 = 3;

/// User-implemented handler. One instance per module; calls are serialized by
/// the host (single exec_env per instance).
pub trait Plugin: Send {
    fn handle(&mut self, op: &str, payload: &[u8]) -> Result<Vec<u8>, i32>;
}

static mut PLUGIN: Option<Box<dyn Plugin>> = None;
static mut FACTORY: Option<fn() -> Box<dyn Plugin>> = None;

/// Registers how to build the plugin instance. The instance itself is created
/// lazily before the first [`miogram_call`] — no constructor sections are
/// involved (WAMR does not run them).
#[macro_export]
macro_rules! register {
    ($ty:ty) => {
        #[no_mangle]
        pub extern "C" fn miogram_create_plugin() {
            $crate::set_plugin_factory(|| Box::new(<$ty>::default()));
        }
    };
}

/// Sets the plugin factory. See [`register!`].
pub fn set_plugin_factory(factory: fn() -> Box<dyn Plugin>) {
    unsafe {
        FACTORY = Some(factory);
    }
}

/// Eager variant for host-side tests and manual setups.
pub fn register_plugin(factory: fn() -> Box<dyn Plugin>) {
    unsafe {
        FACTORY = Some(factory);
        ensure_instance().ok();
    }
}

fn ensure_instance() -> Result<(), i32> {
    unsafe {
        #[allow(static_mut_refs)]
        if PLUGIN.is_none() {
            let factory = FACTORY.ok_or(ERR_NO_PLUGIN)?;
            PLUGIN = Some(factory());
        }
        Ok(())
    }
}

fn dispatch(payload: &[u8]) -> Result<Vec<u8>, i32> {
    let frame = envelope::decode(payload).map_err(|_| ERR_BAD_FRAME)?;
    ensure_instance()?;
    let plugin = unsafe {
        #[allow(static_mut_refs)]
        PLUGIN.as_mut()
    };
    match plugin {
        Some(p) => p.handle(frame.op, frame.payload),
        None => Err(ERR_NO_PLUGIN),
    }
}

// --- FFI surface -----------------------------------------------------------

/// ABI revision expected by the host bridge. Bump on breaking changes.
#[no_mangle]
pub extern "C" fn miogram_abi_version() -> i32 {
    1
}

/// Guest-side allocation used by the host for request AND response buffers.
///
/// # Safety
/// Host-only; returns a dangling-aligned pointer into plugin-owned memory.
#[no_mangle]
pub unsafe extern "C" fn miogram_alloc(len: i32) -> i32 {
    if len <= 0 {
        return 0;
    }
    let mut buf = Vec::<u8>::with_capacity(len as usize);
    let ptr = buf.as_mut_ptr();
    std::mem::forget(buf);
    ptr as i32
}

/// Releases a buffer previously produced by [`miogram_alloc`].
///
/// # Safety
/// Host-only; `ptr`/`len` must come from a buffer this allocator handed out
/// and must not be freed twice.
#[no_mangle]
pub unsafe extern "C" fn miogram_guest_free(ptr: i32, len: i32) {
    if ptr == 0 || len < 0 {
        return;
    }
    drop(Vec::from_raw_parts(ptr as *mut u8, len as usize, len as usize));
}

/// Main entry point. Layout of the returned i64: `(ptr << 32) | len`, or -1.
///
/// # Safety
/// Host-only; `ptr..ptr+len` must contain a valid request frame.
#[no_mangle]
pub unsafe extern "C" fn miogram_call(ptr: *const u8, len: i32) -> i64 {
    if ptr.is_null() || len < 0 {
        return -1;
    }
    let request = std::slice::from_raw_parts(ptr, len as usize);

    // panic = "abort" in release turns panics into traps the host already
    // handles; catch_unwind here covers dev builds where unwinding is on.
    let outcome = panic::catch_unwind(|| dispatch(request)).unwrap_or_else(|_| Err(ERR_HANDLER_PANICKED));

    match outcome {
        Ok(response) => pack_response(response),
        Err(code) => -(code as i64) - 1, // negative, distinguishable from packed pointers
    }
}

unsafe fn pack_response(mut response: Vec<u8>) -> i64 {
    let len = response.len();
    debug_assert!(len <= i32::MAX as usize);
    response.shrink_to_fit();
    let ptr = response.as_mut_ptr();
    std::mem::forget(response);
    ((ptr as i64) << 32) | (len as i64)
}
