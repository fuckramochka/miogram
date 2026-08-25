package app.miogram.core.plugins

import app.miogram.core.plugins.PluginCapability
import java.nio.ByteBuffer

/**
 * Strict, bounds-checked binary codec for signed plugin manifests.
 *
 * Wire layout:
 * ```
 * "HYPE" | u16 version=1
 * | u8 idLen + id(utf8)
 * | u32 versionCode
 * | u8 nameLen + name(utf8)
 * | u16 capCount + per-capability u8 wireId (ascending, unique)
 * | u64 codeSize | codeSha256(32)
 * | u8 keyIdLen + keyId(ascii)
 * | u32 sigLen + Ed25519 signature over every preceding byte
 * ```
 * The codec never "repairs" input: any deviation throws instead of being
 * skipped, because manifests gate arbitrary native execution.
 */
object PluginManifestCodec {

    const val VERSION_1: Short = 1

    private val MAGIC = byteArrayOf(0x48, 0x59, 0x50, 0x45) // "HYPE"
    private const val MAX_ID = 64
    private const val MAX_NAME = 128
    private const val MAX_KEY_ID = 64
    private val MAX_CAPABILITIES = PluginCapability.entries.size
    private const val ED25519_SIG_LENGTH = 64
    private const val SHA256_LENGTH = 32
    private const val MAX_CODE_SIZE = 64L * 1024 * 1024

    /** Manifest bytes with the signature field excluded — the signing range. */
    class Decoded(
        val manifest: PluginManifest,
        val signature: ByteArray,
        val signedBytes: ByteArray,
    )

    fun encodeUnsigned(manifest: PluginManifest): ByteArray {
        val idBytes = manifest.pluginId.toByteArray(Charsets.UTF_8)
        val nameBytes = manifest.displayName.toByteArray(Charsets.UTF_8)
        val keyIdBytes = manifest.signerKeyId.toByteArray(Charsets.US_ASCII)
        validate(idBytes.size in 1..MAX_ID, "pluginId length")
        validate(nameBytes.size <= MAX_NAME, "displayName length")
        validate(keyIdBytes.size in 1..MAX_KEY_ID, "signerKeyId length")
        validate(manifest.codeSha256.size == SHA256_LENGTH, "codeSha256 length")
        validate(manifest.codeSize in 1..MAX_CODE_SIZE, "codeSize")

        val caps = manifest.capabilities.sortedBy(PluginCapability::wireId).distinct()
        var size = MAGIC.size + 2
        size += 1 + idBytes.size
        size += 4
        size += 1 + nameBytes.size
        size += 2 + caps.size
        size += 8 + SHA256_LENGTH
        size += 1 + keyIdBytes.size

        val buffer = ByteBuffer.allocate(size)
        buffer.put(MAGIC)
        buffer.putShort(VERSION_1)
        writeU8Prefixed(buffer, idBytes)
        buffer.putInt(manifest.versionCode)
        writeU8Prefixed(buffer, nameBytes)
        buffer.putShort(caps.size.toShort())
        for (cap in caps) buffer.put(cap.wireId.toByte())
        buffer.putLong(manifest.codeSize)
        buffer.put(manifest.codeSha256)
        writeU8Prefixed(buffer, keyIdBytes)

        check(!buffer.hasRemaining()) { "codec size mismatch" }
        return buffer.array()
    }

    fun attachSignature(unsignedPayload: ByteArray, signature: ByteArray): ByteArray {
        validate(signature.size == ED25519_SIG_LENGTH, "signature length")
        val out = unsignedPayload.copyOf(unsignedPayload.size + 4 + signature.size)
        var offset = unsignedPayload.size
        writeU32(out, offset, signature.size)
        offset += 4
        System.arraycopy(signature, 0, out, offset, signature.size)
        return out
    }

    fun decode(bytes: ByteArray): Decoded {
        val buffer = ByteBuffer.wrap(bytes)
        if (bytes.size < MAGIC.size + 2) throw PluginFormatException("manifest too short")
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC)) throw PluginFormatException("bad magic")
        when (val version = buffer.short) {
            VERSION_1 -> {}
            else -> throw PluginFormatException("unsupported manifest version $version")
        }

        val id = String(readU8Prefixed(buffer, MAX_ID, "id"), Charsets.UTF_8)
        val versionCode = buffer.int
        if (versionCode < 1) throw PluginFormatException("invalid versionCode $versionCode")
        val name = String(readU8Prefixed(buffer, MAX_NAME, "name"), Charsets.UTF_8)

        val capCount = buffer.short.toInt()
        validate(capCount in 0..MAX_CAPABILITIES, "capability count")
        var previousWire = -1
        val capabilities = HashSet<PluginCapability>(capCount)
        repeat(capCount) {
            val wire = buffer.get().toInt() and 0xFF
            if (wire <= previousWire) throw PluginFormatException("capabilities not ascending/unique")
            previousWire = wire
            capabilities.add(PluginCapability.fromWire(wire))
        }

        val codeSize = buffer.long
        validate(codeSize in 1..MAX_CODE_SIZE, "codeSize")
        val codeHash = readExact(buffer, SHA256_LENGTH, "codeSha256")

        val keyId = String(readU8Prefixed(buffer, MAX_KEY_ID, "keyId"), Charsets.US_ASCII)

        val signedEnd = buffer.position()
        if (!buffer.hasRemaining()) throw PluginFormatException("signature missing")
        val sigLen = readU32(bytes, signedEnd)
        validate(sigLen == ED25519_SIG_LENGTH, "signature length")
        if (bytes.size - signedEnd - 4 != sigLen) throw PluginFormatException("trailing bytes after signature")
        val signature = bytes.copyOfRange(signedEnd + 4, bytes.size)

        return Decoded(
            manifest = PluginManifest(
                pluginId = id,
                versionCode = versionCode,
                displayName = name,
                capabilities = capabilities,
                codeSize = codeSize,
                codeSha256 = codeHash,
                signerKeyId = keyId,
            ),
            signature = signature,
            signedBytes = bytes.copyOfRange(0, signedEnd),
        )
    }

    private fun validate(condition: Boolean, what: String) {
        if (!condition) throw PluginFormatException("$what out of range")
    }

    private fun writeU8Prefixed(buffer: ByteBuffer, bytes: ByteArray) {
        if (bytes.size > 0xFF) throw PluginFormatException("field exceeds 255 bytes")
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
    }

    private fun readU8Prefixed(buffer: ByteBuffer, max: Int, field: String): ByteArray {
        val length = buffer.get().toInt() and 0xFF
        return readExact(buffer, length, field, max)
    }

    private fun readExact(buffer: ByteBuffer, length: Int, field: String, max: Int = Int.MAX_VALUE): ByteArray {
        if (length > max) throw PluginFormatException("$field length out of range: $length")
        if (buffer.remaining() < length) throw PluginFormatException("$field truncated")
        return ByteArray(length).also(buffer::get)
    }

    private fun writeU32(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun readU32(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xFF) shl 24) or
                ((source[offset + 1].toInt() and 0xFF) shl 16) or
                ((source[offset + 2].toInt() and 0xFF) shl 8) or
                (source[offset + 3].toInt() and 0xFF)
}
