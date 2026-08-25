package app.miogram.bridge.passcode

import android.content.Context
import app.miogram.core.crypto.KeyMaterial
import app.miogram.core.crypto.Secrets
import app.miogram.core.vault.BlobRepository
import app.miogram.core.vault.ProfileVault
import app.miogram.core.vault.UnlockResult
import app.miogram.bridge.keystore.AndroidKeystoreMetadataCipher
import app.miogram.bridge.storage.FileBlobRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single entry point for the host UI (PasscodeView bridge, future Miogram
 * lock screen). Guarantees:
 *  * every vault call runs serialized on a background dispatcher (no ANR,
 *    no concurrent state mutation);
 *  * PIN char arrays passed by callers are defensively copied and zeroized;
 *  * Argon2id work never touches the main thread.
 */
object MiogramLockFacade {

    private const val VAULT_FILE = "files/miogram_vault.bin"

    private val mutex = Mutex()
    private val vaultDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val adminScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.Default
    )

    /**
     * Bumped by [zeroizeNow]; an in-flight verification whose generation went
     * stale releases whatever session it managed to open, so a wipe can never
     * be undone by a result that was already racing it.
     */
    private val operationGeneration = java.util.concurrent.atomic.AtomicLong()

    @Volatile
    private var vault: ProfileVault? = null

    /**
     * Explicit wiring for tests and alternative hosts. Production code paths
     * use [requireVault], which lazily attaches to the host application
     * context, so forgetting init() cannot silently disable security.
     */
    fun init(context: Context) {
        obtainVault { FileBlobRepository(context, VAULT_FILE) }
    }

    private fun obtainVault(create: () -> BlobRepository): ProfileVault {
        vault?.let { return it }
        synchronized(this) {
            vault?.let { return it }
            return ProfileVault(create(), AndroidKeystoreMetadataCipher()).also { vault = it }
        }
    }

    fun isConfigured(): Boolean = runCatching { requireVault().isInitialized() }.getOrDefault(false)


    fun hasDuressProfiles(): Boolean = runCatching { requireVault().hasDecoyProfiles() }.getOrDefault(false)

    /**
     * Non-blocking consistency-tolerant snapshot of session state for
     * storage-mode decisions. A racing lock()/unlock() may return a value one
     * step stale, which callers treat as advisory only.
     */
    fun isRealSessionActiveSnapshot(): Boolean = vault?.isActiveSessionReal() == true

    suspend fun setup(realPin: CharArray, duressPin: CharArray?) {
        val pinCopy = realPin.copyOf()
        val duressCopy = duressPin?.copyOf()
        try {
            withContext(vaultDispatcher) {
                mutex.withLock {
                    val v = requireVault()
                    v.initialize(ProfileVault.DEFAULT_REAL_LABEL, pinCopy)
                    if (duressCopy != null) {
                        v.addDecoyProfile(ProfileVault.DEFAULT_DECOY_LABEL, duressCopy)
                    }
                }
            }
        } finally {
            Secrets.zeroize(pinCopy, duressCopy)
        }
    }

    /**
     * Verifies [pin] and opens the matching profile session. Both REAL and
     * DECOY successes return [UnlockResult.Success]; the caller branches on
     * `profile.kind` to decide which workspace to reveal.
     */
    suspend fun verifyAndUnlock(pin: CharArray): UnlockResult {
        val copy = pin.copyOf()
        val myGeneration = operationGeneration.get()
        try {
            val result = withContext(vaultDispatcher) {
                mutex.withLock { requireVault().unlock(copy) }
            }
            if (operationGeneration.get() != myGeneration && result is UnlockResult.Success) {
                // A wipe was requested while this verification ran; do not
                // leave a freshly opened session behind it.
                withContext(vaultDispatcher) {
                    mutex.withLock { requireVault().lock() }
                }
            }
            return result
        } finally {
            Secrets.zeroize(copy)
        }
    }

    suspend fun lock() {
        withContext(vaultDispatcher) {
            mutex.withLock { requireVault().lock() }
        }
    }

    /**
     * SQLCipher passphrase for the open REAL session; null when locked or
     * when the active profile is a decoy (decoy workspace must not be able to
     * read the real database). Caller owns and must close the handle.
     */
    suspend fun databasePassphrase(): KeyMaterial? = withContext(vaultDispatcher) {
        mutex.withLock {
            val v = requireVault()
            if (!v.isActiveSessionReal()) return@withLock null
            v.ensureWrappedSecret(ProfileVault.NAMESPACE_DB_PASSPHRASE)
        }
    }

    /**
     * Releases all RAM-held key material asynchronously (safe to call from
     * the main thread — never blocks, never ANRs). Call when the lock screen
     * appears; the next successful unlock opens a fresh session anyway.
     */
    fun zeroizeNow() {
        operationGeneration.incrementAndGet()
        val v = vault ?: return
        adminScope.launch {
            mutex.withLock { v.lock() }
        }
    }

    /** Anti-forensics panic path: destroys persisted vault metadata. */
    suspend fun wipeAll() {
        withContext(vaultDispatcher) {
            mutex.withLock { requireVault().wipeAll() }
        }
    }

    /**
     * BYOK secret for cloud AI (AI Studio key), sealed under the REAL profile.
     * Returns null while locked or in a decoy session — the decoy workspace
     * must not be able to spend the user's cloud quota. Caller closes the
     * handle after copying bytes out.
     */
    suspend fun aiKeyMaterial(): KeyMaterial? {
        return withContext(vaultDispatcher) {
            mutex.withLock {
                val v = requireVault()
                if (!v.isActiveSessionReal()) return@withLock null
                v.ensureWrappedSecret(ProfileVault.NAMESPACE_AI_GEMINI)
            }
        }
    }

    /**
     * Lazily attaches to the host application context. Importing
     * ApplicationLoader here is sanctioned: the bridge layer is the only place
     * where host dependencies are allowed.
     */
    private fun requireVault(): ProfileVault = obtainVault {
        FileBlobRepository(org.telegram.messenger.ApplicationLoader.applicationContext, VAULT_FILE)
    }
}
