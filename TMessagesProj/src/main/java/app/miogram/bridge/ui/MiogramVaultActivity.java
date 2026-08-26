package app.miogram.bridge.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.PasscodeActivity;
import org.telegram.ui.TopicsFragment;

import java.util.HashSet;
import java.util.Set;

import app.miogram.bridge.passcode.MiogramDuressConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Native Telegram-style screen for configuring the Zero-Knowledge Vault and Duress Profile.
 */
public class MiogramVaultActivity extends BaseNekoSettingsActivity implements DialogsActivity.DialogsActivityDelegate {

    private int headerRealRow;
    private int realPinRow;
    private int headerDuressRow;
    private int duressPinRow;
    private int decoyChatsRow;
    private int headerActionsRow;
    private int passcodeSettingsRow;
    private int lockNowRow;
    private int wipeVaultRow;
    private int infoRow;

    private boolean wipeArmed = false;

    @Override
    protected String getActionBarTitle() {
        return "Miogram Vault";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerRealRow = addRow();
        realPinRow = addRow();

        headerDuressRow = addRow();
        duressPinRow = addRow();
        decoyChatsRow = addRow();

        headerActionsRow = addRow();
        passcodeSettingsRow = addRow();
        lockNowRow = addRow();
        wipeVaultRow = addRow();

        infoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == realPinRow) {
            showPinDialog(false);
        } else if (position == duressPinRow) {
            showPinDialog(true);
        } else if (position == decoyChatsRow) {
            openDecoyChatsPicker();
        } else if (position == passcodeSettingsRow) {
            presentFragment(new PasscodeActivity(SharedConfig.passcodeHash.length() > 0 ? 2 : 0));
        } else if (position == lockNowRow) {
            MiogramDuressConfig.setDuressActive(false);
            if (SharedConfig.passcodeHash.length() > 0) {
                SharedConfig.appLocked = true;
            }
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Сховище заблоковано. Ключі стерті з пам'яті.").show();
            }
        } else if (position == wipeVaultRow) {
            if (!wipeArmed) {
                wipeArmed = true;
                if (getParentActivity() != null) {
                    Toast.makeText(getParentActivity(), "Натисніть ще раз для підтвердження знищення", Toast.LENGTH_SHORT).show();
                }
            } else {
                wipeArmed = false;
                MiogramDuressConfig.setRealPin("");
                MiogramDuressConfig.setDuressPin("");
                MiogramDuressConfig.setDecoyDialogIds(new HashSet<>());
                MiogramDuressConfig.setDuressActive(false);
                if (getParentActivity() != null) {
                    Toast.makeText(getParentActivity(), "Сховище та налаштування повністю знищено", Toast.LENGTH_SHORT).show();
                }
                finishFragment();
            }
        }
    }

    private void showPinDialog(boolean isDuress) {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(isDuress ? "Встановлення тривожного PIN" : "Встановлення реального PIN");

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Введіть PIN (мін. 4 цифри)");
        input.setTextSize(16);

        EditTextBoldCursor confirm = new EditTextBoldCursor(ctx);
        confirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirm.setHint("Підтвердіть PIN");
        confirm.setTextSize(16);

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);
        container.addView(input);
        container.addView(confirm);

        builder.setView(container);
        builder.setPositiveButton("Зберегти", (d, w) -> {
            String p1 = input.getText().toString().trim();
            String p2 = confirm.getText().toString().trim();
            if (p1.length() < 4) {
                Toast.makeText(ctx, "PIN має бути не менше 4 цифр", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p1.equals(p2)) {
                Toast.makeText(ctx, "Введені PIN-коди не збігаються", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isDuress) {
                MiogramDuressConfig.setDuressPin(p1);
                Toast.makeText(ctx, "Тривожний PIN збережено", Toast.LENGTH_SHORT).show();
            } else {
                MiogramDuressConfig.setRealPin(p1);
                if (SharedConfig.passcodeHash.isEmpty()) {
                    SharedConfig.setPasscode(p1);
                }
                Toast.makeText(ctx, "Реальний PIN збережено", Toast.LENGTH_SHORT).show();
            }
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openDecoyChatsPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("resetDelegate", false);
        args.putInt("dialogsType", 7);

        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate(this);
        presentFragment(fragment);
    }

    @Override
    public boolean didSelectDialogs(DialogsActivity fragment, java.util.ArrayList<MessagesStorage.TopicKey> dids, CharSequence message, boolean param, boolean notify, int scheduleDate, int scheduleRepeatPeriod, TopicsFragment topicsFragment) {
        if (dids != null) {
            Set<Long> set = new HashSet<>();
            for (MessagesStorage.TopicKey key : dids) {
                if (key != null) {
                    set.add(key.dialogId);
                }
            }
            MiogramDuressConfig.setDecoyDialogIds(set);
            if (getParentActivity() != null) {
                Toast.makeText(getParentActivity(), "Збережено дозволених чатів: " + set.size(), Toast.LENGTH_SHORT).show();
            }
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
        fragment.finishFragment();
        return true;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerRealRow || position == headerDuressRow || position == headerActionsRow) return 4;
            if (position == infoRow) return 7;
            return 2;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (getItemViewType(position)) {
                case 4 -> {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerRealRow) cell.setText("СПРАВЖНІЙ ПРОФІЛЬ");
                    else if (position == headerDuressRow) cell.setText("ТРИВОЖНИЙ ПРОФІЛЬ (DURESS / DECOY)");
                    else if (position == headerActionsRow) cell.setText("КЕРУВАННЯ СХОВИЩЕМ");
                }
                case 7 -> {
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText("При введенні тривожного PIN-коду на екрані блокування Telegram, додаток відкриває аварійний безпечний простір, відображаючи лише вибрані нейтральні чати та повністю приховуючи конфіденційні переписки.");
                }
                default -> {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == realPinRow) {
                        String status = MiogramDuressConfig.hasRealPin() ? "Встановлено ✓" : "Не встановлено";
                        cell.setText("Реальний PIN-код · " + status, true);
                    } else if (position == duressPinRow) {
                        String status = MiogramDuressConfig.hasDuressPin() ? "Встановлено ✓" : "Не встановлено";
                        cell.setText("Тривожний PIN-код · " + status, true);
                    } else if (position == decoyChatsRow) {
                        int count = MiogramDuressConfig.getDecoyDialogIds().size();
                        String countText = count > 0 ? "Вибрано: " + count : "Не вибрано (показувати всі)";
                        cell.setText("Чати тривожного режиму · " + countText, false);
                    } else if (position == passcodeSettingsRow) {
                        cell.setText("Налаштування блокування Telegram", true);
                    } else if (position == lockNowRow) {
                        cell.setText("Заблокувати додаток зараз", true);
                    } else if (position == wipeVaultRow) {
                        cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
                        cell.setText("Знищити сховище", false);
                    }
                }
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }
}
