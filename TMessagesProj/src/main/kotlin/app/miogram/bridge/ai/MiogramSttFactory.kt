package app.miogram.bridge.ai

import android.content.Context
import java.io.File

/**
 * Android wiring for the pure-JVM [LocalSttEngine]. The only place in the STT
 * subsystem that touches android.content.
 */
object MiogramSttFactory {

    fun modelDirectory(context: Context): File =
        File(context.filesDir, "miogram_ai").apply { mkdirs() }

    fun create(context: Context): LocalSttEngine =
        LocalSttEngine(modelDirectory(context))
}
