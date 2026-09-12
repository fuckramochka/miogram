package app.miogram.bridge.ai.tools;

import org.telegram.messenger.Utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import app.miogram.bridge.hooks.MioHook;

/**
 * MioTool — the single tool runtime for the AI companion ("the monster's
 * hands", not a pile of scripts).
 *
 * <ul>
 *   <li>Tools register here with a name, prompt docs and a handler.</li>
 *   <li>Every call goes through the vetoable {@code AI_TOOL_CALL} MioHook
 *       point and is mirrored to the console bus.</li>
 *   <li>Unknown names fall back to the legacy companion toolbox, so old
 *       prompt paths keep working.</li>
 *   <li>{@link #execChain} runs multi-step chains, pausing before sensitive
 *       tools instead of auto-executing them.</li>
 * </ul>
 *
 * <p>Thread contract: handlers run on the caller's thread and may answer on
 * any thread. Console listeners must be thread-safe (the chat UI re-posts).
 */
public final class MioTool {

    private MioTool() {
    }

    public interface Handler {
        void exec(int account, org.json.JSONObject params, Utilities.Callback<String> cb);
    }

    public static final class Def {
        public final String name;
        public final String title;
        public final String promptDoc;
        public final boolean sensitive;
        public final Handler handler;

        public Def(String name, String title, String promptDoc, boolean sensitive, Handler handler) {
            this.name = name != null ? name.toLowerCase(Locale.US) : "?";
            this.title = title != null ? title : this.name;
            this.promptDoc = promptDoc != null ? promptDoc : "";
            this.sensitive = sensitive;
            this.handler = handler;
        }
    }

    private static final Map<String, Def> REGISTRY = new ConcurrentHashMap<>();

    public static void register(Def def) {
        if (def == null || def.handler == null) return;
        REGISTRY.put(def.name, def);
    }

    public static Def get(String name) {
        if (name == null) return null;
        return REGISTRY.get(name.toLowerCase(Locale.US));
    }

    public static List<Def> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    // ==================================================================
    // Console bus (the monster's console)
    // ==================================================================

    public static final class ConsoleLine {
        public final long time;
        public final String tool;
        public final String phase; // started | ok | error | denied | paused
        public final String preview;

        public ConsoleLine(String tool, String phase, String preview) {
            this.time = System.currentTimeMillis();
            this.tool = tool != null ? tool : "?";
            this.phase = phase != null ? phase : "?";
            this.preview = preview != null ? preview : "";
        }

        public String stamp() {
            try {
                return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(time));
            } catch (Throwable ignore) {
                return "";
            }
        }
    }

    public interface ConsoleListener {
        void onConsoleLine(ConsoleLine line);
    }

    private static final CopyOnWriteArrayList<ConsoleLine> RING = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ConsoleListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final int RING_CAP = 200;

    public static void addConsoleListener(ConsoleListener l) {
        if (l != null && !LISTENERS.contains(l)) LISTENERS.add(l);
    }

    public static void removeConsoleListener(ConsoleListener l) {
        LISTENERS.remove(l);
    }

    public static List<ConsoleLine> snapshot() {
        return new ArrayList<>(RING);
    }

    public static void clearConsole() {
        RING.clear();
    }

    private static void emit(String tool, String phase, String preview) {
        ConsoleLine line = new ConsoleLine(tool, phase, preview);
        RING.add(line);
        while (RING.size() > RING_CAP) {
            RING.remove(0);
        }
        for (ConsoleListener l : LISTENERS) {
            try {
                l.onConsoleLine(line);
            } catch (Throwable ignore) {}
        }
    }

    static String preview(String s) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ').trim().replaceAll("\\s+", " ");
        if (oneLine.length() > 140) return oneLine.substring(0, 140) + "…";
        return oneLine;
    }

    // ==================================================================
    // Execution
    // ==================================================================

    /**
     * Runs one tool by name with hook veto + console mirroring.
     * Unknown names fall back to the legacy toolbox.
     */
    public static void exec(int account, String name, org.json.JSONObject params, Utilities.Callback<String> cb) {
        final String toolName = name != null ? name.toLowerCase(Locale.US) : "?";
        final String paramsStr = params != null ? params.toString() : "{}";
        emit(toolName, "started", preview(paramsStr));
        boolean allowed = false;
        try {
            allowed = MioHook.dispatchAiToolCall(toolName, paramsStr, "started", null);
        } catch (Throwable ignore) {
            allowed = true;
        }
        if (!allowed) {
            emit(toolName, "denied", "blocked by hook");
            try {
                cb.run("Tool call denied by a MioHook plugin.");
            } catch (Throwable ignore) {}
            return;
        }
        Def def = REGISTRY.get(toolName);
        Utilities.Callback<String> wrapped = result -> {
            emit(toolName, "ok", preview(result));
            try {
                MioHook.dispatchAiToolCall(toolName, paramsStr, "finished", preview(result));
            } catch (Throwable ignore) {}
            try {
                cb.run(result);
            } catch (Throwable ignore) {}
        };
        try {
            if (def != null && def.handler != null) {
                def.handler.exec(account, params != null ? params : new org.json.JSONObject(), wrapped);
            } else {
                app.miogram.bridge.ai.companion.MiogramCompanionToolbox.executeTool(account,
                        new app.miogram.bridge.ai.companion.MiogramCompanionToolbox.ActionRequest(
                                toolName, params != null ? params : new org.json.JSONObject(), false),
                        wrapped);
            }
        } catch (Throwable t) {
            emit(toolName, "error", preview(t.getMessage()));
            try {
                cb.run("Tool crashed: " + t.getMessage());
            } catch (Throwable ignore) {}
        }
    }

    public static boolean isSensitive(String name, org.json.JSONObject params) {
        Def def = get(name);
        if (def != null) return def.sensitive;
        return app.miogram.bridge.ai.companion.MiogramCompanionToolbox.isSensitiveTool(name);
    }

    // ==================================================================
    // Chains (multi-step agent runs)
    // ==================================================================

    public static final class ChainStep {
        public final String name;
        public final org.json.JSONObject params;

        public ChainStep(String name, org.json.JSONObject params) {
            this.name = name;
            this.params = params;
        }
    }

    public static final class ChainResult {
        /** Human-readable transcript of executed steps. */
        public final String transcript;
        /** First sensitive step hit (not executed) — needs a permission card. */
        public final ChainStep pendingSensitive;
        /** True if every step ran. */
        public final boolean complete;

        public ChainResult(String transcript, ChainStep pendingSensitive, boolean complete) {
            this.transcript = transcript != null ? transcript : "";
            this.pendingSensitive = pendingSensitive;
            this.complete = complete;
        }
    }

    /**
     * Runs steps sequentially, pausing BEFORE the first sensitive one.
     * Never auto-executes sensitive tools.
     */
    public static void execChain(int account, List<ChainStep> steps, Utilities.Callback<ChainResult> done) {
        if (steps == null || steps.isEmpty()) {
            try {
                done.run(new ChainResult("", null, true));
            } catch (Throwable ignore) {}
            return;
        }
        StringBuilder transcript = new StringBuilder();
        execChainAt(account, steps, 0, transcript, done);
    }

    private static void execChainAt(int account, List<ChainStep> steps, int index,
                                    StringBuilder transcript, Utilities.Callback<ChainResult> done) {
        if (index >= steps.size()) {
            try {
                done.run(new ChainResult(transcript.toString(), null, true));
            } catch (Throwable ignore) {}
            return;
        }
        ChainStep step = steps.get(index);
        if (step == null || step.name == null) {
            execChainAt(account, steps, index + 1, transcript, done);
            return;
        }
        if (isSensitive(step.name, step.params)) {
            emit(step.name, "paused", "waiting for permission");
            try {
                done.run(new ChainResult(transcript.toString(), step, false));
            } catch (Throwable ignore) {}
            return;
        }
        exec(account, step.name, step.params, result -> {
            transcript.append("> ").append(step.name).append(": ").append(preview(result)).append("\n");
            execChainAt(account, steps, index + 1, transcript, done);
        });
    }
}
