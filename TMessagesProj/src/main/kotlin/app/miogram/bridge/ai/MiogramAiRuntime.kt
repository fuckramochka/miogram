package app.miogram.bridge.ai

import android.content.Context

/**
 * Process-wide holder for the AI stack: one facade + one STT engine shared by
 * every screen (same prefs, same model directory).
 */
object MiogramAiRuntime {

    @Volatile
    private var facade: MiogramAiFacade? = null
    @Volatile
    private var stt: LocalSttEngine? = null

    @JvmStatic
    fun get(context: Context): MiogramAiFacade {
        val existing = facade
        if (existing != null) return existing
        synchronized(this) {
            facade?.let { return it }
            stt = MiogramSttFactory.create(context.applicationContext)
            return MiogramAiFacade(context.applicationContext, stt!!).also { facade = it }
        }
    }

    @JvmStatic
    fun stt(context: Context): LocalSttEngine {
        get(context)
        return stt!!
    }
}
