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
 * Authentic Pixel Art & Dynamic Micro-Particle Renderer for the 10 Canonical Miogram Badges:
 * - Prominent, iconic pixel heart in foreground (rounded lobes, deep cleft, standalone lower V-point)
 * - Upward-angled aerodynamic cyber wings (attached to upper flanks, never drooping like butterfly wings)
 * - Dynamic flying micro-sparkles (дрібні частинки, що літають і коливаються в реальному часі)
 * - Asymmetric cute pixel eyes • • (left tall pill, right square dot) for unmistakable mascot personality
 */
public class MiogramArrowDrawable extends Drawable {

    private static final int ANIMATION_DURATION_MS = 2400;

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

    // 8 Dynamic Flying Micro-Particles: {baseX, speedFactor, swayFreq, swayAmp, isCross(1f/0f), yOffset}
    private static final float[][] DYNAMIC_PARTICLES = {
            { 3.5f, 0.00035f, 0.08f, 1.4f, 1.0f, 0.05f},
            {24.5f, 0.00042f, 0.07f, 1.5f, 1.0f, 0.35f},
            { 5.0f, 0.00028f, 0.09f, 1.2f, 0.0f, 0.65f},
            {23.0f, 0.00038f, 0.08f, 1.3f, 0.0f, 0.85f},
            {10.0f, 0.00045f, 0.06f, 1.0f, 0.0f, 0.20f},
            {18.0f, 0.00032f, 0.07f, 1.2f, 1.0f, 0.50f},
            {14.0f, 0.00030f, 0.10f, 1.5f, 0.0f, 0.75f},
            { 2.0f, 0.00040f, 0.08f, 1.2f, 1.0f, 0.90f}
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
        final float GRID_H = 24f;
        float px = w / GRID_W;
        float py = h / GRID_H;

        // Gentle floating respiration hop
        float bobY = (float) Math.round(Math.sin(angle) * 0.9) * py;

        canvas.save();
        canvas.translate(bounds.left, bounds.top + bobY);

        // 1. Radiant Atmospheric Bloom Pass
        drawAtmosphericBloom(canvas, px, py);

        // 2. Dynamic Flying Micro-Particles (Non-static, continuously rising & swaying)
        drawFlyingMicroParticles(canvas, px, py, now);

        // 3. Badge-Specific Rendering (Heart in prominent foreground)
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

        canvas.restore();
    }

    private void drawAtmosphericBloom(Canvas canvas, float px, float py) {
        float cx = 14f * px;
        float cy = 12f * py;
        float radius = 13f * px;

        int bloomColor;
        switch (badgeType) {
            case PINK:    bloomColor = 0x35FF2A93; break;
            case CYAN:    bloomColor = 0x3500E5FF; break;
            case DARK:    bloomColor = 0x359D4EDD; break;
            case ANGEL:   bloomColor = 0x30E0AAFF; break;
            case DEVIL:   bloomColor = 0x35FF0055; break;
            case RAINBOW: bloomColor = 0x35FFD166; break;
            case OUTLINE: bloomColor = 0x2200F0FF; break;
            case GLITCH:  bloomColor = 0x3000F0FF; break;
            case PREMIUM: bloomColor = 0x38FFD700; break;
            case ORIGINAL:
            default:      bloomColor = 0x3000F0FF; break;
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
    // Dynamic Flying Micro-Particles (TINY & NON-STATIC)
    // -------------------------------------------------------------
    private void drawFlyingMicroParticles(Canvas canvas, float px, float py, long now) {
        int primaryColor;
        int secondaryColor;

        switch (badgeType) {
            case PINK:
                primaryColor = 0xFFFF2A93;
                secondaryColor = 0xFFFF85C0;
                break;
            case CYAN:
                primaryColor = 0xFF00E5FF;
                secondaryColor = 0xFFB2EBF2;
                break;
            case DARK:
                primaryColor = 0xFFC77DFF;
                secondaryColor = 0xFF9D4EDD;
                break;
            case ANGEL:
                primaryColor = 0xFFFFFFFF;
                secondaryColor = 0xFFE0AAFF;
                break;
            case DEVIL:
                primaryColor = 0xFFFF0055;
                secondaryColor = 0xFFFF5588;
                break;
            case RAINBOW:
                primaryColor = 0xFFFFD166;
                secondaryColor = 0xFF06D6A0;
                break;
            case OUTLINE:
                primaryColor = 0xFF00F0FF;
                secondaryColor = 0xFFFFFFFF;
                break;
            case GLITCH:
                primaryColor = 0xFF00F0FF;
                secondaryColor = 0xFFFF0055;
                break;
            case PREMIUM:
                primaryColor = 0xFFFFD700;
                secondaryColor = 0xFFFFF275;
                break;
            case ORIGINAL:
            default:
                primaryColor = 0xFF00F0FF;
                secondaryColor = 0xFFFF2A93;
                break;
        }

        for (int i = 0; i < DYNAMIC_PARTICLES.length; i++) {
            float[] p = DYNAMIC_PARTICLES[i];
            float travel = (now * p[1] + p[5]) % 1.0f;
            float y = (1.0f - travel) * 23f;
            float x = p[0] + (float) Math.sin(y * p[2] + now * 0.003f) * p[3];

            float alphaSin = (float) Math.sin(travel * Math.PI);
            if (alphaSin <= 0.08f) continue;
            int alpha = (int) (alphaSin * 255);

            int col = (i % 2 == 0) ? primaryColor : secondaryColor;
            paintSparkle.setColor(col);
            paintSparkle.setAlpha(alpha);
            paintSparkleCore.setAlpha(alpha);

            float cx = x * px;
            float cy = y * py;

            if (p[4] > 0.5f) {
                // Tiny 3x3 micro-cross: 1px center with 1px arms
                canvas.drawRect(cx - 0.35f * px, cy - 1.1f * py, cx + 0.35f * px, cy + 1.1f * py, paintSparkle);
                canvas.drawRect(cx - 1.1f * px, cy - 0.35f * py, cx + 1.1f * px, cy + 0.35f * py, paintSparkle);
                canvas.drawRect(cx - 0.35f * px, cy - 0.35f * py, cx + 0.35f * px, cy + 0.35f * py, paintSparkleCore);
            } else {
                // Tiny 2x2 micro-dot
                canvas.drawRect(cx - 0.6f * px, cy - 0.6f * py, cx + 0.6f * px, cy + 0.6f * py, paintSparkle);
                canvas.drawRect(cx - 0.3f * px, cy - 0.3f * py, cx + 0.3f * px, cy + 0.3f * py, paintSparkleCore);
            }
        }
    }

    // -------------------------------------------------------------
    // 01 — ORIGINAL (Classical Winged Heart + Visor)
    // -------------------------------------------------------------
    private void drawOriginalBadge(Canvas canvas, float px, float py, float phase) {
        // Wireframe Visor Antenna
        paintAccent.setColor(0xFF00F0FF);
        drawVisorAntenna(canvas, px, py, paintAccent);

        // Upward-angled white wings with cyan tip on left, pink tip on right
        paintFill.setColor(0xFFF6F8FE);
        drawAngledWings(canvas, px, py, paintFill);

        // Cyan left tip, Pink right tip
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(0.0f * px, 7.1f * py, 1.5f * px, 8.3f * py, paintAccent);
        canvas.drawRect(1.0f * px, 5.9f * py, 2.2f * px, 7.1f * py, paintAccent);
        canvas.drawRect(1.0f * px, 8.3f * py, 2.2f * px, 9.5f * py, paintAccent);

        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(26.5f * px, 7.1f * py, 28.0f * px, 8.3f * py, paintAccent);
        canvas.drawRect(25.8f * px, 5.9f * py, 27.0f * px, 7.1f * py, paintAccent);
        canvas.drawRect(25.8f * px, 8.3f * py, 27.0f * px, 9.5f * py, paintAccent);

        // Heart: Obsidian in prominent foreground
        paintFill.setColor(0xFF080B10);
        drawFullHeart(canvas, px, py, paintFill);

        // Dual Neon V-Contours (Left Cyan, Right Pink)
        drawDualHeartContours(canvas, px, py, 0xFF00F0FF, 0xFFFF2A93);

        // Iconic Asymmetric White Pixel Eyes
        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 02 — PINK (Neon Pink Style with Chevrons)
    // -------------------------------------------------------------
    private void drawPinkBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFFFF2A93);
        drawVisorAntenna(canvas, px, py, paintAccent);

        paintFill.setColor(0xFFFFF0F6);
        drawAngledWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(0.0f * px, 7.1f * py, 1.5f * px, 8.3f * py, paintAccent);
        canvas.drawRect(1.0f * px, 5.9f * py, 2.2f * px, 7.1f * py, paintAccent);
        canvas.drawRect(26.5f * px, 7.1f * py, 28.0f * px, 8.3f * py, paintAccent);
        canvas.drawRect(25.8f * px, 5.9f * py, 27.0f * px, 7.1f * py, paintAccent);

        paintFill.setColor(0xFF140816);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFFFF2A93, 0xFFFF2A93);

        // Pink chevron accents inside lower heart
        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(11.0f * px, 13.5f * py, 17.0f * px, 14.5f * py, paintAccent);
        canvas.drawRect(12.0f * px, 15.5f * py, 16.0f * px, 16.5f * py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFFFE5F0);
    }

    // -------------------------------------------------------------
    // 03 — CYAN (Electric Cyber Sky-Blue)
    // -------------------------------------------------------------
    private void drawCyanBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00E5FF);
        drawVisorAntenna(canvas, px, py, paintAccent);

        paintFill.setColor(0xFFE0F7FA);
        drawAngledWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF00E5FF);
        canvas.drawRect(0.0f * px, 7.1f * py, 2.0f * px, 8.3f * py, paintAccent);
        canvas.drawRect(26.0f * px, 7.1f * py, 28.0f * px, 8.3f * py, paintAccent);

        paintFill.setColor(0xFF06121B);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFF00E5FF, 0xFF00E5FF);
        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 04 — DARK (Velvet Obsidian + Magenta Glow)
    // -------------------------------------------------------------
    private void drawDarkBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF9D4EDD);
        drawVisorAntenna(canvas, px, py, paintAccent);

        paintFill.setColor(0xFF1B142A);
        drawAngledWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFC77DFF);
        canvas.drawRect(0.0f * px, 7.1f * py, 2.0f * px, 8.3f * py, paintAccent);
        canvas.drawRect(26.0f * px, 7.1f * py, 28.0f * px, 8.3f * py, paintAccent);

        paintFill.setColor(0xFF10091C);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFFC77DFF, 0xFF9D4EDD);
        drawPixelEyes(canvas, px, py, 0xFFE0AAFF);
    }

    // -------------------------------------------------------------
    // 05 — ANGEL (Floating Halo + Soft Lavender Heart)
    // -------------------------------------------------------------
    private void drawAngelBadge(Canvas canvas, float px, float py, float phase) {
        float haloY = (float) Math.round(Math.sin((phase + 0.3) * 2 * Math.PI) * 0.6) * py;
        canvas.save();
        canvas.translate(0, haloY);

        paintAccent.setColor(0x60E0AAFF);
        canvas.drawRect(9f * px, 0.5f * py, 19f * px, 3.5f * py, paintAccent);

        paintAccent.setColor(Color.WHITE);
        canvas.drawRect(10.5f * px, 1.2f * py, 17.5f * px, 2.0f * py, paintAccent);
        canvas.drawRect(9.0f * px, 2.0f * py, 10.5f * px, 2.8f * py, paintAccent);
        canvas.drawRect(17.5f * px, 2.0f * py, 19.0f * px, 2.8f * py, paintAccent);
        canvas.restore();

        paintFill.setColor(0xFFFAFAFE);
        drawAngledWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFB8C0EC);
        canvas.drawRect(0.0f * px, 7.1f * py, 1.5f * px, 8.3f * py, paintAccent);
        canvas.drawRect(26.5f * px, 7.1f * py, 28.0f * px, 8.3f * py, paintAccent);

        // Heart: SOFT PASTEL LAVENDER / PERIWINKLE (Authentic Angel Heart)
        paintFill.setColor(0xFFC3BEF0);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, Color.WHITE, Color.WHITE);
        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 06 — DEVIL (Devil Horns + Scalloped Bat Wings)
    // -------------------------------------------------------------
    private void drawDevilBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(7.5f * px, 2.8f * py, 9.5f * px, 5.0f * py, paintAccent);
        canvas.drawRect(6.5f * px, 2.0f * py, 8.0f * px, 3.6f * py, paintAccent);

        canvas.drawRect(18.5f * px, 2.8f * py, 20.5f * px, 5.0f * py, paintAccent);
        canvas.drawRect(20.0f * px, 2.0f * py, 21.5f * px, 3.6f * py, paintAccent);

        paintFill.setColor(0xFFFF3377);
        drawBatWings(canvas, px, py, paintFill);

        paintFill.setColor(0xFF180712);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFFFF0055, 0xFFFF0055);
        drawPixelEyes(canvas, px, py, 0xFFFFB3C6);
    }

    // -------------------------------------------------------------
    // 07 — RAINBOW (Prismatic 5-Color Spectrum)
    // -------------------------------------------------------------
    private void drawRainbowBadge(Canvas canvas, float px, float py, float phase) {
        int[] rainbow = {0xFFFF3377, 0xFF9D4EDD, 0xFF00B4D8, 0xFF06D6A0, 0xFFFFD166};
        drawRainbowAngledWings(canvas, px, py, rainbow);

        paintFill.setColor(0xFF0D1018);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFFFFD166, 0xFFFFD166);
        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 08 — OUTLINE (Glowing Wireframe Cyber Contour)
    // -------------------------------------------------------------
    private void drawOutlineBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00F0FF);
        drawWireframeAngledWings(canvas, px, py, paintAccent);
        drawVisorAntenna(canvas, px, py, paintAccent);
        drawDualHeartContours(canvas, px, py, 0xFF00F0FF, 0xFF00F0FF);

        canvas.drawRect(7.5f * px, 5.0f * py, 12.5f * px, 5.8f * py, paintAccent);
        canvas.drawRect(15.5f * px, 5.0f * py, 20.5f * px, 5.8f * py, paintAccent);

        canvas.drawRect(13.2f * px, 11.0f * py, 14.8f * px, 12.6f * py, paintAccent);
        drawPixelEyes(canvas, px, py, 0xFF00F0FF);
    }

    // -------------------------------------------------------------
    // 09 — GLITCH (Split RGB Chromatic Aberration)
    // -------------------------------------------------------------
    private void drawGlitchBadge(Canvas canvas, float px, float py, float phase, long now) {
        float jitter = (now % 300 < 60) ? 1.5f * px : 0.9f * px;

        canvas.save();
        canvas.translate(-jitter, -0.5f * py);
        paintAccent.setColor(0xCCFF0055);
        drawAngledLeftWing(canvas, px, py, paintAccent);
        canvas.drawRect(6.5f * px, 5.0f * py, 14.0f * px, 21.0f * py, paintAccent);
        canvas.restore();

        canvas.save();
        canvas.translate(jitter, 0.5f * py);
        paintAccent.setColor(0xCC00F0FF);
        drawAngledRightWing(canvas, px, py, paintAccent);
        canvas.drawRect(14.0f * px, 5.0f * py, 21.5f * px, 21.0f * py, paintAccent);
        canvas.restore();

        paintFill.setColor(0xEEFFFFFF);
        drawAngledWings(canvas, px, py, paintFill);

        paintFill.setColor(0xFF0C0E18);
        drawFullHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(1.0f * px, 7.5f * py, 27.0f * px, 8.5f * py, paintAccent);
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(2.0f * px, 13.5f * py, 26.0f * px, 14.5f * py, paintAccent);

        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 10 — PREMIUM (Royal Golden Crown + Golden Wings)
    // -------------------------------------------------------------
    private void drawPremiumBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFFFFD700);
        canvas.drawRect(10.5f * px, 1.8f * py, 12.5f * px, 4.8f * py, paintAccent);
        canvas.drawRect(13.0f * px, 0.8f * py, 15.0f * px, 4.8f * py, paintAccent);
        canvas.drawRect(15.5f * px, 1.8f * py, 17.5f * px, 4.8f * py, paintAccent);
        canvas.drawRect(10.5f * px, 4.8f * py, 17.5f * px, 5.8f * py, paintAccent);

        paintShade.setColor(0xFFFF2A93);
        canvas.drawRect(13.4f * px, 2.5f * py, 14.6f * px, 3.7f * py, paintShade);

        paintFill.setColor(0xFFFFE066);
        drawAngledWings(canvas, px, py, paintFill);

        paintShade.setColor(0xFFCC8800);
        canvas.drawRect(3.0f * px, 7.1f * py, 7.0f * px, 8.1f * py, paintShade);
        canvas.drawRect(21.0f * px, 7.1f * py, 25.0f * px, 8.1f * py, paintShade);

        paintFill.setColor(0xFF161106);
        drawFullHeart(canvas, px, py, paintFill);

        drawDualHeartContours(canvas, px, py, 0xFFFFD700, 0xFFFFD700);

        paintAccent.setColor(0xFFFFD700);
        canvas.drawRect(9.0f * px, 11.5f * py, 19.0f * px, 12.5f * py, paintAccent);
        canvas.drawRect(10.5f * px, 13.8f * py, 17.5f * px, 14.8f * py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFFFF5B8);
    }

    // -------------------------------------------------------------
    // Core Geometry Routines: Wings, Heart, Visor, and Contours
    // -------------------------------------------------------------
    private void drawVisorAntenna(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(10.0f * px, 1.5f * py, 18.0f * px, 2.1f * py, paint);
        canvas.drawRect(9.0f * px, 2.5f * py, 19.0f * px, 3.1f * py, paint);
        canvas.drawRect(9.0f * px, 3.5f * py, 19.0f * px, 4.1f * py, paint);
        canvas.drawRect(10.0f * px, 4.5f * py, 18.0f * px, 5.1f * py, paint);
        canvas.drawRect(9.0f * px, 1.5f * py, 9.6f * px, 5.1f * py, paint);
        canvas.drawRect(18.4f * px, 1.5f * py, 19.0f * px, 5.1f * py, paint);
    }

    private void drawAngledWings(Canvas canvas, float px, float py, Paint paint) {
        drawAngledLeftWing(canvas, px, py, paint);
        drawAngledRightWing(canvas, px, py, paint);
    }

    private void drawAngledLeftWing(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(5.0f * px, 3.5f * py, 9.0f * px, 4.7f * py, paint);
        canvas.drawRect(3.0f * px, 4.7f * py, 9.0f * px, 5.9f * py, paint);
        canvas.drawRect(1.0f * px, 5.9f * py, 8.5f * px, 7.1f * py, paint);
        canvas.drawRect(0.0f * px, 7.1f * py, 8.5f * px, 8.3f * py, paint);
        canvas.drawRect(1.0f * px, 8.3f * py, 8.5f * px, 9.5f * py, paint);
        canvas.drawRect(3.0f * px, 9.5f * py, 8.5f * px, 10.7f * py, paint);
        canvas.drawRect(5.0f * px, 10.7f * py, 8.5f * px, 11.9f * py, paint);
    }

    private void drawAngledRightWing(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(19.0f * px, 3.5f * py, 23.0f * px, 4.7f * py, paint);
        canvas.drawRect(19.0f * px, 4.7f * py, 25.0f * px, 5.9f * py, paint);
        canvas.drawRect(19.5f * px, 5.9f * py, 27.0f * px, 7.1f * py, paint);
        canvas.drawRect(19.5f * px, 7.1f * py, 28.0f * px, 8.3f * py, paint);
        canvas.drawRect(19.5f * px, 8.3f * py, 27.0f * px, 9.5f * py, paint);
        canvas.drawRect(19.5f * px, 9.5f * py, 25.0f * px, 10.7f * py, paint);
        canvas.drawRect(19.5f * px, 10.7f * py, 23.0f * px, 11.9f * py, paint);
    }

    private void drawBatWings(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(1.0f * px, 5.0f * py, 8.5f * px, 7.5f * py, paint);
        canvas.drawRect(0.0f * px, 7.5f * py, 8.5f * px, 9.5f * py, paint);
        canvas.drawRect(2.0f * px, 9.5f * py, 8.5f * px, 11.5f * py, paint);

        canvas.drawRect(19.5f * px, 5.0f * py, 27.0f * px, 7.5f * py, paint);
        canvas.drawRect(19.5f * px, 7.5f * py, 28.0f * px, 9.5f * py, paint);
        canvas.drawRect(19.5f * px, 9.5f * py, 26.0f * px, 11.5f * py, paint);
    }

    private void drawRainbowAngledWings(Canvas canvas, float px, float py, int[] colors) {
        paintAccent.setColor(colors[0]);
        canvas.drawRect(5.0f * px, 3.5f * py, 9.0f * px, 5.0f * py, paintAccent);
        canvas.drawRect(19.0f * px, 3.5f * py, 23.0f * px, 5.0f * py, paintAccent);

        paintAccent.setColor(colors[1]);
        canvas.drawRect(3.0f * px, 5.0f * py, 9.0f * px, 6.5f * py, paintAccent);
        canvas.drawRect(19.0f * px, 5.0f * py, 25.0f * px, 6.5f * py, paintAccent);

        paintAccent.setColor(colors[2]);
        canvas.drawRect(1.0f * px, 6.5f * py, 8.5f * px, 8.3f * py, paintAccent);
        canvas.drawRect(19.5f * px, 6.5f * py, 27.0f * px, 8.3f * py, paintAccent);

        paintAccent.setColor(colors[3]);
        canvas.drawRect(0.0f * px, 8.3f * py, 8.5f * px, 10.0f * py, paintAccent);
        canvas.drawRect(19.5f * px, 8.3f * py, 28.0f * px, 10.0f * py, paintAccent);

        paintAccent.setColor(colors[4]);
        canvas.drawRect(3.0f * px, 10.0f * py, 8.5f * px, 11.9f * py, paintAccent);
        canvas.drawRect(19.5f * px, 10.0f * py, 25.0f * px, 11.9f * py, paintAccent);
    }

    private void drawWireframeAngledWings(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(5.0f * px, 3.5f * py, 9.0f * px, 4.2f * py, paint);
        canvas.drawRect(3.0f * px, 4.7f * py, 4.0f * px, 5.9f * py, paint);
        canvas.drawRect(0.0f * px, 7.1f * py, 1.0f * px, 8.3f * py, paint);
        canvas.drawRect(3.0f * px, 9.5f * py, 4.0f * px, 10.7f * py, paint);
        canvas.drawRect(5.0f * px, 11.2f * py, 8.5f * px, 11.9f * py, paint);

        canvas.drawRect(19.0f * px, 3.5f * py, 23.0f * px, 4.2f * py, paint);
        canvas.drawRect(24.0f * px, 4.7f * py, 25.0f * px, 5.9f * py, paint);
        canvas.drawRect(27.0f * px, 7.1f * py, 28.0f * px, 8.3f * py, paint);
        canvas.drawRect(24.0f * px, 9.5f * py, 25.0f * px, 10.7f * py, paint);
        canvas.drawRect(19.5f * px, 11.2f * py, 23.0f * px, 11.9f * py, paint);
    }

    /**
     * Draws the authentic pixel heart in the foreground.
     * Two rounded lobes at top, wide body, and sharp V-tip pointing down past wings.
     */
    private void drawFullHeart(Canvas canvas, float px, float py, Paint paint) {
        // Left rounded lobe
        canvas.drawRect(7.5f * px, 5.0f * py, 12.5f * px, 6.8f * py, paint);
        canvas.drawRect(7.0f * px, 6.8f * py, 13.0f * px, 8.5f * py, paint);

        // Right rounded lobe
        canvas.drawRect(15.5f * px, 5.0f * py, 20.5f * px, 6.8f * py, paint);
        canvas.drawRect(15.0f * px, 6.8f * py, 21.0f * px, 8.5f * py, paint);

        // Main heart body
        canvas.drawRect(6.5f * px, 8.5f * py, 21.5f * px, 12.0f * py, paint);

        // Standalone lower heart V (extends completely below wings!)
        canvas.drawRect(7.5f * px, 12.0f * py, 20.5f * px, 13.5f * py, paint);
        canvas.drawRect(8.5f * px, 13.5f * py, 19.5f * px, 15.0f * py, paint);
        canvas.drawRect(9.5f * px, 15.0f * py, 18.5f * px, 16.5f * py, paint);
        canvas.drawRect(10.5f * px, 16.5f * py, 17.5f * px, 18.0f * py, paint);
        canvas.drawRect(11.5f * px, 18.0f * py, 16.5f * px, 19.5f * py, paint);
        canvas.drawRect(12.5f * px, 19.5f * py, 15.5f * px, 21.0f * py, paint);
        canvas.drawRect(13.3f * px, 21.0f * py, 14.7f * px, 22.0f * py, paint);
    }

    private void drawDualHeartContours(Canvas canvas, float px, float py, int leftCol, int rightCol) {
        // Left V-Contour
        paintAccent.setColor(leftCol);
        canvas.drawRect(6.5f * px, 11.0f * py, 7.5f * px, 12.5f * py, paintAccent);
        canvas.drawRect(7.5f * px, 12.5f * py, 8.5f * px, 14.0f * py, paintAccent);
        canvas.drawRect(8.5f * px, 14.0f * py, 9.5f * px, 15.5f * py, paintAccent);
        canvas.drawRect(9.5f * px, 15.5f * py, 10.5f * px, 17.0f * py, paintAccent);
        canvas.drawRect(10.5f * px, 17.0f * py, 11.5f * px, 18.5f * py, paintAccent);
        canvas.drawRect(11.5f * px, 18.5f * py, 12.5f * px, 20.0f * py, paintAccent);
        canvas.drawRect(12.5f * px, 20.0f * py, 13.5f * px, 21.2f * py, paintAccent);
        canvas.drawRect(13.3f * px, 21.0f * py, 14.0f * px, 22.0f * py, paintAccent);

        // Right V-Contour
        paintAccent.setColor(rightCol);
        canvas.drawRect(20.5f * px, 11.0f * py, 21.5f * px, 12.5f * py, paintAccent);
        canvas.drawRect(19.5f * px, 12.5f * py, 20.5f * px, 14.0f * py, paintAccent);
        canvas.drawRect(18.5f * px, 14.0f * py, 19.5f * px, 15.5f * py, paintAccent);
        canvas.drawRect(17.5f * px, 15.5f * py, 18.5f * px, 17.0f * py, paintAccent);
        canvas.drawRect(16.5f * px, 17.0f * py, 17.5f * px, 18.5f * py, paintAccent);
        canvas.drawRect(15.5f * px, 18.5f * py, 16.5f * px, 20.0f * py, paintAccent);
        canvas.drawRect(14.5f * px, 20.0f * py, 15.5f * px, 21.2f * py, paintAccent);
        canvas.drawRect(14.0f * px, 21.0f * py, 14.7f * px, 22.0f * py, paintAccent);
    }

    private void drawPixelEyes(Canvas canvas, float px, float py, int eyeColor) {
        paintEyes.setColor(eyeColor);
        // Left Eye: Tall vertical pill/rectangle
        canvas.drawRect(9.0f * px, 8.5f * py, 10.8f * px, 11.5f * py, paintEyes);
        // Right Eye: Cute square pixel dot
        canvas.drawRect(16.5f * px, 9.5f * py, 18.1f * px, 11.1f * py, paintEyes);
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
