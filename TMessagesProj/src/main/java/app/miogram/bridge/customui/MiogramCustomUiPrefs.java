package app.miogram.bridge.customui;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

/**
 * Preferences manager for Miogram Custom UI Studio.
 * Full state store for Bubbles, Name FX shaders, Avatar shapes & rings, Dialog styles, and Performance.
 */
public class MiogramCustomUiPrefs {

    private static final String PREFS_NAME = "miogram_custom_ui_prefs";

    // 1. Chat Bubbles
    public static final String KEY_BUBBLE_GRADIENT = "ui_bubble_gradient";
    public static final String KEY_BUBBLE_COLOR1 = "ui_bubble_c1";
    public static final String KEY_BUBBLE_COLOR2 = "ui_bubble_c2";
    public static final String KEY_BUBBLE_ANGLE = "ui_bubble_angle";
    public static final String KEY_BUBBLE_RADIUS = "ui_bubble_radius";
    public static final String KEY_BUBBLE_TEXT_COLOR = "ui_bubble_text_color";

    // 2. Name & Text FX Shaders
    public static final String KEY_NAME_FX = "ui_name_fx"; // 0=none, 2=grad, 3=glare, 4=rainbow, 6=fire, 7=ice, 8=neon
    public static final String KEY_NAME_GRAD_C1 = "ui_name_grad_c1";
    public static final String KEY_NAME_GRAD_C2 = "ui_name_grad_c2";
    public static final String KEY_NAME_GLOW_ON = "ui_name_glow_on";
    public static final String KEY_NAME_GLOW_COLOR = "ui_name_glow_color";
    public static final String KEY_NAME_GLOW_RADIUS = "ui_name_glow_radius";
    public static final String KEY_NAME_FONT = "ui_name_font"; // 0=default, 1=bold, 2=mono, 3=serif, 4=casual

    // 3. Dialogs & Avatars
    public static final String KEY_AVATAR_SHAPE = "ui_avatar_shape"; // 0=circle, 1=squircle, 2=rounded_rect, 3=hexagon, 4=star, 5=diamond
    public static final String KEY_AVATAR_RING = "ui_avatar_ring_enabled";
    public static final String KEY_AVATAR_RING_COLOR = "ui_avatar_ring_color";
    public static final String KEY_AVATAR_RING_WIDTH = "ui_avatar_ring_width";
    public static final String KEY_AVATAR_RING_PULSE = "ui_avatar_ring_pulse";
    public static final String KEY_DIALOG_STYLE = "ui_dialog_style"; // 0=classic, 1=card, 2=glass
    public static final String KEY_UNREAD_STYLE = "ui_unread_style"; // 0=classic, 1=pill, 2=glow, 3=dot

    // 4. Header & Interface
    public static final String KEY_HEADER_STYLE = "ui_header_style"; // 0=default, 1=glass, 2=gradient
    public static final String KEY_ONLINE_PULSE = "ui_online_pulse";
    public static final String KEY_DRAWER_BANNER = "ui_drawer_banner";

    // 5. Performance & Haptics
    public static final String KEY_HAPTIC_LEVEL = "ui_haptic_level"; // 0=default, 1=crisp, 2=soft, 3=off
    public static final String KEY_PROMOTION_LOCK = "ui_promotion_lock";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- BUBBLE GETTERS & SETTERS ---
    public static boolean isBubbleGradientEnabled() {
        return getPrefs().getBoolean(KEY_BUBBLE_GRADIENT, false);
    }
    public static void setBubbleGradientEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BUBBLE_GRADIENT, enabled).apply();
    }
    public static int getBubbleColor1() {
        return getPrefs().getInt(KEY_BUBBLE_COLOR1, 0xFF7052FF);
    }
    public static void setBubbleColor1(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_COLOR1, color).apply();
    }
    public static int getBubbleColor2() {
        return getPrefs().getInt(KEY_BUBBLE_COLOR2, 0xFF00D2FF);
    }
    public static void setBubbleColor2(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_COLOR2, color).apply();
    }
    public static int getBubbleAngle() {
        return getPrefs().getInt(KEY_BUBBLE_ANGLE, 45);
    }
    public static void setBubbleAngle(int angle) {
        getPrefs().edit().putInt(KEY_BUBBLE_ANGLE, angle).apply();
    }
    public static int getBubbleRadius() {
        return getPrefs().getInt(KEY_BUBBLE_RADIUS, SharedConfig.bubbleRadius > 0 ? SharedConfig.bubbleRadius : 17);
    }
    public static void setBubbleRadius(int radius) {
        getPrefs().edit().putInt(KEY_BUBBLE_RADIUS, radius).apply();
        SharedConfig.setBubbleRadius(radius);
    }
    public static int getBubbleTextColor() {
        return getPrefs().getInt(KEY_BUBBLE_TEXT_COLOR, 0xFFFFFFFF);
    }
    public static void setBubbleTextColor(int color) {
        getPrefs().edit().putInt(KEY_BUBBLE_TEXT_COLOR, color).apply();
    }

    // --- NAME FX GETTERS & SETTERS ---
    public static int getNameFx() {
        return getPrefs().getInt(KEY_NAME_FX, 0);
    }
    public static void setNameFx(int fx) {
        getPrefs().edit().putInt(KEY_NAME_FX, fx).apply();
    }
    public static int getNameGradC1() {
        return getPrefs().getInt(KEY_NAME_GRAD_C1, 0xFFFF007F);
    }
    public static void setNameGradC1(int c) {
        getPrefs().edit().putInt(KEY_NAME_GRAD_C1, c).apply();
    }
    public static int getNameGradC2() {
        return getPrefs().getInt(KEY_NAME_GRAD_C2, 0xFF00F0FF);
    }
    public static void setNameGradC2(int c) {
        getPrefs().edit().putInt(KEY_NAME_GRAD_C2, c).apply();
    }
    public static boolean isNameGlowEnabled() {
        return getPrefs().getBoolean(KEY_NAME_GLOW_ON, false);
    }
    public static void setNameGlowEnabled(boolean on) {
        getPrefs().edit().putBoolean(KEY_NAME_GLOW_ON, on).apply();
    }
    public static int getNameGlowColor() {
        return getPrefs().getInt(KEY_NAME_GLOW_COLOR, 0xFFFF007F);
    }
    public static void setNameGlowColor(int c) {
        getPrefs().edit().putInt(KEY_NAME_GLOW_COLOR, c).apply();
    }
    public static int getNameGlowRadius() {
        return getPrefs().getInt(KEY_NAME_GLOW_RADIUS, 8);
    }
    public static void setNameGlowRadius(int r) {
        getPrefs().edit().putInt(KEY_NAME_GLOW_RADIUS, r).apply();
    }
    public static int getNameFont() {
        return getPrefs().getInt(KEY_NAME_FONT, 0);
    }
    public static void setNameFont(int font) {
        getPrefs().edit().putInt(KEY_NAME_FONT, font).apply();
    }

    // --- AVATARS & RINGS GETTERS & SETTERS ---
    public static int getAvatarShape() {
        return getPrefs().getInt(KEY_AVATAR_SHAPE, 0);
    }
    public static void setAvatarShape(int shape) {
        getPrefs().edit().putInt(KEY_AVATAR_SHAPE, shape).apply();
    }
    public static boolean isAvatarRingEnabled() {
        return getPrefs().getBoolean(KEY_AVATAR_RING, false);
    }
    public static void setAvatarRingEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_AVATAR_RING, enabled).apply();
    }
    public static int getAvatarRingColor() {
        return getPrefs().getInt(KEY_AVATAR_RING_COLOR, 0xFF7052FF);
    }
    public static void setAvatarRingColor(int color) {
        getPrefs().edit().putInt(KEY_AVATAR_RING_COLOR, color).apply();
    }
    public static float getAvatarRingWidth() {
        return getPrefs().getFloat(KEY_AVATAR_RING_WIDTH, 2.5f);
    }
    public static void setAvatarRingWidth(float w) {
        getPrefs().edit().putFloat(KEY_AVATAR_RING_WIDTH, w).apply();
    }
    public static boolean isAvatarRingPulse() {
        return getPrefs().getBoolean(KEY_AVATAR_RING_PULSE, true);
    }
    public static void setAvatarRingPulse(boolean pulse) {
        getPrefs().edit().putBoolean(KEY_AVATAR_RING_PULSE, pulse).apply();
    }

    // --- DIALOGS & CHATS GETTERS & SETTERS ---
    public static int getDialogStyle() {
        return getPrefs().getInt(KEY_DIALOG_STYLE, 0);
    }
    public static void setDialogStyle(int style) {
        getPrefs().edit().putInt(KEY_DIALOG_STYLE, style).apply();
    }
    public static int getUnreadStyle() {
        return getPrefs().getInt(KEY_UNREAD_STYLE, 0);
    }
    public static void setUnreadStyle(int style) {
        getPrefs().edit().putInt(KEY_UNREAD_STYLE, style).apply();
    }

    // --- HEADER & INTERFACE ---
    public static int getHeaderStyle() {
        return getPrefs().getInt(KEY_HEADER_STYLE, 0);
    }
    public static void setHeaderStyle(int style) {
        getPrefs().edit().putInt(KEY_HEADER_STYLE, style).apply();
    }
    public static boolean isOnlinePulseEnabled() {
        return getPrefs().getBoolean(KEY_ONLINE_PULSE, true);
    }
    public static void setOnlinePulseEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_ONLINE_PULSE, enabled).apply();
    }
    public static int getHapticLevel() {
        return getPrefs().getInt(KEY_HAPTIC_LEVEL, 1);
    }
    public static void setHapticLevel(int level) {
        getPrefs().edit().putInt(KEY_HAPTIC_LEVEL, level).apply();
    }
    public static boolean isProMotionLock() {
        return getPrefs().getBoolean(KEY_PROMOTION_LOCK, true);
    }
    public static void setProMotionLock(boolean lock) {
        getPrefs().edit().putBoolean(KEY_PROMOTION_LOCK, lock).apply();
    }

    /* =========================================================================
     * MASTERPIECE THEMES WORKSHOP (CURATED 1-TAP PRESETS)
     * ========================================================================= */
    public static void applyPreset(int presetId) {
        switch (presetId) {
            case 0: // 🚀 Cyberpunk 2077
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFFFF007F);
                setBubbleColor2(0xFF00F0FF);
                setBubbleAngle(135);
                setBubbleRadius(18);
                setBubbleTextColor(0xFFFFFFFF);
                setNameFx(MiogramUiEngine.FX_FIRE); // Fiery Flame Name Shader
                setNameGlowEnabled(true);
                setNameGlowColor(0xFFFF007F);
                setNameGlowRadius(10);
                setNameFont(2); // Cyber Mono
                setAvatarShape(1); // Squircle
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF00F0FF);
                setAvatarRingPulse(true);
                setUnreadStyle(2); // Neon Glow
                setHeaderStyle(1); // Glass
                break;

            case 1: // 💎 Liquid Glass iOS 18
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFF3A88E9);
                setBubbleColor2(0xFF7052FF);
                setBubbleAngle(45);
                setBubbleRadius(22);
                setBubbleTextColor(0xFFFFFFFF);
                setNameFx(MiogramUiEngine.FX_GLARE); // Shimmer Glare
                setNameGlowEnabled(false);
                setNameFont(1); // Rounded Modern
                setAvatarShape(0); // Circle
                setAvatarRingEnabled(false);
                setUnreadStyle(1); // Pill
                setHeaderStyle(1); // Glass
                break;

            case 2: // 🌌 Pure AMOLED Space
                setBubbleGradientEnabled(false);
                setBubbleColor1(0xFF161618);
                setBubbleRadius(14);
                setBubbleTextColor(0xFFEEEEEE);
                setNameFx(MiogramUiEngine.FX_ICE); // Frost Ice Crystals
                setNameGlowEnabled(true);
                setNameGlowColor(0xFF00C7FF);
                setNameGlowRadius(8);
                setNameFont(0);
                setAvatarShape(3); // Hexagon
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF555555);
                setAvatarRingPulse(false);
                setUnreadStyle(3); // Dot
                setHeaderStyle(0);
                break;

            case 3: // 🌅 Sunset Miami Beach
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFFFF5E3A);
                setBubbleColor2(0xFFFF2A68);
                setBubbleAngle(90);
                setBubbleRadius(20);
                setBubbleTextColor(0xFFFFFFFF);
                setNameFx(MiogramUiEngine.FX_RAINBOW); // Animated Rainbow
                setNameGlowEnabled(true);
                setNameGlowColor(0xFFFF5E3A);
                setNameGlowRadius(8);
                setNameFont(4); // Casual
                setAvatarShape(4); // Star
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFFFF2A68);
                setAvatarRingPulse(true);
                setUnreadStyle(1); // Pill
                setHeaderStyle(2); // Gradient
                break;

            case 4: // 🌿 Emerald Cyber Frost
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFF00B09B);
                setBubbleColor2(0xFF96C93D);
                setBubbleAngle(60);
                setBubbleRadius(16);
                setBubbleTextColor(0xFFFFFFFF);
                setNameFx(MiogramUiEngine.FX_CYBER_NEON);
                setNameGlowEnabled(true);
                setNameGlowColor(0xFF00B09B);
                setNameGlowRadius(8);
                setNameFont(1);
                setAvatarShape(1); // Squircle
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF00B09B);
                setAvatarRingPulse(true);
                setUnreadStyle(2); // Neon Glow
                setHeaderStyle(1);
                break;

            case 5: // 💜 Electric Royalty
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFF8A2387);
                setBubbleColor2(0xFFE94057);
                setBubbleAngle(120);
                setBubbleRadius(20);
                setBubbleTextColor(0xFFFFFFFF);
                setNameFx(MiogramUiEngine.FX_GRADIENT);
                setNameGradC1(0xFFFFD700);
                setNameGradC2(0xFF8A2387);
                setNameGlowEnabled(true);
                setNameGlowColor(0xFFFFD700);
                setNameGlowRadius(10);
                setNameFont(3); // Serif
                setAvatarShape(5); // Diamond
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFFFFD700);
                setAvatarRingPulse(true);
                setUnreadStyle(2);
                setHeaderStyle(2);
                break;
        }
    }

    public static void resetDefaults() {
        getPrefs().edit().clear().apply();
        setBubbleRadius(17);
    }
}
