package app.miogram.bridge.badge;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
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

/**
 * Cloud Bridge connecting Miogram clients to the Supabase community badge database.
 * Provides:
 * - Dynamic cloud resolution of 10 badge styles across all users
 * - Instant in-memory and SharedPreferences caching for offline reliability
 * - Respectful community presence registration and opt-in sync
 */
public class MiogramSupabaseBridge {

    private static final String PREFS_NAME = "miogram_supabase_prefs";
    private static final String KEY_OPTIN_COMPLETED = "badge_optin_completed";
    private static final String KEY_SYNC_ENABLED = "badge_sync_enabled";
    private static final String KEY_SELECTED_BADGE = "badge_selected_style";
    private static final String KEY_CACHE_JSON = "badge_cache_json";

    // Supabase REST endpoint configuration
    // (Configurable with fallback to project cluster)
    public static final String DEFAULT_SUPABASE_URL = "https://miogram-badges.supabase.co";
    public static final String DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.placeholder_anon_key";

    private static final LongSparseArray<MiogramBadgeType> badgeCache = new LongSparseArray<>();
    private static boolean initialized = false;

    private static SharedPreferences getPrefs(Context context) {
        Context ctx = context != null ? context : ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // 1. Founder fallback is always guaranteed
        badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, MiogramBadgeType.ORIGINAL);

        // 2. Restore cached cloud badges from local persistent storage
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
    }

    public static MiogramBadgeType getCachedBadge(long userId) {
        init();
        synchronized (badgeCache) {
            return badgeCache.get(userId);
        }
    }

    public static boolean isOptInCompleted(Context context) {
        return getPrefs(context).getBoolean(KEY_OPTIN_COMPLETED, false);
    }

    public static boolean isSyncEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SYNC_ENABLED, false);
    }

    public static void setSyncEnabled(Context context, boolean enabled) {
        getPrefs(context).edit()
                .putBoolean(KEY_OPTIN_COMPLETED, true)
                .putBoolean(KEY_SYNC_ENABLED, enabled)
                .apply();

        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (currentUserId != 0) {
            if (enabled) {
                MiogramBadgeType selected = getSelectedBadge(context);
                synchronized (badgeCache) {
                    badgeCache.put(currentUserId, selected);
                }
                syncUserBadgeToCloud(currentUserId, selected.getId(), true, null);
            } else {
                syncUserBadgeToCloud(currentUserId, "original", false, null);
            }
        }
    }

    public static MiogramBadgeType getSelectedBadge(Context context) {
        String id = getPrefs(context).getString(KEY_SELECTED_BADGE, "original");
        return MiogramBadgeType.fromId(id);
    }

    public static void setSelectedBadge(Context context, MiogramBadgeType type) {
        if (type == null) type = MiogramBadgeType.ORIGINAL;
        getPrefs(context).edit().putString(KEY_SELECTED_BADGE, type.getId()).apply();

        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (currentUserId != 0) {
            synchronized (badgeCache) {
                badgeCache.put(currentUserId, type);
            }
            if (isSyncEnabled(context)) {
                syncUserBadgeToCloud(currentUserId, type.getId(), true, null);
            }
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public static void fetchBadgesFromCloud(Runnable onComplete) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?select=user_id,badge_id,is_active&is_active=eq.true";
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

                    // Save valid response to cache
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
                    if (uid != 0 && active) {
                        badgeCache.put(uid, MiogramBadgeType.fromId(badgeId));
                    }
                }
                // Always ensure founder is preserved
                if (badgeCache.get(MiogramBadgeManager.FOUNDER_USER_ID) == null) {
                    badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, MiogramBadgeType.ORIGINAL);
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
}
