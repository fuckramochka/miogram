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
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        int w = bounds.width();
        int h = bounds.height();
        if (w <= 0 || h <= 0) return;

        long now = SystemClock.uptimeMillis();
        lastDrawTime = now;
        if (!isRunning) {
            isRunning = true;
            AndroidUtilities.runOnUIThread(nextFrameRunnable, 30);
        }

        float phase = (now % ANIMATION_DURATION_MS) / (float) ANIMATION_DURATION_MS;
        double angle = phase * 2.0 * Math.PI;

        // Discrete stepped pixel dimensions (28x22 pixel grid)
        final float GRID_W = 28f;
        final float GRID_H = 22f;
        float px = w / GRID_W;
        float py = h / GRID_H;

        // Discrete retro bobbing (stepped pixel hops)
        float bobY = (float) Math.round(Math.sin(angle) * 1.2) * py;

        canvas.save();
        canvas.translate(bounds.left, bounds.top + bobY);

        // 1. CRT Chromatic Glitch Shadows (Cyan shifted -1px, Pink shifted +1px)
        float glitchPx = Math.max(1f, px * 0.9f);

        // Glitch Cyan
        canvas.save();
        canvas.translate(-glitchPx, 0);
        drawPixelWings(canvas, px, py, wingGlitchCyanPaint);
        drawPixelHeart(canvas, px, py, heartGlitchCyanPaint);
        canvas.restore();

        // Glitch Magenta/Pink
        canvas.save();
        canvas.translate(glitchPx, 0);
        drawPixelWings(canvas, px, py, wingGlitchPinkPaint);
        drawPixelHeart(canvas, px, py, heartGlitchPinkPaint);
        canvas.restore();

        // 2. Halo (Floating Pixel Ring)
        float haloHover = (float) Math.round(Math.sin(angle + 0.3) * 0.8) * py;
        canvas.save();
        canvas.translate(0, haloHover);
        drawPixelHalo(canvas, px, py);
        canvas.restore();

        // 3. Pixel Angel Wings
        drawPixelWings(canvas, px, py, wingFillPaint);
        drawPixelWingsOutline(canvas, px, py, wingStrokePaint);

        // 4. Pixel Ame-chan Heart
        drawPixelHeart(canvas, px, py, heartPaint);
        drawPixelHeartOutline(canvas, px, py, heartStrokePaint);

        // 5. Specular Pixel Highlights
        canvas.drawRect(10 * px, 9 * py, 12 * px, 11 * py, shinePaint);
        canvas.drawRect(16 * px, 10 * py, 17 * px, 11 * py, shinePaint);

        // 6. Twinkling ✦ Pixel Sparkles
        drawPixelSparkles(canvas, px, py, phase);

        canvas.restore();
    }

    private void drawPixelHalo(Canvas canvas, float px, float py) {
        // Halo top
        canvas.drawRect(11 * px, 2 * py, 17 * px, 3 * py, haloCorePaint);
        // Halo sides
        canvas.drawRect(9 * px, 3 * py, 11 * px, 4 * py, haloCorePaint);
        canvas.drawRect(17 * px, 3 * py, 19 * px, 4 * py, haloCorePaint);
        // Halo bottom
        canvas.drawRect(11 * px, 4 * py, 17 * px, 5 * py, haloCorePaint);

        // Halo outer cyan glow
        canvas.drawRect(10 * px, 1 * py, 18 * px, 2 * py, haloGlowPaint);
        canvas.drawRect(8 * px, 2 * py, 9 * px, 5 * py, haloGlowPaint);
        canvas.drawRect(19 * px, 2 * py, 20 * px, 5 * py, haloGlowPaint);
        canvas.drawRect(10 * px, 5 * py, 18 * px, 6 * py, haloGlowPaint);
    }

    private void drawPixelWings(Canvas canvas, float px, float py, Paint paint) {
        // Left Wing Rows
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paint);
        canvas.drawRect(3 * px, 6 * py, 10 * px, 7 * py, paint);
        canvas.drawRect(2 * px, 7 * py, 11 * px, 8 * py, paint);
        canvas.drawRect(1 * px, 8 * py, 11 * px, 9 * py, paint);
        canvas.drawRect(2 * px, 9 * py, 11 * px, 10 * py, paint);
        canvas.drawRect(3 * px, 10 * py, 10 * px, 11 * py, paint);
        canvas.drawRect(4 * px, 11 * py, 9 * px, 12 * py, paint);
        canvas.drawRect(6 * px, 12 * py, 9 * px, 13 * py, paint);

        // Right Wing Rows (Mirrored: x -> 27 - x)
        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paint);
        canvas.drawRect(18 * px, 6 * py, 25 * px, 7 * py, paint);
        canvas.drawRect(17 * px, 7 * py, 26 * px, 8 * py, paint);
        canvas.drawRect(17 * px, 8 * py, 27 * px, 9 * py, paint);
        canvas.drawRect(17 * px, 9 * py, 26 * px, 10 * py, paint);
        canvas.drawRect(18 * px, 10 * py, 25 * px, 11 * py, paint);
        canvas.drawRect(19 * px, 11 * py, 24 * px, 12 * py, paint);
        canvas.drawRect(19 * px, 12 * py, 22 * px, 13 * py, paint);
    }

    private void drawPixelWingsOutline(Canvas canvas, float px, float py, Paint paint) {
        // Left Wing Tips/Outline
        canvas.drawRect(3 * px, 5 * py, 4 * px, 6 * py, paint);
        canvas.drawRect(1 * px, 7 * py, 2 * px, 9 * py, paint);
        canvas.drawRect(2 * px, 9 * py, 3 * px, 10 * py, paint);
        canvas.drawRect(3 * px, 10 * py, 4 * px, 11 * py, paint);
        canvas.drawRect(5 * px, 12 * py, 6 * px, 13 * py, paint);

        // Right Wing Tips/Outline
        canvas.drawRect(24 * px, 5 * py, 25 * px, 6 * py, paint);
        canvas.drawRect(26 * px, 7 * py, 27 * px, 9 * py, paint);
        canvas.drawRect(25 * px, 9 * py, 26 * px, 10 * py, paint);
        canvas.drawRect(24 * px, 10 * py, 25 * px, 11 * py, paint);
        canvas.drawRect(22 * px, 12 * py, 23 * px, 13 * py, paint);
    }

    private void drawPixelHeart(Canvas canvas, float px, float py, Paint paint) {
        // Heart Rows
        canvas.drawRect(10 * px, 8 * py, 13 * px, 9 * py, paint);
        canvas.drawRect(15 * px, 8 * py, 18 * px, 9 * py, paint);
        canvas.drawRect(9 * px, 9 * py, 19 * px, 10 * py, paint);
        canvas.drawRect(8 * px, 10 * py, 20 * px, 11 * py, paint);
        canvas.drawRect(8 * px, 11 * py, 20 * px, 12 * py, paint);
        canvas.drawRect(9 * px, 12 * py, 19 * px, 13 * py, paint);
        canvas.drawRect(10 * px, 13 * py, 18 * px, 14 * py, paint);
        canvas.drawRect(11 * px, 14 * py, 17 * px, 15 * py, paint);
        canvas.drawRect(12 * px, 15 * py, 16 * px, 16 * py, paint);
        canvas.drawRect(13 * px, 16 * py, 15 * px, 17 * py, paint);
    }

    private void drawPixelHeartOutline(Canvas canvas, float px, float py, Paint paint) {
        // Heart Edge Highlights (Kawaii Pastel Outline)
        canvas.drawRect(10 * px, 7 * py, 13 * px, 8 * py, paint);
        canvas.drawRect(15 * px, 7 * py, 18 * px, 8 * py, paint);
        canvas.drawRect(7 * px, 10 * py, 8 * px, 12 * py, paint);
        canvas.drawRect(20 * px, 10 * py, 21 * px, 12 * py, paint);
        canvas.drawRect(13 * px, 17 * py, 15 * px, 18 * py, paint);
    }

    private void drawPixelSparkles(Canvas canvas, float px, float py, float phase) {
        // 4 Starlight Pixel Crosses (✦)
        float[][] sparks = {
                {3f, 3f, 0.10f},
                {24f, 4f, 0.55f},
                {3f, 17f, 0.80f},
                {24f, 18f, 0.35f}
        };

        for (float[] sp : sparks) {
            float t = (phase + sp[2]) % 1.0f;
            float alphaProgress = (float) Math.sin(t * Math.PI);
            int alpha = (int) (alphaProgress * 255);
            if (alpha <= 10) continue;

            float sx = sp[0] * px;
            float sy = sp[1] * py;

            particleStarPaint.setAlpha(alpha);
            particleCorePaint.setAlpha(alpha);

            // 4-point pixel star cross
            canvas.drawRect(sx, sy - py, sx + px, sy + 2 * py, particleStarPaint);
            canvas.drawRect(sx - px, sy, sx + 2 * px, sy + py, particleStarPaint);
            canvas.drawRect(sx, sy, sx + px, sy + py, particleCorePaint);
        }
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
        heartPaint.setColorFilter(colorFilter);
        wingFillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
