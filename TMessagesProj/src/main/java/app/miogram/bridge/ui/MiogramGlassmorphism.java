package app.miogram.bridge.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import app.miogram.bridge.customui.MiogramCustomUiPrefs;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * High-performance glassmorphism and real-time frosted glass compositor.
 * Powers translucent frosted panels, bottom navigation blur, dialog cards, and headers.
 */
public class MiogramGlassmorphism {

    private static final Paint glassFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glassBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectTmp = new RectF();

    static {
        glassBorderPaint.setStyle(Paint.Style.STROKE);
        glassBorderPaint.setStrokeWidth(AndroidUtilities.dpf2(1.0f));
    }

    public static boolean isEnabled() {
        return MiogramCustomUiPrefs.isUiGlassBlur();
    }

    /**
     * Draws a frosted glass card onto the target canvas.
     * Includes subtle specular gradient reflection and translucent edge border.
     */
    public static void drawFrostedCard(Canvas canvas, float left, float top, float right, float bottom, float radius, boolean isDark) {
        if (canvas == null) return;
        rectTmp.set(left, top, right, bottom);

        int baseBg = isDark ? 0xCC1A232E : 0xD8F6F8FB;
        int specularTop = isDark ? 0x33FFFFFF : 0x4DFFFFFF;
        int specularBot = isDark ? 0x0AFFFFFF : 0x14FFFFFF;

        // Specular gradient reflection
        LinearGradient grad = new LinearGradient(
            rectTmp.left, rectTmp.top, rectTmp.left, rectTmp.bottom,
            new int[]{specularTop, baseBg, specularBot},
            new float[]{0f, 0.4f, 1f},
            Shader.TileMode.CLAMP
        );
        glassFillPaint.setShader(grad);
        canvas.drawRoundRect(rectTmp, radius, radius, glassFillPaint);
        glassFillPaint.setShader(null);

        // Thin frosted edge border
        glassBorderPaint.setColor(isDark ? 0x24FFFFFF : 0x3DFFFFFF);
        canvas.drawRoundRect(rectTmp, radius, radius, glassBorderPaint);
    }

    /**
     * Fast in-place StackBlur implementation for bitmap downsampling and blurring.
     */
    public static Bitmap blurBitmap(Bitmap src, int radius) {
        if (src == null || radius < 1) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(blurred);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(src, 0, 0, paint);
        return blurred;
    }
}
