package app.miogram.core.storage

/**
 * Pure decision logic for history-database storage mode. Kept free of
 * android/Room types so the security-relevant branching is unit-testable.
 */
object HistoryStoragePolicy {

    const val SECURE_SUFFIX = ".secure"

    sealed class Decision {
        /** Existing behaviour: platform SQLite at [name]. */
        data class Plaintext(val name: String) : Decision()

        /** SQLCipher-backed store at [name], opened with the session passphrase. */
        data class Encrypted(val name: String) : Decision()
    }

    /**
     * @param requestedName caller-requested database file name
     * @param primaryName   the well-known name of the primary history DB;
     *                      staging/import databases are never redirected
     * @param featureEnabled global kill-switch ([app.miogram.bridge.MiogramFlags])
     * @param realSessionActive whether a REAL profile session currently holds keys
     */
    fun resolve(
        requestedName: String,
        primaryName: String,
        featureEnabled: Boolean,
        realSessionActive: Boolean,
    ): Decision {
        if (!featureEnabled || !realSessionActive) return Decision.Plaintext(requestedName)
        if (requestedName != primaryName) return Decision.Plaintext(requestedName)
        return Decision.Encrypted(requestedName + SECURE_SUFFIX)
    }

    fun isSecureName(resolvedName: String): Boolean = resolvedName.endsWith(SECURE_SUFFIX)

    /** Both plaintext and encrypted variants, used when purging all history. */
    fun variantNames(primaryName: String): List<String> = listOf(primaryName, primaryName + SECURE_SUFFIX)
}
