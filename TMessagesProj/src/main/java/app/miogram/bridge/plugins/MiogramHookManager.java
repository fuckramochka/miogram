package app.miogram.bridge.plugins;

import android.view.ViewGroup;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import app.miogram.bridge.hooks.MioHook;

/**
 * Compatibility facade over {@link MioHook}.
 *
 * <p>Historic note: this class used to own private listener lists whose
 * dispatchers were never called from anywhere — hooks registered here were
 * silently dead. It now delegates every point to the living MioHook bus,
 * so old callers keep compiling AND their hooks actually fire.
 *
 * <p>New code should use {@link MioHook} directly (priorities, quarantine,
 * stats, per-plugin {@code unregisterAll}).
 */
public class MiogramHookManager {

    public interface MessageHook {
        boolean onPreSendMessage(long dialogId, String text, Object params);
        void onPostReceiveMessage(MessageObject message);
    }

    public interface UiHook {
        void onAttachMainTabs(ViewGroup tabsContainer);
        void onAttachChatActionBar(ViewGroup actionBar);
        void onAttachDrawer(ViewGroup drawer);
    }

    public interface AudioHook {
        void onAudioTrackChanged(MessageObject track, boolean isPlaying);
        void onVisualizerAmplitude(float[] amplitudes, float bassLevel);
    }

    private static volatile MiogramHookManager instance;

    /** MioHook handles behind each legacy registration (for unregister). */
    private final Map<MessageHook, MioHook.Handle[]> messageHandles = new ConcurrentHashMap<>();
    private final Map<UiHook, MioHook.Handle[]> uiHandles = new ConcurrentHashMap<>();
    private final Map<AudioHook, MioHook.Handle> audioHandles = new ConcurrentHashMap<>();
    /** 60fps visualizer data deliberately stays off the bus (see below). */
    private final List<AudioHook> visualizerHooks = new CopyOnWriteArrayList<>();
    private final Map<String, Object> globalPluginState = new ConcurrentHashMap<>();

    public static MiogramHookManager getInstance() {
        if (instance == null) {
            synchronized (MiogramHookManager.class) {
                if (instance == null) {
                    instance = new MiogramHookManager();
                }
            }
        }
        return instance;
    }

    public void registerMessageHook(MessageHook hook) {
        if (hook == null || messageHandles.containsKey(hook)) return;
        MioHook.Handle pre = MioHook.onPreSend(hook, hookName(hook), 0,
                (dialogId, text) -> {
                    try {
                        return hook.onPreSendMessage(dialogId, text, null);
                    } catch (Throwable t) {
                        FileLog.e(t);
                        return true;
                    }
                });
        MioHook.Handle post = MioHook.onMessage(hook, hookName(hook), 0,
                (account, message) -> {
                    try {
                        hook.onPostReceiveMessage(message);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
        messageHandles.put(hook, new MioHook.Handle[]{pre, post});
    }

    public void unregisterMessageHook(MessageHook hook) {
        MioHook.Handle[] handles = messageHandles.remove(hook);
        if (handles != null) {
            for (MioHook.Handle h : handles) h.unregister();
        } else {
            MioHook.unregisterAll(hook);
        }
    }

    public void registerUiHook(UiHook hook) {
        if (hook == null || uiHandles.containsKey(hook)) return;
        MioHook.Handle tabs = MioHook.onUiContainer(hook, hookName(hook), 0,
                (tag, container) -> {
                    if (!"main_tabs".equals(tag)) return;
                    try {
                        hook.onAttachMainTabs(container);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
        MioHook.Handle bar = MioHook.onUiContainer(hook, hookName(hook), 0,
                (tag, container) -> {
                    if (!"chat_action_bar".equals(tag)) return;
                    try {
                        hook.onAttachChatActionBar(container);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
        MioHook.Handle drawer = MioHook.onUiContainer(hook, hookName(hook), 0,
                (tag, container) -> {
                    if (!"drawer".equals(tag)) return;
                    try {
                        hook.onAttachDrawer(container);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
        uiHandles.put(hook, new MioHook.Handle[]{tabs, bar, drawer});
    }

    public void unregisterUiHook(UiHook hook) {
        MioHook.Handle[] handles = uiHandles.remove(hook);
        if (handles != null) {
            for (MioHook.Handle h : handles) h.unregister();
        } else {
            MioHook.unregisterAll(hook);
        }
    }

    public void registerAudioHook(AudioHook hook) {
        if (hook == null || audioHandles.containsKey(hook)) return;
        MioHook.Handle h = MioHook.onAudio(hook, hookName(hook), 0,
                (track, playing) -> {
                    try {
                        hook.onAudioTrackChanged(track, playing);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
        audioHandles.put(hook, h);
        if (!visualizerHooks.contains(hook)) visualizerHooks.add(hook);
    }

    public void unregisterAudioHook(AudioHook hook) {
        MioHook.Handle h = audioHandles.remove(hook);
        if (h != null) h.unregister();
        else MioHook.unregisterAll(hook);
        visualizerHooks.remove(hook);
    }

    private static String hookName(Object hook) {
        try {
            return hook.getClass().getSimpleName();
        } catch (Throwable ignored) {
            return "legacy-hook";
        }
    }

    // --- Dispatchers (all live — they hit the MioHook bus) ---

    public boolean dispatchPreSendMessage(long dialogId, String text, Object params) {
        return MioHook.dispatchPreSend(dialogId, text);
    }

    public void dispatchPostReceiveMessage(MessageObject message) {
        MioHook.dispatchMessage(UserConfig.selectedAccount, message);
    }

    public void dispatchAttachMainTabs(ViewGroup tabsContainer) {
        MioHook.dispatchUiContainer("main_tabs", tabsContainer);
    }

    public void dispatchAttachChatActionBar(ViewGroup actionBar) {
        MioHook.dispatchUiContainer("chat_action_bar", actionBar);
    }

    public void dispatchAttachDrawer(ViewGroup drawer) {
        MioHook.dispatchUiContainer("drawer", drawer);
    }

    public void dispatchAudioTrackChanged(MessageObject track, boolean isPlaying) {
        MioHook.dispatchAudio(track, isPlaying);
    }

    /**
     * Visualizer amplitude stays on a dedicated local loop: it fires ~60x/sec
     * and must never pay bus overhead or risk quarantine from a slow hook.
     */
    public void dispatchVisualizerAmplitude(float[] amplitudes, float bassLevel) {
        for (AudioHook hook : visualizerHooks) {
            try {
                hook.onVisualizerAmplitude(amplitudes, bassLevel);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void setPluginState(String key, Object value) {
        if (key != null) {
            if (value == null) {
                globalPluginState.remove(key);
            } else {
                globalPluginState.put(key, value);
            }
        }
    }

    public Object getPluginState(String key) {
        return key != null ? globalPluginState.get(key) : null;
    }
}
