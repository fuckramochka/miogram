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
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Chats & Media Settings.
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
        return "Чати та Медіа";
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
            int cur = NaConfig.INSTANCE.getCombineMessage().Int();
            NaConfig.INSTANCE.getCombineMessage().setConfigInt(cur == 0 ? 1 : 0);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(cur == 0);
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
        String[] options = new String[]{"Системна камера", "Camera2", "CameraX (Висока якість)"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Двигун камери кружечків");
        b.setItems(options, (dialog, which) -> {
            ChatsConfig.cameraType.setConfigInt(which);
            listAdapter.notifyItemChanged(cameraTypeRow);
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void showDoubleTapDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        String[] options = new String[]{"Вимкнено", "Швидка відповідь", "Реакція", "Копіювати текст", "Зберегти в Обране", "Перекласти"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Дія на подвійний тап по повідомленню");
        b.setItems(options, (dialog, which) -> {
            NaConfig.INSTANCE.getDoubleTapAction().setConfigInt(which);
            listAdapter.notifyItemChanged(doubleTapActionRow);
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void showStickerShapeDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        String[] options = new String[]{"Оригінальна", "Скруглена", "Як бульбашка повідомлення"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Форма стікерів");
        b.setItems(options, (dialog, which) -> {
            ChatsConfig.stickerShape.setConfigInt(which);
            listAdapter.notifyItemChanged(stickerShapeRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void showChannelBottomButtonDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        String[] options = new String[]{"Приховати", "Вимкнути сповіщення (Mute)", "Коментарі (Discuss)"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Нижня кнопка в каналах");
        b.setItems(options, (dialog, which) -> {
            ChatsConfig.bottomButton.setConfigInt(which);
            listAdapter.notifyItemChanged(channelBottomButtonRow);
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
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
                        cell.setText("Камера та відеоповідомлення (Кружечки)");
                    } else if (position == headerChatActionsRow) {
                        cell.setText("Дії з повідомленнями та чатом");
                    } else if (position == headerStickersRow) {
                        cell.setText("Стікери та кнопки");
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == cameraMirrorRow) {
                        cell.setTextAndCheck("Дзеркальне відображення фронтальної камери", ChatsConfig.cameraMirrorMode.Bool(), true);
                    } else if (position == cameraWideAngleRow) {
                        cell.setTextAndCheck("Починати запис із ширококутної камери", ChatsConfig.startWithWideAngleCamera.Bool(), true);
                    } else if (position == cameraStabilizationRow) {
                        cell.setTextAndCheck("Стабілізація відеоповідомлень", ChatsConfig.cameraStabilization.Bool(), true);
                    } else if (position == cameraFpsRow) {
                        cell.setTextAndCheck("Підвищена частота кадрів (60 FPS)", ChatsConfig.extendedFramesPerSecond.Bool(), true);
                    } else if (position == cameraZoomRow) {
                        cell.setTextAndCheck("Показувати слайдер зуму при записі", ChatsConfig.zoomSlider.Bool(), false);
                    } else if (position == noQuoteForwardRow) {
                        cell.setTextAndCheck("Пересилати без автора за замовчуванням", NaConfig.INSTANCE.getShowNoQuoteForward().Bool(), true);
                    } else if (position == combineMessagesRow) {
                        cell.setTextAndCheck("Об'єднувати послідовні повідомлення одного автора", NaConfig.INSTANCE.getCombineMessage().Int() != 0, true);
                    } else if (position == showMessageIdRow) {
                        cell.setTextAndCheck("Показувати ID повідомлення в меню дій", NaConfig.INSTANCE.getShowMessageID().Bool(), true);
                    } else if (position == showOnlineStatusRow) {
                        cell.setTextAndCheck("Показувати статус «В мережі» у заголовку чату", NaConfig.INSTANCE.getShowOnlineStatus().Bool(), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == cameraTypeRow) {
                        int t = ChatsConfig.cameraType.Int();
                        String val = t == 2 ? "CameraX" : (t == 1 ? "Camera2" : "Системна");
                        cell.setTextAndValue("Рушій відеокамери", val, true);
                    } else if (position == doubleTapActionRow) {
                        int act = NaConfig.INSTANCE.getDoubleTapAction().Int();
                        String[] opts = new String[]{"Вимкнено", "Відповідь", "Реакція", "Копіювати", "В Обране", "Переклад"};
                        String val = (act >= 0 && act < opts.length) ? opts[act] : "Відповідь";
                        cell.setTextAndValue("Подвійний тап по повідомленню", val, true);
                    } else if (position == stickerShapeRow) {
                        int s = ChatsConfig.stickerShape.Int();
                        String val = s == 2 ? "Бульбашка" : (s == 1 ? "Скруглена" : "Оригінальна");
                        cell.setTextAndValue("Форма стікерів", val, true);
                    } else if (position == channelBottomButtonRow) {
                        int b = ChatsConfig.bottomButton.Int();
                        String val = b == 0 ? "Приховати" : (b == 2 ? "Коментарі" : "Mute");
                        cell.setTextAndValue("Нижня кнопка в каналах", val, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == cameraInfoRow) {
                        cell.setText("Двигун CameraX значно покращує якість кружечків, додає стабілізацію та підтримку 60 FPS.");
                    } else if (position == chatActionsInfoRow) {
                        cell.setText("Швидкі жести та спрощене пересилання повідомлень без вказання джерела.");
                    } else if (position == stickersInfoRow) {
                        cell.setText("Налаштування геометрії стікерів та нижньої панелі дій.");
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
