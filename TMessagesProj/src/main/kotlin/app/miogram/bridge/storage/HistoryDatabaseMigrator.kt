package app.miogram.bridge.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlatformSQLiteDatabase
import app.miogram.core.storage.HistoryStoragePolicy
import java.io.File

/**
 * One-shot conversion of a plaintext SQLite history database into its
 * SQLCipher-backed variant, using the canonical `sqlcipher_export` pipeline.
 *
 * Output layout matches [HistoryStoragePolicy]: the encrypted store lives
 * beside the original under `.secure` suffix. The plaintext original is left
 * untouched on success — deliberate during the transition window; purging it
 * is a separate, explicit operator action (anti-forensics wipe) so that a
 * failed migration never destroys the only copy of user data.
 */
object HistoryDatabaseMigrator {

    private const val SECURE_SUFFIX = HistoryStoragePolicy.SECURE_SUFFIX
    private const val ATTACHED_ALIAS = "plaintext_source"

    sealed class Result {
        /** Target already exists and passes integrity check — nothing to do. */
        data object AlreadyEncrypted : Result()

        data class Migrated(val secureFile: File, val pagesCopied: Boolean) : Result()

        data class Failed(val reason: String, val cause: Throwable? = null) : Result()
    }

    /**
     * @param primaryName e.g. "ayu-data"; produces "<primaryName>.secure"
     *
     * MUST be called off the main thread with no open Room handle on
     * [primaryName] (close/reopen coordination belongs to the caller).
     */
    fun encryptPlaintextDatabase(context: Context, primaryName: String, passphrase: ByteArray): Result {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            return Result.Failed("migration attempted on main thread")
        }
        MiogramDatabaseSecurity.ensureNativeLoaded(context)

        val sourceFile = context.getDatabasePath(primaryName)
        if (!sourceFile.isFile) {
            return Result.Failed("source database missing: ${sourceFile.absolutePath}")
        }
        // -wal/-shm sidecars must not exist after checkpointing below.
        val sourceWal = File(sourceFile.parentFile, "$primaryName-wal")
        val sourceShm = File(sourceFile.parentFile, "$primaryName-shm")

        val targetFile = context.getDatabasePath("$primaryName$SECURE_SUFFIX")
        if (targetFile.isFile && targetFile.length() > 0) {
            return verifyExisting(targetFile, passphrase)
        }

        try {
            // 1. Fold WAL back into the main file so ATTACH sees complete data.
            checkpointSource(sourceFile)

            // 2. Export into a fresh encrypted database.
            targetFile.delete()
            File(targetFile.parentFile, "$primaryName$SECURE_SUFFIX-wal").delete()
            File(targetFile.parentFile, "$primaryName$SECURE_SUFFIX-shm").delete()

            val userVersion = readUserVersion(sourceFile)

            net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                targetFile.absolutePath,
                passphrase.copyOf(),
                null,
                null
            ).use { secure ->
                secure.execSQL(
                    "ATTACH DATABASE ? AS $ATTACHED_ALIAS KEY ''",
                    arrayOf(sourceFile.absolutePath)
                )
                secure.execSQL("SELECT sqlcipher_export('$ATTACHED_ALIAS')")
                secure.version = userVersion
                secure.execSQL("DETACH DATABASE $ATTACHED_ALIAS")
                secure.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst() && !"ok".equals(cursor.getString(0), ignoreCase = true)) {
                        return Result.Failed("integrity_check failed: ${cursor.getString(0)}")
                    }
                }
            }

            // Sidecars of a fully-journaled export are transient; clean them.
            File(targetFile.parentFile, "$primaryName$SECURE_SUFFIX-wal").delete()
            File(targetFile.parentFile, "$primaryName$SECURE_SUFFIX-shm").delete()
            sourceWal.delete()
            sourceShm.delete()

            return Result.Migrated(targetFile, pagesCopied = true)
        } catch (t: Throwable) {
            // Never leave a half-written encrypted store behind.
            targetFile.delete()
            return Result.Failed("export failed", t)
        }
    }

    private fun verifyExisting(targetFile: File, passphrase: ByteArray): Result {
        return try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                targetFile.absolutePath,
                passphrase.copyOf(),
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                null
            ).use { db ->
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst() && "ok".equals(cursor.getString(0), ignoreCase = true)) {
                        Result.AlreadyEncrypted
                    } else {
                        Result.Failed("existing secure store fails integrity check")
                    }
                }
            }
        } catch (t: Throwable) {
            Result.Failed("existing secure store not readable with session key", t)
        }
    }

    private fun checkpointSource(sourceFile: File) {
        val plain: android.database.sqlite.SQLiteDatabase =
            PlatformSQLiteDatabase.openDatabase(
                sourceFile.absolutePath,
                null as android.database.sqlite.SQLiteDatabase.CursorFactory?,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            )
        try {
            val cursor = plain.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
            try {
                cursor.moveToFirst()
            } finally {
                cursor.close()
            }
        } finally {
            plain.close()
        }
    }

    private fun readUserVersion(sourceFile: File): Int =
        PlatformSQLiteDatabase.openDatabase(
            sourceFile.absolutePath,
            null,
            PlatformSQLiteDatabase.OPEN_READONLY
        ).use { plain -> plain.version }
}
