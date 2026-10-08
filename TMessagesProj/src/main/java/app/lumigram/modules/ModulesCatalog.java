package app.lumigram.modules;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Каталог модулів у хмарі: modules.json поруч із релізами.
 * Формат: {"modules":[{id,version,title,url,sha256,size,minCore,signature}]}.
 * Мережа — той самий OkHttp-патерн, що й в апдейтері.
 */
public final class ModulesCatalog {

    /** ЗАМІНИТИ на реальний URL після першого релізу модулів. */
    public static final String DEFAULT_URL = "https://github.com/lumigram/lumigram/releases/latest/download/modules.json";

    public static final class Entry {
        public String id;
        public String version;
        public String title;
        public String url;
        public String sha256;
        public long size;
        public int minCore;
        public String signature;
    }

    public interface Callback {
        void onResult(List<Entry> entries, boolean failed);
    }

    private static volatile OkHttpClient client;

    private ModulesCatalog() {
    }

    private static OkHttpClient client() {
        if (client == null) {
            synchronized (ModulesCatalog.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return client;
    }

    public static void fetch(String url, Callback callback) {
        final String target = url == null || url.isEmpty() ? DEFAULT_URL : url;
        new Thread(() -> {
            List<Entry> out = new ArrayList<>();
            boolean failed = false;
            try {
                Request request = new Request.Builder().url(target)
                        .header("Accept", "application/json")
                        .header("User-Agent", "lumigram")
                        .build();
                try (Response response = client().newCall(request).execute()) {
                    ResponseBody body = response.body();
                    if (!response.isSuccessful() || body == null) {
                        throw new IllegalStateException("HTTP " + response.code());
                    }
                    JSONObject root = new JSONObject(body.string());
                    JSONArray arr = root.optJSONArray("modules");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null || o.optString("id", "").isEmpty()) {
                                continue;
                            }
                            Entry e = new Entry();
                            e.id = o.optString("id");
                            e.version = o.optString("version", "0.0.0");
                            e.title = o.optString("title", e.id);
                            e.url = o.optString("url", "");
                            e.sha256 = o.optString("sha256", "");
                            e.size = o.optLong("size", 0);
                            e.minCore = o.optInt("minCore", 1);
                            e.signature = o.optString("signature", "");
                            if (!e.url.isEmpty()) {
                                out.add(e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                failed = true;
                FileLog.e("LumiModules: catalog fetch failed", e);
            }
            final List<Entry> res = out;
            final boolean err = failed;
            AndroidUtilities.runOnUIThread(() -> callback.onResult(res, err));
        }, "lumi-modules-catalog").start();
    }
}
