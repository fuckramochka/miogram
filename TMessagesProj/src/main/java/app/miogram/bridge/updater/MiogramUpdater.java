package app.miogram.bridge.updater;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.miogram.bridge.ui.MiogramUpdateBottomSheet;

/**
 * Robust in-app updater for Miogram:
 * - Checks GitHub Releases API
 * - Semantic version and build code comparison
 * - Automatic background check every 5 minutes and on launch
 * - Compact anime reaction dialog (Happy on update / Sad on latest)
 */
public class MiogramUpdater {

    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/fuckramochka/miogram/releases/latest";
    private static final String PREFS_NAME = "miogram_updater_prefs";
    private static final String KEY_LAST_SEEN_TAG = "last_seen_tag";

    private static ScheduledExecutorService scheduler;
    private static volatile boolean autoUpdateStarted = false;

    /**
     * Initializes background update checking:
     * - Initial check 5 seconds after startup
     * - Periodic check every 5 minutes
     */
    public static synchronized void initAutoUpdate(Context context) {
        if (autoUpdateStarted) return;
        autoUpdateStarted = true;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                performBackgroundCheck();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, 5, 300, TimeUnit.SECONDS);
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
            Toast.makeText(fragment.getParentActivity(), "Перевірка оновлень Miogram...", Toast.LENGTH_SHORT).show();
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
                    String tag = json.optString("tag_name", "v12.10.0");
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
                    boolean isNewer = isNewerVersion(currentVersion, finalVersion, tag);

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

    public static boolean isNewerVersion(String currentVersion, String remoteVersion, String remoteTag) {
        if (remoteVersion == null || remoteVersion.isEmpty()) return false;

        String c = currentVersion != null ? currentVersion.replace("v", "").replace("V", "").trim() : "";
        String r = remoteVersion.trim();
        if (c.equalsIgnoreCase(r)) return false;

        String[] cParts = c.split("[.-]");
        String[] rParts = r.split("[.-]");
        int len = Math.max(cParts.length, rParts.length);
        for (int i = 0; i < len; i++) {
            int cVal = 0;
            int rVal = 0;
            if (i < cParts.length) {
                try { cVal = Integer.parseInt(cParts[i].replaceAll("\\D+", "")); } catch (Exception ignored) {}
            }
            if (i < rParts.length) {
                try { rVal = Integer.parseInt(rParts[i].replaceAll("\\D+", "")); } catch (Exception ignored) {}
            }
            if (rVal > cVal) return true;
            if (rVal < cVal) return false;
        }
        return false;
    }
}
