package app.miogram.bridge.badge;

import android.graphics.drawable.Drawable;

import org.telegram.messenger.UserConfig;

import app.miogram.bridge.MiogramLocale;

/**
 * Manager for prestigious Miogram badges and community statuses.
 * Integrates dynamic cloud resolution via Supabase and local instant caching.
 * ID 8011880648 is the designated Miogram Founder & Architect.
 */
public class MiogramBadgeManager {

    public static final long FOUNDER_USER_ID = 8011880648L;

    public static boolean hasArrow(long userId) {
        if (userId == FOUNDER_USER_ID) {
            return true;
        }
        MiogramBadgeType cloudBadge = MiogramSupabaseBridge.getCachedBadge(userId);
        if (cloudBadge != null) {
            return true;
        }
        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (userId == currentUserId && MiogramSupabaseBridge.isSyncEnabled(null)) {
            return true;
        }
        return false;
    }

    public static MiogramBadgeType getBadgeType(long userId) {
        MiogramBadgeType cloudBadge = MiogramSupabaseBridge.getCachedBadge(userId);
        if (cloudBadge != null) {
            return cloudBadge;
        }
        if (userId == FOUNDER_USER_ID) {
            return MiogramBadgeType.ORIGINAL;
        }
        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (userId == currentUserId) {
            return MiogramSupabaseBridge.getSelectedBadge(null);
        }
        return MiogramBadgeType.ORIGINAL;
    }

    public static Drawable getArrowDrawable(long userId) {
        return getArrowDrawable(userId, 16);
    }

    public static Drawable getArrowDrawable(long userId, int sizeDp) {
        return new MiogramArrowDrawable(sizeDp, getBadgeType(userId));
    }

    public static String getBadgeTitle(long userId) {
        MiogramBadgeType type = getBadgeType(userId);
        if (userId == FOUNDER_USER_ID) {
            return MiogramLocale.get("Засновник Miogram", "Создатель Miogram", "Miogram Founder") + " (" + type.getCode() + ")";
        }
        return MiogramLocale.get("Користувач Miogram", "Пользователь Miogram", "Miogram Community") + " (" + type.getCode() + ")";
    }
}
