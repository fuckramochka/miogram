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
import org.telegram.messenger.MessagesController;
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

import android.os.Build;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.TimeZone;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

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
        public final java.util.List<MiogramBadgeType> badgeTypes;
        public final String badgeIdString;
        public final String title;
        public final String obtainedReason;
        public final String obtainedAt;
        public final boolean isActive;
        /** Server-staff verification. Self-claimed rows are always false. */
        public final boolean verified;
        /** user_id of the granter (founder grants carry FOUNDER_USER_ID). 0 = self-selected style. */
        public final long grantorId;

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive) {
            this(userId, badgeType != null ? badgeType.getId() : "original", title, obtainedReason, obtainedAt, isActive, userId == MiogramBadgeManager.FOUNDER_USER_ID, 0);
        }

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive, boolean verified) {
            this(userId, badgeType != null ? badgeType.getId() : "original", title, obtainedReason, obtainedAt, isActive, verified, 0);
        }

        public BadgeRecord(long userId, MiogramBadgeType badgeType, String title, String obtainedReason, String obtainedAt, boolean isActive, boolean verified, long grantorId) {
            this(userId, badgeType != null ? badgeType.getId() : "original", title, obtainedReason, obtainedAt, isActive, verified, grantorId);
        }

        public BadgeRecord(long userId, String badgeIds, String title, String obtainedReason, String obtainedAt, boolean isActive, boolean verified, long grantorId) {
            this.userId = userId;
            this.badgeIdString = badgeIds != null && !badgeIds.isEmpty() ? badgeIds : "original";
            java.util.List<MiogramBadgeType> list = new java.util.ArrayList<>();
            String[] parts = this.badgeIdString.split(",");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    list.add(MiogramBadgeType.fromId(clean));
                }
            }
            if (list.isEmpty()) {
                list.add(MiogramBadgeType.ORIGINAL);
            }
            this.badgeTypes = java.util.Collections.unmodifiableList(list);
            this.badgeType = list.get(0);
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
                        badgeCache.put(uid, new BadgeRecord(uid, badgeId, title, reason, date, true, verified, grantorId));
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
                if (code < 200 || code >= 300) {
                    showSyncErrorDialog(null, "Badge sync HTTP " + code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage();
                showSyncErrorDialog(null, (msg != null && !msg.isEmpty()) ? (e.getClass().getSimpleName() + ": " + msg) : e.toString());
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

    private static long lastSyncErrorDialogTime = 0;

    public static void openBugReportChat(Context context, String issueType, String errorDetails) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                int account = UserConfig.selectedAccount;
                StringBuilder sb = new StringBuilder();
                sb.append("Hello @dkramochka,\n\n");
                sb.append("I am reporting an issue encountered in Miogram:\n\n");
                sb.append("[Miogram Bug Report]\n");
                sb.append("Issue: ").append(issueType != null && !issueType.isEmpty() ? issueType : "Runtime Issue").append("\n");
                sb.append("App Version: Miogram ").append(BuildVars.BUILD_VERSION_STRING).append(" (").append(BuildVars.BUILD_VERSION).append(")\n");
                sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
                sb.append("OS: Android ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
                try {
                    long userId = UserConfig.getInstance(account).getClientUserId();
                    if (userId != 0) {
                        sb.append("User ID: ").append(userId).append("\n");
                    }
                } catch (Throwable ignore) {}
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    sb.append("Timestamp: ").append(sdf.format(new Date())).append("\n");
                } catch (Throwable ignore) {}
                sb.append("\nError Log / Details:\n");
                sb.append(errorDetails != null && !errorDetails.trim().isEmpty() ? errorDetails.trim() : "No extra logs provided.");

                String fullReport = sb.toString();

                // 1. Copy to clipboard
                AndroidUtilities.addToClipboard(fullReport);

                Context ctx = context;
                if (ctx == null) {
                    ctx = LaunchActivity.instance;
                }
                if (ctx == null) {
                    ctx = ApplicationLoader.applicationContext;
                }

                try {
                    Toast.makeText(ctx, MiogramLocale.get(
                            "Звіт та лог скопійовано. Відкриваємо чат із @dkramochka...",
                            "Отчет и лог скопированы. Открываем чат с @dkramochka...",
                            "Bug report copied to clipboard. Opening chat with @dkramochka..."
                    ), Toast.LENGTH_SHORT).show();
                } catch (Throwable ignore) {}

                // 2. Resolve @dkramochka, save draft into dialog, and open chat natively
                BaseFragment lastFragment = LaunchActivity.getLastFragment();
                MessagesController mc = MessagesController.getInstance(account);
                mc.getUserNameResolver().resolve("dkramochka", (peerId) -> {
                    if (peerId != null && peerId > 0) {
                        try {
                            MediaDataController.getInstance(account).saveDraft(peerId, 0, fullReport, null, null, true, 0);
                        } catch (Throwable t) {
                            FileLog.e(t);
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (lastFragment != null) {
                                mc.openByUserName("dkramochka", lastFragment, 1);
                            } else {
                                Browser.openUrl(ctx, "https://t.me/dkramochka");
                            }
                        } catch (Throwable t) {
                            FileLog.e(t);
                            Browser.openUrl(ctx, "https://t.me/dkramochka");
                        }
                    });
                });
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    public static void showBugReportDialog(Context context, String title, String message, String issueType, String errorDetails) {
        AndroidUtilities.runOnUIThread(() -> {
            Context ctx = context != null ? context : LaunchActivity.instance;
            if (ctx == null) ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle(title != null ? title : MiogramLocale.get("Звіт про помилку", "Отчет об ошибке", "Bug Report"));
                builder.setMessage(message != null ? message : MiogramLocale.get(
                        "Бажаєте надіслати звіт із логами творцю @dkramochka?",
                        "Желаете отправить отчет с логами создателю @dkramochka?",
                        "Would you like to send a bug report with logs to creator @dkramochka?"
                ));
                builder.setPositiveButton(MiogramLocale.get("Відправити баг", "Отправить баг", "Send Bug"), (d, which) -> {
                    d.dismiss();
                    openBugReportChat(ctx, issueType, errorDetails);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (d, which) -> d.dismiss());
                builder.create().show();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    public static void showSyncErrorDialog(Context context, String errorDetails) {
        AndroidUtilities.runOnUIThread(() -> {
            long now = System.currentTimeMillis();
            if (now - lastSyncErrorDialogTime < 15_000L) {
                return;
            }
            lastSyncErrorDialogTime = now;

            Context ctx = context;
            if (ctx == null) {
                ctx = LaunchActivity.instance;
            }
            if (ctx == null) {
                ctx = ApplicationLoader.applicationContext;
            }
            if (ctx == null) return;

            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle(MiogramLocale.get("Критична помилка синхронізації", "Критическая ошибка синхронизации", "Critical Sync Error"));
                builder.setMessage(MiogramLocale.get(
                        "Критична помилка синхронізації. Щоб уникнути проблем, надішліть помилку творцю",
                        "Критическая ошибка синхронизации. Чтобы избежать проблем, отправьте ошибку создателю",
                        "Critical synchronization error. To avoid issues, please send this error to the creator."
                ));
                builder.setCancelable(false);

                final Context finalCtx = ctx;
                final String finalError = (errorDetails != null && !errorDetails.trim().isEmpty())
                        ? errorDetails.trim()
                        : "Unknown error occurred during Supabase synchronization.";

                builder.setPositiveButton(MiogramLocale.get("Відправити баг", "Отправить баг", "Send Bug"), (d, which) -> {
                    d.dismiss();
                    openBugReportChat(finalCtx, "Critical Supabase Sync Error", finalError);
                });
                builder.setNegativeButton(MiogramLocale.get("Не зараз", "Не сейчас", "Not now"), (d, which) -> {
                    d.dismiss();
                });

                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            } catch (Throwable t) {
                FileLog.e(t);
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
                if (code < 200 || code >= 300) {
                    showSyncErrorDialog(null, "Presence HTTP " + code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage();
                showSyncErrorDialog(null, (msg != null && !msg.isEmpty()) ? (e.getClass().getSimpleName() + ": " + msg) : e.toString());
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
                                fBadge, fTitle, fReason,
                                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()), true, true, fGranter));
                    }
                } else {
                    showSyncErrorDialog(null, "Grant badge HTTP " + code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage();
                showSyncErrorDialog(null, (msg != null && !msg.isEmpty()) ? (e.getClass().getSimpleName() + ": " + msg) : e.toString());
            } finally {
                if (connection != null) connection.disconnect();
            }
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        });
    }

    public static void revokeBadge(long targetUserId, Runnable onComplete) {
        if (targetUserId <= 0) return;
        synchronized (badgeCache) {
            badgeCache.remove(targetUserId);
        }
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME | MessagesController.UPDATE_MASK_AVATAR);
        });

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = DEFAULT_SUPABASE_URL + "/rest/v1/miogram_badges?user_id=eq." + targetUserId;
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("apikey", DEFAULT_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + DEFAULT_ANON_KEY);

                int code = connection.getResponseCode();
                FileLog.d("MiogramSupabaseBridge revoke badge status: " + code);
                if (code >= 200 && code < 300) {
                    if (onComplete != null) {
                        AndroidUtilities.runOnUIThread(onComplete);
                    }
                } else {
                    showSyncErrorDialog(null, "Revoke badge HTTP " + code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = e.getMessage();
                showSyncErrorDialog(null, (msg != null && !msg.isEmpty()) ? (e.getClass().getSimpleName() + ": " + msg) : e.toString());
            } finally {
                if (connection != null) connection.disconnect();
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
