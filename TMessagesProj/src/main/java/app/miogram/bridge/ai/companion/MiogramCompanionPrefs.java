package app.miogram.bridge.ai.companion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Preferences and state persistence for Ame and KAngel AI companions.
 * Tracks active companion, bottom-bar tab replacement, chat history, and NSO stats.
 */
public class MiogramCompanionPrefs {

    private static final String PREFS_NAME = "miogram_companion_prefs";
    private static final String KEY_COMPANION = "active_companion";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed_v1";
    private static final String KEY_REPLACE_CONTACTS = "replace_contacts_with_ai";
    private static final String KEY_HISTORY = "chat_history_json";
    private static final String KEY_AFFECTION = "stat_affection";
    private static final String KEY_STRESS = "stat_stress";
    private static final String KEY_DARKNESS = "stat_darkness";
    private static final String KEY_FOLLOWERS = "stat_followers";

    public static final String COMPANION_AME = "ame";
    public static final String COMPANION_KANGEL = "kangel";

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getActiveCompanion() {
        return getPrefs().getString(KEY_COMPANION, COMPANION_AME);
    }

    public static void setActiveCompanion(String companion) {
        getPrefs().edit().putString(KEY_COMPANION, companion).apply();
    }

    public static boolean isAmeActive() {
        return COMPANION_AME.equalsIgnoreCase(getActiveCompanion());
    }

    public static boolean isContactsReplacedWithAi() {
        return getPrefs().getBoolean(KEY_REPLACE_CONTACTS, false);
    }

    public static void setContactsReplacedWithAi(boolean replaced) {
        getPrefs().edit().putBoolean(KEY_REPLACE_CONTACTS, replaced).apply();
    }

    public static boolean hasCompletedOnboarding() {
        return getPrefs().getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(boolean completed) {
        getPrefs().edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static int getAffection() {
        return getPrefs().getInt(KEY_AFFECTION, 85);
    }

    public static void setAffection(int value) {
        getPrefs().edit().putInt(KEY_AFFECTION, Math.max(0, Math.min(100, value))).apply();
    }

    public static int getStress() {
        return getPrefs().getInt(KEY_STRESS, 25);
    }

    public static void setStress(int value) {
        getPrefs().edit().putInt(KEY_STRESS, Math.max(0, Math.min(100, value))).apply();
    }

    public static int getDarkness() {
        return getPrefs().getInt(KEY_DARKNESS, 40);
    }

    public static void setDarkness(int value) {
        getPrefs().edit().putInt(KEY_DARKNESS, Math.max(0, Math.min(100, value))).apply();
    }

    public static long getFollowers() {
        return getPrefs().getLong(KEY_FOLLOWERS, 1337420L);
    }

    public static void addFollowers(long count) {
        long current = getFollowers();
        getPrefs().edit().putLong(KEY_FOLLOWERS, Math.max(0, current + count)).apply();
    }

    public static class ChatMessage {
        public final boolean isUser;
        public final String text;
        public final String mood;
        public final long timestamp;
        public final String toolAction;
        public final String toolParams;
        public boolean actionApproved;
        public boolean actionExecuted;

        public ChatMessage(boolean isUser, String text, String mood, long timestamp, String toolAction, String toolParams) {
            this.isUser = isUser;
            this.text = text;
            this.mood = mood != null ? mood : "neutral";
            this.timestamp = timestamp;
            this.toolAction = toolAction;
            this.toolParams = toolParams;
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("isUser", isUser);
                obj.put("text", text);
                obj.put("mood", mood);
                obj.put("timestamp", timestamp);
                if (toolAction != null) obj.put("toolAction", toolAction);
                if (toolParams != null) obj.put("toolParams", toolParams);
                obj.put("actionApproved", actionApproved);
                obj.put("actionExecuted", actionExecuted);
                return obj;
            } catch (Throwable t) {
                return null;
            }
        }

        public static ChatMessage fromJson(JSONObject obj) {
            if (obj == null) return null;
            boolean isUser = obj.optBoolean("isUser");
            String text = obj.optString("text");
            String mood = obj.optString("mood", "neutral");
            long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
            String toolAction = obj.optString("toolAction", null);
            String toolParams = obj.optString("toolParams", null);
            ChatMessage msg = new ChatMessage(isUser, text, mood, timestamp, toolAction, toolParams);
            msg.actionApproved = obj.optBoolean("actionApproved", false);
            msg.actionExecuted = obj.optBoolean("actionExecuted", false);
            return msg;
        }
    }

    public static List<ChatMessage> loadHistory() {
        List<ChatMessage> list = new ArrayList<>();
        String jsonStr = getPrefs().getString(KEY_HISTORY, null);
        if (jsonStr == null || jsonStr.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                ChatMessage m = ChatMessage.fromJson(arr.getJSONObject(i));
                if (m != null) list.add(m);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return list;
    }

    public static void saveHistory(List<ChatMessage> messages) {
        if (messages == null) return;
        try {
            JSONArray arr = new JSONArray();
            int start = Math.max(0, messages.size() - 60);
            for (int i = start; i < messages.size(); i++) {
                JSONObject obj = messages.get(i).toJson();
                if (obj != null) arr.put(obj);
            }
            getPrefs().edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static void clearHistory() {
        getPrefs().edit().remove(KEY_HISTORY).apply();
    }
}
