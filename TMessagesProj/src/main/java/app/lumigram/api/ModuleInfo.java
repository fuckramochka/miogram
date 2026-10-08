package app.lumigram.api;

import org.json.JSONObject;

/**
 * Маніфест модуля — вміст module.json всередині .lumi-архіву.
 * Поле entry — повне імʼя класу, що реалізує {@link LumiModule}.
 */
public final class ModuleInfo {

    public final String id;
    public final String version;
    public final int minCore;
    public final int api;
    public final String entry;
    public final String title;

    public ModuleInfo(String id, String version, int minCore, int api, String entry, String title) {
        this.id = id;
        this.version = version;
        this.minCore = minCore;
        this.api = api;
        this.entry = entry;
        this.title = title;
    }

    public static ModuleInfo parse(JSONObject o) {
        if (o == null) {
            return null;
        }
        String id = o.optString("id", "");
        String entry = o.optString("entry", "");
        if (id.isEmpty() || entry.isEmpty()) {
            return null;
        }
        return new ModuleInfo(
                id,
                o.optString("version", "0.0.0"),
                o.optInt("minCore", 1),
                o.optInt("api", 1),
                entry,
                o.optString("title", id));
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("version", version);
            o.put("minCore", minCore);
            o.put("api", api);
            o.put("entry", entry);
            o.put("title", title);
        } catch (Exception ignored) {
        }
        return o;
    }
}
