package app.miogram.bridge.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.miogram.bridge.passcode.MiogramLockFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vault management surface: create/replace the REAL passcode and optional
 * Duress passcode, lock immediately, or destroy the vault.
 *
 * Intentionally a plain standalone Activity with programmatic UI:
 * security-critical input lives outside the shared settings framework,
 * FLAG_SECURE by default, zero resource footprint for upstream merges.
 */
class MiogramVaultSetupActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var statusView: TextView
    private lateinit var realPinInput: EditText
    private lateinit var realPinConfirmInput: EditText
    private lateinit var duressPinInput: EditText
    private lateinit var duressPinConfirmInput: EditText
    private lateinit var applyButton: Button
    private lateinit var wipeButton: Button

    private var wipeArmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val padding = (16 * resources.displayMetrics.density).toInt()

        fun field(hint: String): EditText = EditText(this).apply {
            setHint(hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 18f
        }

        fun label(text: String): TextView = TextView(this).apply {
            setText(text)
            setTextColor(Color.GRAY)
            setPadding(0, padding, 0, padding / 2)
        }

        fun button(text: String, color: Int, action: () -> Unit): Button = Button(this).apply {
            setText(text)
            setBackgroundColor(color)
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }

        statusView = TextView(this).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(padding, padding, padding, padding)
        }

        realPinInput = field("Реальний PIN (мін. 4 цифри)")
        realPinConfirmInput = field("Повторіть реальний PIN")
        duressPinInput = field("Тривожний PIN (опційно)")
        duressPinConfirmInput = field("Повторіть тривожний PIN")

        applyButton = button("Зберегти сховище", 0xFF2E6DA4.toInt()) { applyVault() }
        val lockButton = button("Заблокувати зараз", 0xFF444444.toInt()) {
            scope.launch {
                MiogramLockFacade.lock()
                toast("Ключі сесії стерті з ОЗП")
                refreshStatus()
            }
        }
        wipeButton = button("Знищити сховище", 0xFF9C2B2B.toInt()) {
            if (!wipeArmed) {
                wipeArmed = true
                wipeButton.setText("Торкніться ще раз для підтвердження")
                scheduleWipeDisarm()
            } else {
                wipeArmed = false
                wipeButton.setText("Знищити сховище")
                destroyVault()
            }
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(statusView)
            addView(label("СПРАВЖНІЙ ПРОФІЛЬ"))
            addView(realPinInput)
            addView(realPinConfirmInput)
            addView(label("ТРИВОЖНИЙ ПРОФІЛЬ — відкриває нейтральний простір"))
            addView(duressPinInput)
            addView(duressPinConfirmInput)
            addView(applyButton, linearParams())
            addView(lockButton, linearParams())
            addView(wipeButton, linearParams())
            addView(
                TextView(this@MiogramVaultSetupActivity).apply {
                    setText(
                        "Доки існує сховище, біометричне розблокування вимкнене, а " +
                                "старий пароль програми більше не відкриває застосунок."
                    )
                    setTextColor(Color.GRAY)
                    setPadding(0, padding, 0, 0)
                    textSize = 13f
                }
            )
        }

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(0xFFF2F2F2.toInt())
                addView(form)
            }
        )

        refreshStatus()
    }

    private fun linearParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = (12 * resources.displayMetrics.density).toInt() }

    private fun scheduleWipeDisarm() {
        window.decorView.postDelayed({
            if (wipeArmed) {
                wipeArmed = false
                wipeButton.setText("Знищити сховище")
            }
        }, WIPE_DISARM_DELAY_MS)
    }

    private fun applyVault() {
        val real = realPinInput.text?.toString().orEmpty()
        val realConfirm = realPinConfirmInput.text?.toString().orEmpty()
        val duress = duressPinInput.text?.toString().orEmpty()
        val duressConfirm = duressPinConfirmInput.text?.toString().orEmpty()

        if (real.length < MIN_PIN_LENGTH) return toast("Реальний PIN занадто короткий")
        if (real != realConfirm) return toast("Реальні PIN не збігаються")
        if (duress.isNotEmpty() && duress.length < MIN_PIN_LENGTH) return toast("Тривожний PIN занадто короткий")
        if (duress.isNotEmpty() && duress == real) return toast("Тривожний PIN має відрізнятися від реального")
        if (duress.isNotEmpty() && duress != duressConfirm) return toast("Тривожні PIN не збігаються")

        applyButton.isEnabled = false
        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if (!MiogramLockFacade.isConfigured()) {
                        MiogramLockFacade.setup(real.toCharArray(), duress.ifEmpty { null }?.toCharArray())
                    } else {
                        // Re-keying an existing vault requires its current real PIN;
                        // handled through changePasscodes in a dedicated flow.
                        throw IllegalStateException("Сховище вже налаштоване; використайте зміну PIN")
                    }
                }
                realPinInput.setText("")
                realPinConfirmInput.setText("")
                duressPinInput.setText("")
                duressPinConfirmInput.setText("")
                toast("Сховище збережено")
                refreshStatus()
            } catch (e: Exception) {
                toast("Помилка: ${e.message}")
            } finally {
                applyButton.isEnabled = true
            }
        }
    }

    private fun destroyVault() {
        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    MiogramLockFacade.wipeAll()
                }
                toast("Сховище знищено")
                refreshStatus()
            } catch (e: Exception) {
                toast("Помилка: ${e.message}")
            }
        }
    }

    private fun refreshStatus() {
        val configured = MiogramLockFacade.isConfigured()
        val decoys = if (configured) MiogramLockFacade.hasDuressProfiles() else false
        statusView.text = buildString {
            append("Статус: ")
            append(if (configured) "сховище активне" else "немає сховища")
            if (configured) append(", тривожні профілі: ").append(if (decoys) "так" else "ні")
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val MIN_PIN_LENGTH = 4
        private const val WIPE_DISARM_DELAY_MS = 4000L

        fun start(context: android.content.Context) {
            context.startActivity(
                android.content.Intent(context, MiogramVaultSetupActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
