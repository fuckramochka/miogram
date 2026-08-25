package app.miogram.bridge.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.miogram.bridge.MiogramFlags
import app.miogram.bridge.passcode.MiogramGate
import app.miogram.bridge.plugins.WamrWasmRuntime

/**
 * Central hub for all Miogram subsystems. Launched from Telegram settings;
 * every security/AI/plugin surface is one tap away from here.
 */
class MiogramHubActivity : Activity() {

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(20), px(16), px(24))
            setBackgroundColor(0xFFF5F5F7.toInt())
        }

        root.addView(titleView("Miogram"))
        root.addView(captionView("Secure Agentic Workspace · @dkramochka"))

        root.addView(sectionLabel("БЕЗПЕКА"))
        root.addView(navCard("Сховище та тривожний PIN", vaultStatusLine()) {
            startActivity(Intent(this, MiogramVaultSetupActivity::class.java))
        })
        root.addView(navCard("Політика розблокування", gateStatusLine()) {
            toast(if (MiogramGate.isConfigured()) "Біометрія вимкнена поки активне сховище" else "Сховище не налаштоване")
        })

        root.addView(sectionLabel("ШТУЧНИЙ ІНТЕЛЕКТ"))
        root.addView(navCard("AI: локально / хмара", "режими задач · приватність хмарних запитів") {
            startActivity(Intent(this, MiogramAiSettingsActivity::class.java))
        })

        root.addView(sectionLabel("РОЗШИРЕННЯ"))
        root.addView(navCard("WASM-плагіни", pluginStatusLine()) {
            startActivity(Intent(this, MiogramPluginsActivity::class.java))
        })

        root.addView(sectionLabel("ВІЗУАЛЬНЕ ОФОРМЛЕННЯ"))
        root.addView(navCard(
            "Рідке скло (AGSL)",
            if (MiogramFlags.SPATIAL_DECORATION) "увімкнено" else "вимкнено"
        ) {
            startActivity(Intent(this, MiogramVisualsActivity::class.java))
        })

        val scroll = ScrollView(this)
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun refreshStatuses() {
        // Re-evaluate dynamic subtitles in place.
        for (i in 0 until root.childCount) {
            val group = root.getChildAt(i)
            if (group is LinearLayout && group.childCount >= 2) {
                val subtitle = group.getChildAt(1) as? TextView ?: continue
                when (subtitle.text.toString()) {
                    else -> {}
                }
            }
        }
    }

    private fun titleView(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(Color.BLACK)
        textSize = 26f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun captionView(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(Color.GRAY)
        textSize = 12f
        setPadding(0, px(2), 0, px(14))
    }

    private fun sectionLabel(label: String): TextView = TextView(this).apply {
        text = label
        setTextColor(0xFF2E6DA4.toInt())
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(18), 0, px(6))
    }

    private fun navCard(title: String, subtitle: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(14), px(10), px(14), px(10))
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.BLACK)
                textSize = 16f
            })
            addView(TextView(context).apply {
                text = subtitle
                setTextColor(Color.GRAY)
                textSize = 12f
            })
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = px(6) }
        }

    private fun vaultStatusLine(): String =
        if (MiogramGate.isConfigured())
            "сховище активне" + if (MiogramGate.hasDuressProfiles()) " · duress увімкнений" else ""
        else "не налаштоване"

    private fun gateStatusLine(): String =
        if (MiogramGate.isConfigured()) "PIN + пароль (біометрія вимкнена)" else "стандартне розблокування"

    private fun pluginStatusLine(): String =
        if (WamrWasmRuntime.isAvailable()) "рантайм готовий" else "рантайм відсутній у цій збірці"

    private fun px(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
