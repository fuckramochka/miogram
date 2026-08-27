package app.miogram.bridge.plugins;

import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Omnipotent Hook Engine for Miogram Plugins:
 * - Allows any plugin (Rust WASM, Python, or Dex) to hook into core Telegram events
 * - Dispatches hooks for Messages, UI View Trees, Audio Playback, and Navigation
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

    private final List<MessageHook> messageHooks = Collections.synchronizedList(new ArrayList<>());
    private final List<UiHook> uiHooks = Collections.synchronizedList(new ArrayList<>());
    private final List<AudioHook> audioHooks = Collections.synchronizedList(new ArrayList<>());
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
        if (hook != null && !messageHooks.contains(hook)) {
            messageHooks.add(hook);
        }
    }

    public void unregisterMessageHook(MessageHook hook) {
        messageHooks.remove(hook);
    }

    public void registerUiHook(UiHook hook) {
        if (hook != null && !uiHooks.contains(hook)) {
            uiHooks.add(hook);
        }
    }

    public void unregisterUiHook(UiHook hook) {
        uiHooks.remove(hook);
    }

    public void registerAudioHook(AudioHook hook) {
        if (hook != null && !audioHooks.contains(hook)) {
            audioHooks.add(hook);
        }
    }

    public void unregisterAudioHook(AudioHook hook) {
        audioHooks.remove(hook);
    }

    // --- Dispatchers ---

    public boolean dispatchPreSendMessage(long dialogId, String text, Object params) {
        for (MessageHook hook : messageHooks) {
            try {
                if (!hook.onPreSendMessage(dialogId, text, params)) {
                    return false; // Cancel sending if hook returns false
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        return true;
    }

    public void dispatchPostReceiveMessage(MessageObject message) {
        for (MessageHook hook : messageHooks) {
            try {
                hook.onPostReceiveMessage(message);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void dispatchAttachMainTabs(ViewGroup tabsContainer) {
        for (UiHook hook : uiHooks) {
            try {
                hook.onAttachMainTabs(tabsContainer);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void dispatchAttachChatActionBar(ViewGroup actionBar) {
        for (UiHook hook : uiHooks) {
            try {
                hook.onAttachChatActionBar(actionBar);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void dispatchAttachDrawer(ViewGroup drawer) {
        for (UiHook hook : uiHooks) {
            try {
                hook.onAttachDrawer(drawer);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void dispatchAudioTrackChanged(MessageObject track, boolean isPlaying) {
        for (AudioHook hook : audioHooks) {
            try {
                hook.onAudioTrackChanged(track, isPlaying);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void dispatchVisualizerAmplitude(float[] amplitudes, float bassLevel) {
        for (AudioHook hook : audioHooks) {
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
