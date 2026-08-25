package app.miogram.bridge.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Neutral placeholder revealed by a successful duress unlock.
 *
 * Этап 4 replaces this with the full decoy workspace (own chat list,
 * seeded channels). Until then the surface is intentionally inert:
 * FLAG_SECURE everywhere, no content access, back returns to the lock.
 */
class MiogramDecoyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF141414.toInt())
        }
        val label = TextView(this).apply {
            text = "Немає повідомлень"
            setTextColor(0xFF8A8A8A.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
        }
        root.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(root)
    }

    companion object {
        @JvmStatic
        fun start(context: android.content.Context) {
            val intent = android.content.Intent(context, MiogramDecoyActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(intent)
        }
    }
}
