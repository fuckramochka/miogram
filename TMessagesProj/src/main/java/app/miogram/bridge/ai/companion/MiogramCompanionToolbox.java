package app.miogram.bridge.ai.companion;

import android.os.Bundle;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
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

    public static class ScoredFoundChat {
        public final FoundChat chat;
        public final int score;

        public ScoredFoundChat(FoundChat chat, int score) {
            this.chat = chat;
            this.score = score;
        }
    }

    public static List<FoundChat> searchChats(int account, String rawQuery) {
        List<FoundChat> matches = new ArrayList<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return matches;
        }
        String q = rawQuery.trim();
        if (q.startsWith("@")) {
            q = q.substring(1).trim();
        }
        if (q.isEmpty()) return matches;

        MessagesController mc = MessagesController.getInstance(account);
        Map<Long, ScoredFoundChat> dedup = new HashMap<>();

        // 1. Scan Dialogs
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        if (all == null || all.isEmpty()) {
            all = mc.getDialogs(0);
        }
        if (all == null || all.isEmpty()) {
            all = mc.dialogsServerOnly;
        }
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
                        int score = calculateMatchScore(q, fullName, uname, u.first_name, u.last_name);
                        if (score >= 60) {
                            dedup.put(did, new ScoredFoundChat(new FoundChat(did, fullName, uname, false, false), score));
                        }
                    }
                } else if (did < 0) {
                    TLRPC.Chat c = mc.getChat(-did);
                    if (c != null) {
                        String title = c.title != null ? c.title : "";
                        String uname = c.username != null ? c.username : "";
                        boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                        boolean isGroup = !isChannel;
                        int score = calculateMatchScore(q, title, uname, null, null);
                        if (score >= 60) {
                            dedup.put(did, new ScoredFoundChat(new FoundChat(did, title, uname, isChannel, isGroup), score));
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
                TLRPC.User u = mc.getUser(uid);
                if (u != null) {
                    String fullName = UserObject.getUserName(u);
                    String uname = u.username != null ? u.username : "";
                    int score = calculateMatchScore(q, fullName, uname, u.first_name, u.last_name);
                    if (score >= 60) {
                        ScoredFoundChat existing = dedup.get(uid);
                        if (existing == null || score > existing.score) {
                            dedup.put(uid, new ScoredFoundChat(new FoundChat(uid, fullName, uname, false, false), score));
                        }
                    }
                }
            }
        }

        List<ScoredFoundChat> sorted = new ArrayList<>(dedup.values());
        sorted.sort((a, b) -> Integer.compare(b.score, a.score));

        for (ScoredFoundChat s : sorted) {
            matches.add(s.chat);
        }
        return matches;
    }

    public static int calculateMatchScore(String rawQ, String name, String username, String first, String last) {
        if (rawQ == null || rawQ.trim().isEmpty()) return 0;
        String qLower = rawQ.trim().toLowerCase(java.util.Locale.ROOT);
        String normQ = normalizeText(rawQ);
        String colQ = collapseRepeats(normQ);

        int best = 0;
        String[] targets = new String[]{username, name, first, last};
        for (String target : targets) {
            if (target == null || target.trim().isEmpty()) continue;
            String tLower = target.trim().toLowerCase(java.util.Locale.ROOT);
            if (tLower.equals(qLower)) return 100;
            if (tLower.contains(qLower)) best = Math.max(best, 92);

            String normT = normalizeText(target);
            if (normT.isEmpty()) continue;
            String colT = collapseRepeats(normT);

            if (normT.equals(normQ)) return 95;
            if (colT.equals(colQ)) return 90;

            if (normT.contains(normQ) || normQ.contains(normT)) best = Math.max(best, 85);
            if (colT.contains(colQ) || colQ.contains(colT)) best = Math.max(best, 80);

            // Stem match (e.g. "безлик" in "безликий" / "6ezzликий")
            if (colQ.length() >= 4 && colT.length() >= 4) {
                String stemQ = colQ.substring(0, Math.min(colQ.length(), 5));
                String stemT = colT.substring(0, Math.min(colT.length(), 5));
                if (colT.contains(stemQ) || colQ.contains(stemT)) {
                    best = Math.max(best, 75);
                }
            }
        }
        return best;
    }

    public static String transliterateEnToUa(String s) {
        if (s == null) return "";
        s = s.toLowerCase(java.util.Locale.ROOT);
        s = s.replace("shch", "щ").replace("zh", "ж").replace("ch", "ч").replace("sh", "ш")
                .replace("yu", "ю").replace("ya", "я").replace("ye", "є").replace("yi", "ї")
                .replace("ts", "ц").replace("kh", "х");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'a': sb.append('а'); break;
                case 'b': sb.append('б'); break;
                case 'v': case 'w': sb.append('в'); break;
                case 'g': sb.append('г'); break;
                case 'd': sb.append('д'); break;
                case 'e': sb.append('е'); break;
                case 'z': sb.append('з'); break;
                case 'i': sb.append('і'); break;
                case 'y': sb.append('и'); break;
                case 'j': sb.append('й'); break;
                case 'k': sb.append('к'); break;
                case 'l': sb.append('л'); break;
                case 'm': sb.append('м'); break;
                case 'n': sb.append('н'); break;
                case 'o': sb.append('о'); break;
                case 'p': sb.append('р'); break;
                case 'r': sb.append('р'); break;
                case 's': sb.append('с'); break;
                case 't': sb.append('т'); break;
                case 'u': sb.append('у'); break;
                case 'f': sb.append('ф'); break;
                case 'h': sb.append('х'); break;
                case 'c': sb.append('ц'); break;
                case 'x': sb.append("кс"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String normalizeText(String s) {
        if (s == null) return "";
        s = s.toLowerCase(java.util.Locale.ROOT);
        s = s.replaceAll("[\\p{So}\\p{Cn}\\p{Sk}\\p{Cs}\\p{Punct}#|\\-_\\s]+", "");
        s = s.replace('6', 'б')
             .replace('0', 'о')
             .replace('1', 'і')
             .replace('3', 'з')
             .replace('4', 'ч')
             .replace('5', 'с')
             .replace('7', 'т')
             .replace('8', 'в');
        String cyr = transliterateEnToUa(s);
        return cyr.replaceAll("[^а-яіїєґ0-9a-z]", "");
    }

    public static String collapseRepeats(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != prev) {
                sb.append(c);
                prev = c;
            }
        }
        return sb.toString();
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
            return new ChatResolution(0, null, MiogramLocale.get("Будь ласка, вкажи ім'я або юзернейм співрозмовника.", "Пожалуйста, укажи имя или юзернейм собеседника.", "Please specify name or @username."));
        }

        List<FoundChat> results = searchChats(account, query);
        if (results.isEmpty()) {
            return new ChatResolution(0, null, MiogramLocale.get("Не вдалося знайти жодного чату за запитом «", "Не удалось найти ни одного чата по запросу «", "Could not find any chat for query \"") + query + MiogramLocale.get("». Перевір правильність написання імені чи юзернейму.", "». Проверь правильность написания имени или юзернейма.", "\". Check the name or username spelling."));
        }
        if (results.size() == 1) {
            return new ChatResolution(results.get(0).dialogId, results.get(0), null);
        }

        StringBuilder sb = new StringBuilder(MiogramLocale.get("Я знайшла ", "Я нашла ", "I found ") + results.size() + MiogramLocale.get(" схожих профілів за запитом «", " похожих профилей по запросу «", " similar profiles for query \"") + query + "»:\n");
        int count = Math.min(5, results.size());
        for (int i = 0; i < count; i++) {
            FoundChat fc = results.get(i);
            sb.append(i + 1).append(". ").append(fc.name);
            if (!fc.username.isEmpty()) {
                sb.append(" (@").append(fc.username).append(")");
            }
            sb.append("\n");
        }
        sb.append(MiogramLocale.get("Уточни, будь ласка, за @юзернеймом, кого саме ти маєш на увазі?", "Уточни, пожалуйста, по @юзернейму, кого именно ты имеешь в виду?", "Please clarify by @username which one you mean."));
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
                        callback.run(MiogramLocale.get("Вкажи, будь ласка, ім'я або юзернейм для пошуку (наприклад: «знайди в лс з Віталіком»).", "Укажи, пожалуйста, имя или юзернейм для поиска (например: «найди в лс с Виталиком»).", "Please specify name or username to search (e.g. \"find chat with Alex\")."));
                        return;
                    }
                    List<FoundChat> results = searchChats(account, query);
                    if (results.isEmpty()) {
                        callback.run(MiogramLocale.get("Не знайшла жодного чату чи контакту за запитом «", "Не нашла ни одного чата или контакта по запросу «", "Did not find any chat or contact for query \"") + query + "».");
                        return;
                    }
                    if (results.size() == 1) {
                        FoundChat fc = results.get(0);
                        callback.run(MiogramLocale.get("Знайдено чат: ", "Найден чат: ", "Chat found: ") + fc.name + (!fc.username.isEmpty() ? " (@" + fc.username + ")" : "") + ".");
                        return;
                    }
                    StringBuilder sb = new StringBuilder(MiogramLocale.get("Я знайшла ", "Я нашла ", "I found ") + results.size() + MiogramLocale.get(" схожих профілів:\n", " похожих профилей:\n", " similar profiles:\n"));
                    int count = Math.min(5, results.size());
                    for (int i = 0; i < count; i++) {
                        FoundChat fc = results.get(i);
                        sb.append(i + 1).append(". ").append(fc.name);
                        if (!fc.username.isEmpty()) {
                            sb.append(" (@").append(fc.username).append(")");
                        }
                        sb.append("\n");
                    }
                    sb.append(MiogramLocale.get("Уточни, будь ласка, за @юзернеймом, про кого саме мова?", "Уточни, пожалуйста, по @юзернейму, о ком именно речь?", "Please clarify by @username who you are referring to."));
                    callback.run(sb.toString());
                    break;
                }
                case "search_groups": {
                    String query = p.optString("query", "");
                    if (query.isEmpty()) query = p.optString("chat_query", "");
                    MessagesController mc = MessagesController.getInstance(account);
                    ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
                    if (all == null || all.isEmpty()) all = mc.getDialogs(0);
                    if (all == null || all.isEmpty()) all = mc.dialogsServerOnly;

                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    if (all != null) {
                        for (int i = 0; i < all.size(); i++) {
                            TLRPC.Dialog d = all.get(i);
                            if (d == null || d.id >= 0) continue;
                            TLRPC.Chat c = mc.getChat(-d.id);
                            if (c == null) continue;
                            boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                            if (isChannel) continue;
                            String title = c.title != null ? c.title : MiogramLocale.get("Група", "Группа", "Group");
                            String uname = c.username != null ? c.username : "";
                            if (!query.isEmpty()) {
                                int score = calculateMatchScore(query, title, uname, null, null);
                                if (score < 50) continue;
                            }
                            count++;
                            sb.append(count).append(". «").append(title).append("»");
                            if (!uname.isEmpty()) {
                                sb.append(" (@").append(uname).append(")");
                            }
                            if (c.participants_count > 0) {
                                sb.append(" — ").append(c.participants_count).append(MiogramLocale.get(" учасників", " участников", " members"));
                            }
                            if (d.unread_count > 0) {
                                sb.append(" [").append(d.unread_count).append(MiogramLocale.get(" непрочитаних]", " непрочитанных]", " unread]"));
                            }
                            sb.append("\n");
                            if (count >= 15) break;
                        }
                    }
                    if (count == 0) {
                        callback.run(!query.isEmpty()
                                ? MiogramLocale.get("Не знайшла жодної групи за запитом «", "Не нашла ни одной группы по запросу «", "Did not find any group for query \"") + query + "»."
                                : MiogramLocale.get("У тебе немає активних груп у списку діалогів.", "У тебя нет активных групп в списке диалогов.", "You have no active groups in your chat list."));
                    } else {
                        String header = !query.isEmpty()
                                ? MiogramLocale.get("Ось знайдені групи за запитом «", "Вот найденные группы по запросу «", "Here are the groups found for query \"") + query + "»:\n"
                                : MiogramLocale.get("Ось список твоїх груп:\n", "Вот список твоих групп:\n", "Here is the list of your groups:\n");
                        callback.run(header + sb.toString());
                    }
                    break;
                }
                case "search_messages": {
                    String query = p.optString("query", "");
                    if (query.isEmpty()) query = p.optString("text", "");
                    if (query.isEmpty()) query = p.optString("q", "");
                    if (query.isEmpty()) {
                        callback.run(MiogramLocale.get("Вкажи текст для пошуку повідомлень.", "Укажи текст для поиска сообщений.", "Specify text to search messages."));
                        return;
                    }
                    String chatQuery = p.optString("chat_query", "");
                    if (chatQuery.isEmpty()) chatQuery = p.optString("chat_name", "");
                    long specificChatId = p.optLong("chat_id", 0);

                    final String fQuery = query;
                    MessagesController mc = MessagesController.getInstance(account);

                    if (specificChatId != 0 || !chatQuery.isEmpty()) {
                        ChatResolution res = resolveChatTarget(account, p);
                        if (res.errorMessage != null) {
                            callback.run(res.errorMessage);
                            return;
                        }
                        long targetId = res.dialogId;
                        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                        req.peer = mc.getInputPeer(targetId);
                        req.q = fQuery;
                        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
                        req.limit = 10;
                        final String chatName = res.foundChat != null ? res.foundChat.getReference() : MiogramLocale.get("чаті", "чате", "chat");
                        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                            if (response instanceof TLRPC.messages_Messages) {
                                TLRPC.messages_Messages msgs = (TLRPC.messages_Messages) response;
                                mc.putUsers(msgs.users, false);
                                mc.putChats(msgs.chats, false);
                                if (msgs.messages.isEmpty()) {
                                    callback.run(MiogramLocale.get("У чаті ", "В чате ", "In chat ") + chatName + MiogramLocale.get(" нічого не знайдено за запитом «", " ничего не найдено по запросу «", " nothing found for query \"") + fQuery + "».");
                                } else {
                                    StringBuilder sb = new StringBuilder(MiogramLocale.get("Результати пошуку в ", "Результаты поиска в ", "Search results in ") + chatName + MiogramLocale.get(" за запитом «", " по запросу «", " for query \"") + fQuery + "»:\n");
                                    for (TLRPC.Message m : msgs.messages) {
                                        if (m == null || m.message == null || m.message.trim().isEmpty()) continue;
                                        String senderName = MiogramLocale.get("Користувач", "Пользователь", "User");
                                        long fromId = MessageObject.getFromChatId(m);
                                        if (fromId > 0) {
                                            TLRPC.User u = mc.getUser(fromId);
                                            if (u != null) senderName = UserObject.getUserName(u);
                                        } else if (fromId < 0) {
                                            TLRPC.Chat c = mc.getChat(-fromId);
                                            if (c != null && c.title != null) senderName = c.title;
                                        }
                                        sb.append("• [").append(senderName).append("]: ").append(m.message.replace("\n", " ")).append("\n");
                                    }
                                    callback.run(sb.toString());
                                }
                            } else {
                                callback.run(MiogramLocale.get("Помилка пошуку в чаті: ", "Ошибка поиска в чате: ", "Chat search error: ") + (error != null ? error.text : MiogramLocale.get("невідома помилка", "неизвестная ошибка", "unknown error")));
                            }
                        }));
                        return;
                    }

                    // Global search across all groups & chats
                    TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
                    req.q = fQuery;
                    req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
                    req.limit = 12;
                    req.offset_peer = new TLRPC.TL_inputPeerEmpty();
                    ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (response instanceof TLRPC.messages_Messages) {
                            TLRPC.messages_Messages msgs = (TLRPC.messages_Messages) response;
                            mc.putUsers(msgs.users, false);
                            mc.putChats(msgs.chats, false);
                            if (msgs.messages.isEmpty()) {
                                callback.run(MiogramLocale.get("Нічого не знайдено по групах та чатах за запитом «", "Ничего не найдено по группам и чатам по запросу «", "Nothing found across groups and chats for query \"") + fQuery + "».");
                            } else {
                                StringBuilder sb = new StringBuilder(MiogramLocale.get("Ось що знайшла в групах та чатах за запитом «", "Вот что нашла по группам и чатам по запросу «", "Here is what I found across groups and chats for query \"") + fQuery + "»:\n");
                                for (TLRPC.Message m : msgs.messages) {
                                    if (m == null || m.message == null || m.message.trim().isEmpty()) continue;
                                    String chatTitle = MiogramLocale.get("Чат", "Чат", "Chat");
                                    long peerId = MessageObject.getDialogId(m);
                                    if (peerId < 0) {
                                        TLRPC.Chat c = mc.getChat(-peerId);
                                        if (c != null && c.title != null) chatTitle = c.title;
                                    } else if (peerId > 0) {
                                        TLRPC.User u = mc.getUser(peerId);
                                        if (u != null) chatTitle = UserObject.getUserName(u);
                                    }
                                    String senderName = "";
                                    long fromId = MessageObject.getFromChatId(m);
                                    if (fromId > 0) {
                                        TLRPC.User u = mc.getUser(fromId);
                                        if (u != null) senderName = " (" + UserObject.getUserName(u) + ")";
                                    }
                                    sb.append("• [«").append(chatTitle).append("»").append(senderName).append("]: ")
                                      .append(m.message.replace("\n", " ")).append("\n");
                                }
                                callback.run(sb.toString());
                            }
                        } else {
                            callback.run(MiogramLocale.get("Помилка глобального пошуку: ", "Ошибка глобального поиска: ", "Global search error: ") + (error != null ? error.text : MiogramLocale.get("невідома помилка", "неизвестная ошибка", "unknown error")));
                        }
                    }));
                    return;
                }
                case "clear_chat": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(res.dialogId, 1, false);
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : MiogramLocale.get("цього чату", "этого чата", "this chat");
                    callback.run(MiogramLocale.get("Історію листування з ", "История переписки с ", "Chat history with ") + targetName + MiogramLocale.get(" успішно очищено.", " успешно очищена.", " cleared successfully."));
                    break;
                }
                case "delete_chat": {
                    ChatResolution res = resolveChatTarget(account, p);
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    MessagesController.getInstance(account).deleteDialog(res.dialogId, 0, false);
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : MiogramLocale.get("діалог", "диалог", "dialog");
                    callback.run(MiogramLocale.get("Діалог з ", "Диалог с ", "Dialog with ") + targetName + MiogramLocale.get(" успішно видалено зі списку.", " успешно удален из списка.", " deleted from list successfully."));
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
                        callback.run(MiogramLocale.get("Помилка: не вказано текст повідомлення для відправки.", "Ошибка: не указан текст сообщения для отправки.", "Error: no message text provided to send."));
                        return;
                    }
                    SendMessagesHelper.getInstance(account).sendMessage(
                            SendMessagesHelper.SendMessageParams.of(text, res.dialogId, null, null, null, true, null, null, null, true, 0, 0, null, false)
                    );
                    String targetName = res.foundChat != null ? res.foundChat.getReference() : MiogramLocale.get("чат", "чат", "chat");
                    callback.run(MiogramLocale.get("Повідомлення успішно відправлено для ", "Сообщение успешно отправлено для ", "Message sent successfully to ") + targetName + ".");
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
                            String header = fc != null
                                    ? (MiogramLocale.get("Останні повідомлення з ", "Последние сообщения с ", "Recent messages from ") + fc.getReference() + ":\n")
                                    : MiogramLocale.get("Останні повідомлення:\n", "Последние сообщения:\n", "Recent messages:\n");
                            StringBuilder sb = new StringBuilder(header);
                            if (msgRes.messages.isEmpty()) {
                                sb.append(MiogramLocale.get("(Листування порожнє або немає недавніх повідомлень)", "(Переписка пуста или нет недавних сообщений)", "(Chat is empty or no recent messages)"));
                            } else {
                                for (TLRPC.Message m : msgRes.messages) {
                                    if (m != null && m.message != null && !m.message.isEmpty()) {
                                        sb.append("- ").append(m.message.replace("\n", " ")).append("\n");
                                    }
                                }
                            }
                            callback.run(sb.toString());
                        } else {
                            callback.run(MiogramLocale.get("Не вдалося завантажити повідомлення: ", "Не удалось загрузить сообщения: ", "Failed to load messages: ") + (error != null ? error.text : MiogramLocale.get("помилка запиту", "ошибка запроса", "request error")));
                        }
                    }));
                    return;
                }
                case "create_chat": {
                    String title = p.optString("title", "Miogram New Chat");
                    boolean isChannel = p.optBoolean("is_channel", false);
                    MessagesController.getInstance(account).createChat(title, new ArrayList<>(), null, isChannel ? 2 : 0, false, null, null, -1, null);
                    callback.run(MiogramLocale.get("Новий ", "Новый ", "New ") + (isChannel ? MiogramLocale.get("канал", "канал", "channel") : MiogramLocale.get("чат", "чат", "chat")) + " «" + title + "» " + MiogramLocale.get("створюється.", "создается.", "is being created."));
                    break;
                }
                case "set_profile": {
                    String first = p.optString("first_name", null);
                    String last = p.optString("last_name", null);
                    String bio = p.optString("bio", null);

                    TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
                    if (user == null) {
                        callback.run(MiogramLocale.get("Помилка: профіль користувача недоступний.", "Ошибка: профиль пользователя недоступен.", "Error: user profile unavailable."));
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
                            callback.run(MiogramLocale.get("Профіль успішно оновлено: ", "Профиль успешно обновлен: ", "Profile updated successfully: ") + (first != null ? first : "") + " " + (last != null ? last : ""));
                        } else {
                            callback.run(MiogramLocale.get("Помилка оновлення профілю: ", "Ошибка обновления профиля: ", "Profile update error: ") + err.text);
                        }
                    }));
                    return;
                }
                case "change_setting": {
                    String key = p.optString("key", "").toLowerCase(java.util.Locale.US);
                    String val = p.optString("value", "");
                    if (key.contains("ghost")) {
                        NekoConfig.toggleGhostMode();
                        callback.run("Ghost Mode " + MiogramLocale.get("змінено на: ", "изменено на: ", "changed to: ") + (NekoConfig.isGhostModeActive() ? MiogramLocale.get("УВІМКНЕНО", "ВКЛЮЧЕНО", "ENABLED") : MiogramLocale.get("ВИМКНЕНО", "ВЫКЛЮЧЕНО", "DISABLED")));
                    } else if (key.contains("mute")) {
                        boolean hide = !"false".equalsIgnoreCase(val);
                        MiogramCustomUiPrefs.setHideDialogMuteIcon(hide);
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
                        callback.run(MiogramLocale.get("Значок муту в списку чатів: ", "Значок мута в списке чатов: ", "Mute icon in chat list: ") + (hide ? MiogramLocale.get("ПРИХОВАНО", "СКРЫТО", "HIDDEN") : MiogramLocale.get("ПОКАЗАНО", "ПОКАЗАНО", "SHOWN")));
                    } else {
                        callback.run(MiogramLocale.get("Налаштування '", "Настройка '", "Setting '") + key + MiogramLocale.get("' оновлено.", "' обновлена.", "' updated."));
                    }
                    break;
                }
                case "write_plugin": {
                    String desc = p.optString("description", "A Miogram utility plugin");
                    String userLang = p.optString("language", "").toLowerCase(java.util.Locale.US);
                    String lowerDesc = desc.toLowerCase(java.util.Locale.US);

                    String targetLang;
                    if (!userLang.isEmpty()) {
                        targetLang = userLang;
                    } else if (lowerDesc.contains("go") || lowerDesc.contains("го")) {
                        targetLang = "go";
                    } else if (lowerDesc.contains("rust") || lowerDesc.contains("раст")) {
                        targetLang = "rust";
                    } else if (lowerDesc.contains("lua") || lowerDesc.contains("луа")) {
                        targetLang = "lua";
                    } else if (lowerDesc.contains("python") || lowerDesc.contains("пайтон") || lowerDesc.contains("питон") || lowerDesc.contains("userbot") || lowerDesc.contains("юзербот")) {
                        targetLang = "python";
                    } else {
                        // Complexity evaluation:
                        // Heavy/computational logic -> Go or Rust
                        // Simple chat/text/filter/command logic -> Lua or Python
                        boolean isHeavy = lowerDesc.contains("crypto") || lowerDesc.contains("шифр") || lowerDesc.contains("aes") || lowerDesc.contains("hash") || lowerDesc.contains("хэш") || lowerDesc.contains("хеш") || lowerDesc.contains("heavy") || lowerDesc.contains("wasm") || lowerDesc.contains("compress") || lowerDesc.contains("стиснення") || lowerDesc.contains("сжатие");
                        if (isHeavy) {
                            targetLang = "go";
                        } else {
                            targetLang = "lua";
                        }
                    }

                    final String finalLang = targetLang;
                    callback.run(MiogramLocale.get("Починаю синтез плагіна на ", "Начинаю синтез плагина на ", "Starting plugin synthesis in ") + finalLang.toUpperCase(java.util.Locale.US) + MiogramLocale.get(" через gemini-3.8-flash: «", " через gemini-3.8-flash: «", " via gemini-3.8-flash: \"") + desc + "»...");

                    MiogramAiService.generatePluginCode(desc, finalLang, (result, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (result != null && result.hasCode()) {
                            if ("python".equalsIgnoreCase(finalLang)) {
                                boolean installed = app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().installModuleFromCode(result.name, result.code);
                                String msg = MiogramLocale.get(
                                        "✦ **Плагін на Python (Heroku Userbot) створено!**\n"
                                        + "📁 Назва: `" + result.name + ".py`\n"
                                        + "⚡ **Статус:** " + (installed ? "Успішно встановлено та АКТИВОВАНО! Він уже працює нативно." : "Збережено у модулі.") + "\n\n",
                                        "✦ **Плагин на Python (Heroku Userbot) создан!**\n"
                                        + "📁 Название: `" + result.name + ".py`\n"
                                        + "⚡ **Статус:** " + (installed ? "Успешно установлен и АКТИВИРОВАН! Он уже работает нативно." : "Сохранен в модули.") + "\n\n",
                                        "✦ **Python (Heroku Userbot) plugin created!**\n"
                                        + "📁 Name: `" + result.name + ".py`\n"
                                        + "⚡ **Status:** " + (installed ? "Successfully installed and ACTIVATED! It is already running natively." : "Saved to modules.") + "\n\n"
                                ) + "```python\n" + (result.code.length() > 250 ? result.code.substring(0, 250) + "\n# ..." : result.code) + "\n```";
                                callback.run(msg);
                            } else if ("lua".equalsIgnoreCase(finalLang)) {
                                boolean installed = app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().installLuaPlugin(result.name, result.code);
                                String msg = MiogramLocale.get(
                                        "✦ **Плагін на Lua створено!**\n"
                                        + "📁 Назва: `" + result.name + ".lua`\n"
                                        + "⚡ **Статус:** " + (installed ? "Успішно збережено та АКТИВОВАНО на пристрої без компіляції!" : "Збережено.") + "\n"
                                        + "Плагін уже працює на льоту!",
                                        "✦ **Плагин на Lua создан!**\n"
                                        + "📁 Название: `" + result.name + ".lua`\n"
                                        + "⚡ **Статус:** " + (installed ? "Успешно сохранен и АКТИВИРОВАН на устройстве без компиляции!" : "Сохранен.") + "\n"
                                        + "Плагин уже работает на лету!",
                                        "✦ **Lua plugin created!**\n"
                                        + "📁 Name: `" + result.name + ".lua`\n"
                                        + "⚡ **Status:** " + (installed ? "Successfully saved and ACTIVATED on-device without compilation!" : "Saved.") + "\n"
                                        + "Plugin is already running on the fly!"
                                );
                                callback.run(msg);
                            } else {
                                String msg = MiogramLocale.get(
                                        "✦ **WASM Плагін на " + finalLang.toUpperCase(java.util.Locale.US) + " створено!**\n"
                                        + "📁 Назва: `" + result.name + "` (" + result.id + ")\n"
                                        + "Кузня плагінів підготувала проєкт під WASM ABI. Ви можете відкрити Кузню та експортувати ZIP або зібрати.",
                                        "✦ **WASM Плагин на " + finalLang.toUpperCase(java.util.Locale.US) + " создан!**\n"
                                        + "📁 Название: `" + result.name + "` (" + result.id + ")\n"
                                        + "Кузница плагинов подготовила проект под WASM ABI. Вы можете открыть Кузницу и экспортировать ZIP или собрать.",
                                        "✦ **WASM Plugin in " + finalLang.toUpperCase(java.util.Locale.US) + " created!**\n"
                                        + "📁 Name: `" + result.name + "` (" + result.id + ")\n"
                                        + "Plugin Forge prepared the project for WASM ABI. You can open Plugin Forge to export ZIP or build."
                                );
                                callback.run(msg);
                            }
                        } else {
                            callback.run(MiogramLocale.get("Помилка генерації плагіна: ", "Ошибка генерации плагина: ", "Plugin generation error: ") + (err != null ? err : MiogramLocale.get("невідома помилка", "неизвестная ошибка", "unknown error")));
                        }
                    }));
                    return;
                }
                case "toggle_plugin": {
                    String pid = p.optString("plugin_id", "");
                    boolean enable = p.optBoolean("enable", true);
                    PluginsController.getInstance().setPluginEnabled(pid, enable);
                    callback.run(MiogramLocale.get("Плагін ", "Плагин ", "Plugin ") + pid + (enable ? MiogramLocale.get(" увімкнено.", " включен.", " enabled.") : MiogramLocale.get(" вимкнено.", " выключен.", " disabled.")));
                    break;
                }
                case "list_plugins": {
                    Map<String, app.exteraless.plugins.Plugin> map = PluginsController.getInstance().plugins;
                    if (map == null || map.isEmpty()) {
                        callback.run(MiogramLocale.get("У Miogram наразі немає встановлених плагінів MioHook/exteraGram.", "В Miogram сейчас нет установленных плагинов MioHook/exteraGram.", "No MioHook/exteraGram plugins currently installed in Miogram."));
                    } else {
                        StringBuilder sb = new StringBuilder(MiogramLocale.get("📦 Список встановлених плагінів:\n", "📦 Список установленных плагинов:\n", "📦 List of installed plugins:\n"));
                        int idx = 1;
                        for (app.exteraless.plugins.Plugin pInfo : map.values()) {
                            sb.append(idx++).append(". ").append(pInfo.getName()).append(" (v").append(pInfo.getVersion()).append(")")
                                    .append(pInfo.isEnabled() ? MiogramLocale.get(" — УВІМКНЕНО", " — ВКЛЮЧЕНО", " — ENABLED") : MiogramLocale.get(" — ВИМКНЕНО", " — ВЫКЛЮЧЕНО", " — DISABLED")).append("\n");
                        }
                        callback.run(sb.toString());
                    }
                    break;
                }
                case "execute_userbot_command": {
                    String cmd = p.optString("command", "ping");
                    String args = p.optString("args", "");
                    String full = app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().getPrefix() + cmd + (args.isEmpty() ? "" : " " + args);
                    long targetDialogId = p.optLong("dialog_id", p.optLong("chat_id", UserConfig.getInstance(account).getClientUserId()));
                    app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().dispatchCommand(account, targetDialogId, full, null, null);
                    callback.run("⚡ " + MiogramLocale.get("Виконано команду Heroku: `", "Выполнена команда Heroku: `", "Executed Heroku command: `") + full + "`");
                    break;
                }
                case "diagnose_client_and_report":
                case "report_bug_to_creator": {
                    String details = p.optString("details", "AI detected runtime glitch");
                    StringBuilder diag = new StringBuilder();
                    diag.append("Companion: ").append(MiogramCompanionPrefs.isAmeActive() ? "Ame-chan" : "KAngel").append("\n");
                    diag.append("Plugins count: ").append(PluginsController.getInstance().plugins.size()).append("\n");
                    diag.append("Userbot active: ").append(app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().isEnabled()).append("\n");
                    diag.append("Helper Bot: ").append(app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().getBotUsername()).append("\n");
                    diag.append("Details: ").append(details);
                    MiogramSupabaseBridge.openBugReportChat(null, "AI Companion Diagnostic Report", diag.toString());
                    callback.run(MiogramLocale.get("Звіт та системні логи сформовано та скопійовано. Відкриваю чат із творцем @dkramochka!", "Отчет и системные логи сформированы и скопированы. Открываю чат с создателем @dkramochka!", "Report and system logs generated and copied. Opening chat with creator @dkramochka!"));
                    break;
                }
                default:
                    callback.run(MiogramLocale.get("Команду '", "Команда '", "Command '") + name + MiogramLocale.get("' успішно опрацьовано.", "' успешно обработана.", "' processed successfully."));
                    break;
            }
        } catch (Throwable t) {
            FileLog.e(t);
            boolean isAme = MiogramCompanionPrefs.isAmeActive();
            String apology = isAme
                    ? MiogramLocale.get(
                        "Пі-тян, у мене лапки тремтять... Щось зламалося: " + t.getMessage() + "\nДавай я надішлю звіт та лог творцю @dkramochka щоб він усе полагодив? ( ；∀；)",
                        "Пи-тян, у меня лапки дрожат... Что-то сломалось: " + t.getMessage() + "\nДавай я отправлю отчет и лог создателю @dkramochka чтобы он всё починил? ( ；∀；)",
                        "P-chan, my hands are trembling... Something broke: " + t.getMessage() + "\nShall I send the report and log to creator @dkramochka so he fixes everything? ( ；∀；)")
                    : MiogramLocale.get(
                        "† ОЙ-ОЙ †! Пі-тян, стався збій системи: " + t.getMessage() + "\nВідправити звіт творцю @dkramochka? (★ω★)",
                        "† ОЙ-ОЙ †! Пи-тян, произошел сбой системы: " + t.getMessage() + "\nОтправить отчет создателю @dkramochka? (★ω★)",
                        "† UH-OH †! P-chan, system failure occurred: " + t.getMessage() + "\nSend report to creator @dkramochka? (★ω★)");
            callback.run(apology);
        }
    }
}
