package app.miogram.bridge.system;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.vault.MiogramDoubleBottomManager;

/**
 * System Dialogs Manager for Miogram:
 * Injects official system dialogs into DialogsActivity:
 * 1. "Новини Miogram ໒꒱" (Smart Feed) -> DIALOG_ID_FEED = -999001L
 * 2. "Канбан-нотатки 📋" (Kanban Board) -> DIALOG_ID_KANBAN = -999002L
 */
public class MiogramSystemDialogsManager {

    public static final long DIALOG_ID_FEED = -999001L;
    public static final long DIALOG_ID_KANBAN = -999002L;

    private static TLRPC.TL_channel feedChat;
    private static TLRPC.TL_channel kanbanChat;

    private static TLRPC.TL_dialog feedDialog;
    private static TLRPC.TL_dialog kanbanDialog;

    private static MessageObject feedMessage;
    private static MessageObject kanbanMessage;

    public static boolean isSystemDialog(long dialogId) {
        return dialogId == DIALOG_ID_FEED || dialogId == DIALOG_ID_KANBAN;
    }

    public static boolean isSystemChat(long chatId) {
        return chatId == -DIALOG_ID_FEED || chatId == -DIALOG_ID_KANBAN;
    }

    public static boolean isSystemUser(long userId) {
        return false;
    }

    public static synchronized TLRPC.Chat getSystemChat(long chatId) {
        if (chatId == -DIALOG_ID_FEED) {
            if (feedChat == null) {
                feedChat = new TLRPC.TL_channel();
                feedChat.id = -DIALOG_ID_FEED;
                feedChat.title = MiogramLocale.get("Новини Miogram ໒꒱", "Новости Miogram ໒꒱", "Miogram News ໒꒱");
                feedChat.broadcast = true;
                feedChat.verified = true;
                feedChat.creator = true;
                feedChat.flags = TLRPC.CHAT_FLAG_IS_PUBLIC;
            }
            return feedChat;
        } else if (chatId == -DIALOG_ID_KANBAN) {
            if (kanbanChat == null) {
                kanbanChat = new TLRPC.TL_channel();
                kanbanChat.id = -DIALOG_ID_KANBAN;
                kanbanChat.title = MiogramLocale.get("Канбан-нотатки 📋", "Канбан-заметки 📋", "Kanban Notes 📋");
                kanbanChat.broadcast = false;
                kanbanChat.creator = true;
                kanbanChat.flags = TLRPC.CHAT_FLAG_IS_PUBLIC;
            }
            return kanbanChat;
        }
        return null;
    }

    public static synchronized TLRPC.Dialog getSystemDialog(long dialogId) {
        int now = (int) (System.currentTimeMillis() / 1000);
        if (dialogId == DIALOG_ID_FEED) {
            if (feedDialog == null) {
                feedDialog = new TLRPC.TL_dialog();
                feedDialog.id = DIALOG_ID_FEED;
                feedDialog.unread_count = 0;
                feedDialog.pinned = false;
            }
            feedDialog.last_message_date = now;
            return feedDialog;
        } else if (dialogId == DIALOG_ID_KANBAN) {
            if (kanbanDialog == null) {
                kanbanDialog = new TLRPC.TL_dialog();
                kanbanDialog.id = DIALOG_ID_KANBAN;
                kanbanDialog.unread_count = 0;
                kanbanDialog.pinned = false;
            }
            kanbanDialog.last_message_date = now;
            return kanbanDialog;
        }
        return null;
    }

    public static synchronized MessageObject getSystemMessage(long dialogId, int currentAccount) {
        if (dialogId == DIALOG_ID_FEED) {
            if (feedMessage == null) {
                TLRPC.TL_message msg = new TLRPC.TL_message();
                msg.id = 1;
                msg.date = (int) (System.currentTimeMillis() / 1000);
                msg.message = MiogramLocale.get(
                        "Останні публікації ваших каналів та новини клієнта ໒꒱",
                        "Последние публикации ваших каналов и новости клиента ໒꒱",
                        "Latest posts from your channels and client updates ໒꒱"
                );
                msg.peer_id = new TLRPC.TL_peerChannel();
                msg.peer_id.channel_id = -DIALOG_ID_FEED;
                feedMessage = new MessageObject(currentAccount, msg, false, false);
            }
            return feedMessage;
        } else if (dialogId == DIALOG_ID_KANBAN) {
            if (kanbanMessage == null) {
                TLRPC.TL_message msg = new TLRPC.TL_message();
                msg.id = 2;
                msg.date = (int) (System.currentTimeMillis() / 1000);
                msg.message = MiogramLocale.get(
                        "Ваша персональна дошка завдань та швидких заміток 📋",
                        "Ваша персональная доска задач и быстрых заметок 📋",
                        "Your personal task board and quick notes 📋"
                );
                msg.peer_id = new TLRPC.TL_peerChannel();
                msg.peer_id.channel_id = -DIALOG_ID_KANBAN;
                kanbanMessage = new MessageObject(currentAccount, msg, false, false);
            }
            return kanbanMessage;
        }
        return null;
    }

    /**
     * Injects system dialogs into list if appropriate.
     */
    public static void injectSystemDialogs(ArrayList<TLRPC.Dialog> targetList, int currentAccount) {
        if (targetList == null) return;
        if (MiogramDoubleBottomManager.isDuressActive()) {
            return; // Never show custom bridge dialogs during duress
        }

        boolean hasFeed = false;
        boolean hasKanban = false;
        for (int i = 0; i < targetList.size(); i++) {
            TLRPC.Dialog d = targetList.get(i);
            if (d != null) {
                if (d.id == DIALOG_ID_FEED) hasFeed = true;
                if (d.id == DIALOG_ID_KANBAN) hasKanban = true;
            }
        }

        if (!hasKanban) {
            targetList.add(0, getSystemDialog(DIALOG_ID_KANBAN));
        }
        if (!hasFeed) {
            targetList.add(0, getSystemDialog(DIALOG_ID_FEED));
        }
    }

    /**
     * Creates custom stylized circular avatar for system dialogs.
     */
    public static Drawable getSystemAvatar(Context context, long dialogId) {
        int size = AndroidUtilities.dp(54);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        RectF rect = new RectF(0, 0, size, size);
        if (dialogId == DIALOG_ID_FEED) {
            paint.setColor(0xFF26122C);
            canvas.drawOval(rect, paint);
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_feed);
            if (icon != null) {
                int iconSize = AndroidUtilities.dp(28);
                int left = (size - iconSize) / 2;
                int top = (size - iconSize) / 2;
                icon.setBounds(left, top, left + iconSize, top + iconSize);
                icon.setTint(0xFFFF5C97);
                icon.draw(canvas);
            }
        } else if (dialogId == DIALOG_ID_KANBAN) {
            paint.setColor(0xFF132230);
            canvas.drawOval(rect, paint);
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.msg_saved);
            if (icon != null) {
                int iconSize = AndroidUtilities.dp(26);
                int left = (size - iconSize) / 2;
                int top = (size - iconSize) / 2;
                icon.setBounds(left, top, left + iconSize, top + iconSize);
                icon.setTint(0xFF4CE0D2);
                icon.draw(canvas);
            }
        }

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
