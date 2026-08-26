package app.miogram.bridge.ui

import android.content.Context

object MiogramVisualsPrefs {
    @JvmStatic
    fun loadInt(context: Context, key: String, def: Int): Int =
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE).getInt(key, def)
        }.getOrDefault(def)

    @JvmStatic
    fun saveInt(context: Context, key: String, value: Int) {
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE)
                .edit().putInt(key, value).apply()
        }
    }

    @JvmStatic
    fun loadBool(context: Context, key: String, def: Boolean): Boolean =
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE).getBoolean(key, def)
        }.getOrDefault(def)

    @JvmStatic
    fun saveBool(context: Context, key: String, value: Boolean) {
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).apply()
        }
    }
}
