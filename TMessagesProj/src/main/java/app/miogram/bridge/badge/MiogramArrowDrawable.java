package app.miogram.bridge.badge;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

/**
 * Next-Gen Pixel Art & Shading Renderer for the 10 Canonical Miogram Badges:
 * - Radiant neon bloom & soft atmospheric lighting passes
 * - True feathered stair-step silhouettes from reference design
 * - 6 animated floating ✦ starlight sparkle particles with twinkling phases
 * - High-contrast luminous contours & cute pixel eyes • •
 */
public class MiogramArrowDrawable extends Drawable {

    private static final int ANIMATION_DURATION_MS = 2200;

    private final MiogramBadgeType badgeType;
    private final int size;

    private final Paint paintBloom = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintShade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEyes = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSparkle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSparkleCore = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long lastDrawTime;
    private boolean isRunning;

    // 6 Dynamic Flying Micro-Particles: {baseX, speedFactor, swayFreq, swayAmp, isCross(1f/0f), yOffset}
    private static final float[][] DYNAMIC_PARTICLES = {
            { 2.5f, 0.00035f, 0.08f, 1.2f, 1.0f, 0.05f},
            {24.5f, 0.00042f, 0.07f, 1.2f, 1.0f, 0.45f},
            { 3.5f, 0.00028f, 0.09f, 1.0f, 0.0f, 0.70f},
            {23.5f, 0.00038f, 0.08f, 1.0f, 0.0f, 0.25f},
            {13.5f, 0.00045f, 0.06f, 1.4f, 0.0f, 0.85f},
            {13.5f, 0.00032f, 0.07f, 1.2f, 1.0f, 0.35f}
    };

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
        this(16, MiogramBadgeType.ORIGINAL);
    }

    public MiogramArrowDrawable(int sizeDp) {
        this(sizeDp, MiogramBadgeType.ORIGINAL);
    }

    public MiogramArrowDrawable(int sizeDp, @Nullable MiogramBadgeType type) {
        this.size = AndroidUtilities.dp(sizeDp);
        this.badgeType = (type != null) ? type : MiogramBadgeType.ORIGINAL;
        setBounds(0, 0, size, size);

        paintBloom.setStyle(Paint.Style.FILL);
        paintFill.setStyle(Paint.Style.FILL);
        paintShade.setStyle(Paint.Style.FILL);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintAccent.setStyle(Paint.Style.FILL);
        paintEyes.setStyle(Paint.Style.FILL);
        paintEyes.setColor(Color.WHITE);
        paintSparkle.setStyle(Paint.Style.FILL);
        paintSparkleCore.setStyle(Paint.Style.FILL);
        paintSparkleCore.setColor(Color.WHITE);
    }

    public MiogramBadgeType getBadgeType() {
        return badgeType;
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

        final float GRID_W = 28f;
        final float GRID_H = 22f;
        float px = w / GRID_W;
        float py = h / GRID_H;

        // Gentle floating respiration
        float bobY = (float) Math.round(Math.sin(angle) * 0.9) * py;

        canvas.save();
        canvas.translate(bounds.left, bounds.top + bobY);

        // 1. Radiant Atmospheric Neon Bloom Pass
        drawAtmosphericBloom(canvas, px, py, phase);

        // 2. Badge-Specific Rendering
        switch (badgeType) {
            case PINK:
                drawPinkBadge(canvas, px, py, phase);
                break;
            case CYAN:
                drawCyanBadge(canvas, px, py, phase);
                break;
            case DARK:
                drawDarkBadge(canvas, px, py, phase);
                break;
            case ANGEL:
                drawAngelBadge(canvas, px, py, phase);
                break;
            case DEVIL:
                drawDevilBadge(canvas, px, py, phase);
                break;
            case RAINBOW:
                drawRainbowBadge(canvas, px, py, phase);
                break;
            case OUTLINE:
                drawOutlineBadge(canvas, px, py, phase);
                break;
            case GLITCH:
                drawGlitchBadge(canvas, px, py, phase, now);
                break;
            case PREMIUM:
                drawPremiumBadge(canvas, px, py, phase);
                break;
            case ORIGINAL:
            default:
                drawOriginalBadge(canvas, px, py, phase);
                break;
        }

        // 3. Starlight Sparkle Particle Overlay (✦)
        drawTwinklingSparkles(canvas, px, py, now);

        canvas.restore();
    }

    private void drawAtmosphericBloom(Canvas canvas, float px, float py, float phase) {
        float cx = 14f * px;
        float cy = 11f * py;
        float radius = 13f * px;

        int bloomColor;
        switch (badgeType) {
            case PINK:    bloomColor = 0x40FF2A93; break;
            case CYAN:    bloomColor = 0x4000E5FF; break;
            case DARK:    bloomColor = 0x409D4EDD; break;
            case ANGEL:   bloomColor = 0x35E0AAFF; break;
            case DEVIL:   bloomColor = 0x40FF0055; break;
            case RAINBOW: bloomColor = 0x40FFD166; break;
            case OUTLINE: bloomColor = 0x2800F0FF; break;
            case GLITCH:  bloomColor = 0x3500F0FF; break;
            case PREMIUM: bloomColor = 0x45FFD700; break;
            case ORIGINAL:
            default:      bloomColor = 0x3800F0FF; break;
        }

        RadialGradient gradient = new RadialGradient(
                cx, cy, radius,
                new int[]{bloomColor, Color.TRANSPARENT},
                null,
                Shader.TileMode.CLAMP
        );
        paintBloom.setShader(gradient);
        canvas.drawCircle(cx, cy, radius, paintBloom);
        paintBloom.setShader(null);
    }

    // -------------------------------------------------------------
    // Core Geometry Helpers (1:1 with website/app.js)
    // -------------------------------------------------------------
    private void drawWings(Canvas canvas, float px, float py, int fillColor, int fringeColor) {
        paintFill.setColor(fillColor);
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paintFill);
        canvas.drawRect(3 * px, 6 * py, 9 * px, 7 * py, paintFill);
        canvas.drawRect(1 * px, 7 * py, 9 * px, 10 * py, paintFill);
        canvas.drawRect(2 * px, 10 * py, 8 * px, 11 * py, paintFill);
        canvas.drawRect(4 * px, 11 * py, 7 * px, 13 * py, paintFill);

        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paintFill);
        canvas.drawRect(19 * px, 6 * py, 25 * px, 7 * py, paintFill);
        canvas.drawRect(19 * px, 7 * py, 27 * px, 10 * py, paintFill);
        canvas.drawRect(20 * px, 10 * py, 26 * px, 11 * py, paintFill);
        canvas.drawRect(21 * px, 11 * py, 24 * px, 13 * py, paintFill);

        paintAccent.setColor(fringeColor);
        canvas.drawRect(1 * px, 8 * py, 3 * px, 10 * py, paintAccent);
        canvas.drawRect(25 * px, 8 * py, 27 * px, 10 * py, paintAccent);
        canvas.drawRect(4 * px, 12 * py, 7 * px, 13 * py, paintAccent);
        canvas.drawRect(21 * px, 12 * py, 24 * px, 13 * py, paintAccent);
    }

    private void drawHeart(Canvas canvas, float px, float py, int coreColor, int contourColor) {
        paintFill.setColor(coreColor);
        canvas.drawRect(10 * px, 7 * py, 13 * px, 8 * py, paintFill);
        canvas.drawRect(15 * px, 7 * py, 18 * px, 8 * py, paintFill);
        canvas.drawRect(9 * px, 8 * py, 19 * px, 10 * py, paintFill);
        canvas.drawRect(8 * px, 10 * py, 20 * px, 12 * py, paintFill);
        canvas.drawRect(9 * px, 12 * py, 19 * px, 13 * py, paintFill);
        canvas.drawRect(10 * px, 13 * py, 18 * px, 14 * py, paintFill);
        canvas.drawRect(11 * px, 14 * py, 17 * px, 15 * py, paintFill);
        canvas.drawRect(12 * px, 15 * py, 16 * px, 16 * py, paintFill);
        canvas.drawRect(13 * px, 16 * py, 15 * px, 17 * py, paintFill);

        paintAccent.setColor(contourColor);
        canvas.drawRect(10 * px, 6 * py, 13 * px, 7 * py, paintAccent);
        canvas.drawRect(15 * px, 6 * py, 18 * px, 7 * py, paintAccent);
        canvas.drawRect(8 * px, 8 * py, 9 * px, 12 * py, paintAccent);
        canvas.drawRect(19 * px, 8 * py, 20 * px, 12 * py, paintAccent);
    }

    private void drawEyes(Canvas canvas, float px, float py, int eyeColor) {
        paintEyes.setColor(eyeColor);
        canvas.drawRect(11 * px, 9.5f * py, 12.5f * px, 11 * py, paintEyes);
        canvas.drawRect(15.5f * px, 9.5f * py, 17 * px, 11 * py, paintEyes);
    }

    // -------------------------------------------------------------
    // 01 — ORIGINAL (Classic Winged Heart + Visor)
    // -------------------------------------------------------------
    private void drawOriginalBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        canvas.drawRect(12 * px, 4 * py, 16 * px, 5 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFF0FDFE, 0xFFFF55A3);
        drawHeart(canvas, px, py, 0xFF0F141C, 0xFF00F0FF);
        drawEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 02 — PINK (Neon Pink Style with Chevrons)
    // -------------------------------------------------------------
    private void drawPinkBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFFFF0F7, 0xFFFF2A93);
        drawHeart(canvas, px, py, 0xFF1B0F1C, 0xFFFF2A93);
        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(11 * px, 12 * py, 17 * px, 13 * py, paintAccent);
        drawEyes(canvas, px, py, 0xFFFFE5F0);
    }

    // -------------------------------------------------------------
    // 03 — CYAN (Electric Cyber Winged Heart)
    // -------------------------------------------------------------
    private void drawCyanBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00E5FF);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFE0F7FA, 0xFF00E5FF);
        drawHeart(canvas, px, py, 0xFF0A1822, 0xFF00E5FF);
        drawEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 04 — DARK (Midnight Obsidian & Violet)
    // -------------------------------------------------------------
    private void drawDarkBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF9D4EDD);
        canvas.drawRect(11 * px, 3 * py, 17 * px, 4 * py, paintAccent);
        drawWings(canvas, px, py, 0xFF1B142A, 0xFFC77DFF);
        drawHeart(canvas, px, py, 0xFF120B20, 0xFF9D4EDD);
        drawEyes(canvas, px, py, 0xFFE0AAFF);
    }

    // -------------------------------------------------------------
    // 05 — ANGEL (Halo & Seraphim Wings)
    // -------------------------------------------------------------
    private void drawAngelBadge(Canvas canvas, float px, float py, float phase) {
        // Halo
        paintAccent.setColor(Color.WHITE);
        canvas.drawRect(10 * px, 1 * py, 18 * px, 2 * py, paintAccent);
        canvas.drawRect(8 * px, 2 * py, 10 * px, 3 * py, paintAccent);
        canvas.drawRect(18 * px, 2 * py, 20 * px, 3 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFFAFAFE, 0xFFB8C0EC);
        drawHeart(canvas, px, py, 0xFFC3BEF0, Color.WHITE);
        drawEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 06 — DEVIL (Horns & Bat Wings)
    // -------------------------------------------------------------
    private void drawDevilBadge(Canvas canvas, float px, float py, float phase) {
        // Horns
        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(9 * px, 4 * py, 11 * px, 7 * py, paintAccent);
        canvas.drawRect(8 * px, 3 * py, 10 * px, 5 * py, paintAccent);
        canvas.drawRect(17 * px, 4 * py, 19 * px, 7 * py, paintAccent);
        canvas.drawRect(18 * px, 3 * py, 20 * px, 5 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFFF3377, 0xFFB8003D);
        drawHeart(canvas, px, py, 0xFF1C0A15, 0xFFFF0055);
        drawEyes(canvas, px, py, 0xFFFFB3C6);
    }

    // -------------------------------------------------------------
    // 07 — RAINBOW (Prismatic Rainbow Feathers)
    // -------------------------------------------------------------
    private void drawRainbowBadge(Canvas canvas, float px, float py, float phase) {
        int[] rainbow = new int[]{0xFFFF3377, 0xFF9D4EDD, 0xFF00B4D8, 0xFF06D6A0, 0xFFFFD166};
        for (int i = 0; i < 5; i++) {
            paintAccent.setColor(rainbow[i]);
            canvas.drawRect(4 * px, (5 + i * 1.5f) * py, 9 * px, (6.5f + i * 1.5f) * py, paintAccent);
            canvas.drawRect(19 * px, (5 + i * 1.5f) * py, 24 * px, (6.5f + i * 1.5f) * py, paintAccent);
        }
        drawHeart(canvas, px, py, 0xFF10141E, 0xFFFFD166);
        drawEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 08 — OUTLINE (Minimalist Cyber Wireframe)
    // -------------------------------------------------------------
    private void drawOutlineBadge(Canvas canvas, float px, float py, float phase) {
        paintStroke.setColor(0xFF00F0FF);
        paintStroke.setStrokeWidth(1.5f * px);
        canvas.drawRect(4 * px, 5 * py, 9 * px, 12 * py, paintStroke);
        canvas.drawRect(19 * px, 5 * py, 24 * px, 12 * py, paintStroke);
        canvas.drawRect(9 * px, 7 * py, 19 * px, 16 * py, paintStroke);
        drawEyes(canvas, px, py, 0xFF00F0FF);
    }

    // -------------------------------------------------------------
    // 09 — GLITCH (Chromatic RGB Displacement)
    // -------------------------------------------------------------
    private void drawGlitchBadge(Canvas canvas, float px, float py, float phase, long now) {
        // Magenta Left
        paintAccent.setColor(0xB3FF0055);
        canvas.drawRect(3 * px, 5 * py, 8 * px, 12 * py, paintAccent);
        // Cyan Right
        paintAccent.setColor(0xB300F0FF);
        canvas.drawRect(20 * px, 5 * py, 25 * px, 12 * py, paintAccent);
        drawWings(canvas, px, py, Color.WHITE, 0xFF00F0FF);
        drawHeart(canvas, px, py, 0xFF10121C, 0xFF00F0FF);
        drawEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 10 — PREMIUM (Royal Golden Crown & Armor)
    // -------------------------------------------------------------
    private void drawPremiumBadge(Canvas canvas, float px, float py, float phase) {
        // Crown
        paintAccent.setColor(0xFFFFD700);
        canvas.drawRect(10 * px, 2 * py, 12 * px, 5 * py, paintAccent);
        canvas.drawRect(13 * px, 1 * py, 15 * px, 5 * py, paintAccent);
        canvas.drawRect(16 * px, 2 * py, 18 * px, 5 * py, paintAccent);
        canvas.drawRect(10 * px, 5 * py, 18 * px, 6 * py, paintAccent);
        drawWings(canvas, px, py, 0xFFFFE066, 0xFFCC8800);
        drawHeart(canvas, px, py, 0xFF1B1408, 0xFFFFD700);
        paintAccent.setColor(0xFFFFD700);
        canvas.drawRect(10 * px, 11 * py, 18 * px, 12 * py, paintAccent);
        drawEyes(canvas, px, py, 0xFFFFF5B8);
    }


    private void drawTwinklingSparkles(Canvas canvas, float px, float py, long now) {
        int sparkleColor;
        switch (badgeType) {
            case PINK:    sparkleColor = 0xFFFF2A93; break;
            case CYAN:    sparkleColor = 0xFF00E5FF; break;
            case DARK:    sparkleColor = 0xFFC77DFF; break;
            case ANGEL:   sparkleColor = 0xFFFFFFFF; break;
            case DEVIL:   sparkleColor = 0xFFFF006E; break;
            case RAINBOW: sparkleColor = 0xFFFFD166; break;
            case OUTLINE: sparkleColor = 0xFF00F0FF; break;
            case GLITCH:  sparkleColor = 0xFF00F0FF; break;
            case PREMIUM: sparkleColor = 0xFFFFD700; break;
            case ORIGINAL:
            default:      sparkleColor = 0xFF00F0FF; break;
        }

        paintSparkle.setColor(sparkleColor);

        for (float[] p : DYNAMIC_PARTICLES) {
            float travel = (now * p[1] + p[5]) % 1.0f;
            float y = (1.0f - travel) * 22f;
            float x = p[0] + (float) Math.sin(y * p[2] + now * 0.0025f) * p[3];

            float alphaSin = (float) Math.sin(travel * Math.PI);
            if (alphaSin <= 0.08f) continue;
            int alpha = (int) (alphaSin * 255);

            paintSparkle.setAlpha(alpha);
            paintSparkleCore.setAlpha(alpha);

            float sx = x * px;
            float sy = y * py;

            if (p[4] > 0.5f) {
                // Delicate 4-point micro-star (✦)
                canvas.drawRect(sx, sy - 1.0f * py, sx + px, sy + 2.0f * py, paintSparkle);
                canvas.drawRect(sx - 1.0f * px, sy, sx + 2.0f * px, sy + py, paintSparkle);
                canvas.drawRect(sx, sy, sx + px, sy + py, paintSparkleCore);
            } else {
                // Delicate micro-dot
                canvas.drawRect(sx - 0.5f * px, sy - 0.5f * py, sx + 0.5f * px, sy + 0.5f * py, paintSparkle);
                canvas.drawRect(sx - 0.25f * px, sy - 0.25f * py, sx + 0.25f * px, sy + 0.25f * py, paintSparkleCore);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paintBloom.setAlpha(alpha);
        paintFill.setAlpha(alpha);
        paintShade.setAlpha(alpha);
        paintStroke.setAlpha(alpha);
        paintAccent.setAlpha(alpha);
        paintEyes.setAlpha(alpha);
        paintSparkle.setAlpha(alpha);
        paintSparkleCore.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paintFill.setColorFilter(colorFilter);
        paintAccent.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
