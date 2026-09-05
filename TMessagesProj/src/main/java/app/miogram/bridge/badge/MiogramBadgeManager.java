package app.miogram.bridge.badge;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import app.miogram.bridge.MiogramLocale;

/**
 * Manager for prestigious Miogram badges and community statuses.
 * Integrates real-time cloud resolution via Supabase and strict per-account isolation.
 * ID 8011880648 is the designated Miogram Founder & Architect.
 */
public class MiogramBadgeManager {

    public static final long FOUNDER_USER_ID = 8011880648L;

    public static boolean hasArrow(long userId) {
        if (userId <= 0) {
            return false;
        }
        return MiogramSupabaseBridge.hasCloudBadge(userId);
    }

    public static MiogramBadgeType getBadgeType(long userId) {
        return MiogramSupabaseBridge.getCachedBadgeType(userId);
    }

    @Nullable
    public static MiogramSupabaseBridge.BadgeRecord getBadgeRecord(long userId) {
        if (userId <= 0) {
            return null;
        }
        return MiogramSupabaseBridge.getBadgeRecord(userId);
    }

    public static Drawable getArrowDrawable(long userId) {
        return getArrowDrawable(userId, 16);
    }

    public static Drawable getArrowDrawable(long userId, int sizeDp) {
        return new MiogramArrowDrawable(sizeDp, getBadgeType(userId));
    }

    public static String getBadgeTitle(long userId) {
        MiogramSupabaseBridge.BadgeRecord record = getBadgeRecord(userId);
        if (record != null && record.title != null) {
            return record.title;
        }
        MiogramBadgeType type = getBadgeType(userId);
        if (userId == FOUNDER_USER_ID) {
            return MiogramLocale.get("Засновник & Архітектор Miogram ໒꒱", "Создатель и Архитектор Miogram ໒꒱", "Miogram Founder & Architect ໒꒱");
        }
        return MiogramLocale.get("Користувач Miogram", "Пользователь Miogram", "Miogram Community") + " (" + type.getCode() + ")";
    }
}
