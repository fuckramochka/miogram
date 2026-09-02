package app.miogram.bridge.updater;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ui.MiogramUpdateBottomSheet;

/**
 * Updater service for Miogram.
 * Automatically checks GitHub releases for updates and prompts the user.
 */
public class MiogramUpdater {

    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/fuckramochka/miogram/releases/latest";
    private static final String PREFS_NAME = "miogram_updater_prefs";
    private static final String KEY_LAST_SEEN_TAG = "last_seen_tag";

    private static final String KEY_LAST_CHECK_TIME = "last_check_timestamp";
    private static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L; // 24 hours cooldown
    private static volatile boolean autoUpdateStarted = false;

    /**
     * Safe, battery-friendly launch check (at most once every 24 hours).
     */
    public static synchronized void initAutoUpdate(Context context) {
        if (autoUpdateStarted) return;
        autoUpdateStarted = true;

        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context ctx = ApplicationLoader.applicationContext != null ? ApplicationLoader.applicationContext : context;
                if (ctx == null) return;
                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                long lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L);
                long now = System.currentTimeMillis();
                if (now - lastCheck < CHECK_INTERVAL_MS) {
                    return; // Skip if checked within the last 24 hours
                }
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply();
                performBackgroundCheck();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, 10000L); // Delayed by 10s so it doesn't block app launch
    }

    /**
     * Checks for updates immediately upon entry/unlock.
     * Silent if on the latest version; presents update bottom sheet if a newer version is found.
     */
    public static void checkOnEntry(Context context) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                performBackgroundCheck();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, 1500L); // 1.5s post-unlock delay so it doesn't stutter unlock animations
    }

    private static void performBackgroundCheck() {
        LaunchActivity act = LaunchActivity.instance;
        if (act == null || act.isFinishing()) return;

        BaseFragment fragment = act.getSafeLastFragment();
        if (fragment == null) return;

        fetchLatestRelease((hasUpdate, version, changelog, apkUrl) -> {
            if (hasUpdate) {
                SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String lastSeen = prefs.getString(KEY_LAST_SEEN_TAG, "");
                if (version != null && version.equalsIgnoreCase(lastSeen)) {
                    return; // Already notified the user about this specific release
                }
                prefs.edit().putString(KEY_LAST_SEEN_TAG, version).apply();

                new Handler(Looper.getMainLooper()).post(() -> {
                    LaunchActivity currentAct = LaunchActivity.instance;
                    if (currentAct != null && !currentAct.isFinishing()) {
                        BaseFragment currentFrag = currentAct.getSafeLastFragment();
                        if (currentFrag != null) {
                            MiogramUpdateBottomSheet sheet = new MiogramUpdateBottomSheet(currentFrag, true, version, changelog, apkUrl);
                            sheet.show();
                        }
                    }
                });
            }
        });
    }

    public static void checkAndShowUpdate(BaseFragment fragment, boolean manualCheck) {
        if (fragment == null || fragment.getParentActivity() == null) return;

        if (manualCheck) {
            Toast.makeText(fragment.getParentActivity(), MiogramLocale.get("Перевірка оновлень Miogram...", "Проверка обновлений Miogram...", "Checking for Miogram updates..."), Toast.LENGTH_SHORT).show();
        }

        fetchLatestRelease((hasUpdate, version, changelog, apkUrl) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (fragment.getParentActivity() == null || fragment.getParentActivity().isFinishing()) {
                    return;
                }
                final String finalVer = (version != null && !version.isEmpty()) ? version : getCurrentAppVersion();
                MiogramUpdateBottomSheet sheet = new MiogramUpdateBottomSheet(fragment, hasUpdate, finalVer, changelog, apkUrl);
                sheet.show();
            });
        });
    }

    private interface UpdateCallback {
        void onResult(boolean hasUpdate, String version, String changelog, String apkUrl);
    }

    private static void fetchLatestRelease(UpdateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_API_LATEST);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    String tag = json.optString("tag_name", "v12.10.1");
                    String body = json.optString("body", "");
                    String apkUrl = "";

                    JSONArray assets = json.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            String name = asset.optString("name", "");
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", "");
                                break;
                            }
                        }
                    }

                    final String finalVersion = tag.replace("v", "").replace("V", "").trim();
                    final String currentVersion = getCurrentAppVersion();
                    boolean isNewer = isNewerVersion(currentVersion, finalVersion, tag, body);

                    callback.onResult(isNewer, finalVersion, body, apkUrl);
                } else {
                    callback.onResult(false, getCurrentAppVersion(), null, null);
                }
            } catch (Exception e) {
                FileLog.e(e);
                callback.onResult(false, getCurrentAppVersion(), null, null);
            }
        }).start();
    }

    public static String getCurrentAppVersion() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : BuildVars.BUILD_VERSION_STRING;
        } catch (Exception e) {
            return BuildVars.BUILD_VERSION_STRING != null ? BuildVars.BUILD_VERSION_STRING : "12.10.1";
        }
    }

    public static boolean isNewerVersion(String currentVersion, String remoteVersion, String remoteTag, String changelog) {
        if (TextUtils.isEmpty(remoteVersion)) return false;

        String c = currentVersion != null ? currentVersion.replace("v", "").replace("V", "").trim() : "";
        String r = remoteVersion.trim();

        // 1. Exact match or prefix match
        if (c.equalsIgnoreCase(r)) return false;
        if (!c.isEmpty() && !r.isEmpty()) {
            if (c.startsWith(r) || r.startsWith(c)) {
                // If it's the exact same base release with a commit hash (e.g. 12.10.1-83b6b68 vs 12.10.1), it is up to date
                return false;
            }
        }

        // 2. If changelog or remote tag contains commit hash that matches installed app
        if (!TextUtils.isEmpty(changelog) && !TextUtils.isEmpty(c)) {
            String[] parts = c.split("-");
            if (parts.length > 1) {
                String currentHash = parts[parts.length - 1].trim();
                if (currentHash.length() >= 4 && changelog.contains(currentHash)) {
                    return false; // Same commit already running!
                }
            }
        }

        // 3. Compare numeric version components
        String[] cParts = c.split("[^0-9]+");
        String[] rParts = r.split("[^0-9]+");

        int len = Math.max(cParts.length, rParts.length);
        for (int i = 0; i < len; i++) {
            int cVal = 0;
            int rVal = 0;
            if (i < cParts.length && !cParts[i].isEmpty()) {
                try { cVal = Integer.parseInt(cParts[i]); } catch (Exception ignored) {}
            }
            if (i < rParts.length && !rParts[i].isEmpty()) {
                try { rVal = Integer.parseInt(rParts[i]); } catch (Exception ignored) {}
            }
            if (rVal > cVal) return true;
            if (rVal < cVal) return false;
        }
        return false;
    }
}
