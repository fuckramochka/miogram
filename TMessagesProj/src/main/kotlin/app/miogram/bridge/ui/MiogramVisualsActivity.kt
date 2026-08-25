package app.miogram.bridge.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import app.miogram.bridge.HyperionFlags

/**
 * AGSL liquid-glass tuning screen. Sliders persist into the same prefs the
 * decoration view reads, so effects apply app-wide after re-inflation.
 */
class MiogramVisualsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val density = resources.displayMetrics.density
        fun px(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(16), px(16), px(24))
            setBackgroundColor(0xFFF5F5F7.toInt())
        }

        root.addView(title("Рідке скло (AGSL)"))
        if (Build.VERSION.SDK_INT >= 33) {
            root.addView(caption("Апаратне заломлення світла та хроматична аберація. Android 13+."))
        } else {
            root.addView(caption("Потрібен Android 13+ — зараз використовується спрощений fallback."))
        }

        val toggle = Button(this).apply {
            setText(if (HyperionFlags.SPATIAL_DECORATION) "Ефект: увімкнений" else "Ефект: вимкнений")
            setBackgroundColor(if (HyperionFlags.SPATIAL_DECORATION) 0xFF2E6DA4.toInt() else 0xFF888888.toInt())
            setTextColor(Color.WHITE)
            setOnClickListener {
                HyperionFlags.SPATIAL_DECORATION = !HyperionFlags.SPATIAL_DECORATION
                setText(if (HyperionFlags.SPATIAL_DECORATION) "Ефект: увімкнений" else "Ефект: вимкнений")
                setBackgroundColor(if (HyperionFlags.SPATIAL_DECORATION) 0xFF2E6DA4.toInt() else 0xFF888888.toInt())
                persist()
            }
        }
        root.addView(toggle, margin())

        root.addView(caption("Інтенсивність ефекту"))
        root.addView(seek(HyperionVisualsPrefs.loadInt(this, KEY_INTENSITY, 60)) { value ->
            HyperionVisualsPrefs.saveInt(this@MiogramVisualsActivity, KEY_INTENSITY, value)
        })

        root.addView(title("Примітка"))
        root.addView(caption("Ефект застосовується до декоративних панелей Miogram після повторного відкриття екрана. Стек пілюль налаштовується в розділі exteraless."))

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    // --- helpers -------------------------------------------------------------

    private fun seek(initialPercent: Int, onChange: (Int) -> Unit): SeekBar = SeekBar(this).apply {
        max = 100
        progress = initialPercent.coerceIn(0, 100)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) onChange(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }


    private fun title(text: String) = TextView(this).apply {
        setText(text)
        setTextColor(Color.BLACK)
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(14), 0, px(6))
    }

    private fun caption(text: String) = TextView(this).apply {
        setText(text)
        setTextColor(Color.DKGRAY)
        textSize = 13f
        setPadding(0, px(4), 0, px(8))
    }

    private fun margin() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = px(10) }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private companion object {
        const val KEY_INTENSITY = "liquid_glass_intensity"
    }
}

object HyperionVisualsPrefs {
    fun loadInt(context: android.content.Context, key: String, def: Int): Int =
        runCatching {
            context.getSharedPreferences("miogram_visuals", android.content.Context.MODE_PRIVATE).getInt(key, def)
        }.getOrDefault(def)

    fun saveInt(context: android.content.Context, key: String, value: Int) {
        runCatching {
            context.getSharedPreferences("miogram_visuals", android.content.Context.MODE_PRIVATE).edit().putInt(key, value).apply()
        }
    }
}
