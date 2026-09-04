package app.miogram.bridge.badge;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

/**
 * High-definition pixel renderer for all 10 canonical Miogram Badges:
 * 01 - ORIGINAL (Classic winged heart with antenna & cyan contour)
 * 02 - PINK (Neon pink style with chevrons)
 * 03 - CYAN (Electric cyber sky blue style)
 * 04 - DARK (Obsidian with glowing velvet edges)
 * 05 - ANGEL (Floating halo with lavender heart & white wings)
 * 06 - DEVIL (Devil horns & bat wings)
 * 07 - RAINBOW (Prismatic rainbow wings)
 * 08 - OUTLINE (Crisp 1px wireframe pixel contour)
 * 09 - GLITCH (Split RGB displacement glitch)
 * 10 - PREMIUM (Golden royal crown & golden wings)
 *
 * Guaranteed visibility: Contours and eyes remain crisp and vibrant on both AMOLED dark and light themes.
 */
public class MiogramArrowDrawable extends Drawable {

    private static final int ANIMATION_DURATION_MS = 2400;

    private final MiogramBadgeType badgeType;
    private final int size;

    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEyes = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSparkle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintExtra = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        this(16, MiogramBadgeType.ORIGINAL);
    }

    public MiogramArrowDrawable(int sizeDp) {
        this(sizeDp, MiogramBadgeType.ORIGINAL);
    }

    public MiogramArrowDrawable(int sizeDp, @Nullable MiogramBadgeType type) {
        this.size = AndroidUtilities.dp(sizeDp);
        this.badgeType = (type != null) ? type : MiogramBadgeType.ORIGINAL;
        setBounds(0, 0, size, size);

        paintFill.setStyle(Paint.Style.FILL);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintAccent.setStyle(Paint.Style.FILL);
        paintEyes.setStyle(Paint.Style.FILL);
        paintEyes.setColor(Color.WHITE);
        paintSparkle.setStyle(Paint.Style.FILL);
        paintExtra.setStyle(Paint.Style.FILL);
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

        float bobY = (float) Math.round(Math.sin(angle) * 1.0) * py;

        canvas.save();
        canvas.translate(bounds.left, bounds.top + bobY);

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
                drawGlitchBadge(canvas, px, py, phase);
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

    // -------------------------------------------------------------
    // 01 - ORIGINAL
    // -------------------------------------------------------------
    private void drawOriginalBadge(Canvas canvas, float px, float py, float phase) {
        // Antenna Visor
        paintAccent.setColor(0xCC00F0FF);
        canvas.drawRect(10 * px, 4 * py, 18 * px, 5 * py, paintAccent);
        canvas.drawRect(12 * px, 2 * py, 16 * px, 3 * py, paintAccent);
        canvas.drawRect(13 * px, 3 * py, 15 * px, 4 * py, paintAccent);

        // Wings: White with cyan tint and pink tips
        paintFill.setColor(0xF0FFFFFF);
        drawStandardWings(canvas, px, py, paintFill);

        // Pink outer wing tips
        paintAccent.setColor(0xFFFF55A3);
        canvas.drawRect(1 * px, 7 * py, 3 * px, 9 * py, paintAccent);
        canvas.drawRect(25 * px, 7 * py, 27 * px, 9 * py, paintAccent);

        // Cyan Wing Outline
        paintAccent.setColor(0xDD00F0FF);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Obsidian fill with bright glowing cyan contour & white eyes
        paintFill.setColor(0xFF141724);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF00F0FF);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, Color.WHITE);
        drawSparkles(canvas, px, py, phase, 0xFF00F0FF);
    }

    // -------------------------------------------------------------
    // 02 - PINK
    // -------------------------------------------------------------
    private void drawPinkBadge(Canvas canvas, float px, float py, float phase) {
        // Pink Visor
        paintAccent.setColor(0xFFFF2A93);
        canvas.drawRect(10 * px, 4 * py, 18 * px, 5 * py, paintAccent);
        canvas.drawRect(12 * px, 3 * py, 16 * px, 4 * py, paintAccent);

        // Wings: White to pastel pink
        paintFill.setColor(0xFFFFF0F5);
        drawStandardWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF70A6);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Obsidian with pink chevron rim & pink eyes
        paintFill.setColor(0xFF1A101C);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF2A93);
        drawStandardHeartOutline(canvas, px, py, paintAccent);
        // Inner chevron stripe
        canvas.drawRect(11 * px, 12 * py, 17 * px, 13 * py, paintAccent);

        drawHeartEyes(canvas, px, py, 0xFFFFE5F0);
        drawSparkles(canvas, px, py, phase, 0xFFFF2A93);
    }

    // -------------------------------------------------------------
    // 03 - CYAN
    // -------------------------------------------------------------
    private void drawCyanBadge(Canvas canvas, float px, float py, float phase) {
        paintAccent.setColor(0xFF00E5FF);
        canvas.drawRect(10 * px, 4 * py, 18 * px, 5 * py, paintAccent);

        // Wings: Electric cyan
        paintFill.setColor(0xFFE0F7FA);
        drawStandardWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF00E5FF);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Dark with glowing cyan edge
        paintFill.setColor(0xFF0E1A24);
        drawStandardHeart(canvas, px, py, paintFill);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, Color.WHITE);
        drawSparkles(canvas, px, py, phase, 0xFF00E5FF);
    }

    // -------------------------------------------------------------
    // 04 - DARK
    // -------------------------------------------------------------
    private void drawDarkBadge(Canvas canvas, float px, float py, float phase) {
        // Dark velvet visor
        paintAccent.setColor(0xFF9D4EDD);
        canvas.drawRect(11 * px, 4 * py, 17 * px, 5 * py, paintAccent);

        // Wings: Obsidian with glowing magenta edges
        paintFill.setColor(0xFF191326);
        drawStandardWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFC77DFF);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Deep purple with violet rim
        paintFill.setColor(0xFF120C1F);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFF9D4EDD);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, 0xFFE0AAFF);
        drawSparkles(canvas, px, py, phase, 0xFFC77DFF);
    }

    // -------------------------------------------------------------
    // 05 - ANGEL
    // -------------------------------------------------------------
    private void drawAngelBadge(Canvas canvas, float px, float py, float phase) {
        // Floating Halo Ring
        paintAccent.setColor(Color.WHITE);
        canvas.drawRect(10 * px, 1 * py, 18 * px, 2 * py, paintAccent);
        canvas.drawRect(8 * px, 2 * py, 10 * px, 3 * py, paintAccent);
        canvas.drawRect(18 * px, 2 * py, 20 * px, 3 * py, paintAccent);
        canvas.drawRect(10 * px, 3 * py, 18 * px, 4 * py, paintAccent);

        // Wings: Pure fluffy white with soft sky glow
        paintFill.setColor(0xFFF5FAFF);
        drawStandardWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFA0C4FF);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Soft pastel lavender periwinkle with white outline
        paintFill.setColor(0xFFB8C0EC);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(Color.WHITE);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, Color.WHITE);
        drawSparkles(canvas, px, py, phase, 0xFFEBF4F6);
    }

    // -------------------------------------------------------------
    // 06 - DEVIL
    // -------------------------------------------------------------
    private void drawDevilBadge(Canvas canvas, float px, float py, float phase) {
        // Cute Devil Horns
        paintAccent.setColor(0xFFFF006E);
        canvas.drawRect(9 * px, 5 * py, 11 * px, 8 * py, paintAccent);
        canvas.drawRect(8 * px, 4 * py, 10 * px, 6 * py, paintAccent);
        canvas.drawRect(17 * px, 5 * py, 19 * px, 8 * py, paintAccent);
        canvas.drawRect(18 * px, 4 * py, 20 * px, 6 * py, paintAccent);

        // Bat Wings (Scalloped pointed wings)
        paintFill.setColor(0xFFFF4D8D);
        drawBatWings(canvas, px, py, paintFill);

        // Heart: Dark with glowing crimson/pink rim
        paintFill.setColor(0xFF200B1A);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFF006E);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, 0xFFFFB3C6);
        drawSparkles(canvas, px, py, phase, 0xFFFF4D8D);
    }

    // -------------------------------------------------------------
    // 07 - RAINBOW
    // -------------------------------------------------------------
    private void drawRainbowBadge(Canvas canvas, float px, float py, float phase) {
        // Prismatic Rainbow Spectrum Wings
        int[] rainbowColors = {0xFFFF477E, 0xFF9D4EDD, 0xFF00B4D8, 0xFF06D6A0, 0xFFFFD166};
        drawRainbowWings(canvas, px, py, rainbowColors);

        // Heart: Obsidian with golden glowing rim
        paintFill.setColor(0xFF141724);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFFD166);
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        drawHeartEyes(canvas, px, py, Color.WHITE);
        drawSparkles(canvas, px, py, phase, 0xFFFFD166);
    }

    // -------------------------------------------------------------
    // 08 - OUTLINE
    // -------------------------------------------------------------
    private void drawOutlineBadge(Canvas canvas, float px, float py, float phase) {
        // Pure 1px wireframe pixel contour (crisp cyber white/cyan)
        paintAccent.setColor(0xFFE2E8F0);
        drawWireframeWings(canvas, px, py, paintAccent);

        // Antenna wireframe
        canvas.drawRect(11 * px, 4 * py, 17 * px, 5 * py, paintAccent);

        // Heart Outline
        drawStandardHeartOutline(canvas, px, py, paintAccent);

        // Center pixel highlight
        canvas.drawRect(13.5f * px, 11 * py, 14.5f * px, 12 * py, paintAccent);

        drawHeartEyes(canvas, px, py, paintAccent.getColor());
        drawSparkles(canvas, px, py, phase, 0xFFE2E8F0);
    }

    // -------------------------------------------------------------
    // 09 - GLITCH
    // -------------------------------------------------------------
    private void drawGlitchBadge(Canvas canvas, float px, float py, float phase) {
        // RGB Displacement: Magenta offset left, Cyan offset right
        float shift = px * 1.2f;

        // Left split (Magenta)
        canvas.save();
        canvas.translate(-shift, -0.6f * py);
        paintAccent.setColor(0xCCFF0055);
        drawLeftWing(canvas, px, py, paintAccent);
        drawLeftHeart(canvas, px, py, paintAccent);
        canvas.restore();

        // Right split (Cyan)
        canvas.save();
        canvas.translate(shift, 0.6f * py);
        paintAccent.setColor(0xCC00F0FF);
        drawRightWing(canvas, px, py, paintAccent);
        drawRightHeart(canvas, px, py, paintAccent);
        canvas.restore();

        // Core White Glitch Body
        paintFill.setColor(0xE6FFFFFF);
        drawStandardWings(canvas, px, py, paintFill);
        paintFill.setColor(0xFF121420);
        drawStandardHeart(canvas, px, py, paintFill);

        // Glitch Scanlines
        paintAccent.setColor(0xFFFF0055);
        canvas.drawRect(2 * px, 9 * py, 26 * px, 10 * py, paintAccent);
        paintAccent.setColor(0xFF00F0FF);
        canvas.drawRect(4 * px, 12 * py, 24 * px, 13 * py, paintAccent);

        drawHeartEyes(canvas, px, py, Color.WHITE);
        drawSparkles(canvas, px, py, phase, 0xFF00F0FF);
    }

    // -------------------------------------------------------------
    // 10 - PREMIUM
    // -------------------------------------------------------------
    private void drawPremiumBadge(Canvas canvas, float px, float py, float phase) {
        // Golden Royal 3-peak Crown
        paintAccent.setColor(0xFFFFD700);
        // Crown peaks
        canvas.drawRect(10 * px, 4 * py, 12 * px, 6 * py, paintAccent);
        canvas.drawRect(13 * px, 3 * py, 15 * px, 6 * py, paintAccent);
        canvas.drawRect(16 * px, 4 * py, 18 * px, 6 * py, paintAccent);
        // Crown band
        canvas.drawRect(10 * px, 6 * py, 18 * px, 7 * py, paintAccent);

        // Wings: Golden Amber with feather stripes
        paintFill.setColor(0xFFFFE066);
        drawStandardWings(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFCC8800);
        drawStandardWingTips(canvas, px, py, paintAccent);

        // Heart: Obsidian with rich golden borders & horizontal stripes
        paintFill.setColor(0xFF1C150A);
        drawStandardHeart(canvas, px, py, paintFill);

        paintAccent.setColor(0xFFFFD700);
        drawStandardHeartOutline(canvas, px, py, paintAccent);
        // Golden internal chest bars
        canvas.drawRect(10 * px, 11 * py, 18 * px, 12 * py, paintAccent);
        canvas.drawRect(11 * px, 13 * py, 17 * px, 14 * py, paintAccent);

        drawHeartEyes(canvas, px, py, 0xFFFFF3B0);
        drawSparkles(canvas, px, py, phase, 0xFFFFD700);
    }

    // -------------------------------------------------------------
    // Shared Geometry
    // -------------------------------------------------------------
    private void drawStandardWings(Canvas canvas, float px, float py, Paint paint) {
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

    private void drawStandardWingTips(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(3 * px, 5 * py, 4 * px, 6 * py, paint);
        canvas.drawRect(1 * px, 7 * py, 2 * px, 9 * py, paint);
        canvas.drawRect(2 * px, 9 * py, 3 * px, 10 * py, paint);
        canvas.drawRect(5 * px, 12 * py, 6 * px, 13 * py, paint);

        canvas.drawRect(24 * px, 5 * py, 25 * px, 6 * py, paint);
        canvas.drawRect(26 * px, 7 * py, 27 * px, 9 * py, paint);
        canvas.drawRect(25 * px, 9 * py, 26 * px, 10 * py, paint);
        canvas.drawRect(22 * px, 12 * py, 23 * px, 13 * py, paint);
    }

    private void drawBatWings(Canvas canvas, float px, float py, Paint paint) {
        // Scalloped pointed bat wings for Devil badge
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
        // Row 5-6: Pink
        paintAccent.setColor(colors[0]);
        canvas.drawRect(4 * px, 5 * py, 9 * px, 6 * py, paintAccent);
        canvas.drawRect(19 * px, 5 * py, 24 * px, 6 * py, paintAccent);

        // Row 6-7: Purple
        paintAccent.setColor(colors[1]);
        canvas.drawRect(3 * px, 6 * py, 10 * px, 7 * py, paintAccent);
        canvas.drawRect(18 * px, 6 * py, 25 * px, 7 * py, paintAccent);

        // Row 7-8: Cyan
        paintAccent.setColor(colors[2]);
        canvas.drawRect(2 * px, 7 * py, 11 * px, 8 * py, paintAccent);
        canvas.drawRect(17 * px, 7 * py, 26 * px, 8 * py, paintAccent);

        // Row 8-10: Mint green
        paintAccent.setColor(colors[3]);
        canvas.drawRect(1 * px, 8 * py, 11 * px, 10 * py, paintAccent);
        canvas.drawRect(17 * px, 8 * py, 27 * px, 10 * py, paintAccent);

        // Row 10-13: Gold
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

    private void drawStandardHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(10 * px, 8 * py, 13 * px, 9 * py, paint);
        canvas.drawRect(15 * px, 8 * py, 18 * px, 9 * py, paint);
        canvas.drawRect(9 * px, 9 * py, 19 * px, 10 * py, paint);
        canvas.drawRect(8 * px, 10 * py, 20 * px, 12 * py, paint);
        canvas.drawRect(9 * px, 12 * py, 19 * px, 13 * py, paint);
        canvas.drawRect(10 * px, 13 * py, 18 * px, 14 * py, paint);
        canvas.drawRect(11 * px, 14 * py, 17 * px, 15 * py, paint);
        canvas.drawRect(12 * px, 15 * py, 16 * px, 16 * py, paint);
        canvas.drawRect(13 * px, 16 * py, 15 * px, 17 * py, paint);
    }

    private void drawLeftHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(9 * px, 8 * py, 14 * px, 16 * py, paint);
    }

    private void drawRightHeart(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(14 * px, 8 * py, 19 * px, 16 * py, paint);
    }

    private void drawStandardHeartOutline(Canvas canvas, float px, float py, Paint paint) {
        canvas.drawRect(10 * px, 7 * py, 13 * px, 8 * py, paint);
        canvas.drawRect(15 * px, 7 * py, 18 * px, 8 * py, paint);
        canvas.drawRect(7 * px, 10 * py, 8 * px, 12 * py, paint);
        canvas.drawRect(20 * px, 10 * py, 21 * px, 12 * py, paint);
        canvas.drawRect(13 * px, 17 * py, 15 * px, 18 * py, paint);
    }

    private void drawHeartEyes(Canvas canvas, float px, float py, int eyeColor) {
        paintEyes.setColor(eyeColor);
        // Left Eye • and Right Eye •
        canvas.drawRect(11 * px, 10 * py, 12.5f * px, 11.5f * py, paintEyes);
        canvas.drawRect(15.5f * px, 10 * py, 17 * px, 11.5f * py, paintEyes);
    }

    private void drawSparkles(Canvas canvas, float px, float py, float phase, int sparkleColor) {
        float[][] sparks = {
                {3f, 3f, 0.10f},
                {24f, 4f, 0.55f},
                {3f, 17f, 0.80f},
                {24f, 18f, 0.35f}
        };

        paintSparkle.setColor(sparkleColor);

        for (float[] sp : sparks) {
            float t = (phase + sp[2]) % 1.0f;
            float alphaProgress = (float) Math.sin(t * Math.PI);
            int alpha = (int) (alphaProgress * 255);
            if (alpha <= 10) continue;

            float sx = sp[0] * px;
            float sy = sp[1] * py;

            paintSparkle.setAlpha(alpha);
            canvas.drawRect(sx, sy - py, sx + px, sy + 2 * py, paintSparkle);
            canvas.drawRect(sx - px, sy, sx + 2 * px, sy + py, paintSparkle);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paintFill.setAlpha(alpha);
        paintStroke.setAlpha(alpha);
        paintAccent.setAlpha(alpha);
        paintEyes.setAlpha(alpha);
        paintSparkle.setAlpha(alpha);
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
