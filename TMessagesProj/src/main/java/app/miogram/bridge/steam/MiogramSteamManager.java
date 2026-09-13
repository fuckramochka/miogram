package app.miogram.bridge.steam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.badge.MiogramSupabaseBridge;

/**
 * Secure Steam Gaming & Profile Bridge for Miogram.
 * Features:
 * - Public Steam Community XML metadata resolver (zero API keys required, zero private data).
 * - Safe Supabase cloud sync for cross-user profile visibility.
 * - Steam URI Deep-linking (steam://run/<appId>, steam://friends/add/<steamId>).
 */
public class MiogramSteamManager {

    private static volatile MiogramSteamManager instance;

    public static MiogramSteamManager getInstance() {
        if (instance == null) {
            synchronized (MiogramSteamManager.class) {
                if (instance == null) {
                    instance = new MiogramSteamManager();
                }
            }
        }
        return instance;
    }

    public static class SteamProfile {
        public long userId;
        public String steamId = "";
        public String personaName = "";
        public String avatarUrl = "";
        public String profileUrl = "";
        public String gameId = "";
        public String gameName = "";
        public String gameIconUrl = "";
        public String gameHours2Weeks = "";
        public String gameHoursTotal = "";
        public boolean isInGame = false;
        public String stateMessage = "";
        public long lastUpdated = 0;

        public boolean hasGame() {
            return isInGame && !TextUtils.isEmpty(gameName);
        }
    }

    public interface ProfileCallback {
        void onProfileLoaded(SteamProfile profile);
    }

    private static final String PREFS_NAME = "miogram_steam_prefs";
    private static final String KEY_SELF_STEAM_ID = "self_steam_id";
    private static final String KEY_BROADCAST_ENABLED = "self_steam_broadcast_enabled";

    private final LongSparseArray<SteamProfile> profileCache = new LongSparseArray<>();
    private final LongSparseArray<Long> lastFetchTime = new LongSparseArray<>();

    private MiogramSteamManager() {
        // Load self profile from cache if present
        loadSelfFromCache();
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isBroadcastEnabled() {
        return getPrefs().getBoolean(KEY_BROADCAST_ENABLED, true);
    }

    public void setBroadcastEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BROADCAST_ENABLED, enabled).apply();
    }

    public String getLinkedSteamId() {
        return getPrefs().getString(KEY_SELF_STEAM_ID, "");
    }

    public String getSelfSteamId() {
        return getLinkedSteamId();
    }

    public void setLinkedSteamId(String steamId) {
        getPrefs().edit().putString(KEY_SELF_STEAM_ID, steamId != null ? steamId.trim() : "").apply();
    }

    public boolean isLinked() {
        return !TextUtils.isEmpty(getLinkedSteamId());
    }

    /**
     * Resolves public Steam Community profile using the official XML feed.
     * Works for custom vanity URLs, friend IDs, vanity URLs with/without https, and SteamID64.
     */
    public void resolvePublicSteam(String query, ProfileCallback callback) {
        if (TextUtils.isEmpty(query)) {
            if (callback != null) callback.onProfileLoaded(null);
            return;
        }

        String q = query.trim();
        if (q.startsWith("https://")) q = q.substring(8);
        else if (q.startsWith("http://")) q = q.substring(7);

        boolean explicitProfiles = false;
        boolean explicitId = false;

        if (q.contains("steamcommunity.com/profiles/")) {
            explicitProfiles = true;
            q = q.substring(q.indexOf("steamcommunity.com/profiles/") + "steamcommunity.com/profiles/".length());
        } else if (q.contains("steamcommunity.com/id/")) {
            explicitId = true;
            q = q.substring(q.indexOf("steamcommunity.com/id/") + "steamcommunity.com/id/".length());
        }

        if (q.contains("?")) q = q.substring(0, q.indexOf("?"));
        if (q.contains("#")) q = q.substring(0, q.indexOf("#"));
        if (q.contains("/")) q = q.substring(0, q.indexOf("/"));
        final String clean = q.trim();

        if (TextUtils.isEmpty(clean)) {
            if (callback != null) callback.onProfileLoaded(null);
            return;
        }

        // Friend Code (7 to 10 digits) -> convert to SteamID64
        String resolvedId64 = null;
        if (!explicitId && clean.matches("\\d{7,10}")) {
            try {
                long friendCode = Long.parseLong(clean);
                resolvedId64 = String.valueOf(76561197960265728L + friendCode);
            } catch (Throwable ignore) {}
        } else if (clean.matches("\\d{17}")) {
            resolvedId64 = clean;
        }

        final String primaryUrl;
        final String secondaryUrl;

        if (resolvedId64 != null || explicitProfiles) {
            String targetId = resolvedId64 != null ? resolvedId64 : clean;
            primaryUrl = "https://steamcommunity.com/profiles/" + targetId + "/?xml=1";
            secondaryUrl = null;
        } else {
            primaryUrl = "https://steamcommunity.com/id/" + clean + "/?xml=1";
            secondaryUrl = "https://steamcommunity.com/profiles/" + clean + "/?xml=1";
        }

        Utilities.globalQueue.postRunnable(() -> {
            SteamProfile profile = fetchSteamXmlWithRedirects(primaryUrl);
            if (profile == null && secondaryUrl != null) {
                profile = fetchSteamXmlWithRedirects(secondaryUrl);
            }

            final SteamProfile result = profile;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) callback.onProfileLoaded(result);
            });
        });
    }

    private SteamProfile fetchSteamXmlWithRedirects(String urlStr) {
        String currentUrl = urlStr;
        for (int redirects = 0; redirects < 4; redirects++) {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(currentUrl);
                conn = (HttpURLConnection) u.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "text/xml,application/xml,*/*");

                int code = conn.getResponseCode();
                if (code >= 300 && code < 400) {
                    String loc = conn.getHeaderField("Location");
                    if (TextUtils.isEmpty(loc)) break;
                    if (!loc.contains("?xml=1") && !loc.contains("&xml=1")) {
                        loc += (loc.contains("?") ? "&" : "?") + "xml=1";
                    }
                    currentUrl = loc;
                    continue;
                }

                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder xml = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        xml.append(line).append("\n");
                    }
                    reader.close();

                    return parseSteamXml(xml.toString());
                }
                break;
            } catch (Throwable t) {
                FileLog.e("MiogramSteamManager: fetch error for " + currentUrl, t);
                break;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }

    private SteamProfile parseSteamXml(String xml) {
        if (TextUtils.isEmpty(xml) || !xml.contains("<profile>") || xml.contains("<error>")) return null;

        SteamProfile p = new SteamProfile();
        p.steamId = extractTag(xml, "steamID64");
        p.personaName = extractTag(xml, "steamID");
        p.avatarUrl = extractTag(xml, "avatarMedium");
        if (TextUtils.isEmpty(p.avatarUrl)) p.avatarUrl = extractTag(xml, "avatarFull");
        if (TextUtils.isEmpty(p.avatarUrl)) p.avatarUrl = extractTag(xml, "avatarIcon");
        p.stateMessage = extractTag(xml, "stateMessage");
        String hours = extractTag(xml, "hoursPlayed2Wk");
        if (!TextUtils.isEmpty(hours) && !"0.0".equals(hours) && !"0".equals(hours)) {
            p.gameHours2Weeks = hours;
        }

        String onlineState = extractTag(xml, "onlineState");

        // In-game info block
        if (xml.contains("<inGameInfo>")) {
            p.isInGame = true;
            p.gameName = extractTag(xml, "gameName");
            p.gameIconUrl = extractTag(xml, "gameIcon");
            String gameLink = extractTag(xml, "gameLink");
            if (!TextUtils.isEmpty(gameLink)) {
                Matcher m = Pattern.compile("app/(\\d+)").matcher(gameLink);
                if (m.find()) {
                    p.gameId = m.group(1);
                }
            }
        } else if ("in-game".equalsIgnoreCase(onlineState) || (p.stateMessage != null && p.stateMessage.contains("In-Game"))) {
            p.isInGame = true;
            if (p.stateMessage != null && p.stateMessage.contains("<br/>")) {
                p.gameName = p.stateMessage.substring(p.stateMessage.indexOf("<br/>") + 5).replace("<![CDATA[", "").replace("]]>", "").trim();
            }
        } else {
            p.isInGame = false;
        }

        String customUrl = extractTag(xml, "customURL");
        if (!TextUtils.isEmpty(customUrl)) {
            p.profileUrl = "https://steamcommunity.com/id/" + customUrl;
        } else if (!TextUtils.isEmpty(p.steamId)) {
            p.profileUrl = "https://steamcommunity.com/profiles/" + p.steamId;
        }
        p.lastUpdated = System.currentTimeMillis();
        return p;
    }

    private String extractTag(String xml, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">(?:<\\!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?<\\/" + tag + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            String val = matcher.group(1).trim();
            return val.replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&#39;", "'");
        }
        return "";
    }

    /**
     * Publishes self profile to Supabase.
     */
    public void syncSelfToCloud(long userId, SteamProfile profile, Runnable onDone) {
        if (userId == 0 || profile == null || !isBroadcastEnabled()) {
            if (onDone != null) onDone.run();
            return;
        }

        profile.userId = userId;
        synchronized (profileCache) {
            profileCache.put(userId, profile);
        }

        // Persist locally
        saveSelfToCache(profile);

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(MiogramSupabaseBridge.DEFAULT_SUPABASE_URL + "/rest/v1/miogram_steam");
                conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setDoOutput(true);
                conn.setRequestProperty("apikey", MiogramSupabaseBridge.DEFAULT_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + MiogramSupabaseBridge.DEFAULT_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates");

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("steam_id", profile.steamId);
                json.put("persona_name", profile.personaName);
                json.put("avatar_url", profile.avatarUrl);
                json.put("profile_url", profile.profileUrl);
                json.put("game_id", profile.gameId);
                json.put("game_name", profile.gameName);
                json.put("game_icon_url", profile.gameIconUrl);
                json.put("game_hours_2weeks", profile.gameHours2Weeks);
                json.put("is_in_game", profile.isInGame);
                json.put("state_message", profile.stateMessage);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                conn.getResponseCode();
            } catch (Throwable t) {
                FileLog.e("MiogramSteamManager: syncSelfToCloud failed", t);
            } finally {
                if (conn != null) conn.disconnect();
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (onDone != null) onDone.run();
            });
        });
    }

    /**
     * Retrieves steam profile for any user from Supabase.
     */
    public void getProfile(long userId, ProfileCallback callback) {
        if (userId == 0) {
            if (callback != null) callback.onProfileLoaded(null);
            return;
        }

        // Return memory cached profile immediately if recent
        synchronized (profileCache) {
            SteamProfile cached = profileCache.get(userId);
            Long lastFetch = lastFetchTime.get(userId);
            long now = SystemClock.elapsedRealtime();
            if (cached != null && lastFetch != null && (now - lastFetch < 60000)) {
                if (callback != null) callback.onProfileLoaded(cached);
                return;
            }
        }

        // Fetch from Supabase
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(MiogramSupabaseBridge.DEFAULT_SUPABASE_URL + "/rest/v1/miogram_steam?user_id=eq." + userId + "&select=*");
                conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("apikey", MiogramSupabaseBridge.DEFAULT_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + MiogramSupabaseBridge.DEFAULT_ANON_KEY);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resp.append(line);
                    }
                    reader.close();

                    JSONArray arr = new JSONArray(resp.toString());
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        SteamProfile p = new SteamProfile();
                        p.userId = obj.optLong("user_id", userId);
                        p.steamId = obj.optString("steam_id", "");
                        p.personaName = obj.optString("persona_name", "");
                        p.avatarUrl = obj.optString("avatar_url", "");
                        p.profileUrl = obj.optString("profile_url", "");
                        p.gameId = obj.optString("game_id", "");
                        p.gameName = obj.optString("game_name", "");
                        p.gameIconUrl = obj.optString("game_icon_url", "");
                        p.gameHours2Weeks = obj.optString("game_hours_2weeks", "");
                        p.isInGame = obj.optBoolean("is_in_game", false);
                        p.stateMessage = obj.optString("state_message", "");
                        p.lastUpdated = System.currentTimeMillis();

                        synchronized (profileCache) {
                            profileCache.put(userId, p);
                            lastFetchTime.put(userId, SystemClock.elapsedRealtime());
                        }

                        AndroidUtilities.runOnUIThread(() -> {
                            if (callback != null) callback.onProfileLoaded(p);
                        });
                        return;
                    }
                }
            } catch (Throwable t) {
                FileLog.e("MiogramSteamManager: getProfile error", t);
            } finally {
                if (conn != null) conn.disconnect();
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) callback.onProfileLoaded(null);
            });
        });
    }

    public void openGame(Context context, String gameId) {
        if (context == null || TextUtils.isEmpty(gameId)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("steam://run/" + gameId));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            // Web fallback
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://store.steampowered.com/app/" + gameId));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Throwable ignore) {}
        }
    }

    public void addFriend(Context context, String steamId) {
        if (context == null || TextUtils.isEmpty(steamId)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("steam://friends/add/" + steamId));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            // Web fallback
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://steamcommunity.com/profiles/" + steamId));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Throwable ignore) {}
        }
    }

    public void openProfile(Context context, String profileUrl, String steamId) {
        if (context == null) return;
        String url = !TextUtils.isEmpty(profileUrl) ? profileUrl : ("https://steamcommunity.com/profiles/" + steamId);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("steam://url/SteamIDPage/" + steamId));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Throwable ignore) {}
        }
    }

    private void saveSelfToCache(SteamProfile p) {
        if (p == null) return;
        SharedPreferences.Editor ed = getPrefs().edit();
        ed.putString("self_steam_id", p.steamId);
        ed.putString("self_persona_name", p.personaName);
        ed.putString("self_avatar_url", p.avatarUrl);
        ed.putString("self_game_id", p.gameId);
        ed.putString("self_game_name", p.gameName);
        ed.putString("self_game_icon", p.gameIconUrl);
        ed.putString("self_game_hours", p.gameHours2Weeks);
        ed.putBoolean("self_is_in_game", p.isInGame);
        ed.apply();
    }

    private void loadSelfFromCache() {
        SharedPreferences p = getPrefs();
        String steamId = p.getString("self_steam_id", "");
        if (TextUtils.isEmpty(steamId)) return;

        SteamProfile self = new SteamProfile();
        self.steamId = steamId;
        self.personaName = p.getString("self_persona_name", "");
        self.avatarUrl = p.getString("self_avatar_url", "");
        self.gameId = p.getString("self_game_id", "");
        self.gameName = p.getString("self_game_name", "");
        self.gameIconUrl = p.getString("self_game_icon", "");
        self.gameHours2Weeks = p.getString("self_game_hours", "");
        self.isInGame = p.getBoolean("self_is_in_game", false);

        long myId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (myId != 0) {
            synchronized (profileCache) {
                profileCache.put(myId, self);
            }
        }
    }
}
