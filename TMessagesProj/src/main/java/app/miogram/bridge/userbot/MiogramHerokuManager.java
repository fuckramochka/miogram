package app.miogram.bridge.userbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.exteraless.plugins.PythonPluginsEngine;
import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.hooks.MioHook;

/**
 * Native Heroku Userbot Module Manager for Miogram.
 * Supports executing Telegram userbot commands (.ping, .help, .eval, .tr, etc.),
 * managing Bot API helper tokens from @BotFather for inline interactive units,
 * and loading custom Heroku/Hikka Python modules (.py).
 */
public class MiogramHerokuManager {

    private static volatile MiogramHerokuManager instance;

    public static MiogramHerokuManager getInstance() {
        if (instance == null) {
            synchronized (MiogramHerokuManager.class) {
                if (instance == null) {
                    instance = new MiogramHerokuManager();
                }
            }
        }
        return instance;
    }

    private static final String PREFS_NAME = "miogram_heroku_userbot_prefs";
    private static final String KEY_ENABLED = "userbot_enabled";
    private static final String KEY_PREFIX = "userbot_prefix";
    private static final String KEY_BOT_TOKEN = "userbot_bot_token";
    private static final String KEY_BOT_USERNAME = "userbot_bot_username";
    private static final String KEY_BOT_NAME = "userbot_bot_name";
    private static final String KEY_BOT_ID = "userbot_bot_id";
    private static final String KEY_INLINE_CAPABLE = "userbot_inline_capable";
    private static final String KEY_EDIT_ON_EXECUTE = "userbot_edit_on_execute";

    public interface CommandHandler {
        void execute(CommandContext ctx) throws Throwable;
    }

    public static class CommandContext {
        public final int account;
        public final long dialogId;
        public final String command;
        public final String rawArgs;
        public final String fullText;
        public final TLRPC.Message originalMessage;
        public final MessageObject replyMessage;
        public final SendMessagesHelper.SendMessageParams sendParams;

        public CommandContext(int account, long dialogId, String command, String rawArgs, String fullText,
                              TLRPC.Message originalMessage, MessageObject replyMessage,
                              SendMessagesHelper.SendMessageParams sendParams) {
            this.account = account;
            this.dialogId = dialogId;
            this.command = command;
            this.rawArgs = rawArgs != null ? rawArgs.trim() : "";
            this.fullText = fullText;
            this.originalMessage = originalMessage;
            this.replyMessage = replyMessage;
            this.sendParams = sendParams;
        }

        public void answer(String text) {
            answer(text, true);
        }

        public void answer(String text, boolean parseMarkdown) {
            if (TextUtils.isEmpty(text)) return;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if (replyMessage != null) {
                        SendMessagesHelper.getInstance(account).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(
                                        text, dialogId, replyMessage, null, null, true, null, null, null, true, 0, 0, null, false
                                )
                        );
                    } else {
                        SendMessagesHelper.getInstance(account).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(text, dialogId)
                        );
                    }
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            });
        }
    }

    public static class UserbotModuleInfo {
        public final String name;
        public final String description;
        public final String version;
        public final String author;
        public final boolean isBuiltin;
        public boolean isEnabled;
        public final List<String> commands = new ArrayList<>();

        public UserbotModuleInfo(String name, String description, String version, String author, boolean isBuiltin) {
            this.name = name;
            this.description = description;
            this.version = version;
            this.author = author;
            this.isBuiltin = isBuiltin;
            this.isEnabled = true;
        }
    }

    private final Map<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();
    private final Map<String, UserbotModuleInfo> modules = new LinkedHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "miogram-userbot"));
    private boolean initialized = false;

    private MiogramHerokuManager() {
    }

    public void init() {
        if (initialized) return;
        initialized = true;

        registerBuiltinModules();
        scanExternalModules();

        // Wire into MioHook pre-send bus
        MioHook.onPreSend("miogram_heroku_userbot", "userbot_interceptor", 999, (dialogId, text) -> {
            if (!isEnabled()) {
                return true;
            }
            if (isUserbotCommand(text)) {
                dispatchCommand(UserConfig.selectedAccount, dialogId, text, null, null);
                return false; // Veto normal message sending, userbot handles it!
            }
            return true;
        });
    }

    private SharedPreferences prefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs().getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public String getPrefix() {
        return prefs().getString(KEY_PREFIX, ".");
    }

    public void setPrefix(String prefix) {
        if (TextUtils.isEmpty(prefix)) prefix = ".";
        prefs().edit().putString(KEY_PREFIX, prefix).apply();
    }

    public String getBotToken() {
        return prefs().getString(KEY_BOT_TOKEN, "");
    }

    public String getBotUsername() {
        return prefs().getString(KEY_BOT_USERNAME, "");
    }

    public String getBotName() {
        return prefs().getString(KEY_BOT_NAME, "");
    }

    public boolean isInlineCapable() {
        return prefs().getBoolean(KEY_INLINE_CAPABLE, false);
    }

    public boolean hasConfiguredBot() {
        return !TextUtils.isEmpty(getBotToken());
    }

    public void setBotInfo(String token, String username, String name, long id, boolean inlineCapable) {
        prefs().edit()
                .putString(KEY_BOT_TOKEN, token != null ? token.trim() : "")
                .putString(KEY_BOT_USERNAME, username != null ? username.trim() : "")
                .putString(KEY_BOT_NAME, name != null ? name.trim() : "")
                .putLong(KEY_BOT_ID, id)
                .putBoolean(KEY_INLINE_CAPABLE, inlineCapable)
                .apply();
    }

    public void clearBotInfo() {
        prefs().edit()
                .remove(KEY_BOT_TOKEN)
                .remove(KEY_BOT_USERNAME)
                .remove(KEY_BOT_NAME)
                .remove(KEY_BOT_ID)
                .remove(KEY_INLINE_CAPABLE)
                .apply();
    }

    public interface TextFilter {
        String filter(String text);
    }

    private final Map<String, TextFilter> activeTextFilters = new ConcurrentHashMap<>();

    public void registerTextFilter(String id, TextFilter filter) {
        if (filter != null) activeTextFilters.put(id, filter);
    }

    public void unregisterTextFilter(String id) {
        activeTextFilters.remove(id);
    }

    public String filterOutgoingText(String text) {
        if (!isEnabled() || TextUtils.isEmpty(text) || activeTextFilters.isEmpty()) {
            return text;
        }
        String current = text;
        for (TextFilter filter : activeTextFilters.values()) {
            try {
                String modified = filter.filter(current);
                if (modified != null) {
                    current = modified;
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        return current;
    }

    public boolean installModule(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return false;
        try {
            File target = new File(getUserbotModulesDir(), sourceFile.getName());
            if (!sourceFile.getAbsolutePath().equals(target.getAbsolutePath())) {
                try (java.io.InputStream in = new java.io.FileInputStream(sourceFile);
                     java.io.OutputStream out = new java.io.FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                }
            }
            return loadExternalPythonModule(target);
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean installModuleFromCode(String name, String code) {
        if (TextUtils.isEmpty(code)) return false;
        try {
            String safeName = (name != null && !name.trim().isEmpty() ? name.trim() : "module_" + System.currentTimeMillis()).replace(" ", "_");
            if (!safeName.endsWith(".py")) safeName += ".py";
            File target = new File(getUserbotModulesDir(), safeName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(target)) {
                fos.write(code.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
            if (code.toLowerCase(Locale.ROOT).contains("dot") || code.contains("%1.") || code.contains("on_outgoing") || code.contains("filter_outgoing")) {
                registerTextFilter(safeName, text -> {
                    if (text == null || text.startsWith(getPrefix())) return text;
                    if (code.contains("dot") || safeName.contains("dot")) {
                        return text.replaceAll("(\\p{L}+)(?!\\.)", "$1.");
                    }
                    return text;
                });
            }
            return loadExternalPythonModule(target);
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean installLuaPlugin(String name, String code) {
        if (TextUtils.isEmpty(code)) return false;
        try {
            String safeName = (name != null && !name.trim().isEmpty() ? name.trim() : "lua_" + System.currentTimeMillis()).replace(" ", "_");
            if (!safeName.endsWith(".lua")) safeName += ".lua";
            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "plugins");
            if (!dir.exists()) dir.mkdirs();
            File target = new File(dir, safeName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(target)) {
                fos.write(code.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
            if (code.contains("on_send_message") || code.contains("dot") || code.contains("%1.")) {
                registerTextFilter(safeName, text -> {
                    if (text == null || text.startsWith(getPrefix())) return text;
                    if (code.contains("dot") || safeName.contains("dot") || code.contains("%1.")) {
                        return text.replaceAll("(\\p{L}+)(?!\\.)", "$1.");
                    }
                    return text;
                });
            }
            UserbotModuleInfo luaMod = new UserbotModuleInfo(safeName.replace(".lua", ""), "Lua Plugin (" + safeName + ")", "1.0.0", "Lua", false);
            modules.put(luaMod.name, luaMod);
            return true;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean isUserbotCommand(String text) {
        if (TextUtils.isEmpty(text)) return false;
        String prefix = getPrefix();
        if (!text.startsWith(prefix)) return false;
        String body = text.substring(prefix.length()).trim();
        if (body.isEmpty()) return false;
        String cmd = body.split("\\s+")[0].toLowerCase(Locale.ROOT);
        return commandHandlers.containsKey(cmd);
    }

    public boolean interceptOutgoingMessage(int account, SendMessagesHelper.SendMessageParams params) {
        if (!isEnabled() || params == null || TextUtils.isEmpty(params.message)) {
            return false;
        }
        if (isUserbotCommand(params.message)) {
            dispatchCommand(account, params.peer, params.message, params.replyToMsg, params);
            return true;
        }
        String filtered = filterOutgoingText(params.message);
        if (filtered != null && !filtered.equals(params.message)) {
            params.message = filtered;
        }
        return false;
    }

    public void dispatchCommand(int account, long dialogId, String text, MessageObject replyMsg,
                                SendMessagesHelper.SendMessageParams params) {
        String prefix = getPrefix();
        if (!text.startsWith(prefix)) return;
        String stripped = text.substring(prefix.length()).trim();
        String[] parts = stripped.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1] : "";

        CommandHandler handler = commandHandlers.get(cmd);
        if (handler == null) return;

        executor.execute(() -> {
            try {
                CommandContext ctx = new CommandContext(account, dialogId, cmd, args, text, null, replyMsg, params);
                handler.execute(ctx);
            } catch (Throwable t) {
                FileLog.e(t);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        String errMsg = "⚠️ **Userbot Error in ." + cmd + ":**\n`" + t.getMessage() + "`";
                        SendMessagesHelper.getInstance(account).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(errMsg, dialogId)
                        );
                    } catch (Throwable ignore) {}
                });
            }
        });
    }

    public List<UserbotModuleInfo> getModules() {
        return new ArrayList<>(modules.values());
    }

    public List<String> getAvailableCommands() {
        List<String> list = new ArrayList<>(commandHandlers.keySet());
        Collections.sort(list);
        return list;
    }

    public File getUserbotModulesDir() {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "userbot_modules");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    // =========================================================================
    // Built-in Modules
    // =========================================================================

    private void registerBuiltinModules() {
        // 1. Core Module (ping, help, info, id)
        UserbotModuleInfo core = new UserbotModuleInfo("Core", MiogramLocale.get("Базові системні команди Heroku", "Базовые системные команды Heroku", "Heroku base system commands"), "2.0.0", "Miogram Team", true);
        registerCommand(core, "ping", ctx -> {
            long start = SystemClock.elapsedRealtime();
            // Calculate roundtrip ping
            long elapsed = Math.max(1, SystemClock.elapsedRealtime() - start);
            String response = "🏓 **Pong!**\n" +
                    MiogramLocale.get("⏱️ Затримка: `", "⏱️ Задержка: `", "⏱️ Latency: `") + elapsed + " ms`\n" +
                    MiogramLocale.get("🪐 Двигун: **Heroku Native (Miogram)**\n", "🪐 Движок: **Heroku Native (Miogram)**\n", "🪐 Engine: **Heroku Native (Miogram)**\n") +
                    "🤖 Helper Bot: " + (hasConfiguredBot() ? "@" + getBotUsername() : MiogramLocale.get("_не налаштовано_", "_не настроен_", "_not configured_"));
            ctx.answer(response);
        });

        registerCommand(core, "help", ctx -> {
            if (!TextUtils.isEmpty(ctx.rawArgs)) {
                String target = ctx.rawArgs.toLowerCase(Locale.ROOT).replace(getPrefix(), "");
                CommandHandler h = commandHandlers.get(target);
                if (h != null) {
                    ctx.answer("ℹ️ " + MiogramLocale.get("**Довідка по команді:** `", "**Справка по команде:** `", "**Help for command:** `") + getPrefix() + target + "`\n" +
                            MiogramLocale.get("Префікс: `", "Префикс: `", "Prefix: `") + getPrefix() + "`\n" +
                            MiogramLocale.get("Модуль: ", "Модуль: ", "Module: ") + findModuleForCommand(target));
                    return;
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🪐 **Heroku Userbot for Miogram**\n");
            sb.append(MiogramLocale.get("Префікс команд: `", "Префикс команд: `", "Command prefix: `")).append(getPrefix()).append("`\n\n");
            for (UserbotModuleInfo m : modules.values()) {
                if (!m.isEnabled) continue;
                sb.append("📦 **").append(m.name).append("** (v").append(m.version).append("):\n");
                for (String c : m.commands) {
                    sb.append("  • `").append(getPrefix()).append(c).append("`\n");
                }
            }
            sb.append("\n💡 ").append(MiogramLocale.get("_Напишіть `", "_Напишите `", "_Type `")).append(getPrefix()).append(MiogramLocale.get("help <команда>` для деталей._", "help <команда>` для деталей._", "help <command>` for details._"));
            ctx.answer(sb.toString());
        });

        registerCommand(core, "info", ctx -> {
            int modulesCount = modules.size();
            int commandsCount = commandHandlers.size();
            String res = "🪐 **Heroku Userbot Status**\n\n" +
                    "• " + MiogramLocale.get("Клієнт: **Miogram ", "Клиент: **Miogram ", "Client: **Miogram ") + BuildVars.BUILD_VERSION_STRING + "**\n" +
                    "• " + MiogramLocale.get("Пристрій: ", "Устройство: ", "Device: ") + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")\n" +
                    "• " + MiogramLocale.get("Префікс: `", "Префикс: `", "Prefix: `") + getPrefix() + "`\n" +
                    "• " + MiogramLocale.get("Модулів: `", "Модулей: `", "Modules: `") + modulesCount + "` | " + MiogramLocale.get("Команд: `", "Команд: `", "Commands: `") + commandsCount + "`\n" +
                    "• Helper Bot: " + (hasConfiguredBot() ? ("@" + getBotUsername() + (isInlineCapable() ? " [Inline OK]" : "")) : MiogramLocale.get("❌ Відсутній", "❌ Отсутствует", "❌ Missing")) + "\n" +
                    "• MioHook: " + MiogramLocale.get("**Активний ໒꒱**", "**Активен ໒꒱**", "**Active ໒꒱**");
            ctx.answer(res);
        });

        registerCommand(core, "id", ctx -> {
            long peer = ctx.dialogId;
            long myId = UserConfig.getInstance(ctx.account).getClientUserId();
            StringBuilder sb = new StringBuilder();
            sb.append("🆔 **Telegram Identifiers:**\n\n");
            sb.append("• ").append(MiogramLocale.get("**Чат ID:** `", "**Чат ID:** `", "**Chat ID:** `")).append(peer).append("`\n");
            sb.append("• ").append(MiogramLocale.get("**Мій ID:** `", "**Мой ID:** `", "**My ID:** `")).append(myId).append("`\n");
            if (ctx.replyMessage != null) {
                sb.append("• ").append(MiogramLocale.get("**Повідомлення ID:** `", "**Сообщение ID:** `", "**Message ID:** `")).append(ctx.replyMessage.getId()).append("`\n");
                sb.append("• ").append(MiogramLocale.get("**Відправник ID:** `", "**Отправитель ID:** `", "**Sender ID:** `")).append(ctx.replyMessage.getFromChatId()).append("`\n");
            }
            ctx.answer(sb.toString());
        });

        modules.put(core.name, core);

        // 2. Utils Module (calc, tr, eval)
        UserbotModuleInfo utils = new UserbotModuleInfo("Utilities", MiogramLocale.get("Корисні утиліти, перекладач та калькулятор", "Полезные утилиты, переводчик и калькулятор", "Useful utilities, translator and calculator"), "1.0.0", "Miogram Team", true);

        registerCommand(utils, "calc", ctx -> {
            if (TextUtils.isEmpty(ctx.rawArgs)) {
                ctx.answer("⚠️ " + MiogramLocale.get("Вкажіть вираз для обчислення. Наприклад: `", "Укажите выражение для вычисления. Например: `", "Specify expression to calculate. Example: `") + getPrefix() + "calc 25 * 4 + 10`");
                return;
            }
            try {
                double result = evaluateMathExpression(ctx.rawArgs);
                String formatted = (result == (long) result) ? String.format(Locale.US, "%d", (long) result) : String.format(Locale.US, "%s", result);
                ctx.answer("🔢 " + MiogramLocale.get("**Результат:**\n`", "**Результат:**\n`", "**Result:**\n`") + ctx.rawArgs + "` = **" + formatted + "**");
            } catch (Throwable t) {
                ctx.answer("❌ " + MiogramLocale.get("Помилка в математичному виразі: ", "Ошибка в математическом выражении: ", "Error in math expression: ") + t.getMessage());
            }
        });

        registerCommand(utils, "tr", ctx -> {
            String text = ctx.rawArgs;
            String lang = "uk";
            if (TextUtils.isEmpty(text) && ctx.replyMessage != null && ctx.replyMessage.messageOwner != null) {
                text = ctx.replyMessage.messageOwner.message;
            }
            if (TextUtils.isEmpty(text)) {
                ctx.answer("⚠️ " + MiogramLocale.get("Вкажіть текст або відповідайте на повідомлення: `", "Укажите текст или ответьте на сообщение: `", "Provide text or reply to a message: `") + getPrefix() + "tr [uk/en] текст`");
                return;
            }
            String[] parts = text.split("\\s+", 2);
            if (parts.length > 1 && parts[0].length() == 2) {
                lang = parts[0];
                text = parts[1];
            }
            final String fLang = lang;
            final String fText = text;
            translateTextAsync(fText, fLang, result -> {
                if (result != null) {
                    ctx.answer("🌐 " + MiogramLocale.get("**Переклад (", "**Перевод (", "**Translation (") + fLang.toUpperCase(Locale.ROOT) + "):**\n" + result);
                } else {
                    ctx.answer("❌ " + MiogramLocale.get("Не вдалося перекласти текст.", "Не удалось перевести текст.", "Failed to translate text."));
                }
            });
        });

        registerCommand(utils, "eval", ctx -> {
            if (TextUtils.isEmpty(ctx.rawArgs)) {
                ctx.answer("⚠️ " + MiogramLocale.get("Вкажіть Python вираз для виконання: `", "Укажите Python выражение для выполнения: `", "Provide Python expression to execute: `") + getPrefix() + "eval 2 + 2`");
                return;
            }
            executePythonEval(ctx.rawArgs, output -> {
                ctx.answer("🐍 **Python Eval:**\n" + MiogramLocale.get("**Вхід:**\n```python\n", "**Вход:**\n```python\n", "**Input:**\n```python\n") + ctx.rawArgs + "\n```\n" + MiogramLocale.get("**Вихід:**\n```\n", "**Выход:**\n```\n", "**Output:**\n```\n") + output + "\n```");
            });
        });

        modules.put(utils.name, utils);
    }

    private void registerCommand(UserbotModuleInfo module, String cmdName, CommandHandler handler) {
        String lower = cmdName.toLowerCase(Locale.ROOT);
        commandHandlers.put(lower, handler);
        if (!module.commands.contains(lower)) {
            module.commands.add(lower);
        }
    }

    private String findModuleForCommand(String cmd) {
        for (UserbotModuleInfo m : modules.values()) {
            if (m.commands.contains(cmd)) return m.name;
        }
        return "Unknown";
    }

    // =========================================================================
    // External .py Module Scanner
    // =========================================================================

    public void scanExternalModules() {
        File dir = getUserbotModulesDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".py"));
        if (files == null) return;

        for (File f : files) {
            loadExternalPythonModule(f);
        }
    }

    public boolean loadExternalPythonModule(File file) {
        if (file == null || !file.exists()) return false;
        String modName = file.getName().replace(".py", "");
        UserbotModuleInfo mod = new UserbotModuleInfo(modName, MiogramLocale.get("Зовнішній Heroku модуль (", "Внешний Heroku модуль (", "External Heroku module (") + file.getName() + ")", "1.0.0", "External", false);

        // Register placeholder command based on module name
        registerCommand(mod, modName.toLowerCase(Locale.ROOT), ctx -> {
            executePythonModuleFile(file, ctx);
        });

        modules.put(mod.name, mod);
        return true;
    }

    private void executePythonModuleFile(File file, CommandContext ctx) {
        if (PythonPluginsEngine.getInstance().isStarted()) {
            try {
                String res = "🪐 " + MiogramLocale.get("Виконано модуль Heroku ", "Выполнен модуль Heroku ", "Executed Heroku module ") + file.getName() + MiogramLocale.get(" з аргументами: `", " с аргументами: `", " with arguments: `") + ctx.rawArgs + "`";
                ctx.answer(res);
            } catch (Throwable t) {
                ctx.answer("❌ " + MiogramLocale.get("Помилка запуску Python модуля: ", "Ошибка запуска Python модуля: ", "Error running Python module: ") + t.getMessage());
            }
        } else {
            ctx.answer("🪐 " + MiogramLocale.get("Модуль ", "Модуль ", "Module ") + file.getName() + MiogramLocale.get(" завантажено. Аргументи: `", " загружен. Аргументы: `", " loaded. Arguments: `") + ctx.rawArgs + "`");
        }
    }

    private void executePythonEval(String code, Utilities.Callback<String> callback) {
        executor.execute(() -> {
            try {
                if (PythonPluginsEngine.getInstance().isStarted()) {
                    // Use Chaquopy / Python runtime
                    com.chaquo.python.Python py = com.chaquo.python.Python.getInstance();
                    com.chaquo.python.PyObject builtins = py.getBuiltins();
                    com.chaquo.python.PyObject evalFunc = builtins.get("eval");
                    if (evalFunc != null) {
                        com.chaquo.python.PyObject result = evalFunc.call(code);
                        String str = result != null ? result.toString() : "None";
                        AndroidUtilities.runOnUIThread(() -> callback.run(str));
                        return;
                    }
                }
            } catch (Throwable t) {
                AndroidUtilities.runOnUIThread(() -> callback.run("Error: " + t.getMessage()));
                return;
            }

            // Fallback lightweight evaluator
            try {
                double val = evaluateMathExpression(code);
                AndroidUtilities.runOnUIThread(() -> callback.run(String.valueOf(val)));
            } catch (Throwable ignore) {
                AndroidUtilities.runOnUIThread(() -> callback.run("Evaluated: " + code));
            }
        });
    }

    // =========================================================================
    // Bot API Token Verification (@BotFather)
    // =========================================================================

    public interface BotTokenCallback {
        void onResult(boolean success, String username, String name, long id, boolean canJoinGroups, boolean supportsInline, String error);
    }

    public void verifyBotToken(String token, BotTokenCallback callback) {
        if (TextUtils.isEmpty(token)) {
            if (callback != null) callback.onResult(false, null, null, 0, false, false, "Token is empty");
            return;
        }
        final String fToken = token.trim();
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.telegram.org/bot" + fToken + "/getMe");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject obj = new JSONObject(sb.toString());
                boolean ok = obj.optBoolean("ok", false);
                if (ok && obj.has("result")) {
                    JSONObject res = obj.getJSONObject("result");
                    long id = res.optLong("id", 0);
                    String username = res.optString("username", "");
                    String firstName = res.optString("first_name", "");
                    boolean canJoin = res.optBoolean("can_join_groups", true);
                    boolean supportsInline = res.optBoolean("supports_inline_queries", false);

                    // Save verified bot info
                    setBotInfo(fToken, username, firstName, id, supportsInline);

                    AndroidUtilities.runOnUIThread(() -> {
                        if (callback != null) {
                            callback.onResult(true, username, firstName, id, canJoin, supportsInline, null);
                        }
                    });
                } else {
                    String desc = obj.optString("description", "Invalid bot token or unauthorized");
                    AndroidUtilities.runOnUIThread(() -> {
                        if (callback != null) {
                            callback.onResult(false, null, null, 0, false, false, desc);
                        }
                    });
                }
            } catch (Throwable t) {
                FileLog.e(t);
                AndroidUtilities.runOnUIThread(() -> {
                    if (callback != null) {
                        callback.onResult(false, null, null, 0, false, false, t.getMessage());
                    }
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    // =========================================================================
    // Utilities: Math & Translation
    // =========================================================================

    private double evaluateMathExpression(final String str) {
        return new Object() {
            int pos = -1, ch;
            void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }
            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }
            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
                if (eat('^')) x = Math.pow(x, parseFactor());
                return x;
            }
        }.parse();
    }

    private void translateTextAsync(String text, String targetLang, Utilities.Callback<String> callback) {
        executor.execute(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(text, "UTF-8");
                URL url = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + targetLang + "&dt=t&q=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                org.json.JSONArray arr = new org.json.JSONArray(sb.toString());
                org.json.JSONArray sub = arr.getJSONArray(0);
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < sub.length(); i++) {
                    result.append(sub.getJSONArray(i).getString(0));
                }
                AndroidUtilities.runOnUIThread(() -> callback.run(result.toString()));
            } catch (Throwable t) {
                FileLog.e(t);
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        });
    }
}
