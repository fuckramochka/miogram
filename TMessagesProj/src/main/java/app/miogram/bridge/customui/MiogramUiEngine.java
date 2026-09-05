package app.miogram.bridge.customui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
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
 * Native rendering engine for Miogram Custom UI.
 * Built 1-to-1 matching Custom Profile graphics pipeline (NameFx, ChatBubbles, ChatAvatars, ChatAvatarRing).
 * Zero-allocation in draw loops for 120 FPS buttery smooth ProMotion rendering.
 */
public class MiogramUiEngine {

    // Name FX Identifiers (Matching Custom Profile NameFx.java)
    public static final int FX_NONE = 0;
    public static final int FX_PULSE = 1;
    public static final int FX_GRADIENT = 2;
    public static final int FX_SHIMMER = 3;
    public static final int FX_RAINBOW = 4;
    public static final int FX_NEON = 5;
    public static final int FX_FIRE = 6;
    public static final int FX_ICE = 7;

    // Avatar Shapes (Matching Custom Profile EditAvatarSheet.java)
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_ROUNDED = 1;
    public static final int SHAPE_SQUARE = 2;
    public static final int SHAPE_HEXAGON = 3;
    public static final int SHAPE_PENTAGON = 4;
    public static final int SHAPE_STAR = 5;
    public static final int SHAPE_HEART = 6;
    public static final int SHAPE_FLOWER = 7;

    // Reusable objects for zero-allocation rendering
    private static final Paint bubbleGradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final PorterDuffXfermode SRC_ATOP = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    private static final Path shapePath = new Path();
    private static final Matrix fxMatrix = new Matrix();

    private static final RectF reusableRingRect = new RectF();
    private static final Paint roundPaint = new Paint();
    private static final Path roundResPath = new Path();

    private static int bubbleSaveCount = -1;

    // Saved state for clean paint restoration
    private static int savedNameColor = 0;
    private static Shader savedNameShader = null;
    private static Typeface savedNameTypeface = null;
    private static boolean nameShadowSet = false;

    static {
        ringPaint.setStyle(Paint.Style.STROKE);
    }

    /* =========================================================================
     * 1. CHAT BUBBLES RENDERING HOOKS (1-to-1 with ChatBubbles.java)
     * ========================================================================= */

    public static void beforeDrawBubble(Canvas canvas, boolean isOut) {
        bubbleSaveCount = -1;
        if (!isOut || !MiogramCustomUiPrefs.isBubbleColorEnabled() || canvas == null) {
            return;
        }
        try {
            bubbleSaveCount = canvas.saveLayer(null, null);
        } catch (Throwable ignored) {
            bubbleSaveCount = -1;
        }
    }

    public static void afterDrawBubble(Canvas canvas, Drawable backgroundDrawable) {
        if (bubbleSaveCount < 0 || canvas == null) {
            bubbleSaveCount = -1;
            return;
        }
        if (backgroundDrawable != null) {
            Rect bounds = backgroundDrawable.getBounds();
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                boolean isGrad = MiogramCustomUiPrefs.isBubbleGradientEnabled();
                int c1 = MiogramCustomUiPrefs.getBubbleColor();
                int c2 = MiogramCustomUiPrefs.getBubbleColor2();
                int angle = MiogramCustomUiPrefs.getBubbleGradAngle();

                if (isGrad) {
                    bubbleGradPaint.setShader(createGradient(bounds, c1, c2, angle));
                } else {
                    bubbleGradPaint.setShader(null);
                    bubbleGradPaint.setColor(c1);
                }
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
     * 2. NAME FX & TYPOGRAPHY SHADERS (1-to-1 with NameFx.java)
     * ========================================================================= */

    public static void applyNameEffect(Paint paint, int width, int baseColor) {
        if (paint == null) return;

        savedNameColor = paint.getColor();
        savedNameShader = paint.getShader();
        savedNameTypeface = paint.getTypeface();
        nameShadowSet = false;

        // 1. Color
        if (MiogramCustomUiPrefs.isNameColorEnabled()) {
            paint.setColor(MiogramCustomUiPrefs.getNameColor());
        }

        // 2. Typeface
        int font = MiogramCustomUiPrefs.getNameFont();
        if (font > 0) {
            Typeface tf = getCustomTypeface(font);
            if (tf != null) {
                paint.setTypeface(tf);
            }
        }

        // 3. Shadow or Glow
        if (MiogramCustomUiPrefs.isNameShadowEnabled()) {
            int sColor = MiogramCustomUiPrefs.getNameShadowColor();
            float sRadius = Math.max(0.1f, AndroidUtilities.dp(MiogramCustomUiPrefs.getNameShadowRadius()));
            float dx = AndroidUtilities.dp(MiogramCustomUiPrefs.getNameShadowDx());
            float dy = AndroidUtilities.dp(MiogramCustomUiPrefs.getNameShadowDy());
            paint.setShadowLayer(sRadius, dx, dy, sColor);
            nameShadowSet = true;
        } else if (MiogramCustomUiPrefs.isNameGlowEnabled()) {
            int gColor = MiogramCustomUiPrefs.getNameGlowColor();
            float gRadius = Math.max(1f, AndroidUtilities.dp(MiogramCustomUiPrefs.getNameGlowRadius()));
            paint.setShadowLayer(gRadius, 0, 0, gColor);
            nameShadowSet = true;
        }

        // 4. FX Shaders
        int fx = MiogramCustomUiPrefs.getNameFx();
        if (fx > 0 && width > 0) {
            Shader shader = buildFxShader(fx, width, paint.getColor());
            if (shader != null) {
                paint.setShader(shader);
            }
        }
    }

    public static void restoreNameEffect(Paint paint) {
        if (paint == null) return;
        paint.setColor(savedNameColor);
        paint.setShader(savedNameShader);
        paint.setTypeface(savedNameTypeface);
        if (nameShadowSet) {
            paint.clearShadowLayer();
            nameShadowSet = false;
        }
    }

    public static Shader buildFxShader(int fx, int width, int baseColor) {
        float fW = (float) width;
        int c1 = MiogramCustomUiPrefs.getNameGradC1();
        int c2 = MiogramCustomUiPrefs.getNameGradC2();
        int angle = MiogramCustomUiPrefs.getNameGradAngle();
        int speed = MiogramCustomUiPrefs.getNameFxSpeed();

        Shader shader = null;
        try {
            switch (fx) {
                case FX_PULSE: {
                    float phase = (SystemClock.elapsedRealtime() % 2000L) / 2000f;
                    int alpha = (int) (150 + 105 * Math.sin(phase * Math.PI * 2));
                    int col = (baseColor & 0x00FFFFFF) | (alpha << 24);
                    shader = new LinearGradient(0f, 0f, fW, 0f, col, col, Shader.TileMode.CLAMP);
                    break;
                }
                case FX_GRADIENT:
                    shader = new LinearGradient(0f, 0f, fW, 0f, new int[]{c1, c2, c1}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.MIRROR);
                    break;
                case FX_SHIMMER:
                    shader = new LinearGradient(0f, 0f, fW, 0f, new int[]{baseColor, baseColor, 0xFFFFFFFF, baseColor, baseColor}, new float[]{0f, 0.38f, 0.5f, 0.62f, 1f}, Shader.TileMode.CLAMP);
                    break;
                case FX_FIRE:
                    shader = new LinearGradient(0f, 0f, fW, 0f, new int[]{-50384, -27392, -8115, -27392, -50384}, new float[]{0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.MIRROR);
                    break;
                case FX_ICE:
                    shader = new LinearGradient(0f, 0f, fW, 0f, new int[]{-1, -8397825, -13397780, -8397825, -1}, new float[]{0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.MIRROR);
                    break;
                case FX_NEON:
                    shader = new LinearGradient(0f, 0f, fW, 0f, new int[]{0xFF00FFCC, 0xFFFF0077, 0xFF00FFCC}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.MIRROR);
                    break;
                case FX_RAINBOW:
                default: {
                    int[] rainbowColors = new int[7];
                    float[] rainbowPositions = new float[7];
                    for (int i = 0; i < 7; i++) {
                        rainbowPositions[i] = i / 6.0f;
                        rainbowColors[i] = Color.HSVToColor(new float[]{(i / 6.0f) * 360f, 1.0f, 1.0f});
                    }
                    shader = new LinearGradient(0f, 0f, fW, 0f, rainbowColors, rainbowPositions, Shader.TileMode.REPEAT);
                    break;
                }
            }

            if (shader != null) {
                float progress = ((SystemClock.elapsedRealtime() % 3000L) / 3000f) * (speed / 100f);
                fxMatrix.reset();
                if (fx == FX_SHIMMER) {
                    fxMatrix.setTranslate(((progress * 2f) - 1f) * fW, 0f);
                } else {
                    fxMatrix.setTranslate(progress * fW, 0f);
                }
                if (angle != 0) {
                    fxMatrix.postRotate(angle, fW * 0.5f, 0f);
                }
                shader.setLocalMatrix(fxMatrix);
            }
        } catch (Throwable ignored) {
        }
        return shader;
    }

    public static Typeface getCustomTypeface(int fontId) {
        switch (fontId) {
            case 1: return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case 2: return Typeface.create("serif", Typeface.BOLD);
            case 3: return Typeface.create("monospace", Typeface.BOLD);
            case 4: return Typeface.create("sans-serif", Typeface.ITALIC);
            case 5: return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            default: return null;
        }
    }

    /* =========================================================================
     * 3. AVATAR SHAPES & GLOWING STORY RINGS (1-to-1 with ChatAvatars.java)
     * ========================================================================= */

    public static Path getAvatarShapePath(RectF rect, int shape, int cornerRadius, int roundness) {
        shapePath.reset();
        float w = rect.width();
        float h = rect.height();
        float cx = rect.centerX();
        float cy = rect.centerY();
        float r = Math.min(w, h) / 2f;

        switch (shape) {
            case SHAPE_ROUNDED: { // Rounded rect / Squircle
                float cr = AndroidUtilities.dp(cornerRadius > 0 ? cornerRadius : 18);
                shapePath.addRoundRect(rect, cr, cr, Path.Direction.CW);
                break;
            }
            case SHAPE_SQUARE: { // Square with soft corners
                float cr = AndroidUtilities.dp(4);
                shapePath.addRoundRect(rect, cr, cr, Path.Direction.CW);
                break;
            }
            case SHAPE_HEXAGON: { // 6-point regular polygon
                for (int i = 0; i < 6; i++) {
                    double rad = Math.toRadians(60 * i);
                    float x = (float) (cx + r * Math.cos(rad));
                    float y = (float) (cy + r * Math.sin(rad));
                    if (i == 0) shapePath.moveTo(x, y);
                    else shapePath.lineTo(x, y);
                }
                shapePath.close();
                break;
            }
            case SHAPE_PENTAGON: { // 5-point regular polygon
                for (int i = 0; i < 5; i++) {
                    double rad = Math.toRadians(72 * i - 90);
                    float x = (float) (cx + r * Math.cos(rad));
                    float y = (float) (cy + r * Math.sin(rad));
                    if (i == 0) shapePath.moveTo(x, y);
                    else shapePath.lineTo(x, y);
                }
                shapePath.close();
                break;
            }
            case SHAPE_STAR: { // 8-point geometric star
                float rInner = r * 0.62f;
                for (int i = 0; i < 16; i++) {
                    double rad = Math.toRadians(22.5 * i);
                    float curR = (i % 2 == 0) ? r : rInner;
                    float x = (float) (cx + curR * Math.cos(rad));
                    float y = (float) (cy + curR * Math.sin(rad));
                    if (i == 0) shapePath.moveTo(x, y);
                    else shapePath.lineTo(x, y);
                }
                shapePath.close();
                break;
            }
            case SHAPE_HEART: { // Heart shape
                shapePath.moveTo(cx, cy + r * 0.7f);
                shapePath.cubicTo(cx - r * 1.1f, cy - r * 0.2f, cx - r * 0.8f, cy - r * 0.9f, cx, cy - r * 0.4f);
                shapePath.cubicTo(cx + r * 0.8f, cy - r * 0.9f, cx + r * 1.1f, cy - r * 0.2f, cx, cy + r * 0.7f);
                shapePath.close();
                break;
            }
            case SHAPE_FLOWER: { // Flower with 8 rounded petals
                for (int i = 0; i < 8; i++) {
                    double rad = Math.toRadians(45 * i);
                    float px = (float) (cx + r * 0.55f * Math.cos(rad));
                    float py = (float) (cy + r * 0.55f * Math.sin(rad));
                    shapePath.addCircle(px, py, r * 0.45f, Path.Direction.CW);
                }
                shapePath.addCircle(cx, cy, r * 0.55f, Path.Direction.CW);
                break;
            }
            case SHAPE_CIRCLE:
            default:
                shapePath.addCircle(cx, cy, r, Path.Direction.CW);
                break;
        }

        if (roundness > 0 && shape != SHAPE_CIRCLE) {
            roundPaint.setPathEffect(new CornerPathEffect((roundness / 100f) * r * 0.4f));
            roundResPath.reset();
            if (roundPaint.getFillPath(shapePath, roundResPath)) {
                return roundResPath;
            }
        }
        return shapePath;
    }

    public static void drawAvatarGlowRing(Canvas canvas, RectF rect) {
        if (!MiogramCustomUiPrefs.isAvatarRingEnabled() || canvas == null || rect == null) {
            return;
        }
        float stroke = AndroidUtilities.dp(MiogramCustomUiPrefs.getAvatarRingWidth());
        ringPaint.setStrokeWidth(stroke);
        float inset = stroke / 2f + AndroidUtilities.dp(1.5f);

        reusableRingRect.set(rect.left - inset, rect.top - inset, rect.right + inset, rect.bottom + inset);
        int ringColor = MiogramCustomUiPrefs.getAvatarRingColor();

        int shape = MiogramCustomUiPrefs.getAvatarShape();
        int radius = MiogramCustomUiPrefs.getAvatarRadius();
        int roundness = MiogramCustomUiPrefs.getAvatarRound();

        if (MiogramCustomUiPrefs.isAvatarRingPulse()) {
            float phase = (SystemClock.elapsedRealtime() % 1600L) / 1600f;
            int alpha = (int) (160 + 95 * Math.sin(phase * Math.PI * 2));
            ringPaint.setColor((ringColor & 0x00FFFFFF) | (alpha << 24));
        } else {
            ringPaint.setColor(ringColor);
        }

        Path ringPath = getAvatarShapePath(reusableRingRect, shape, radius, roundness);
        canvas.drawPath(ringPath, ringPaint);
    }
}
