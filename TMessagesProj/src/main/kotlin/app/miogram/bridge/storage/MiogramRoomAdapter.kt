package app.miogram.bridge.storage

import android.content.Context
import android.os.Looper
import androidx.room.RoomDatabase
import app.miogram.bridge.MiogramFlags
import app.miogram.core.crypto.KeyMaterial
import app.miogram.core.storage.HistoryStoragePolicy
import app.miogram.bridge.passcode.MiogramLockFacade

/**
 * Thin Android shell over [HistoryStoragePolicy], applied at Room build time.
 *
 * Activation contract: encryption engages only when the kill-switch is on AND
 * a REAL session already holds keys at database-construction time. Until the
 * Этап 1.5 lifecycle audit adds unlock-triggered re-opening, that means warm
 * restarts while unlocked only; every other path keeps legacy plaintext
 * behaviour with zero risk to existing installs.
 */
object MiogramRoomAdapter {

    private const val PRIMARY_HISTORY_DB = "ayu-data"

    /**
     * Redirects only the primary history database; import/staging databases
     * always keep their requested name.
     */
    @JvmStatic
    fun resolveName(requested: String): String {
        val decision = HistoryStoragePolicy.resolve(
            requestedName = requested,
            primaryName = PRIMARY_HISTORY_DB,
            featureEnabled = MiogramFlags.ENCRYPT_AYU_DB,
            realSessionActive = MiogramLockFacade.isRealSessionActiveSnapshot(),
        )
        return when (decision) {
            is HistoryStoragePolicy.Decision.Plaintext -> decision.name
            is HistoryStoragePolicy.Decision.Encrypted -> decision.name
        }
    }

    /** Attaches SQLCipher when [resolvedName] denotes the secure variant. */
    @JvmStatic
    fun <T : RoomDatabase> applyOpenHelperFactory(builder: RoomDatabase.Builder<T>, resolvedName: String) {
        if (!HistoryStoragePolicy.isSecureName(resolvedName)) return

        // createDatabase() may run during class init on the main thread;
        // blocking there for Argon2id/keystore work would ANR. Failing open to
        // plaintext is safe while ENCRYPT_AYU_DB ships disabled.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            android.util.Log.e("Miogram", "secure history requested on main thread; refusing to block")
            return
        }

        val passphrase: KeyMaterial = kotlinx.coroutines.runBlocking {
            MiogramLockFacade.databasePassphrase()
        } ?: run {
            android.util.Log.e("Miogram", "no real session passphrase for secure history")
            return
        }

        passphrase.use {
            builder.openHelperFactory(
                MiogramDatabaseSecurity.openHelperFactory(it.bytes())
            )
        }
    }

    /** Purges both storage variants; wired into AyuData.clean(). */
    @JvmStatic
    fun deleteVariants(context: Context, primaryName: String) {
        for (name in HistoryStoragePolicy.variantNames(primaryName)) {
            context.deleteDatabase(name)
        }
    }

    @JvmStatic
    fun isSecureHistoryActive(): Boolean =
        MiogramFlags.ENCRYPT_AYU_DB && MiogramLockFacade.isRealSessionActiveSnapshot()
}
