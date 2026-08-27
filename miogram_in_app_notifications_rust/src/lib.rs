// --- Miogram Native Rust/WASM In-App Notifications Plugin ---
// High performance, zero GC, microsecond event processing

// Host FFI Imports
#[link(wasm_import_module = "env")]
extern "C" {
    fn host_log(ptr: *const u8, len: usize);
    fn host_show_bulletin(payload_ptr: *const u8, payload_len: usize);
    fn host_vibrate(ms: u32);
    fn host_mark_read(dialog_id: i64, message_id: i32);
    fn host_open_chat(dialog_id: i64, message_id: i32);
}

#[derive(Default)]
pub struct PluginConfig {
    pub show_on_top: bool,
    pub allow_glass_blur: bool,
    pub notify_unmuted_only: bool,
    pub duration_ms: u32,
    pub vibrate: bool,
    pub action_type: u32, // 0: Open, 1: Mark Read
}

static mut CONFIG: PluginConfig = PluginConfig {
    show_on_top: true,
    allow_glass_blur: true,
    notify_unmuted_only: true,
    duration_ms: 2500,
    vibrate: true,
    action_type: 0,
};

// Memory Allocation FFI for Host (WAMR Runtime)
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

// Lifecycle Hooks
#[no_mangle]
pub extern "C" fn on_plugin_init() -> u32 {
    let msg = "Miogram In-App Notifications WASM Engine initialized.";
    unsafe { host_log(msg.as_ptr(), msg.len()) };
    0
}

// Event Dispatcher: Called by Miogram when a new message arrives
#[no_mangle]
pub extern "C" fn on_message_received(ptr: *const u8, len: usize) -> u32 {
    if ptr.is_null() || len == 0 {
        return 1;
    }

    let payload = unsafe { std::slice::from_raw_parts(ptr, len) };
    let json_str = match std::str::from_utf8(payload) {
        Ok(s) => s,
        Err(_) => return 1,
    };

    // Quick extraction without heavy external dependencies
    let dialog_id = extract_i64(json_str, "dialog_id").unwrap_or(0);
    let message_id = extract_i32(json_str, "message_id").unwrap_or(0);
    let sender_id = extract_i64(json_str, "sender_id").unwrap_or(0);
    let sender_name = extract_str(json_str, "sender_name").unwrap_or("User".to_string());
    let chat_title = extract_str(json_str, "chat_title");
    let text = extract_str(json_str, "text").unwrap_or("New message".to_string());
    let is_active_chat = extract_bool(json_str, "is_active_chat").unwrap_or(false);
    let is_muted = extract_bool(json_str, "is_muted").unwrap_or(false);

    if is_active_chat {
        return 0;
    }

    unsafe {
        if CONFIG.notify_unmuted_only && is_muted {
            return 0;
        }

        let title = match chat_title {
            Some(group) => format!("{} ({})", group, sender_name),
            None => sender_name,
        };

        let action_text = match CONFIG.action_type {
            1 => "Mark Read",
            _ => "Open",
        };

        let avatar_id = if dialog_id < 0 { dialog_id } else { sender_id };

        // Construct JSON response payload
        let response = format!(
            r#"{{"title":"{}","subtitle":"{}","avatar_id":{},"dialog_id":{},"message_id":{},"duration_ms":{},"show_on_top":{},"allow_glass_blur":{},"action_text":"{}","action_id":{}}}"#,
            escape_json(&title),
            escape_json(&text),
            avatar_id,
            dialog_id,
            message_id,
            CONFIG.duration_ms,
            CONFIG.show_on_top,
            CONFIG.allow_glass_blur,
            action_text,
            CONFIG.action_type
        );

        host_show_bulletin(response.as_ptr(), response.len());

        if CONFIG.vibrate {
            host_vibrate(25); // 25ms tactile haptic
        }
    }

    0
}

#[no_mangle]
pub extern "C" fn on_action_clicked(dialog_id: i64, message_id: i32, action_id: u32) {
    unsafe {
        match action_id {
            1 => host_mark_read(dialog_id, message_id),
            _ => host_open_chat(dialog_id, message_id),
        }
    }
}

// Fast string & JSON helpers
fn extract_str(json: &str, key: &str) -> Option<String> {
    let pattern = format!("\"{}\":\"", key);
    if let Some(start) = json.find(&pattern) {
        let val_start = start + pattern.len();
        if let Some(end) = json[val_start..].find('"') {
            return Some(json[val_start..val_start + end].to_string());
        }
    }
    None
}

fn extract_i64(json: &str, key: &str) -> Option<i64> {
    let pattern = format!("\"{}\":", key);
    if let Some(start) = json.find(&pattern) {
        let val_start = start + pattern.len();
        let s = json[val_start..].trim_start();
        let end = s.find(|c: char| !c.is_numeric() && c != '-').unwrap_or(s.len());
        return s[..end].parse().ok();
    }
    None
}

fn extract_i32(json: &str, key: &str) -> Option<i32> {
    extract_i64(json, key).map(|v| v as i32)
}

fn extract_bool(json: &str, key: &str) -> Option<bool> {
    let pattern = format!("\"{}\":", key);
    if let Some(start) = json.find(&pattern) {
        let s = json[start + pattern.len()..].trim_start();
        if s.starts_with("true") {
            return Some(true);
        } else if s.starts_with("false") {
            return Some(false);
        }
    }
    None
}

fn escape_json(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            _ => out.push(c),
        }
    }
    out
}
