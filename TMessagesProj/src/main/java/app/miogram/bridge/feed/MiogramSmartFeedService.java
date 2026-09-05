package app.miogram.bridge.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.miogram.bridge.ai.MiogramAiService;

public class MiogramSmartFeedService {

    private static final String PREFS_NAME = "miogram_smart_feed_prefs";
    private static final String KEY_CHANNELS = "tracked_channels";
    private static final String KEY_CACHED_FEED = "cached_feed_json";
    private static final Gson gson = new Gson();

    public static class FeedItem {
        public long dialogId;
        public int messageId;
        public String channelTitle;
        public String title;
        public String summary;
        public String category;
        public long timestamp;
        public boolean hasPhoto;
        public TLRPC.Message originalMessage;

        public FeedItem() {}

        public FeedItem(long dialogId, int messageId, String channelTitle, String title, String summary, String category, long timestamp) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.channelTitle = channelTitle;
            this.title = title;
            this.summary = summary;
            this.category = category;
            this.timestamp = timestamp;
        }
    }

    public static class RawAiFeedItem {
        public int message_id;
        public String title;
        public String summary;
        public String category;
        public boolean is_ad;
    }

    public static Set<Long> getTrackedChannels() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return new HashSet<>();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> strSet = prefs.getStringSet(KEY_CHANNELS, new HashSet<>());
        Set<Long> result = new HashSet<>();
        for (String s : strSet) {
            try {
                result.add(Long.parseLong(s));
            } catch (Exception ignored) {}
        }
        return result;
    }

    public static void setTrackedChannels(Set<Long> channels) {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> strSet = new HashSet<>();
        for (Long id : channels) {
            strSet.add(String.valueOf(id));
        }
        prefs.edit().putStringSet(KEY_CHANNELS, strSet).apply();
    }

    public static void addTrackedChannel(long channelId) {
        Set<Long> channels = getTrackedChannels();
        channels.add(channelId);
        setTrackedChannels(channels);
    }

    public static void removeTrackedChannel(long channelId) {
        Set<Long> channels = getTrackedChannels();
        channels.remove(channelId);
        setTrackedChannels(channels);
    }

    public static List<FeedItem> getCachedFeed() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return new ArrayList<>();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CACHED_FEED, "[]");
        try {
            Type listType = new TypeToken<ArrayList<FeedItem>>(){}.getType();
            List<FeedItem> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveCachedFeed(List<FeedItem> items) {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(items);
        prefs.edit().putString(KEY_CACHED_FEED, json).apply();
    }

    public interface FeedCallback {
        void onProgress(String status);
        void onComplete(List<FeedItem> items);
        void onError(String error);
    }

    /**
     * Reads messages over the last 7 days from all tracked channels,
     * uses Gemini AI to filter ads and squeeze context into crisp digest cards.
     */
    public static void generateWeeklyDigest(int currentAccount, FeedCallback callback) {
        Set<Long> channelIds = getTrackedChannels();
        if (channelIds.isEmpty()) {
            callback.onError("Не обрано жодного каналу для розумної стрічки. Будь ласка, додайте канали у налаштуваннях.");
            return;
        }

        callback.onProgress("Зчитуємо повідомлення за 7 днів з " + channelIds.size() + " каналів...");

        final long sevenDaysAgo = (System.currentTimeMillis() / 1000L) - (7L * 24L * 3600L);
        final List<FeedItem> aggregatedItems = new ArrayList<>();
        final List<Long> channelList = new ArrayList<>(channelIds);
        final int totalChannels = channelList.size();

        processNextChannel(currentAccount, channelList, 0, sevenDaysAgo, aggregatedItems, callback);
    }

    private static void processNextChannel(int currentAccount, List<Long> channels, int index, long minDate, List<FeedItem> resultAccumulator, FeedCallback callback) {
        if (index >= channels.size()) {
            // All channels processed! Save and return
            saveCachedFeed(resultAccumulator);
            AndroidUtilities.runOnUIThread(() -> callback.onComplete(resultAccumulator));
            return;
        }

        long dialogId = channels.get(index);
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        String channelName = chat != null && chat.title != null ? chat.title : "Канал #" + Math.abs(dialogId);

        AndroidUtilities.runOnUIThread(() -> callback.onProgress("ШІ аналізує '" + channelName + "' (" + (index + 1) + "/" + channels.size() + ")..."));

        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.offset_id = 0;
        req.offset_date = 0;
        req.add_offset = 0;
        req.limit = 45;
        req.max_id = 0;
        req.min_id = 0;
        req.hash = 0;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                List<TLRPC.Message> recentMessages = new ArrayList<>();
                StringBuilder promptPosts = new StringBuilder();

                for (TLRPC.Message msg : res.messages) {
                    if (msg.date >= minDate && !TextUtils.isEmpty(msg.message)) {
                        recentMessages.add(msg);
                        promptPosts.append("--- ПОСТ ID: ").append(msg.id).append(" ---
");
                        promptPosts.append(msg.message.trim()).append("

");
                    }
                }

                if (recentMessages.isEmpty()) {
                    // Nothing to digest in last 7 days for this channel
                    processNextChannel(currentAccount, channels, index + 1, minDate, resultAccumulator, callback);
                    return;
                }

                // Send to Gemini for Ad-filtering & context-preserving squeeze
                String aiPrompt = "Ти — інтелектуальний редактор Miogram Smart Feed.\n"
                        + "Проаналізуй повідомлення з каналу '" + channelName + "' за тиждень.\n"
                        + "Правила:\n"
                        + "1. ПОВНІСТЮ ВИДАЛИ або познач is_ad: true будь-яку рекламу, промо, крипто-скам, казино, рефералки, спонсорські інтеграції та заклики підписатись на сторонні ресурси.\n"
                        + "2. Справжні авторські новини, апдейти, дослідження стисни в якісну вижимку (3-4 ключові речення), зберігаючи контекст, факти, початковий тон автора.\n"
                        + "3. Поверни суворий JSON-масив без markdown форматування, наприклад: [{"message_id": 10, "title": "Заголовок", "summary": "Вижимка...", "category": "Новини", "is_ad": false}]\n\n"
                        + promptPosts.toString();

                MiogramAiService.summarizeText(aiPrompt, aiResult -> {
                    if (!TextUtils.isEmpty(aiResult)) {
                        try {
                            String cleanedJson = aiResult.trim();
                            if (cleanedJson.startsWith("```json")) {
                                cleanedJson = cleanedJson.substring(7);
                            }
                            if (cleanedJson.startsWith("```")) {
                                cleanedJson = cleanedJson.substring(3);
                            }
                            if (cleanedJson.endsWith("```")) {
                                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
                            }
                            cleanedJson = cleanedJson.trim();

                            Type listType = new TypeToken<ArrayList<RawAiFeedItem>>(){}.getType();
                            List<RawAiFeedItem> rawItems = gson.fromJson(cleanedJson, listType);
                            if (rawItems != null) {
                                for (RawAiFeedItem raw : rawItems) {
                                    if (raw.is_ad) continue; // Skip ads!
                                    TLRPC.Message matchedMsg = null;
                                    for (TLRPC.Message m : recentMessages) {
                                        if (m.id == raw.message_id) {
                                            matchedMsg = m;
                                            break;
                                        }
                                    }
                                    FeedItem item = new FeedItem(dialogId, raw.message_id, channelName, raw.title, raw.summary, raw.category, matchedMsg != null ? (long) matchedMsg.date * 1000L : System.currentTimeMillis());
                                    if (matchedMsg != null && matchedMsg.media instanceof TLRPC.TL_messageMediaPhoto) {
                                        item.hasPhoto = true;
                                        item.originalMessage = matchedMsg;
                                    }
                                    resultAccumulator.add(item);
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e("SmartFeed parse error: " + e.getMessage());
                        }
                    }

                    // Move to next channel
                    processNextChannel(currentAccount, channels, index + 1, minDate, resultAccumulator, callback);
                });
            } else {
                // If error, continue to next channel
                processNextChannel(currentAccount, channels, index + 1, minDate, resultAccumulator, callback);
            }
        });
    }
}
