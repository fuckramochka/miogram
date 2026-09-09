package app.miogram.bridge.plugins;

import android.os.Bundle;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import app.exteraless.plugins.Plugin;
import app.exteraless.plugins.PluginsController;

/**
 * High-performance Native & WASM In-App Notifications Engine for Miogram.
 * Intercepts incoming messages and renders interactive top banner notifications
 * when the user is chatting in another conversation or browsing the app.
 */
public class MiogramInAppNotifications implements NotificationCenter.NotificationCenterDelegate {

    private static volatile MiogramInAppNotifications instance;
    private boolean registered = false;

    public static MiogramInAppNotifications getInstance() {
        if (instance == null) {
            synchronized (MiogramInAppNotifications.class) {
                if (instance == null) {
                    instance = new MiogramInAppNotifications();
                }
            }
        }
        return instance;
    }

    public synchronized void register() {
        if (registered) return;
        registered = true;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.didReceiveNewMessages);
        }
    }

    public synchronized void unregister() {
        if (!registered) return;
        registered = false;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        }
    }

    private boolean isPluginActive(String id) {
        try {
            Plugin p = PluginsController.getInstance().getPlugin(id);
            return p != null && p.enabled;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.didReceiveNewMessages) return;
        if (args == null || args.length < 2) return;

        // Check if in-app notifications plugin is active or installed
        boolean pluginEnabled = isPluginActive("in_app_notifications")
                || isPluginActive("miogram_in_app_notifications")
                || isPluginActive("in_app_notifications_rust")
                || isPluginActive("miogram_in_app_notifications_wasm")
                || isPluginActive("miogram_in_app_notifications_mioplugin");

        if (!pluginEnabled) {
            return;
        }

        try {
            long dialogId = (Long) args[0];
            ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];
            boolean scheduled = args.length > 2 && (Boolean) args[2];

            if (scheduled || messageObjects == null || messageObjects.isEmpty()) return;

            // Living MioHook site: every incoming message passes here on all accounts.
            for (int i = 0; i < messageObjects.size(); i++) {
                try {
                    MessageObject m = messageObjects.get(i);
                    if (m != null) app.miogram.bridge.hooks.MioHook.dispatchMessage(account, m);
                } catch (Throwable ignored) {}
            }

            LaunchActivity act = LaunchActivity.instance;
            if (act == null || act.isFinishing()) return;

            BaseFragment fragment = act.getSafeLastFragment();
            if (fragment == null) return;

            // If user is currently looking at this exact chat, do not distract with a banner
            if (fragment instanceof ChatActivity) {
                ChatActivity chat = (ChatActivity) fragment;
                if (chat.getDialogId() == dialogId) {
                    return;
                }
            }

            MessageObject msg = messageObjects.get(0);
            if (msg.isOutOwner() || msg.isAyuDeleted()) return;

            CharSequence messageText = msg.messageText;
            if (TextUtils.isEmpty(messageText)) {
                if (msg.isVideo()) messageText = app.miogram.bridge.MiogramLocale.get("📹 Відеоповідомлення", "📹 Видеосообщение", "📹 Video message");
                else if (msg.isVoice()) messageText = app.miogram.bridge.MiogramLocale.get("🎙️ Голосове повідомлення", "🎙️ Голосовое сообщение", "🎙️ Voice message");
                else if (msg.isRoundVideo()) messageText = app.miogram.bridge.MiogramLocale.get("⭕ Відеоповідомлення (кружечок)", "⭕ Видеосообщение (кружок)", "⭕ Round video message");
                else if (msg.isPhoto()) messageText = app.miogram.bridge.MiogramLocale.get("🖼️ Фотографія", "🖼️ Фотография", "🖼️ Photo");
                else if (msg.isSticker()) messageText = app.miogram.bridge.MiogramLocale.get("✨ Стікер", "✨ Стикер", "✨ Sticker");
                else messageText = app.miogram.bridge.MiogramLocale.get("Повідомлення", "Сообщение", "Message");
            }

            String title = "";
            MessagesController mc = MessagesController.getInstance(account);
            if (dialogId > 0) {
                TLRPC.User user = mc.getUser(dialogId);
                if (user != null) {
                    title = user.first_name != null ? user.first_name : app.miogram.bridge.MiogramLocale.get("Користувач", "Пользователь", "User");
                }
            } else {
                TLRPC.Chat chat = mc.getChat(-dialogId);
                if (chat != null) {
                    title = chat.title != null ? chat.title : app.miogram.bridge.MiogramLocale.get("Група", "Группа", "Group");
                }
            }

            if (TextUtils.isEmpty(title)) {
                title = app.miogram.bridge.MiogramLocale.get("Нове повідомлення", "Новое сообщение", "New message");
            }

            final String finalTitle = title;
            final CharSequence finalBody = messageText;
            final long finalDialogId = dialogId;

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    LaunchActivity currentAct = LaunchActivity.instance;
                    if (currentAct == null || currentAct.isFinishing()) return;
                    BaseFragment currentFrag = currentAct.getSafeLastFragment();
                    if (currentFrag == null) return;

                    // If still in another chat or home screen, show interactive Bulletin banner
                    Bulletin bulletin = BulletinFactory.of(currentFrag).createSimpleBulletin(
                            R.drawable.msg_notifications,
                            finalTitle + ": " + finalBody,
                            app.miogram.bridge.MiogramLocale.get("Відкрити", "Открыть", "Open"),
                            () -> {
                                Bundle b = new Bundle();
                                if (finalDialogId > 0) {
                                    b.putLong("user_id", finalDialogId);
                                } else {
                                    b.putLong("chat_id", -finalDialogId);
                                }
                                currentFrag.presentFragment(new ChatActivity(b));
                            }
                    );
                    bulletin.show();
                } catch (Throwable t) {
                    // Fallback to simple toast or log
                }
            });
        } catch (Throwable ignored) {
        }
    }
}
