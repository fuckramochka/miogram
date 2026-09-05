package app.miogram.bridge.customui;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.util.Locale;

/**
 * Preferences manager for Miogram Custom UI Studio.
 * Crafted 1-to-1 in the exact style, naming, and architecture of Custom Profile.
 * Stores values in "cpb_native_settings" using the exact userId_tag convention of Cfg.java,
 * while maintaining backward compatibility with miogram_custom_ui_prefs.
 */
public class MiogramCustomUiPrefs {

    public static final String PREFS_CPB = "cpb_native_settings";
    public static final String PREFS_LOCAL = "miogram_custom_ui_prefs";

    // 1. Message Bubbles
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

    // 2. Name & Text FX
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
    public static final String KEY_NAME_FX = "name_fx";
    public static final String KEY_NAME_FX_SPEED = "name_fx_speed";
    public static final String KEY_NAME_GRAD_C1 = "name_grad_c1";
    public static final String KEY_NAME_GRAD_C2 = "name_grad_c2";
    public static final String KEY_NAME_GRAD_ANGLE = "name_grad_angle";
    public static final String KEY_NAME_SIZE = "name_size";
    public static final String KEY_NAME_FONT = "name_font";

    // 3. Avatar Shapes & Glowing Story Rings
    public static final String KEY_AVATAR_SHAPE = "avatar_shape";
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

    // 4. Profile Text Colors & Palette
    public static final String KEY_PROFILE_TEXT_COLOR_ENABLED = "profile_text_color_enabled";
    public static final String KEY_PROFILE_TEXT_COLOR = "profile_text_color";
    public static final String KEY_PROFILE_PALETTE = "profile_palette";
    public static final String KEY_PROVIDER_ENGINE = "provider_engine";

    // 5. Banner (Header)
    public static final String KEY_BANNER_ENABLED = "enabled";
    public static final String KEY_BANNER_MODE = "banner_mode";
    public static final String KEY_BANNER_ALPHA = "banner_alpha";
    public static final String KEY_BANNER_DIM = "banner_dim";
    public static final String KEY_BANNER_COLOR = "banner_color";
    public static final String KEY_BANNER_BLEND = "banner_blend";
    public static final String KEY_BANNER_BLEND_RADIUS = "banner_blend_radius";
    public static final String KEY_SHOW_EMOJI = "show_emoji";

    // 6. Background
    public static final String KEY_BG_ENABLED = "bg_enabled";
    public static final String KEY_BG_COMPAT = "bg_compat";
    public static final String KEY_BG_MODE = "bg_mode";
    public static final String KEY_BG_COLOR = "bg_color";
    public static final String KEY_BG_ALPHA = "bg_alpha";
    public static final String KEY_BG_DIM = "bg_dim";

    // 7. Blocks
    public static final String KEY_BLOCKS_COLOR_ENABLED = "blocks_color_enabled";
    public static final String KEY_BLOCKS_COLOR = "blocks_color";
    public static final String KEY_BLOCKS_ALPHA = "blocks_alpha";
    public static final String KEY_BLOCKS_JOIN = "blocks_join";
    public static final String KEY_BLOCKS_RADIUS_ENABLED = "blocks_radius_enabled";
    public static final String KEY_BLOCKS_RADIUS = "blocks_radius";
    public static final String KEY_BLOCKS_BLUR = "blocks_blur";
    public static final String KEY_BLOCKS_DEPTH = "blocks_depth";

    // 8. Thought
    public static final String KEY_THOUGHT_TEXT = "thought_text";
    public static final String KEY_THOUGHT_TEXT_COLOR = "thought_text_color";
    public static final String KEY_THOUGHT_BG_COLOR = "thought_bg_color";
    public static final String KEY_THOUGHT_SHADOW_ENABLED = "thought_shadow_enabled";
    public static final String KEY_THOUGHT_SHADOW_COLOR = "thought_shadow_color";
    public static final String KEY_THOUGHT_SHADOW_RADIUS = "thought_shadow_radius";
    public static final String KEY_THOUGHT_SHADOW_STRENGTH = "thought_shadow_strength";
    public static final String KEY_THOUGHT_SHADOW_DX = "thought_shadow_dx";
    public static final String KEY_THOUGHT_SHADOW_DY = "thought_shadow_dy";
    public static final String KEY_THOUGHT_FONT_COPY = "thought_font_copy";
    public static final String KEY_THOUGHT_FONT = "thought_font";

    // 9. Visibility
    public static final String KEY_HIDE_ROW_PHONE = "hide_row_phone";
    public static final String KEY_HIDE_ROW_USERNAME = "hide_row_username";
    public static final String KEY_HIDE_ROW_BIO = "hide_row_bio";
    public static final String KEY_HIDE_MEDIA_TABS = "hide_media_tabs";

    // 10. UI & Dialogs
    public static final String KEY_UI_GLASS_BLUR = "ui_glass_blur";
    public static final String KEY_UI_DIALOG_CARDS = "ui_dialog_cards";
    public static final String KEY_UI_BADGE_CUSTOM = "ui_badge_custom";
    public static final String KEY_UI_BADGE_COLOR = "ui_badge_color";
    public static final String KEY_UI_BADGE_TEXT_COLOR = "ui_badge_text_color";
    public static final String KEY_UI_BADGE_GLOW = "ui_badge_glow";
    public static final String KEY_UI_HAPTIC = "ui_haptic";

    private static SharedPreferences getCpbPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return null;
        return ctx.getSharedPreferences(PREFS_CPB, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getLocalPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return null;
        return ctx.getSharedPreferences(PREFS_LOCAL, Context.MODE_PRIVATE);
    }

    private static String keyTag() {
        int slot = UserConfig.selectedAccount;
        long uid = UserConfig.getInstance(slot).getClientUserId();
        if (uid <= 0) {
            return "slot" + slot;
        }
        return Long.toString(uid);
    }

    public static String hex(int color) {
        if ((color & 0xFF000000) == 0xFF000000) {
            return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
        }
        return String.format(Locale.US, "#%08X", color);
    }

    public static int parseColor(String str, int defColor) {
        if (str == null || str.trim().isEmpty()) {
            return defColor;
        }
        String s = str.trim();
        try {
            if (s.startsWith("#")) {
                long val = Long.parseLong(s.substring(1), 16);
                if (s.length() <= 7) {
                    return (int) (0xFF000000L | val);
                }
                return (int) val;
            }
            return Integer.parseInt(s);
        } catch (Throwable ignore) {
            return defColor;
        }
    }

    // --- Core Read/Write helpers ---

    public static boolean getBool(String key, boolean def) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            String val = cpb.getString(fullKey, null);
            if (val == null) {
                val = cpb.getString(key, null);
            }
            if (val != null) {
                val = val.trim();
                if ("1".equals(val) || "true".equalsIgnoreCase(val)) return true;
                if ("0".equals(val) || "false".equalsIgnoreCase(val)) return false;
            }
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null && local.contains(key)) {
            return local.getBoolean(key, def);
        }
        return def;
    }

    public static void setBool(String key, boolean val) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            cpb.edit().putString(fullKey, val ? "1" : "0").apply();
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null) {
            local.edit().putBoolean(key, val).apply();
        }
    }

    public static int getInt(String key, int def) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            String val = cpb.getString(fullKey, null);
            if (val == null) {
                val = cpb.getString(key, null);
            }
            if (val != null) {
                try {
                    return Integer.parseInt(val.trim());
                } catch (Throwable ignore) {}
            }
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null && local.contains(key)) {
            return local.getInt(key, def);
        }
        return def;
    }

    public static void setInt(String key, int val) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            cpb.edit().putString(fullKey, Integer.toString(val)).apply();
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null) {
            local.edit().putInt(key, val).apply();
        }
    }

    public static int getColor(String key, int def) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            String val = cpb.getString(fullKey, null);
            if (val == null) {
                val = cpb.getString(key, null);
            }
            if (val != null) {
                return parseColor(val, def);
            }
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null && local.contains(key)) {
            return local.getInt(key, def);
        }
        return def;
    }

    public static void setColor(String key, int color) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            cpb.edit().putString(fullKey, hex(color)).apply();
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null) {
            local.edit().putInt(key, color).apply();
        }
    }

    public static String getString(String key, String def) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            String val = cpb.getString(fullKey, null);
            if (val == null) {
                val = cpb.getString(key, null);
            }
            if (val != null) return val;
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null && local.contains(key)) {
            return local.getString(key, def);
        }
        return def;
    }

    public static void setString(String key, String val) {
        SharedPreferences cpb = getCpbPrefs();
        if (cpb != null) {
            String fullKey = keyTag() + "_" + key;
            cpb.edit().putString(fullKey, val).apply();
        }
        SharedPreferences local = getLocalPrefs();
        if (local != null) {
            local.edit().putString(key, val).apply();
        }
    }

    // =========================================================================
    // 1. MESSAGE BUBBLES
    // =========================================================================
    public static boolean isBubbleColorEnabled() {
        return getBool(KEY_BUBBLE_COLOR_ENABLED, false);
    }
    public static void setBubbleColorEnabled(boolean enabled) {
        setBool(KEY_BUBBLE_COLOR_ENABLED, enabled);
    }
    public static boolean isBubbleGradientEnabled() {
        return getBool(KEY_BUBBLE_GRADIENT, false);
    }
    public static void setBubbleGradientEnabled(boolean enabled) {
        setBool(KEY_BUBBLE_GRADIENT, enabled);
    }
    public static boolean isBubbleGradient() {
        return isBubbleGradientEnabled();
    }
    public static void setBubbleGradient(boolean enabled) {
        setBubbleGradientEnabled(enabled);
    }
    public static int getBubbleColor() {
        return getColor(KEY_BUBBLE_COLOR, 0xFF2A87FF);
    }
    public static void setBubbleColor(int color) {
        setColor(KEY_BUBBLE_COLOR, color);
    }
    public static int getBubbleColor2() {
        return getColor(KEY_BUBBLE_COLOR2, 0xFF00C6FF);
    }
    public static void setBubbleColor2(int color) {
        setColor(KEY_BUBBLE_COLOR2, color);
    }
    public static int getBubbleGradAngle() {
        return getInt(KEY_BUBBLE_GRAD_ANGLE, 0);
    }
    public static void setBubbleGradAngle(int angle) {
        setInt(KEY_BUBBLE_GRAD_ANGLE, angle);
    }
    public static int getBubbleTextColor() {
        return getColor(KEY_BUBBLE_TEXT_COLOR, 0xFFFFFFFF);
    }
    public static void setBubbleTextColor(int color) {
        setColor(KEY_BUBBLE_TEXT_COLOR, color);
    }
    public static int getBubbleRadius() {
        return getInt(KEY_BUBBLE_RADIUS, 17);
    }
    public static void setBubbleRadius(int radius) {
        setInt(KEY_BUBBLE_RADIUS, radius);
    }
    public static boolean isBubbleGlowEnabled() {
        return getBool(KEY_BUBBLE_GLOW_ENABLED, false);
    }
    public static void setBubbleGlowEnabled(boolean enabled) {
        setBool(KEY_BUBBLE_GLOW_ENABLED, enabled);
    }
    public static int getBubbleGlowColor() {
        return getColor(KEY_BUBBLE_GLOW_COLOR, 0xFF2A87FF);
    }
    public static void setBubbleGlowColor(int color) {
        setColor(KEY_BUBBLE_GLOW_COLOR, color);
    }
    public static int getBubbleGlowRadius() {
        return getInt(KEY_BUBBLE_GLOW_RADIUS, 12);
    }
    public static void setBubbleGlowRadius(int radius) {
        setInt(KEY_BUBBLE_GLOW_RADIUS, radius);
    }

    // =========================================================================
    // 2. NAME & TEXT FX
    // =========================================================================
    public static boolean isNameColorEnabled() {
        return getBool(KEY_NAME_COLOR_ENABLED, false);
    }
    public static void setNameColorEnabled(boolean enabled) {
        setBool(KEY_NAME_COLOR_ENABLED, enabled);
    }
    public static int getNameColor() {
        return getColor(KEY_NAME_COLOR, 0xFFFFFFFF);
    }
    public static void setNameColor(int color) {
        setColor(KEY_NAME_COLOR, color);
    }
    public static boolean isNameGlowEnabled() {
        return getBool(KEY_NAME_GLOW_ENABLED, false);
    }
    public static void setNameGlowEnabled(boolean enabled) {
        setBool(KEY_NAME_GLOW_ENABLED, enabled);
    }
    public static int getNameGlowColor() {
        return getColor(KEY_NAME_GLOW_COLOR, 0xFF2A87FF);
    }
    public static void setNameGlowColor(int color) {
        setColor(KEY_NAME_GLOW_COLOR, color);
    }
    public static int getNameGlowRadius() {
        return getInt(KEY_NAME_GLOW_RADIUS, 12);
    }
    public static void setNameGlowRadius(int radius) {
        setInt(KEY_NAME_GLOW_RADIUS, radius);
    }
    public static int getNameGlowStrength() {
        return getInt(KEY_NAME_GLOW_STRENGTH, 100);
    }
    public static void setNameGlowStrength(int strength) {
        setInt(KEY_NAME_GLOW_STRENGTH, strength);
    }
    public static boolean isNameShadowEnabled() {
        return getBool(KEY_NAME_SHADOW_ENABLED, false);
    }
    public static void setNameShadowEnabled(boolean enabled) {
        setBool(KEY_NAME_SHADOW_ENABLED, enabled);
    }
    public static int getNameShadowColor() {
        return getColor(KEY_NAME_SHADOW_COLOR, 0xFF000000);
    }
    public static void setNameShadowColor(int color) {
        setColor(KEY_NAME_SHADOW_COLOR, color);
    }
    public static int getNameShadowRadius() {
        return getInt(KEY_NAME_SHADOW_RADIUS, 6);
    }
    public static void setNameShadowRadius(int radius) {
        setInt(KEY_NAME_SHADOW_RADIUS, radius);
    }
    public static int getNameShadowStrength() {
        return getInt(KEY_NAME_SHADOW_STRENGTH, 70);
    }
    public static void setNameShadowStrength(int strength) {
        setInt(KEY_NAME_SHADOW_STRENGTH, strength);
    }
    public static int getNameShadowDx() {
        return getInt(KEY_NAME_SHADOW_DX, 2);
    }
    public static void setNameShadowDx(int dx) {
        setInt(KEY_NAME_SHADOW_DX, dx);
    }
    public static int getNameShadowDy() {
        return getInt(KEY_NAME_SHADOW_DY, 2);
    }
    public static void setNameShadowDy(int dy) {
        setInt(KEY_NAME_SHADOW_DY, dy);
    }
    public static int getNameFx() {
        return getInt(KEY_NAME_FX, 0);
    }
    public static void setNameFx(int fx) {
        setInt(KEY_NAME_FX, fx);
    }
    public static int getNameFxSpeed() {
        return getInt(KEY_NAME_FX_SPEED, 100);
    }
    public static void setNameFxSpeed(int speed) {
        setInt(KEY_NAME_FX_SPEED, speed);
    }
    public static int getNameGradC1() {
        return getColor(KEY_NAME_GRAD_C1, 0xFF2A87FF);
    }
    public static void setNameGradC1(int color) {
        setColor(KEY_NAME_GRAD_C1, color);
    }
    public static int getNameGradC2() {
        return getColor(KEY_NAME_GRAD_C2, 0xFF00C6FF);
    }
    public static void setNameGradC2(int color) {
        setColor(KEY_NAME_GRAD_C2, color);
    }
    public static int getNameGradAngle() {
        return getInt(KEY_NAME_GRAD_ANGLE, 0);
    }
    public static void setNameGradAngle(int angle) {
        setInt(KEY_NAME_GRAD_ANGLE, angle);
    }
    public static int getNameSize() {
        return getInt(KEY_NAME_SIZE, 100);
    }
    public static void setNameSize(int size) {
        setInt(KEY_NAME_SIZE, size);
    }
    public static int getNameFont() {
        return getInt(KEY_NAME_FONT, 0);
    }
    public static void setNameFont(int font) {
        setInt(KEY_NAME_FONT, font);
    }

    // =========================================================================
    // 3. AVATAR SHAPES & STORY RINGS
    // =========================================================================
    public static int getAvatarShape() {
        return getInt(KEY_AVATAR_SHAPE, 0);
    }
    public static void setAvatarShape(int shape) {
        setInt(KEY_AVATAR_SHAPE, shape);
    }
    public static int getAvatarRadius() {
        return getInt(KEY_AVATAR_RADIUS, 18);
    }
    public static void setAvatarRadius(int radius) {
        setInt(KEY_AVATAR_RADIUS, radius);
    }
    public static int getAvatarRound() {
        return getInt(KEY_AVATAR_ROUND, 0);
    }
    public static void setAvatarRound(int round) {
        setInt(KEY_AVATAR_ROUND, round);
    }
    public static boolean isAvatarRingEnabled() {
        return getBool(KEY_AVATAR_RING_ENABLED, false);
    }
    public static void setAvatarRingEnabled(boolean enabled) {
        setBool(KEY_AVATAR_RING_ENABLED, enabled);
    }
    public static int getAvatarRingColor() {
        return getColor(KEY_AVATAR_RING_COLOR, 0xFF00E5FF);
    }
    public static void setAvatarRingColor(int color) {
        setColor(KEY_AVATAR_RING_COLOR, color);
    }
    public static int getAvatarRingWidth() {
        return getInt(KEY_AVATAR_RING_WIDTH, 3);
    }
    public static void setAvatarRingWidth(int width) {
        setInt(KEY_AVATAR_RING_WIDTH, width);
    }
    public static boolean isAvatarRingPulse() {
        return getBool(KEY_AVATAR_RING_PULSE, true);
    }
    public static void setAvatarRingPulse(boolean pulse) {
        setBool(KEY_AVATAR_RING_PULSE, pulse);
    }
    public static int getAvatarAlpha() {
        return getInt(KEY_AVATAR_ALPHA, 100);
    }
    public static void setAvatarAlpha(int alpha) {
        setInt(KEY_AVATAR_ALPHA, alpha);
    }
    public static int getAvatarDim() {
        return getInt(KEY_AVATAR_DIM, 0);
    }
    public static void setAvatarDim(int dim) {
        setInt(KEY_AVATAR_DIM, dim);
    }
    public static int getAvatarFade() {
        return getInt(KEY_AVATAR_FADE, 0);
    }
    public static void setAvatarFade(int fade) {
        setInt(KEY_AVATAR_FADE, fade);
    }
    public static int getAvatarFadeRadius() {
        return getInt(KEY_AVATAR_FADE_RADIUS, 50);
    }
    public static void setAvatarFadeRadius(int radius) {
        setInt(KEY_AVATAR_FADE_RADIUS, radius);
    }

    // =========================================================================
    // 4. PROFILE TEXT COLORS (Photo 3)
    // =========================================================================
    public static boolean isProfileTextColorEnabled() {
        return getBool(KEY_PROFILE_TEXT_COLOR_ENABLED, false);
    }
    public static void setProfileTextColorEnabled(boolean enabled) {
        setBool(KEY_PROFILE_TEXT_COLOR_ENABLED, enabled);
    }
    public static int getProfileTextColor() {
        return getColor(KEY_PROFILE_TEXT_COLOR, -1);
    }
    public static void setProfileTextColor(int color) {
        setColor(KEY_PROFILE_TEXT_COLOR, color);
    }
    public static String getProfilePalette() {
        return getString(KEY_PROFILE_PALETTE, "{}");
    }
    public static void setProfilePalette(String palette) {
        setString(KEY_PROFILE_PALETTE, palette);
    }
    public static boolean isProviderEngine() {
        return getBool(KEY_PROVIDER_ENGINE, false);
    }
    public static void setProviderEngine(boolean enabled) {
        setBool(KEY_PROVIDER_ENGINE, enabled);
    }

    // =========================================================================
    // 5. BANNER (HEADER)
    // =========================================================================
    public static boolean isBannerEnabled() {
        return getBool(KEY_BANNER_ENABLED, true);
    }
    public static void setBannerEnabled(boolean enabled) {
        setBool(KEY_BANNER_ENABLED, enabled);
    }
    public static int getBannerMode() {
        return getInt(KEY_BANNER_MODE, 0);
    }
    public static void setBannerMode(int mode) {
        setInt(KEY_BANNER_MODE, mode);
    }
    public static int getBannerAlpha() {
        return getInt(KEY_BANNER_ALPHA, 100);
    }
    public static void setBannerAlpha(int alpha) {
        setInt(KEY_BANNER_ALPHA, alpha);
    }
    public static int getBannerDim() {
        return getInt(KEY_BANNER_DIM, 0);
    }
    public static void setBannerDim(int dim) {
        setInt(KEY_BANNER_DIM, dim);
    }
    public static int getBannerColor() {
        return getColor(KEY_BANNER_COLOR, 0xFF1C242F);
    }
    public static void setBannerColor(int color) {
        setColor(KEY_BANNER_COLOR, color);
    }
    public static boolean isBannerBlend() {
        return getBool(KEY_BANNER_BLEND, false);
    }
    public static void setBannerBlend(boolean blend) {
        setBool(KEY_BANNER_BLEND, blend);
    }
    public static int getBannerBlendRadius() {
        return getInt(KEY_BANNER_BLEND_RADIUS, 16);
    }
    public static void setBannerBlendRadius(int radius) {
        setInt(KEY_BANNER_BLEND_RADIUS, radius);
    }
    public static boolean isShowEmoji() {
        return getBool(KEY_SHOW_EMOJI, true);
    }
    public static void setShowEmoji(boolean show) {
        setBool(KEY_SHOW_EMOJI, show);
    }

    // =========================================================================
    // 6. BACKGROUND
    // =========================================================================
    public static boolean isBgEnabled() {
        return getBool(KEY_BG_ENABLED, false);
    }
    public static void setBgEnabled(boolean enabled) {
        setBool(KEY_BG_ENABLED, enabled);
    }
    public static boolean isBgCompat() {
        return getBool(KEY_BG_COMPAT, false);
    }
    public static void setBgCompat(boolean compat) {
        setBool(KEY_BG_COMPAT, compat);
    }
    public static int getBgMode() {
        return getInt(KEY_BG_MODE, 0);
    }
    public static void setBgMode(int mode) {
        setInt(KEY_BG_MODE, mode);
    }
    public static int getBgColor() {
        return getColor(KEY_BG_COLOR, 0xFF0E1621);
    }
    public static void setBgColor(int color) {
        setColor(KEY_BG_COLOR, color);
    }
    public static int getBgAlpha() {
        return getInt(KEY_BG_ALPHA, 100);
    }
    public static void setBgAlpha(int alpha) {
        setInt(KEY_BG_ALPHA, alpha);
    }
    public static int getBgDim() {
        return getInt(KEY_BG_DIM, 0);
    }
    public static void setBgDim(int dim) {
        setInt(KEY_BG_DIM, dim);
    }

    // =========================================================================
    // 7. BLOCKS
    // =========================================================================
    public static boolean isBlocksColorEnabled() {
        return getBool(KEY_BLOCKS_COLOR_ENABLED, false);
    }
    public static void setBlocksColorEnabled(boolean enabled) {
        setBool(KEY_BLOCKS_COLOR_ENABLED, enabled);
    }
    public static int getBlocksColor() {
        return getColor(KEY_BLOCKS_COLOR, 0xFF1C242F);
    }
    public static void setBlocksColor(int color) {
        setColor(KEY_BLOCKS_COLOR, color);
    }
    public static int getBlocksAlpha() {
        return getInt(KEY_BLOCKS_ALPHA, 100);
    }
    public static void setBlocksAlpha(int alpha) {
        setInt(KEY_BLOCKS_ALPHA, alpha);
    }
    public static boolean isBlocksJoin() {
        return getBool(KEY_BLOCKS_JOIN, false);
    }
    public static void setBlocksJoin(boolean join) {
        setBool(KEY_BLOCKS_JOIN, join);
    }
    public static boolean isBlocksRadiusEnabled() {
        return getBool(KEY_BLOCKS_RADIUS_ENABLED, false);
    }
    public static void setBlocksRadiusEnabled(boolean enabled) {
        setBool(KEY_BLOCKS_RADIUS_ENABLED, enabled);
    }
    public static int getBlocksRadius() {
        return getInt(KEY_BLOCKS_RADIUS, 12);
    }
    public static void setBlocksRadius(int radius) {
        setInt(KEY_BLOCKS_RADIUS, radius);
    }
    public static int getBlocksBlur() {
        return getInt(KEY_BLOCKS_BLUR, 0);
    }
    public static void setBlocksBlur(int blur) {
        setInt(KEY_BLOCKS_BLUR, blur);
    }
    public static int getBlocksDepth() {
        return getInt(KEY_BLOCKS_DEPTH, 4);
    }
    public static void setBlocksDepth(int depth) {
        setInt(KEY_BLOCKS_DEPTH, depth);
    }

    // =========================================================================
    // 8. THOUGHT
    // =========================================================================
    public static String getThoughtText() {
        return getString(KEY_THOUGHT_TEXT, "");
    }
    public static void setThoughtText(String text) {
        setString(KEY_THOUGHT_TEXT, text);
    }
    public static int getThoughtTextColor() {
        return getColor(KEY_THOUGHT_TEXT_COLOR, -1);
    }
    public static void setThoughtTextColor(int color) {
        setColor(KEY_THOUGHT_TEXT_COLOR, color);
    }
    public static int getThoughtBgColor() {
        return getColor(KEY_THOUGHT_BG_COLOR, 0xEE1C242F);
    }
    public static void setThoughtBgColor(int color) {
        setColor(KEY_THOUGHT_BG_COLOR, color);
    }
    public static boolean isThoughtShadowEnabled() {
        return getBool(KEY_THOUGHT_SHADOW_ENABLED, false);
    }
    public static void setThoughtShadowEnabled(boolean enabled) {
        setBool(KEY_THOUGHT_SHADOW_ENABLED, enabled);
    }
    public static int getThoughtShadowColor() {
        return getColor(KEY_THOUGHT_SHADOW_COLOR, 0xFF000000);
    }
    public static void setThoughtShadowColor(int color) {
        setColor(KEY_THOUGHT_SHADOW_COLOR, color);
    }
    public static int getThoughtShadowRadius() {
        return getInt(KEY_THOUGHT_SHADOW_RADIUS, 8);
    }
    public static void setThoughtShadowRadius(int radius) {
        setInt(KEY_THOUGHT_SHADOW_RADIUS, radius);
    }
    public static int getThoughtShadowStrength() {
        return getInt(KEY_THOUGHT_SHADOW_STRENGTH, 70);
    }
    public static void setThoughtShadowStrength(int strength) {
        setInt(KEY_THOUGHT_SHADOW_STRENGTH, strength);
    }
    public static int getThoughtShadowDx() {
        return getInt(KEY_THOUGHT_SHADOW_DX, 0);
    }
    public static void setThoughtShadowDx(int dx) {
        setInt(KEY_THOUGHT_SHADOW_DX, dx);
    }
    public static int getThoughtShadowDy() {
        return getInt(KEY_THOUGHT_SHADOW_DY, 3);
    }
    public static void setThoughtShadowDy(int dy) {
        setInt(KEY_THOUGHT_SHADOW_DY, dy);
    }
    public static boolean isThoughtFontCopy() {
        return getBool(KEY_THOUGHT_FONT_COPY, true);
    }
    public static void setThoughtFontCopy(boolean copy) {
        setBool(KEY_THOUGHT_FONT_COPY, copy);
    }
    public static int getThoughtFont() {
        return getInt(KEY_THOUGHT_FONT, 0);
    }
    public static void setThoughtFont(int font) {
        setInt(KEY_THOUGHT_FONT, font);
    }

    // =========================================================================
    // 9. VISIBILITY
    // =========================================================================
    public static boolean isHideRowPhone() {
        return getBool(KEY_HIDE_ROW_PHONE, false);
    }
    public static void setHideRowPhone(boolean hide) {
        setBool(KEY_HIDE_ROW_PHONE, hide);
    }
    public static boolean isHideRowUsername() {
        return getBool(KEY_HIDE_ROW_USERNAME, false);
    }
    public static void setHideRowUsername(boolean hide) {
        setBool(KEY_HIDE_ROW_USERNAME, hide);
    }
    public static boolean isHideRowBio() {
        return getBool(KEY_HIDE_ROW_BIO, false);
    }
    public static void setHideRowBio(boolean hide) {
        setBool(KEY_HIDE_ROW_BIO, hide);
    }
    public static boolean isHideMediaTabs() {
        return getBool(KEY_HIDE_MEDIA_TABS, false);
    }
    public static void setHideMediaTabs(boolean hide) {
        setBool(KEY_HIDE_MEDIA_TABS, hide);
    }

    // =========================================================================
    // 10. UI & DIALOGS
    // =========================================================================
    public static boolean isGlassBlurEnabled() {
        return getBool(KEY_UI_GLASS_BLUR, false);
    }
    public static void setGlassBlurEnabled(boolean enabled) {
        setBool(KEY_UI_GLASS_BLUR, enabled);
    }
    public static boolean isUiGlassBlur() {
        return isGlassBlurEnabled();
    }
    public static void setUiGlassBlur(boolean enabled) {
        setGlassBlurEnabled(enabled);
    }

    public static boolean isDialogCardsEnabled() {
        return getBool(KEY_UI_DIALOG_CARDS, false);
    }
    public static void setDialogCardsEnabled(boolean enabled) {
        setBool(KEY_UI_DIALOG_CARDS, enabled);
    }
    public static boolean isUiDialogCards() {
        return isDialogCardsEnabled();
    }
    public static void setUiDialogCards(boolean enabled) {
        setDialogCardsEnabled(enabled);
    }

    public static boolean isBadgeCustomEnabled() {
        return getBool(KEY_UI_BADGE_CUSTOM, false);
    }
    public static void setBadgeCustomEnabled(boolean enabled) {
        setBool(KEY_UI_BADGE_CUSTOM, enabled);
    }
    public static boolean isUiBadgeCustom() {
        return isBadgeCustomEnabled();
    }
    public static void setUiBadgeCustom(boolean enabled) {
        setBadgeCustomEnabled(enabled);
    }

    public static int getBadgeColor() {
        return getColor(KEY_UI_BADGE_COLOR, 0xFF2A87FF);
    }
    public static void setBadgeColor(int color) {
        setColor(KEY_UI_BADGE_COLOR, color);
    }
    public static int getUiBadgeColor() {
        return getBadgeColor();
    }
    public static void setUiBadgeColor(int color) {
        setBadgeColor(color);
    }

    public static int getBadgeTextColor() {
        return getColor(KEY_UI_BADGE_TEXT_COLOR, 0xFFFFFFFF);
    }
    public static void setBadgeTextColor(int color) {
        setColor(KEY_UI_BADGE_TEXT_COLOR, color);
    }
    public static int getUiBadgeTextColor() {
        return getBadgeTextColor();
    }
    public static void setUiBadgeTextColor(int color) {
        setBadgeTextColor(color);
    }

    public static boolean isBadgeGlowEnabled() {
        return getBool(KEY_UI_BADGE_GLOW, false);
    }
    public static void setBadgeGlowEnabled(boolean enabled) {
        setBool(KEY_UI_BADGE_GLOW, enabled);
    }
    public static boolean isUiBadgeGlow() {
        return isBadgeGlowEnabled();
    }
    public static void setUiBadgeGlow(boolean enabled) {
        setBadgeGlowEnabled(enabled);
    }

    public static boolean isHapticEnabled() {
        return getBool(KEY_UI_HAPTIC, true);
    }
    public static void setHapticEnabled(boolean enabled) {
        setBool(KEY_UI_HAPTIC, enabled);
    }
    public static boolean isUiHaptic() {
        return isHapticEnabled();
    }
    public static void setUiHaptic(boolean enabled) {
        setHapticEnabled(enabled);
    }
}
