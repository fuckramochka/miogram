package app.miogram.bridge.localizer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance string customization and localization engine for Miogram.
 * Allows users to inspect, override, export, and import any interface string in real time.
 */
public class MiogramLocalizerEngine {

    private static final String PREFS_NAME = "miogram_localizer_overrides";
    private static final Map<String, String> overrides = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    public static class StringEntry {
        public final String key;
        public final String originalValue;
        public String customValue;
        public final int resId;

        public StringEntry(String key, String originalValue, String customValue, int resId) {
            this.key = key;
            this.originalValue = originalValue;
            this.customValue = customValue;
            this.resId = resId;
        }

        public boolean isOverridden() {
            return customValue != null && !customValue.equals(originalValue);
        }

        public String getDisplayValue() {
            return customValue != null ? customValue : (originalValue != null ? originalValue : "");
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (MiogramLocalizerEngine.class) {
            if (loaded) return;
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                Map<String, ?> all = sp.getAll();
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        overrides.put(entry.getKey(), (String) entry.getValue());
                    }
                }
            }
            loaded = true;
        }
    }

    public static String getOverride(String key) {
        if (key == null) return null;
        ensureLoaded();
        return overrides.get(key);
    }

    public static void setOverride(String key, String value) {
        if (key == null) return;
        ensureLoaded();
        Context ctx = ApplicationLoader.applicationContext;
        if (value == null || value.trim().isEmpty()) {
            removeOverride(key);
            return;
        }
        overrides.put(key, value);
        if (ctx != null) {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, value)
                    .apply();
        }
    }

    public static void removeOverride(String key) {
        if (key == null) return;
        ensureLoaded();
        overrides.remove(key);
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx != null) {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(key)
                    .apply();
        }
    }

    public static void clearAll() {
        ensureLoaded();
        overrides.clear();
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx != null) {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        }
    }

    public static int getOverridesCount() {
        ensureLoaded();
        return overrides.size();
    }

    public static Map<String, String> getAllOverrides() {
        ensureLoaded();
        return new HashMap<>(overrides);
    }

    public static String exportToJson() {
        ensureLoaded();
        JSONObject obj = new JSONObject(overrides);
        return obj.toString();
    }

    public static int importFromJson(String json) {
        ensureLoaded();
        int count = 0;
        try {
            JSONObject obj = new JSONObject(json);
            Context ctx = ApplicationLoader.applicationContext;
            SharedPreferences.Editor editor = ctx != null ? ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit() : null;
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                String v = obj.optString(k, null);
                if (v != null) {
                    overrides.put(k, v);
                    if (editor != null) editor.putString(k, v);
                    count++;
                }
            }
            if (editor != null) editor.apply();
        } catch (Throwable ignored) {
        }
        return count;
    }

    /**
     * Extracts all known string keys across R.string and LocaleController dictionaries.
     */
    public static List<StringEntry> loadAllStrings(Context context) {
        ensureLoaded();
        Map<String, StringEntry> map = new HashMap<>();

        // 1. Scan R.string resources
        Field[] fields = R.string.class.getFields();
        for (Field f : fields) {
            try {
                String name = f.getName();
                int id = f.getInt(null);
                String orig = null;
                try {
                    orig = context.getString(id);
                } catch (Exception ignored) {}

                String custom = overrides.get(name);
                map.put(name, new StringEntry(name, orig, custom, id));
            } catch (Exception ignored) {}
        }

        // 2. Scan active custom overrides that may not be in R.string
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                map.put(entry.getKey(), new StringEntry(entry.getKey(), null, entry.getValue(), 0));
            }
        }

        List<StringEntry> list = new ArrayList<>(map.values());
        Collections.sort(list, (o1, o2) -> {
            if (o1.isOverridden() && !o2.isOverridden()) return -1;
            if (!o1.isOverridden() && o2.isOverridden()) return 1;
            return o1.key.compareToIgnoreCase(o2.key);
        });
        return list;
    }
}
