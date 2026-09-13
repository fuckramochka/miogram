package app.miogram.bridge.discord;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import app.miogram.bridge.MiogramLocale;

/**
 * Public Discord Presence & Status Bridge for Miogram.
 * Uses the open-source keyless Lanyard API (https://api.lanyard.rest).
 * Zero credentials, zero private keys, 100% public presence.
 */
public class MiogramDiscordManager {

    private static volatile MiogramDiscordManager instance;

    public static MiogramDiscordManager getInstance() {
        if (instance == null) {
            synchronized (MiogramDiscordManager.class) {
                if (instance == null) {
                    instance = new MiogramDiscordManager();
                }
            }
        }
        return instance;
    }

    public static class DiscordPresence {
        public String userId = "";
        public String username = "";
        public String globalName = "";
        public String avatarUrl = "";
        public String status = "offline"; // "online", "idle", "dnd", "offline"
        public String customStatus = "";
        public String activityName = "";
        public String activityDetails = "";
        public long lastUpdated = 0;

        public boolean isOnline() {
            return "online".equals(status) || "idle".equals(status) || "dnd".equals(status);
        }

        public String getDisplayName() {
            return !TextUtils.isEmpty(globalName) ? globalName : username;
        }

        public String getStatusText() {
            if ("online".equals(status)) return MiogramLocale.get("В мережі", "В сети", "Online");
            if ("idle".equals(status)) return MiogramLocale.get("Не на місці", "Не на месте", "Idle");
            if ("dnd".equals(status)) return MiogramLocale.get("Не турбувати", "Не беспокоить", "Do Not Disturb");
            return MiogramLocale.get("Не в мережі", "Не в сети", "Offline");
        }

        public int getStatusColor() {
            if ("online".equals(status)) return 0xFF23A55A;
            if ("idle".equals(status)) return 0xFFF0B232;
            if ("dnd".equals(status)) return 0xFFF23F43;
            return 0xFF80848E;
        }
    }

    public interface PresenceCallback {
        void onPresenceLoaded(DiscordPresence presence);
    }

    private static final String PREFS_NAME = "miogram_discord_prefs";
    private static final String KEY_DISCORD_USER_ID = "discord_user_id";

    private DiscordPresence cachedPresence;
    private long lastFetchTime = 0;

    private MiogramDiscordManager() {
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getLinkedUserId() {
        return getPrefs().getString(KEY_DISCORD_USER_ID, "");
    }

    public void setLinkedUserId(String userId) {
        getPrefs().edit().putString(KEY_DISCORD_USER_ID, userId != null ? userId.trim() : "").apply();
        cachedPresence = null;
        lastFetchTime = 0;
    }

    /**
     * Resolves public Discord presence via Lanyard API.
     */
    public void fetchPresence(boolean force, PresenceCallback callback) {
        fetchPresence(getLinkedUserId(), force, callback);
    }

    public void fetchPresence(String targetUid, boolean force, PresenceCallback callback) {
        final String uid = !TextUtils.isEmpty(targetUid) ? targetUid.trim() : getLinkedUserId();
        if (TextUtils.isEmpty(uid)) {
            if (callback != null) callback.onPresenceLoaded(null);
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (!force && cachedPresence != null && uid.equals(cachedPresence.userId) && (now - lastFetchTime < 30000)) {
            if (callback != null) callback.onPresenceLoaded(cachedPresence);
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            try {
                URL u = new URL("https://api.lanyard.rest/v1/users/" + uid);
                conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "Miogram-App");

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resp.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(resp.toString());
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            DiscordPresence p = new DiscordPresence();
                            p.userId = uid;
                            p.status = data.optString("discord_status", "offline");

                            JSONObject du = data.optJSONObject("discord_user");
                            if (du != null) {
                                p.username = du.optString("username", "");
                                p.globalName = du.optString("global_name", "");
                                String avatarHash = du.optString("avatar", "");
                                if (!TextUtils.isEmpty(avatarHash)) {
                                    p.avatarUrl = "https://cdn.discordapp.com/avatars/" + uid + "/" + avatarHash + ".png?size=128";
                                }
                            }

                            JSONArray acts = data.optJSONArray("activities");
                            if (acts != null && acts.length() > 0) {
                                for (int i = 0; i < acts.length(); i++) {
                                    JSONObject a = acts.getJSONObject(i);
                                    int type = a.optInt("type", -1);
                                    if (type == 4) { // Custom status
                                        p.customStatus = a.optString("state", "");
                                    } else if (type == 0 || type == 1 || type == 2) { // Game / Streaming / Listening
                                        p.activityName = a.optString("name", "");
                                        p.activityDetails = a.optString("details", a.optString("state", ""));
                                    }
                                }
                            }

                            p.lastUpdated = System.currentTimeMillis();
                            cachedPresence = p;
                            lastFetchTime = SystemClock.elapsedRealtime();

                            AndroidUtilities.runOnUIThread(() -> {
                                if (callback != null) callback.onPresenceLoaded(p);
                            });
                            return;
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e("MiogramDiscordManager: fetch error", t);
            } finally {
                if (conn != null) conn.disconnect();
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) callback.onPresenceLoaded(cachedPresence);
            });
        });
    }

    public void openProfile(Context context) {
        if (context == null) return;
        String uid = getLinkedUserId();
        if (TextUtils.isEmpty(uid)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/users/" + uid));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignore) {}
    }

    public void copyId(Context context) {
        if (context == null) return;
        String uid = getLinkedUserId();
        if (TextUtils.isEmpty(uid)) return;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Discord ID", uid));
                Toast.makeText(context, MiogramLocale.get("Discord ID скопійовано", "Discord ID скопирован", "Discord ID copied"), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignore) {}
    }

    public void showConfigDialog(Context context, PresenceCallback callback) {
        if (context == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Підключення Discord", "Подключение Discord", "Link Discord"));
        builder.setMessage(MiogramLocale.get(
                "Введіть ваш числовий Discord User ID (потрібна присутність на сервері з ботом Lanyard):",
                "Введите ваш числовой Discord User ID (требуется присутствие на сервере с ботом Lanyard):",
                "Enter your numeric Discord User ID (requires presence on server with Lanyard bot):"
        ));
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setSingleLine(true);
        input.setText(getLinkedUserId());
        builder.setView(input);
        builder.setPositiveButton(MiogramLocale.get("Зберегти", "Сохранить", "Save"), (dialog, which) -> {
            String val = input.getText().toString().trim();
            setLinkedUserId(val);
            fetchPresence(val, true, callback);
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        builder.show();
    }
}
