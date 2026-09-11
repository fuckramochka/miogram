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

import app.miogram.bridge.MiogramLocale;

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
        /** Server-staff verification. Self-claimed rows are always false. */
        public final boolean verified;
        /** user_id of the granter (founder grants carry FOUNDER_USER_ID). 0 = self-selected style. */
        public final long grantorId;

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive) {
            this(userId, badgeType, title, obtainedReason, obtainedAt, isActive, userId == MiogramBadgeManager.FOUNDER_USER_ID, 0);
        }

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive, boolean verified) {
            this(userId, badgeType, title, obtainedReason, obtainedAt, isActive, verified, 0);
        }

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive, boolean verified, long grantorId) {
            this.userId = userId;
            this.badgeType = badgeType != null ? badgeType : MiogramBadgeType.ORIGINAL;
            this.title = title != null ? title : "Miogram Community ໒꒱";
            this.obtainedReason = obtainedReason != null ? obtainedReason : "Верифікований учасник спільноти Miogram";
            this.obtainedAt = obtainedAt != null ? obtainedAt : "01.09.2026";
            this.isActive = isActive;
            this.verified = verified;
            this.grantorId = grantorId;
        }
    }

    private static final String PREFS_NAME = "miogram_supabase_prefs";
    private static final String KEY_OPTIN_COMPLETED = "badge_optin_completed";
    private static final String KEY_SYNC_ENABLED = "badge_sync_enabled_";
    private static final String KEY_SELECTED_BADGE = "badge_selected_style_";
    private static final String KEY_CACHE_JSON = "badge_cache_cloud_v10";
    private static final String KEY_TELEMETRY_ENABLED = "telemetry_enabled";

    public static boolean isTelemetryEnabled() {
        return getPrefs(null).getBoolean(KEY_TELEMETRY_ENABLED, true);
    }

    public static void setTelemetryEnabled(boolean enabled) {
        getPrefs(null).edit().putBoolean(KEY_TELEMETRY_ENABLED, enabled).apply();
    }

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

        synchronized (badgeCache) {
            badgeCache.clear();
            badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, createDefaultFounderRecord());
        }

        // 1. Restore cached cloud badges from local storage
        try {
            SharedPreferences prefs = getPrefs(null);
            String cachedJson = prefs.getString(KEY_CACHE_JSON, null);
            if (!TextUtils.isEmpty(cachedJson)) {
                parseAndApplyBadgesJson(cachedJson);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        // 2. Trigger asynchronous background fetch from Supabase
        fetchBadgesFromCloud(null);

        // 3. Report active user account(s) presence into users table
        reportCurrentUserPresence();
    }

    private static BadgeRecord createDefaultFounderRecord() {
        return new BadgeRecord(
                MiogramBadgeManager.FOUNDER_USER_ID,
                MiogramBadgeType.ORIGINAL,
                "Засновник & Архітектор Miogram ໒꒱",
                "Особиста відзнака засновника та головного архітектора екосистеми Miogram (@fuckramochka).",
                "01.09.2026",
                true,
                true,
                MiogramBadgeManager.FOUNDER_USER_ID
        );
    }

    public static boolean hasCloudBadge(long userId) {
        if (userId <= 0) {
            return false;
        }
        if (userId == MiogramBadgeManager.FOUNDER_USER_ID) {
            return true;
        }
        init();
        synchronized (badgeCache) {
            BadgeRecord record = badgeCache.get(userId);
            return record != null && record.isActive;
        }
    }

    public static BadgeRecord getBadgeRecord(long userId) {
        if (userId <= 0) {
            return null;
        }
        init();
        synchronized (badgeCache) {
            BadgeRecord record = badgeCache.get(userId);
            if (record == null && userId == MiogramBadgeManager.FOUNDER_USER_ID) {
                record = createDefaultFounderRecord();
                badgeCache.put(userId, record);
            }
            return record;
        }
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

    /** Fallback title/reason shown in UI when cloud has no record yet — always trilingual. */
    public static String fallbackTitle(boolean founder) {
        return founder
                ? MiogramLocale.get("Засновник & Архітектор Miogram ໒꒱", "Основатель & Архитектор Miogram ໒꒱", "Miogram Founder & Architect ໒꒱")
                : MiogramLocale.get("Учасник спільноти Miogram", "Участник сообщества Miogram", "Miogram community member");
    }

    public static String fallbackReason(boolean founder) {
        return founder
                ? MiogramLocale.get("Особиста відзнака засновника Miogram", "Личная награда основателя Miogram", "Personal founder badge of Miogram")
                : MiogramLocale.get("Отримано через хмарну синхронізацію спільноти", "Получено через облачную синхронизацию сообщества", "Granted via community cloud sync");
    }

    /** True when a row claims founder status (title/reason/id) without staff verification. */
    private static boolean looksLikeFounderClaim(long userId, String title, String reason) {
        if (userId == MiogramBadgeManager.FOUNDER_USER_ID) return true;
        String t = (title != null ? title : "").toLowerCase(java.util.Locale.ROOT);
        String r = (reason != null ? reason : "").toLowerCase(java.util.Locale.ROOT);
        return t.contains("засновник") || t.contains("основатель") || t.contains("founder")
                || t.contains("архітектор") || t.contains("архитектор") || t.contains("architect")
                || r.contains("засновник") || r.contains("основатель") || r.contains("founder");
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
                    badgeCache.put(userId, new BadgeRecord(userId, selected, fallbackTitle(false), fallbackReason(false), "2026", true));
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
        getPrefs(context).edit()
                .putString(KEY_SELECTED_BADGE + userId, type.getId())
                .putBoolean(KEY_SYNC_ENABLED + userId, true)
                .apply();

        if (userId != 0) {
            boolean founder = userId == MiogramBadgeManager.FOUNDER_USER_ID;
            String title = fallbackTitle(founder);
            String reason = fallbackReason(founder);
            String date = "2026";
            synchronized (badgeCache) {
                BadgeRecord existing = badgeCache.get(userId);
                if (existing != null) {
                    if (existing.title != null) title = existing.title;
                    if (existing.obtainedReason != null) reason = existing.obtainedReason;
                    if (existing.obtainedAt != null) date = existing.obtainedAt;
                }
                badgeCache.put(userId, new BadgeRecord(userId, type, title, reason, date, true));
            }
            syncUserBadgeToCloud(userId, type.getId(), true, null);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public static void fetchBadgesFromCloud(Runnable onComplete) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?select=*&is_active=eq.true";
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
                badgeCache.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    long uid = obj.optLong("user_id");
                    boolean active = obj.optBoolean("is_active", true);
                    String badgeId = obj.optString("badge_id", "original");
                    String title = obj.optString("title", "Miogram Community ໒꒱");
                    String reason = obj.optString("obtained_reason", "Верифікований учасник спільноти Miogram");
                    String date = obj.optString("obtained_at", "01.09.2026");
                    boolean verified = obj.optBoolean("verified", uid == MiogramBadgeManager.FOUNDER_USER_ID);
                    long grantorId = obj.optLong("grantor_id", 0);

                    if (uid != 0 && active) {
                        // Anti-abuse: unverified founder claims render as plain member rows.
                        if (!verified && looksLikeFounderClaim(uid, title, reason)) {
                            title = fallbackTitle(false);
                            reason = fallbackReason(false);
                        }
                        badgeCache.put(uid, new BadgeRecord(uid, MiogramBadgeType.fromId(badgeId), title, reason, date, true, verified, grantorId));
                    }
                }
                if (badgeCache.get(MiogramBadgeManager.FOUNDER_USER_ID) == null) {
                    badgeCache.put(MiogramBadgeManager.FOUNDER_USER_ID, createDefaultFounderRecord());
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static long lastFetchTime = 0;

    public static void checkRefreshBadges() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > 60_000L) {
            lastFetchTime = now;
            fetchBadgesFromCloud(null);
        }
    }

    public static void syncUserBadgeToCloud(long userId, String badgeId, boolean isActive, Runnable onComplete) {
        if (userId <= 0) return;
        final BadgeRecord record;
        synchronized (badgeCache) {
            record = badgeCache.get(userId);
        }
        final String fTitle = record != null && record.title != null ? record.title : fallbackTitle(false);
        final String fReason = record != null && record.obtainedReason != null ? record.obtainedReason : fallbackReason(false);
        final String fDate = record != null && record.obtainedAt != null ? record.obtainedAt : "2026";

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?on_conflict=user_id";
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
                body.put("badge_id", badgeId != null ? badgeId : "original");
                body.put("is_active", isActive);
                body.put("title", fTitle);
                body.put("obtained_reason", fReason);
                body.put("obtained_at", fDate);
                body.put("client_version", "Miogram " + BuildVars.BUILD_VERSION_STRING);

                byte[] outBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(outBytes.length);
                OutputStream os = connection.getOutputStream();
                os.write(outBytes);
                os.flush();
                os.close();

                int code = connection.getResponseCode();
                FileLog.d("MiogramSupabaseBridge upsert badge status: " + code);
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
        if (userId == 0 || !isTelemetryEnabled()) return;
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                // Presence lives in miogram_users (NOT miogram_badges — the old
                // code wrote last_seen_at into badges where the column does not
                // exist, so every presence call failed and the counter stayed 0).
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_users?on_conflict=user_id";
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


            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Founder grant: writes ANOTHER user's row with an explicit badge, title
     * and reason. The row carries grantor_id so clients can render
     * "Granted by Founder". Note: with the anon key this is a trust signal,
     * not proof — real proof is the staff-only {@code verified} flag
     * (service_role / dashboard). Until Supabase Auth with Telegram login
     * lands, treat unverified founder lore as cosmetic.
     */
    public static void grantBadgeToUser(long targetUserId, String badgeId, String title, String reason, Runnable onComplete) {
        if (targetUserId <= 0) return;
        final String fBadge = badgeId != null ? badgeId : "original";
        final String fTitle = title != null ? title : fallbackTitle(false);
        final String fReason = reason != null ? reason : fallbackReason(false);
        long granter = 0;
        try {
            granter = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        } catch (Throwable ignored) {}
        final long fGranter = granter;
        synchronized (badgeCache) {
            badgeCache.put(targetUserId, new BadgeRecord(targetUserId,
                    MiogramBadgeType.fromId(fBadge), fTitle, fReason,
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()), true, true, fGranter));
        }
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME | MessagesController.UPDATE_MASK_AVATAR);
        });
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?on_conflict=user_id");
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
                body.put("user_id", targetUserId);
                body.put("badge_id", fBadge);
                body.put("is_active", true);
                body.put("title", fTitle);
                body.put("obtained_reason", fReason);
                body.put("obtained_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));
                body.put("client_version", "Miogram " + BuildVars.BUILD_VERSION_STRING);

                byte[] outBytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(outBytes.length);
                OutputStream os = connection.getOutputStream();
                os.write(outBytes);
                os.flush();
                os.close();

                int code = connection.getResponseCode();
                FileLog.d("MiogramSupabaseBridge grant badge status: " + code);
                if (code >= 200 && code < 300) {
                    synchronized (badgeCache) {
                        badgeCache.put(targetUserId, new BadgeRecord(targetUserId,
                                MiogramBadgeType.fromId(fBadge), fTitle, fReason,
                                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()), true, true, fGranter));
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) connection.disconnect();
            }
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        });
    }

    /** Public community counters for the website/app (via SECURITY DEFINER RPC, no table scan). */    public static void getCommunityStats(Utilities.Callback2<Long, Long> callback) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            long users = -1;
            long badges = -1;
            try {
                URL url = new URL(DEFAULT_SUPABASE_URL + "/rest/v1/rpc/miogram_community_stats");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("apikey", DEFAULT_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + DEFAULT_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                byte[] outBytes = "{}".getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(outBytes.length);
                OutputStream os = connection.getOutputStream();
                os.write(outBytes);
                os.flush();
                os.close();

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    JSONObject obj = new JSONObject(sb.toString());
                    users = obj.optLong("users_count", -1);
                    badges = obj.optLong("badges_count", -1);
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) connection.disconnect();
            }
            final long fUsers = users;
            final long fBadges = badges;
            AndroidUtilities.runOnUIThread(() -> callback.run(fUsers, fBadges));
        });
    }
}
