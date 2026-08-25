package app.miogram.bridge.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.miogram.bridge.passcode.MiogramGate

/**
 * Central hub for all Miogram subsystems. Launched from Telegram settings;
 * every security/AI/plugin surface is one tap away from here.
 */
class MiogramHubActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val density = resources.displayMetrics.density
        fun px(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(20), px(16), px(24))
            setBackgroundColor(0xFFF5F5F7.toInt())
        }

        root.addView(TextView(this).apply {
            text = "Miogram"
            setTextColor(Color.BLACK)
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Secure Agentic Workspace · @dkramochka"
            setTextColor(Color.GRAY)
            textSize = 12f
            setPadding(0, px(2), 0, px(14))
        })

        root.addView(sectionLabel("БЕЗПЕКА"))
        root.addView(navCard("Сховище та тривожний PIN", vaultStatusLine()) {
            startActivity(Intent(this, MiogramVaultSetupActivity::class.java))
        })
        root.addView(navCard("Розблокування: режим", gateStatusLine()) {
            // Gate policy is managed inside the vault screen by design.
            toast("Політика розблокування керується у сховищі")
        })

        root.addView(sectionLabel("ШТУЧНИЙ ІНТЕЛЕКТ"))
        root.addView(navCard("AI: локально / хмара", aiStatusLine()) {
            startActivity(Intent(this, MiogramAiSettingsActivity::class.java))
        })

        root.addView(sectionLabel("РОЗШИРЕННЯ"))
        root.addView(navCard("WASM-плагіни", pluginStatusLine()) {
            startActivity(Intent(this, MiogramPluginsActivity::class.java))
        })

        root.addView(sectionLabel("ВІЗУАЛЬНЕ ОФОРМЛЕННЯ"))
        root.addView(navCard("Рідке скло (AGSL)", if (app.miogram.bridge.HyperionFlags.SPATIAL_DECORATION) "увімкнено" else "вимкнено") {
            startActivity(Intent(this, MiogramVisualsActivity::class.java))
        })

        setContentView(
            ScrollView(root).apply {
                addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        )
    }

    private fun ScrollView(content: View): View {
        val sv = android.widget.ScrollView(this)
        sv.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return sv
    }

    private fun sectionLabel(text: String): TextView = TextView(this).apply {
        text = text
        setTextColor(0xFF2E6DA4.toInt())
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(18), 0, px(6))
    }

    private fun navCard(title: String, subtitle: String, onClick: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(px(14), px(10), px(14), px(10))
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MiogramHubActivity).apply {
                text = title
                setTextColor(Color.BLACK)
                textSize = 16f
            })
            addView(TextView(this@MiogramHubActivity).apply {
                text = subtitle
                setTextColor(Color.GRAY)
                textSize = 12f
            })
            setOnClickListener { onClick() }
        }.also { card ->
            val p = px(8)
            card.setPadding(card.paddingLeft, card.paddingTop, card.paddingRight, card.paddingBottom)
            card.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = p / 2 }
        }

    private fun vaultStatusLine(): String =
        if (MiogramGate.isConfigured()) "сховище активне" + (if (MiogramGate.hasDuressProfiles()) " · duress увімкнений" else "") else "не налаштоване"

    private fun gateStatusLine(): String =
        if (MiogramGate.isConfigured()) "PIN + пароль (біометрія вимкнена)" else "стандартне розблокування"

    private fun aiStatusLine(): String = "режими задач · приватність хмарних запитів"

    private fun pluginStatusLine(): String =
        if (app.miogram.bridge.plugins.WamrWasmRuntime.isAvailable()) "рантайм готовий" else "рантайм відсутній у цій збірці"

    private fun toast(m: String) = android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show()
}
