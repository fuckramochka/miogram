package app.miogram.bridge;

import org.telegram.messenger.LocaleController;
import java.util.Locale;

/**
 * Clean and lightweight dynamic localization helper for Miogram components.
 * Automatically adapts to user's Telegram language settings (Ukrainian, Russian, English/Default).
 */
public class MiogramLocale {

    public static String get(String uk, String ru, String en) {
        try {
            Locale locale = LocaleController.getInstance().getCurrentLocale();
            if (locale == null) {
                locale = Locale.getDefault();
            }
            String lang = locale.getLanguage().toLowerCase();
            if (lang.startsWith("uk")) {
                return uk;
            } else if (lang.startsWith("ru") || lang.startsWith("be") || lang.startsWith("kk")) {
                return ru;
            }
        } catch (Throwable ignored) {}
        return en;
    }

    public static String format(String uk, String ru, String en, Object... args) {
        String template = get(uk, ru, en);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}
