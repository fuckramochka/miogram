//! Minimal echo plugin: returns the request payload back inside the response
//! frame. Serves as the golden sample for SDK consumers and for host-side
//! integration tests once device builds are available.

use miogram_plugin_sdk::{envelope, Plugin};

#[derive(Default)]
struct Echo;

impl Plugin for Echo {
    fn handle(&mut self, op: &str, payload: &[u8]) -> Result<Vec<u8>, i32> {
        match op {
            "echo" => Ok(envelope::encode("echo", payload)),
            "ping" => Ok(envelope::encode("ping", b"pong")),
            _ => Err(1), // ERR_BAD_FRAME semantics: unknown op
        }
    }
}

miogram_plugin_sdk::register!(Echo);

// Host-target stub: real plugins are built as cdylib for wasm32, where the
// entry point is hyperion_call and a main function is meaningless.
#[allow(dead_code)]
fn main() {}
