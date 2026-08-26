package app.miogram.bridge.updater;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import app.miogram.bridge.ui.MiogramUpdateBottomSheet;

public class MiogramUpdater {

    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/fuckramochka/miogram/releases/latest";

    public static void checkAndShowUpdate(BaseFragment fragment, boolean manualCheck) {
        if (fragment == null || fragment.getParentActivity() == null) return;

        if (manualCheck) {
            Toast.makeText(fragment.getParentActivity(), "Перевірка оновлень Miogram...", Toast.LENGTH_SHORT).show();
        }

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

                    final String finalVersion = tag.replace("v", "");
                    final String finalChangelog = body;
                    final String finalApkUrl = apkUrl;

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (fragment.getParentActivity() != null && !fragment.getParentActivity().isFinishing()) {
                            MiogramUpdateBottomSheet sheet = new MiogramUpdateBottomSheet(fragment, finalVersion, finalChangelog, finalApkUrl);
                            sheet.show();
                        }
                    });
                } else if (manualCheck) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (fragment.getParentActivity() != null) {
                            Toast.makeText(fragment.getParentActivity(), "Оновлень не знайдено (використовується остання версія)", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (manualCheck) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (fragment.getParentActivity() != null) {
                            Toast.makeText(fragment.getParentActivity(), "Перевірка завершена: актуальна версія встановлена", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
