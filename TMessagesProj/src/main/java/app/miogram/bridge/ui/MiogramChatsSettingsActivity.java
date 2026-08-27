package app.miogram.bridge.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.exteraless.chats.ChatsConfig;
import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Chats & Media Settings with dynamic multilingual localization.
 */
public class MiogramChatsSettingsActivity extends BaseNekoSettingsActivity {

    private int headerCameraRow;
    private int cameraTypeRow;
    private int cameraMirrorRow;
    private int cameraWideAngleRow;
    private int cameraStabilizationRow;
    private int cameraFpsRow;
    private int cameraZoomRow;
    private int cameraInfoRow;

    private int headerChatActionsRow;
    private int doubleTapActionRow;
    private int noQuoteForwardRow;
    private int combineMessagesRow;
    private int showMessageIdRow;
    private int showOnlineStatusRow;
    private int chatActionsInfoRow;

    private int headerStickersRow;
    private int stickerShapeRow;
    private int channelBottomButtonRow;
    private int stickersInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Чати та Медіа", "Чаты и Медиа", "Chats & Media");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerCameraRow = addRow();
        cameraTypeRow = addRow();
        cameraMirrorRow = addRow();
        cameraWideAngleRow = addRow();
        cameraStabilizationRow = addRow();
        cameraFpsRow = addRow();
        cameraZoomRow = addRow();
        cameraInfoRow = addRow();

        headerChatActionsRow = addRow();
        doubleTapActionRow = addRow();
        noQuoteForwardRow = addRow();
        combineMessagesRow = addRow();
        showMessageIdRow = addRow();
        showOnlineStatusRow = addRow();
        chatActionsInfoRow = addRow();

        headerStickersRow = addRow();
        stickerShapeRow = addRow();
        channelBottomButtonRow = addRow();
        stickersInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == cameraTypeRow) {
            showCameraTypeDialog();
        } else if (position == cameraMirrorRow) {
            boolean v = !ChatsConfig.cameraMirrorMode.Bool();
            ChatsConfig.cameraMirrorMode.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == cameraWideAngleRow) {
            boolean v = !ChatsConfig.startWithWideAngleCamera.Bool();
            ChatsConfig.startWithWideAngleCamera.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == cameraStabilizationRow) {
            boolean v = !ChatsConfig.cameraStabilization.Bool();
            ChatsConfig.cameraStabilization.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == cameraFpsRow) {
            boolean v = !ChatsConfig.extendedFramesPerSecond.Bool();
            ChatsConfig.extendedFramesPerSecond.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == cameraZoomRow) {
            boolean v = !ChatsConfig.zoomSlider.Bool();
            ChatsConfig.zoomSlider.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == doubleTapActionRow) {
            showDoubleTapDialog();
        } else if (position == noQuoteForwardRow) {
            boolean v = !NaConfig.INSTANCE.getShowNoQuoteForward().Bool();
            NaConfig.INSTANCE.getShowNoQuoteForward().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == combineMessagesRow) {
            int v = NaConfig.INSTANCE.getCombineMessage().Int() == 0 ? 1 : 0;
            NaConfig.INSTANCE.getCombineMessage().setConfigInt(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v != 0);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == showMessageIdRow) {
            boolean v = !NaConfig.INSTANCE.getShowMessageID().Bool();
            NaConfig.INSTANCE.getShowMessageID().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == showOnlineStatusRow) {
            boolean v = !NaConfig.INSTANCE.getShowOnlineStatus().Bool();
            NaConfig.INSTANCE.getShowOnlineStatus().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == stickerShapeRow) {
            showStickerShapeDialog();
        } else if (position == channelBottomButtonRow) {
            showChannelBottomButtonDialog();
        }
    }

    private void showCameraTypeDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{"CameraX (Рекомендовано)", "Camera2", "Camera1 (Legacy)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Рушій камери", "Движок камеры", "Camera Engine"));
        builder.setItems(options, (dialog, which) -> {
            int val = which == 0 ? 2 : (which == 1 ? 1 : 0);
            ChatsConfig.cameraType.setConfigInt(val);
            listAdapter.notifyItemChanged(cameraTypeRow);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDoubleTapDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Вимкнено", "Выключено", "Disabled"),
                MiogramLocale.get("Відповідь", "Ответ", "Reply"),
                MiogramLocale.get("Реакція", "Реакция", "Reaction"),
                MiogramLocale.get("Копіювати", "Копировать", "Copy"),
                MiogramLocale.get("В Обране", "В Избранное", "Save to Saved"),
                MiogramLocale.get("Переклад", "Перевод", "Translate")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Подвійний тап по повідомленню", "Двойной тап по сообщению", "Double Tap on Message"));
        builder.setItems(options, (dialog, which) -> {
            NaConfig.INSTANCE.getDoubleTapAction().setConfigInt(which);
            listAdapter.notifyItemChanged(doubleTapActionRow);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showStickerShapeDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Оригінальна", "Оригинальная", "Original"),
                MiogramLocale.get("Скруглена", "Скругленная", "Rounded"),
                MiogramLocale.get("Бульбашка", "Пузырь", "Bubble")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Форма стікерів", "Форма стикеров", "Sticker Shape"));
        builder.setItems(options, (dialog, which) -> {
            ChatsConfig.stickerShape.setConfigInt(which);
            listAdapter.notifyItemChanged(stickerShapeRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showChannelBottomButtonDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Приховати", "Скрыть", "Hide"),
                "Mute / Unmute",
                MiogramLocale.get("Коментарі (Обговорення)", "Комментарии (Обсуждение)", "Comments")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Нижня кнопка в каналах", "Нижняя кнопка в каналах", "Channel Bottom Action"));
        builder.setItems(options, (dialog, which) -> {
            ChatsConfig.bottomButton.setConfigInt(which);
            listAdapter.notifyItemChanged(channelBottomButtonRow);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerCameraRow || position == headerChatActionsRow || position == headerStickersRow) {
                return TYPE_HEADER;
            } else if (position == cameraMirrorRow || position == cameraWideAngleRow
                    || position == cameraStabilizationRow || position == cameraFpsRow
                    || position == cameraZoomRow || position == noQuoteForwardRow
                    || position == combineMessagesRow || position == showMessageIdRow
                    || position == showOnlineStatusRow) {
                return TYPE_CHECK;
            } else if (position == cameraInfoRow || position == chatActionsInfoRow || position == stickersInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerCameraRow) {
                        cell.setText(MiogramLocale.get("Камера та відеоповідомлення (Кружечки)", "Камера и видеосообщения (Кружочки)", "Camera & Video Notes"));
                    } else if (position == headerChatActionsRow) {
                        cell.setText(MiogramLocale.get("Дії з повідомленнями та чатом", "Действия с сообщениями и чатом", "Message Actions & Gestures"));
                    } else if (position == headerStickersRow) {
                        cell.setText(MiogramLocale.get("Стікери та кнопки дій", "Стикеры и кнопки действий", "Stickers & Action Buttons"));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == cameraMirrorRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Дзеркальне відображення фронтальної камери", "Зеркальное отображение фронтальной камеры", "Mirror front camera"), ChatsConfig.cameraMirrorMode.Bool(), true);
                    } else if (position == cameraWideAngleRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Починати запис із ширококутної камери", "Начинать запись с широкоугольной камеры", "Start recording with ultra-wide lens"), ChatsConfig.startWithWideAngleCamera.Bool(), true);
                    } else if (position == cameraStabilizationRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Стабілізація відеоповідомлень", "Стабилизация видеосообщений", "Video note stabilization"), ChatsConfig.cameraStabilization.Bool(), true);
                    } else if (position == cameraFpsRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Підвищена частота кадрів (60 FPS)", "Повышенная частота кадров (60 FPS)", "High frame rate (60 FPS)"), ChatsConfig.extendedFramesPerSecond.Bool(), true);
                    } else if (position == cameraZoomRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Показувати слайдер зуму при записі", "Показывать слайдер зума при записи", "Show zoom slider during recording"), ChatsConfig.zoomSlider.Bool(), false);
                    } else if (position == noQuoteForwardRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Пересилати без автора за замовчуванням", "Пересылать без автора по умолчанию", "Forward without author quote by default"), NaConfig.INSTANCE.getShowNoQuoteForward().Bool(), true);
                    } else if (position == combineMessagesRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Об'єднувати послідовні повідомлення одного автора", "Объединять последовательные сообщения одного автора", "Group consecutive messages from same sender"), NaConfig.INSTANCE.getCombineMessage().Int() != 0, true);
                    } else if (position == showMessageIdRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Показувати ID повідомлення в меню дій", "Показывать ID сообщения в меню действий", "Show message ID in context menu"), NaConfig.INSTANCE.getShowMessageID().Bool(), true);
                    } else if (position == showOnlineStatusRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Показувати статус «В мережі» у заголовку чату", "Показывать статус «В сети» в заголовке чата", "Show 'Online' status in chat header"), NaConfig.INSTANCE.getShowOnlineStatus().Bool(), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == cameraTypeRow) {
                        int t = ChatsConfig.cameraType.Int();
                        String val = t == 2 ? "CameraX" : (t == 1 ? "Camera2" : MiogramLocale.get("Системна", "Системная", "System"));
                        cell.setTextAndValue(MiogramLocale.get("Рушій відеокамери", "Движок видеокамеры", "Video Camera Engine"), val, true);
                    } else if (position == doubleTapActionRow) {
                        int act = NaConfig.INSTANCE.getDoubleTapAction().Int();
                        String[] opts = new String[]{
                                MiogramLocale.get("Вимкнено", "Выключено", "Disabled"),
                                MiogramLocale.get("Відповідь", "Ответ", "Reply"),
                                MiogramLocale.get("Реакція", "Реакция", "Reaction"),
                                MiogramLocale.get("Копіювати", "Копировать", "Copy"),
                                MiogramLocale.get("В Обране", "В Избранное", "Save"),
                                MiogramLocale.get("Переклад", "Перевод", "Translate")
                        };
                        String val = (act >= 0 && act < opts.length) ? opts[act] : MiogramLocale.get("Відповідь", "Ответ", "Reply");
                        cell.setTextAndValue(MiogramLocale.get("Подвійний тап по повідомленню", "Двойной тап по сообщению", "Double Tap Action"), val, true);
                    } else if (position == stickerShapeRow) {
                        int s = ChatsConfig.stickerShape.Int();
                        String val = s == 2 ? MiogramLocale.get("Бульбашка", "Пузырь", "Bubble") : (s == 1 ? MiogramLocale.get("Скруглена", "Скругленная", "Rounded") : MiogramLocale.get("Оригінальна", "Оригинальная", "Original"));
                        cell.setTextAndValue(MiogramLocale.get("Форма стікерів", "Форма стикеров", "Sticker Shape"), val, true);
                    } else if (position == channelBottomButtonRow) {
                        int b = ChatsConfig.bottomButton.Int();
                        String val = b == 0 ? MiogramLocale.get("Приховати", "Скрыть", "Hide") : (b == 2 ? MiogramLocale.get("Коментарі", "Комментарии", "Comments") : "Mute");
                        cell.setTextAndValue(MiogramLocale.get("Нижня кнопка в каналах", "Нижняя кнопка в каналах", "Channel Bottom Button"), val, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == cameraInfoRow) {
                        cell.setText(MiogramLocale.get("Двигун CameraX значно покращує якість кружечків, додає стабілізацію та підтримку 60 FPS.",
                                "Движок CameraX значительно улучшает качество кружочков, добавляет стабилизацию и поддержку 60 FPS.",
                                "CameraX engine vastly improves video note clarity, adds hardware stabilization and 60 FPS capture."));
                    } else if (position == chatActionsInfoRow) {
                        cell.setText(MiogramLocale.get("Швидкі жести та спрощене пересилання повідомлень без вказання джерела.",
                                "Быстрые жесты и упрощенная пересылка сообщений без указания источника.",
                                "Quick gestures and author-hidden forwarding options."));
                    } else if (position == stickersInfoRow) {
                        cell.setText(MiogramLocale.get("Налаштування геометрії стікерів та нижньої панелі дій.",
                                "Настройка геометрии стикеров и нижней панели действий.",
                                "Customizes sticker corner styles and channel bottom action button."));
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }
}
