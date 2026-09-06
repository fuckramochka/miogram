package app.miogram.bridge.ai;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tw.nekomimi.nekogram.config.ConfigItem;
import tw.nekomimi.nekogram.llm.LlmConfig;
import tw.nekomimi.nekogram.llm.preset.PresetRegistry;
import xyz.nextalone.nagram.NaConfig;

/**
 * High-performance Miogram AI service for text summarization, rephrasing and voice transcription.
 * Fully synchronized key, model, and provider vault across Miogram AI and Neko Translator.
 */
public class MiogramAiService {

    private static final Gson gson = new Gson();
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String AI_PREFS = "miogram_ai_prefs";
    private static final String KEY_API_KEYS = "gemini_api_keys";
    private static final AtomicInteger apiKeyCursor = new AtomicInteger();
    // Base64 expands data; stay well below Gemini's 20 MB inline audio limit.
    private static final long MAX_INLINE_AUDIO_BYTES = 14L * 1024L * 1024L;

    public static int getProvider() {
        try {
            return NaConfig.INSTANCE.getLlmProviderPreset().Int();
        } catch (Throwable ignored) {
            return PresetRegistry.GOOGLE_AI_STUDIO;
        }
    }

    public static void setProvider(int provider) {
        try {
            NaConfig.INSTANCE.getLlmProviderPreset().setConfigInt(provider);
        } catch (Throwable ignored) {}
    }

    public static boolean hasApiKey() {
        return !getApiKeys().isEmpty();
    }

    /** Returns every configured key while preserving legacy single-key settings. */
    public static List<String> getApiKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        try {
            ConfigItem item = LlmConfig.getApiKeyConfigItem(PresetRegistry.GOOGLE_AI_STUDIO);
            if (item != null) addApiKeys(keys, item.String());
        } catch (Throwable ignored) {}

        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                android.content.SharedPreferences prefs = ctx.getSharedPreferences(AI_PREFS, Context.MODE_PRIVATE);
                addApiKeys(keys, prefs.getString(KEY_API_KEYS, ""));
                addApiKeys(keys, prefs.getString("gemini_api_key", ""));
                addApiKeys(keys, prefs.getString("gemini_key", ""));
            }
        } catch (Throwable ignored) {}

        try {
            addApiKeys(keys, NaConfig.INSTANCE.getTranscribeProviderGeminiApiKey().String());
            addApiKeys(keys, NaConfig.INSTANCE.getLlmProviderGeminiKey().String());
        } catch (Throwable ignored) {}
        return new ArrayList<>(keys);
    }

    public static String getApiKey() {
        List<String> keys = getApiKeys();
        if (keys.isEmpty()) return "";
        return keys.get(Math.floorMod(apiKeyCursor.getAndIncrement(), keys.size()));
    }

    public static void setApiKey(String key) {
        ArrayList<String> keys = new ArrayList<>();
        if (key != null) keys.add(key);
        setApiKeys(keys);
    }

    /** Saves a normalized keyring and keeps legacy consumers on the primary key. */
    public static void setApiKeys(List<String> rawKeys) {
        ArrayList<String> keys = sanitizeApiKeys(rawKeys);
        String serialized = TextUtils.join("\n", keys);
        String primary = keys.isEmpty() ? "" : keys.get(0);
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                ctx.getSharedPreferences(AI_PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_API_KEYS, serialized)
                        .putString("gemini_api_key", primary)
                        .putString("gemini_key", primary)
                        .apply();
            }
        } catch (Throwable ignored) {}

        try {
            ConfigItem item = LlmConfig.getApiKeyConfigItem(PresetRegistry.GOOGLE_AI_STUDIO);
            if (item != null) {
                item.setConfigString(TextUtils.join(",", keys));
            }
            NaConfig.INSTANCE.getTranscribeProviderGeminiApiKey().setConfigString(primary);
            NaConfig.INSTANCE.getLlmProviderGeminiKey().setConfigString(primary);
        } catch (Throwable ignored) {}
    }

    public static List<String> parseApiKeys(String raw) {
        ArrayList<String> input = new ArrayList<>();
        if (raw != null) input.add(raw);
        return sanitizeApiKeys(input);
    }

    private static void addApiKeys(LinkedHashSet<String> target, String raw) {
        if (TextUtils.isEmpty(raw)) return;
        for (String part : raw.split("[,\\n\\r]+")) {
            String key = part.trim();
            if (!key.isEmpty()) target.add(key);
        }
    }

    private static ArrayList<String> sanitizeApiKeys(List<String> rawKeys) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (rawKeys != null) {
            for (String raw : rawKeys) addApiKeys(keys, raw);
        }
        return new ArrayList<>(keys);
    }

    public static String getModel() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                String model = ctx.getSharedPreferences("miogram_ai_prefs", Context.MODE_PRIVATE)
                        .getString("gen_model", "");
                if (!TextUtils.isEmpty(model)) return model;
            }
        } catch (Throwable ignored) {}

        try {
            String model = LlmConfig.getEffectiveModelName(PresetRegistry.GOOGLE_AI_STUDIO);
            if (!TextUtils.isEmpty(model)) return model;
        } catch (Throwable ignored) {}
        return "gemini-2.0-flash";
    }

    public static void setModel(String model) {
        String trimmed = model != null ? model.trim() : "";
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                ctx.getSharedPreferences("miogram_ai_prefs", Context.MODE_PRIVATE).edit()
                        .putString("gen_model", trimmed)
                        .apply();
            }
        } catch (Throwable ignored) {}

        try {
            LlmConfig.setSavedModelName(PresetRegistry.GOOGLE_AI_STUDIO, trimmed);
        } catch (Throwable ignored) {}
    }

    /**
     * Generate 3-4 bullet point summary of a long message or post.
     */
    public static void summarizeText(String text, Utilities.Callback<String> callback) {
        String prompt = "Зроби короткий, структурований і чіткий стислий зміст (3-4 головні тези з маркерами •) цього тексту українською мовою:\n\n" + text;
        generateContent(prompt, getModel(), (res, err) -> {
            if (res != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(res));
            } else if (err != null && (err.contains("404") || err.contains("400") || err.contains("503"))) {
                // Hierarchical fallback: 3.5-flash-lite -> 3.1-flash-lite -> 2.5-flash
                String curModel = getModel();
                String fb = "gemini-3.1-flash-lite".equals(curModel) ? "gemini-2.5-flash" : "gemini-3.1-flash-lite";
                generateContent(prompt, fb, (fallbackRes, fallbackErr) -> {
                    if (fallbackRes != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.run(fallbackRes));
                    } else {
                        generateContent(prompt, "gemini-2.5-flash", (fb2Res, fb2Err) -> {
                            AndroidUtilities.runOnUIThread(() -> callback.run(fb2Res));
                        });
                    }
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
        List<String> apiKeys = getApiKeys();
        if (apiKeys.isEmpty()) {
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
                RequestBody body = RequestBody.create(json, JSON);
                String lastError = "No usable API key";
                int start = Math.floorMod(apiKeyCursor.getAndIncrement(), apiKeys.size());
                for (int offset = 0; offset < apiKeys.size(); offset++) {
                    String apiKey = apiKeys.get((start + offset) % apiKeys.size());
                    Request request = new Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                            .header("x-goog-api-key", apiKey)
                            .post(body)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            lastError = "Error " + response.code() + ": " + truncateError(respStr);
                            if (response.code() == 401 || response.code() == 403 || response.code() == 429) {
                                continue;
                            }
                            FileLog.e("MiogramAiService error: " + lastError);
                            callback.run(null, lastError);
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
                                        if (candParts.size() > 0 && candParts.get(0).getAsJsonObject().has("text")) {
                                            String result = candParts.get(0).getAsJsonObject().get("text").getAsString();
                                            callback.run(result.trim(), null);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        callback.run(null, "Empty response from Gemini");
                        return;
                    }
                }
                callback.run(null, lastError);
            } catch (Exception e) {
                FileLog.e(e);
                callback.run(null, e.getMessage());
            }
        });
    }

    /**
     * Sends a downloaded audio track to Gemini for a real LRC transcription.
     * This deliberately refuses large files rather than creating placeholder lyrics.
     */
    public static void transcribeAudio(File audioFile, String mimeType, String title, String artist,
                                       int durationSeconds, Utilities.Callback2<String, String> callback) {
        if (audioFile == null || !audioFile.isFile() || audioFile.length() <= 0) {
            callback.run(null, "Audio file is not downloaded yet");
            return;
        }
        if (audioFile.length() > MAX_INLINE_AUDIO_BYTES) {
            callback.run(null, "Audio file is too large for AI transcription (maximum 14 MB)");
            return;
        }
        List<String> apiKeys = getApiKeys();
        if (apiKeys.isEmpty()) {
            callback.run(null, "No Gemini API key configured");
            return;
        }

        executor.submit(() -> {
            byte[] audioBytes = null;
            try (FileInputStream input = new FileInputStream(audioFile)) {
                audioBytes = new byte[(int) audioFile.length()];
                int offset = 0;
                while (offset < audioBytes.length) {
                    int read = input.read(audioBytes, offset, audioBytes.length - offset);
                    if (read < 0) break;
                    offset += read;
                }
                if (offset != audioBytes.length) {
                    callback.run(null, "Could not read the complete audio file");
                    return;
                }

                String prompt = "Transcribe the lyrics in this audio track. Return only standard LRC lines "
                        + "in the exact format [mm:ss.xx] lyric text, one line per timestamp. "
                        + "Do not use Markdown, headings, translations, descriptions, or invented words. "
                        + "If there are no confidently intelligible lyrics, return exactly [00:00.00] [Instrumental]. "
                        + "Track metadata: title=" + (title == null ? "" : title)
                        + ", artist=" + (artist == null ? "" : artist)
                        + ", duration=" + Math.max(0, durationSeconds) + " seconds.";

                JsonObject root = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", prompt);
                parts.add(textPart);
                JsonObject audioPart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", TextUtils.isEmpty(mimeType) ? "audio/mpeg" : mimeType);
                inlineData.addProperty("data", Base64.encodeToString(audioBytes, Base64.NO_WRAP));
                audioPart.add("inlineData", inlineData);
                parts.add(audioPart);
                content.add("parts", parts);
                contents.add(content);
                root.add("contents", contents);

                RequestBody body = RequestBody.create(gson.toJson(root), JSON);
                String lastError = "No usable Gemini API key";
                int start = Math.floorMod(apiKeyCursor.getAndIncrement(), apiKeys.size());
                for (int offsetKey = 0; offsetKey < apiKeys.size(); offsetKey++) {
                    String apiKey = apiKeys.get((start + offsetKey) % apiKeys.size());
                    Request request = new Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/" + getModel() + ":generateContent")
                            .header("x-goog-api-key", apiKey)
                            .post(body)
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            lastError = "Error " + response.code() + ": " + truncateError(responseBody);
                            if (response.code() == 401 || response.code() == 403 || response.code() == 429) continue;
                            callback.run(null, lastError);
                            return;
                        }
                        String lrc = extractGeneratedText(responseBody);
                        if (!TextUtils.isEmpty(lrc)) {
                            callback.run(lrc.trim(), null);
                            return;
                        }
                        callback.run(null, "Gemini returned no transcription text");
                        return;
                    }
                }
                callback.run(null, lastError);
            } catch (Exception e) {
                FileLog.e(e);
                callback.run(null, e.getMessage() != null ? e.getMessage() : "Audio transcription failed");
            } finally {
                if (audioBytes != null) java.util.Arrays.fill(audioBytes, (byte) 0);
            }
        });
    }

    private static String extractGeneratedText(String responseBody) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root == null || !root.has("candidates")) return "";
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) return "";
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            if (content == null || !content.has("parts")) return "";
            JsonArray parts = content.getAsJsonArray("parts");
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part.has("text")) return part.get("text").getAsString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String truncateError(String error) {
        if (error == null) return "";
        return error.length() > 512 ? error.substring(0, 512) : error;
    }

    /**
     * Specialized raw execution for Smart Feed with ad-filtering and structured digest,
     * maintaining the hierarchical fallback chain without overriding user instructions.
     */
    public static void processFeedWithAi(String customPrompt, Utilities.Callback<String> callback) {
        generateContent(customPrompt, getModel(), (res, err) -> {
            if (res != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(res));
            } else if (err != null && (err.contains("404") || err.contains("400") || err.contains("503"))) {
                String curModel = getModel();
                String fb = "gemini-3.1-flash-lite".equals(curModel) ? "gemini-2.5-flash" : "gemini-3.1-flash-lite";
                generateContent(customPrompt, fb, (fb1Res, fb1Err) -> {
                    if (fb1Res != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.run(fb1Res));
                    } else {
                        generateContent(customPrompt, "gemini-2.5-flash", (fb2Res, fb2Err) -> {
                            AndroidUtilities.runOnUIThread(() -> callback.run(fb2Res));
                        });
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        });
    }

    /**
     * General text generation for digests, analysis, and custom prompts.
     */
    public static void generateText(String prompt, Utilities.Callback2<String, String> callback) {
        generateContent(prompt, getModel(), (res, err) -> {
            if (res != null) {
                callback.run(res, null);
            } else if (err != null && (err.contains("404") || err.contains("400") || err.contains("503"))) {
                String curModel = getModel();
                String fb = "gemini-3.1-flash-lite".equals(curModel) ? "gemini-2.5-flash" : "gemini-3.1-flash-lite";
                generateContent(prompt, fb, (fb1Res, fb1Err) -> {
                    if (fb1Res != null) {
                        callback.run(fb1Res, null);
                    } else {
                        generateContent(prompt, "gemini-2.5-flash", (fb2Res, fb2Err) -> {
                            if (fb2Res != null) {
                                callback.run(fb2Res, null);
                            } else {
                                callback.run(null, fb2Err != null ? fb2Err : err);
                            }
                        });
                    }
                });
            } else {
                callback.run(null, err);
            }
        });
    }

}
