package app.lumigram.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import app.lumigram.api.LumiModule;
import app.lumigram.api.ModuleHost;
import app.lumigram.api.ModuleInfo;
import dalvik.system.DexClassLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Менеджер нативних модулів LumiGram.
 *
 * <p>Життєвий цикл: catalog → download (.part, атомарно) → sha256 (+підпис
 * перед релізом) → install (розпаковка module.json) → load (DexClassLoader)
 * → enable (onEnable → registerScreen) / disable / uninstall.
 *
 * <p>Синглтон на аплікаційному контексті. init() кликати раз з
 * ApplicationLoader.onCreate поруч з іншими Config.init().
 */
public final class ModuleManager {

    /** Версія ядра: модулі з більшим minCore відхиляються. Росте при зміні api. */
    public static final int CORE_VERSION = 1;

    private static final String PREFS = "lumigram_modules";
    private static final String KEY_ENABLED_PREFIX = "enabled_";
    private static final String KEY_VERSION_PREFIX = "version_";
    private static final String DIR_NAME = "lumi_modules";
    private static final String MANIFEST_NAME = "module.json";

    public interface DownloadListener {
        void onProgress(long done, long total);
        void onFinished(boolean success, boolean canceled);
    }

    private static volatile ModuleManager instance;

    private final Context appContext;
    private final Map<String, Loaded> loaded = new LinkedHashMap<>();
    private volatile okhttp3.Call downloadCall;

    private static final class Loaded {
        ModuleInfo info;
        LumiModule module;
        DexClassLoader loader;
        File dir;
    }

    private ModuleManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static ModuleManager getInstance() {
        return instance;
    }

    /** Разовий init з ApplicationLoader; після — loadEnabled(). */
    public static synchronized ModuleManager init(Context context) {
        if (instance == null) {
            instance = new ModuleManager(context);
            instance.loadEnabled();
        }
        return instance;
    }

    private SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private File modulesDir() {
        File dir = new File(appContext.getFilesDir(), DIR_NAME);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File dexOptDir() {
        File dir = new File(appContext.getCodeCacheDir(), "lumi_dex");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    // ---------- стан ----------

    public boolean isInstalled(String id) {
        return new File(modulesDir(), id + File.separator + MANIFEST_NAME).isFile();
    }

    public boolean isEnabled(String id) {
        return prefs().getBoolean(KEY_ENABLED_PREFIX + id, false);
    }

    public String installedVersion(String id) {
        return prefs().getString(KEY_VERSION_PREFIX + id, null);
    }

    public List<ModuleInfo> installed() {
        List<ModuleInfo> out = new ArrayList<>();
        File[] dirs = modulesDir().listFiles();
        if (dirs == null) {
            return out;
        }
        for (File d : dirs) {
            try {
                File manifest = new File(d, MANIFEST_NAME);
                if (!manifest.isFile()) {
                    continue;
                }
                String text = readText(manifest);
                ModuleInfo info = ModuleInfo.parse(new JSONObject(text));
                if (info != null) {
                    out.add(info);
                }
            } catch (Exception e) {
                FileLog.e("LumiModules: read manifest failed", e);
            }
        }
        return out;
    }

    // ---------- install / uninstall ----------

    /**
     * Встановити .lumi-архів: sha → підпис → module.json → minCore → розкладка.
     * Повертає ModuleInfo або null з логом причини.
     */
    public ModuleInfo install(File archive, String expectedSha256, String signature) {
        if (archive == null || !archive.isFile()) {
            return null;
        }
        if (!ModuleVerifier.verifySha(archive, expectedSha256)) {
            FileLog.e("LumiModules: sha mismatch");
            return null;
        }
        if (!ModuleVerifier.verifySignature(archive, signature)) {
            FileLog.e("LumiModules: signature rejected");
            return null;
        }
        ModuleInfo info = readManifestFromZip(archive);
        if (info == null) {
            FileLog.e("LumiModules: bad module.json");
            return null;
        }
        if (info.minCore > CORE_VERSION) {
            FileLog.e("LumiModules: module " + info.id + " needs core " + info.minCore);
            return null;
        }
        File target = new File(modulesDir(), info.id + File.separator + info.id + ".lumi");
        try {
            target.getParentFile().mkdirs();
            copyFile(archive, target);
            writeText(new File(target.getParentFile(), MANIFEST_NAME), manifestJson(info));
        } catch (Exception e) {
            FileLog.e("LumiModules: install failed", e);
            return null;
        }
        prefs().edit()
                .putString(KEY_VERSION_PREFIX + info.id, info.version)
                .putBoolean(KEY_ENABLED_PREFIX + info.id, true)
                .apply();
        loadOne(info.id);
        return info;
    }

    public void uninstall(String id) {
        setEnabled(id, false);
        deleteRecursive(new File(modulesDir(), id));
        prefs().edit()
                .remove(KEY_ENABLED_PREFIX + id)
                .remove(KEY_VERSION_PREFIX + id)
                .apply();
    }

    // ---------- enable / load ----------

    public void setEnabled(String id, boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED_PREFIX + id, enabled).apply();
        if (enabled) {
            loadOne(id);
        } else {
            unloadOne(id);
        }
    }

    private void loadEnabled() {
        for (ModuleInfo info : installed()) {
            if (isEnabled(info.id)) {
                loadOne(info.id);
            }
        }
    }

    private void loadOne(String id) {
        try {
            File dir = new File(modulesDir(), id);
            File zip = new File(dir, id + ".lumi");
            File manifest = new File(dir, MANIFEST_NAME);
            if (!zip.isFile() || !manifest.isFile()) {
                return;
            }
            ModuleInfo info = ModuleInfo.parse(new JSONObject(readText(manifest)));
            if (info == null || info.minCore > CORE_VERSION) {
                return;
            }
            DexClassLoader loader = new DexClassLoader(
                    zip.getAbsolutePath(),
                    dexOptDir().getAbsolutePath(),
                    null,
                    appContext.getClassLoader());
            Class<?> entry = loader.loadClass(info.entry);
            Object raw = entry.getDeclaredConstructor().newInstance();
            if (!(raw instanceof LumiModule)) {
                FileLog.e("LumiModules: entry does not implement LumiModule: " + info.entry);
                return;
            }
            LumiModule module = (LumiModule) raw;
            ModuleHost host = ModuleRegistry.hostFor(appContext, id);
            module.onEnable(host, appContext);
            Loaded l = new Loaded();
            l.info = info;
            l.module = module;
            l.loader = loader;
            l.dir = dir;
            synchronized (loaded) {
                loaded.put(id, l);
            }
        } catch (Exception e) {
            FileLog.e("LumiModules: load failed: " + id, e);
        }
    }

    private void unloadOne(String id) {
        Loaded l;
        synchronized (loaded) {
            l = loaded.remove(id);
        }
        if (l != null) {
            try {
                l.module.onDisable();
            } catch (Exception e) {
                FileLog.e("LumiModules: onDisable failed", e);
            }
        }
        ModuleRegistry.unregisterModule(id);
    }

    // ---------- download (атомарно, як в апдейтері) ----------

    public void downloadAndInstall(ModulesCatalog.Entry entry, DownloadListener listener) {
        if (downloadCall != null) {
            return;
        }
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        okhttp3.Call call = http.newCall(new Request.Builder().url(entry.url)
                .header("User-Agent", "lumigram").build());
        downloadCall = call;
        final String sha = entry.sha256;
        final String sig = entry.signature;
        new Thread(() -> {
            boolean ok = false;
            File cache = new File(appContext.getCacheDir(), "lumi_modules");
            cache.mkdirs();
            File part = new File(cache, entry.id + ".lumi.part");
            File done = new File(cache, entry.id + ".lumi");
            part.delete();
            done.delete();
            try (Response response = call.execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    throw new IllegalStateException("HTTP " + response.code());
                }
                long total = body.contentLength() > 0 ? body.contentLength() : entry.size;
                try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(part)) {
                    byte[] buf = new byte[64 * 1024];
                    long wrote = 0;
                    int lastPercent = -1;
                    int read;
                    while ((read = in.read(buf)) > 0) {
                        if (call.isCanceled()) {
                            break;
                        }
                        out.write(buf, 0, read);
                        wrote += read;
                        if (total > 0) {
                            int p = (int) (wrote * 100 / total);
                            if (p != lastPercent) {
                                lastPercent = p;
                                final long w = wrote;
                                AndroidUtilities.runOnUIThread(() -> listener.onProgress(w, total));
                            }
                        }
                    }
                    out.flush();
                    if (!call.isCanceled() && part.length() > 64 * 1024
                            && (total <= 0 || part.length() >= total)
                            && part.renameTo(done)) {
                        ok = true;
                    }
                }
            } catch (Exception e) {
                if (!call.isCanceled()) {
                    FileLog.e("LumiModules: download failed", e);
                }
            }
            final boolean success = ok;
            AndroidUtilities.runOnUIThread(() -> {
                downloadCall = null;
                if (success) {
                    ModuleInfo info = install(done, sha, sig);
                    done.delete();
                    listener.onFinished(info != null, false);
                } else {
                    part.delete();
                    done.delete();
                    listener.onFinished(false, call.isCanceled());
                }
            });
        }, "lumi-modules-download").start();
    }

    public void cancelDownload() {
        okhttp3.Call c = downloadCall;
        if (c != null) {
            c.cancel();
        }
    }

    // ---------- resources модуля (res.apk всередині .lumi — v2) ----------

    public Resources resourcesFor(String id) {
        try {
            File zip = new File(new File(modulesDir(), id), id + ".lumi");
            AssetManager assets = AssetManager.class.newInstance();
            assets.getClass().getMethod("addAssetPath", String.class).invoke(assets, zip.getAbsolutePath());
            Resources base = appContext.getResources();
            return new Resources(assets, base.getDisplayMetrics(), base.getConfiguration());
        } catch (Exception e) {
            FileLog.e("LumiModules: resources failed", e);
            return null;
        }
    }

    // ---------- io ----------

    private ModuleInfo readManifestFromZip(File zip) {
        try (ZipFile zf = new ZipFile(zip)) {
            ZipEntry e = zf.getEntry(MANIFEST_NAME);
            if (e == null) {
                return null;
            }
            try (InputStream in = zf.getInputStream(e)) {
                byte[] data = readAll(in);
                return ModuleInfo.parse(new JSONObject(new String(data, "UTF-8")));
            }
        } catch (Exception ex) {
            FileLog.e("LumiModules: manifest read failed", ex);
            return null;
        }
    }

    private static String manifestJson(ModuleInfo info) {
        return info.toJson().toString();
    }

    private static String readText(File f) throws Exception {
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            return new String(readAll(in), "UTF-8");
        }
    }

    private static void writeText(File f, String text) throws Exception {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(text.getBytes("UTF-8"));
        }
    }

    private static void copyFile(File from, File to) throws Exception {
        try (InputStream in = new BufferedInputStream(new FileInputStream(from));
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[64 * 1024];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[32 * 1024];
        int read;
        while ((read = in.read(buf)) > 0) {
            bos.write(buf, 0, read);
        }
        return bos.toByteArray();
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    deleteRecursive(k);
                }
            }
        }
        f.delete();
    }
}
