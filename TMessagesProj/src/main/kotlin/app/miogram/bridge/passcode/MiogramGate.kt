package app.miogram.bridge.passcode

import app.miogram.bridge.MiogramFlags
import app.miogram.core.vault.ProfileKind
import app.miogram.core.vault.UnlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Lock-screen interception point for the host UI.
 *
 * Security invariants:
 *  * when a vault is configured, legacy passcode stores are NOT consulted —
 *    falling back to SharedConfig.checkPasscode would let an observer bypass
 *    the duress mechanism with the old PIN;
 *  * verification is asynchronous: Argon2id costs tens to hundreds of
 *    milliseconds and must never run on the UI thread;
 *  * both REAL and DECOY successes open a facade session; callers branch on
 *    the verdict to decide which workspace may be shown.
 */
object MiogramGate {

    const val VERDICT_LEGACY = 0
    const val VERDICT_REAL_UNLOCKED = 1
    const val VERDICT_DECOY_UNLOCKED = 2
    const val VERDICT_DENIED = 3

    /** Java-SAM friendly so host call sites can use lambdas. */
    fun interface Callback {
        fun onVerdict(verdict: Int)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val generation = AtomicLong()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @JvmStatic
    fun isConfigured(): Boolean =
        MiogramFlags.DURESS_GATE && MiogramLockFacade.isConfigured()

    /**
     * Verifies [pin] against every vault profile off the main thread and
     * posts exactly one verdict. Only the latest invocation delivers: stale
     * verdicts must never unlock UI state that has already moved on.
     */
    @JvmStatic
    fun interceptUnlockAsync(pin: String, callback: Callback) {
        val myGeneration = generation.incrementAndGet()
        val chars = pin.toCharArray()
        scope.launch {
            val verdict = try {
                when (val result = MiogramLockFacade.verifyAndUnlock(chars)) {
                    is UnlockResult.Success ->
                        if (result.profile.kind == ProfileKind.REAL) VERDICT_REAL_UNLOCKED
                        else VERDICT_DECOY_UNLOCKED
                    UnlockResult.Denied -> VERDICT_DENIED
                }
            } catch (e: Exception) {
                VERDICT_DENIED
            } finally {
                chars.fill(' ')
            }
            mainHandler.post {
                if (generation.get() == myGeneration) {
                    callback.onVerdict(verdict)
                }
            }
        }
    }

    /** Releases all RAM-held key material; call from host onPause/onStop hooks. */
    @JvmStatic
    fun onHostPaused() {
        MiogramLockFacade.zeroizeNow()
    }
}
