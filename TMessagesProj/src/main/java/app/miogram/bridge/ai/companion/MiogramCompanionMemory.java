package app.miogram.bridge.ai.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import app.miogram.bridge.github.MiogramGitHubManager;
import app.miogram.bridge.steam.MiogramSteamManager;

/**
 * Long-term persistent semantic memory for Ame-chan and KAngel.
 * Autonomously stores user habits, preferences, facts, and context across sessions.
 * Automatically seeded with Telegram identity and tracked configurations.
 */
public class MiogramCompanionMemory {

    private static final String PREFS_NAME = miogram_companion_memory;
    private static final String KEY_FACTS_JSON = companion_facts_v1;

    private static volatile MiogramCompanionMemory instance;
    private final Map<String, String> memoryMap = new HashMap<>();
    private boolean initialized = false;

    public static MiogramCompanionMemory getInstance() {
        if (instance == null) {
            synchronized (MiogramCompanionMemory.class) {
                if (instance == null) {
                    instance = new MiogramCompanionMemory();
                }
            }
        }
        return instance;
    }

    private MiogramCompanionMemory() {
        loadMemory();
    }

    private synchronized void loadMemory() {
        if (initialized) return;
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String jsonStr = prefs.getString(KEY_FACTS_JSON, null);
                if (!TextUtils.isEmpty(jsonStr)) {
                    JSONObject obj = new JSONObject(jsonStr);
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        memoryMap.put(k, obj.optString(k));
                    }
                }
            }
        } catch (Throwable ignore) {}
        initialized = true;
    }

    private synchronized void persistMemory() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                JSONObject obj = new JSONObject();
                for (Map.Entry<String, String> e : memoryMap.entrySet()) {
                    obj.put(e.getKey(), e.getValue());
                }
                SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_FACTS_JSON, obj.toString()).apply();
            }
        } catch (Throwable ignore) {}
    }

    public synchronized void setFact(String key, String value) {
        loadMemory();
        if (TextUtils.isEmpty(key)) return;
        if (value == null || value.trim().isEmpty()) {
            memoryMap.remove(key.trim());
        } else {
            memoryMap.put(key.trim(), value.trim());
        }
        persistMemory();
    }

    public synchronized String getFact(String key) {
        loadMemory();
        if (TextUtils.isEmpty(key)) return null;
        return memoryMap.get(key.trim());
    }

    public synchronized void removeFact(String key) {
        loadMemory();
        if (TextUtils.isEmpty(key)) return;
        memoryMap.remove(key.trim());
        persistMemory();
    }

    public synchronized void clearMemory() {
        loadMemory();
        memoryMap.clear();
        persistMemory();
    }

    public synchronized Map<String, String> getAllFacts() {
        loadMemory();
        return Collections.unmodifiableMap(new HashMap<>(memoryMap));
    }

    /**
     * Builds dynamic markdown context for system prompt injection.
     */
    public synchronized String getMemoryContextForPrompt(int account) {
        loadMemory();
        StringBuilder sb = new StringBuilder();
        sb.append(### LONG-TERM MEMORY & P-CHAN CONTEXT (Learned Facts):\n);

        // Dynamic auto-context from Telegram environment
        try {
            TLRPC.User me = UserConfig.getInstance(account).getCurrentUser();
            if (me != null) {
                sb.append(- pchan_name: ).append(UserObject.getUserName(me)).append(\n);
                if (!TextUtils.isEmpty(me.username)) {
                    sb.append(- pchan_telegram_username: @).append(me.username).append(\n);
                }
            }
        } catch (Throwable ignore) {}

        try {
            String repo = MiogramGitHubManager.getInstance().getTrackedRepo();
            if (!TextUtils.isEmpty(repo)) {
                sb.append(- tracked_github_repo: ).append(repo).append(\n);
            }
        } catch (Throwable ignore) {}

        try {
            String steamId = MiogramSteamManager.getInstance().getSelfSteamId();
            if (!TextUtils.isEmpty(steamId)) {
                sb.append(- pchan_steam_id: ).append(steamId).append(\n);
            }
        } catch (Throwable ignore) {}

        if (memoryMap.isEmpty()) {
            sb.append(- (No custom facts learned yet. Autonomously use remember_fact to memorize new things about P-chan!)\n);
        } else {
            for (Map.Entry<String, String> entry : memoryMap.entrySet()) {
                sb.append(- ).append(entry.getKey()).append(: ).append(entry.getValue()).append(\n);
            }
        }
        sb.append(\n);
        return sb.toString();
    }
}
