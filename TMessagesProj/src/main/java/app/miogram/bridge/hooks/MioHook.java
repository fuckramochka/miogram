package app.miogram.bridge.hooks;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MioHook — unified hook bus for the whole app.
 *
 * <p>Unlike the previous fire-and-forget registries, every dispatch here is:
 * <ul>
 *   <li><b>Ordered</b> — higher priority runs first, registration order breaks ties.</li>
 *   <li><b>Isolated</b> — a throwing hook never breaks the host or other hooks;
 *       3 consecutive failures quarantine (auto-disable) the hook with a log.</li>
 *   <li><b>Zero-cost when empty</b> — each point keeps a volatile array snapshot;
 *       dispatch is a single length check when nobody listens.</li>
 *   <li><b>Observable</b> — per-point dispatch counts, total time and failure
 *       counters via {@link #dumpStats()}.</li>
 *   <li><b>Ownable</b> — every registration carries an owner tag (plugin id);
 *       {@link #unregisterAll(Object)} detaches a whole plugin at once.</li>
 * </ul>
 *
 * <p>Thread contract: dispatch runs synchronously on the caller's thread.
 * Points marked {@code UI} must be dispatched from the main thread (callers
 * in this codebase already are — see the wired sites).
 */
public final class MioHook {

    private MioHook() {}

    // ==================================================================
    // Hook points
    // ==================================================================

    public enum Point {
        /** Vetoable. Fired before a message is sent. Return false to cancel. */
        MESSAGE_PRE_SEND,
        /** Fired for every incoming message batch head. */
        MESSAGE_RECEIVED,
        /** Vetoable per feed card. Return false to drop the item. */
        FEED_ITEM,
        /** Mutable result filter for AI text (summary/digest/rephrase). */
        AI_TEXT_RESULT,
        /** Fired when the audio track or play state changes. */
        AUDIO_TRACK_CHANGED,
        /** Fired after a divine preset is applied (UI thread). */
        PRESET_CHANGED,
        /** Fired on vault file uploaded / downloaded / deleted. */
        VAULT_FILE_EVENT,
        /** Vetoable. Return false to hide a dialog row (duress-style filters). */
        DIALOG_VISIBILITY,
        /**
         * Fired when a named UI container is created (tags: "main_tabs", ...).
         * Lets plugins inject views WITHOUT hacking fragmentView (the old
         * Discord-rail way that caused zombie UI).
         */
        UI_CONTAINER,
    }

    // ==================================================================
    // Listener types (one per point family)
    // ==================================================================

    public interface PreSendListener {
        boolean onPreSend(long dialogId, String text);
    }

    public interface MessageListener {
        void onMessage(int account, MessageObject message);
    }

    public interface FeedItemListener {
        /** Return false to drop this card from the feed. */
        boolean onFeedItem(long dialogId, int messageId, String title, String summary, String category);
    }

    /** Mutable carrier — hooks may rewrite the AI result in place. */
    public static final class AiText {
        public final String kind;
        public String text;
        public AiText(String kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    public interface AiTextListener {
        void onAiText(AiText result);
    }

    public interface AudioListener {
        void onAudioTrackChanged(MessageObject track, boolean isPlaying);
    }

    public interface PresetListener {
        void onPresetChanged(String presetName);
    }

    public static final class VaultEvent {
        public final String action; // "uploaded" | "downloaded" | "deleted"
        public final String fileId;
        public final String fileName;
        public VaultEvent(String action, String fileId, String fileName) {
            this.action = action;
            this.fileId = fileId;
            this.fileName = fileName;
        }
    }

    public interface VaultListener {
        void onVaultEvent(VaultEvent event);
    }

    public interface DialogVisibilityListener {
        /** Return false to hide this dialog row. */
        boolean isDialogVisible(long dialogId);
    }

    public interface UiContainerListener {
        /** Called on the UI thread right after the container is constructed. */
        void onUiContainerAttached(String tag, android.view.ViewGroup container);
    }

    // ==================================================================
    // Registration
    // ==================================================================

    /** Token returned by every register() — pause/resume/unregister one hook. */
    public static final class Handle {
        private final Point point;
        private final Entry entry;
        private Handle(Point point, Entry entry) {
            this.point = point;
            this.entry = entry;
        }
        public void pause() { entry.enabled = false; }
        public void resume() { entry.enabled = true; }
        public boolean isActive() { return entry.enabled && entry.failures < QUARANTINE_AFTER; }
        public void unregister() { remove(point, entry); }
        public Object owner() { return entry.owner; }
        public int priority() { return entry.priority; }
    }

    private static final int QUARANTINE_AFTER = 3;

    private static final class Entry {
        final Object listener;
        final Object owner;
        final String name;
        final int priority;
        final long seq;
        volatile boolean enabled = true;
        volatile int failures = 0;
        Entry(Object listener, Object owner, String name, int priority, long seq) {
            this.listener = listener;
            this.owner = owner;
            this.name = name;
            this.priority = priority;
            this.seq = seq;
        }
    }

    private static final AtomicLong SEQ = new AtomicLong();
    private static final Comparator<Entry> ORDER = (a, b) -> {
        if (a.priority != b.priority) return Integer.compare(b.priority, a.priority);
        return Long.compare(a.seq, b.seq);
    };

    private static final Map<Point, List<Entry>> REGISTRY = new ConcurrentHashMap<>();
    private static volatile Map<Point, Entry[]> snapshots = Collections.emptyMap();

    private static final Map<Point, Long> dispatchCount = new ConcurrentHashMap<>();
    private static final Map<Point, Long> dispatchTimeNs = new ConcurrentHashMap<>();
    private static final Map<Point, Long> dispatchFailures = new ConcurrentHashMap<>();

    static {
        for (Point p : Point.values()) {
            REGISTRY.put(p, Collections.synchronizedList(new ArrayList<>()));
            dispatchCount.put(p, 0L);
            dispatchTimeNs.put(p, 0L);
            dispatchFailures.put(p, 0L);
        }
        rebuildSnapshots();
    }

    private static void rebuildSnapshots() {
        Map<Point, Entry[]> next = new ConcurrentHashMap<>();
        for (Map.Entry<Point, List<Entry>> e : REGISTRY.entrySet()) {
            List<Entry> copy;
            synchronized (e.getValue()) {
                copy = new ArrayList<>(e.getValue());
            }
            Collections.sort(copy, ORDER);
            next.put(e.getKey(), copy.toArray(new Entry[0]));
        }
        snapshots = next;
    }

    private static Handle add(Point point, Object listener, Object owner, String name, int priority) {
        if (listener == null) throw new IllegalArgumentException("MioHook listener is null (" + point + ")");
        Entry entry = new Entry(listener, owner, name != null ? name : String.valueOf(owner), priority, SEQ.getAndIncrement());
        List<Entry> list = REGISTRY.get(point);
        synchronized (list) {
            list.add(entry);
        }
        rebuildSnapshots();
        return new Handle(point, entry);
    }

    private static void remove(Point point, Entry entry) {
        List<Entry> list = REGISTRY.get(point);
        if (list == null) return;
        synchronized (list) {
            list.remove(entry);
        }
        rebuildSnapshots();
    }

    /** Detaches every hook owned by {@code owner} (e.g. a disabled plugin). Returns the count removed. */
    public static int unregisterAll(Object owner) {
        int removed = 0;
        for (Map.Entry<Point, List<Entry>> e : REGISTRY.entrySet()) {
            synchronized (e.getValue()) {
                for (int i = e.getValue().size() - 1; i >= 0; i--) {
                    Entry en = e.getValue().get(i);
                    if (en.owner == owner || (owner != null && owner.equals(en.owner))) {
                        e.getValue().remove(i);
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) rebuildSnapshots();
        return removed;
    }

    // Typed register shortcuts ------------------------------------------

    public static Handle onPreSend(Object owner, String name, int priority, PreSendListener l) {
        return add(Point.MESSAGE_PRE_SEND, l, owner, name, priority);
    }

    public static Handle onMessage(Object owner, String name, int priority, MessageListener l) {
        return add(Point.MESSAGE_RECEIVED, l, owner, name, priority);
    }

    public static Handle onFeedItem(Object owner, String name, int priority, FeedItemListener l) {
        return add(Point.FEED_ITEM, l, owner, name, priority);
    }

    public static Handle onAiText(Object owner, String name, int priority, AiTextListener l) {
        return add(Point.AI_TEXT_RESULT, l, owner, name, priority);
    }

    public static Handle onAudio(Object owner, String name, int priority, AudioListener l) {
        return add(Point.AUDIO_TRACK_CHANGED, l, owner, name, priority);
    }

    public static Handle onPreset(Object owner, String name, int priority, PresetListener l) {
        return add(Point.PRESET_CHANGED, l, owner, name, priority);
    }

    public static Handle onVault(Object owner, String name, int priority, VaultListener l) {
        return add(Point.VAULT_FILE_EVENT, l, owner, name, priority);
    }

    public static Handle onDialogVisibility(Object owner, String name, int priority, DialogVisibilityListener l) {
        return add(Point.DIALOG_VISIBILITY, l, owner, name, priority);
    }

    public static Handle onUiContainer(Object owner, String name, int priority, UiContainerListener l) {
        return add(Point.UI_CONTAINER, l, owner, name, priority);
    }

    // ==================================================================
    // Dispatch (all re-entrant; a hook may register/unregister mid-flight
    // thanks to array snapshots)
    // ==================================================================

    private static void noteStart(Point p) {
        dispatchCount.put(p, dispatchCount.get(p) + 1);
    }

    private static void noteTime(Point p, long startedNs) {
        dispatchTimeNs.put(p, dispatchTimeNs.get(p) + (System.nanoTime() - startedNs));
    }

    private static boolean failed(Entry e, Point p, Throwable t) {
        FileLog.e("MioHook [" + p + "] '" + e.name + "' failed", t);
        e.failures++;
        dispatchFailures.put(p, dispatchFailures.get(p) + 1);
        if (e.failures >= QUARANTINE_AFTER && e.enabled) {
            e.enabled = false;
            FileLog.e("MioHook [" + p + "] '" + e.name + "' quarantined after " + e.failures + " failures");
        }
        return e.enabled;
    }

    /** Vetoable pre-send. Any hook returning false cancels the send. */
    public static boolean dispatchPreSend(long dialogId, String text) {
        Entry[] arr = snapshots.get(Point.MESSAGE_PRE_SEND);
        if (arr == null || arr.length == 0) return true;
        long t0 = System.nanoTime();
        noteStart(Point.MESSAGE_PRE_SEND);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    if (!((PreSendListener) e.listener).onPreSend(dialogId, text)) return false;
                } catch (Throwable t) {
                    failed(e, Point.MESSAGE_PRE_SEND, t);
                }
            }
            return true;
        } finally {
            noteTime(Point.MESSAGE_PRE_SEND, t0);
        }
    }

    public static void dispatchMessage(int account, MessageObject message) {
        Entry[] arr = snapshots.get(Point.MESSAGE_RECEIVED);
        if (arr == null || arr.length == 0) return;
        long t0 = System.nanoTime();
        noteStart(Point.MESSAGE_RECEIVED);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((MessageListener) e.listener).onMessage(account, message);
                } catch (Throwable t) {
                    failed(e, Point.MESSAGE_RECEIVED, t);
                }
            }
        } finally {
            noteTime(Point.MESSAGE_RECEIVED, t0);
        }
    }

    /** Returns false when any hook vetoes the card. */
    public static boolean dispatchFeedItem(long dialogId, int messageId, String title, String summary, String category) {
        Entry[] arr = snapshots.get(Point.FEED_ITEM);
        if (arr == null || arr.length == 0) return true;
        long t0 = System.nanoTime();
        noteStart(Point.FEED_ITEM);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    if (!((FeedItemListener) e.listener).onFeedItem(dialogId, messageId, title, summary, category)) {
                        return false;
                    }
                } catch (Throwable t) {
                    failed(e, Point.FEED_ITEM, t);
                }
            }
            return true;
        } finally {
            noteTime(Point.FEED_ITEM, t0);
        }
    }

    /** Runs the mutable AI result through all filters; returns the final text. */
    public static String dispatchAiText(String kind, String text) {
        Entry[] arr = snapshots.get(Point.AI_TEXT_RESULT);
        if (arr == null || arr.length == 0) return text;
        long t0 = System.nanoTime();
        noteStart(Point.AI_TEXT_RESULT);
        try {
            AiText carrier = new AiText(kind, text);
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((AiTextListener) e.listener).onAiText(carrier);
                    if (carrier.text == null) carrier.text = "";
                } catch (Throwable t) {
                    failed(e, Point.AI_TEXT_RESULT, t);
                }
            }
            return carrier.text;
        } finally {
            noteTime(Point.AI_TEXT_RESULT, t0);
        }
    }

    public static void dispatchAudio(MessageObject track, boolean isPlaying) {
        Entry[] arr = snapshots.get(Point.AUDIO_TRACK_CHANGED);
        if (arr == null || arr.length == 0) return;
        long t0 = System.nanoTime();
        noteStart(Point.AUDIO_TRACK_CHANGED);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((AudioListener) e.listener).onAudioTrackChanged(track, isPlaying);
                } catch (Throwable t) {
                    failed(e, Point.AUDIO_TRACK_CHANGED, t);
                }
            }
        } finally {
            noteTime(Point.AUDIO_TRACK_CHANGED, t0);
        }
    }

    public static void dispatchPreset(String presetName) {
        Entry[] arr = snapshots.get(Point.PRESET_CHANGED);
        if (arr == null || arr.length == 0) return;
        long t0 = System.nanoTime();
        noteStart(Point.PRESET_CHANGED);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((PresetListener) e.listener).onPresetChanged(presetName);
                } catch (Throwable t) {
                    failed(e, Point.PRESET_CHANGED, t);
                }
            }
        } finally {
            noteTime(Point.PRESET_CHANGED, t0);
        }
    }

    public static void dispatchVault(String action, String fileId, String fileName) {
        Entry[] arr = snapshots.get(Point.VAULT_FILE_EVENT);
        if (arr == null || arr.length == 0) return;
        long t0 = System.nanoTime();
        noteStart(Point.VAULT_FILE_EVENT);
        try {
            VaultEvent event = new VaultEvent(action, fileId, fileName);
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((VaultListener) e.listener).onVaultEvent(event);
                } catch (Throwable t) {
                    failed(e, Point.VAULT_FILE_EVENT, t);
                }
            }
        } finally {
            noteTime(Point.VAULT_FILE_EVENT, t0);
        }
    }

    /** Returns false when any hook hides the dialog. */
    public static boolean isDialogVisible(long dialogId) {        Entry[] arr = snapshots.get(Point.DIALOG_VISIBILITY);
        if (arr == null || arr.length == 0) return true;
        long t0 = System.nanoTime();
        noteStart(Point.DIALOG_VISIBILITY);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    if (!((DialogVisibilityListener) e.listener).isDialogVisible(dialogId)) {
                        return false;
                    }
                } catch (Throwable t) {
                    failed(e, Point.DIALOG_VISIBILITY, t);
                }
            }
            return true;
        } finally {
            noteTime(Point.DIALOG_VISIBILITY, t0);
        }
    }

    /** Notifies plugins that a named UI container was created (UI thread). */
    public static void dispatchUiContainer(String tag, android.view.ViewGroup container) {
        Entry[] arr = snapshots.get(Point.UI_CONTAINER);
        if (arr == null || arr.length == 0 || container == null) return;
        long t0 = System.nanoTime();
        noteStart(Point.UI_CONTAINER);
        try {
            for (Entry e : arr) {
                if (!e.enabled) continue;
                try {
                    ((UiContainerListener) e.listener).onUiContainerAttached(tag, container);
                } catch (Throwable t) {
                    failed(e, Point.UI_CONTAINER, t);
                }
            }
        } finally {
            noteTime(Point.UI_CONTAINER, t0);
        }
    }

    // ==================================================================
    // Diagnostics
    // ==================================================================    /** One-line-per-point stats for logs / debug settings. */
    public static String dumpStats() {
        StringBuilder sb = new StringBuilder("MioHook stats:\n");
        for (Point p : Point.values()) {
            Entry[] arr = snapshots.get(p);
            long n = dispatchCount.get(p);
            long ns = dispatchTimeNs.get(p);
            long f = dispatchFailures.get(p);
            sb.append("  ").append(p)
              .append(": hooks=").append(arr == null ? 0 : arr.length)
              .append(" calls=").append(n)
              .append(" avgUs=").append(n == 0 ? 0 : (ns / n) / 1000)
              .append(" failures=").append(f).append('\n');
        }
        return sb.toString();
    }

    /** Runs on UI thread (used by debuggers, not by dispatch). */
    public static void logStats() {
        AndroidUtilities.runOnUIThread(() -> FileLog.d(dumpStats()));
    }
}
