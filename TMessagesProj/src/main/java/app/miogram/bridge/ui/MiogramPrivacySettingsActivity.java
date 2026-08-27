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
import org.telegram.ui.Components.RecyclerListView;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Privacy & Security Settings:
 * - Zero-Knowledge Vault & Duress PIN
 * - Ghost Mode (Read stealth, Hide online, Hide typing)
 * - Message History (Save deleted messages, media saving)
 * - Privacy enhancements (Hide phone number, Allow screenshots)
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
            boolean v = !NaConfig.INSTANCE.getSendReadMessage().Bool();
            NaConfig.INSTANCE.getSendReadMessage().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!v);
        } else if (position == ghostOnlineRow) {
            boolean v = !NaConfig.INSTANCE.getSendOnlinePackets().Bool();
            NaConfig.INSTANCE.getSendOnlinePackets().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!v);
        } else if (position == ghostTypingRow) {
            boolean v = !NaConfig.INSTANCE.getSendUploadProgress().Bool();
            NaConfig.INSTANCE.getSendUploadProgress().setConfigBool(v);
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
            boolean v = !NaConfig.INSTANCE.getHidePhone().Bool();
            NaConfig.INSTANCE.getHidePhone().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == allowScreenshotRow) {
            boolean v = !NaConfig.INSTANCE.getAllowScreenshot().Bool();
            NaConfig.INSTANCE.getAllowScreenshot().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        }
    }

    private class ListAdapter extends BaseListAdapter {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_CHECK = 1;
        private static final int TYPE_SETTINGS = 2;
        private static final int TYPE_INFO = 3;

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
                return TYPE_INFO;
            }
            return TYPE_SETTINGS;
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
                        // sendReadMessage == false means ghost reading is ON
                        cell.setTextAndCheck("Не надсилати звіт про прочитання", !NaConfig.INSTANCE.getSendReadMessage().Bool(), true);
                    } else if (position == ghostOnlineRow) {
                        cell.setTextAndCheck("Приховувати статус «В мережі»", !NaConfig.INSTANCE.getSendOnlinePackets().Bool(), true);
                    } else if (position == ghostTypingRow) {
                        cell.setTextAndCheck("Приховувати статус «Друкує / записує»", !NaConfig.INSTANCE.getSendUploadProgress().Bool(), false);
                    } else if (position == saveDeletedMessagesRow) {
                        cell.setTextAndCheck("Зберігати видалені та відредаговані повідомлення", NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool(), true);
                    } else if (position == saveDeletedMediaRow) {
                        cell.setTextAndCheck("Зберігати медіафайли видалених повідомлень", NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool(), false);
                    } else if (position == hidePhoneRow) {
                        cell.setTextAndCheck("Приховати номер телефону в меню та налаштуваннях", NaConfig.INSTANCE.getHidePhone().Bool(), true);
                    } else if (position == allowScreenshotRow) {
                        cell.setTextAndCheck("Дозволити знімки екрана в секретних чатах", NaConfig.INSTANCE.getAllowScreenshot().Bool(), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == vaultManageRow) {
                        cell.setTextAndIcon("Налаштування сховку та PIN під примусом", R.drawable.msg_fave, false);
                    }
                    break;
                }
                case TYPE_INFO: {
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

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_HEADER: view = new HeaderCell(mContext); break;
                case TYPE_CHECK: view = new TextCheckCell(mContext); break;
                case TYPE_SETTINGS: view = new TextCell(mContext); break;
                case TYPE_INFO: default: view = new TextInfoPrivacyCell(mContext); break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }
}
