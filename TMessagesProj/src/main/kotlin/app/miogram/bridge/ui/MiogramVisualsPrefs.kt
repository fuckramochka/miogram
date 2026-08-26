package app.miogram.bridge.ui

import android.content.Context

object MiogramVisualsPrefs {
    fun loadInt(context: Context, key: String, def: Int): Int =
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE).getInt(key, def)
        }.getOrDefault(def)

    fun saveInt(context: Context, key: String, value: Int) {
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE)
                .edit().putInt(key, value).apply()
        }
    }

    fun loadBool(context: Context, key: String, def: Boolean): Boolean =
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE).getBoolean(key, def)
        }.getOrDefault(def)

    fun saveBool(context: Context, key: String, value: Boolean) {
        runCatching {
            context.getSharedPreferences("miogram_visuals", Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).apply()
        }
    }
}
