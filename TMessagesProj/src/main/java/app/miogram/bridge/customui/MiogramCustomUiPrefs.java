package app.miogram.bridge.customui;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

/**
 * Preferences manager for Miogram Custom UI Studio.
 * Built 1-to-1 in the exact style, naming, and architecture of Custom Profile.
 * Provides granular state storage for every single parameter of message bubbles,
 * name shaders, avatar geometries, and client UI.
 */
public class MiogramCustomUiPrefs {

    private static final String PREFS_NAME = "miogram_custom_ui_prefs";

    // 1. Message Bubbles (Exact Custom Profile keys)
    public static final String KEY_BUBBLE_COLOR_ENABLED = "bubble_color_enabled";
    public static final String KEY_BUBBLE_GRADIENT = "bubble_gradient";
    public static final String KEY_BUBBLE_COLOR = "bubble_color";
    public static final String KEY_BUBBLE_COLOR2 = "bubble_color2";
    public static final String KEY_BUBBLE_GRAD_ANGLE = "bubble_grad_angle";
    public static final String KEY_BUBBLE_TEXT_COLOR = "bubble_text_color";
    public static final String KEY_BUBBLE_RADIUS = "bubble_radius";
    public static final String KEY_BUBBLE_GLOW_ENABLED = "bubble_glow_enabled";
    public static final String KEY_BUBBLE_GLOW_COLOR = "bubble_glow_color";
    public static final String KEY_BUBBLE_GLOW_RADIUS = "bubble_glow_radius";

    // 2. Name & Text FX (Exact Custom Profile keys)
    public static final String KEY_NAME_COLOR_ENABLED = "name_color_enabled";
    public static final String KEY_NAME_COLOR = "name_color";
    public static final String KEY_NAME_GLOW_ENABLED = "name_glow_enabled";
    public static final String KEY_NAME_GLOW_COLOR = "name_glow_color";
    public static final String KEY_NAME_GLOW_RADIUS = "name_glow_radius";
    public static final String KEY_NAME_GLOW_STRENGTH = "name_glow_strength";
    public static final String KEY_NAME_SHADOW_ENABLED = "name_shadow_enabled";
    public static final String KEY_NAME_SHADOW_COLOR = "name_shadow_color";
    public static final String KEY_NAME_SHADOW_RADIUS = "name_shadow_radius";
    public static final String KEY_NAME_SHADOW_STRENGTH = "name_shadow_strength";
    public static final String KEY_NAME_SHADOW_DX = "name_shadow_dx";
    public static final String KEY_NAME_SHADOW_DY = "name_shadow_dy";
    public static final String KEY_NAME_FX = "name_fx"; // 0=None, 1=Pulse, 2=Gradient, 3=Shimmer, 4=Rainbow, 5=Neon, 6=Fire, 7=Ice
    public static final String KEY_NAME_FX_SPEED = "name_fx_speed";
    public static final String KEY_NAME_GRAD_C1 = "name_grad_c1";
    public static final String KEY_NAME_GRAD_C2 = "name_grad_c2";
    public static final String KEY_NAME_GRAD_ANGLE = "name_grad_angle";
    public static final String KEY_NAME_SIZE = "name_size";
    public static final String KEY_NAME_FONT = "name_font"; // 0=Standard, 1=Thin, 2=Serif, 3=Mono, 4=Italic, 5=Narrow

    // 3. Avatar Shapes & Glowing Story Rings (Exact Custom Profile keys)
    public static final String KEY_AVATAR_SHAPE = "avatar_shape"; // 0=Circle, 1=Rounded, 2=Square, 3=Hexagon, 4=Pentagon, 5=Star, 6=Heart, 7=Flower
    public static final String KEY_AVATAR_RADIUS = "avatar_radius";
    public static final String KEY_AVATAR_ROUND = "avatar_round";
    public static final String KEY_AVATAR_RING_ENABLED = "avatar_ring_enabled";
    public static final String KEY_AVATAR_RING_COLOR = "avatar_ring_color";
    public static final String KEY_AVATAR_RING_WIDTH = "avatar_ring_width";
    public static final String KEY_AVATAR_RING_PULSE = "avatar_ring_pulse";
    public static final String KEY_AVATAR_ALPHA = "avatar_alpha";
    public static final String KEY_AVATAR_DIM = "avatar_dim";
    public static final String KEY_AVATAR_FADE = "avatar_fade";
    public static final String KEY_AVATAR_FADE_RADIUS = "avatar_fade_radius";

    // 4. UI & Dialogs (Glassmorphic blur, cards, badges, haptics)
    public static final String KEY_UI_GLASS_BLUR = "ui_glass_blur";
    public static final String KEY_UI_DIALOG_CARDS = "ui_dialog_cards";
    public static final String KEY_UI_BADGE_CUSTOM = "ui_badge_custom";
    public static final String KEY_UI_BADGE_COLOR = "ui_badge_color";
    public static final String KEY_UI_BADGE_TEXT_COLOR = "ui_badge_text_color";
    public static final String KEY_UI_BADGE_GLOW = "ui_badge_glow";
    public static final String KEY_UI_HAPTIC = "ui_haptic";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- 1. MESSAGE BUBBLES ---
    public static boolean isBubbleColorEnabled() {
        return getPrefs().getBoolean(KEY_BUBBLE_COLOR_ENABLED, false);
    }
    public static void setBubbleColorEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BUBBLE_COLOR_ENABLED, enabled).apply();
    }
    public static boolean isBubbleGradientEnabled() {
        return getPrefs().getBoolean(KEY_BUBBLE_GRADIENT, false);
    }
    public static void setBubbleGradientEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BUBBLE_GRADIENT, enabled).apply();
    }
    public static int getBubbleColor() {
        return getPrefs().getInt(KEY_BUBBLE_COLOR, 0xFF2A87FF);
    }
    public static void setBubbleColor(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_COLOR, color).apply();
    }
    public static int getBubbleColor2() {
        return getPrefs().getInt(KEY_BUBBLE_COLOR2, 0xFF00C6FF);
    }
    public static void setBubbleColor2(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_COLOR2, color).apply();
    }
    public static int getBubbleGradAngle() {
        return getPrefs().getInt(KEY_BUBBLE_GRAD_ANGLE, 0);
    }
    public static void setBubbleGradAngle(int angle) {
        getPrefs().edit().putInt(KEY_BUBBLE_GRAD_ANGLE, angle).apply();
    }
    public static int getBubbleTextColor() {
        return getPrefs().getInt(KEY_BUBBLE_TEXT_COLOR, 0xFFFFFFFF);
    }
    public static void setBubbleTextColor(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_TEXT_COLOR, color).apply();
    }
    public static int getBubbleRadius() {
        return getPrefs().getInt(KEY_BUBBLE_RADIUS, SharedConfig.bubbleRadius > 0 ? SharedConfig.bubbleRadius : 17);
    }
    public static void setBubbleRadius(int radius) {
        getPrefs().edit().putInt(KEY_BUBBLE_RADIUS, radius).apply();
        SharedConfig.setBubbleRadius(radius);
    }
    public static boolean isBubbleGlowEnabled() {
        return getPrefs().getBoolean(KEY_BUBBLE_GLOW_ENABLED, false);
    }
    public static void setBubbleGlowEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BUBBLE_GLOW_ENABLED, enabled).apply();
    }
    public static int getBubbleGlowColor() {
        return getPrefs().getInt(KEY_BUBBLE_GLOW_COLOR, 0x662A87FF);
    }
    public static void setBubbleGlowColor(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_GLOW_COLOR, color).apply();
    }
    public static int getBubbleGlowRadius() {
        return getPrefs().getInt(KEY_BUBBLE_GLOW_RADIUS, 12);
    }
    public static void setBubbleGlowRadius(int radius) {
        getPrefs().edit().putInt(KEY_BUBBLE_GLOW_RADIUS, radius).apply();
    }

    // --- 2. NAME FX & TYPOGRAPHY ---
    public static boolean isNameColorEnabled() {
        return getPrefs().getBoolean(KEY_NAME_COLOR_ENABLED, false);
    }
    public static void setNameColorEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_NAME_COLOR_ENABLED, enabled).apply();
    }
    public static int getNameColor() {
        return getPrefs().getInt(KEY_NAME_COLOR, 0xFF3390EC);
    }
    public static void setNameColor(int color) {
        getPrefs().edit().putInt(KEY_NAME_COLOR, color).apply();
    }
    public static boolean isNameGlowEnabled() {
        return getPrefs().getBoolean(KEY_NAME_GLOW_ENABLED, false);
    }
    public static void setNameGlowEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_NAME_GLOW_ENABLED, enabled).apply();
    }
    public static int getNameGlowColor() {
        return getPrefs().getInt(KEY_NAME_GLOW_COLOR, 0xFF3390EC);
    }
    public static void setNameGlowColor(int color) {
        getPrefs().edit().putInt(KEY_NAME_GLOW_COLOR, color).apply();
    }
    public static int getNameGlowRadius() {
        return getPrefs().getInt(KEY_NAME_GLOW_RADIUS, 12);
    }
    public static void setNameGlowRadius(int radius) {
        getPrefs().edit().putInt(KEY_NAME_GLOW_RADIUS, radius).apply();
    }
    public static int getNameGlowStrength() {
        return getPrefs().getInt(KEY_NAME_GLOW_STRENGTH, 100);
    }
    public static void setNameGlowStrength(int strength) {
        getPrefs().edit().putInt(KEY_NAME_GLOW_STRENGTH, strength).apply();
    }
    public static boolean isNameShadowEnabled() {
        return getPrefs().getBoolean(KEY_NAME_SHADOW_ENABLED, false);
    }
    public static void setNameShadowEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_NAME_SHADOW_ENABLED, enabled).apply();
    }
    public static int getNameShadowColor() {
        return getPrefs().getInt(KEY_NAME_SHADOW_COLOR, 0x80000000);
    }
    public static void setNameShadowColor(int color) {
        getPrefs().edit().putInt(KEY_NAME_SHADOW_COLOR, color).apply();
    }
    public static int getNameShadowRadius() {
        return getPrefs().getInt(KEY_NAME_SHADOW_RADIUS, 6);
    }
    public static void setNameShadowRadius(int radius) {
        getPrefs().edit().putInt(KEY_NAME_SHADOW_RADIUS, radius).apply();
    }
    public static int getNameShadowStrength() {
        return getPrefs().getInt(KEY_NAME_SHADOW_STRENGTH, 70);
    }
    public static void setNameShadowStrength(int strength) {
        getPrefs().edit().putInt(KEY_NAME_SHADOW_STRENGTH, strength).apply();
    }
    public static int getNameShadowDx() {
        return getPrefs().getInt(KEY_NAME_SHADOW_DX, 2);
    }
    public static void setNameShadowDx(int dx) {
        getPrefs().edit().putInt(KEY_NAME_SHADOW_DX, dx).apply();
    }
    public static int getNameShadowDy() {
        return getPrefs().getInt(KEY_NAME_SHADOW_DY, 2);
    }
    public static void setNameShadowDy(int dy) {
        getPrefs().edit().putInt(KEY_NAME_SHADOW_DY, dy).apply();
    }
    public static int getNameFx() {
        return getPrefs().getInt(KEY_NAME_FX, 0);
    }
    public static void setNameFx(int fx) {
        getPrefs().edit().putInt(KEY_NAME_FX, fx).apply();
    }
    public static int getNameFxSpeed() {
        return getPrefs().getInt(KEY_NAME_FX_SPEED, 100);
    }
    public static void setNameFxSpeed(int speed) {
        getPrefs().edit().putInt(KEY_NAME_FX_SPEED, speed).apply();
    }
    public static int getNameGradC1() {
        return getPrefs().getInt(KEY_NAME_GRAD_C1, 0xFFFF0077);
    }
    public static void setNameGradC1(int color) {
        getPrefs().edit().putInt(KEY_NAME_GRAD_C1, color).apply();
    }
    public static int getNameGradC2() {
        return getPrefs().getInt(KEY_NAME_GRAD_C2, 0xFF7700FF);
    }
    public static void setNameGradC2(int color) {
        getPrefs().edit().putInt(KEY_NAME_GRAD_C2, color).apply();
    }
    public static int getNameGradAngle() {
        return getPrefs().getInt(KEY_NAME_GRAD_ANGLE, 0);
    }
    public static void setNameGradAngle(int angle) {
        getPrefs().edit().putInt(KEY_NAME_GRAD_ANGLE, angle).apply();
    }
    public static int getNameSize() {
        return getPrefs().getInt(KEY_NAME_SIZE, 100);
    }
    public static void setNameSize(int size) {
        getPrefs().edit().putInt(KEY_NAME_SIZE, size).apply();
    }
    public static int getNameFont() {
        return getPrefs().getInt(KEY_NAME_FONT, 0);
    }
    public static void setNameFont(int font) {
        getPrefs().edit().putInt(KEY_NAME_FONT, font).apply();
    }

    // --- 3. AVATAR SHAPES & STORY RINGS ---
    public static int getAvatarShape() {
        return getPrefs().getInt(KEY_AVATAR_SHAPE, 0);
    }
    public static void setAvatarShape(int shape) {
        getPrefs().edit().putInt(KEY_AVATAR_SHAPE, shape).apply();
    }
    public static int getAvatarRadius() {
        return getPrefs().getInt(KEY_AVATAR_RADIUS, 18);
    }
    public static void setAvatarRadius(int radius) {
        getPrefs().edit().putInt(KEY_AVATAR_RADIUS, radius).apply();
    }
    public static int getAvatarRound() {
        return getPrefs().getInt(KEY_AVATAR_ROUND, 0);
    }
    public static void setAvatarRound(int round) {
        getPrefs().edit().putInt(KEY_AVATAR_ROUND, round).apply();
    }
    public static boolean isAvatarRingEnabled() {
        return getPrefs().getBoolean(KEY_AVATAR_RING_ENABLED, false);
    }
    public static void setAvatarRingEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_AVATAR_RING_ENABLED, enabled).apply();
    }
    public static int getAvatarRingColor() {
        return getPrefs().getInt(KEY_AVATAR_RING_COLOR, 0xFF00FFCC);
    }
    public static void setAvatarRingColor(int color) {
        getPrefs().edit().putInt(KEY_AVATAR_RING_COLOR, color).apply();
    }
    public static int getAvatarRingWidth() {
        return getPrefs().getInt(KEY_AVATAR_RING_WIDTH, 2);
    }
    public static void setAvatarRingWidth(int width) {
        getPrefs().edit().putInt(KEY_AVATAR_RING_WIDTH, width).apply();
    }
    public static boolean isAvatarRingPulse() {
        return getPrefs().getBoolean(KEY_AVATAR_RING_PULSE, true);
    }
    public static void setAvatarRingPulse(boolean pulse) {
        getPrefs().edit().putBoolean(KEY_AVATAR_RING_PULSE, pulse).apply();
    }
    public static int getAvatarAlpha() {
        return getPrefs().getInt(KEY_AVATAR_ALPHA, 100);
    }
    public static void setAvatarAlpha(int alpha) {
        getPrefs().edit().putInt(KEY_AVATAR_ALPHA, alpha).apply();
    }
    public static int getAvatarDim() {
        return getPrefs().getInt(KEY_AVATAR_DIM, 0);
    }
    public static void setAvatarDim(int dim) {
        getPrefs().edit().putInt(KEY_AVATAR_DIM, dim).apply();
    }
    public static int getAvatarFade() {
        return getPrefs().getInt(KEY_AVATAR_FADE, 0);
    }
    public static void setAvatarFade(int fade) {
        getPrefs().edit().putInt(KEY_AVATAR_FADE, fade).apply();
    }
    public static int getAvatarFadeRadius() {
        return getPrefs().getInt(KEY_AVATAR_FADE_RADIUS, 50);
    }
    public static void setAvatarFadeRadius(int radius) {
        getPrefs().edit().putInt(KEY_AVATAR_FADE_RADIUS, radius).apply();
    }

    // --- 4. UI & DIALOGS ---
    public static boolean isGlassBlurEnabled() {
        return getPrefs().getBoolean(KEY_UI_GLASS_BLUR, false);
    }
    public static void setGlassBlurEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_UI_GLASS_BLUR, enabled).apply();
    }
    public static boolean isDialogCardsEnabled() {
        return getPrefs().getBoolean(KEY_UI_DIALOG_CARDS, false);
    }
    public static void setDialogCardsEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_UI_DIALOG_CARDS, enabled).apply();
    }
    public static boolean isBadgeCustomEnabled() {
        return getPrefs().getBoolean(KEY_UI_BADGE_CUSTOM, false);
    }
    public static void setBadgeCustomEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_UI_BADGE_CUSTOM, enabled).apply();
    }
    public static int getBadgeColor() {
        return getPrefs().getInt(KEY_UI_BADGE_COLOR, 0xFF3390EC);
    }
    public static void setBadgeColor(int color) {
        getPrefs().edit().putInt(KEY_UI_BADGE_COLOR, color).apply();
    }
    public static int getBadgeTextColor() {
        return getPrefs().getInt(KEY_UI_BADGE_TEXT_COLOR, 0xFFFFFFFF);
    }
    public static void setBadgeTextColor(int color) {
        getPrefs().edit().putInt(KEY_UI_BADGE_TEXT_COLOR, color).apply();
    }
    public static boolean isBadgeGlowEnabled() {
        return getPrefs().getBoolean(KEY_UI_BADGE_GLOW, false);
    }
    public static void setBadgeGlowEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_UI_BADGE_GLOW, enabled).apply();
    }
    public static boolean isHapticEnabled() {
        return getPrefs().getBoolean(KEY_UI_HAPTIC, true);
    }
    public static void setHapticEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_UI_HAPTIC, enabled).apply();
    }
}
