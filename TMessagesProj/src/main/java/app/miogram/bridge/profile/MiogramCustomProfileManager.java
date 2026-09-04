package app.miogram.bridge.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.io.File;

/**
 * 100% Native Custom Profile Manager for Miogram.
 * Fully replaces the legacy Xposed/DEX core with native Java canvas rendering.
 * Provides custom banners, avatar shapes, neon glows, glass blocks, and thought bubbles.
 */
public class MiogramCustomProfileManager {

    private static final String PREFS_NAME = "miogram_custom_profile";

    // Banner Modes
    public static final int BANNER_MODE_DEFAULT = 0;
    public static final int BANNER_MODE_SOLID = 1;
    public static final int BANNER_MODE_LINEAR = 2;
    public static final int BANNER_MODE_RADIAL = 3;
    public static final int BANNER_MODE_MESH = 4;
    public static final int BANNER_MODE_PHOTO = 5;

    // Avatar Shapes
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUIRCLE = 1;
    public static final int SHAPE_HEXAGON = 2;
    public static final int SHAPE_STAR = 3;
    public static final int SHAPE_HEART = 4;
    public static final int SHAPE_DIAMOND = 5;

    public static void init(Context context) {
    }

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Configuration Getters & Setters ---

    public static boolean isEnabled() {
        return getPrefs().getBoolean("enabled", true);
    }

    public static void setEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("enabled", enabled).apply();
    }

    public static int getBannerMode() {
        return getPrefs().getInt("banner_mode", BANNER_MODE_MESH);
    }

    public static void setBannerMode(int mode) {
        getPrefs().edit().putInt("banner_mode", mode).apply();
    }

    public static int getBannerSolidColor() {
        return getPrefs().getInt("banner_color", Color.parseColor("#261C37"));
    }

    public static void setBannerSolidColor(int color) {
        getPrefs().edit().putInt("banner_color", color).apply();
    }

    public static int getGradC1() {
        return getPrefs().getInt("grad_c1", Color.parseColor("#FF55A3"));
    }

    public static void setGradC1(int color) {
        getPrefs().edit().putInt("grad_c1", color).apply();
    }

    public static int getGradC2() {
        return getPrefs().getInt("grad_c2", Color.parseColor("#7B2CBF"));
    }

    public static void setGradC2(int color) {
        getPrefs().edit().putInt("grad_c2", color).apply();
    }

    public static int getGradC3() {
        return getPrefs().getInt("grad_c3", Color.parseColor("#00F5D4"));
    }

    public static void setGradC3(int color) {
        getPrefs().edit().putInt("grad_c3", color).apply();
    }

    public static float getGradAngle() {
        return getPrefs().getFloat("grad_angle", 45f);
    }

    public static void setGradAngle(float angle) {
        getPrefs().edit().putFloat("grad_angle", angle).apply();
    }

    public static String getBannerPhotoPath() {
        return getPrefs().getString("banner_path", null);
    }

    public static void setBannerPhotoPath(String path) {
        getPrefs().edit().putString("banner_path", path).apply();
        cachedBannerBitmap = null;
    }

    public static int getAvatarShape() {
        return getPrefs().getInt("avatar_shape", SHAPE_SQUIRCLE);
    }

    public static void setAvatarShape(int shape) {
        getPrefs().edit().putInt("avatar_shape", shape).apply();
    }

    public static boolean isAvatarGlowEnabled() {
        return getPrefs().getBoolean("avatar_glow_enabled", true);
    }

    public static void setAvatarGlowEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("avatar_glow_enabled", enabled).apply();
    }

    public static int getAvatarGlowColor() {
        return getPrefs().getInt("avatar_glow_color", Color.parseColor("#FF55A3"));
    }

    public static void setAvatarGlowColor(int color) {
        getPrefs().edit().putInt("avatar_glow_color", color).apply();
    }

    public static String getThoughtText() {
        return getPrefs().getString("thought_text", "✦ Miogram ✦");
    }

    public static void setThoughtText(String text) {
        getPrefs().edit().putString("thought_text", text).apply();
    }

    public static boolean isThoughtEnabled() {
        return getPrefs().getBoolean("thought_enabled", true);
    }

    public static void setThoughtEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("thought_enabled", enabled).apply();
    }

    public static int getThoughtBgColor() {
        return getPrefs().getInt("thought_bg_color", Color.parseColor("#D02B1D3A"));
    }

    public static void setThoughtBgColor(int color) {
        getPrefs().edit().putInt("thought_bg_color", color).apply();
    }

    public static int getThoughtTextColor() {
        return getPrefs().getInt("thought_text_color", Color.WHITE);
    }

    public static void setThoughtTextColor(int color) {
        getPrefs().edit().putInt("thought_text_color", color).apply();
    }

    public static boolean isBlocksColorEnabled() {
        return getPrefs().getBoolean("blocks_color_enabled", false);
    }

    public static void setBlocksColorEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("blocks_color_enabled", enabled).apply();
    }

    public static int getBlocksColor() {
        return getPrefs().getInt("blocks_color", Color.parseColor("#D01C1428"));
    }

    public static void setBlocksColor(int color) {
        getPrefs().edit().putInt("blocks_color", color).apply();
    }

    // --- Rendering Pipelines ---

    private static Bitmap cachedBannerBitmap = null;
    private static String cachedBannerPath = null;
    private static final Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint thoughtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint thoughtTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        glowPaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(2));
        thoughtTextPaint.setTextSize(AndroidUtilities.dp(12));
        thoughtTextPaint.setTypeface(AndroidUtilities.bold());
    }

    public static boolean shouldApplyToUser(long userId) {
        if (!isEnabled()) return false;
        long myId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        // Applies to self profile or founder
        return userId == myId || userId == 0;
    }

    /**
     * Draws the custom profile banner background into TopView.onDraw.
     */
    public static void drawBanner(Canvas canvas, int width, int height, long userId) {
        if (!shouldApplyToUser(userId) || width <= 0 || height <= 0) return;

        int mode = getBannerMode();
        if (mode == BANNER_MODE_DEFAULT) return;

        if (mode == BANNER_MODE_SOLID) {
            bannerPaint.setShader(null);
            bannerPaint.setColor(getBannerSolidColor());
            canvas.drawRect(0, 0, width, height, bannerPaint);
        } else if (mode == BANNER_MODE_LINEAR) {
            float angle = (float) Math.toRadians(getGradAngle());
            float x1 = (float) (width * Math.cos(angle));
            float y1 = (float) (height * Math.sin(angle));
            LinearGradient lg = new LinearGradient(0, 0, x1, y1,
                    new int[]{getGradC1(), getGradC2(), getGradC3()},
                    new float[]{0.0f, 0.55f, 1.0f}, Shader.TileMode.CLAMP);
            bannerPaint.setShader(lg);
            canvas.drawRect(0, 0, width, height, bannerPaint);
        } else if (mode == BANNER_MODE_RADIAL) {
            float radius = Math.max(width, height) * 0.9f;
            RadialGradient rg = new RadialGradient(width * 0.5f, height * 0.4f, radius,
                    new int[]{getGradC1(), getGradC2(), getGradC3()},
                    new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
            bannerPaint.setShader(rg);
            canvas.drawRect(0, 0, width, height, bannerPaint);
        } else if (mode == BANNER_MODE_MESH) {
            // High-aesthetic Needy Girl Overdose / PC-98 Cyber Mesh
            LinearGradient bgLg = new LinearGradient(0, 0, width, height,
                    new int[]{getGradC2(), Color.parseColor("#150A21")},
                    null, Shader.TileMode.CLAMP);
            bannerPaint.setShader(bgLg);
            canvas.drawRect(0, 0, width, height, bannerPaint);

            // Layer 1: Radiant Pink orb top-left
            RadialGradient orb1 = new RadialGradient(width * 0.2f, height * 0.3f, width * 0.7f,
                    new int[]{Color.argb(200, Color.red(getGradC1()), Color.green(getGradC1()), Color.blue(getGradC1())), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
            bannerPaint.setShader(orb1);
            canvas.drawRect(0, 0, width, height, bannerPaint);

            // Layer 2: Radiant Cyan orb bottom-right
            RadialGradient orb2 = new RadialGradient(width * 0.85f, height * 0.8f, width * 0.65f,
                    new int[]{Color.argb(180, Color.red(getGradC3()), Color.green(getGradC3()), Color.blue(getGradC3())), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
            bannerPaint.setShader(orb2);
            canvas.drawRect(0, 0, width, height, bannerPaint);
        } else if (mode == BANNER_MODE_PHOTO) {
            String path = getBannerPhotoPath();
            if (!TextUtils.isEmpty(path)) {
                if (cachedBannerBitmap == null || !TextUtils.equals(cachedBannerPath, path)) {
                    try {
                        File file = new File(path);
                        if (file.exists()) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(path, opts);
                            opts.inSampleSize = Math.max(1, opts.outWidth / Math.max(1, width));
                            opts.inJustDecodeBounds = false;
                            cachedBannerBitmap = BitmapFactory.decodeFile(path, opts);
                            cachedBannerPath = path;
                        }
                    } catch (Throwable ignore) {
                    }
                }
                if (cachedBannerBitmap != null && !cachedBannerBitmap.isRecycled()) {
                    RectF src = new RectF(0, 0, cachedBannerBitmap.getWidth(), cachedBannerBitmap.getHeight());
                    RectF dst = new RectF(0, 0, width, height);
                    canvas.drawBitmap(cachedBannerBitmap, null, dst, null);

                    // Scrim gradient for readability of profile header text
                    LinearGradient scrim = new LinearGradient(0, height * 0.4f, 0, height,
                            Color.TRANSPARENT, Color.argb(190, 10, 8, 16), Shader.TileMode.CLAMP);
                    bannerPaint.setShader(scrim);
                    canvas.drawRect(0, 0, width, height, bannerPaint);
                    return;
                }
            }
            // Fallback if photo missing
            bannerPaint.setShader(null);
            bannerPaint.setColor(getBannerSolidColor());
            canvas.drawRect(0, 0, width, height, bannerPaint);
        }
    }

    /**
     * Generates a geometric path for the given shape type to clip avatars.
     */
    public static Path getAvatarShapePath(int width, int height, int shapeType) {
        if (width <= 0 || height <= 0) return null;
        Path path = new Path();
        float w = width;
        float h = height;
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(cx, cy);

        switch (shapeType) {
            case SHAPE_SQUIRCLE: {
                float corner = r * 0.52f;
                RectF rect = new RectF(0, 0, w, h);
                path.addRoundRect(rect, corner, corner, Path.Direction.CW);
                return path;
            }
            case SHAPE_HEXAGON: {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(60 * i - 30);
                    float x = (float) (cx + r * Math.cos(angle));
                    float y = (float) (cy + r * Math.sin(angle));
                    if (i == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                path.close();
                return path;
            }
            case SHAPE_STAR: {
                float innerR = r * 0.48f;
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(36 * i - 90);
                    float radius = (i % 2 == 0) ? r : innerR;
                    float x = (float) (cx + radius * Math.cos(angle));
                    float y = (float) (cy + radius * Math.sin(angle));
                    if (i == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                path.close();
                return path;
            }
            case SHAPE_HEART: {
                // Smooth Ame-chan cubic heart
                path.moveTo(cx, cy + r * 0.85f);
                path.cubicTo(cx - r * 1.05f, cy + r * 0.35f, cx - r * 1.15f, cy - r * 0.7f, cx - r * 0.45f, cy - r * 0.7f);
                path.cubicTo(cx - r * 0.1f, cy - r * 0.7f, cx, cy - r * 0.35f, cx, cy - r * 0.35f);
                path.cubicTo(cx, cy - r * 0.35f, cx + r * 0.1f, cy - r * 0.7f, cx + r * 0.45f, cy - r * 0.7f);
                path.cubicTo(cx + r * 1.15f, cy - r * 0.7f, cx + r * 1.05f, cy + r * 0.35f, cx, cy + r * 0.85f);
                path.close();
                return path;
            }
            case SHAPE_DIAMOND: {
                float pad = r * 0.08f;
                path.moveTo(cx, pad);
                path.lineTo(w - pad, cy);
                path.lineTo(cx, h - pad);
                path.lineTo(pad, cy);
                path.close();
                return path;
            }
            case SHAPE_CIRCLE:
            default:
                path.addCircle(cx, cy, r, Path.Direction.CW);
                return path;
        }
    }

    /**
     * Draws neon glow and smooth accent border around the custom avatar shape.
     */
    public static void drawAvatarGlowAndBorder(Canvas canvas, int width, int height, int shapeType) {
        if (!isAvatarGlowEnabled() || width <= 0 || height <= 0) return;
        Path path = getAvatarShapePath(width, height, shapeType);
        if (path == null) return;

        int glowColor = getAvatarGlowColor();

        // Neon Glow Outline
        glowPaint.setColor(Color.argb(110, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)));
        glowPaint.setStrokeWidth(AndroidUtilities.dp(5));
        canvas.drawPath(path, glowPaint);

        // Crisp Core Border
        strokePaint.setColor(glowColor);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(2));
        canvas.drawPath(path, strokePaint);
    }

    /**
     * Draws an interactive thought bubble over/beside the avatar.
     */
    public static void drawThoughtBubble(Canvas canvas, float avatarX, float avatarY, float avatarW) {
        if (!isThoughtEnabled()) return;
        String text = getThoughtText();
        if (TextUtils.isEmpty(text)) return;

        float textWidth = thoughtTextPaint.measureText(text);
        float padH = AndroidUtilities.dp(12);
        float padV = AndroidUtilities.dp(6);
        float bubbleW = textWidth + padH * 2;
        float bubbleH = AndroidUtilities.dp(24);

        float bubbleX = avatarX + (avatarW - bubbleW) / 2f;
        float bubbleY = avatarY - bubbleH - AndroidUtilities.dp(8);

        if (bubbleY < AndroidUtilities.dp(10)) {
            bubbleY = avatarY + AndroidUtilities.dp(6);
        }

        RectF bubbleRect = new RectF(bubbleX, bubbleY, bubbleX + bubbleW, bubbleY + bubbleH);
        thoughtBgPaint.setColor(getThoughtBgColor());
        thoughtBgPaint.setStyle(Paint.Style.FILL);
        float radius = AndroidUtilities.dp(12);
        canvas.drawRoundRect(bubbleRect, radius, radius, thoughtBgPaint);

        // Thin neon border for bubble
        strokePaint.setColor(Color.argb(150, 255, 85, 163));
        strokePaint.setStrokeWidth(AndroidUtilities.dp(1));
        canvas.drawRoundRect(bubbleRect, radius, radius, strokePaint);

        // Bubble Text
        thoughtTextPaint.setColor(getThoughtTextColor());
        Paint.FontMetricsInt fm = thoughtTextPaint.getFontMetricsInt();
        float baseline = bubbleRect.centerY() - (fm.descent + fm.ascent) / 2f;
        canvas.drawText(text, bubbleX + padH, baseline, thoughtTextPaint);
    }
}
