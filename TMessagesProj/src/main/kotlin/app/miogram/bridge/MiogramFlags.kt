package app.miogram.bridge

/**
 * Runtime feature switches for Miogram. Backed by volatile fields so
 * settings UI (Этап 1.5) can flip them without restarts; defaults keep new
 * subsystems dormant until their lifecycle integration is audited.
 */
object MiogramFlags {

    /**
     * Routes the AyuGram history database through SQLCipher while a REAL
     * session is open. OFF until the lazy-open lifecycle audit completes:
     * records written between process start and first unlock would otherwise
     * land in a differently-encrypted store than the one later displayed.
     */
    @Volatile
    var ENCRYPT_AYU_DB: Boolean = false

    /** Master switch for the Duress PIN interception on the lock screen. */
    @Volatile
    var DURESS_GATE: Boolean = true

    /**
     * Liquid-glass decoration layer (Этап 4). Off until the chat-bubble
     * decorator integration is reviewed for 60/120 FPS budgets.
     */
    @Volatile
    var SPATIAL_DECORATION: Boolean = false
}
