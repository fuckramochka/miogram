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
    // 01 — ORIGINAL (Classic Winged Heart + Visor)
    // -------------------------------------------------------------
    private void drawOriginalBadge(Canvas canvas, float px, float py, float phase) {
        // Glowing Visor Antenna
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        canvas.drawRect(12 * px, 4 * py, 16 * px, 5 * py, paintAccent);

        // White feathered wings with subtle cyan shading
        paintFill.setColor(0xFFF0FDFE);
        drawClassicWings(canvas, px, py, paintFill);

        // Translucent cyan feather shade
        paintShade.setColor(0x3300F0FF);
        canvas.drawRect(4 * px, 7 * py, 9 * px, 10 * py, paintShade);
        canvas.drawRect(19 * px, 7 * py, 24 * px, 10 * py, paintShade);

        // Hot pink wing tips
        paintAccent.setColor(0xFFFF55A3);
        canvas.drawRect(1 * px, 7 * py, 3 * px, 10 * py, paintAccent);
        canvas.drawRect(25 * px, 7 * py, 27 * px, 10 * py, paintAccent);

        // Heart: Obsidian with vibrant cyan glowing contour & white eyes
        paintFill.setColor(0xFF0F141C);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF00F0FF);
        drawHeartContour(canvas, px, py, paintAccent);

        // Specular highlight on heart
        paintAccent.setColor(0x66FFFFFF);
        canvas.drawRect(10 * px, 9 * py, 12 * px, 10 * py, paintAccent);

        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 02 — PINK (Neon Pink Style with Chevrons)
    // -------------------------------------------------------------
    private void drawPinkBadge(Canvas canvas, float px, float py, float phase) {
        // Pink Visor
        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        canvas.drawRect(12 * px, 4 * py, 16 * px, 5 * py, paintAccent);

        // White-to-pink gradient wings
        paintFill.setColor(0xFFFFF0F7);
        drawClassicWings(canvas, px, py, paintFill);

        paintShade.setColor(0x44FF2A93);
        canvas.drawRect(3 * px, 7 * py, 9 * px, 10 * py, paintShade);
        canvas.drawRect(19 * px, 7 * py, 25 * px, 10 * py, paintShade);

        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(1 * px, 7 * py, 3 * px, 10 * py, paintAccent);
        canvas.drawRect(25 * px, 7 * py, 27 * px, 10 * py, paintAccent);

        // Heart: Obsidian with pink chevron rim & pink ribs
        paintFill.setColor(0xFF1B0F1C);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF2A93);
        drawHeartContour(canvas, px, py, paintAccent);
        // Chevron V-lines inside
        canvas.drawRect(11 * px, 12 * py, 17 * px, 13 * py, paintAccent);
        canvas.drawRect(12 * px, 14 * py, 16 * px, 15 * py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFFFE5F0);
    }

    // -------------------------------------------------------------
    // 03 — CYAN (Electric Cyber Sky-Blue)
    // -------------------------------------------------------------
    private void drawCyanBadge(Canvas canvas, float px, float py, float phase) {
        // Electric Cyan Visor
        paintAccent.setColor(0xFF00E5FF);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);
        canvas.drawRect(12 * px, 4 * py, 16 * px, 5 * py, paintAccent);

        // Vibrant Cyan Wings
        paintFill.setColor(0xFFE0F7FA);
        drawClassicWings(canvas, px, py, paintFill);

        paintShade.setColor(0x5500E5FF);
        canvas.drawRect(3 * px, 7 * py, 9 * px, 10 * py, paintShade);
        canvas.drawRect(19 * px, 7 * py, 25 * px, 10 * py, paintShade);

        paintAccent.setColor(0xFF00E5FF);
        canvas.drawRect(1 * px, 7 * py, 3 * px, 10 * py, paintAccent);
        canvas.drawRect(25 * px, 7 * py, 27 * px, 10 * py, paintAccent);

        // Heart: Obsidian with neon cyan rim & eyes
        paintFill.setColor(0xFF0A1822);
        drawShapedHeart(canvas, px, py, paintFill);

        drawHeartContour(canvas, px, py, paintAccent);
        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 04 — DARK (Velvet Obsidian + Magenta Glow)
    // -------------------------------------------------------------
    private void drawDarkBadge(Canvas canvas, float px, float py, float phase) {
        // Purple Visor
        paintAccent.setColor(0xFF9D4EDD);
        canvas.drawRect(11 * px, 3 * py, 17 * px, 4 * py, paintAccent);

        // Obsidian Wings with purple shading
        paintFill.setColor(0xFF1B142A);
        drawClassicWings(canvas, px, py, paintFill);

        // Glowing velvet purple edges
        paintAccent.setColor(0xFFC77DFF);
        canvas.drawRect(1 * px, 7 * py, 3 * px, 10 * py, paintAccent);
        canvas.drawRect(25 * px, 7 * py, 27 * px, 10 * py, paintAccent);
        canvas.drawRect(4 * px, 5 * py, 6 * px, 6 * py, paintAccent);
        canvas.drawRect(22 * px, 5 * py, 24 * px, 6 * py, paintAccent);

        // Heart: Deep midnight violet with radiant rim
        paintFill.setColor(0xFF120B20);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF9D4EDD);
        drawHeartContour(canvas, px, py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFE0AAFF);
    }

    // -------------------------------------------------------------
    // 05 — ANGEL (Floating Halo + Lavender Heart)
    // -------------------------------------------------------------
    private void drawAngelBadge(Canvas canvas, float px, float py, float phase) {
        // Floating Halo Ring with radiant glow
        float haloY = (float) Math.round(Math.sin((phase + 0.3) * 2 * Math.PI) * 0.6) * py;
        canvas.save();
        canvas.translate(0, haloY);

        paintAccent.setColor(0x55E0AAFF);
        canvas.drawRect(8 * px, 0 * py, 20 * px, 3 * py, paintAccent);

        paintAccent.setColor(Color.WHITE);
        canvas.drawRect(10 * px, 1 * py, 18 * px, 2 * py, paintAccent);
        canvas.drawRect(8 * px, 2 * py, 10 * px, 3 * py, paintAccent);
        canvas.drawRect(18 * px, 2 * py, 20 * px, 3 * py, paintAccent);
        canvas.restore();

        // Fluffy Pure White Wings with subtle lavender tint
        paintFill.setColor(0xFFFAFAFE);
        drawClassicWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFB8C0EC);
        canvas.drawRect(2 * px, 8 * py, 4 * px, 10 * py, paintAccent);
        canvas.drawRect(24 * px, 8 * py, 26 * px, 10 * py, paintAccent);

        // Heart: Soft pastel periwinkle/lavender with white contour
        paintFill.setColor(0xFFC3BEF0);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(Color.WHITE);
        drawHeartContour(canvas, px, py, paintAccent);

        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 06 — DEVIL (Devil Horns + Pointed Bat Wings)
    // -------------------------------------------------------------
    private void drawDevilBadge(Canvas canvas, float px, float py, float phase) {
        // Pointed Devil Horns
        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(9 * px, 4 * py, 11 * px, 7 * py, paintAccent);
        canvas.drawRect(8 * px, 3 * py, 10 * px, 5 * py, paintAccent);
        canvas.drawRect(17 * px, 4 * py, 19 * px, 7 * py, paintAccent);
        canvas.drawRect(18 * px, 3 * py, 20 * px, 5 * py, paintAccent);

        // Scalloped Pointed Bat Wings
        paintFill.setColor(0xFFFF3377);
        drawBatWings(canvas, px, py, paintFill);

        paintShade.setColor(0xFFB8003D);
        canvas.drawRect(3 * px, 7 * py, 8 * px, 9 * py, paintShade);
        canvas.drawRect(20 * px, 7 * py, 25 * px, 9 * py, paintShade);

        // Heart: Obsidian with hot magenta/crimson contour
        paintFill.setColor(0xFF1C0A15);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF0055);
        drawHeartContour(canvas, px, py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFFFB3C6);
    }

    // -------------------------------------------------------------
    // 07 — RAINBOW (Prismatic 5-Color Spectrum)
    // -------------------------------------------------------------
    private void drawRainbowBadge(Canvas canvas, float px, float py, float phase) {
        // Prismatic Rainbow Spectrum Wings
        int[] rainbow = {0xFFFF3377, 0xFF9D4EDD, 0xFF00B4D8, 0xFF06D6A0, 0xFFFFD166};
        drawRainbowWings(canvas, px, py, rainbow);

        // Heart: Obsidian with golden glowing rim
        paintFill.setColor(0xFF10141E);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFFD166);
        drawHeartContour(canvas, px, py, paintAccent);

        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 08 — OUTLINE (Glowing Wireframe Cyber Contour)
    // -------------------------------------------------------------
    private void drawOutlineBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00F0FF);
        drawWireframeWings(canvas, px, py, paintAccent);

        canvas.drawRect(11 * px, 3 * py, 17 * px, 4 * py, paintAccent);
        drawHeartContour(canvas, px, py, paintAccent);

        // Center pixel starlight core
        canvas.drawRect(13 * px, 11 * py, 15 * px, 13 * py, paintAccent);
        drawPixelEyes(canvas, px, py, 0xFF00F0FF);
    }

    // -------------------------------------------------------------
    // 09 — GLITCH (Split RGB Chromatic Aberration)
    // -------------------------------------------------------------
    private void drawGlitchBadge(Canvas canvas, float px, float py, float phase, long now) {
        float jitter = (now % 300 < 50) ? 1.4f * px : 0.9f * px;

        // Magenta Shift (Left)
        canvas.save();
        canvas.translate(-jitter, -0.5f * py);
        paintAccent.setColor(0xCCFF0055);
        drawLeftWing(canvas, px, py, paintAccent);
        drawLeftHeart(canvas, px, py, paintAccent);
        canvas.restore();

        // Cyan Shift (Right)
        canvas.save();
        canvas.translate(jitter, 0.5f * py);
        paintAccent.setColor(0xCC00F0FF);
        drawRightWing(canvas, px, py, paintAccent);
        drawRightHeart(canvas, px, py, paintAccent);
        canvas.restore();

        // White/dark core
        paintFill.setColor(0xEEFFFFFF);
        drawClassicWings(canvas, px, py, paintFill);

        paintFill.setColor(0xFF10121C);
        drawShapedHeart(canvas, px, py, paintFill);

        // Scanlines
        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(2 * px, 8 * py, 26 * px, 9 * py, paintAccent);
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(3 * px, 13 * py, 25 * px, 14 * py, paintAccent);

        drawPixelEyes(canvas, px, py, Color.WHITE);
    }

    // -------------------------------------------------------------
    // 10 — PREMIUM (Golden Crown + Golden Wings)
    // -------------------------------------------------------------
    private void drawPremiumBadge(Canvas canvas, float px, float py, float phase) {
        // Royal 3-Peak Golden Crown
        paintAccent.setColor(0xFFFFD700);
        canvas.drawRect(10 * px, 2 * py, 12 * px, 5 * py, paintAccent);
        canvas.drawRect(13 * px, 1 * py, 15 * px, 5 * py, paintAccent);
        canvas.drawRect(16 * px, 2 * py, 18 * px, 5 * py, paintAccent);
        canvas.drawRect(10 * px, 5 * py, 18 * px, 6 * py, paintAccent);

        // Crown jewels
        paintShade.setColor(0xFFFF2A93);
        canvas.drawRect(13.5f * px, 2.5f * py, 14.5f * px, 3.5f * py, paintShade);

        // Golden Wings
        paintFill.setColor(0xFFFFE066);
        drawClassicWings(canvas, px, py, paintFill);

        // Dark feather slits
        paintShade.setColor(0xFFCC8800);
        canvas.drawRect(4 * px, 8 * py, 8 * px, 9 * py, paintShade);
        canvas.drawRect(20 * px, 8 * py, 24 * px, 9 * py, paintShade);
        canvas.drawRect(3 * px, 10 * py, 7 * px, 11 * py, paintShade);
        canvas.drawRect(21 * px, 10 * py, 25 * px, 11 * py, paintShade);

        // Heart: Obsidian with golden armor ribs
        paintFill.setColor(0xFF1B1408);
        drawShapedHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFFD700);
        drawHeartContour(canvas, px, py, paintAccent);
        canvas.drawRect(10 * px, 11 * py, 18 * px, 12 * py, paintAccent);
        canvas.drawRect(11 * px, 13 * py, 17 * px, 14 * py, paintAccent);

        drawPixelEyes(canvas, px, py, 0xFFFFF5B8);
    }

    // -------------------------------------------------------------
    // Core Geometry Routines
    // -------------------------------------------------------------
    private void drawClassicWings(Canvas canvas, float px, float py, Paint paint) {
        drawLeftWing(canvas, px, py, paint);
        drawRightWing(canvas, px, py, paint);
    }

    private void drawLeftWing(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paint);
        canvas.drawRect(3 * px, 6 * py, 10 * px, 7 * py, paint);
        canvas.drawRect(2 * px, 7 * py, 11 * px, 8 * py, paint);
        canvas.drawRect(1 * px, 8 * py, 11 * px, 9 * py, paint);
        canvas.drawRect(2 * px, 9 * py, 11 * px, 10 * py, paint);
        canvas.drawRect(3 * px, 10 * py, 10 * px, 11 * py, paint);
        canvas.drawRect(4 * px, 11 * py, 9 * px, 12 * py, paint);
        canvas.drawRect(6 * px, 12 * py, 9 * px, 13 * py, paint);
    }

    private void drawRightWing(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paint);
        canvas.drawRect(18 * px, 6 * py, 25 * px, 7 * py, paint);
        canvas.drawRect(17 * px, 7 * py, 26 * px, 8 * py, paint);
        canvas.drawRect(17 * px, 8 * py, 27 * px, 9 * py, paint);
        canvas.drawRect(17 * px, 9 * py, 26 * px, 10 * py, paint);
        canvas.drawRect(18 * px, 10 * py, 25 * px, 11 * py, paint);
        canvas.drawRect(19 * px, 11 * py, 24 * px, 12 * py, paint);
        canvas.drawRect(19 * px, 12 * py, 22 * px, 13 * py, paint);
    }

    private void drawBatWings(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(2 * px, 5 * py, 10 * px, 7 * py, paint);
        canvas.drawRect(1 * px, 7 * py, 11 * px, 9 * py, paint);
        canvas.drawRect(3 * px, 9 * py, 11 * px, 11 * py, paint);
        canvas.drawRect(5 * px, 11 * py, 9 * px, 13 * py, paint);

        canvas.drawRect(18 * px, 5 * py, 26 * px, 7 * py, paint);
        canvas.drawRect(17 * px, 7 * py, 27 * px, 9 * py, paint);
        canvas.drawRect(17 * px, 9 * py, 25 * px, 11 * py, paint);
        canvas.drawRect(19 * px, 11 * py, 23 * px, 13 * py, paint);
    }

    private void drawRainbowWings(Canvas canvas, float px, float py, int[] colors) {
        paintAccent.setColor(colors[0]);
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paintAccent);
        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paintAccent);

        paintAccent.setColor(colors[1]);
        canvas.drawRect(3 * px, 6 * py, 10 * px, 7 * py, paintAccent);
        canvas.drawRect(18 * px, 6 * py, 25 * px, 7 * py, paintAccent);

        paintAccent.setColor(colors[2]);
        canvas.drawRect(2 * px, 7 * py, 11 * px, 8 * py, paintAccent);
        canvas.drawRect(17 * px, 7 * py, 26 * px, 8 * py, paintAccent);

        paintAccent.setColor(colors[3]);
        canvas.drawRect(1 * px, 8 * py, 11 * px, 10 * py, paintAccent);
        canvas.drawRect(17 * px, 8 * py, 27 * px, 10 * py, paintAccent);

        paintAccent.setColor(colors[4]);
        canvas.drawRect(3 * px, 10 * py, 10 * px, 13 * py, paintAccent);
        canvas.drawRect(18 * px, 10 * py, 25 * px, 13 * py, paintAccent);
    }

    private void drawWireframeWings(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paint);
        canvas.drawRect(3 * px, 6 * py, 4 * px, 7 * py, paint);
        canvas.drawRect(1 * px, 7 * py, 2 * px, 10 * py, paint);
        canvas.drawRect(2 * px, 10 * py, 4 * px, 11 * py, paint);
        canvas.drawRect(4 * px, 11 * py, 7 * px, 13 * py, paint);

        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paint);
        canvas.drawRect(24 * px, 6 * py, 25 * px, 7 * py, paint);
        canvas.drawRect(26 * px, 7 * py, 27 * px, 10 * py, paint);
        canvas.drawRect(24 * px, 10 * py, 26 * px, 11 * py, paint);
        canvas.drawRect(21 * px, 11 * py, 24 * px, 13 * py, paint);
    }

    private void drawShapedHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(10 * px, 7 * py, 13 * px, 8 * py, paint);
        canvas.drawRect(15 * px, 7 * py, 18 * px, 8 * py, paint);
        canvas.drawRect(9 * px, 8 * py, 19 * px, 10 * py, paint);
        canvas.drawRect(8 * px, 10 * py, 20 * px, 12 * py, paint);
        canvas.drawRect(9 * px, 12 * py, 19 * px, 13 * py, paint);
        canvas.drawRect(10 * px, 13 * py, 18 * px, 14 * py, paint);
        canvas.drawRect(11 * px, 14 * py, 17 * px, 15 * py, paint);
        canvas.drawRect(12 * px, 15 * py, 16 * px, 16 * py, paint);
        canvas.drawRect(13 * px, 16 * py, 15 * px, 17 * py, paint);
    }

    private void drawLeftHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(9 * px, 7 * py, 14 * px, 16 * py, paint);
    }

    private void drawRightHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(14 * px, 7 * py, 19 * px, 16 * py, paint);
    }

    private void drawHeartContour(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(10 * px, 6 * py, 13 * px, 7 * py, paint);
        canvas.drawRect(15 * px, 6 * py, 18 * px, 7 * py, paint);
        canvas.drawRect(8 * px, 8 * py, 9 * px, 12 * py, paint);
        canvas.drawRect(19 * px, 8 * py, 20 * px, 12 * py, paint);
        // Bottom pixel removed: natural clean heart tip
    }

    private void drawPixelEyes(Canvas canvas, float px, float py, int eyeColor) {
        paintEyes.setColor(eyeColor);
        canvas.drawRect(11 * px, 9.5f * py, 12.5f * px, 11 * py, paintEyes);
        canvas.drawRect(15.5f * px, 9.5f * py, 17 * px, 11 * py, paintEyes);
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
