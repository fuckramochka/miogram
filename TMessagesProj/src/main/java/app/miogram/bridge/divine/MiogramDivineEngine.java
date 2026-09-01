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
        AME_DIVINE,
        IOS_GLASS,
        CYBER_NEO,
        MINIMALIST_PRO
    }

    private static final String PREFS_NAME = "miogram_divine_prefs";
    private static final String KEY_CURRENT_PRESET = "current_divine_preset";

    public static Preset getCurrentPreset(Context context) {
        if (context == null) context = ApplicationLoader.applicationContext;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_CURRENT_PRESET, Preset.AME_DIVINE.name());
        try {
            return Preset.valueOf(name);
        } catch (Throwable t) {
            return Preset.AME_DIVINE;
        }
    }

    public static String getPresetTitle(Preset preset) {
        switch (preset) {
            case AME_DIVINE:
                return app.miogram.bridge.MiogramLocale.get("Ame Divine (Кіберпанк-Пастель)", "Ame Divine (Киберпанк-Пастель)", "Ame Divine (Cyberpunk Pastel)");
            case DISCORD_ULTRA:
                return app.miogram.bridge.MiogramLocale.get("Discord Ultra (Сервери та канали)", "Discord Ultra (Серверы и каналы)", "Discord Ultra (Guilds & Channels)");
            case IOS_GLASS:
                return app.miogram.bridge.MiogramLocale.get("iOS Glassmorphism (Apple Style)", "iOS Glassmorphism (Apple Style)", "iOS Glassmorphism (Apple Style)");
            case CYBER_NEO:
                return app.miogram.bridge.MiogramLocale.get("Cyber Neo (Світіння та неон)", "Cyber Neo (Свечение и неон)", "Cyber Neo (Neon Glow & Mesh)");
            case MINIMALIST_PRO:
                return app.miogram.bridge.MiogramLocale.get("Minimalist Pro (Швидкість та фокус)", "Minimalist Pro (Скорость и фокус)", "Minimalist Pro (Speed & Focus)");
            case CLASSIC_TG:
            default:
                return app.miogram.bridge.MiogramLocale.get("Classic Telegram (Оригінал)", "Classic Telegram (Оригинал)", "Classic Telegram (Stock)");
        }
    }

    public static void applyPreset(Context context, Preset preset) {
        if (context == null) context = ApplicationLoader.applicationContext;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_PRESET, preset.name()).apply();

        switch (preset) {
            case AME_DIVINE:
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

            case DISCORD_ULTRA:
                MiogramDiscordLayout.setDiscordUiEnabled(true);
                MiogramFlags.setSpatialDecoration(false);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", false);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(true);
                break;

            case IOS_GLASS:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(true);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", true);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", true);
                AppearanceConfig.singleCornerRadius.setConfigBool(true);
                AppearanceConfig.senderMiniAvatars.setConfigBool(false);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(false);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(false);
                break;

            case CYBER_NEO:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(true);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", true);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", true);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", true);
                AppearanceConfig.singleCornerRadius.setConfigBool(false);
                AppearanceConfig.senderMiniAvatars.setConfigBool(true);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(false);
                NaConfig.INSTANCE.getHideBottomNavigationBar().setConfigBool(false);
                break;

            case MINIMALIST_PRO:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(false);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", false);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", false);
                NaConfig.INSTANCE.getMainTabsHideTitles().setConfigBool(true);
                break;

            case CLASSIC_TG:
            default:
                MiogramDiscordLayout.setDiscordUiEnabled(false);
                MiogramFlags.setSpatialDecoration(false);
                MiogramVisualsPrefs.saveBool(context, "agsl_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "ame_vibe_enabled", false);
                MiogramVisualsPrefs.saveBool(context, "apple_music_player", false);
                MiogramVisualsPrefs.saveBool(context, "mini_bass_glow", false);
                break;
        }

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme);
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}
