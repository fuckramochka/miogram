package app.miogram.bridge.customui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * Native rendering engine for Miogram Custom UI Studio.
 * Bridges advanced shaders, geometry masks, neon glows, and custom typography
 * directly to Telegram chat cells, dialogs, and navigation components.
 */
public class MiogramUiEngine {

    // Name FX Identifiers (Matches Custom Profile Architecture)
    public static final int FX_NONE = 0;
    public static final int FX_GRADIENT = 2;
    public static final int FX_GLARE = 3;
    public static final int FX_RAINBOW = 4;
    public static final int FX_FIRE = 6;
    public static final int FX_ICE = 7;
    public static final int FX_CYBER_NEON = 8;

    // Avatar Shapes
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUIRCLE = 1;
    public static final int SHAPE_ROUNDED_RECT = 2;
    public static final int SHAPE_HEXAGON = 3;
    public static final int SHAPE_STAR = 4;
    public static final int SHAPE_DIAMOND = 5;

    // Reusable objects for zero-allocation 120 FPS rendering
    private static final Paint bubblePaint = new Paint();
    private static final Paint bubbleGradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final PorterDuffXfermode SRC_ATOP = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    private static final Path avatarPath = new Path();
    private static final Matrix matrix = new Matrix();

    private static int bubbleSaveCount = -1;
    private static Drawable activeBubbleDrawable = null;

    // Saved name paint states for clean restoration
    private static int savedNameColor = 0;
    private static Shader savedNameShader = null;
    private static Typeface savedNameTypeface = null;
    private static boolean nameShadowApplied = false;

    static {
        ringPaint.setStyle(Paint.Style.STROKE);
    }

    /* =========================================================================
     * 1. CHAT BUBBLES RENDERING HOOKS
     * ========================================================================= */

    public static void beforeDrawBubble(Canvas canvas, Drawable backgroundDrawable, boolean isOut) {
        bubbleSaveCount = -1;
        activeBubbleDrawable = null;
        if (!isOut || !MiogramCustomUiPrefs.isBubbleGradientEnabled() || canvas == null || backgroundDrawable == null) {
            return;
        }
        try {
            bubbleSaveCount = canvas.saveLayer(null, null);
            activeBubbleDrawable = backgroundDrawable;
        } catch (Throwable ignored) {
            bubbleSaveCount = -1;
        }
    }

    public static void afterDrawBubble(Canvas canvas) {
        if (bubbleSaveCount < 0 || canvas == null) {
            bubbleSaveCount = -1;
            activeBubbleDrawable = null;
            return;
        }
        if (activeBubbleDrawable != null) {
            Rect bounds = activeBubbleDrawable.getBounds();
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                int c1 = MiogramCustomUiPrefs.getBubbleColor1();
                int c2 = MiogramCustomUiPrefs.getBubbleColor2();
                int angle = MiogramCustomUiPrefs.getBubbleAngle();

                bubbleGradPaint.setShader(createGradient(bounds, c1, c2, angle));
                bubbleGradPaint.setXfermode(SRC_ATOP);
                canvas.drawRect(bounds, bubbleGradPaint);
                bubbleGradPaint.setXfermode(null);
                bubbleGradPaint.setShader(null);
            }
        }
        try {
            canvas.restoreToCount(bubbleSaveCount);
        } catch (Throwable ignored) {
        }
        bubbleSaveCount = -1;
        activeBubbleDrawable = null;
    }

    public static LinearGradient createGradient(Rect rect, int c1, int c2, int angle) {
        double radians = Math.toRadians(angle);
        float cx = rect.exactCenterX();
        float cy = rect.exactCenterY();
        float maxR = (float) Math.hypot(rect.width(), rect.height()) / 2f;
        float cos = (float) Math.cos(radians) * maxR;
        float sin = (float) Math.sin(radians) * maxR;
        return new LinearGradient(cx - cos, cy - sin, cx + cos, cy + sin, c1, c2, Shader.TileMode.CLAMP);
    }

    /* =========================================================================
     * 2. NAME & TEXT SHADERS (FIRE, ICE, RAINBOW, GLOW)
     * ========================================================================= */

    public static void applyNameEffect(Paint paint, int width, int baseColor) {
        if (paint == null || width <= 0) return;

        savedNameColor = paint.getColor();
        savedNameShader = paint.getShader();
        savedNameTypeface = paint.getTypeface();
        nameShadowApplied = false;

        int fx = MiogramCustomUiPrefs.getNameFx();
        if (fx == FX_NONE && !MiogramCustomUiPrefs.isNameGlowEnabled()) {
            return;
        }

        Shader fxShader = buildNameShader(fx, width, baseColor);
        if (fxShader != null) {
            paint.setShader(fxShader);
        }

        Typeface tf = getCustomTypeface(MiogramCustomUiPrefs.getNameFont());
        if (tf != null) {
            paint.setTypeface(tf);
        }

        if (MiogramCustomUiPrefs.isNameGlowEnabled()) {
            int glowColor = MiogramCustomUiPrefs.getNameGlowColor();
            float radius = AndroidUtilities.dp(Math.max(1, MiogramCustomUiPrefs.getNameGlowRadius()));
            paint.setShadowLayer(radius, 0, 0, glowColor);
            nameShadowApplied = true;
        }
    }

    public static void restoreNameEffect(Paint paint) {
        if (paint == null) return;
        paint.setColor(savedNameColor);
        paint.setShader(savedNameShader);
        if (savedNameTypeface != null) {
            paint.setTypeface(savedNameTypeface);
        }
        if (nameShadowApplied) {
            paint.clearShadowLayer();
            nameShadowApplied = false;
        }
    }

    public static Shader buildNameShader(int fx, int width, int baseColor) {
        float fW = Math.max(1, width);
        float phase = (SystemClock.elapsedRealtime() % 2500L) / 2500f; // Smooth animation wave

        switch (fx) {
            case FX_FIRE: // Flame fire gradient: crimson -> fiery orange -> bright gold
                return new LinearGradient(0, 0, fW, 0,
                        new int[]{0xFFFF2A2A, 0xFFFF7A00, 0xFFFFD700, 0xFFFF7A00, 0xFFFF2A2A},
                        new float[]{0f, 0.25f, 0.5f, 0.75f, 1f}, Shader.TileMode.MIRROR);

            case FX_ICE: // Frost ice crystals: deep cyan -> light frost cyan -> pure white
                return new LinearGradient(0, 0, fW, 0,
                        new int[]{0xFF00C7FF, 0xFF54D6FF, 0xFFFFFFFF, 0xFF54D6FF, 0xFF00C7FF},
                        new float[]{0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.MIRROR);

            case FX_RAINBOW: // Animated 7-color RGB spectrum
                matrix.reset();
                matrix.setTranslate(phase * fW, 0);
                LinearGradient rainbow = new LinearGradient(0, 0, fW, 0,
                        new int[]{0xFFFF0055, 0xFFFF7700, 0xFFFFDD00, 0xFF00FF66, 0xFF00CCFF, 0xFF7700FF, 0xFFFF0055},
                        null, Shader.TileMode.REPEAT);
                rainbow.setLocalMatrix(matrix);
                return rainbow;

            case FX_GLARE: // Shimmer glare passing light beam
                matrix.reset();
                matrix.setTranslate(((phase * 2f) - 1f) * fW, 0);
                LinearGradient glare = new LinearGradient(0, 0, fW, 0,
                        new int[]{baseColor, baseColor, 0xFFFFFFFF, baseColor, baseColor},
                        new float[]{0f, 0.35f, 0.5f, 0.65f, 1f}, Shader.TileMode.CLAMP);
                glare.setLocalMatrix(matrix);
                return glare;

            case FX_CYBER_NEON: // Neon Magenta to Cyber Cyan
                return new LinearGradient(0, 0, fW, 0,
                        new int[]{0xFFFF007F, 0xFF00F0FF, 0xFFFF007F},
                        new float[]{0f, 0.5f, 1f}, Shader.TileMode.MIRROR);

            case FX_GRADIENT: // User custom dual gradient
                int c1 = MiogramCustomUiPrefs.getNameGradC1();
                int c2 = MiogramCustomUiPrefs.getNameGradC2();
                return new LinearGradient(0, 0, fW, 0,
                        new int[]{c1, c2, c1},
                        new float[]{0f, 0.5f, 1f}, Shader.TileMode.MIRROR);

            default:
                return null;
        }
    }

    public static Typeface getCustomTypeface(int fontId) {
        switch (fontId) {
            case 1: return Typeface.create("sans-serif-medium", Typeface.BOLD);
            case 2: return Typeface.create("monospace", Typeface.BOLD);
            case 3: return Typeface.create("serif", Typeface.BOLD_ITALIC);
            case 4: return Typeface.create("casual", Typeface.BOLD);
            default: return null;
        }
    }

    /* =========================================================================
     * 3. AVATAR SHAPES CLIPPING & GLOW RINGS
     * ========================================================================= */

    public static Path getAvatarShapePath(RectF rect, int shape) {
        avatarPath.reset();
        float w = rect.width();
        float h = rect.height();
        float cx = rect.centerX();
        float cy = rect.centerY();

        switch (shape) {
            case SHAPE_SQUIRCLE: // Smooth iOS squircle
                avatarPath.addRoundRect(rect, w * 0.28f, h * 0.28f, Path.Direction.CW);
                break;

            case SHAPE_ROUNDED_RECT:
                avatarPath.addRoundRect(rect, AndroidUtilities.dp(8), AndroidUtilities.dp(8), Path.Direction.CW);
                break;

            case SHAPE_HEXAGON: // 6-point regular polygon
                float r = Math.min(w, h) / 2f;
                for (int i = 0; i < 6; i++) {
                    double rad = Math.toRadians(60 * i);
                    float x = (float) (cx + r * Math.cos(rad));
                    float y = (float) (cy + r * Math.sin(rad));
                    if (i == 0) avatarPath.moveTo(x, y);
                    else avatarPath.lineTo(x, y);
                }
                avatarPath.close();
                break;

            case SHAPE_STAR: // 8-point geometric star
                float rOuter = Math.min(w, h) / 2f;
                float rInner = rOuter * 0.65f;
                for (int i = 0; i < 16; i++) {
                    double rad = Math.toRadians(22.5 * i);
                    float curR = (i % 2 == 0) ? rOuter : rInner;
                    float x = (float) (cx + curR * Math.cos(rad));
                    float y = (float) (cy + curR * Math.sin(rad));
                    if (i == 0) avatarPath.moveTo(x, y);
                    else avatarPath.lineTo(x, y);
                }
                avatarPath.close();
                break;

            case SHAPE_DIAMOND: // Rotated diamond
                avatarPath.moveTo(cx, rect.top);
                avatarPath.lineTo(rect.right, cy);
                avatarPath.lineTo(cx, rect.bottom);
                avatarPath.lineTo(rect.left, cy);
                avatarPath.close();
                break;

            case SHAPE_CIRCLE:
            default:
                avatarPath.addCircle(cx, cy, Math.min(w, h) / 2f, Path.Direction.CW);
                break;
        }
        return avatarPath;
    }

    public static void drawAvatarGlowRing(Canvas canvas, RectF rect) {
        if (!MiogramCustomUiPrefs.isAvatarRingEnabled() || canvas == null || rect == null) {
            return;
        }
        float stroke = AndroidUtilities.dp(MiogramCustomUiPrefs.getAvatarRingWidth());
        ringPaint.setStrokeWidth(stroke);
        float inset = stroke / 2f + AndroidUtilities.dp(1.5f);

        RectF ringRect = new RectF(rect.left - inset, rect.top - inset, rect.right + inset, rect.bottom + inset);
        int ringColor = MiogramCustomUiPrefs.getAvatarRingColor();

        int shape = MiogramCustomUiPrefs.getAvatarShape();
        if (MiogramCustomUiPrefs.isAvatarRingPulse()) {
            float phase = (SystemClock.elapsedRealtime() % 1600L) / 1600f;
            int alpha = (int) (160 + 95 * Math.sin(phase * Math.PI * 2));
            ringPaint.setColor((ringColor & 0x00FFFFFF) | (alpha << 24));
        } else {
            ringPaint.setColor(ringColor);
        }

        Path ringPath = getAvatarShapePath(ringRect, shape);
        canvas.drawPath(ringPath, ringPaint);
    }
}
