package app.miogram.bridge.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;
import app.miogram.bridge.MiogramLocale;

/**
 * Built-in Native Custom Profile Engine for Miogram:
 * Integrates CPB (Custom Profile Banner, Mesh Gradients, Bubble Sheets, Workshop)
 * directly into the client as a native first-class system feature.
 *
 * Lifecycle contract:
 *  - init() is idempotent: NOT_LOADED -> LOADING -> LOADED / FAILED.
 *  - Actions requested before LOADED are queued and replayed on the main
 *    thread once the core finishes loading (no fixed-delay races).
 *  - The extracted DEX is version-stamped against the bundled asset, so an
 *    app update always ships a fresh core instead of a stale cache.
 */
public class CustomProfileEngine {

    private static final String BUILD_SHA = "ef53cd2e64ed2590d750186a851bd4f7343a49f722792306ff48189cca1d8493";
    private static final String DIAG_VERSION = "1.8.1";
    private static final String DIAG_SHA = "e1c352144a748eb91be2513e4164063e2179d3b3c5c74c93d97dd162386583f3";

    private static final int STATE_NONE = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;
    private static final int STATE_FAILED = 3;

    private static volatile int loadState = STATE_NONE;
    private static Class<?> cpbNativeClass = null;
    private static volatile long lastFailedAt = 0L;
    private static final long RETRY_COOLDOWN_MS = 4000L;
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final List<String> pendingActions = new ArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        synchronized (CustomProfileEngine.class) {
            loadState = STATE_LOADED;
            return;
        }
    }

        if (context == null) context = ApplicationLoader.applicationContext;
        if (context == null) {
            synchronized (CustomProfileEngine.class) { loadState = STATE_NONE; }
            return;
        }

        final Context appCtx = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        new Thread(() -> {
            boolean ok = false;
            try {
                File baseDir = new File(appCtx.getFilesDir(), "cpb_native");
                if (!baseDir.exists()) baseDir.mkdirs();

                File dexFile = new File(baseDir, "cpb_core.dex");
                File optDir = new File(baseDir, "opt");
                if (!optDir.exists()) optDir.mkdirs();
                File versionStamp = new File(baseDir, "cpb_core.version");

                // Re-extract whenever the bundled asset changes (size + build sha stamp),
                // not just when the extracted file is missing.
                long assetSize = -1L;
                try (InputStream is = appCtx.getAssets().openFd("cpb_core.bin").createInputStream()) {
                    assetSize = is.available();
                } catch (Throwable ignore) {
                    try (InputStream is = appCtx.getAssets().open("cpb_core.bin")) {
                        assetSize = is.available();
                    } catch (Throwable ignored) {
                    }
                }
                String stamp = BUILD_SHA + ":" + assetSize;
                String oldStamp = null;
                try {
                    byte[] b = new byte[(int) versionStamp.length()];
                    try (InputStream fis = new java.io.FileInputStream(versionStamp)) {
                        fis.read(b);
                    }
                    oldStamp = new String(b, "UTF-8").trim();
                } catch (Throwable ignore) {
                }

                boolean needExtract = !dexFile.exists() || dexFile.length() < 100000 || !stamp.equals(oldStamp);
                if (needExtract) {
                    File tmp = new File(baseDir, "cpb_core.dex.tmp");
                    try (InputStream is = appCtx.getAssets().open("cpb_core.bin");
                         FileOutputStream fos = new FileOutputStream(tmp)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.getFD().sync();
                    }
                    if (tmp.length() < 100000) {
                        throw new IllegalStateException("cpb_core.bin asset is missing or truncated");
                    }
                    if (dexFile.exists() && !dexFile.delete()) {
                        FileLog.e("CustomProfileEngine: cannot remove stale cpb_core.dex");
                    }
                    if (!tmp.renameTo(dexFile)) {
                        throw new IllegalStateException("cannot move extracted cpb_core.dex into place");
                    }
                    dexFile.setReadOnly();
                    try (FileOutputStream fos = new FileOutputStream(versionStamp)) {
                        fos.write(stamp.getBytes("UTF-8"));
                    }
                    // Old odex artifacts must not survive a core swap.
                    for (File f : optDir.listFiles() != null ? optDir.listFiles() : new File[0]) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }

                DexClassLoader classLoader = new DexClassLoader(
                        dexFile.getAbsolutePath(),
                        optDir.getAbsolutePath(),
                        null,
                        appCtx.getClassLoader()
                );

                cpbNativeClass = classLoader.loadClass("cpb.CpbNative");
                methodCache.clear();
                for (Method m : cpbNativeClass.getDeclaredMethods()) {
                    m.setAccessible(true);
                    // Overloads collide in the name-keyed cache; keep the first no-arg variant findable.
                    methodCache.putIfAbsent(m.getName(), m);
                }

                try {
                    app.exteraless.plugins.xposed.XposedHooks.ensureReady();
                } catch (Throwable ignore) {
                }

                Method loadMethod = pickMethod("load");
                if (loadMethod == null) {
                    throw new NoSuchMethodException("cpb.CpbNative.load(Context, Map) not found in bundled core");
                }

                HashMap<String, Object> config = new HashMap<>();
                config.put("__cpb_build_sha", BUILD_SHA);

                // Surface any saved legacy keys through the config map.
                try {
                    SharedPreferences prefs = appCtx.getSharedPreferences("plugin_settings_custom_profile", Context.MODE_PRIVATE);
                    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                        if (entry.getValue() != null) {
                            config.put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Throwable ignore) {
                }

                loadMethod.invoke(null, appCtx, config);

                Method diagBuild = pickMethod("diagBuild");
                if (diagBuild != null) {
                    try {
                        diagBuild.invoke(null, DIAG_VERSION, DIAG_SHA);
                    } catch (Throwable ignore) {
                    }
                }

                ok = true;
                FileLog.d("CustomProfileEngine: successfully loaded native CPB core");
            } catch (Throwable t) {
                FileLog.e("CustomProfileEngine: failed to load CPB core", t);
            } finally {
                List<String> toRun = null;
                synchronized (CustomProfileEngine.class) {
                    loadState = ok ? STATE_LOADED : STATE_FAILED;
                    if (!ok) lastFailedAt = System.currentTimeMillis();
                    if (ok && !pendingActions.isEmpty()) {
                        toRun = new ArrayList<>(pendingActions);
                        pendingActions.clear();
                    } else if (!ok) {
                        final int dropped = pendingActions.size();
                        pendingActions.clear();
                        if (dropped > 0) {
                            mainHandler.post(() -> notifyFailure());
                        }
                    }
                }
                if (toRun != null) {
                    final List<String> actions = toRun;
                    mainHandler.post(() -> {
                        for (String a : actions) invokeLoaded(a);
                    });
                }
                if (ok) {
                    flushListenersOnLoad();
                }
            }
        }).start();
    }

    private static Method pickMethod(String name) {
        Method m = methodCache.get(name);
        if (m == null || cpbNativeClass == null) return null;
        // Prefer a no-arg overload when several share the name.
        for (Method cand : cpbNativeClass.getDeclaredMethods()) {
            if (name.equals(cand.getName()) && cand.getParameterCount() == 0) {
                return cand;
            }
        }
        return m;
    }

    private static void notifyFailure() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                Toast.makeText(ctx, MiogramLocale.get(
                        "Не вдалося завантажити ядро оформлення профілю",
                        "Не удалось загрузить ядро оформления профиля",
                        "Failed to load the profile decoration core"), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean isLoaded() {
        return loadState == STATE_LOADED;
    }

    public static void openEditor() {
        invoke("openEditor");
    }

    public static void openBubbleSheet() {
        invoke("openBubbleSheet");
    }

    public static void openThanks() {
        invoke("openThanks");
    }

    public static void openLog() {
        invoke("openLog");
    }

    public static void openEgg() {
        invoke("openEgg");
    }

    public static void pingServers() {
        invoke("pingServers");
    }

    public static void checkUpdate() {
        invoke("checkUpdate");
    }

    public static void unskipRelease() {
        invoke("unskipRelease");
    }

    /**
     * Queue-until-loaded dispatcher. Runs immediately when the core is up,
     * otherwise the action waits for init() and is replayed exactly once on
     * the main thread; on failure the user sees a toast instead of silence.
     */
    private static void invoke(String methodName) {
        synchronized (CustomProfileEngine.class) {
            if (loadState != STATE_LOADED) {
                pendingActions.add(methodName);
                if (loadState == STATE_NONE) {
                    init(ApplicationLoader.applicationContext);
                }
                return;
            }
        }
        mainHandler.post(() -> invokeLoaded(methodName));
    }

    private static Object getNativeInstance() {
        if (cpbNativeClass == null) return null;
        try {
            Method getM = methodCache.get("get");
            if (getM == null) {
                getM = cpbNativeClass.getDeclaredMethod("get");
                getM.setAccessible(true);
                methodCache.put("get", getM);
            }
            return getM.invoke(null);
        } catch (Throwable t) {
            FileLog.e("CustomProfileEngine: getNativeInstance failed", t);
        }
        return null;
    }

    private static Object invokeTarget(Method m, Object... args) throws Exception {
        if (m == null) return null;
        Object target = java.lang.reflect.Modifier.isStatic(m.getModifiers()) ? null : getNativeInstance();
        return m.invoke(target, args);
    }

    private static void invokeLoaded(String methodName) {
        if (loadState != STATE_LOADED || cpbNativeClass == null) return;
        try {
            Method m = pickMethod(methodName);
            if (m != null) {
                invokeTarget(m);
            } else {
                FileLog.e("CustomProfileEngine: no method " + methodName + " in CPB core");
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static boolean isDevMode() {
        return readNoArgFlag("devMode", false);
    }

    public static String getLangMode() {
        if (loadState != STATE_LOADED || cpbNativeClass == null) return "auto";
        try {
            Method m = pickMethod("langMode");
            if (m != null) {
                Object res = invokeTarget(m);
                if (res != null) return res.toString();
            }
        } catch (Throwable ignore) {
        }
        return "auto";
    }

    public static void setLangMode(String mode) {
        if (loadState != STATE_LOADED || cpbNativeClass == null) return;
        try {
            Method m = pickMethod("setLangMode");
            if (m != null) {
                invokeTarget(m, mode);
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean flagOf(String key, boolean def) {
        if (loadState == STATE_LOADED && cpbNativeClass != null) {
            try {
                Method m = methodCache.get("flagOf");
                if (m != null) {
                    Object res = invokeTarget(m, key);
                    if (res instanceof Boolean) return (Boolean) res;
                    if (res != null) return Boolean.parseBoolean(res.toString());
                }
            } catch (Throwable ignore) {
            }
        }
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx != null) {
            SharedPreferences prefs = ctx.getSharedPreferences("plugin_settings_custom_profile", Context.MODE_PRIVATE);
            return prefs.getBoolean(key, def);
        }
        return def;
    }

    private static boolean readNoArgFlag(String methodName, boolean def) {
        if (loadState != STATE_LOADED || cpbNativeClass == null) return def;
        try {
            Method m = pickMethod(methodName);
            if (m != null) {
                Object res = invokeTarget(m);
                if (res instanceof Boolean) return (Boolean) res;
                if (res != null) return Boolean.parseBoolean(res.toString());
            }
        } catch (Throwable ignore) {
        }
        return def;
    }

    public static void flagSet(String key, boolean value) {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx != null) {
            SharedPreferences prefs = ctx.getSharedPreferences("plugin_settings_custom_profile", Context.MODE_PRIVATE);
            prefs.edit().putBoolean(key, value).apply();
        }
        if (loadState == STATE_LOADED && cpbNativeClass != null) {
            try {
                Method m = methodCache.get("flagSet");
                if (m != null) {
                    invokeTarget(m, key, value);
                }
            } catch (Throwable ignore) {
            }
        }
    }

    /** Notifies waiting UI (e.g. dev-mode rows) once the core is up. */
    public static void whenLoaded(Runnable action) {
        if (action == null) return;
        if (loadState == STATE_LOADED) {
            AndroidUtilities.runOnUIThread(action);
            return;
        }
        synchronized (CustomProfileEngine.class) {
            if (loadState != STATE_LOADED) {
                loadListeners.add(action);
                if (loadState == STATE_NONE) init(ApplicationLoader.applicationContext);
                return;
            }
        }
        AndroidUtilities.runOnUIThread(action);
    }

    private static final List<Runnable> loadListeners = new ArrayList<>();

    private static void flushListenersOnLoad() {
        List<Runnable> toRun;
        synchronized (CustomProfileEngine.class) {
            if (loadState != STATE_LOADED || loadListeners.isEmpty()) return;
            toRun = new ArrayList<>(loadListeners);
            loadListeners.clear();
        }
        AndroidUtilities.runOnUIThread(() -> {
            for (Runnable r : toRun) {
                try {
                    r.run();
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            }
        });
    }
}
