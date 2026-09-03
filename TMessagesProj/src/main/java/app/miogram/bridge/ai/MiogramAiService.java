package app.miogram.bridge.ai;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import xyz.nextalone.nagram.NaConfig;

/**
 * High-performance Miogram AI service for text summarization, rephrasing and voice transcription.
 * Uses Google Gemini API with smart fallback and unified key vault.
 */
public class MiogramAiService {

    private static final Gson gson = new Gson();
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static String getApiKey() {
        String key = "";
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                key = ctx.getSharedPreferences("miogram_ai_prefs", Context.MODE_PRIVATE).getString("gemini_api_key", "");
                if (TextUtils.isEmpty(key)) {
                    key = ctx.getSharedPreferences("miogram_ai_prefs", Context.MODE_PRIVATE).getString("gemini_key", "");
                }
            }
        } catch (Throwable ignored) {}

        if (TextUtils.isEmpty(key)) {
            key = NaConfig.INSTANCE.getTranscribeProviderGeminiApiKey().String();
        }
        if (TextUtils.isEmpty(key)) {
            try {
                String llmKey = NaConfig.INSTANCE.getLlmProviderGeminiKey().String();
                if (!TextUtils.isEmpty(llmKey)) {
                    key = llmKey.split(",")[0].trim();
                }
            } catch (Throwable ignored) {}
        }
        return key != null ? key.trim() : "";
    }

    public static String getModel() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                return ctx.getSharedPreferences("miogram_ai_prefs", Context.MODE_PRIVATE)
                        .getString("gen_model", "gemini-3.5-flash-lite");
            }
        } catch (Throwable ignored) {}
        return "gemini-3.5-flash-lite";
    }

    /**
     * Generate 3-4 bullet point summary of a long message or post.
     */
    public static void summarizeText(String text, Utilities.Callback<String> callback) {
        String prompt = "Зроби короткий, структурований і чіткий стислий зміст (3-4 головні тези з маркерами •) цього тексту українською мовою:\n\n" + text;
        generateContent(prompt, getModel(), (res, err) -> {
            if (res != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(res));
            } else if (err != null && err.contains("404") && !"gemini-2.5-flash".equals(getModel())) {
                // Fallback to gemini-2.5-flash on 404
                generateContent(prompt, "gemini-2.5-flash", (fallbackRes, fallbackErr) -> {
                    AndroidUtilities.runOnUIThread(() -> callback.run(fallbackRes));
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        });
    }

    /**
     * Rephrase / Improve / Grammar check text.
     */
    public static void rephraseText(String text, String tone, Utilities.Callback<String> callback) {
        String prompt = "Перепиши та покращ цей текст українською мовою у стилі '" + tone + "'. Виправ граматичні помилки та збережи суть:\n\n" + text;
        generateContent(prompt, getModel(), (res, err) -> {
            if (res != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(res));
            } else if (err != null && err.contains("404") && !"gemini-2.5-flash".equals(getModel())) {
                generateContent(prompt, "gemini-2.5-flash", (fallbackRes, fallbackErr) -> {
                    AndroidUtilities.runOnUIThread(() -> callback.run(fallbackRes));
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        });
    }

    private static void generateContent(String prompt, String model, Utilities.Callback2<String, String> callback) {
        String apiKey = getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            callback.run(null, "No API key configured");
            return;
        }

        executor.submit(() -> {
            try {
                JsonObject root = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                root.add("contents", contents);

                String json = gson.toJson(root);
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String respStr = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        FileLog.e("MiogramAiService error: " + response.code() + " " + respStr);
                        callback.run(null, "Error " + response.code() + ": " + respStr);
                        return;
                    }

                    JsonObject resJson = gson.fromJson(respStr, JsonObject.class);
                    if (resJson != null && resJson.has("candidates")) {
                        JsonArray candidates = resJson.getAsJsonArray("candidates");
                        if (candidates.size() > 0) {
                            JsonObject cand = candidates.get(0).getAsJsonObject();
                            if (cand.has("content")) {
                                JsonObject candContent = cand.getAsJsonObject("content");
                                if (candContent.has("parts")) {
                                    JsonArray candParts = candContent.getAsJsonArray("parts");
                                    if (candParts.size() > 0) {
                                        String result = candParts.get(0).getAsJsonObject().get("text").getAsString();
                                        callback.run(result.trim(), null);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    callback.run(null, "Empty response from Gemini");
                }
            } catch (Exception e) {
                FileLog.e(e);
                callback.run(null, e.getMessage());
            }
        });
    }
}
