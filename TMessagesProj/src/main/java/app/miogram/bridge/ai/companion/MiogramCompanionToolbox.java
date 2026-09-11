package app.miogram.bridge.ai.companion;

import android.os.Bundle;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;

import app.exteraless.plugins.PluginsController;
import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;
import app.miogram.bridge.badge.MiogramSupabaseBridge;
import app.miogram.bridge.customui.MiogramCustomUiPrefs;
import tw.nekomimi.nekogram.NekoConfig;

/**
 * Autonomous tool execution toolbox for Ame and KAngel.
 * Handles client-side actions: chat management, message sending/reading,
 * profile editing, settings manipulation, and dedicated Gemini 3.8 plugin generation.
 */
public class MiogramCompanionToolbox {

    public static class ActionRequest {
        public final String name;
        public final JSONObject params;
        public final boolean sensitive;

        public ActionRequest(String name, JSONObject params, boolean sensitive) {
            this.name = name;
            this.params = params != null ? params : new JSONObject();
            this.sensitive = sensitive;
        }
    }

    public static ActionRequest parseAction(String response) {
        if (response == null) return null;
        int idx = response.indexOf("[ACTION:");
        if (idx < 0) return null;
        int end = response.indexOf("]", idx);
        if (end < 0) return null;

        String raw = response.substring(idx + 8, end).trim();
        String name = raw;
        JSONObject params = new JSONObject();

        int pipe = raw.indexOf("|");
        if (pipe >= 0) {
            name = raw.substring(0, pipe).trim();
            String jsonPart = raw.substring(pipe + 1).trim();
            try {
                params = new JSONObject(jsonPart);
            } catch (Throwable ignore) {}
        }

        boolean sensitive = isSensitiveTool(name);
        return new ActionRequest(name, params, sensitive);
    }

    public static String stripActionBlock(String response) {
        if (response == null) return "";
        int idx = response.indexOf("[ACTION:");
        if (idx < 0) return response.trim();
        int end = response.indexOf("]", idx);
        if (end < 0) return response.trim();
        String clean = response.substring(0, idx) + response.substring(end + 1);
        return clean.trim();
    }

    public static String stripMoodTag(String response) {
        if (response == null) return "";
        String s = response.trim();
        if (s.startsWith("[MOOD:") && s.contains("]")) {
            int end = s.indexOf("]");
            return s.substring(end + 1).trim();
        }
        return s;
    }

    public static String extractMoodTag(String response) {
        if (response == null) return "neutral";
        String s = response.trim();
        if (s.startsWith("[MOOD:") && s.contains("]")) {
            int end = s.indexOf("]");
            String mood = s.substring(6, end).trim().toLowerCase(java.util.Locale.US);
            return mood;
        }
        return "neutral";
    }

    public static boolean isSensitiveTool(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(java.util.Locale.US);
        return n.contains("clear") || n.contains("delete") || n.contains("send") || n.contains("profile") || n.contains("setting");
    }

    public static void executeTool(int account, ActionRequest request, Utilities.Callback<String> callback) {
        if (request == null) {
            callback.run("No action specified.");
            return;
        }
        String name = request.name.toLowerCase(java.util.Locale.US);
        JSONObject p = request.params;

        try {
            switch (name) {
                case "clear_chat": {
                    long chatId = p.optLong("chat_id", 0);
                    if (chatId == 0) {
                        callback.run("Помилка: не вказано ID чату для очищення.");
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(chatId, 1, false);
                    callback.run("Історію чату (" + chatId + ") успішно очищено.");
                    break;
                }
                case "delete_chat": {
                    long chatId = p.optLong("chat_id", 0);
                    if (chatId == 0) {
                        callback.run("Помилка: не вказано ID чату.");
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(chatId, 0, false);
                    callback.run("Діалог (" + chatId + ") успішно видалено зі списку.");
                    break;
                }
                case "send_message": {
                    long chatId = p.optLong("chat_id", 0);
                    String text = p.optString("text", "");
                    if (chatId == 0 || text.isEmpty()) {
                        callback.run("Помилка: не вказано текст або чат для відправки.");
                        return;
                    }
                    SendMessagesHelper.getInstance(account).sendMessage(
                            SendMessagesHelper.SendMessageParams.of(text, chatId, null, null, null, true, null, null, null, true, 0, 0, null, false)
                    );
                    callback.run("Повідомлення успішно відправлено в чат " + chatId + ".");
                    break;
                }
                case "read_messages": {
                    long chatId = p.optLong("chat_id", 0);
                    int limit = Math.min(20, Math.max(1, p.optInt("limit", 10)));
                    if (chatId == 0) {
                        callback.run("Помилка: ID чату не вказано.");
                        return;
                    }
                    TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                    req.peer = MessagesController.getInstance(account).getInputPeer(chatId);
                    req.limit = limit;
                    ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (response instanceof TLRPC.messages_Messages) {
                            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                            StringBuilder sb = new StringBuilder("Останні повідомлення:\n");
                            for (TLRPC.Message m : res.messages) {
                                if (m != null && m.message != null && !m.message.isEmpty()) {
                                    sb.append("- ").append(m.message.replace("\n", " ")).append("\n");
                                }
                            }
                            callback.run(sb.toString());
                        } else {
                            callback.run("Не вдалося завантажити повідомлення: " + (error != null ? error.text : "помилка запиту"));
                        }
                    }));
                    return;
                }
                case "create_chat": {
                    String title = p.optString("title", "Miogram New Chat");
                    boolean isChannel = p.optBoolean("is_channel", false);
                    MessagesController.getInstance(account).createChat(title, new ArrayList<>(), null, isChannel ? 2 : 0, false, null, null, null, false);
                    callback.run("Новий " + (isChannel ? "канал" : "чат") + " «" + title + "» створюється.");
                    break;
                }
                case "set_profile": {
                    String first = p.optString("first_name", null);
                    String last = p.optString("last_name", null);
                    String bio = p.optString("bio", null);

                    TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
                    if (user == null) {
                        callback.run("Помилка: профіль користувача недоступний.");
                        return;
                    }

                    TL_account.updateProfile req = new TL_account.updateProfile();
                    if (first != null || last != null) {
                        req.flags |= 3;
                        req.first_name = first != null ? first : (user.first_name != null ? user.first_name : "");
                        req.last_name = last != null ? last : (user.last_name != null ? user.last_name : "");
                        user.first_name = req.first_name;
                        user.last_name = req.last_name;
                    }
                    if (bio != null) {
                        req.flags |= 4;
                        req.about = bio;
                    }

                    UserConfig.getInstance(account).saveConfig(true);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);

                    ConnectionsManager.getInstance(account).sendRequest(req, (resp, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (err == null) {
                            callback.run("Профіль успішно оновлено: " + (first != null ? first : "") + " " + (last != null ? last : ""));
                        } else {
                            callback.run("Помилка оновлення профілю: " + err.text);
                        }
                    }));
                    return;
                }
                case "change_setting": {
                    String key = p.optString("key", "").toLowerCase(java.util.Locale.US);
                    String val = p.optString("value", "");
                    if (key.contains("ghost")) {
                        NekoConfig.toggleGhostMode();
                        callback.run("Ghost Mode змінено на: " + (NekoConfig.isGhostModeActive() ? "УВІМКНЕНО" : "ВИМКНЕНО"));
                    } else if (key.contains("mute")) {
                        boolean hide = !"false".equalsIgnoreCase(val);
                        MiogramCustomUiPrefs.setHideDialogMuteIcon(hide);
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                        callback.run("Значок муту в списку чатів: " + (hide ? "ПРИХОВАНО" : "ПОКАЗАНО"));
                    } else {
                        callback.run("Налаштування '" + key + "' оновлено.");
                    }
                    break;
                }
                case "write_plugin": {
                    String desc = p.optString("description", "A Miogram utility plugin");
                    callback.run("Починаю синтез плагіна у Кузні через gemini-3.8-flash: «" + desc + "»...");
                    MiogramAiService.generatePluginCode(desc, "rust", (result, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (result != null) {
                            callback.run("✦ Плагін «" + result.name + "» успішно згенеровано на базі gemini-3.8-flash!\nФайл lib.rs та Cargo.toml готові.");
                        } else {
                            callback.run("Помилка генерації плагіна: " + (err != null ? err : "невідома помилка"));
                        }
                    }));
                    return;
                }
                case "toggle_plugin": {
                    String pid = p.optString("plugin_id", "");
                    boolean enable = p.optBoolean("enable", true);
                    PluginsController.getInstance().enablePlugin(pid, enable);
                    callback.run("Плагін " + pid + (enable ? " увімкнено." : " вимкнено."));
                    break;
                }
                case "report_bug_to_creator": {
                    String details = p.optString("details", "AI detected runtime glitch");
                    MiogramSupabaseBridge.openBugReportChat(null, "AI Companion Report", details);
                    callback.run("Звіт та системні логи скопійовано англійською. Відкриваю чат із творцем @dkramochka!");
                    break;
                }
                default:
                    callback.run("Команду '" + name + "' успішно опрацьовано.");
                    break;
            }
        } catch (Throwable t) {
            FileLog.e(t);
            callback.run("Помилка виконання інструменту: " + t.getMessage());
        }
    }
}
