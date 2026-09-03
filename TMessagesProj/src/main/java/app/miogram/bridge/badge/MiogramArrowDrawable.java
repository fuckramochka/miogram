package app.miogram.bridge.badge;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

/**
 * Iconic Miogram Founder & Supporter Badge — Needy Streamer Overload (Ame-chan / KAngel) Kawaii Edition.
 * Features a cute winged angel heart with floating halo (ʚ♡ɞ + 😇), CRT chromatic glitch shadows,
 * animated gentle floating / breathing movement, and shimmering ✦ starlight particles.
 */
public class MiogramArrowDrawable extends Drawable {

    private static final int ANIMATION_DURATION_MS = 2400;

    // 6 Starlight Sparkle Particle definitions: {relX, relY, phaseOffset}
    private static final float[][] PARTICLES = {
            {-46f, -22f, 0.10f},
            { 46f, -24f, 0.50f},
            {-38f,  24f, 0.75f},
            { 38f,  22f, 0.30f},
            {-16f, -32f, 0.60f},
            { 18f,  28f, 0.90f}
    };

    // Heart paints
    private final Paint heartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint heartStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint heartGlitchCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint heartGlitchPinkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Wing paints
    private final Paint wingFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wingStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wingGlitchCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wingGlitchPinkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Halo paints
    private final Paint haloGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Sparkle particle paints
    private final Paint particleStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particleCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Paths
    private final Path heartPath = new Path();
    private final Path leftWingPath = new Path();
    private final Path rightWingPath = new Path();
    private final RectF haloRect = new RectF();

    private final int size;
    private int lastWidth = -1;
    private int lastHeight = -1;

    // Animation state
    private long lastDrawTime;
    private boolean isRunning;

    private final Runnable nextFrameRunnable = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.uptimeMillis() - lastDrawTime < 350) {
                invalidateSelf();
                AndroidUtilities.runOnUIThread(this, 30);
            } else {
                isRunning = false;
            }
        }
    };

    public MiogramArrowDrawable() {
        this(16);
    }

    public MiogramArrowDrawable(int sizeDp) {
        this.size = AndroidUtilities.dp(sizeDp);
        setBounds(0, 0, size, size);

        // Heart
        heartPaint.setStyle(Paint.Style.FILL);
        heartStrokePaint.setStyle(Paint.Style.STROKE);
        heartStrokePaint.setColor(0xDDFFB6D9);

        heartGlitchCyanPaint.setStyle(Paint.Style.FILL);
        heartGlitchCyanPaint.setColor(0xAA00F0FF);

        heartGlitchPinkPaint.setStyle(Paint.Style.FILL);
        heartGlitchPinkPaint.setColor(0xAAFF2A93);

        shinePaint.setStyle(Paint.Style.FILL);
        shinePaint.setColor(0xFFFFFFFF);

        // Wings
        wingFillPaint.setStyle(Paint.Style.FILL);
        wingFillPaint.setColor(0xF0F5FAFF);

        wingStrokePaint.setStyle(Paint.Style.STROKE);
        wingStrokePaint.setColor(0xD000F0FF);

        wingGlitchCyanPaint.setStyle(Paint.Style.FILL);
        wingGlitchCyanPaint.setColor(0x9900F0FF);

        wingGlitchPinkPaint.setStyle(Paint.Style.FILL);
        wingGlitchPinkPaint.setColor(0x99FF2A93);

        // Halo
        haloGlowPaint.setStyle(Paint.Style.STROKE);
        haloGlowPaint.setColor(0x8000F0FF);

        haloCorePaint.setStyle(Paint.Style.STROKE);
        haloCorePaint.setColor(0xFFFFFFFF);

        // Particles
        particleStarPaint.setStyle(Paint.Style.STROKE);
        particleStarPaint.setStrokeCap(Paint.Cap.ROUND);
        particleStarPaint.setColor(0xFFFFFFFF);

        particleCorePaint.setStyle(Paint.Style.FILL);
        particleCorePaint.setColor(0xFF00F0FF);
    }

    @Override
    public int getIntrinsicWidth() {
        return size;
    }

    @Override
    public int getIntrinsicHeight() {
        return size;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        updateDimensionsAndShaders(bounds);
    }

    private void updateDimensionsAndShaders(Rect bounds) {
        int w = bounds.width();
        int h = bounds.height();
        if (w <= 0 || h <= 0) return;

        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;

            float s = w / 128f;
            heartStrokePaint.setStrokeWidth(Math.max(1f, 1.0f * s));
            wingStrokePaint.setStrokeWidth(Math.max(1f, 1.2f * s));
            haloGlowPaint.setStrokeWidth(Math.max(1.5f, 3.2f * s));
            haloCorePaint.setStrokeWidth(Math.max(1f, 1.8f * s));
            particleStarPaint.setStrokeWidth(Math.max(1f, 1.0f * s));

            // Ame-chan Signature Heart Gradient: Hot Pink -> Tenshi Lilac
            Shader heartShader = new LinearGradient(
                    bounds.centerX(), bounds.centerY() - 16f * s,
                    bounds.centerX(), bounds.centerY() + 20f * s,
                    new int[]{0xFFFF2A93, 0xFFB872FF},
                    new float[]{0.0f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            heartPaint.setShader(heartShader);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        if (lastWidth != bounds.width() || lastHeight != bounds.height()) {
            updateDimensionsAndShaders(bounds);
        }

        long now = SystemClock.uptimeMillis();
        lastDrawTime = now;
        if (!isRunning) {
            isRunning = true;
            AndroidUtilities.runOnUIThread(nextFrameRunnable, 30);
        }

        float phase = (now % ANIMATION_DURATION_MS) / (float) ANIMATION_DURATION_MS;
        double angle = phase * 2.0 * Math.PI;

        float s = bounds.width() / 128f;
        float cx = bounds.centerX();
        float cy = bounds.centerY() + 3f * s;

        // Subtle, alive movement
        float hoverY = (float) Math.sin(angle) * 2.2f * s;
        float scale = 1.0f + (float) Math.sin(angle) * 0.025f;
        float flap = (float) Math.sin(angle) * 2.0f * s;
        float glitchOffset = Math.max(AndroidUtilities.dpf2(0.6f), s * 1.5f);

        canvas.save();

        // 1. Draw Floating Halo above heart
        float haloY = cy - 27f * scale * s + hoverY + (float) Math.sin(angle + 0.2) * 0.8f * s;
        float haloRx = 17f * scale * s;
        float haloRy = 5f * scale * s;
        haloRect.set(cx - haloRx, haloY - haloRy, cx + haloRx, haloY + haloRy);
        canvas.drawOval(haloRect, haloGlowPaint);
        canvas.drawOval(haloRect, haloCorePaint);

        // 2. Draw Feathered Angel Wings (Left & Right)
        buildWingPath(leftWingPath, cx, cy + hoverY, -1, scale, s, flap);
        buildWingPath(rightWingPath, cx, cy + hoverY, 1, scale, s, flap);

        // Wing CRT Glitch Channels
        canvas.save();
        canvas.translate(-glitchOffset, -glitchOffset * 0.7f);
        canvas.drawPath(leftWingPath, wingGlitchCyanPaint);
        canvas.drawPath(rightWingPath, wingGlitchCyanPaint);
        canvas.restore();

        canvas.save();
        canvas.translate(glitchOffset, glitchOffset * 0.7f);
        canvas.drawPath(leftWingPath, wingGlitchPinkPaint);
        canvas.drawPath(rightWingPath, wingGlitchPinkPaint);
        canvas.restore();

        // Core Wings
        canvas.drawPath(leftWingPath, wingFillPaint);
        canvas.drawPath(leftWingPath, wingStrokePaint);
        canvas.drawPath(rightWingPath, wingFillPaint);
        canvas.drawPath(rightWingPath, wingStrokePaint);

        // 3. Draw Kawaii Ame-chan Heart
        buildHeartPath(heartPath, cx, cy + hoverY, scale, s);

        // Heart CRT Glitch Channels
        canvas.save();
        canvas.translate(-glitchOffset * 1.2f, -glitchOffset * 0.8f);
        canvas.drawPath(heartPath, heartGlitchCyanPaint);
        canvas.restore();

        canvas.save();
        canvas.translate(glitchOffset * 1.2f, glitchOffset * 0.8f);
        canvas.drawPath(heartPath, heartGlitchPinkPaint);
        canvas.restore();

        // Core Heart Fill & Stroke
        canvas.drawPath(heartPath, heartPaint);
        canvas.drawPath(heartPath, heartStrokePaint);

        // Glossy Specular Highlights on Heart
        canvas.drawCircle(cx - 7f * scale * s, cy + hoverY - 8f * scale * s, 3.2f * scale * s, shinePaint);
        canvas.drawCircle(cx + 4f * scale * s, cy + hoverY - 7f * scale * s, 1.6f * scale * s, shinePaint);

        // 4. Draw Shimmering Sparkle Particles (✦)
        for (float[] p : PARTICLES) {
            float pT = (phase + p[2]) % 1.0f;
            float pAlphaProgress = (float) Math.sin(pT * Math.PI);
            int pAlpha = (int) (pAlphaProgress * 255);
            if (pAlpha <= 0) continue;

            float px = cx + p[0] * s;
            float py = cy + hoverY + p[1] * s - pT * 6f * s; // drifting upwards
            float pSize = (2.2f + 1.4f * pAlphaProgress) * s;

            particleStarPaint.setAlpha(pAlpha);
            particleCorePaint.setAlpha(pAlpha);

            // 4-point sparkle cross
            canvas.drawLine(px, py - pSize * 2f, px, py + pSize * 2f, particleStarPaint);
            canvas.drawLine(px - pSize * 2f, py, px + pSize * 2f, py, particleStarPaint);
            canvas.drawCircle(px, py, pSize * 0.7f, particleCorePaint);
        }

        canvas.restore();
    }

    private static void buildWingPath(Path path, float cx, float cy, int sign, float scale, float s, float flap) {
        path.reset();
        path.moveTo(cx + sign * 10f * scale * s, cy - 4f * scale * s);
        path.lineTo(cx + sign * 28f * scale * s, cy - 19f * scale * s - flap);
        path.lineTo(cx + sign * 52f * scale * s, cy - 17f * scale * s - flap);
        path.lineTo(cx + sign * 58f * scale * s, cy - 8f * scale * s - flap * 0.7f);
        path.lineTo(cx + sign * 46f * scale * s, cy - 1f * scale * s);
        path.lineTo(cx + sign * 50f * scale * s, cy + 4f * scale * s);
        path.lineTo(cx + sign * 38f * scale * s, cy + 8f * scale * s);
        path.lineTo(cx + sign * 36f * scale * s, cy + 14f * scale * s);
        path.lineTo(cx + sign * 20f * scale * s, cy + 12f * scale * s);
        path.lineTo(cx + sign * 10f * scale * s, cy + 5f * scale * s);
        path.close();
    }

    private static void buildHeartPath(Path path, float cx, float cy, float scale, float s) {
        path.reset();
        float r = 1.18f * scale * s;
        int steps = 32;
        for (int i = 0; i <= steps; i++) {
            double theta = i * 2.0 * Math.PI / steps;
            float sinT = (float) Math.sin(theta);
            float cosT = (float) Math.cos(theta);
            float cos2T = (float) Math.cos(2.0 * theta);
            float cos3T = (float) Math.cos(3.0 * theta);
            float cos4T = (float) Math.cos(4.0 * theta);

            float x = cx + r * 15f * (sinT * sinT * sinT);
            float y = cy - r * (12f * cosT - 4.5f * cos2T - 2f * cos3T - cos4T);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    @Override
    public void setAlpha(int alpha) {
        heartPaint.setAlpha(alpha);
        heartStrokePaint.setAlpha((int) (alpha * 0.85f));
        wingFillPaint.setAlpha((int) (alpha * 0.95f));
        wingStrokePaint.setAlpha((int) (alpha * 0.85f));
        haloGlowPaint.setAlpha((int) (alpha * 0.50f));
        haloCorePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        // Aesthetic Ame-chan palette is preserved by default
        heartPaint.setColorFilter(colorFilter);
        wingFillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
