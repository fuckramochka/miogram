package app.miogram.bridge.player;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.concurrent.CopyOnWriteArrayList;

public class MiogramPlayerPrefs {

    private static final String PREFS_NAME = "miogram_player_prefs";

    public static final int BG_MODE_COVER_BLUR = 0;
    public static final int BG_MODE_GRADIENT = 1;
    public static final int BG_MODE_SOLID = 2;
    public static final int BG_MODE_TRANSPARENT = 3;
    public static final int BG_MODE_CUSTOM_PHOTO = 4;
    public static final int BG_MODE_CUSTOM_VIDEO = 5;

    public static final String DEFAULT_CONTROLS_ORDER = "shuffle,repeat,prev,play,next,queue";

    public interface OnPrefsChangedListener {
        void onPrefsChanged();
    }

    private static final CopyOnWriteArrayList<OnPrefsChangedListener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(OnPrefsChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(OnPrefsChangedListener listener) {
        listeners.remove(listener);
    }

    public static void notifyChanged() {
        for (OnPrefsChangedListener l : listeners) {
            try {
                l.onPrefsChanged();
            } catch (Throwable ignore) {}
        }
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Background ---

    public static int getBackgroundMode() {
        return getPrefs().getInt("bg_mode", BG_MODE_COVER_BLUR);
    }

    public static void setBackgroundMode(int mode) {
        getPrefs().edit().putInt("bg_mode", mode).apply();
        notifyChanged();
    }

    public static float getBgOpacity() {
        return getPrefs().getFloat("bg_opacity", 0.90f);
    }

    public static void setBgOpacity(float opacity) {
        getPrefs().edit().putFloat("bg_opacity", opacity).apply();
        notifyChanged();
    }

    public static int getBgBlur() {
        return getPrefs().getInt("bg_blur", 15);
    }

    public static void setBgBlur(int blur) {
        getPrefs().edit().putInt("bg_blur", blur).apply();
        notifyChanged();
    }

    public static float getBgBrightness() {
        return getPrefs().getFloat("bg_brightness", 1.0f);
    }

    public static void setBgBrightness(float brightness) {
        getPrefs().edit().putFloat("bg_brightness", brightness).apply();
        notifyChanged();
    }

    public static int getGradientColor1() {
        return getPrefs().getInt("grad_col1", 0xFF231B32);
    }

    public static void setGradientColor1(int col) {
        getPrefs().edit().putInt("grad_col1", col).apply();
        notifyChanged();
    }

    public static int getGradientColor2() {
        return getPrefs().getInt("grad_col2", 0xFF0E0D13);
    }

    public static void setGradientColor2(int col) {
        getPrefs().edit().putInt("grad_col2", col).apply();
        notifyChanged();
    }

    public static int getGradientOrientation() {
        return getPrefs().getInt("grad_orientation", 0);
    }

    public static void setGradientOrientation(int orientation) {
        getPrefs().edit().putInt("grad_orientation", orientation).apply();
        notifyChanged();
    }

    public static int getSolidColor() {
        return getPrefs().getInt("solid_col", 0xFF13151D);
    }

    public static void setSolidColor(int col) {
        getPrefs().edit().putInt("solid_col", col).apply();
        notifyChanged();
    }

    public static String getCustomPhotoPath() {
        return getPrefs().getString("custom_photo_path", "");
    }

    public static void setCustomPhotoPath(String path) {
        getPrefs().edit().putString("custom_photo_path", path != null ? path : "").apply();
        notifyChanged();
    }

    public static String getCustomVideoPath() {
        return getPrefs().getString("custom_video_path", "");
    }

    public static void setCustomVideoPath(String path) {
        getPrefs().edit().putString("custom_video_path", path != null ? path : "").apply();
        notifyChanged();
    }

    public static boolean isVisualizerEnabled() {
        return getPrefs().getBoolean("visualizer_enabled", false);
    }

    public static void setVisualizerEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("visualizer_enabled", enabled).apply();
        notifyChanged();
    }

    public static boolean isProfileButtonEnabled() {
        return getPrefs().getBoolean("profile_btn_enabled", true);
    }

    public static void setProfileButtonEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("profile_btn_enabled", enabled).apply();
        notifyChanged();
    }

    // --- Buttons & Elements ---

    public static float getButtonOpacity() {
        return getPrefs().getFloat("btn_opacity", 1.0f);
    }

    public static void setButtonOpacity(float opacity) {
        getPrefs().edit().putFloat("btn_opacity", opacity).apply();
        notifyChanged();
    }

    public static boolean isButtonGlowEnabled() {
        return getPrefs().getBoolean("btn_glow", true);
    }

    public static void setButtonGlowEnabled(boolean glow) {
        getPrefs().edit().putBoolean("btn_glow", glow).apply();
        notifyChanged();
    }

    public static int getButtonGlowColor() {
        return getPrefs().getInt("btn_glow_color", 0); // 0 = accent
    }

    public static void setButtonGlowColor(int col) {
        getPrefs().edit().putInt("btn_glow_color", col).apply();
        notifyChanged();
    }

    // --- Lyrics ---

    public static int getLyricsFontSize() {
        return getPrefs().getInt("lyrics_font_size", 18);
    }

    public static void setLyricsFontSize(int size) {
        getPrefs().edit().putInt("lyrics_font_size", size).apply();
        notifyChanged();
    }

    public static boolean isLyricsActiveGlow() {
        return getPrefs().getBoolean("lyrics_active_glow", true);
    }

    public static void setLyricsActiveGlow(boolean glow) {
        getPrefs().edit().putBoolean("lyrics_active_glow", glow).apply();
        notifyChanged();
    }

    public static int getLyricsActiveColor() {
        return getPrefs().getInt("lyrics_active_color", 0); // 0 = accent
    }

    public static void setLyricsActiveColor(int col) {
        getPrefs().edit().putInt("lyrics_active_color", col).apply();
        notifyChanged();
    }

    public static float getLyricsInactiveOpacity() {
        return getPrefs().getFloat("lyrics_inactive_opacity", 0.55f);
    }

    public static void setLyricsInactiveOpacity(float opacity) {
        getPrefs().edit().putFloat("lyrics_inactive_opacity", opacity).apply();
        notifyChanged();
    }

    // --- Controls order / visibility (drag&drop) ---

    public static String getControlsOrder() {
        return getPrefs().getString("controls_order", DEFAULT_CONTROLS_ORDER);
    }

    public static void setControlsOrder(String order) {
        getPrefs().edit().putString("controls_order", order != null ? order : DEFAULT_CONTROLS_ORDER).apply();
        notifyChanged();
    }

    public static java.util.List<String> getControlsOrderList() {
        String raw = getControlsOrder();
        java.util.List<String> out = new java.util.ArrayList<>();
        if (raw != null) {
            for (String s : raw.split(",")) {
                s = s.trim();
                if (!s.isEmpty() && !out.contains(s)) out.add(s);
            }
        }
        for (String def : DEFAULT_CONTROLS_ORDER.split(",")) {
            if (!out.contains(def)) out.add(def);
        }
        return out;
    }

    public static void moveControl(String id, int delta) {
        try {
            java.util.List<String> order = new java.util.ArrayList<>(getControlsOrderList());
            int idx = order.indexOf(id);
            int to = idx + delta;
            if (idx < 0 || to < 0 || to >= order.size()) return;
            order.remove(idx);
            order.add(to, id);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(order.get(i));
            }
            setControlsOrder(sb.toString());
        } catch (Throwable ignore) {}
    }

    public static boolean isControlHidden(String id) {
        return getPrefs().getBoolean("hide_ctrl_" + id, false);
    }

    public static void setControlHidden(String id, boolean hidden) {
        getPrefs().edit().putBoolean("hide_ctrl_" + id, hidden).apply();
        notifyChanged();
    }

    public static void resetControlsLayout() {
        getPrefs().edit().putString("controls_order", DEFAULT_CONTROLS_ORDER).apply();
        for (String def : DEFAULT_CONTROLS_ORDER.split(",")) {
            getPrefs().edit().putBoolean("hide_ctrl_" + def, false).apply();
        }
        notifyChanged();
    }

    public static boolean isCustomMediaValid(String path) {
        if (path == null || path.isEmpty()) return false;
        try {
            java.io.File f = new java.io.File(path);
            return f.exists() && f.isFile() && f.length() > 0 && f.canRead();
        } catch (Throwable ignore) {
            return false;
        }
    }

    // Reset all to defaults
    public static void resetToDefaults() {
        String order = DEFAULT_CONTROLS_ORDER;
        getPrefs().edit().clear().apply();
        getPrefs().edit().putString("controls_order", order).apply();
        notifyChanged();
    }
}
