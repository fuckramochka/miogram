package app.miogram.bridge.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.miogram.bridge.ai.MiogramAiFacade
import app.miogram.bridge.ai.LocalSttEngine
import app.miogram.bridge.ai.MiogramSttFactory
import app.miogram.core.ai.AiTask
import app.miogram.core.ai.ExecutionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Per-task local/cloud selection for the agentic engine.
 *
 * Defaults follow the product policy: transcription = on-device first,
 * semantic index = device-only, text tasks prefer cloud Flash Lite when a
 * key exists. Every row cycles
 * LOCAL_ONLY -> LOCAL_FIRST -> CLOUD_FIRST -> CLOUD_ONLY -> DISABLED.
 */
class MiogramAiSettingsActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var facade: MiogramAiFacade
    private lateinit var stt: LocalSttEngine

    private val taskButtons = mutableMapOf<AiTask, Button>()
    private lateinit var statusView: TextView
    private lateinit var meteredButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        stt = MiogramSttFactory.create(applicationContext)
        facade = MiogramAiFacade(applicationContext, stt)

        setContentView(buildForm())
        refreshAll()
    }

    private fun buildForm(): View {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            setBackgroundColor(0xFFF2F2F2.toInt())
        }

        statusView = TextView(this).apply {
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(statusView)

        root.addView(header("ЛОКАЛЬНЕ ГОЛОС → ТЕКСТ"))
        root.addView(caption("Whisper працює на пристрої: аудіо не залишає телефон. Це режим за замовчуванням."))
        root.addView(rowButton("Модель STT", "керування") {
            toast("Завантаження моделей з'явиться разом із ONNX-бекендом (Етап 3.1)")
        })

        root.addView(header("ЗАВДАННЯ: ЛОКАЛЬНО / ХМАРА"))
        root.addView(caption("Хмара — Gemini Flash Lite через ваш ключ Google AI Studio. Перед відправленням текст очищається від телефонів, карток і ключів."))
        for (task in AiTask.entries) {
            if (task == AiTask.TRANSCRIBE_AUDIO || task == AiTask.SEMANTIC_INDEX) continue
            root.addView(caption(taskLabel(task)))
            val button = button("", color = 0xFF2E6DA4.toInt()) { cycleMode(task) }
            taskButtons[task] = button
            root.addView(button)
        }

        root.addView(header("STT: ВИКЛЮЧНО ЛОКАЛЬНО?"))
        root.addView(
            button("", 0xFF444444.toInt()) { cycleMode(AiTask.TRANSCRIBE_AUDIO) }.also {
                taskButtons[AiTask.TRANSCRIBE_AUDIO] = it
            }
        )
        root.addView(caption("Семантичний індекс завжди рахується на пристрої — він читає всю історію."))

        root.addView(header("МЕРЕЖА"))
        meteredButton = button("", 0xFF444444.toInt()) {
            facade.preferences = facade.preferences.copy(
                cloudAllowedOnMeteredNetwork = !facade.preferences.cloudAllowedOnMeteredNetwork
            )
            refreshAll()
        }
        root.addView(meteredButton)

        return ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun cycleMode(task: AiTask) {
        val order = listOf(
            ExecutionMode.LOCAL_ONLY,
            ExecutionMode.LOCAL_FIRST,
            ExecutionMode.CLOUD_FIRST,
            ExecutionMode.CLOUD_ONLY,
            ExecutionMode.DISABLED,
        )
        val current = facade.preferences.modeFor(task)
        val next = order[(order.indexOf(current) + 1).mod(order.size)]
        facade.preferences = facade.preferences.copy(
            overrides = facade.preferences.overrides + (task to next)
        )
        refreshAll()
        toast("${taskLabel(task)} → ${modeLabel(next)}")
    }

    private fun refreshAll() {
        scope.launch {
            val envSnapshot = facade.environmentSnapshot()
            applySnapshot(envSnapshot)
        }
    }

    private fun applySnapshot(envSnapshot: app.miogram.core.ai.AiEnvironment) {
        statusView.text = buildString {
            append("Локальна модель STT: ")
            append(if (envSnapshot.localModelReady) "готова" else "не завантажена")
            append("\nКлюч хмари: ")
            append(if (envSnapshot.cloudKeyConfigured) "налаштований" else "немає")
            append("\nМережа: ")
            append(
                when {
                    !envSnapshot.networkOnline -> "офлайн"
                    envSnapshot.networkMetered -> "мобільна"
                    else -> "Wi-Fi"
                }
            )
        }
        meteredButton.text =
            "Хмара в мобільній мережі: " +
                    if (facade.preferences.cloudAllowedOnMeteredNetwork) "дозволено" else "заборонено"

        for ((task, view) in taskButtons) {
            view.text = "${taskLabel(task)}: ${modeLabel(facade.preferences.modeFor(task))}"
        }
    }

    private fun taskLabel(task: AiTask) = when (task) {
        AiTask.TRANSCRIBE_AUDIO -> "Розшифровка голосових"
        AiTask.SUMMARIZE_THREAD -> "Резюмування тредів"
        AiTask.EXTRACT_ACTIONS -> "Вилучення задач і дат"
        AiTask.SMART_REPLIES -> "Розумні відповіді"
        AiTask.SEMANTIC_INDEX -> "Семантичний індекс"
    }

    private fun modeLabel(mode: ExecutionMode) = when (mode) {
        ExecutionMode.LOCAL_ONLY -> "лише локально"
        ExecutionMode.LOCAL_FIRST -> "локально (хмара як запас)"
        ExecutionMode.CLOUD_FIRST -> "хмара (локально як запас)"
        ExecutionMode.CLOUD_ONLY -> "лише хмара"
        ExecutionMode.DISABLED -> "вимкнено"
    }

    // --- small programmatic-ui helpers -------------------------------------

    private fun header(text: String): TextView = TextView(this).apply {
        setText(text)
        setTextColor(Color.BLACK)
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(20), 0, px(6))
    }

    private fun caption(text: String): TextView = TextView(this).apply {
        setText(text)
        setTextColor(Color.DKGRAY)
        textSize = 13f
        setPadding(0, px(4), 0, px(8))
    }

    private fun button(text: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            setText(text)
            setBackgroundColor(color)
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }

    private fun rowButton(label: String, value: String, action: () -> Unit): Button =
        button("$label: $value", 0xFF444444.toInt(), action)

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun start(context: android.content.Context) {
            context.startActivity(
                android.content.Intent(context, MiogramAiSettingsActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
