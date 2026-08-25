package app.miogram.bridge.storage

import android.content.Context
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Wires ProfileVault-derived passphrases into Room via SQLCipher.
 *
 * Usage at the single integration point
 * (`com.radolyn.ayugram.database.AyuData#createDatabase`):
 * ```
 * .openHelperFactory(MiogramRoomAdapter.applyOpenHelperFactory(builder, name))
 * ```
 */
object MiogramDatabaseSecurity {

    @Volatile
    private var nativeLoaded = false

    /**
     * sqlcipher-android 4.x initializes through System.loadLibrary alone
     * (verified against the 4.18 AAR — there is no loadLibs entry point).
     * Idempotent and thread-safe.
     */
    fun ensureNativeLoaded(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (!nativeLoaded) {
            synchronized(this) {
                if (!nativeLoaded) {
                    System.loadLibrary("sqlcipher")
                    nativeLoaded = true
                }
            }
        }
    }

    /**
     * Builds a Room-compatible factory from raw passphrase bytes. The array is
     * copied by SQLCipher during construction; zeroize the source afterwards.
     */
    fun openHelperFactory(passphrase: ByteArray): SupportOpenHelperFactory =
        SupportOpenHelperFactory(passphrase.copyOf())

    fun defaultDatabaseFileName(context: Context): String =
        context.getDatabasePath("miogram_secure.db").absolutePath
}
