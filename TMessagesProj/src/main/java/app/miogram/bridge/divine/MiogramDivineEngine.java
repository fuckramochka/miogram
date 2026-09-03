package app.miogram.bridge.divine;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import app.exteraless.appearance.AppearanceConfig;
import app.miogram.bridge.MiogramFlags;
import app.miogram.bridge.ui.MiogramVisualsPrefs;
import app.miogram.bridge.ui.discord.MiogramDiscordLayout;
import xyz.nextalone.nagram.NaConfig;

/**
 * Miogram Divine Engine:
 * Unified global preset and theme orchestration engine.
 * Enables 1-click batch application of themes, layouts, spatial decorations, and macro transforms.
 */
public class MiogramDivineEngine {

    public enum Preset {
        CLASSIC_TG,
        DISCORD_ULTRA,
        IOS_GLASS,
        MINIMALIST
    }

    private static final String PREFS_NAME = "miogram_divine_prefs";
    private static final String KEY_CURRENT_PRESET = "current_divine_preset";

    public static Preset getCurrentPreset(Context context) {
        if (context == null) context = ApplicationLoader.applicationContext;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_CURRENT_PRESET, Preset.CLASSIC_TG.name());
        try {
            return Preset.valueOf(name);
        } catch (Throwable t) {
            return Preset.CLASSIC_TG;
        }
    }

    public static String getPresetTitle(Preset preset) {
        switch (preset) {
            case DISCORD_ULTRA:
                return app.miogram.bridge.MiogramLocale.get("🎮 Discord Ultra (Автентичний 2026)", "🎮 Discord Ultra (Аутентичный 2026)", "🎮 Discord Ultra (Authentic 2026)");
            case IOS_GLASS:
                return app.miogram.bridge.MiogramLocale.get("🍏 iOS Glassmorphism (1:1 Apple Style)", "🍏 iOS Glassmorphism (1:1 Apple Style)", "🍏 iOS Glassmorphism (1:1 Apple Style)");
            case MINIMALIST:
                return app.miogram.bridge.MiogramLocale.get("⚡ Minimalist (Швидкість та фокус)", "⚡ Minimalist (Скорость и фокус)", "⚡ Minimalist (Speed & Focus)");
            case CLASSIC_TG:
            default:
                return app.miogram.bridge.MiogramLocale.get("📱 Classic TG (Ame-Chan Стандарт)", "📱 Classic TG (Ame-Chan Стандарт)", "📱 Classic TG (Ame-Chan Standard)");
        }
    }

    public static void applyPreset(Context context, Preset preset) {
        if (context == null) context = ApplicationLoader.applicationContext;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_PRESET, preset.name()).apply();

        switch (preset) {
            case DISCORD_ULTRA:
                MiogramDiscordLayout.setDiscordUiEnabled(true);
                MiogramFlags.setSpatialDecoration(false);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", true);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(true);
                break;

            case IOS_GLASS:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(true);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", true);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", true);
                AppearanceConfig.singleCornerRadius.setConfigBool(true);
                AppearanceConfig.senderMiniAvatars.setConfigBool(false);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(false);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(false);
                break;

            case MINIMALIST:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(false);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", false);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", false);
                AppearanceConfig.singleCornerRadius.setConfigBool(false);
                AppearanceConfig.senderMiniAvatars.setConfigBool(false);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(true);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(true);
                NaConfig.INSTANCE.getDisableStories().setConfigBool(true);
                break;

            case CLASSIC_TG:
            default:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(true);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", true);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", true);
                AppearanceConfig.singleCornerRadius.setConfigBool(true);
                AppearanceConfig.senderMiniAvatars.setConfigBool(true);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(false);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(false);
                break;
        }

        org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
            org.telegram.ui.LaunchActivity act = org.telegram.ui.LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                act.rebuildAllFragments(false);
            }
        });
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}
