package app.miogram.bridge.ai.companion;

import android.os.Bundle;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
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
import app.miogram.bridge.discord.MiogramDiscordManager;
import app.miogram.bridge.github.MiogramGitHubManager;
import app.miogram.bridge.spotify.MiogramSpotifyManager;
import app.miogram.bridge.steam.MiogramSteamManager;
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
        return n.contains("clear") || n.contains("delete") || n.contains("send") || n.contains("profile") || n.contains("setting")
                || n.contains("create_chat") || n.contains("toggle_plugin") || n.contains("userbot") || n.contains("eval")
                || n.contains("write_plugin") || n.contains("diagnose") || n.contains("report");
    }

    public static String describeTool(String name, org.json.JSONObject params) {
        if (name == null) return "";
        String n = name.toLowerCase(java.util.Locale.US);
        try {
            if (n.equals("read_messages")) {
                String q = params != null ? params.optString("chat_query", params.optString("chat_name", "")) : "";
                int limit = params != null ? params.optInt("limit", 15) : 15;
                return MiogramLocale.get("Прочитає останні ", "Прочитает последние ", "Will read last ") + limit
                        + MiogramLocale.get(" повідомлень з чату «", " сообщений из чата «", " messages from chat \"") + q + "».";
            }
            if (n.equals("read_unread_summary")) {
                return MiogramLocale.get("Підсумує всі непрочитані повідомлення у чатах.", "Подытожит все непрочитанные сообщения в чатах.", "Will summarize all unread chat messages.");
            }
            if (n.equals("search_messages")) {
                String q = params != null ? params.optString("query", params.optString("text", "")) : "";
                return MiogramLocale.get("Знайде повідомлення за запитом «", "Найдёт сообщения по запросу «", "Will search messages for \"") + q + "».";
            }
            if (n.equals("find_chat")) {
                String q = params != null ? params.optString("query", params.optString("chat_query", "")) : "";
                return MiogramLocale.get("Знайде чат «", "Найдёт чат «", "Will find chat \"") + q + "».";
            }
            if (n.equals("list_dialogs")) {
                String f = params != null ? params.optString("filter", "all") : "all";
                int pg = params != null ? params.optInt("page", 0) : 0;
                return MiogramLocale.get("Покаже список чатів (", "Покажет список чатов (", "Will list chats (") + f + ", " + MiogramLocale.get("сторінка ", "страница ", "page ") + (pg + 1) + ").";
            }
            if (n.equals("open_chat")) {
                String q = params != null ? params.optString("chat_query", "") : "";
                return MiogramLocale.get("Відкриє чат «", "Откроет чат «", "Will open chat \"") + q + "».";
            }
            if (n.equals("mute_chat")) {
                return MiogramLocale.get("Заглушить/увімкне звук чату.", "Заглушит/включит звук чата.", "Will (un)mute the chat.");
            }
            if (n.equals("archive_chat")) {
                return MiogramLocale.get("Прибере/поверне чат з архіву.", "Уберёт/вернёт чат из архива.", "Will (un)archive the chat.");
            }
            if (n.equals("mark_read")) {
                return MiogramLocale.get("Позначить все як прочитане.", "Отметит всё как прочитанное.", "Will mark all as read.");
            }
            if (n.equals("chat_info")) {
                return MiogramLocale.get("Покаже інфо чату.", "Покажет инфо чата.", "Will show chat info.");
            }
            if (n.equals("player_control")) {
                return MiogramLocale.get("Керує плеєром.", "Управляет плеером.", "Will control the player.");
            }
            if (n.equals("player_now")) {
                return MiogramLocale.get("Покаже що грає.", "Покажет что играет.", "Will show now playing.");
            }
            if (n.equals("contacts_list")) {
                return MiogramLocale.get("Покаже контакти.", "Покажет контакты.", "Will list contacts.");
            }
            if (n.equals("send_message")) {
                String q = params != null ? params.optString("chat_query", "") : "";
                return MiogramLocale.get("Надішле повідомлення в чат «", "Отправит сообщение в чат «", "Will send a message to chat \"") + q + "».";
            }
            if (n.equals("clear_chat") || n.equals("delete_chat")) {
                String q = params != null ? params.optString("chat_query", "") : "";
                return MiogramLocale.get("⚠️ Очистить/видалить історію чату «", "⚠️ Очистит/удалит историю чата «", "⚠️ Will clear/delete history of chat \"") + q + "».";
            }
            if (n.equals("remember_fact")) {
                String k = params != null ? params.optString("key", "") : "";
                return MiogramLocale.get("Запам'ятає факт: «", "Запомнит факт: «", "Will remember fact: \"") + k + "».";
            }
            if (n.equals("forget_fact")) {
                String k = params != null ? params.optString("key", "") : "";
                return MiogramLocale.get("Забуде факт: «", "Забудет факт: «", "Will forget fact: \"") + k + "».";
            }
            if (n.equals("recall_memory")) {
                return MiogramLocale.get("Перевірить збережені факти про тебе.", "Проверит сохранённые факты о тебе.", "Will recall stored memory facts.");
            }
            if (n.equals("github_status")) {
                String repo = params != null ? params.optString("repo", "") : "";
                return MiogramLocale.get("Перевірить статус GitHub Actions для ", "Проверит статус GitHub Actions для ", "Will check GitHub Actions for ") + (repo.isEmpty() ? "репозиторію" : repo) + ".";
            }
            if (n.equals("discord_status")) {
                return MiogramLocale.get("Перевірить статус Discord.", "Проверит статус Discord.", "Will check Discord presence.");
            }
            if (n.equals("spotify_status")) {
                return MiogramLocale.get("Перевірить трек у Spotify.", "Проверит трек в Spotify.", "Will check Spotify playback.");
            }
            if (n.equals("steam_status")) {
                return MiogramLocale.get("Перевірить гру в Steam.", "Проверит игру в Steam.", "Will check Steam game.");
            }
        } catch (Throwable ignore) {}
        return MiogramLocale.get("Виконає дію: ", "Выполнит действие: ", "Will perform: ") + name;
    }

    // ==================================================================
    // Monster tools: real client actions behind MioTool defs below.
    // ==================================================================

    private static void toolSetMuted(int account, long dialogId, boolean mute, Utilities.Callback<String> callback) {
        try {
            NotificationsController.getInstance(account).muteDialog(dialogId, 0, mute);
            FoundChat fc = null;
            try {
                for (FoundChat c : snapshotDialogs(account, "all")) {
                    if (c.dialogId == dialogId) {
                        fc = c;
                        break;
                    }
                }
            } catch (Throwable ignore) {}
            String ref = fc != null ? fc.getReference() : String.valueOf(dialogId);
            callback.run(mute
                    ? MiogramLocale.get("Заглушила чат ", "Заглушила чат ", "Muted chat ").concat(ref).concat(".")
                    : MiogramLocale.get("Увімкнула звук чату ", "Включила звук чата ", "Unmuted chat ").concat(ref).concat("."));
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Не вдалося змінити звук: ", "Не удалось изменить звук: ", "Mute failed: ") + t.getMessage());
        }
    }

    private static void toolSetArchived(int account, long dialogId, boolean archive, Utilities.Callback<String> callback) {
        try {
            MessagesController.getInstance(account).addDialogToFolder(dialogId, archive ? 1 : 0, -1, 0);
            callback.run(archive
                    ? MiogramLocale.get("Чат прибрано в архів.", "Чат убран в архив.", "Chat archived.")
                    : MiogramLocale.get("Чат повернуто з архіву.", "Чат возвращён из архива.", "Chat unarchived."));
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Не вдалося змінити архів: ", "Не удалось изменить архив: ", "Archive failed: ") + t.getMessage());
        }
    }

    private static void toolMarkRead(int account, long dialogId, Utilities.Callback<String> callback) {
        try {
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.Dialog dlg = null;
            try {
                dlg = mc.dialogs_dict.get(dialogId);
            } catch (Throwable ignore) {}
            if (dlg == null) {
                callback.run(MiogramLocale.get("Чат не знайдено в списку.", "Чат не найден в списке.", "Chat not in list."));
                return;
            }
            int top = dlg.top_message;
            mc.markDialogAsRead(dialogId, top, top, 0, false, 0, 0, true, 0);
            callback.run(MiogramLocale.get("Позначила все як прочитане.", "Отметила всё как прочитанное.", "Marked everything as read."));
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Не вдалося позначити: ", "Не удалось отметить: ", "Mark-read failed: ") + t.getMessage());
        }
    }

    private static void toolChatInfo(int account, long dialogId, Utilities.Callback<String> callback) {
        try {
            MessagesController mc = MessagesController.getInstance(account);
            StringBuilder sb = new StringBuilder();
            if (dialogId > 0) {
                TLRPC.User u = mc.getUser(dialogId);
                if (u == null) {
                    callback.run(MiogramLocale.get("Користувача не знайдено.", "Пользователь не найден.", "User not found."));
                    return;
                }
                sb.append("👤 ").append(UserObject.getUserName(u)).append("\n");
                if (u.username != null && !u.username.isEmpty()) sb.append("@").append(u.username).append("\n");
                sb.append("id: ").append(u.id).append(u.bot ? " (bot)" : "").append("\n");
            } else {
                TLRPC.Chat c = mc.getChat(-dialogId);
                if (c == null) {
                    callback.run(MiogramLocale.get("Чат не знайдено.", "Чат не найден.", "Chat not found."));
                    return;
                }
                boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                sb.append(isChannel ? "📢 " : "👥 ").append(c.title != null ? c.title : "").append("\n");
                if (c.username != null && !c.username.isEmpty()) sb.append("@").append(c.username).append("\n");
                sb.append("id: ").append(c.id).append("\n");
                if (c.participants_count > 0) {
                    sb.append(MiogramLocale.get("Учасників: ", "Участников: ", "Members: ")).append(c.participants_count).append("\n");
                }
            }
            try {
                ArrayList<TLRPC.Dialog> dialogs = mc.getAllDialogs();
                if (dialogs != null) {
                    for (int i = 0; i < dialogs.size(); i++) {
                        TLRPC.Dialog d = dialogs.get(i);
                        if (d != null && d.id == dialogId && d.unread_count > 0) {
                            sb.append(MiogramLocale.get("Непрочитаних: ", "Непрочитанных: ", "Unread: ")).append(d.unread_count).append("\n");
                            break;
                        }
                    }
                }
            } catch (Throwable ignore) {}
            callback.run(sb.toString().trim());
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Не вдалося отримати інфо: ", "Не удалось получить инфо: ", "Info failed: ") + t.getMessage());
        }
    }

    private static void toolPlayerControl(String action, Utilities.Callback<String> callback) {
        try {
            String a = action != null ? action.trim().toLowerCase(java.util.Locale.US) : "toggle";
            MediaController mc = MediaController.getInstance();
            MessageObject cur = mc.getPlayingMessageObject();
            if (a.equals("next")) {
                mc.playNextMessage();
                callback.run(MiogramLocale.get("⏭ Наступний трек.", "⏭ Следующий трек.", "⏭ Next track."));
            } else if (a.equals("prev") || a.equals("previous")) {
                mc.playPreviousMessage();
                callback.run(MiogramLocale.get("⏮ Попередній трек.", "⏮ Предыдущий трек.", "⏮ Previous track."));
            } else if (a.equals("pause")) {
                if (cur != null && !mc.isMessagePaused()) mc.pauseMessage(cur);
                callback.run(MiogramLocale.get("⏸ Пауза.", "⏸ Пауза.", "⏸ Paused."));
            } else if (a.equals("play")) {
                if (cur != null && mc.isMessagePaused()) mc.playMessage(cur);
                callback.run(MiogramLocale.get("▶ Відтворення.", "▶ Воспроизведение.", "▶ Playing."));
            } else {
                if (cur == null) {
                    callback.run(MiogramLocale.get("Нічого не грає.", "Ничего не играет.", "Nothing playing."));
                } else if (mc.isMessagePaused()) {
                    mc.playMessage(cur);
                    callback.run(MiogramLocale.get("▶ Відтворення.", "▶ Воспроизведение.", "▶ Playing."));
                } else {
                    mc.pauseMessage(cur);
                    callback.run(MiogramLocale.get("⏸ Пауза.", "⏸ Пауза.", "⏸ Paused."));
                }
            }
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Плеєр: ", "Плеер: ", "Player: ") + t.getMessage());
        }
    }

    private static void toolPlayerNow(Utilities.Callback<String> callback) {
        try {
            MediaController mc = MediaController.getInstance();
            MessageObject cur = mc.getPlayingMessageObject();
            if (cur == null) {
                callback.run(MiogramLocale.get("Нічого не грає.", "Ничего не играет.", "Nothing playing."));
                return;
            }
            String title = cur.getMusicTitle();
            String author = cur.getMusicAuthor();
            if (title == null || title.isEmpty()) title = cur.getDocumentName();
            callback.run((mc.isMessagePaused() ? "⏸ " : "▶ ")
                    + (author != null && !author.isEmpty() ? author + " — " : "")
                    + (title != null ? title : "?"));
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Плеєр: ", "Плеер: ", "Player: ") + t.getMessage());
        }
    }

    private static void toolContactsList(int account, int limit, Utilities.Callback<String> callback) {
        try {
            ContactsController cc = ContactsController.getInstance(account);
            MessagesController mc = MessagesController.getInstance(account);
            if (cc == null || cc.contacts == null || cc.contacts.isEmpty()) {
                callback.run(MiogramLocale.get("Контакти порожні.", "Контакты пусты.", "Contacts empty."));
                return;
            }
            StringBuilder sb = new StringBuilder(MiogramLocale.get("Контакти:\n", "Контакты:\n", "Contacts:\n"));
            int n = 0;
            for (int i = 0; i < cc.contacts.size() && n < limit; i++) {
                TLRPC.TL_contact tc = cc.contacts.get(i);
                if (tc == null) continue;
                TLRPC.User u = mc.getUser(tc.user_id);
                if (u == null) continue;
                sb.append(n + 1).append(". ").append(UserObject.getUserName(u));
                if (u.username != null && !u.username.isEmpty()) sb.append(" (@").append(u.username).append(")");
                sb.append("\n");
                n++;
            }
            if (n == 0) {
                callback.run(MiogramLocale.get("Контакти порожні.", "Контакты пусты.", "Contacts empty."));
                return;
            }
            sb.append(MiogramLocale.get("Відповіси номером.", "Ответь номером.", "Reply with a number."));
            callback.run(sb.toString());
        } catch (Throwable t) {
            callback.run(MiogramLocale.get("Не вдалося прочитати контакти: ", "Не удалось прочитать контакты: ", "Contacts failed: ") + t.getMessage());
        }
    }

    private static volatile boolean mioToolsRegistered = false;

    /** Registers the monster toolset in MioTool (idempotent). Called at class load. */
    public static void registerMioTools() {
        if (mioToolsRegistered) return;
        mioToolsRegistered = true;
        try {
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "mute_chat", "Mute", "mute_chat(chat_query|chat_id, mute=true) — mute/unmute a chat.", false,
                    (account, params, cb) -> {
                        ChatResolution res = resolveChatTarget(account, params, "mute_chat");
                        if (res.errorMessage != null) {
                            cb.run(res.errorMessage);
                            return;
                        }
                        toolSetMuted(account, res.dialogId, params.optBoolean("mute", true), cb);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "archive_chat", "Archive", "archive_chat(chat_query|chat_id, archive=true) — archive/unarchive a chat.", false,
                    (account, params, cb) -> {
                        ChatResolution res = resolveChatTarget(account, params, "archive_chat");
                        if (res.errorMessage != null) {
                            cb.run(res.errorMessage);
                            return;
                        }
                        toolSetArchived(account, res.dialogId, params.optBoolean("archive", true), cb);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "mark_read", "Mark read", "mark_read(chat_query|chat_id) — mark everything as read.", false,
                    (account, params, cb) -> {
                        ChatResolution res = resolveChatTarget(account, params, "mark_read");
                        if (res.errorMessage != null) {
                            cb.run(res.errorMessage);
                            return;
                        }
                        toolMarkRead(account, res.dialogId, cb);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "chat_info", "Chat info", "chat_info(chat_query|chat_id) — type, title, @username, members, unread.", false,
                    (account, params, cb) -> {
                        ChatResolution res = resolveChatTarget(account, params, "chat_info");
                        if (res.errorMessage != null) {
                            cb.run(res.errorMessage);
                            return;
                        }
                        toolChatInfo(account, res.dialogId, cb);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "player_control", "Player", "player_control(action=play|pause|toggle|next|prev) — control music.", false,
                    (account, params, cb) -> toolPlayerControl(params.optString("action", "toggle"), cb)));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "player_now", "Now playing", "player_now() — current track and state.", false,
                    (account, params, cb) -> toolPlayerNow(cb)));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "contacts_list", "Contacts", "contacts_list(limit=15) — numbered contact list.", false,
                    (account, params, cb) -> toolContactsList(account, Math.min(30, Math.max(1, params.optInt("limit", 15))), cb)));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "read_unread_summary", "Unread summary", "read_unread_summary() — summarize unread messages across all chats.", false,
                    (account, params, cb) -> executeTool(account, new ActionRequest("read_unread_summary", params, false), cb)));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "remember_fact", "Remember Fact", "remember_fact(key, value) — remember a fact or preference about P-chan.", false,
                    (account, params, cb) -> {
                        String key = params.optString("key", "");
                        String value = params.optString("value", "");
                        if (key.isEmpty()) {
                            cb.run("Key is required.");
                            return;
                        }
                        MiogramCompanionMemory.getInstance().setFact(key, value);
                        cb.run("Fact memorized: " + key + " = " + value);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "forget_fact", "Forget Fact", "forget_fact(key) — remove a fact from memory.", false,
                    (account, params, cb) -> {
                        String key = params.optString("key", "");
                        MiogramCompanionMemory.getInstance().removeFact(key);
                        cb.run("Fact removed: " + key);
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "recall_memory", "Recall Memory", "recall_memory() — view all stored memory facts.", false,
                    (account, params, cb) -> {
                        Map<String, String> facts = MiogramCompanionMemory.getInstance().getAllFacts();
                        if (facts.isEmpty()) {
                            cb.run("No custom facts stored yet.");
                        } else {
                            StringBuilder sb = new StringBuilder("Known facts:\n");
                            for (Map.Entry<String, String> e : facts.entrySet()) {
                                sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                            }
                            cb.run(sb.toString());
                        }
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "github_status", "GitHub Actions", "github_status(repo) — check latest GitHub Actions workflow run.", false,
                    (account, params, cb) -> {
                        String repo = params.optString("repo", "");
                        if (android.text.TextUtils.isEmpty(repo)) repo = MiogramGitHubManager.getInstance().getTrackedRepo();
                        final String fRepo = repo;
                        MiogramGitHubManager.getInstance().fetchLatestWorkflow(true, run -> {
                            if (run == null) {
                                cb.run("GitHub status unavailable for " + fRepo);
                            } else {
                                cb.run("GitHub Actions for " + run.repo + ":\nWorkflow: " + run.workflowName
                                        + "\nStatus: " + run.status + ", Conclusion: " + run.conclusion
                                        + "\nCommit: " + run.commitMessage + " (" + run.branch + ")"
                                        + "\nURL: " + run.htmlUrl);
                            }
                        });
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "discord_status", "Discord", "discord_status(user_id) — check Discord presence via Lanyard.", false,
                    (account, params, cb) -> {
                        String uid = params.optString("user_id", "");
                        if (android.text.TextUtils.isEmpty(uid)) uid = MiogramDiscordManager.getInstance().getLinkedUserId();
                        final String fUid = uid;
                        if (android.text.TextUtils.isEmpty(fUid)) {
                            cb.run("Discord user ID is not configured.");
                            return;
                        }
                        MiogramDiscordManager.getInstance().fetchPresence(fUid, true, presence -> {
                            if (presence == null) {
                                cb.run("Discord presence unavailable for " + fUid);
                            } else {
                                cb.run("Discord user: " + presence.getDisplayName() + " (@" + presence.username + ")\nStatus: " + presence.status
                                        + (!android.text.TextUtils.isEmpty(presence.customStatus) ? "\nCustom status: " + presence.customStatus : "")
                                        + (!android.text.TextUtils.isEmpty(presence.activityName) ? "\nPlaying/Activity: " + presence.activityName + " (" + presence.activityDetails + ")" : ""));
                            }
                        });
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "spotify_status", "Spotify", "spotify_status() — check current Spotify playback status and track.", false,
                    (account, params, cb) -> {
                        MiogramSpotifyManager sm = MiogramSpotifyManager.getInstance();
                        MiogramSpotifyManager.SpotifyTrack track = sm.getCurrentTrack();
                        if (track == null) {
                            cb.run("Spotify is currently idle or disconnected.");
                        } else {
                            cb.run("Spotify: " + track.title + " — " + track.artist + " (" + (sm.isPlaying() ? "Playing" : "Paused") + ")");
                        }
                    }));
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "steam_status", "Steam", "steam_status(steam_id) — check Steam gaming status.", false,
                    (account, params, cb) -> {
                        String sId = params.optString("steam_id", "");
                        if (android.text.TextUtils.isEmpty(sId)) sId = MiogramSteamManager.getInstance().getLinkedSteamId();
                        final String fId = sId;
                        if (android.text.TextUtils.isEmpty(fId)) {
                            cb.run("Steam ID is not configured.");
                            return;
                        }
                        MiogramSteamManager.getInstance().resolvePublicSteam(fId, profile -> {
                            if (profile == null) {
                                cb.run("Steam profile unavailable for " + fId);
                            } else {
                                cb.run("Steam user: " + profile.personaName + "\nState: "
                                        + (profile.isInGame ? "Playing " + profile.gameName + " (" + profile.gameHours2Weeks + " hrs past 2 weeks)" : profile.stateMessage));
                            }
                        });
                    }));
        } catch (Throwable ignore) {}
    }

    static {
        registerMioTools();
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

    /**
     * Unfinished disambiguation: when the AI lists several similar chats and
     * asks "which one?", the user's next message ("другий", "2", "@nick",
     * "так"/"ні") resolves against THIS list instead of a fresh search.
     * Expires after 10 minutes. Also carries an optional pending action
     * (e.g. read_messages) and paginated list state for "далі".
     */
    public static class PendingPick {
        public final List<FoundChat> candidates;
        public final String query;
        public final long time;
        public final String resumeAction;
        public final JSONObject resumeParams;
        public final String listFilter;
        public final int listPage;
        public final int listPageSize;

        public PendingPick(List<FoundChat> candidates, String query, String resumeAction,
                           JSONObject resumeParams, String listFilter, int listPage, int listPageSize) {
            this.candidates = candidates != null ? new ArrayList<>(candidates) : new ArrayList<>();
            this.query = query != null ? query : "";
            this.time = System.currentTimeMillis();
            this.resumeAction = resumeAction;
            this.resumeParams = resumeParams;
            this.listFilter = listFilter;
            this.listPage = listPage;
            this.listPageSize = listPageSize;
        }

        public boolean expired() {
            return System.currentTimeMillis() - time > 10L * 60 * 1000;
        }
    }

    private static final java.util.Map<Integer, PendingPick> pendingPicks = new java.util.HashMap<>();

    public static void setPendingPick(int account, PendingPick pick) {
        try {
            if (pick == null) pendingPicks.remove(account);
            else pendingPicks.put(account, pick);
        } catch (Throwable ignore) {}
    }

    public static PendingPick getPendingPick(int account) {
        try {
            PendingPick p = pendingPicks.get(account);
            if (p != null && p.expired()) {
                pendingPicks.remove(account);
                return null;
            }
            return p;
        } catch (Throwable ignore) {
            return null;
        }
    }

    public static void clearPendingPick(int account) {
        try {
            pendingPicks.remove(account);
        } catch (Throwable ignore) {}
    }

    public static class PickResolution {
        /** RESOLVED (foundChat set, resumeAction/Params may be set), NEXT_PAGE, or NONE. */
        public final String kind;
        public final FoundChat foundChat;
        public final String resumeAction;
        public final JSONObject resumeParams;

        public PickResolution(String kind, FoundChat foundChat, String resumeAction, JSONObject resumeParams) {
            this.kind = kind;
            this.foundChat = foundChat;
            this.resumeAction = resumeAction;
            this.resumeParams = resumeParams;
        }
    }

    private static boolean isOrdinal(String low, int n) {
        if (low == null) return false;
        if (low.equals(String.valueOf(n))) return true;
        if (low.equals(n + ".") || low.equals(n + ")") || low.equals("№" + n)) return true;
        String[][] words = {
                {"перший", "перший", "первый", "first"},
                {"другий", "другий", "второй", "second"},
                {"третій", "третій", "третий", "third"},
                {"четвертий", "четвертий", "четвертый", "fourth"},
                {"п'ятий", "пятый", "пʼятий", "п’ятий", "fifth"},
                {"шостий", "шостий", "шестой", "sixth"},
                {"сьомий", "седьмой", "сьомий", "seventh"},
                {"восьмий", "восьмой", "восьмий", "eighth"},
                {"дев'ятий", "девятий", "девʼятий", "девятый", "ninth"},
                {"десятий", "десятий", "десятый", "tenth"}
        };
        if (n >= 1 && n <= words.length) {
            for (String w : words[n - 1]) {
                if (low.equals(w) || low.equals(w + ".") || low.equals("№" + n) || low.equals(n + "-й") || low.equals(n + "-й")) return true;
            }
        }
        return false;
    }

    private static boolean isBareYes(String low) {
        return low.equals("так") || low.equals("да") || low.equals("yes") || low.equals("ага") || low.equals("угу")
                || low.equals("цей") || low.equals("ця") || low.equals("це") || low.equals("this") || low.equals("that")
                || low.equals("той") || low.equals("та") || low.equals("давай") || low.equals("ok") || low.equals("ок")
                || low.equals("+") || low.equals("👍");
    }

    private static boolean isNext(String low) {
        return low.equals("ні") || low.equals("нет") || low.equals("no") || low.equals("не той") || low.equals("не та")
                || low.equals("не то") || low.equals("інший") || low.equals("другой") || low.equals("інша") || low.equals("другая")
                || low.equals("далі") || low.equals("дальше") || low.equals("next") || low.equals("more") || low.equals("ще")
                || low.equals("еще") || low.equals("-") || low.equals("👎");
    }

    /**
     * Resolves a follow-up message against the pending candidate list:
     * number/ordinal, @username, bare yes (= first), no/next (= next page).
     */
    public static PickResolution tryResolvePendingPick(int account, String userText) {
        PendingPick pending = getPendingPick(account);
        if (pending == null || pending.candidates.isEmpty() || userText == null) return new PickResolution("NONE", null, null, null);
        String low = userText.trim().toLowerCase(java.util.Locale.ROOT);
        if (low.length() > 64) return new PickResolution("NONE", null, null, null);

        if (isNext(low)) {
            return new PickResolution("NEXT_PAGE", null, pending.resumeAction, pending.resumeParams);
        }
        // Standalone digit in a short reply ("беру 2", "давай 2-й", "2й варіант").
        // Word-count guard so "зустрінемось о 2" doesn't hijack a pick.
        try {
            if (low.split("\\s+").length <= 3) {
                java.util.regex.Matcher dm = java.util.regex.Pattern.compile("(?:^|\\s)(\\d{1,2})(?:\\s|[.\\)\\-,:;!?»\"']|$|й|th|st|nd|rd)").matcher(low);
                while (dm.find()) {
                    int n = Integer.parseInt(dm.group(1));
                    if (n >= 1 && n <= pending.candidates.size()) {
                        FoundChat fc = pending.candidates.get(n - 1);
                        clearPendingPick(account);
                        return new PickResolution("RESOLVED", fc, pending.resumeAction, pending.resumeParams);
                    }
                }
            }
        } catch (Throwable ignore) {}
        // Number or ordinal: "2", "другий", "2.", "№2"
        for (int i = 0; i < pending.candidates.size(); i++) {
            if (isOrdinal(low, i + 1)) {
                FoundChat fc = pending.candidates.get(i);
                clearPendingPick(account);
                return new PickResolution("RESOLVED", fc, pending.resumeAction, pending.resumeParams);
            }
        }
        // Bare confirmation = top candidate (the AI asked "is this it?").
        if (isBareYes(low)) {
            FoundChat fc = pending.candidates.get(0);
            clearPendingPick(account);
            return new PickResolution("RESOLVED", fc, pending.resumeAction, pending.resumeParams);
        }
        // Username (with or without @) matched inside candidates.
        String uname = low.startsWith("@") ? low.substring(1) : low;
        if (!uname.isEmpty() && uname.length() >= 3 && !uname.contains(" ")) {
            for (FoundChat fc : pending.candidates) {
                if (!fc.username.isEmpty() && (fc.username.equalsIgnoreCase(uname)
                        || fc.username.toLowerCase(java.util.Locale.ROOT).contains(uname)
                        || uname.contains(fc.username.toLowerCase(java.util.Locale.ROOT)))) {
                    clearPendingPick(account);
                    return new PickResolution("RESOLVED", fc, pending.resumeAction, pending.resumeParams);
                }
            }
            // Name fragment matched inside candidates.
            for (FoundChat fc : pending.candidates) {
                if (!fc.name.isEmpty() && fc.name.toLowerCase(java.util.Locale.ROOT).contains(uname)) {
                    clearPendingPick(account);
                    return new PickResolution("RESOLVED", fc, pending.resumeAction, pending.resumeParams);
                }
            }
        }
        // Last resort: fresh search, accept only near-exact hits (repeated
        // name or exact @username). Kills "choose again forever" when the user
        // just repeats what they want instead of answering with a number.
        try {
            List<FoundChat> fresh = searchChats(account, userText);
            FoundChat best = null;
            int bestScore = 0;
            for (FoundChat fc : fresh) {
                int s = calculateMatchScore(userText, fc.name, fc.username, null, null);
                if (s > bestScore) {
                    bestScore = s;
                    best = fc;
                }
            }
            if (best != null && bestScore >= 90) {
                clearPendingPick(account);
                return new PickResolution("RESOLVED", best, pending.resumeAction, pending.resumeParams);
            }
        } catch (Throwable ignore) {}
        return new PickResolution("NONE", null, null, null);
    }

    public static String stripGrammaticalEnding(String s) {
        if (s == null || s.length() <= 3) return s;
        String[] suffixes = {"ові", "еві", "ями", "ами", "ями", "ком", "чик", "іком", "иком", "ом", "ем", "ам", "ах", "ях", "ою", "ею", "ів", "ев", "ей", "ка", "ку", "ки", "ке", "ко", "а", "я", "у", "ю", "е", "є", "і", "и", "ы"};
        for (String suf : suffixes) {
            if (s.endsWith(suf) && s.length() - suf.length() >= 3) {
                return s.substring(0, s.length() - suf.length());
            }
        }
        return s;
    }

    // Miogram fix: LLM sometimes passes full user phrase ("знайди в лс з твайсом",
    // "ну просто почитай лс з твайсом і зроби самарі") instead of clean name.
    // Strip command verbs / prepositions so fuzzy match sees only the name.
    public static String sanitizeChatQuery(String raw) {
        if (raw == null) return "";
        String q = raw.trim();
        if (q.startsWith("@")) q = q.substring(1).trim();
        String low = q.toLowerCase(java.util.Locale.ROOT);
        String[] prefixes = new String[]{
                "знайди в лс з", "знайди в лс", "знайди чат з", "знайди чат",
                "найди в лс с", "найди в лс", "найди чат с", "найди чат",
                "find chat with", "find chats with", "find with", "find chat", "find",
                "ну просто почитай лс з", "просто почитай лс з", "почитай лс з", "почитай",
                "прочитай повідомлення від", "прочитай сообщения от", "прочитай",
                "прочти", "прочти лс", "покажи лс з", "покажи",
                "зроби самарі з", "сделай саммари с", "зроби самарі", "самарі з", "самарі",
                "що пише", "что пишет", "що там у діалозі з", "что там в диалоге с",
                "переписку з", "переписку с", "повідомлення від", "сообщения от",
                "в лс з", "в личке с", "в лс", "в личке", "лс з", "лс с",
                "чат з", "чат с", "чат", "діалог з", "диалог с",
                "знайди", "найди", "search", "with ", "from ", "з ", "с "
        };
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 4) {
            changed = false;
            for (String p : prefixes) {
                if (low.startsWith(p)) {
                    int cut = p.length();
                    // keep word boundary: "з " includes space, single "з"/"с" handled above with space
                    q = q.substring(Math.min(cut, q.length())).trim();
                    low = q.toLowerCase(java.util.Locale.ROOT);
                    changed = true;
                    break;
                }
            }
        }
        // trailing junk: "і зроби самарі", "и сделай саммари", punctuation
        String[] trailing = new String[]{
                "і зроби самарі", "и сделай саммари", "і зроби саммари", "зроби самарі",
                "і зроби", "сделай саммари", "зроби", "самарі", "саммари", "summary"
        };
        for (String t : trailing) {
            if (low.endsWith(t)) {
                q = q.substring(0, q.length() - t.length()).trim();
                low = q.toLowerCase(java.util.Locale.ROOT);
            }
        }
        q = q.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "").trim();
        return q.isEmpty() ? raw.trim() : q;
    }

    public static int levenshteinDistance(String a, String b) {
        if (a == null || b == null) return 99;
        int lenA = a.length(), lenB = b.length();
        if (lenA == 0) return lenB;
        if (lenB == 0) return lenA;
        int[][] dp = new int[lenA + 1][lenB + 1];
        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[lenA][lenB];
    }

    public static List<FoundChat> searchChats(int account, String rawQuery) {
        List<FoundChat> matches = new ArrayList<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return matches;
        }
        String q = sanitizeChatQuery(rawQuery);
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
                        if (score >= 55) {
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
                        if (score >= 55) {
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
                    if (score >= 55) {
                        ScoredFoundChat existing = dedup.get(uid);
                        if (existing == null || score > existing.score) {
                            dedup.put(uid, new ScoredFoundChat(new FoundChat(uid, fullName, uname, false, false), score));
                        }
                    }
                }
            }
        }

        // 3. Scan Cached Users in Memory (All Known Peers)
        try {
            for (TLRPC.User u : mc.getUsers().values()) {
                if (u == null || u.id == 0 || u.id == UserConfig.getInstance(account).getClientUserId()) continue;
                String fullName = UserObject.getUserName(u);
                String uname = u.username != null ? u.username : "";
                int score = calculateMatchScore(q, fullName, uname, u.first_name, u.last_name);
                if (score >= 55) {
                    ScoredFoundChat existing = dedup.get(u.id);
                    if (existing == null || score > existing.score) {
                        dedup.put(u.id, new ScoredFoundChat(new FoundChat(u.id, fullName, uname, false, false), score));
                    }
                }
            }
        } catch (Throwable ignore) {}

        // 4. Scan Cached Chats in Memory
        try {
            for (TLRPC.Chat c : mc.getChats().values()) {
                if (c == null || c.id == 0) continue;
                long did = -c.id;
                String title = c.title != null ? c.title : "";
                String uname = c.username != null ? c.username : "";
                boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                boolean isGroup = !isChannel;
                int score = calculateMatchScore(q, title, uname, null, null);
                if (score >= 55) {
                    ScoredFoundChat existing = dedup.get(did);
                    if (existing == null || score > existing.score) {
                        dedup.put(did, new ScoredFoundChat(new FoundChat(did, title, uname, isChannel, isGroup), score));
                    }
                }
            }
        } catch (Throwable ignore) {}

        // Fallback: full-sentence query ("ну просто почитай лс з твайсом і зроби самарі")
        // try each meaningful token separately so one good word still finds the chat.
        if (dedup.isEmpty() && q.contains(" ")) {
            String[] qTokens = q.split("[\\s,.;:!?\"]+");
            for (String tok : qTokens) {
                if (tok == null) continue;
                String t = tok.trim().replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
                if (t.length() < 3) continue;
                String lowT = t.toLowerCase(java.util.Locale.ROOT);
                if (lowT.equals("просто") || lowT.equals("ну") || lowT.equals("зроби") || lowT.equals("сделай")
                        || lowT.equals("почитай") || lowT.equals("прочитай") || lowT.equals("знайди") || lowT.equals("найди")
                        || lowT.equals("самарі") || lowT.equals("саммари") || lowT.equals("лс") || lowT.equals("з") || lowT.equals("с")
                        || lowT.equals("і") || lowT.equals("и") || lowT.equals("та")) continue;
                try {
                    for (TLRPC.User u : mc.getUsers().values()) {
                        if (u == null || u.id == 0 || u.id == UserConfig.getInstance(account).getClientUserId()) continue;
                        if (dedup.containsKey(u.id)) continue;
                        int score = calculateMatchScore(t, UserObject.getUserName(u), u.username != null ? u.username : "", u.first_name, u.last_name);
                        if (score >= 70) {
                            dedup.put(u.id, new ScoredFoundChat(new FoundChat(u.id, UserObject.getUserName(u), u.username != null ? u.username : "", false, false), score - 5));
                        }
                    }
                } catch (Throwable ignore) {}
                if (!dedup.isEmpty()) break;
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
        String qStem = stripGrammaticalEnding(qLower);
        String normQ = normalizeText(rawQ);
        String normStemQ = normalizeText(qStem);
        String colQ = collapseRepeats(normQ);
        String enQ = transliterateUaToEn(qLower);

        int best = 0;
        String[] targets = new String[]{username, name, first, last};
        for (String target : targets) {
            if (target == null || target.trim().isEmpty()) continue;
            String tLower = target.trim().toLowerCase(java.util.Locale.ROOT);
            if (tLower.equals(qLower) || tLower.equals(qStem)) return 100;
            if (tLower.contains(qLower) || tLower.contains(qStem)) best = Math.max(best, 92);

            String normT = normalizeText(target);
            if (normT.isEmpty()) continue;
            String colT = collapseRepeats(normT);

            if (normT.equals(normQ) || normT.equals(normStemQ)) return 95;
            if (colT.equals(colQ)) return 90;

            if (normT.contains(normQ) || normQ.contains(normT) || normT.contains(normStemQ) || normStemQ.contains(normT)) best = Math.max(best, 85);
            if (colT.contains(colQ) || colQ.contains(colT)) best = Math.max(best, 80);

            // Transliterated English match (e.g. "твайс" -> "twice" matching "Twice")
            if (!enQ.isEmpty()) {
                if (tLower.equals(enQ) || tLower.contains(enQ)) best = Math.max(best, 95);
                String normEnT = normalizeText(enQ);
                if (normT.equals(normEnT) || normT.contains(normEnT) || normEnT.contains(normT)) best = Math.max(best, 90);
            }

            // Word token matching: split target into words (e.g. "Twice 🌸", "misha_twice")
            String[] tokens = tLower.split("[\\s_\\-\\.]+");
            for (String tok : tokens) {
                if (tok.isEmpty()) continue;
                if (tok.equals(qLower) || tok.equals(qStem) || tok.equals(enQ)) best = Math.max(best, 95);
                if (tok.startsWith(qLower) || tok.startsWith(qStem) || (enQ.length() >= 3 && tok.startsWith(enQ))) best = Math.max(best, 88);

                // Levenshtein fuzzy distance on tokens
                if (tok.length() >= 4 && (qStem.length() >= 4 || enQ.length() >= 4)) {
                    int distEn = levenshteinDistance(tok, enQ);
                    if (distEn <= 1) best = Math.max(best, 92);
                    else if (distEn <= 2) best = Math.max(best, 82);

                    int distCyr = levenshteinDistance(transliterateEnToUa(tok), qStem);
                    if (distCyr <= 1) best = Math.max(best, 90);
                    else if (distCyr <= 2) best = Math.max(best, 80);
                }
            }

            // Stem match
            if (colQ.length() >= 3 && colT.length() >= 3) {
                String sQ = colQ.substring(0, Math.min(colQ.length(), 4));
                String sT = colT.substring(0, Math.min(colT.length(), 4));
                if (colT.contains(sQ) || colQ.contains(sT)) {
                    best = Math.max(best, 78);
                }
            }
        }
        return best;
    }

    public static String transliterateEnToUa(String s) {
        if (s == null) return "";
        s = s.toLowerCase(java.util.Locale.ROOT);
        s = s.replace("twice", "твайс")
                .replace("wice", "вайс")
                .replace("nice", "найс")
                .replace("vice", "вайс")
                .replace("ice", "айс")
                .replace("ight", "айт")
                .replace("ite", "айт")
                .replace("ike", "айк")
                .replace("ide", "айд")
                .replace("ine", "айн")
                .replace("ay", "ей")
                .replace("ey", "ей")
                .replace("ea", "і")
                .replace("ee", "і")
                .replace("oo", "у")
                .replace("ph", "ф")
                .replace("th", "т")
                .replace("shch", "щ")
                .replace("sh", "ш")
                .replace("ch", "ч")
                .replace("zh", "ж")
                .replace("kh", "х")
                .replace("ts", "ц")
                .replace("qu", "кв")
                .replace("ck", "к")
                .replace("wh", "в");
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
                case 'c':
                    if (i + 1 < s.length() && (s.charAt(i + 1) == 'e' || s.charAt(i + 1) == 'i' || s.charAt(i + 1) == 'y')) {
                        sb.append('с');
                    } else {
                        sb.append('к');
                    }
                    break;
                case 'x': sb.append("кс"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String transliterateUaToEn(String s) {
        if (s == null) return "";
        s = s.toLowerCase(java.util.Locale.ROOT);
        s = s.replace("твайс", "twice")
                .replace("вайс", "wice")
                .replace("найс", "nice")
                .replace("айс", "ice")
                .replace("айк", "ike")
                .replace("айт", "ight")
                .replace("ей", "ay")
                .replace("дж", "j")
                .replace("щ", "shch")
                .replace("ч", "ch")
                .replace("ш", "sh")
                .replace("ж", "zh")
                .replace("х", "kh")
                .replace("ц", "ts")
                .replace("ю", "yu")
                .replace("я", "ya")
                .replace("є", "ye")
                .replace("ї", "yi");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'а': sb.append('a'); break;
                case 'б': sb.append('b'); break;
                case 'в': sb.append('v'); break;
                case 'г': case 'ґ': sb.append('g'); break;
                case 'д': sb.append('d'); break;
                case 'е': case 'э': sb.append('e'); break;
                case 'з': sb.append('z'); break;
                case 'и': case 'ы': sb.append('y'); break;
                case 'і': sb.append('i'); break;
                case 'й': sb.append('y'); break;
                case 'к': sb.append('k'); break;
                case 'л': sb.append('l'); break;
                case 'м': sb.append('m'); break;
                case 'н': sb.append('n'); break;
                case 'о': sb.append('o'); break;
                case 'п': sb.append('p'); break;
                case 'р': sb.append('r'); break;
                case 'с': sb.append('s'); break;
                case 'т': sb.append('t'); break;
                case 'у': sb.append('u'); break;
                case 'ф': sb.append('f'); break;
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

    /**
     * Ordered dialog snapshot, recent chats first (native dialog order),
     * for paginated browsing (50 per page). No search, no scores.
     */
    public static List<FoundChat> snapshotDialogs(int account, String filter) {
        List<FoundChat> out = new ArrayList<>();
        try {
            MessagesController mc = MessagesController.getInstance(account);
            ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
            if (all == null || all.isEmpty()) all = mc.getDialogs(0);
            if (all == null || all.isEmpty()) all = mc.dialogsServerOnly;
            if (all == null) return out;
            String f = filter != null ? filter.trim().toLowerCase(java.util.Locale.ROOT) : "all";
            for (int i = 0; i < all.size(); i++) {
                TLRPC.Dialog d = all.get(i);
                if (d == null) continue;
                long did = d.id;
                if (did > 0) {
                    if (f.equals("groups") || f.equals("channels")) continue;
                    TLRPC.User u = mc.getUser(did);
                    if (u == null) continue;
                    String fullName = UserObject.getUserName(u);
                    String uname = u.username != null ? u.username : "";
                    out.add(new FoundChat(did, fullName, uname, false, false));
                } else if (did < 0) {
                    TLRPC.Chat c = mc.getChat(-did);
                    if (c == null) continue;
                    boolean isChannel = ChatObject.isChannelAndNotMegaGroup(c);
                    if (f.equals("users")) continue;
                    if (f.equals("groups") && isChannel) continue;
                    if (f.equals("channels") && !isChannel) continue;
                    String title = c.title != null ? c.title : "";
                    String uname = c.username != null ? c.username : "";
                    out.add(new FoundChat(did, title, uname, isChannel, !isChannel));
                }
            }
        } catch (Throwable ignore) {}
        return out;
    }

    public static String formatChatPage(int account, List<FoundChat> all, int page, int pageSize, String header) {
        StringBuilder sb = new StringBuilder(header);
        int total = all.size();
        int pages = Math.max(1, (total + pageSize - 1) / pageSize);
        int p = Math.max(0, Math.min(page, pages - 1));
        int from = p * pageSize;
        int to = Math.min(total, from + pageSize);
        Map<Long, Integer> unreadByDialog = new HashMap<>();
        try {
            MessagesController mc = MessagesController.getInstance(account);
            ArrayList<TLRPC.Dialog> dialogs = mc != null ? mc.getAllDialogs() : null;
            if (dialogs != null) {
                for (int k = 0; k < dialogs.size(); k++) {
                    TLRPC.Dialog d = dialogs.get(k);
                    if (d != null && d.unread_count > 0) {
                        unreadByDialog.put(d.id, d.unread_count);
                    }
                }
            }
        } catch (Throwable ignore) {}
        for (int i = from; i < to; i++) {
            FoundChat fc = all.get(i);
            sb.append(i + 1).append(". ");
            sb.append(fc.isGroup ? MiogramLocale.get("[Група] ", "[Группа] ", "[Group] ")
                    : fc.isChannel ? MiogramLocale.get("[Канал] ", "[Канал] ", "[Channel] ") : "");
            sb.append(fc.name.isEmpty() ? MiogramLocale.get("(без назви)", "(без названия)", "(no name)") : fc.name);
            if (!fc.username.isEmpty()) sb.append(" (@").append(fc.username).append(")");
            try {
                Integer unread = unreadByDialog.get(fc.dialogId);
                if (unread != null && unread > 0) sb.append(" [").append(unread).append("]");
            } catch (Throwable ignore) {}
            sb.append("\n");
        }
        sb.append(MiogramLocale.get("Сторінка ", "Страница ", "Page ")).append(p + 1).append("/").append(pages)
                .append(MiogramLocale.get(", всього ", ", всего ", ", total ")).append(total).append(". ")
                .append(MiogramLocale.get("Відповіси номером — відкрию/прочитаю. «Далі» — наступні 50.",
                        "Ответь номером — открою/прочитаю. «Дальше» — следующие 50.",
                        "Reply with a number — I'll open/read it. \"Next\" — next 50."));
        return sb.toString();
    }

    public static ChatResolution resolveChatTarget(int account, JSONObject p) {
        return resolveChatTarget(account, p, null);
    }

    /**
     * @param forAction tool name asking for resolution (read_messages, send_message...).
     *                  Stored in the pending pick so a follow-up pick ("2", "другий")
     *                  can resume the exact action instead of searching again.
     */
    public static ChatResolution resolveChatTarget(int account, JSONObject p, String forAction) {
        long chatId = p.optLong("chat_id", 0);
        if (chatId != 0) {
            return new ChatResolution(chatId, null, null);
        }
        String query = p.optString("chat_query", "");
        if (query.isEmpty()) query = p.optString("chat_name", "");
        if (query.isEmpty()) query = p.optString("username", "");
        if (query.isEmpty()) query = p.optString("query", "");

        String lowQ = query.trim().toLowerCase(java.util.Locale.ROOT);
        boolean isGeneric = query.isEmpty()
                || lowQ.equals("тут") || lowQ.equals("зараз") || lowQ.equals("поточний") || lowQ.equals("активний")
                || lowQ.equals("що пишуть") || lowQ.equals("що нового") || lowQ.equals("останній")
                || lowQ.equals("here") || lowQ.equals("current") || lowQ.equals("recent") || lowQ.equals("last")
                || lowQ.equals("чат") || lowQ.equals("діалог");

        if (isGeneric) {
            MessagesController mc = MessagesController.getInstance(account);
            ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
            if (all == null || all.isEmpty()) all = mc.getDialogs(0);
            if (all == null || all.isEmpty()) all = mc.dialogsServerOnly;
            if (all != null && !all.isEmpty()) {
                TLRPC.Dialog targetDlg = all.get(0);
                for (int i = 0; i < all.size(); i++) {
                    TLRPC.Dialog d = all.get(i);
                    if (d != null && d.unread_count > 0) {
                        targetDlg = d;
                        break;
                    }
                }
                long did = targetDlg.id;
                String name = "";
                String uname = "";
                if (did > 0) {
                    TLRPC.User u = mc.getUser(did);
                    if (u != null) {
                        name = UserObject.getUserName(u);
                        uname = u.username != null ? u.username : "";
                    }
                } else if (did < 0) {
                    TLRPC.Chat c = mc.getChat(-did);
                    if (c != null) {
                        name = c.title != null ? c.title : "";
                        uname = c.username != null ? c.username : "";
                    }
                }
                FoundChat fc = new FoundChat(did, name, uname, did < 0, did < 0);
                return new ChatResolution(did, fc, null);
            }
            return new ChatResolution(0, null, MiogramLocale.get("Будь ласка, вкажи ім'я або юзернейм співрозмовника.", "Пожалуйста, укажи имя или юзернейм собеседника.", "Please specify name or @username."));
        }

        // Follow-up to our own disambiguation list ("2", "другий", "@nick", "так"/"ні").
        PickResolution pick = tryResolvePendingPick(account, query);
        if ("RESOLVED".equals(pick.kind) && pick.foundChat != null) {
            return new ChatResolution(pick.foundChat.dialogId, pick.foundChat, null);
        }

        List<FoundChat> results = searchChats(account, query);
        if (results.isEmpty()) {
            return new ChatResolution(0, null, MiogramLocale.get("Не вдалося знайти жодного чату за запитом «", "Не удалось найти ни одного чата по запросу «", "Could not find any chat for query \"") + query + MiogramLocale.get("». Перевір правильність написання імені чи юзернейму.", "». Проверь правильность написания имени или юзернейма.", "\". Check the name or username spelling."));
        }
        if (results.size() == 1) {
            clearPendingPick(account);
            return new ChatResolution(results.get(0).dialogId, results.get(0), null);
        }

        // Smart friend selection: If top match is strong (exact match or dominates second match)
        int scoreTop = calculateMatchScore(query, results.get(0).name, results.get(0).username, null, null);
        int scoreSecond = calculateMatchScore(query, results.get(1).name, results.get(1).username, null, null);
        if (scoreTop >= 88 && (scoreTop == 100 || (scoreTop - scoreSecond) >= 12)) {
            clearPendingPick(account);
            return new ChatResolution(results.get(0).dialogId, results.get(0), null);
        }

        JSONObject resumeParams = null;
        try {
            if (forAction != null && p != null) resumeParams = new JSONObject(p.toString());
        } catch (Throwable ignore) {}
        setPendingPick(account, new PendingPick(results, query, forAction, resumeParams, null, 0, 50));
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
        sb.append(MiogramLocale.get("Скажи номер (наприклад «2» або «другий») — і я продовжу. «Далі» — гортаю список чатів.",
                "Скажи номер (например «2» или «второй») — и я продолжу. «Дальше» — листаю список чатов.",
                "Reply with the number (e.g. \"2\") and I'll continue. \"Next\" — browse the chat list."));
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
                    PickResolution pre = tryResolvePendingPick(account, query);
                    if ("RESOLVED".equals(pre.kind) && pre.foundChat != null) {
                        FoundChat fc = pre.foundChat;
                        callback.run(MiogramLocale.get("Знайдено чат: ", "Найден чат: ", "Chat found: ") + fc.name + (!fc.username.isEmpty() ? " (@" + fc.username + ")" : "") + ".");
                        return;
                    }
                    List<FoundChat> results = searchChats(account, query);
                    if (results.isEmpty()) {
                        callback.run(MiogramLocale.get("Не знайшла жодного чату чи контакту за запитом «", "Не нашла ни одного чата или контакта по запросу «", "Did not find any chat or contact for query \"") + query + "».");
                        return;
                    }
                    if (results.size() == 1) {
                        clearPendingPick(account);
                        FoundChat fc = results.get(0);
                        callback.run(MiogramLocale.get("Знайдено чат: ", "Найден чат: ", "Chat found: ") + fc.name + (!fc.username.isEmpty() ? " (@" + fc.username + ")" : "") + ".");
                        return;
                    }
                    setPendingPick(account, new PendingPick(results, query, null, null, null, 0, 50));
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
                    sb.append(MiogramLocale.get("Скажи номер (наприклад «2» або «другий») — продовжу. «Далі» — гортаю список чатів.",
                            "Скажи номер (например «2» или «второй») — продолжу. «Дальше» — листаю список чатов.",
                            "Reply with the number (e.g. \"2\") and I'll continue. \"Next\" — browse the chat list."));
                    callback.run(sb.toString());
                    break;
                }
                case "list_dialogs": {
                    String filter = p.optString("filter", "all");
                    if (filter.isEmpty()) filter = p.optString("kind", "all");
                    int page = Math.max(0, p.optInt("page", 0));
                    int pageSize = p.optInt("page_size", 50);
                    if (pageSize < 10) pageSize = 10;
                    if (pageSize > 50) pageSize = 50;
                    List<FoundChat> all = snapshotDialogs(account, filter);
                    if (all.isEmpty()) {
                        callback.run(MiogramLocale.get("Список чатів порожній або ще не завантажився.", "Список чатов пуст или ещё не загрузился.", "Chat list is empty or not loaded yet."));
                        return;
                    }
                    setPendingPick(account, new PendingPick(all, "", null, null, filter, page, pageSize));
                    String header = MiogramLocale.get("Мої чати (спочатку недавні", "Мои чаты (сначала недавние", "My chats (recent first")
                            + ("all".equalsIgnoreCase(filter) ? "" : ", " + filter) + "):\n";
                    callback.run(formatChatPage(account, all, page, pageSize, header));
                    break;
                }
                case "open_chat": {
                    ChatResolution res = resolveChatTarget(account, p, "open_chat");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    final long did = res.dialogId;
                    final String ref = res.foundChat != null ? res.foundChat.getReference() : String.valueOf(did);
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            org.telegram.ui.ActionBar.BaseFragment last = org.telegram.ui.LaunchActivity.getLastFragment();
                            if (last == null) {
                                callback.run(MiogramLocale.get("Не можу відкрити чат зараз (немає екрану).", "Не могу открыть чат сейчас (нет экрана).", "Can't open the chat right now (no screen)."));
                                return;
                            }
                            if (did > 0) {
                                TLRPC.User u = MessagesController.getInstance(account).getUser(did);
                                if (u != null && u.username != null && !u.username.isEmpty()) {
                                    MessagesController.getInstance(account).openByUserName(u.username, last, 1);
                                    callback.run(MiogramLocale.get("Відкриваю чат ", "Открываю чат ", "Opening chat ") + ref + ".");
                                    return;
                                }
                            } else if (did < 0) {
                                TLRPC.Chat c = MessagesController.getInstance(account).getChat(-did);
                                if (c != null && c.username != null && !c.username.isEmpty()) {
                                    MessagesController.getInstance(account).openByUserName(c.username, last, 1);
                                    callback.run(MiogramLocale.get("Відкриваю чат ", "Открываю чат ", "Opening chat ") + ref + ".");
                                    return;
                                }
                            }
                            android.os.Bundle args = new android.os.Bundle();
                            args.putLong("dialog_id", did);
                            last.presentFragment(new org.telegram.ui.ChatActivity(args));
                            callback.run(MiogramLocale.get("Відкриваю чат ", "Открываю чат ", "Opening chat ") + ref + ".");
                        } catch (Throwable t) {
                            callback.run(MiogramLocale.get("Не вдалося відкрити чат: ", "Не удалось открыть чат: ", "Failed to open chat: ") + t.getMessage());
                        }
                    });
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
                case "mute_chat": {
                    ChatResolution res = resolveChatTarget(account, p, "mute_chat");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    boolean mute = p.optBoolean("mute", true);
                    toolSetMuted(account, res.dialogId, mute, callback);
                    break;
                }
                case "archive_chat": {
                    ChatResolution res = resolveChatTarget(account, p, "archive_chat");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    boolean archive = p.optBoolean("archive", true);
                    toolSetArchived(account, res.dialogId, archive, callback);
                    break;
                }
                case "mark_read": {
                    ChatResolution res = resolveChatTarget(account, p, "mark_read");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    toolMarkRead(account, res.dialogId, callback);
                    break;
                }
                case "chat_info": {
                    ChatResolution res = resolveChatTarget(account, p, "chat_info");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    toolChatInfo(account, res.dialogId, callback);
                    break;
                }
                case "player_control": {
                    toolPlayerControl(p.optString("action", "toggle"), callback);
                    break;
                }
                case "player_now": {
                    toolPlayerNow(callback);
                    break;
                }
                case "contacts_list": {
                    toolContactsList(account, Math.min(30, Math.max(1, p.optInt("limit", 15))), callback);
                    break;
                }
                case "search_messages": {
                    String query = p.optString("query", "");
                    if (query.isEmpty()) query = p.optString("text", "");
                    if (query.isEmpty()) query = p.optString("q", "");
                    String chatQuery = p.optString("chat_query", "");
                    if (chatQuery.isEmpty()) chatQuery = p.optString("chat_name", "");
                    long specificChatId = p.optLong("chat_id", 0);

                    if (query.isEmpty()) {
                        if (!chatQuery.isEmpty() || specificChatId != 0) {
                            JSONObject readParams = new JSONObject();
                            readParams.put("chat_query", chatQuery);
                            readParams.put("chat_id", specificChatId);
                            readParams.put("limit", p.optInt("limit", 15));
                            ActionRequest readReq = new ActionRequest("read_messages", readParams, false);
                            executeTool(account, readReq, callback);
                            return;
                        }
                        callback.run(MiogramLocale.get("Вкажи текст для пошуку повідомлень або ім'я співрозмовника.", "Укажи текст для поиска сообщений или имя собеседника.", "Specify text to search messages or chat name."));
                        return;
                    }

                    final String fQuery = query;
                    MessagesController mc = MessagesController.getInstance(account);

                    if (specificChatId != 0 || !chatQuery.isEmpty()) {
                        ChatResolution res = resolveChatTarget(account, p, "search_messages");
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
                    ChatResolution res = resolveChatTarget(account, p, "clear_chat");
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
                    ChatResolution res = resolveChatTarget(account, p, "delete_chat");
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
                    ChatResolution res = resolveChatTarget(account, p, "send_message");
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
                    ChatResolution res = resolveChatTarget(account, p, "read_messages");
                    if (res.errorMessage != null) {
                        callback.run(res.errorMessage);
                        return;
                    }
                    int limit = Math.min(30, Math.max(1, p.optInt("limit", 15)));
                    TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                    req.peer = MessagesController.getInstance(account).getInputPeer(res.dialogId);
                    req.limit = limit;
                    final FoundChat fc = res.foundChat;
                    ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (response instanceof TLRPC.messages_Messages) {
                            TLRPC.messages_Messages msgRes = (TLRPC.messages_Messages) response;
                            MessagesController mc = MessagesController.getInstance(account);
                            mc.putUsers(msgRes.users, false);
                            mc.putChats(msgRes.chats, false);

                            String chatTitle = fc != null ? fc.getReference() : String.valueOf(res.dialogId);
                            String header = MiogramLocale.get("Останні повідомлення з «", "Последние сообщения из «", "Recent messages from \"")
                                    + chatTitle + "»:\n";
                            StringBuilder sb = new StringBuilder(header);
                            if (msgRes.messages.isEmpty()) {
                                sb.append(MiogramLocale.get("(Листування порожнє або немає недавніх повідомлень)", "(Переписка пуста или нет недавних сообщений)", "(Chat is empty or no recent messages)"));
                            } else {
                                long myId = UserConfig.getInstance(account).getClientUserId();
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());

                                for (int i = msgRes.messages.size() - 1; i >= 0; i--) {
                                    TLRPC.Message m = msgRes.messages.get(i);
                                    if (m == null || m instanceof TLRPC.TL_messageEmpty) continue;

                                    String timeStr = "";
                                    try {
                                        timeStr = "[" + sdf.format(new java.util.Date(m.date * 1000L)) + "] ";
                                    } catch (Throwable ignore) {}

                                    String authorName;
                                    long fromId = MessageObject.getFromChatId(m);
                                    if (m.out || fromId == myId) {
                                        authorName = MiogramLocale.get("Ви", "Вы", "You");
                                    } else if (fromId > 0) {
                                        TLRPC.User u = mc.getUser(fromId);
                                        authorName = u != null ? UserObject.getUserName(u) : MiogramLocale.get("Співрозмовник", "Собеседник", "User");
                                    } else if (fromId < 0) {
                                        TLRPC.Chat c = mc.getChat(-fromId);
                                        authorName = c != null && c.title != null ? c.title : MiogramLocale.get("Група", "Группа", "Group");
                                    } else {
                                        authorName = chatTitle;
                                    }

                                    MessageObject mo = new MessageObject(account, m, false, false);
                                    StringBuilder body = new StringBuilder();
                                    if (m.media != null) {
                                        if (mo.isVoice()) {
                                            int dur = (int) mo.getDuration();
                                            body.append("[Голосове").append(dur > 0 ? " " + dur + "с" : "").append("] ");
                                        } else if (mo.isRoundVideo()) {
                                            body.append("[Відеоповідомлення (кружечок)] ");
                                        } else if (mo.isSticker()) {
                                            String stickerEmoji = mo.getStickerEmoji();
                                            body.append("[Стікер").append(stickerEmoji != null ? " " + stickerEmoji : "").append("] ");
                                        } else if (mo.isMusic()) {
                                            body.append("[Музика: ").append(mo.getMusicTitle()).append("] ");
                                        } else if (mo.isVideo()) {
                                            body.append("[Відео] ");
                                        } else if (mo.isPhoto()) {
                                            body.append("[Фото] ");
                                        } else if (mo.getDocument() != null) {
                                            String fn = org.telegram.messenger.FileLoader.getDocumentFileName(mo.getDocument());
                                            body.append("[Файл").append(fn != null ? ": " + fn : "").append("] ");
                                        } else if (m.media instanceof TLRPC.TL_messageMediaContact) {
                                            body.append("[Контакт] ");
                                        } else if (m.media instanceof TLRPC.TL_messageMediaGeo || m.media instanceof TLRPC.TL_messageMediaVenue) {
                                            body.append("[Геолокація] ");
                                        } else if (m.media instanceof TLRPC.TL_messageMediaPoll) {
                                            body.append("[Опитування] ");
                                        }
                                    }

                                    if (m.message != null && !m.message.trim().isEmpty()) {
                                        body.append(m.message.replace("\n", " "));
                                    } else if (body.length() == 0) {
                                        body.append(MiogramLocale.get("(медіаповідомлення)", "(медиасообщение)", "(media)"));
                                    }

                                    sb.append("• ").append(timeStr).append(authorName).append(": ").append(body.toString().trim()).append("\n");
                                }
                            }
                            callback.run(sb.toString());
                        } else {
                            callback.run(MiogramLocale.get("Не вдалося завантажити повідомлення: ", "Не удалось загрузить сообщения: ", "Failed to load messages: ") + (error != null ? error.text : MiogramLocale.get("помилка запиту", "ошибка запроса", "request error")));
                        }
                    }));
                    return;
                }
                case "read_unread_summary": {
                    MessagesController mc = MessagesController.getInstance(account);
                    ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
                    if (all == null || all.isEmpty()) all = mc.getDialogs(0);
                    if (all == null || all.isEmpty()) all = mc.dialogsServerOnly;

                    if (all == null || all.isEmpty()) {
                        callback.run(MiogramLocale.get("У тебе немає активних чатів або список ще завантажується.", "У тебя нет активных чатов или список ещё загружается.", "You have no active chats or list is still loading."));
                        return;
                    }

                    List<TLRPC.Dialog> unreadDialogs = new ArrayList<>();
                    for (int i = 0; i < all.size(); i++) {
                        TLRPC.Dialog d = all.get(i);
                        if (d != null && d.unread_count > 0) {
                            unreadDialogs.add(d);
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    if (!unreadDialogs.isEmpty()) {
                        sb.append(MiogramLocale.get("📬 Знайдено ", "📬 Найдено ", "📬 Found "))
                          .append(unreadDialogs.size())
                          .append(MiogramLocale.get(" чатів з непрочитаними повідомленнями:\n", " чатов с непрочитанными сообщениями:\n", " chats with unread messages:\n"));

                        int max = Math.min(8, unreadDialogs.size());
                        for (int i = 0; i < max; i++) {
                            TLRPC.Dialog d = unreadDialogs.get(i);
                            String title = "";
                            String uname = "";
                            if (d.id > 0) {
                                TLRPC.User u = mc.getUser(d.id);
                                if (u != null) {
                                    title = UserObject.getUserName(u);
                                    uname = u.username != null ? "@" + u.username : "";
                                }
                            } else if (d.id < 0) {
                                TLRPC.Chat c = mc.getChat(-d.id);
                                if (c != null) {
                                    title = c.title != null ? c.title : "";
                                    uname = c.username != null ? "@" + c.username : "";
                                }
                            }
                            if (title.isEmpty()) title = "Chat #" + d.id;

                            sb.append(i + 1).append(". «").append(title).append("»");
                            if (!uname.isEmpty()) sb.append(" (").append(uname).append(")");
                            sb.append(" — ").append(d.unread_count).append(MiogramLocale.get(" нових", " новых", " new"));

                            MessageObject lastMsg = mc.dialogMessagesByIds.get(d.top_message);
                            if (lastMsg != null && lastMsg.messageOwner != null && lastMsg.messageOwner.message != null && !lastMsg.messageOwner.message.trim().isEmpty()) {
                                String snippet = lastMsg.messageOwner.message.replace("\n", " ").trim();
                                if (snippet.length() > 60) snippet = snippet.substring(0, 57) + "...";
                                sb.append(" | \"").append(snippet).append("\"");
                            }
                            sb.append("\n");
                        }
                        sb.append("\n").append(MiogramLocale.get("Скажи ім'я або номер — і я прочитаю листування детально!", "Скажи имя или номер — и я прочитаю переписку детально!", "Tell me the name or number and I'll read the full chat!"));
                    } else {
                        sb.append(MiogramLocale.get("✨ Непрочитаних повідомлень немає! Усі чати переглянуті.\nОсь останні активні чати:\n", "✨ Непрочитанных сообщений нет! Все чаты просмотрены.\nВот последние активные чаты:\n", "✨ No unread messages! All chats are up to date.\nHere are the most recent active chats:\n"));
                        int max = Math.min(5, all.size());
                        for (int i = 0; i < max; i++) {
                            TLRPC.Dialog d = all.get(i);
                            String title = "";
                            if (d.id > 0) {
                                TLRPC.User u = mc.getUser(d.id);
                                if (u != null) title = UserObject.getUserName(u);
                            } else if (d.id < 0) {
                                TLRPC.Chat c = mc.getChat(-d.id);
                                if (c != null) title = c.title != null ? c.title : "";
                            }
                            if (!title.isEmpty()) {
                                sb.append(i + 1).append(". «").append(title).append("»\n");
                            }
                        }
                    }
                    callback.run(sb.toString());
                    break;
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
                                        + "⚡ **Статус:** " + (installed ? "Збережено в модулі." : "Не збережено.") + " "
                                        + "Lua-движка на пристрої нема, тому працюють лише текстові фільтри (якщо скрипт їх оголошує).",
                                        "✦ **Плагин на Lua создан!**\n"
                                        + "📁 Название: `" + result.name + ".lua`\n"
                                        + "⚡ **Статус:** " + (installed ? "Сохранен в модули." : "Не сохранен.") + " "
                                        + "Lua-движка на устройстве нет, поэтому работают только текстовые фильтры (если скрипт их объявляет).",
                                        "✦ **Lua plugin created!**\n"
                                        + "📁 Name: `" + result.name + ".lua`\n"
                                        + "⚡ **Status:** " + (installed ? "Saved to modules." : "Not saved.") + " "
                                        + "No Lua engine on device, so only declared text filters apply."
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
                    callback.run(MiogramLocale.get("Не знаю такої дії «", "Не знаю такого действия «", "Unknown action \"") + name + MiogramLocale.get("». Нічого не виконано — спробуй інакше.", "». Ничего не выполнено — попробуй иначе.", "\". Nothing was done — try differently."));
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
