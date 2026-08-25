package app.miogram.core.vault

import app.miogram.core.crypto.AesGcm
import app.miogram.core.crypto.MiogramKdf
import app.miogram.core.crypto.KeyMaterial
import app.miogram.core.crypto.MetadataCipher
import app.miogram.core.crypto.Secrets
import java.security.SecureRandom
import java.util.UUID

/**
 * Multi-profile passcode vault with duress semantics.
 *
 * Key model (see docs/miogram/ARCHITECTURE.md §2):
 * ```
 * Argon2id(pin, salt, params) -> [ check_tag | wrapping_key ]
 * wrapping_key --AES-GCM--> master_secret(32B) --AES-GCM--> derived secrets
 * ```
 *
 * When at least one DECOY profile exists, [unlock] always evaluates the KDF
 * for every registered profile before branching, so a duress entry costs the
 * same as a regular one and leaks no profile count through timing. A decoy
 * success never touches real-profile key material because that material is
 * simply never unwrapped.
 *
 * Not thread-safe: serialize access externally (the bridge layer guards this
 * with a single-threaded dispatcher).
 */
class ProfileVault(
    private val metadataRepo: BlobRepository,
    private val metadataCipher: MetadataCipher,
    private val kdf: MiogramKdf = MiogramKdf(),
) {

    private val random = SecureRandom()
    private var session: Session? = null

    private class Session(var record: ProfileRecord, val masterSecret: KeyMaterial) : AutoCloseable {
        override fun close() = masterSecret.close()
    }

    fun isInitialized(): Boolean = metadataRepo.read() != null

    fun hasDecoyProfiles(): Boolean = loadMetadata().decoys().isNotEmpty()

    /** Creates the REAL profile. Fails if any metadata already exists. */
    fun initialize(realLabel: String, realPin: CharArray, params: MiogramKdf.Params = DEFAULT_PARAMS) {
        if (metadataRepo.read() != null) throw VaultStateException("vault already initialized")
        val real = createProfile(ProfileKind.REAL, realLabel.ifBlank { DEFAULT_REAL_LABEL }, realPin, params)
        persist(VaultMetadata(listOf(real)))
    }

    /** Adds an additional DECOY profile revealed by its own duress passcode. */
    fun addDecoyProfile(label: String, duressPin: CharArray, params: MiogramKdf.Params = DEFAULT_PARAMS) {
        val metadata = loadMetadata()
        if (metadata.findReal() == null) throw VaultStateException("vault not initialized")
        if (metadata.profiles.size >= MAX_PROFILES) throw VaultStateException("profile limit reached")
        val decoy = createProfile(ProfileKind.DECOY, label.ifBlank { DEFAULT_DECOY_LABEL }, duressPin, params)
        persist(VaultMetadata(metadata.profiles + decoy))
    }

    /**
     * Verifies [pin] against every registered profile — REAL first, then
     * decoys in registration order — before branching, then opens a session
     * for the first match. Any previously open session (including a real one,
     * when a duress pin is entered) is released before returning.
     */
    fun unlock(pin: CharArray): UnlockResult {
        require(pin.isNotEmpty()) { "pin must not be empty" }
        val metadata = loadMetadata()
        val candidates = buildList(metadata.profiles.size) {
            metadata.findReal()?.let(::add)
            addAll(metadata.decoys())
        }
        if (candidates.isEmpty()) throw VaultStateException("vault not initialized")

        var matchedRecord: ProfileRecord? = null
        var matchedMaster: ByteArray? = null

        for (record in candidates) {
            kdf.deriveChar(pin, record.verifier.salt, record.verifier.params).useWrappingKey { tag, wrappingKey ->
                if (!Secrets.constantTimeEquals(tag, record.verifier.checkTag)) return@useWrappingKey
                val sealed = record.wrappedSecrets[NAMESPACE_MASTER]
                    ?: throw VaultFormatException("profile ${record.id} missing master secret")
                try {
                    matchedMaster = AesGcm.decrypt(wrappingKey, sealed, masterAad(record.id))
                    matchedRecord = record
                } catch (_: AesGcm.SealedBoxException) {
                    throw VaultFormatException("profile ${record.id} master secret failed authentication")
                }
            }
        }

        val record = matchedRecord ?: return UnlockResult.Denied
        val master = matchedMaster ?: return UnlockResult.Denied

        closeSessionLocked()
        // Ownership of `master` transfers to the new session; do not zeroize here.
        session = Session(record, KeyMaterial.of(master))
        return UnlockResult.Success(record)
    }

    /** Releases all session key material held in RAM. Idempotent. */
    fun lock() {
        closeSessionLocked()
    }

    fun activeProfile(): ProfileRecord? = session?.record

    fun isActiveSessionReal(): Boolean = session?.record?.kind == ProfileKind.REAL

    /**
     * Returns the secret bound to [namespace] under the open profile,
     * generating and persisting it on first access. The returned handle is an
     * independent copy owned by the caller and must be closed after use.
     */
    fun ensureWrappedSecret(namespace: String, length: Int = SECRET_LENGTH): KeyMaterial {
        validateNamespace(namespace)
        require(length in 16..512) { "secret length out of range: $length" }
        val s = session ?: throw VaultStateException("vault is locked")
        val existing = s.record.wrappedSecrets[namespace]
        if (existing != null) {
            return KeyMaterial.of(unwrapWithSession(s, namespace, existing))
        }
        val fresh = ByteArray(length).also(random::nextBytes)
        val sealed = wrapWithSession(s, namespace, fresh)
        Secrets.zeroize(fresh)
        val updatedRecord = s.record.withWrappedSecret(namespace, sealed)
        replaceProfile(updatedRecord)
        s.record = updatedRecord
        return KeyMaterial.of(unwrapWithSession(s, namespace, sealed))
    }

    /**
     * Re-keys every profile. Passing `null` for [newDuressPin] removes all
     * decoy profiles. Re-keying existing decoys requires [currentDuressPin]
     * matching every one of them — without it their wrapped secrets cannot be
     * decrypted and preserved.
     */
    fun changePasscodes(
        currentRealPin: CharArray,
        newRealPin: CharArray,
        newDuressPin: CharArray?,
        currentDuressPin: CharArray? = null,
    ) {
        require(newRealPin.isNotEmpty()) { "real pin must not be empty" }
        if (newDuressPin != null && newDuressPin.isEmpty()) {
            throw IllegalArgumentException("duress pin must not be empty when provided")
        }
        val metadata = loadMetadata()
        val real = metadata.findReal() ?: throw VaultStateException("vault not initialized")
        verifyAgainst(real, currentRealPin)

        val nextProfiles = ArrayList<ProfileRecord>(metadata.profiles.size)
        nextProfiles.add(rewrapProfile(real, currentRealPin, newRealPin))

        if (newDuressPin != null) {
            val decoys = metadata.decoys()
            if (decoys.isEmpty()) {
                nextProfiles.add(createProfile(ProfileKind.DECOY, DEFAULT_DECOY_LABEL, newDuressPin, DEFAULT_PARAMS))
            } else {
                val currentDuress = currentDuressPin
                    ?: throw VaultStateException("current duress passcode is required to re-key decoy profiles")
                for (decoy in decoys) {
                    verifyAgainst(decoy, currentDuress)
                }
                for (decoy in decoys) {
                    nextProfiles.add(rewrapProfile(decoy, currentDuress, newDuressPin))
                }
            }
        }

        val activeId = session?.record?.id
        persist(VaultMetadata(nextProfiles))

        if (activeId == null) return
        val refreshed = nextProfiles.firstOrNull { it.id == activeId }
        val pinForProfile = when {
            refreshed == null -> null
            refreshed.kind == ProfileKind.REAL -> newRealPin
            else -> newDuressPin
        }
        if (refreshed == null || pinForProfile == null) {
            lock()
        } else {
            reopenSession(refreshed, pinForProfile)
        }
    }

    /** Anti-forensics panic path: destroys persisted metadata and RAM state. */
    fun wipeAll() {
        metadataRepo.delete()
        closeSessionLocked()
    }

    private fun reopenSession(record: ProfileRecord, pin: CharArray) {
        val master = unwrapMasterFor(record, pin)
        closeSessionLocked()
        session = Session(record, KeyMaterial.of(master))
    }

    private fun unwrapMasterFor(record: ProfileRecord, pin: CharArray): ByteArray =
        kdf.deriveChar(pin, record.verifier.salt, record.verifier.params).useWrappingKey { tag, wrappingKey ->
            if (!Secrets.constantTimeEquals(tag, record.verifier.checkTag)) {
                throw VaultStateException("passcode rejected")
            }
            val sealed = record.wrappedSecrets[NAMESPACE_MASTER]
                ?: throw VaultFormatException("profile ${record.id} missing master secret")
            try {
                AesGcm.decrypt(wrappingKey, sealed, masterAad(record.id))
            } catch (e: AesGcm.SealedBoxException) {
                throw VaultFormatException("master secret failed authentication", e)
            }
        }

    private fun verifyAgainst(record: ProfileRecord, pin: CharArray) {
        kdf.deriveChar(pin, record.verifier.salt, record.verifier.params).useWrappingKey { tag, _ ->
            if (!Secrets.constantTimeEquals(tag, record.verifier.checkTag)) {
                throw VaultStateException("current passcode rejected")
            }
        }
    }

    /**
     * Unwraps the profile master secret under [unwrapPin] and re-seals it
     * under a freshly derived key for [sealPin], rotating salt and verifier.
     */
    private fun rewrapProfile(record: ProfileRecord, unwrapPin: CharArray, sealPin: CharArray): ProfileRecord {
        val master = unwrapMasterFor(record, unwrapPin)
        try {
            val salt = kdf.newSalt()
            return kdf.deriveChar(sealPin, salt, record.verifier.params).useWrappingKey { tag, wrappingKey ->
                val newSealed = AesGcm.encrypt(wrappingKey, master, masterAad(record.id))
                record.withVerifier(PasscodeVerifierSpec(salt, record.verifier.params, tag.copyOf()))
                    .withWrappedSecret(NAMESPACE_MASTER, newSealed)
            }
        } finally {
            Secrets.zeroize(master)
        }
    }

    private fun createProfile(kind: ProfileKind, label: String, pin: CharArray, params: MiogramKdf.Params): ProfileRecord {
        require(pin.isNotEmpty()) { "pin must not be empty" }
        val id = UUID.randomUUID().toString()
        val salt = kdf.newSalt()
        val master = ByteArray(SECRET_LENGTH).also(random::nextBytes)
        return kdf.deriveChar(pin, salt, params).useWrappingKey { tag, wrappingKey ->
            val sealed = AesGcm.encrypt(wrappingKey, master, masterAad(id))
            Secrets.zeroize(master)
            ProfileRecord(id, kind, label, PasscodeVerifierSpec(salt, params, tag.copyOf()))
                .withWrappedSecret(NAMESPACE_MASTER, sealed)
        }
    }

    private fun wrapWithSession(s: Session, namespace: String, plaintext: ByteArray): ByteArray =
        s.masterSecret.withRaw { master -> AesGcm.encrypt(master, plaintext, secretAad(s.record.id, namespace)) }

    private fun unwrapWithSession(s: Session, namespace: String, sealed: ByteArray): ByteArray =
        s.masterSecret.withRaw { master ->
            try {
                AesGcm.decrypt(master, sealed, secretAad(s.record.id, namespace))
            } catch (e: AesGcm.SealedBoxException) {
                throw VaultFormatException("secret '$namespace' failed authentication", e)
            }
        }

    private fun replaceProfile(updated: ProfileRecord) {
        val metadata = loadMetadata()
        val next = metadata.profiles.map { if (it.id == updated.id) updated else it }
        persist(VaultMetadata(next))
    }

    private fun loadMetadata(): VaultMetadata {
        val blob = metadataRepo.read() ?: return VaultMetadata(emptyList())
        val payload = try {
            metadataCipher.open(blob, METADATA_AAD)
        } catch (e: AesGcm.SealedBoxException) {
            throw VaultFormatException("metadata failed authentication — wrong device keystore or tampering", e)
        }
        return try {
            VaultCodec.decode(payload)
        } finally {
            Secrets.zeroize(payload)
        }
    }

    private fun persist(metadata: VaultMetadata) {
        val payload = VaultCodec.encode(metadata)
        try {
            metadataRepo.write(metadataCipher.seal(payload, METADATA_AAD))
        } finally {
            Secrets.zeroize(payload)
        }
    }

    private fun closeSessionLocked() {
        session?.close()
        session = null
    }

    private fun validateNamespace(namespace: String) {
        if (namespace.isBlank() || namespace.length > 64 || !namespace.matches(NS_REGEX)) {
            throw IllegalArgumentException("invalid secret namespace: '$namespace'")
        }
    }

    companion object {
        const val NAMESPACE_MASTER = "master"
        const val NAMESPACE_DB_PASSPHRASE = "db.sqlcipher.passphrase"
        const val NAMESPACE_AI_GEMINI = "ai.gemini.key"
        const val SECRET_LENGTH = 32
        const val MAX_PROFILES = 8
        val DEFAULT_PARAMS = MiogramKdf.Params.STANDARD

        private val NS_REGEX = Regex("[a-z0-9._-]{1,64}")

        const val DEFAULT_REAL_LABEL = "Main"
        const val DEFAULT_DECOY_LABEL = "Personal"

        private val METADATA_AAD = "miogram.vault.meta.v1".toByteArray(Charsets.UTF_8)

        private fun masterAad(profileId: String) = "miogram.profile.master.v1|$profileId".toByteArray(Charsets.UTF_8)

        private fun secretAad(profileId: String, namespace: String) =
            "miogram.profile.secret.v1|$profileId|$namespace".toByteArray(Charsets.UTF_8)
    }
}
