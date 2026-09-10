package app.miogram.bridge.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/**
 * Live Subtle Mini-Bass & Audio Pulse Visualizer:
 * - Subtly pulsates with audio amplitude on the interface edges
 * - Adds a living, reactive aesthetic to the music player and chat UI
 */
public class MiogramBassVisualizer extends View {

    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private float[] currentAmplitudes = new float[16];
    private float bassLevel = 0f;
    private float animatedBass = 0f;
    private long lastUpdateMs = 0L;

    public MiogramBassVisualizer(Context context) {
        super(context);
        init();
    }

    private void init() {
        wavePaint.setStyle(Paint.Style.FILL);
    }

    public void updateAmplitudes(float[] values) {
        if (values != null && values.length > 0) {
            float sum = 0f;
            int count = Math.min(values.length, currentAmplitudes.length);
            for (int i = 0; i < count; i++) {
                currentAmplitudes[i] = values[i];
                if (i < 4) sum += values[i]; // Low-frequency bass bands
            }
            bassLevel = Math.min(1.0f, (sum / 4.0f) * 1.5f);
            lastUpdateMs = android.os.SystemClock.elapsedRealtime();
            postInvalidateOnAnimation();
        }
    }

    /** Lets bars settle to idle instead of freezing mid-pose on pause. */
    public void decayToIdle() {
        bassLevel = 0f;
        for (int i = 0; i < currentAmplitudes.length; i++) {
            currentAmplitudes[i] *= 0.5f;
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        animatedBass += (bassLevel - animatedBass) * 0.25f;

        // No updates for >500ms (paused/ended): ease everything toward idle.
        if (android.os.SystemClock.elapsedRealtime() - lastUpdateMs > 500) {
            boolean settled = true;
            for (int i = 0; i < currentAmplitudes.length; i++) {
                currentAmplitudes[i] *= 0.85f;
                if (currentAmplitudes[i] > 0.02f) settled = false;
            }
            animatedBass *= 0.85f;
            if (!settled) postInvalidateOnAnimation();
        }

        // Subtle glowing bass edge pulse
        int bars = 16;
        float barWidth = (float) w / (bars * 2 - 1);
        float maxBarHeight = h * 0.7f;
        float corner = AndroidUtilities.dp(2);

        for (int i = 0; i < bars; i++) {
            float amp = currentAmplitudes[i % currentAmplitudes.length];
            float barH = Math.max(AndroidUtilities.dp(3), amp * maxBarHeight * (1.0f + animatedBass * 0.4f));

            float left = i * (barWidth * 2);
            float top = h - barH;
            float right = left + barWidth;

            int alpha = (int) (120 + 135 * Math.min(1.0f, amp + animatedBass * 0.3f));
            wavePaint.setColor(0x00FF77AA | (Math.min(255, alpha) << 24));

            barRect.set(left, top, right, h);
            canvas.drawRoundRect(barRect, corner, corner, wavePaint);
        }
    }
}
