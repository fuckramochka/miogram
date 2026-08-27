// --- CompactBottomNav Rust/WASM Native Plugin for Miogram ---
// Compact bottom navigation bar without labels and with floating search button.

#[link(wasm_import_module = "env")]
extern "C" {
    fn host_log(ptr: *const u8, len: usize);
    fn host_apply_bottom_nav_compact(hide_labels: bool, add_search_button: bool);
}

#[derive(Default)]
pub struct BottomNavConfig {
    pub hide_labels: bool,
    pub add_search_button: bool,
}

static mut CONFIG: BottomNavConfig = BottomNavConfig {
    hide_labels: true,
    add_search_button: true,
};

#[no_mangle]
pub extern "C" fn alloc(size: usize) -> *mut u8 {
    let mut buf = Vec::with_capacity(size);
    let ptr = buf.as_mut_ptr();
    core::mem::forget(buf);
    ptr
}

#[no_mangle]
pub extern "C" fn dealloc(ptr: *mut u8, size: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(ptr, 0, size);
    }
}

#[no_mangle]
pub extern "C" fn on_plugin_init() -> u32 {
    let msg = "CompactBottomNav Rust plugin initialized.";
    unsafe {
        host_log(msg.as_ptr(), msg.len());
        host_apply_bottom_nav_compact(CONFIG.hide_labels, CONFIG.add_search_button);
    }
    0
}

#[no_mangle]
pub extern "C" fn on_settings_changed(add_search_button: u32, hide_labels: u32) -> u32 {
    unsafe {
        CONFIG.add_search_button = add_search_button != 0;
        CONFIG.hide_labels = hide_labels != 0;
        host_apply_bottom_nav_compact(CONFIG.hide_labels, CONFIG.add_search_button);
    }
    0
}
