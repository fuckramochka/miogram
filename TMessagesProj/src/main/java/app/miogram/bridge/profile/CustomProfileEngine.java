package app.miogram.bridge.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * Built-in Native Custom Profile Engine for Miogram:
 * Integrates CPB (Custom Profile Banner, Mesh Gradients, Bubble Sheets, Workshop)
 * directly into the client as a native first-class system feature.
 */
public class CustomProfileEngine {

    private static volatile boolean isLoaded = false;
    private static volatile boolean isInitializing = false;
    private static Class<?> cpbNativeClass = null;
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    public static synchronized void init(Context context) {
        if (isLoaded || isInitializing) return;
        isInitializing = true;

        if (context == null) context = ApplicationLoader.applicationContext;
        if (context == null) {
            isInitializing = false;
            return;
        }

        final Context appCtx = context.getApplicationContext() != null ? context.getApplicationContext() : context;

        new Thread(() -> {
            try {
                File baseDir = new File(appCtx.getFilesDir(), "cpb_native");
                if (!baseDir.exists()) baseDir.mkdirs();

                File dexFile = new File(baseDir, "cpb_core.dex");
                File optDir = new File(baseDir, "opt");
                if (!optDir.exists()) optDir.mkdirs();

                // Extract asset if dexFile does not exist or size is outdated
                boolean needExtract = !dexFile.exists() || dexFile.length() < 100000;
                if (needExtract) {
                    try (InputStream is = appCtx.getAssets().open("cpb_core.bin");
                         FileOutputStream fos = new FileOutputStream(dexFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                }

                DexClassLoader classLoader = new DexClassLoader(
                        dexFile.getAbsolutePath(),
                        optDir.getAbsolutePath(),
                        null,
                        appCtx.getClassLoader()
                );

                cpbNativeClass = classLoader.loadClass("cpb.CpbNative");

                for (Method m : cpbNativeClass.getDeclaredMethods()) {
                    m.setAccessible(true);
                    methodCache.put(m.getName(), m);
                }

                Method loadMethod = methodCache.get("load");
                if (loadMethod != null) {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("__cpb_build_sha", "ef53cd2e64ed2590d750186a851bd4f7343a49f722792306ff48189cca1d8493");
                    
                    // Load any saved legacy keys from shared preferences
                    try {
                        android.content.SharedPreferences prefs = appCtx.getSharedPreferences("plugin_settings_custom_profile", Context.MODE_PRIVATE);
                        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                            if (entry.getValue() != null) {
                                config.put(entry.getKey(), entry.getValue());
                            }
                        }
                    } catch (Throwable ignore) {}

                    loadMethod.invoke(null, appCtx, config);

                    Method diagBuild = methodCache.get("diagBuild");
                    if (diagBuild != null) {
                        try {
                            diagBuild.invoke(null, "1.8.1", "e1c352144a748eb91be2513e4164063e2179d3b3c5c74c93d97dd162386583f3");
                        } catch (Throwable ignore) {}
                    }

                    isLoaded = true;
                    FileLog.d("CustomProfileEngine: successfully loaded native CPB core");
                }
            } catch (Throwable t) {
                FileLog.e("CustomProfileEngine: failed to load CPB core", t);
            } finally {
                isInitializing = false;
            }
        }).start();
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static void openEditor() {
        invokeOnMainThread("openEditor");
    }

    public static void openBubbleSheet() {
        invokeOnMainThread("openBubbleSheet");
    }

    public static void openThanks() {
        invokeOnMainThread("openThanks");
    }

    public static void openLog() {
        invokeOnMainThread("openLog");
    }

    public static void openEgg() {
        invokeOnMainThread("openEgg");
    }

    public static void pingServers() {
        invokeOnMainThread("pingServers");
    }

    public static void checkUpdate() {
        invokeOnMainThread("checkUpdate");
    }

    public static void unskipRelease() {
        invokeOnMainThread("unskipRelease");
    }

    public static boolean isDevMode() {
        if (!isLoaded || cpbNativeClass == null) return false;
        try {
            Method m = methodCache.get("devMode");
            if (m != null) {
                Object res = m.invoke(null);
                if (res instanceof Boolean) return (Boolean) res;
            }
        } catch (Throwable ignore) {}
        return false;
    }

    public static String getLangMode() {
        if (!isLoaded || cpbNativeClass == null) return "auto";
        try {
            Method m = methodCache.get("langMode");
            if (m != null) {
                Object res = m.invoke(null);
                if (res != null) return res.toString();
            }
        } catch (Throwable ignore) {}
        return "auto";
    }

    public static void setLangMode(String mode) {
        if (!isLoaded || cpbNativeClass == null) return;
        try {
            Method m = methodCache.get("setLangMode");
            if (m != null) {
                m.invoke(null, mode);
            }
        } catch (Throwable ignore) {}
    }

    public static boolean flagOf(String key, boolean def) {
        if (!isLoaded || cpbNativeClass == null) return def;
        try {
            Method m = methodCache.get("flagOf");
            if (m != null) {
                Object res = m.invoke(null, key);
                if (res instanceof Boolean) return (Boolean) res;
                if (res != null) return Boolean.parseBoolean(res.toString());
            }
        } catch (Throwable ignore) {}
        return def;
    }

    public static void flagSet(String key, boolean value) {
        if (!isLoaded || cpbNativeClass == null) return;
        try {
            Method m = methodCache.get("flagSet");
            if (m != null) {
                m.invoke(null, key, value);
            }
        } catch (Throwable ignore) {}
    }

    private static void invokeOnMainThread(String methodName) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isLoaded || cpbNativeClass == null) {
                init(ApplicationLoader.applicationContext);
                return;
            }
            try {
                Method m = methodCache.get(methodName);
                if (m != null) {
                    m.invoke(null);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }
}
