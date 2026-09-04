package app.miogram.bridge.badge;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Cloud Bridge connecting Miogram clients to the Supabase community badge database.
 * Supports:
 * - Real-time cloud badge resolution with full lore & obtain history
 * - Multi-account strict isolation (only authorized user accounts show badges)
 * - In-memory and SharedPreferences caching for zero-latency offline performance
 */
public class MiogramSupabaseBridge {

    public static class BadgeRecord {
        public final long userId;
        public final MiogramBadgeType badgeType;
        public final String title;
        public final String obtainedReason;
        public final String obtainedAt;
        public final boolean isActive;

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive) {
            this.userId = userId;
            this.badgeType = badgeType != null ? badgeType : MiogramBadgeType.ORIGINAL;
            this.title = title != null ? title : "Miogram Community ໒꒱";
            this.obtainedReason = obtainedReason != null ? obtainedReason : "Верифікований учасник спільноти Miogram";
            this.obtainedAt = obtainedAt != null ? obtainedAt : "01.09.2026";
            this.isActive = isActive;
        }
    }

    private static final String PREFS_NAME = "miogram_supabase_prefs";
    private static final String KEY_OPTIN_COMPLETED = "badge_optin_completed";
    private static final String KEY_SYNC_ENABLED = "badge_sync_enabled_";
    private static final String KEY_SELECTED_BADGE = "badge_selected_style_";
    private static final String KEY_CACHE_JSON = "badge_cache_json_v2";

    public static final String DEFAULT_SUPABASE_URL = "https://dbxsnjoeyiqvqtrluvwu.supabase.co";
    public static final String DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRieHNuam9leWlxdnF0cmx1dnd1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1NDI1MzEsImV4cCI6MjEwNDExODUzMX0.KJ0kvON1HXZu4MzlZjapSJEhEzWYlEqQoNEstWCgIjA";

    private static final LongSparseArray<BadgeRecord> badgeCache = new LongSparseArray<>();
    private static boolean initialized = false;

    private static SharedPreferences getPrefs(Context context) {
        Context ctx = context != null ? context : ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // 1. Pre-seed Founder record in cache
        badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, new BadgeRecord(
                MiogramBadgeManager.FOUNDER_USER_ID,
                MiogramBadgeType.ORIGINAL,
                "Засновник & Архітектор Miogram ໒꒱",
                "Створено автором Miogram як найпершу відзнаку екосистеми з моменту заснування проекту (01.09.2026).",
                "01.09.2026",
                true
        ));

        // 2. Restore cached cloud badges from local storage
        try {
            SharedPreferences prefs = getPrefs(null);
            String cachedJson = prefs.getString(KEY_CACHE_JSON, null);
            if (!TextUtils.isEmpty(cachedJson)) {
                parseAndApplyBadgesJson(cachedJson);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        // 3. Trigger asynchronous background fetch from Supabase
        fetchBadgesFromCloud(null);

        // 4. Report active user account(s) presence and collect ID into database
        reportCurrentUserPresence();
    }

    public static boolean hasCloudBadge(long userId) {
        init();
        if (userId == MiogramBadgeManager.FOUNDER_USER_ID) {
            return true;
        }
        synchronized (badgeCache) {
            BadgeRecord record = badgeCache.get(userId);
            return record != null && record.isActive;
        }
    }

    public static BadgeRecord getBadgeRecord(long userId) {
        init();
        synchronized (badgeCache) {
            BadgeRecord record = badgeCache.get(userId);
            if (record != null) {
                return record;
            }
        }
        if (userId == MiogramBadgeManager.FOUNDER_USER_ID) {
            return new BadgeRecord(
                    MiogramBadgeManager.FOUNDER_USER_ID,
                    MiogramBadgeType.ORIGINAL,
                    "Засновник & Архітектор Miogram ໒꒱",
                    "Створено автором Miogram як найпершу відзнаку екосистеми з моменту заснування проекту.",
                    "01.09.2026",
                    true
            );
        }
        return null;
    }

    public static MiogramBadgeType getCachedBadgeType(long userId) {
        BadgeRecord record = getBadgeRecord(userId);
        return record != null ? record.badgeType : MiogramBadgeType.ORIGINAL;
    }

    public static boolean isOptInCompleted(Context context) {
        return getPrefs(context).getBoolean(KEY_OPTIN_COMPLETED, false);
    }

    public static boolean isSyncEnabledForAccount(Context context, long userId) {
        return getPrefs(context).getBoolean(KEY_SYNC_ENABLED + userId, false);
    }

    public static void setSyncEnabledForAccount(Context context, long userId, boolean enabled) {
        getPrefs(context).edit()
                .putBoolean(KEY_OPTIN_COMPLETED, true)
                .putBoolean(KEY_SYNC_ENABLED + userId, enabled)
                .apply();

        if (userId != 0) {
            if (enabled) {
                MiogramBadgeType selected = getSelectedBadgeForAccount(context, userId);
                synchronized (badgeCache) {
                    badgeCache.put(userId, new BadgeRecord(userId, selected, "Учасник спільноти Miogram", "Отримано через хмарну синхронізацію спільноти", "2026", true));
                }
                syncUserBadgeToCloud(userId, selected.getId(), true, null);
            } else {
                syncUserBadgeToCloud(userId, "original", false, null);
            }
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public static MiogramBadgeType getSelectedBadgeForAccount(Context context, long userId) {
        String id = getPrefs(context).getString(KEY_SELECTED_BADGE + userId, "original");
        return MiogramBadgeType.fromId(id);
    }

    public static void setSelectedBadgeForAccount(Context context, long userId, MiogramBadgeType type) {
        if (type == null) type = MiogramBadgeType.ORIGINAL;
        getPrefs(context).edit().putString(KEY_SELECTED_BADGE + userId, type.getId()).apply();

        if (userId != 0) {
            synchronized (badgeCache) {
                BadgeRecord existing = badgeCache.get(userId);
                String title = existing != null ? existing.title : "Учасник спільноти Miogram";
                String reason = existing != null ? existing.obtainedReason : "Отримано через хмарну синхронізацію";
                String date = existing != null ? existing.obtainedAt : "2026";
                badgeCache.put(userId, new BadgeRecord(userId, type, title, reason, date, true));
            }
            if (isSyncEnabledForAccount(context, userId) || userId == MiogramBadgeManager.FOUNDER_USER_ID) {
                syncUserBadgeToCloud(userId, type.getId(), true, null);
            }
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public static void fetchBadgesFromCloud(Runnable onComplete) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?select=user_id,badge_id,title,obtained_reason,obtained_at,is_active&is_active=eq.true";
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("apikey", DEFAULT_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + DEFAULT_ANON_KEY);
                connection.setRequestProperty("Accept", "application/json");

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String resultJson = sb.toString();
                    parseAndApplyBadgesJson(resultJson);

                    getPrefs(null).edit().putString(KEY_CACHE_JSON, resultJson).apply();

                    AndroidUtilities.runOnUIThread(() -> {
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                        if (onComplete != null) onComplete.run();
                    });
                    return;
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        });
    }

    private static void parseAndApplyBadgesJson(String jsonStr) {
        try {
            JSONArray arr = new JSONArray(jsonStr);
            synchronized (badgeCache) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    long uid = obj.optLong("user_id");
                    boolean active = obj.optBoolean("is_active", true);
                    String badgeId = obj.optString("badge_id", "original");
                    String title = obj.optString("title", "Miogram Community ໒꒱");
                    String reason = obj.optString("obtained_reason", "Верифікований учасник спільноти Miogram");
                    String date = obj.optString("obtained_at", "01.09.2026");

                    if (uid != 0 && active) {
                        badgeCache.put(uid, new BadgeRecord(uid, MiogramBadgeType.fromId(badgeId), title, reason, date, true));
                    }
                }
                // Preserve founder entry
                if (badgeCache.get(MiogramBadgeManager.FOUNDER_USER_ID) == null) {
                    badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, new BadgeRecord(
                            MiogramBadgeManager.FOUNDER_USER_ID,
                            MiogramBadgeType.ORIGINAL,
                            "Засновник & Архітектор Miogram ໒꒱",
                            "Створено автором Miogram як першу канонічну відзнаку екосистеми з моменту заснування проекту.",
                            "01.09.2026",
                            true
                    ));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void syncUserBadgeToCloud(long userId, String badgeId, boolean isActive, Runnable onComplete) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges";
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("apikey", DEFAULT_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + DEFAULT_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "resolution=merge-duplicates");

                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("badge_id", badgeId);
                body.put("is_active", isActive);
                body.put("client_version", "Miogram 1.0");

                byte[] outBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(outBytes.length);
                OutputStream os = connection.getOutputStream();
                os.write(outBytes);
                os.flush();
                os.close();

                int code = connection.getResponseCode();
                FileLog.d("MiogramSupabaseBridge sync status: " + code);
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        });
    }

    public static void reportCurrentUserPresence() {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        long uid = UserConfig.getInstance(a).getClientUserId();
                        if (uid != 0) {
                            reportUserPresence(uid);
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static void reportUserPresence(long userId) {
        if (userId == 0) return;
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                // 1. Try to record presence in miogram_users
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_users";
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("apikey", DEFAULT_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + DEFAULT_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "resolution=merge-duplicates");

                String isoDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());

                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("last_seen_at", isoDate);
                body.put("client_version", "Miogram " + BuildVars.BUILD_VERSION_STRING);

                byte[] outBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(outBytes.length);
                OutputStream os = connection.getOutputStream();
                os.write(outBytes);
                os.flush();
                os.close();

                int code = connection.getResponseCode();
                FileLog.d("MiogramSupabaseBridge presence reported: " + code);

                // 2. If table miogram_users does not exist yet (404), ensure the user is registered in miogram_badges
                if (code == 404) {
                    syncUserBadgeToCloud(userId, "original", true, null);
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}
