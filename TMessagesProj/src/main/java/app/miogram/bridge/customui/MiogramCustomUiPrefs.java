package app.miogram.bridge.customui;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;

/**
 * Preferences manager for Miogram Custom UI.
 * Coordinates visual styles across chat bubbles, headers, dialog cells, avatars, and animations.
 */
public class MiogramCustomUiPrefs {

    private static final String PREFS_NAME = "miogram_custom_ui_prefs";

    // Chat Bubbles
    public static final String KEY_BUBBLE_GRADIENT = "ui_bubble_gradient";
    public static final String KEY_BUBBLE_COLOR1 = "ui_bubble_c1";
    public static final String KEY_BUBBLE_COLOR2 = "ui_bubble_c2";
    public static final String KEY_BUBBLE_ANGLE = "ui_bubble_angle";
    public static final String KEY_BUBBLE_RADIUS = "ui_bubble_radius";
    public static final String KEY_BUBBLE_TEXT_COLOR = "ui_bubble_text_color";

    // Dialogs & Avatars
    public static final String KEY_AVATAR_SHAPE = "ui_avatar_shape"; // 0=circle, 1=squircle, 2=rounded_rect, 3=hexagon
    public static final String KEY_AVATAR_RING = "ui_avatar_ring_enabled";
    public static final String KEY_AVATAR_RING_COLOR = "ui_avatar_ring_color";
    public static final String KEY_DIALOG_STYLE = "ui_dialog_style"; // 0=classic, 1=card, 2=glass
    public static final String KEY_UNREAD_STYLE = "ui_unread_style"; // 0=classic, 1=pill, 2=glow

    // Header & Menus
    public static final String KEY_HEADER_STYLE = "ui_header_style"; // 0=default, 1=glass, 2=gradient
    public static final String KEY_ONLINE_PULSE = "ui_online_pulse";
    public static final String KEY_DRAWER_BANNER = "ui_drawer_banner";

    // Performance & Haptics
    public static final String KEY_HAPTIC_LEVEL = "ui_haptic_level"; // 0=default, 1=crisp, 2=soft, 3=off
    public static final String KEY_PROMOTION_LOCK = "ui_promotion_lock";

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

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

    public static void applyPreset(int presetId) {
        switch (presetId) {
            case 0: // Cyberpunk Neon
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFFFF007F);
                setBubbleColor2(0xFF00F0FF);
                setBubbleAngle(135);
                setBubbleRadius(18);
                setBubbleTextColor(0xFFFFFFFF);
                setAvatarShape(1); // Squircle
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF00F0FF);
                setUnreadStyle(2); // Glow
                setHeaderStyle(1); // Glass
                break;
            case 1: // Liquid Glass iOS
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFF3A88E9);
                setBubbleColor2(0xFF7052FF);
                setBubbleAngle(45);
                setBubbleRadius(22);
                setBubbleTextColor(0xFFFFFFFF);
                setAvatarShape(0); // Circle
                setAvatarRingEnabled(false);
                setUnreadStyle(1); // Pill
                setHeaderStyle(1); // Glass
                break;
            case 2: // Pure AMOLED
                setBubbleGradientEnabled(false);
                setBubbleColor1(0xFF18181A);
                setBubbleRadius(14);
                setBubbleTextColor(0xFFEEEEEE);
                setAvatarShape(3); // Hexagon
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF444444);
                setUnreadStyle(0);
                setHeaderStyle(0);
                break;
            case 3: // Sunset Coral
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFFFF5E3A);
                setBubbleColor2(0xFFFF2A68);
                setBubbleAngle(90);
                setBubbleRadius(20);
                setBubbleTextColor(0xFFFFFFFF);
                setAvatarShape(2); // Rounded Rect
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFFFF5E3A);
                setUnreadStyle(1); // Pill
                setHeaderStyle(2); // Gradient
                break;
            case 4: // Emerald Frost
                setBubbleGradientEnabled(true);
                setBubbleColor1(0xFF00B09B);
                setBubbleColor2(0xFF96C93D);
                setBubbleAngle(60);
                setBubbleRadius(16);
                setBubbleTextColor(0xFFFFFFFF);
                setAvatarShape(1); // Squircle
                setAvatarRingEnabled(true);
                setAvatarRingColor(0xFF00B09B);
                setUnreadStyle(2); // Glow
                setHeaderStyle(1);
                break;
        }
    }

    public static void resetDefaults() {
        getPrefs().edit().clear().apply();
        setBubbleRadius(17);
    }
}
