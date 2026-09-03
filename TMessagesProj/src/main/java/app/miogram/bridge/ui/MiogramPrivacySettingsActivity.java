package app.miogram.bridge.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Privacy & Security Settings with dynamic multilingual localization.
 */
public class MiogramPrivacySettingsActivity extends BaseNekoSettingsActivity {

    private int headerVaultRow;
    private int vaultManageRow;
    private int vaultInfoRow;

    private int headerGhostRow;
    private int ghostReadRow;
    private int ghostOnlineRow;
    private int ghostTypingRow;
    private int ghostInfoRow;

    private int headerHistoryRow;
    private int saveDeletedMessagesRow;
    private int saveDeletedMediaRow;
    private int historyInfoRow;

    private int headerGeneralPrivacyRow;
    private int hidePhoneRow;
    private int allowScreenshotRow;
    private int generalPrivacyInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Конфіденційність та Захист", "Конфиденциальность и Защита", "Privacy & Security");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerVaultRow = addRow();
        vaultManageRow = addRow();
        vaultInfoRow = addRow();

        headerGhostRow = addRow();
        ghostReadRow = addRow();
        ghostOnlineRow = addRow();
        ghostTypingRow = addRow();
        ghostInfoRow = addRow();

        headerHistoryRow = addRow();
        saveDeletedMessagesRow = addRow();
        saveDeletedMediaRow = addRow();
        historyInfoRow = addRow();

        headerGeneralPrivacyRow = addRow();
        hidePhoneRow = addRow();
        allowScreenshotRow = addRow();
        generalPrivacyInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == vaultManageRow) {
            presentFragment(new app.miogram.bridge.vault.MiogramDoubleBottomActivity());
        } else if (position == ghostReadRow) {
            boolean v = !NekoConfig.sendReadMessagePackets.Bool();
            NekoConfig.sendReadMessagePackets.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!v);
        } else if (position == ghostOnlineRow) {
            boolean v = !NekoConfig.sendOnlinePackets.Bool();
            NekoConfig.sendOnlinePackets.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!v);
        } else if (position == ghostTypingRow) {
            boolean v = !NekoConfig.sendUploadProgress.Bool();
            NekoConfig.sendUploadProgress.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!v);
        } else if (position == saveDeletedMessagesRow) {
            boolean v = !NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool();
            NaConfig.INSTANCE.getEnableSaveDeletedMessages().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
            listAdapter.notifyItemChanged(saveDeletedMediaRow);
        } else if (position == saveDeletedMediaRow) {
            boolean v = !NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool();
            NaConfig.INSTANCE.getMessageSavingSaveMedia().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == hidePhoneRow) {
            boolean v = !NekoConfig.hidePhone.Bool();
            NekoConfig.hidePhone.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == allowScreenshotRow) {
            boolean v = !NekoConfig.ignoreContentRestrictions.Bool();
            NekoConfig.ignoreContentRestrictions.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerVaultRow || position == headerGhostRow
                    || position == headerHistoryRow || position == headerGeneralPrivacyRow) {
                return TYPE_HEADER;
            } else if (position == ghostReadRow || position == ghostOnlineRow || position == ghostTypingRow
                    || position == saveDeletedMessagesRow || position == saveDeletedMediaRow
                    || position == hidePhoneRow || position == allowScreenshotRow) {
                return TYPE_CHECK;
            } else if (position == vaultInfoRow || position == ghostInfoRow
                    || position == historyInfoRow || position == generalPrivacyInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerVaultRow) {
                        cell.setText(MiogramLocale.get("Подвійне сховище (Double Bottom)", "Двойное дно (Double Bottom)", "Double Bottom (Hidden Accounts)"));
                    } else if (position == headerGhostRow) {
                        cell.setText(MiogramLocale.get("Режим невидимки (Ghost Mode)", "Режим невидимки (Ghost Mode)", "Ghost Mode"));
                    } else if (position == headerHistoryRow) {
                        cell.setText(MiogramLocale.get("Історія та збереження повідомлень", "История и сохранение сообщений", "Message History & Anti-Delete"));
                    } else if (position == headerGeneralPrivacyRow) {
                        cell.setText(MiogramLocale.get("Загальна конфіденційність", "Общая конфиденциальность", "General Privacy"));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == ghostReadRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Не надсилати звіт про прочитання", "Не отправлять отчет о прочтении", "Don't send read receipts"), !NekoConfig.sendReadMessagePackets.Bool(), true);
                    } else if (position == ghostOnlineRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Приховувати статус «В мережі»", "Скрывать статус «В сети»", "Hide 'Online' status"), !NekoConfig.sendOnlinePackets.Bool(), true);
                    } else if (position == ghostTypingRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Приховувати статус «Друкує / записує»", "Скрывать статус «Печатает / записывает»", "Hide 'Typing / recording' action"), !NekoConfig.sendUploadProgress.Bool(), false);
                    } else if (position == saveDeletedMessagesRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Зберігати видалені та відредаговані повідомлення", "Сохранять удаленные и отредактированные сообщения", "Save deleted and edited messages"), NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool(), true);
                    } else if (position == saveDeletedMediaRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Зберігати медіафайли видалених повідомлень", "Сохранять медиафайлы удаленных сообщений", "Save media from deleted messages"), NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool(), false);
                    } else if (position == hidePhoneRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Приховати номер телефону в меню та налаштуваннях", "Скрыть номер телефона в меню и настройках", "Hide phone number in menu & settings"), NekoConfig.hidePhone.Bool(), true);
                    } else if (position == allowScreenshotRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Дозволити знімки екрана та копіювання в обмежених чатах", "Разрешить снимки экрана и копирование в ограниченных чатах", "Allow screenshots in protected content chats"), NekoConfig.ignoreContentRestrictions.Bool(), false);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == vaultManageRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Налаштування прихованих акаунтів та кодів", "Настройки скрытых аккаунтов и кодов", "Hidden accounts & Passcodes setup"), R.drawable.msg_permissions, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == vaultInfoRow) {
                        cell.setText(MiogramLocale.get("Встановіть різні PIN-коди для кожного акаунта. Прихований акаунт не відображатиметься в інтерфейсі та відкриватиметься лише при введенні його секретного PIN-коду на екрані блокування. Екстрений PIN миттєво видаляє сесії схованих акаунтів.",
                                "Установите разные PIN-коды для каждого аккаунта. Скрытый аккаунт не отображается в интерфейсе и открывается только при вводе его секретного PIN-кода на экране блокировки. Экстренный PIN мгновенно завершает сессии защищённых аккаунтов.",
                                "Configure separate passcodes for each account. Hidden accounts are completely invisible and unlocked strictly via their secret passcode on the lockscreen. Panic code immediately terminates sessions of protected accounts."));
                    } else if (position == ghostInfoRow) {
                        cell.setText(MiogramLocale.get("Дозволяє читати повідомлення непомітно для співрозмовника.",
                                "Позволяет читать сообщения незаметно для собеседника.",
                                "Enables stealth message viewing without notifying the other party."));
                    } else if (position == historyInfoRow) {
                        cell.setText(MiogramLocale.get("Зберігає копію оригінального тексту навіть після того, як співрозмовник його видалив або змінив.",
                                "Сохраняет копию оригинального текста даже после того, как собеседник его удалил или изменил.",
                                "Preserves original messages and media even if edited or retracted."));
                    } else if (position == generalPrivacyInfoRow) {
                        cell.setText(MiogramLocale.get("Додатковий захист персональних даних та екрану.",
                                "Дополнительная защита персональных данных и экрана.",
                                "Additional protection for personal identity and screen captures."));
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
