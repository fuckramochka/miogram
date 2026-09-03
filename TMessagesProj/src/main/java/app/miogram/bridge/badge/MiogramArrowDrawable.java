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
 * Iconic Miogram Arrow (Стрілка Miogram) — prestigious founder and supporter badge.
 * Features an authentic glowing Neon Violet to Cyan gradient chevron.
 */
public class MiogramArrowDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int size;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public MiogramArrowDrawable() {
        this(16);
    }

    public MiogramArrowDrawable(int sizeDp) {
        this.size = AndroidUtilities.dp(sizeDp);
        setBounds(0, 0, size, size);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(AndroidUtilities.dpf2(2.4f));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);
        glowPaint.setStrokeWidth(AndroidUtilities.dpf2(3.6f));
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
        updatePathAndShader(bounds);
    }

    private void updatePathAndShader(Rect bounds) {
        int w = bounds.width();
        int h = bounds.height();
        if (w <= 0 || h <= 0) return;

        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;

            Shader shader = new LinearGradient(
                    bounds.left, bounds.top,
                    bounds.right, bounds.bottom,
                    new int[]{0xFF9D4EDD, 0xFF7928CA, 0xFF00F0FF},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            paint.setShader(shader);

            Shader glowShader = new LinearGradient(
                    bounds.left, bounds.top,
                    bounds.right, bounds.bottom,
                    new int[]{0x559D4EDD, 0x557928CA, 0x5500F0FF},
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glowShader);
        }

        path.reset();
        // Draw modern sleek right-pointing chevron: >
        float left = bounds.left + w * 0.32f;
        float top = bounds.top + h * 0.22f;
        float midX = bounds.left + w * 0.72f;
        float midY = bounds.top + h * 0.50f;
        float bottom = bounds.top + h * 0.78f;

        path.moveTo(left, top);
        path.lineTo(midX, midY);
        path.lineTo(left, bottom);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (lastWidth != bounds.width() || lastHeight != bounds.height()) {
            updatePathAndShader(bounds);
        }
        // Subtle ambient glow
        canvas.drawPath(path, glowPaint);
        // Core crisp neon arrow
        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        glowPaint.setAlpha(alpha / 3);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        glowPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
