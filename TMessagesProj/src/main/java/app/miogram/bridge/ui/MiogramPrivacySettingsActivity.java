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

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Privacy & Security Settings.
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
        return "Конфіденційність та Захист";
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
            presentFragment(new MiogramVaultActivity());
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
            boolean v = !NekoConfig.allowScreenshot.Bool();
            NekoConfig.allowScreenshot.setConfigBool(v);
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
                        cell.setText("Секретний сховок (Zero-Knowledge Vault)");
                    } else if (position == headerGhostRow) {
                        cell.setText("Режим невидимки (Ghost Mode)");
                    } else if (position == headerHistoryRow) {
                        cell.setText("Історія та збереження повідомлень");
                    } else if (position == headerGeneralPrivacyRow) {
                        cell.setText("Загальна конфіденційність");
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == ghostReadRow) {
                        cell.setTextAndCheck("Не надсилати звіт про прочитання", !NekoConfig.sendReadMessagePackets.Bool(), true);
                    } else if (position == ghostOnlineRow) {
                        cell.setTextAndCheck("Приховувати статус «В мережі»", !NekoConfig.sendOnlinePackets.Bool(), true);
                    } else if (position == ghostTypingRow) {
                        cell.setTextAndCheck("Приховувати статус «Друкує / записує»", !NekoConfig.sendUploadProgress.Bool(), false);
                    } else if (position == saveDeletedMessagesRow) {
                        cell.setTextAndCheck("Зберігати видалені та відредаговані повідомлення", NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool(), true);
                    } else if (position == saveDeletedMediaRow) {
                        cell.setTextAndCheck("Зберігати медіафайли видалених повідомлень", NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool(), false);
                    } else if (position == hidePhoneRow) {
                        cell.setTextAndCheck("Приховати номер телефону в меню та налаштуваннях", NekoConfig.hidePhone.Bool(), true);
                    } else if (position == allowScreenshotRow) {
                        cell.setTextAndCheck("Дозволити знімки екрана в секретних чатах", NekoConfig.allowScreenshot.Bool(), false);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == vaultManageRow) {
                        cell.setTextAndIcon("Налаштування сховку та PIN під примусом", R.drawable.msg_fave, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == vaultInfoRow) {
                        cell.setText("Криптографічний захист чатів: при введенні фейкового PIN-коду під примусом відображаються лише безпечні чати.");
                    } else if (position == ghostInfoRow) {
                        cell.setText("Дозволяє читати повідомлення непомітно для співрозмовника.");
                    } else if (position == historyInfoRow) {
                        cell.setText("Зберігає копію оригінального тексту навіть після того, як співрозмовник його видалив або змінив.");
                    } else if (position == generalPrivacyInfoRow) {
                        cell.setText("Додатковий захист персональних даних та екрану.");
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
