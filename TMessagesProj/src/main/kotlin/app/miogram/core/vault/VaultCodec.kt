package app.miogram.core.vault

import app.miogram.core.crypto.AesGcm
import app.miogram.core.crypto.MiogramKdf
import java.nio.ByteBuffer

/**
 * Strict bounds-checked binary codec for vault payloads (the plaintext that
 * gets enveloped by [AesGcm] before hitting storage). Any deviation from the
 * layout throws instead of being skipped: stored metadata is trusted config,
 * silent truncation would be an anti-forensics hole.
 */
object VaultCodec {

    const val VERSION_1: Short = 1

    private val MAGIC = byteArrayOf(0x4D, 0x49, 0x47, 0x56) // "MIGV"

    private const val MAX_PROFILES = 8
    private const val MAX_SECRETS_PER_PROFILE = 16
    private const val MAX_ID_LENGTH = 64
    private const val MAX_LABEL_LENGTH = 128
    private const val MAX_NAMESPACE_LENGTH = 64
    private const val MAX_SEALED_LENGTH = 4096
    private const val MAX_SALT_TAG_LENGTH = 128

    fun encode(metadata: VaultMetadata): ByteArray {
        if (metadata.profiles.isEmpty() || metadata.profiles.size > MAX_PROFILES) {
            throw VaultFormatException("invalid profile count: ${metadata.profiles.size}")
        }
        var size = MAGIC.size + 2 + 2
        for (p in metadata.profiles) {
            validateStrings(p)
            size += 1 + 1 + p.id.toByteArray(Charsets.UTF_8).size
            size += 1 + p.label.toByteArray(Charsets.UTF_8).size
            size += 4 + 4 + 4
            size += 1 + p.verifier.salt.size
            size += 1 + p.verifier.checkTag.size
            size += 2
            for ((ns, sealed) in p.wrappedSecrets) {
                val nsBytes = ns.toByteArray(Charsets.UTF_8)
                if (nsBytes.isEmpty() || nsBytes.size > MAX_NAMESPACE_LENGTH) {
                    throw VaultFormatException("namespace length out of range: ${nsBytes.size}")
                }
                if (sealed.isEmpty() || sealed.size > MAX_SEALED_LENGTH) {
                    throw VaultFormatException("sealed length out of range: ${sealed.size}")
                }
                size += 1 + nsBytes.size + 2 + sealed.size
            }
        }

        val buffer = ByteBuffer.allocate(size)
        buffer.put(MAGIC)
        buffer.putShort(VERSION_1)
        buffer.putShort(metadata.profiles.size.toShort())

        for (p in metadata.profiles) {
            buffer.put(p.kind.wireId.toByte())
            writeLengthPrefixed(buffer, p.id.toByteArray(Charsets.UTF_8), MAX_ID_LENGTH)
            writeLengthPrefixed(buffer, p.label.toByteArray(Charsets.UTF_8), MAX_LABEL_LENGTH)
            buffer.putInt(p.verifier.params.memoryKiB)
            buffer.putInt(p.verifier.params.iterations)
            buffer.putInt(p.verifier.params.parallelism)
            writeLengthPrefixed(buffer, p.verifier.salt, MAX_SALT_TAG_LENGTH)
            writeLengthPrefixed(buffer, p.verifier.checkTag, MAX_SALT_TAG_LENGTH)
            buffer.putShort(p.wrappedSecrets.size.toShort())
            for ((ns, sealed) in p.wrappedSecrets) {
                writeLengthPrefixed(buffer, ns.toByteArray(Charsets.UTF_8), MAX_NAMESPACE_LENGTH)
                writeU16Prefixed(buffer, sealed, MAX_SEALED_LENGTH)
            }
        }

        check(!buffer.hasRemaining()) { "codec size mismatch" }
        return buffer.array()
    }

    fun decode(payload: ByteArray): VaultMetadata {
        val buffer = try {
            ByteBuffer.wrap(payload)
        } catch (e: Exception) {
            throw VaultFormatException("wrap failed", e)
        }
        if (payload.size < MAGIC.size + 4) throw VaultFormatException("payload too short")
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC)) throw VaultFormatException("bad magic")

        val version = buffer.short
        if (version != VERSION_1) throw VaultFormatException("unsupported version $version")

        val profileCount = buffer.short.toInt()
        if (profileCount <= 0 || profileCount > MAX_PROFILES) {
            throw VaultFormatException("invalid profile count: $profileCount")
        }

        val profiles = ArrayList<ProfileRecord>(profileCount)
        repeat(profileCount) {
            val kind = ProfileKind.fromWire(buffer.get().toInt())
            val id = String(readLengthPrefixed(buffer, MAX_ID_LENGTH, "id"), Charsets.UTF_8)
            val label = String(readLengthPrefixed(buffer, MAX_LABEL_LENGTH, "label"), Charsets.UTF_8)

            val memKiB = buffer.int
            val iterations = buffer.int
            val parallelism = buffer.int
            val params = try {
                MiogramKdf.Params(memKiB, iterations, parallelism)
            } catch (e: IllegalArgumentException) {
                throw VaultFormatException("invalid kdf params", e)
            }

            val salt = readLengthPrefixed(buffer, MAX_SALT_TAG_LENGTH, "salt")
            val tag = readLengthPrefixed(buffer, MAX_SALT_TAG_LENGTH, "checkTag")
            val verifier = PasscodeVerifierSpec(salt.copyOf(), params, tag.copyOf())

            val secretCount = buffer.short.toInt()
            if (secretCount < 0 || secretCount > MAX_SECRETS_PER_PROFILE) {
                throw VaultFormatException("invalid secret count: $secretCount")
            }
            var record = ProfileRecord(id, kind, label, verifier)
            repeat(secretCount) {
                val ns = String(readLengthPrefixed(buffer, MAX_NAMESPACE_LENGTH, "namespace"), Charsets.UTF_8)
                val sealed = readU16Prefixed(buffer, MAX_SEALED_LENGTH, "sealed:$ns")
                record = record.withWrappedSecret(ns, sealed)
            }
            profiles.add(record)
        }

        if (buffer.hasRemaining()) throw VaultFormatException("trailing bytes: ${buffer.remaining()}")
        return VaultMetadata(profiles)
    }

    private fun validateStrings(p: ProfileRecord) {
        if (p.id.isEmpty() || p.id.length > MAX_ID_LENGTH) {
            throw VaultFormatException("profile id length out of range: ${p.id.length}")
        }
        if (p.label.length > MAX_LABEL_LENGTH) {
            throw VaultFormatException("profile label too long: ${p.label.length}")
        }
        if (p.verifier.salt.isEmpty() || p.verifier.salt.size > MAX_SALT_TAG_LENGTH ||
            p.verifier.checkTag.isEmpty() || p.verifier.checkTag.size > MAX_SALT_TAG_LENGTH
        ) {
            throw VaultFormatException("verifier field length out of range")
        }
    }

    private fun writeLengthPrefixed(buffer: ByteBuffer, bytes: ByteArray, max: Int) {
        if (bytes.size > max || bytes.size > Byte.MAX_VALUE) {
            throw VaultFormatException("field exceeds $max bytes: ${bytes.size}")
        }
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
    }

    private fun writeU16Prefixed(buffer: ByteBuffer, bytes: ByteArray, max: Int) {
        if (bytes.size > max || bytes.size > UShort.MAX_VALUE.toInt()) {
            throw VaultFormatException("field exceeds $max bytes: ${bytes.size}")
        }
        buffer.putShort(bytes.size.toShort())
        buffer.put(bytes)
    }

    private fun readLengthPrefixed(buffer: ByteBuffer, max: Int, field: String): ByteArray {
        val length = buffer.get().toInt() and 0xFF
        return readExact(buffer, length, max, field)
    }

    private fun readU16Prefixed(buffer: ByteBuffer, max: Int, field: String): ByteArray {
        val length = buffer.short.toInt() and 0xFFFF
        return readExact(buffer, length, max, field)
    }

    private fun readExact(buffer: ByteBuffer, length: Int, max: Int, field: String): ByteArray {
        if (length > max) throw VaultFormatException("$field length out of range: $length")
        if (buffer.remaining() < length) throw VaultFormatException("$field truncated")
        val out = ByteArray(length)
        buffer.get(out)
        return out
    }
}
