package app.miogram.bridge.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;

import app.miogram.bridge.MiogramFlags;

/**
 * True Ultra-Glass & Liquid Frosted Glassmorphism Engine for Miogram.
 * Renders multi-layered frosted glass with specular reflection borders,
 * light sheen gradients, and crystal clarity without distorting text or icons.
 */
public class MiogramGlassEffect {

    private static final Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    static {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(AndroidUtilities.dp(1.2f));
    }

    public static boolean isEnabled() {
        return MiogramFlags.isSpatialDecoration() ||
                MiogramVisualsPrefs.loadBool(ApplicationLoader.applicationContext, "agsl_enabled", false);
    }

    public static float getIntensityFactor() {
        int pct = MiogramVisualsPrefs.loadInt(ApplicationLoader.applicationContext, "liquid_glass_intensity", 60);
        return Math.max(0.1f, pct / 100.0f);
    }

    /**
     * Draws a stunning liquid glassmorphism layer on top of a background surface.
     */
    public static void drawGlassSurface(Canvas canvas, float left, float top, float right, float bottom, float cornerRadius) {
        if (!isEnabled() || canvas == null) return;

        float intensity = getIntensityFactor();
        rectF.set(left, top, right, bottom);
        float width = Math.max(1f, right - left);
        float height = Math.max(1f, bottom - top);

        // 1. Semi-translucent Frosted Glass Tint
        int baseAlpha = (int) (35 + 65 * intensity);
        glassPaint.setColor(Color.argb(baseAlpha, 255, 255, 255));
        if (cornerRadius > 0) {
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glassPaint);
        } else {
            canvas.drawRect(rectF, glassPaint);
        }

        // 2. Diagonal Specular Sheen (Apple VisionOS / Crystal Glass style reflection)
        LinearGradient sheenGradient = new LinearGradient(
                left, top, left + width * 0.7f, top + height,
                new int[]{
                        Color.argb((int) (55 * intensity), 255, 255, 255),
                        Color.argb((int) (15 * intensity), 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP
        );
        sheenPaint.setShader(sheenGradient);
        if (cornerRadius > 0) {
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, sheenPaint);
        } else {
            canvas.drawRect(rectF, sheenPaint);
        }

        // 3. Ultra-refined Specular Luminous Border (Light rim reflection)
        LinearGradient borderGradient = new LinearGradient(
                left, top, left, bottom,
                new int[]{
                        Color.argb((int) (130 * intensity), 255, 255, 255),
                        Color.argb((int) (45 * intensity), 255, 255, 255),
                        Color.argb((int) (12 * intensity), 255, 255, 255)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        borderPaint.setShader(borderGradient);
        if (cornerRadius > 0) {
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);
        } else {
            canvas.drawLine(left, bottom - AndroidUtilities.dp(1), right, bottom - AndroidUtilities.dp(1), borderPaint);
        }
    }
}
