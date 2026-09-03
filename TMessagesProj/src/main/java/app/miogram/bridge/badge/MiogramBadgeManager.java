package app.miogram.bridge.badge;

import android.graphics.drawable.Drawable;

/**
 * Manager for prestigious Miogram badges and elite statuses.
 * ID 8011880648 is the designated Miogram Founder & First Owner of the Arrow.
 */
public class MiogramBadgeManager {

    public static final long FOUNDER_USER_ID = 8011880648L;

    public static boolean hasArrow(long userId) {
        return userId == FOUNDER_USER_ID;
    }

    public static Drawable getArrowDrawable(long userId) {
        return new MiogramArrowDrawable(16);
    }

    public static Drawable getArrowDrawable(long userId, int sizeDp) {
        return new MiogramArrowDrawable(sizeDp);
    }

    public static String getBadgeTitle(long userId) {
        if (userId == FOUNDER_USER_ID) {
            return "Miogram Founder (Ame-chan Edition)";
        }
        return "Miogram Supporter";
    }
}
