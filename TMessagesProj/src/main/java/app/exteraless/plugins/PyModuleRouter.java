package app.exteraless.plugins;

import android.app.Activity;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.regex.Pattern;

import app.miogram.bridge.MiogramLocale;

/**
 * Automatic router for {@code .py} files shared by two subsystems:
 * exteraGram plugins and Heroku/Hikka userbot modules.
 *
 * <p>Detection is static (regex over the file head, never executed):
 * exteraGram markers are {@code __id__}/{@code __permissions__}/
 * {@code create_settings} (see {@code extera_utils/metadata_parser.py});
 * Heroku/Hikka markers are {@code loader.Module}, {@code @loader.command},
 * {@code strings = {...}}, {@code def xxxcmd} (see Hikka {@code loader.py}).
 *
 * <p>Routing rule: HEROKU kind installs as a userbot module immediately;
 * anything else flows into the normal exteraGram consent sheet, and if
 * exteraGram metadata parsing fails there, the file falls back to Heroku
 * instead of dying with an error. The reverse direction (exteraGram file
 * picked in the userbot screen) is routed back to the consent sheet.
 */
public final class PyModuleRouter {

    private PyModuleRouter() {
    }

    public enum Kind {
        EXTERAGRAM,
        HEROKU,
        UNKNOWN
    }

    private static final int MAX_SCAN_BYTES = 256 * 1024;

    private static final Pattern EXTERA_NAME = Pattern.compile("\\bextera[Gg]ram\\b|extera_utils|PluginPermissions");
    private static final Pattern HEROKU_IMPORT_LOADER = Pattern.compile("from\\s+[.\\w]*\\s*import\\s+loader\\b|import\\s+loader\\b");
    private static final Pattern HEROKU_STRINGS = Pattern.compile("strings\\s*=\\s*\\{");
    private static final Pattern HEROKU_CMD_DEF = Pattern.compile("def\\s+\\w+cmd\\s*\\(");

    public static Kind detectKind(File file) {
        String text = readHead(file);
        if (text.isEmpty()) return Kind.UNKNOWN;

        int extera = 0;
        if (text.contains("__id__")) extera += 2;
        if (text.contains("__permissions__")) extera += 2;
        if (text.contains("__sdk_version__") || text.contains("__app_version__")) extera += 2;
        if (text.contains("__name__")) extera += 1;
        if (text.contains("create_settings")) extera += 2;
        if (EXTERA_NAME.matcher(text).find()) extera += 2;

        int heroku = 0;
        if (text.contains("loader.Module")) heroku += 3;
        if (text.contains("@loader.command")) heroku += 3;
        if (text.contains("@loader.watcher") || text.contains("@loader.inline_handler")) heroku += 2;
        if (HEROKU_IMPORT_LOADER.matcher(text).find()) heroku += 2;
        if (HEROKU_STRINGS.matcher(text).find()) heroku += 2;
        if (HEROKU_CMD_DEF.matcher(text).find()) heroku += 2;
        if (text.contains("hikka")) heroku += 1;

        if (heroku > 0 && heroku >= extera) return Kind.HEROKU;
        if (extera > 0 && extera > heroku) return Kind.EXTERAGRAM;
        return Kind.UNKNOWN;
    }

    private static String readHead(File file) {
        if (file == null || !file.isFile()) return "";
        try (FileInputStream in = new FileInputStream(file)) {
            long len = Math.min(file.length(), MAX_SCAN_BYTES);
            if (len <= 0) return "";
            byte[] buf = new byte[(int) len];
            int read = 0;
            while (read < buf.length) {
                int n = in.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
            return new String(buf, 0, read, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Throwable t) {
            FileLog.e(t);
            return "";
        }
    }

    /**
     * Installs the file as a Heroku userbot module with user feedback.
     *
     * @return true if the install was attempted (caller must stop).
     */
    public static boolean installAsHerokuModule(Activity activity, File file) {
        boolean ok = false;
        String name = file != null ? file.getName() : "?";
        try {
            ok = app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().installModule(file);
        } catch (Throwable t) {
            FileLog.e(t);
        }
        final boolean result = ok;
        try {
            org.telegram.ui.ActionBar.BaseFragment fragment = LaunchActivity.getSafeLastFragment();
            if (fragment != null && fragment.getParentActivity() != null) {
                String text = result
                        ? MiogramLocale.get("Встановлено як модуль юзербота: ", "Установлен как модуль юзербота: ", "Installed as userbot module: ") + name
                        : MiogramLocale.get("Не вдалося встановити модуль юзербота: ", "Не удалось установить модуль юзербота: ", "Userbot module install failed: ") + name;
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, text).show();
            }
        } catch (Throwable ignore) {
        }
        return true;
    }

    /**
     * Fast path used by pickers: HEROKU files go straight to the userbot,
     * everything else returns false so the normal flow continues.
     */
    public static boolean routeToHerokuIfNeeded(Activity activity, File file) {
        try {
            if (file != null && detectKind(file) == Kind.HEROKU) {
                return installAsHerokuModule(activity, file);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return false;
    }

    /**
     * Fallback used when exteraGram metadata parsing failed: anything that is
     * not clearly an exteraGram plugin gets a second life as a Heroku module
     * instead of an install error.
     *
     * @return true if handled (caller must stop).
     */
    public static boolean fallbackToHerokuOnMetadataFailure(Activity activity, File file) {
        try {
            if (file != null && detectKind(file) != Kind.EXTERAGRAM) {
                return installAsHerokuModule(activity, file);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return false;
    }

    public static String kindLabel(Kind kind) {
        if (kind == Kind.HEROKU) return "heroku";
        if (kind == Kind.EXTERAGRAM) return "exteragram";
        return "unknown";
    }

    public static String debugName(File file) {
        String name = file != null ? file.getName().toLowerCase(Locale.ROOT) : "?";
        return name + " [" + kindLabel(detectKind(file)) + "]";
    }
}
