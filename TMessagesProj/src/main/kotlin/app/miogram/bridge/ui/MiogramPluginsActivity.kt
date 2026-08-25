package app.miogram.bridge.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.miogram.bridge.plugins.WamrWasmRuntime
import app.miogram.core.plugins.MiogramPluginEngine
import app.miogram.core.plugins.InMemoryPluginRepository
import app.miogram.core.plugins.InMemoryTrustAnchors
import app.miogram.core.plugins.PluginState

/**
 * WebAssembly plugin manager.
 *
 * v1 scope (honest): the sandbox runtime is available only in builds that
 * bundle WAMR; installs require an Ed25519-signed manifest next to the code,
 * so unsigned modules are rejected by the engine before any guest byte runs.
 * The trust anchor set is empty by default — a signer key must be embedded at
 * build time or imported through a future flow.
 */
class MiogramPluginsActivity : Activity() {

    private val repository = InMemoryPluginRepository()
    private val engine = MiogramPluginEngine(repository, WamrWasmRuntime, InMemoryTrustAnchors())

    private var pendingCode: Pair<Uri, ByteArray>? = null
    private var pendingManifest: Pair<Uri, ByteArray>? = null

    private lateinit var listHost: LinearLayout
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)


        fun button(text: String, color: Int, action: () -> Unit) = Button(this).apply {
            setText(text)
            setBackgroundColor(color)
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(16), px(16), px(24))
            setBackgroundColor(0xFFF5F5F7.toInt())
        }

        statusView = TextView(this).apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, px(6), 0, px(10))
        }
        root.addView(statusView)

        root.addView(note(if (WamrWasmRuntime.isAvailable())
            "Рантайм WAMR активний. Плагіни виконуються в пісочниці без доступу до системи; потрібен Ed25519-підписаний манифест."
        else
            "Ця збірка без WASM-рантайму (відсутній сабмодуль WAMR). Встановлення недоступне."))

        root.addView(button("Обрати код (.wasm)", 0xFF444444.toInt()) {
            pick(FILE_CODE)
        })
        root.addView(button("Обрати манифест (.manifest)", 0xFF444444.toInt()) {
            pick(FILE_MANIFEST)
        })
        root.addView(button("Встановити", 0xFF2E6DA4.toInt()) { installPicked() })

        root.addView(header("ВСТАНОВЛЕНІ ПЛАГІНИ"))
        listHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listHost)

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        refreshList()
    }

    // --- SAF picking --------------------------------------------------------

    private fun pick(which: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        startActivityForResult(intent, which)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val bytes = contentResolver.openInputStream(data.data!!)?.use { it.readBytes() } ?: return
        if (requestCode == FILE_CODE) pendingCode = data.data!! to bytes
        if (requestCode == FILE_MANIFEST) pendingManifest = data.data!! to bytes
        toast("Файл завантажено (${bytes.size} Б)")
    }

    private fun installPicked() {
        val manifestPart = pendingManifest
        val codePart = pendingCode
        if (manifestPart == null || codePart == null) {
            toast("Обидва файли мають бути обрані")
            return
        }
        val verdict = engine.install(manifestPart.second, codePart.second)
        when (verdict) {
            is MiogramPluginEngine.InstallResult.Installed -> {
                toast("Встановлено: ${verdict.pluginId}")
                refreshList()
            }
            is MiogramPluginEngine.InstallResult.Rejected ->
                toast("Відмовлено: ${verdict.reason}${verdict.detail?.let { " ($it)" } ?: ""}")
        }
    }

    // --- plugin rows ----------------------------------------------------------

    private fun refreshList() {
        listHost.removeAllViews()

        for (p in repository.list()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(px(12), px(8), px(12), px(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = px(8) }
            }
            row.addView(TextView(this@MiogramPluginsActivity).apply {
                text = "${p.displayName}  v${p.versionCode}  [${p.state}]"
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.BLACK)
            })
            row.addView(TextView(this@MiogramPluginsActivity).apply {
                text = p.pluginId + " · caps: " + p.capabilities.toList().joinToString(",") { it.name }
                textSize = 11f
                setTextColor(Color.DKGRAY)
            })

            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fun small(text: String, color: Int, action: () -> Unit) =
                Button(this@MiogramPluginsActivity).apply {
                    setText(text); setBackgroundColor(color); setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = px(6) }
                    setOnClickListener { action() }
                }

            when (p.state) {
                PluginState.ENABLED -> actions.addView(small("Вимкнути", 0xFF444444.toInt()) {
                    engine.disable(p.pluginId); refreshList()
                })
                PluginState.INSTALLED, PluginState.DISABLED, PluginState.QUARANTINED ->
                    actions.addView(small("Увімкнути", 0xFF2E6DA4.toInt()) {
                        when (engine.enable(p.pluginId)) {
                            MiogramPluginEngine.EnableResult.Enabled -> {}
                            else -> toast("Не вдалося увімкнути (див. аудит)")
                        }
                        refreshList()
                    })
            }
            if (p.state == PluginState.ENABLED) {
                actions.addView(small("Ping", 0xFF3C7A46.toInt()) { ping(p.pluginId) })
            }
            actions.addView(small("Видалити", 0xFF9C2B2B.toInt()) {
                engine.uninstall(p.pluginId); refreshList()
            })
            row.addView(actions)
            listHost.addView(row)
        }
        if (repository.list().isEmpty()) {
            listHost.addView(TextView(this).apply {
                text = "Ще немає встановлених плагінів."
                setTextColor(Color.GRAY)
                setPadding(0, px(6), 0, 0)
            })
        }
    }

    private fun ping(pluginId: String) {
        val started = System.nanoTime()
        val outcome = engine.dispatch(pluginId, "ping", null)
        val elapsedUs = (System.nanoTime() - started) / 1000
        when (outcome) {
            is MiogramPluginEngine.DispatchOutcome.Ok ->
                toast("Pong за $elapsedUs мкс")
            is MiogramPluginEngine.DispatchOutcome.Denied ->
                toast("Відмовлено: ${outcome.reason}")
            is MiogramPluginEngine.DispatchOutcome.Failed ->
                toast("Збій: ${outcome.reason.take(80)}")
        }
    }

    private fun note(text: String) = TextView(this).apply {
        setText(text)
        setTextColor(Color.DKGRAY)
        textSize = 12f
        setPadding(0, px(4), 0, px(10))
    }

    private fun header(text: String) = TextView(this).apply {
        setText(text)
        setTextColor(0xFF2E6DA4.toInt())
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(16), 0, px(4))
    }

    private fun px(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    companion object {
        private const val FILE_CODE = 41
        private const val FILE_MANIFEST = 42
    }
}
