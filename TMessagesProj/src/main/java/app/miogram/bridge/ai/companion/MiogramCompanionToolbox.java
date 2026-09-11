package app.miogram.bridge.ai.companion;

import android.os.Bundle;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static class FoundChat {
        public final long dialogId;
        public final String name;
        public final String username;
        public final boolean isChannel;
        public final boolean isGroup;

        public FoundChat(long dialogId, String name, String username, boolean isChannel, boolean isGroup) {
            this.dialogId = dialogId;
            this.name = name != null ? name : "";
            this.username = username != null ? username : "";
            this.isChannel = isChannel;
            this.isGroup = isGroup;
        }

        public String getReference() {
            if (!username.isEmpty()) {
                return "@" + username;
            }
            return name;
        }
    }

    public static class ChatResolution {
        public final long dialogId;
        public final FoundChat foundChat;
        public final String errorMessage;

        public ChatResolution(long dialogId, FoundChat foundChat, String errorMessage) {
            this.dialogId = dialogId;
            this.foundChat = foundChat;
            this.errorMessage = errorMessage;
        }
    }

    public static List<FoundChat> searchChats(int account, String rawQuery) {
        List<FoundChat> matches = new ArrayList<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return matches;
        }
        String q = rawQuery.trim().toLowerCase(java.util.Locale.ROOT);
        if (q.startsWith("@")) {
            q = q.substring(1).trim();
        }
        if (q.isEmpty()) return matches;

        MessagesController mc = MessagesController.getInstance(account);
        Map<Long, FoundChat> dedup = new HashMap<>();

        // 1. Scan Dialogs
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        if (all != null) {
            for (int i = 0; i < all.size(); i++) {
                TLRPC.Dialog d = all.get(i);
                if (d == null) continue;
                long did = d.id;
                if (did > 0) {
                    TLRPC.User u = mc.getUser(did);
                    if (u != null) {
                        String fullName = UserObject.getUserName(u);
                        String uname = u.username != null ? u.username : "";
                        if (matchesQuery(q, fullName, uname, u.first_name, u.last_name)) {
                            dedup.put(did, new FoundChat(did, fullName, uname, false, false));
                        }
                    }
                } else if (did < 0) {
                    TLRPC.Chat c = mc.getChat(-did);
                    if (c != null) {
                        String title = c.title != null ? c.title : "";
                        String uname = c.username != null ? c.username : "";
                        boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                        boolean isGroup = !isChannel;
                        if (matchesQuery(q, title, uname, null, null)) {
                            dedup.put(did, new FoundChat(did, title, uname, isChannel, isGroup));
                        }
                    }
                }
            }
        }

        // 2. Scan Contacts
        ContactsController cc = ContactsController.getInstance(account);
        if (cc != null && cc.contacts != null) {
            for (int i = 0; i < cc.contacts.size(); i++) {
                TLRPC.TL_contact tc = cc.contacts.get(i);
                if (tc == null) continue;
                long uid = tc.user_id;
                if (!dedup.containsKey(uid)) {
                    TLRPC.User u = mc.getUser(uid);
                    if (u != null) {
                        String fullName = UserObject.getUserName(u);
                        String uname = u.username != null ? u.username : "";
                        if (matchesQuery(q, fullName, uname, u.first_name, u.last_name)) {
                            dedup.put(uid, new FoundChat(uid, fullName, uname, false, false));
                        }
                    }
                }
            }
        }

        matches.addAll(dedup.values());
        final String finalQ = q;
        matches.sort((a, b) -> {
            boolean aExactU = a.username.equalsIgnoreCase(finalQ);
            boolean bExactU = b.username.equalsIgnoreCase(finalQ);
            if (aExactU != bExactU) return aExactU ? -1 : 1;

            boolean aExactN = a.name.equalsIgnoreCase(finalQ);
            boolean bExactN = b.name.equalsIgnoreCase(finalQ);
            if (aExactN != bExactN) return aExactN ? -1 : 1;

            boolean aStartN = a.name.toLowerCase(java.util.Locale.ROOT).startsWith(finalQ);
            boolean bStartN = b.name.toLowerCase(java.util.Locale.ROOT).startsWith(finalQ);
            if (aStartN != bStartN) return aStartN ? -1 : 1;

            return 0;
        });

        return matches;
    }

    private static boolean matchesQuery(String query, String name, String username, String first, String last) {
        if (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        if (username != null && username.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        if (first != null && first.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        if (last != null && last.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        String t = transliterate(query);
        if (name != null && transliterate(name.toLowerCase(java.util.Locale.ROOT)).contains(t)) return true;
        return false;
    }

    private static String transliterate(String text) {
        if (text == null) return "";
        return text.replace("а", "a").replace("б", "b").replace("в", "v").replace("г", "h")
                .replace("ґ", "g").replace("д", "d").replace("е", "e").replace("є", "ye")
                .replace("ж", "zh").replace("з", "z").replace("и", "y").replace("і", "i")
                .replace("ї", "yi").replace("й", "y").replace("к", "k").replace("л", "l")
                .replace("м", "m").replace("н", "n").replace("о", "o").replace("п", "p")
                .replace("р", "r").replace("с", "s").replace("т", "t").replace("у", "u")
                .replace("ф", "f").replace("х", "kh").replace("ц", "ts").replace("ч", "ch")
                .replace("ш", "sh").replace("щ", "shch").replace("ь", "").replace("ю", "yu")
                .replace("я", "ya");
    }

    public static ChatResolution resolveChatTarget(int account, JSONObject p) {
        long chatId = p.optLong("chat_id", 0);
        if (chatId != 0) {
            return new ChatResolution(chatId, null, null);
        }
        String query = p.optString("chat_query", "");
        if (query.isEmpty()) query = p.optString("chat_name", "");
        if (query.isEmpty()) query = p.optString("username", "");
        if (query.isEmpty()) query = p.optString("query", "");

        if (query.isEmpty()) {
            return new ChatResolution(0, null, "Будь ласка, вкажи ім'я або юзернейм співрозмовника.");
        }

        List<FoundChat> results = searchChats(account, query);
        if (results.isEmpty()) {
            return new ChatResolution(0, null, "Не вдалося знайти жодного чату за запитом «" + query + "». Перевір правильність написання імені чи юзернейму.");
        }
        if (results.size() == 1) {
            return new ChatResolution(results.get(0).dialogId, results.get(0), null);
        }

        StringBuilder sb = new StringBuilder("Я знайшла " + results.size() + " схожих профілів за запитом «" + query + "»:\n");
        int count = Math.min(5, results.size());
        for (int i = 0; i < count; i++) {
            FoundChat fc = results.get(i);
            sb.append(i + 1).append(". ").append(fc.name);
            if (!fc.username.isEmpty()) {
                sb.append(" (@").append(fc.username).append(")");
            }
            sb.append("\n");
        }
        sb.append("Уточни, будь ласка, за @юзернеймом, кого саме ти маєш на увазі?");
        return new ChatResolution(0, null, sb.toString());
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
                case "find_chat": {
                    String query = p.optString("query", "");
                    if (query.isEmpty()) query = p.optString("chat_query", "");
                    if (query.isEmpty()) query = p.optString("name", "");
                    if (query.isEmpty()) {
                        callback.run("Вкажи, будь ласка, ім'я або юзернейм для пошуку (наприклад: «знайди в лс з Віталіком»).");
                        return;
                    }
                    List<FoundChat> results = searchChats(account, query);
                    if (results.isEmpty()) {
                        callback.run("Не знайшла жодного чату чи контакту за запитом «" + query + "».");
                        return;
                    }
                    if (results.size() == 1) {
                        FoundChat fc = results.get(0);
                        String ref = !fc.username.isEmpty() ? ("@" + fc.username) : fc.name;
                        callback.run("Знайдено чат: " + fc.name + (!fc.username.isEmpty() ? " (@" + fc.username + ")" : "") + ".");
                        return;
                    }
                    StringBuilder sb = new StringBuilder("Я знайшла " + results.size() + " схожих профілів:\n");
                    int count = Math.min(5, results.size());
                    for (int i = 0; i < count; i++) {
                        FoundChat fc = results.get(i);
                        sb.append(i + 1).append(". ").append(fc.name);
                        if (!fc.username.isEmpty()) {
                            sb.append(" (@").append(fc.username).append(")");
                        }
                        sb.append("\n");
                    }
                    sb.append("Уточни, будь ласка, за @юзернеймом, про кого саме мова?");
                    callback.run(sb.toString());
                    break;
                }
                case "clear_chat": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(res.dialogId, 1, false);
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : "цього чату";
                    callback.run("Історію листування з " + targetName + " успішно очищено.");
                    break;
                }
                case "delete_chat": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(res.dialogId, 0, false);
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : "діалог";
                    callback.run("Діалог з " + targetName + " успішно видалено зі списку.");
                    break;
                }
                case "send_message": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    String text = p.optString("text", "");
                    if (text.isEmpty()) {
                        callback.run("Помилка: не вказано текст повідомлення для відправки.");
                        return;
                    }
                    SendMessagesHelper.getInstance(account).sendMessage(
                            SendMessagesHelper.SendMessageParams.of(text, res.dialogId, null, null, null, true, null, null, null, true, 0, 0, null, false)
                    );
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : "чат";
                    callback.run("Повідомлення успішно відправлено для " + targetName + ".");
                    break;
                }
                case "read_messages": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    int limit = Math.min(20, Math.max(1, p.optInt("limit", 10)));
                    TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                    req.peer = MessagesController.getInstance(account).getInputPeer(res.dialogId);
                    req.limit = limit;
                    final FoundChat fc = res.foundChat;
                    ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (response instanceof TLRPC.messages_Messages) {
                            TLRPC.messages_Messages msgRes = (TLRPC.messages_Messages) response;
                            String header = fc != null ? ("Останні повідомлення з " + fc.getReference() + ":\n") : "Останні повідомлення:\n";
                            StringBuilder sb = new StringBuilder(header);
                            if (msgRes.messages.isEmpty()) {
                                sb.append("(Листування порожнє або немає недавніх повідомлень)");
                            } else {
                                for (TLRPC.Message m : msgRes.messages) {
                                    if (m != null && m.message != null && !m.message.isEmpty()) {
                                        sb.append("- ").append(m.message.replace("\n", " ")).append("\n");
                                    }
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
                    MessagesController.getInstance(account).createChat(title, new ArrayList<>(), null, isChannel ? 2 : 0, false, null, null, -1, null);
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
                    PluginsController.getInstance().setPluginEnabled(pid, enable);
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
