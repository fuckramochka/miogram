//! Wire envelope between the Miogram host and plugin code.
//!
//! The C bridge treats the byte blob as opaque; both sides agree on this
//! layout. Deliberately NOT FlatBuffers for v1: zero dependencies inside the
//! guest keeps wasm binaries tiny. `host_api.fbs` remains the target format
//! once generated bindings are adopted — the envelope is trivially
//! transcodable.
//!
//! Layout (little-endian):
//! ```text
//! 0..4   magic b"MIOG"
//! 4      version = 1
//! 5..8   reserved (zeros)
//! 8..12  op_len: u32
//! 12..12+op_len   op: UTF-8
//! 12+op_len..     payload bytes
//! ```

pub const MAGIC: [u8; 4] = *b"MIOG";
pub const VERSION: u8 = 1;

const HEADER_LEN: usize = 12;
const MAX_OP_LEN: u32 = 128;
const MAX_TOTAL: u32 = 4 * 1024 * 1024;

#[derive(Debug, PartialEq)]
pub struct Frame<'a> {
    pub op: &'a str,
    pub payload: &'a [u8],
}

#[derive(Debug)]
pub enum EnvelopeError {
    TooShort,
    BadMagic,
    BadVersion(u8),
    ReservedNonZero,
    OpTooLong(u32),
    OpNotUtf8,
    Truncated,
    TotalTooLarge(u32),
}

/// Parses a request frame received from the host.
pub fn decode(buf: &[u8]) -> Result<Frame<'_>, EnvelopeError> {
    if buf.len() < HEADER_LEN {
        return Err(EnvelopeError::TooShort);
    }
    if buf[0..4] != MAGIC {
        return Err(EnvelopeError::BadMagic);
    }
    if buf[4] != VERSION {
        return Err(EnvelopeError::BadVersion(buf[4]));
    }
    if buf[5..8].iter().any(|&b| b != 0) {
        return Err(EnvelopeError::ReservedNonZero);
    }
    let op_len = u32::from_le_bytes([buf[8], buf[9], buf[10], buf[11]]);
    if op_len > MAX_OP_LEN {
        return Err(EnvelopeError::OpTooLong(op_len));
    }
    let total = HEADER_LEN as u32 + op_len + payload_len_hint(buf)?;
    if total > MAX_TOTAL {
        return Err(EnvelopeError::TotalTooLarge(total));
    }
    let op_end = HEADER_LEN + op_len as usize;
    if buf.len() < op_end {
        return Err(EnvelopeError::Truncated);
    }
    let op = std::str::from_utf8(&buf[HEADER_LEN..op_end]).map_err(|_| EnvelopeError::OpNotUtf8)?;
    Ok(Frame { op, payload: &buf[op_end..] })
}

fn payload_len_hint(_buf: &[u8]) -> Result<u32, EnvelopeError> {
    // Payload length is implicit ("rest of the buffer"); kept as a hook so a
    // length-prefixed variant can slot in without breaking callers.
    Ok(0)
}

/// Encodes a response frame. Owned buffer is returned to the host via
/// `Box::into_raw` by [`crate::miogram_call`] trampoline.
pub fn encode(op: &str, payload: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(HEADER_LEN + op.len() + payload.len());
    out.extend_from_slice(&MAGIC);
    out.push(VERSION);
    out.extend_from_slice(&[0, 0, 0]);
    out.extend_from_slice(&(op.len() as u32).to_le_bytes());
    out.extend_from_slice(op.as_bytes());
    out.extend_from_slice(payload);
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn roundtrip() {
        let frame = encode("on_message_receive", b"hello");
        let decoded = decode(&frame).expect("valid");
        assert_eq!(decoded.op, "on_message_receive");
        assert_eq!(decoded.payload, b"hello");
    }

    #[test]
    fn empty_payload_ok() {
        let frame = encode("ping", &[]);
        let decoded = decode(&frame).expect("valid");
        assert_eq!(decoded.op, "ping");
        assert!(decoded.payload.is_empty());
    }

    #[test]
    fn bad_magic_rejected() {
        let mut frame = encode("x", &[]);
        frame[0] = b'X';
        assert!(matches!(decode(&frame), Err(EnvelopeError::BadMagic)));
    }

    #[test]
    fn too_short_rejected() {
        let frame = encode("some_op", b"data");
        assert!(matches!(decode(&frame[..8]), Err(EnvelopeError::TooShort)));
    }

    #[test]
    fn truncated_rejected() {
        let frame = encode("some_op", b"data");
        // HEADER_LEN is 12, "some_op" is 7 bytes -> op_end is 19.
        // Slice of length 15 is >= HEADER_LEN but < op_end, so it must return Truncated.
        assert!(matches!(decode(&frame[..15]), Err(EnvelopeError::Truncated)));
    }

    #[test]
    fn wrong_version_rejected() {
        let mut frame = encode("x", &[]);
        frame[4] = 9;
        assert!(matches!(decode(&frame), Err(EnvelopeError::BadVersion(9))));
    }

    #[test]
    fn multibyte_op_roundtrip() {
        let frame = encode("операция", &[1, 2, 3]);
        let decoded = decode(&frame).expect("valid");
        assert_eq!(decoded.op, "операция");
    }
}
