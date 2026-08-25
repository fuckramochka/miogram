package app.miogram.core.vault

import app.miogram.core.crypto.MiogramKdf
import app.miogram.core.crypto.KeyMaterial

enum class ProfileKind(val wireId: Int) {
    REAL(1), DECOY(2);

    companion object {
        fun fromWire(wireId: Int): ProfileKind =
            entries.firstOrNull { it.wireId == wireId } ?: throw VaultFormatException("unknown profile kind $wireId")
    }
}

/**
 * Persistable proof of passcode knowledge: Argon2id check-tag plus the exact
 * cost parameters needed to recompute it. Never contains the wrapping key.
 */
class PasscodeVerifierSpec(
    val salt: ByteArray,
    val params: MiogramKdf.Params,
    val checkTag: ByteArray,
)

/**
 * Immutable profile record. Mutation goes through [copy]-style helpers so the
 * vault cannot accidentally alias mutable state into persisted snapshots.
 */
class ProfileRecord(
    val id: String,
    val kind: ProfileKind,
    val label: String,
    val verifier: PasscodeVerifierSpec,
    /** Namespace -> AES-GCM-sealed secret, bound to this profile id via AAD. */
    val wrappedSecrets: Map<String, ByteArray> = emptyMap(),
) {
    fun withWrappedSecret(namespace: String, sealed: ByteArray): ProfileRecord {
        val next = LinkedHashMap(wrappedSecrets)
        next[namespace] = sealed.copyOf()
        return ProfileRecord(id, kind, label, verifier, next)
    }

    fun withoutSecrets(): ProfileRecord = ProfileRecord(id, kind, label, verifier, emptyMap())

    fun withVerifier(verifier: PasscodeVerifierSpec): ProfileRecord =
        ProfileRecord(id, kind, label, verifier, wrappedSecrets)
}

class VaultMetadata(val profiles: List<ProfileRecord>) {
    fun findReal(): ProfileRecord? = profiles.firstOrNull { it.kind == ProfileKind.REAL }

    fun decoys(): List<ProfileRecord> = profiles.filter { it.kind == ProfileKind.DECOY }
}

sealed class UnlockResult {
    /**
     * Both REAL and DECOY unlocks surface as Success — callers branch on
     * [profile].kind, keeping the mechanical behaviour of a duress entry
     * identical to a normal one. Key material stays owned by the vault
     * session; access derived secrets via ProfileVault.ensureWrappedSecret.
     */
    class Success(val profile: ProfileRecord) : UnlockResult() {
        override fun release() = Unit
    }

    data object Denied : UnlockResult() {
        override fun release() = Unit
    }

    abstract fun release()
}

class VaultFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

class VaultStateException(message: String) : IllegalStateException(message)
