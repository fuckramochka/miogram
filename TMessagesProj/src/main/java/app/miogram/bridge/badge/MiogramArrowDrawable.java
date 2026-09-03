package app.miogram.bridge.badge;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

/**
 * Iconic Miogram Arrow (Стрілка Miogram) — Needy Streamer Overload / Ame-chan Edition.
 * Features KAngel's iconic 4-pointed angel sparkle star (✦) coupled with the cyber-kawaii
 * neon chevron (>), chromatic aberration CRT glitch shadows, and Ame hot pink to cyan gradient.
 */
public class MiogramArrowDrawable extends Drawable {

    // Main paints
    private final Paint chevronPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Chromatic aberration glitch paints
    private final Paint glitchCyanStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glitchCyanFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glitchPinkStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glitchPinkFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path chevronPath = new Path();
    private final Path starPath = new Path();
    private final int size;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public MiogramArrowDrawable() {
        this(16);
    }

    public MiogramArrowDrawable(int sizeDp) {
        this.size = AndroidUtilities.dp(sizeDp);
        setBounds(0, 0, size, size);

        // Core Chevron
        chevronPaint.setStyle(Paint.Style.STROKE);
        chevronPaint.setStrokeCap(Paint.Cap.ROUND);
        chevronPaint.setStrokeJoin(Paint.Join.ROUND);

        // Core Star
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(0xFFFFFFFF);

        // Subtle Glow
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);

        // Cyan CRT Glitch Channel
        glitchCyanStrokePaint.setStyle(Paint.Style.STROKE);
        glitchCyanStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        glitchCyanStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        glitchCyanStrokePaint.setColor(0xAA00F0FF);

        glitchCyanFillPaint.setStyle(Paint.Style.FILL);
        glitchCyanFillPaint.setColor(0x8800F0FF);

        // Pink CRT Glitch Channel
        glitchPinkStrokePaint.setStyle(Paint.Style.STROKE);
        glitchPinkStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        glitchPinkStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        glitchPinkStrokePaint.setColor(0xAAFF2A93);

        glitchPinkFillPaint.setStyle(Paint.Style.FILL);
        glitchPinkFillPaint.setColor(0x88FF2A93);
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
        updatePathsAndShaders(bounds);
    }

    private void updatePathsAndShaders(Rect bounds) {
        int w = bounds.width();
        int h = bounds.height();
        if (w <= 0 || h <= 0) return;

        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;

            float strokeW = Math.max(AndroidUtilities.dpf2(1.8f), w * 0.11f);
            chevronPaint.setStrokeWidth(strokeW);
            glitchCyanStrokePaint.setStrokeWidth(strokeW);
            glitchPinkStrokePaint.setStrokeWidth(strokeW);
            glowPaint.setStrokeWidth(strokeW * 1.5f);

            // Ame-chan Signature Palette: Ame Hot Pink -> Tenshi Lilac -> Electric Cyan
            Shader shader = new LinearGradient(
                    bounds.left, bounds.top,
                    bounds.right, bounds.bottom,
                    new int[]{0xFFFF2A93, 0xFFB872FF, 0xFF00F0FF},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            chevronPaint.setShader(shader);

            Shader glowShader = new LinearGradient(
                    bounds.left, bounds.top,
                    bounds.right, bounds.bottom,
                    new int[]{0x55FF2A93, 0x55B872FF, 0x5500F0FF},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glowShader);
        }

        // 1. KAngel 4-pointed Sparkle Star (✦) on the left
        float cxStar = bounds.left + w * 0.30f;
        float cyStar = bounds.top + h * 0.50f;
        float rx = w * 0.20f;
        float ry = h * 0.36f;

        starPath.reset();
        starPath.moveTo(cxStar, cyStar - ry);
        starPath.quadTo(cxStar, cyStar, cxStar + rx, cyStar);
        starPath.quadTo(cxStar, cyStar, cxStar, cyStar + ry);
        starPath.quadTo(cxStar, cyStar, cxStar - rx, cyStar);
        starPath.quadTo(cxStar, cyStar, cxStar, cyStar - ry);
        starPath.close();

        // 2. Cyber-kawaii chevron (>) on the right
        float chevLeft = bounds.left + w * 0.54f;
        float chevMidX = bounds.left + w * 0.88f;
        float chevTop = bounds.top + h * 0.20f;
        float chevMidY = bounds.top + h * 0.50f;
        float chevBot = bounds.top + h * 0.80f;

        chevronPath.reset();
        chevronPath.moveTo(chevLeft, chevTop);
        chevronPath.lineTo(chevMidX, chevMidY);
        chevronPath.lineTo(chevLeft, chevBot);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (lastWidth != bounds.width() || lastHeight != bounds.height()) {
            updatePathsAndShaders(bounds);
        }

        float glitchOffset = Math.max(AndroidUtilities.dpf2(0.6f), bounds.width() * 0.025f);

        // 1. Cyan CRT Glitch layer (offset top-left)
        canvas.save();
        canvas.translate(-glitchOffset, -glitchOffset * 0.7f);
        canvas.drawPath(starPath, glitchCyanFillPaint);
        canvas.drawPath(chevronPath, glitchCyanStrokePaint);
        canvas.restore();

        // 2. Pink CRT Glitch layer (offset bottom-right)
        canvas.save();
        canvas.translate(glitchOffset, glitchOffset * 0.7f);
        canvas.drawPath(starPath, glitchPinkFillPaint);
        canvas.drawPath(chevronPath, glitchPinkStrokePaint);
        canvas.restore();

        // 3. Subtle ambient glow
        canvas.drawPath(chevronPath, glowPaint);

        // 4. Core Ame-chan Chevron
        canvas.drawPath(chevronPath, chevronPaint);

        // 5. Core Tenshi Sparkle Star
        canvas.drawPath(starPath, starPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        chevronPaint.setAlpha(alpha);
        starPaint.setAlpha(alpha);
        glitchCyanStrokePaint.setAlpha((int) (alpha * 0.65f));
        glitchCyanFillPaint.setAlpha((int) (alpha * 0.55f));
        glitchPinkStrokePaint.setAlpha((int) (alpha * 0.65f));
        glitchPinkFillPaint.setAlpha((int) (alpha * 0.55f));
        glowPaint.setAlpha((int) (alpha * 0.35f));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        chevronPaint.setColorFilter(colorFilter);
        starPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
