package app.miogram.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.CharBuffer
import java.security.SecureRandom

/**
 * Argon2id KDF (RFC 9106) with explicit cost profiles.
 *
 * Output layout contract used across Miogram:
 * `[ check_tag(32B) | wrapping_key(32B) ]` for a 64-byte derivation,
 * so verifier comparison and key wrapping derive from a single KDF call.
 */
class MiogramKdf(private val random: SecureRandom = SecureRandom()) {

    data class Params(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
        init {
            require(memoryKiB in MIN_MEMORY_KIB..MAX_MEMORY_KIB) { "memoryKiB out of range: $memoryKiB" }
            require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "iterations out of range: $iterations" }
            require(parallelism in MIN_PARALLELISM..MAX_PARALLELISM) { "parallelism out of range: $parallelism" }
        }

        companion object {
            const val MIN_MEMORY_KIB = 256
            const val MAX_MEMORY_KIB = 1 shl 20
            const val MIN_ITERATIONS = 1
            const val MAX_ITERATIONS = 64
            const val MIN_PARALLELISM = 1
            const val MAX_PARALLELISM = 64

            val INTERACTIVE = Params(16 * 1024, 2, 1)
            val STANDARD = Params(32 * 1024, 3, 1)
            val PARANOID = Params(64 * 1024, 4, 1)
            /** For unit tests only; never persist verifiers using this profile. */
            val TEST_FAST = Params(1024, 1, 1)
        }
    }

    /**
     * Derived material. [checkTag] is safe to persist inside a verifier record;
     * the wrapping key must be zeroized after use.
     */
    class Result internal constructor(raw: ByteArray) : AutoCloseable {
        val checkTag: ByteArray = raw.copyOfRange(0, CHECK_TAG_LENGTH)
        private val wrappingKeyBytes: ByteArray =
            raw.copyOfRange(CHECK_TAG_LENGTH, CHECK_TAG_LENGTH + WRAPPING_KEY_LENGTH)

        fun wrappingKey(): ByteArray = wrappingKeyBytes.copyOf()

        /**
         * Runs [block] with direct access to the internal wrapping key buffer,
         * zeroizing it afterwards regardless of outcome.
         */
        fun <T> useWrappingKey(block: (checkTag: ByteArray, wrappingKey: ByteArray) -> T): T {
            try {
                return block(checkTag, wrappingKeyBytes)
            } finally {
                wrappingKeyBytes.fill(0)
            }
        }

        override fun close() {
            wrappingKeyBytes.fill(0)
        }
    }

    fun newSalt(length: Int = SALT_LENGTH): ByteArray {
        require(length in 8..128) { "salt length out of range: $length" }
        return ByteArray(length).also(random::nextBytes)
    }

    /**
     * Derives [outputLength] bytes. Must be called off the main thread;
     * STANDARD profile costs ~32 MiB and ~100ms on mid-range hardware.
     */
    fun derive(passphrase: ByteArray, salt: ByteArray, params: Params, outputLength: Int = DERIVED_LENGTH): Result {
        require(passphrase.isNotEmpty()) { "passphrase must not be empty" }
        require(salt.size in 8..128) { "salt length out of range: ${salt.size}" }
        require(outputLength >= CHECK_TAG_LENGTH + WRAPPING_KEY_LENGTH) {
            "outputLength too small: $outputLength"
        }
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withIterations(params.iterations)
            .withMemoryAsKB(params.memoryKiB)
            .withParallelism(params.parallelism)
        val generator = Argon2BytesGenerator()
        generator.init(builder.build())
        val out = ByteArray(outputLength)
        generator.generateBytes(passphrase, out)
        return Result(out)
    }

    fun deriveChar(passphrase: CharArray, salt: ByteArray, params: Params, outputLength: Int = DERIVED_LENGTH): Result {
        require(passphrase.isNotEmpty()) { "passphrase must not be empty" }
        val bytes = passphrase.toUtf8ZeroizingSource()
        try {
            return derive(bytes, salt, params, outputLength)
        } finally {
            bytes.fill(0)
        }
    }

    private fun CharArray.toUtf8ZeroizingSource(): ByteArray {
        val buffer = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(this))
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        java.util.Arrays.fill(buffer.array(), 0.toByte())
        return bytes
    }

    companion object {
        const val SALT_LENGTH = 16
        const val CHECK_TAG_LENGTH = 32
        const val WRAPPING_KEY_LENGTH = 32
        const val DERIVED_LENGTH = CHECK_TAG_LENGTH + WRAPPING_KEY_LENGTH
    }
}
