package app.miogram.bridge.vault;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.UsersSelectActivity;

import java.util.ArrayList;
import java.util.Set;

import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoPasscodeSettingsActivity;

/**
 * Beautiful, user-friendly UI for Double Bottom (Подвійне сховище).
 * Allows regular users to easily:
 *  - Set Real Passcode
 *  - Set Emergency (Duress) Passcode
 *  - Select Decoy Account
 *  - Select Whitelisted/Allowed Chats with native Telegram picker
 *  - Configure Panic Logout
 */
public class MiogramDoubleBottomActivity extends BaseNekoSettingsActivity {

    private int headerStatusRow;
    private int statusRow;
    private int statusInfoRow;

    private int headerCodesRow;
    private int realPinRow;
    private int duressPinRow;
    private int codesInfoRow;

    private int headerAccountRow;
    private int decoyAccountRow;
    private int accountInfoRow;

    private int headerChatsRow;
    private int allowedChatsRow;
    private int chatsInfoRow;

    private int headerPanicRow;
    private int panicLogoutRow;
    private int panicInfoRow;

    private int headerAdvanceRow;
    private int legacyAccountsRow;
    private int clearRow;
    private int advanceInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Подвійне сховище", "Двойное дно", "Double Bottom");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerStatusRow = addRow();
        statusRow = addRow();
        statusInfoRow = addRow();

        headerCodesRow = addRow();
        realPinRow = addRow();
        duressPinRow = addRow();
        codesInfoRow = addRow();

        headerAccountRow = addRow();
        decoyAccountRow = addRow();
        accountInfoRow = addRow();

        headerChatsRow = addRow();
        allowedChatsRow = addRow();
        chatsInfoRow = addRow();

        headerPanicRow = addRow();
        panicLogoutRow = addRow();
        panicInfoRow = addRow();

        headerAdvanceRow = addRow();
        legacyAccountsRow = addRow();
        clearRow = addRow();
        advanceInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == realPinRow) {
            showPinDialog(true);
        } else if (position == duressPinRow) {
            showPinDialog(false);
        } else if (position == decoyAccountRow) {
            showAccountSelectDialog();
        } else if (position == allowedChatsRow) {
            openChatsSelector();
        } else if (position == panicLogoutRow) {
            boolean next = !MiogramDoubleBottomManager.isPanicLogoutEnabled();
            MiogramDoubleBottomManager.setPanicLogoutEnabled(next);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
        } else if (position == legacyAccountsRow) {
            presentFragment(new NekoPasscodeSettingsActivity());
        } else if (position == clearRow) {
            showResetDialog();
        }
    }

    private void showPinDialog(boolean isReal) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(isReal ?
                MiogramLocale.get("Основний PIN (Повний доступ)", "Основной PIN (Полный доступ)", "Real Passcode (Full access)") :
                MiogramLocale.get("Аварійний PIN (Тривожний режим)", "Аварийный PIN (Тревожный режим)", "Emergency Passcode (Duress)"));

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        TextView hintView = new TextView(getParentActivity());
        hintView.setTextSize(14);
        hintView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hintView.setText(isReal ?
                MiogramLocale.get("Введіть PIN для повного доступу до всіх чатів та акаунтів (мін. 4 цифри):", "Введите PIN для полного доступа ко всем чатам и аккаунтам (мин. 4 цифры):", "Enter PIN for full access to all chats and accounts (min 4 digits):") :
                MiogramLocale.get("Введіть PIN під примусом. Він відкриє лише дозволені чати або аварійний акаунт:", "Введите PIN под принуждением. Он откроет только разрешенные чаты или аварийный аккаунт:", "Enter Duress PIN. It will reveal only allowed chats or decoy account:"));
        layout.addView(hintView);

        EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setTextSize(20);
        editText.setGravity(Gravity.CENTER);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        String current = isReal ? MiogramDoubleBottomManager.getRealPasscode() : MiogramDoubleBottomManager.getDuressPasscode();
        if (!TextUtils.isEmpty(current)) {
            editText.setText(current);
            editText.setSelection(current.length());
        }
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 8));

        builder.setView(layout);
        builder.setPositiveButton(MiogramLocale.get("Зберегти", "Сохранить", "Save"), (dialog, which) -> {
            String pin = editText.getText().toString().trim();
            if (pin.length() < 4) {
                return;
            }
            if (isReal) {
                MiogramDoubleBottomManager.setRealPasscode(pin);
            } else {
                MiogramDoubleBottomManager.setDuressPasscode(pin);
            }
            updateRows();
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);

        if (!TextUtils.isEmpty(current)) {
            builder.setNeutralButton(MiogramLocale.get("Видалити", "Удалить", "Delete"), (dialog, which) -> {
                if (isReal) {
                    MiogramDoubleBottomManager.setRealPasscode("");
                } else {
                    MiogramDoubleBottomManager.setDuressPasscode("");
                }
                updateRows();
                listAdapter.notifyDataSetChanged();
            });
        }

        showDialog(builder.create());
    }

    private void showAccountSelectDialog() {
        if (getParentActivity() == null) return;
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();

        int currentDecoy = MiogramDoubleBottomManager.getDecoyAccount();
        names.add((currentDecoy == -1 ? "✓ " : "   ") + MiogramLocale.get("Залишати поточний акаунт", "Оставлять текущий аккаунт", "Keep current account"));
        indices.add(-1);

        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                TLRPC.User u = UserConfig.getInstance(i).getCurrentUser();
                String name = UserObject.getUserName(u);
                String label = MiogramLocale.get("Акаунт ", "Аккаунт ", "Account ") + (i + 1) + (name != null ? " (" + name + ")" : "");
                names.add((currentDecoy == i ? "✓ " : "   ") + label);
                indices.add(i);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Вибір аварійного акаунта", "Выбор аварийного аккаунта", "Select Emergency Account"));
        builder.setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
            MiogramDoubleBottomManager.setDecoyAccount(indices.get(which));
            updateRows();
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        showDialog(builder.create());
    }

    private void openChatsSelector() {
        int targetAccount = MiogramDoubleBottomManager.getDecoyAccount() >= 0 ? MiogramDoubleBottomManager.getDecoyAccount() : currentAccount;
        Set<Long> allowed = MiogramDoubleBottomManager.getAllowedDialogIds(targetAccount);
        ArrayList<Long> initialList = new ArrayList<>(allowed);

        UsersSelectActivity activity = new UsersSelectActivity(true, initialList, 0);
        activity.setDelegate((didSelect, flags) -> {
            MiogramDoubleBottomManager.setAllowedDialogIds(targetAccount, didSelect);
            updateRows();
            listAdapter.notifyDataSetChanged();
        });
        presentFragment(activity);
    }

    private void showResetDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Скинути налаштування?", "Сбросить настройки?", "Reset Double Bottom?"));
        builder.setMessage(MiogramLocale.get("Це видалить аварійні коди та списки дозволених чатів. Усі акаунти залишаться на пристрої.", "Это удалит аварийные коды и списки разрешенных чатов. Все аккаунты останутся на устройстве.", "This will clear emergency codes and allowed chats lists. All accounts remain on device."));
        builder.setPositiveButton(MiogramLocale.get("Скинути", "Сбросить", "Reset"), (dialog, which) -> {
            MiogramDoubleBottomManager.clearAll();
            updateRows();
            listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        TextView btn = (TextView) dialog.getButton(Dialog.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerStatusRow || position == headerCodesRow || position == headerAccountRow || position == headerChatsRow || position == headerPanicRow || position == headerAdvanceRow) {
                return TYPE_HEADER;
            } else if (position == statusInfoRow || position == codesInfoRow || position == accountInfoRow || position == chatsInfoRow || position == panicInfoRow || position == advanceInfoRow) {
                return TYPE_INFO_PRIVACY;
            } else if (position == panicLogoutRow) {
                return TYPE_CHECK;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            int targetAccount = MiogramDoubleBottomManager.getDecoyAccount() >= 0 ? MiogramDoubleBottomManager.getDecoyAccount() : currentAccount;
            int allowedCount = MiogramDoubleBottomManager.getAllowedDialogIds(targetAccount).size();
            boolean hasReal = !TextUtils.isEmpty(MiogramDoubleBottomManager.getRealPasscode());
            boolean hasDuress = !TextUtils.isEmpty(MiogramDoubleBottomManager.getDuressPasscode());

            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerStatusRow) {
                        cell.setText(MiogramLocale.get("СТАН СХОВИЩА", "СОСТОЯНИЕ СЕЙФА", "VAULT STATUS"));
                    } else if (position == headerCodesRow) {
                        cell.setText(MiogramLocale.get("КОДИ ДОСТУПУ (PIN)", "КОДЫ ДОСТУПА (PIN)", "ACCESS CODES (PIN)"));
                    } else if (position == headerAccountRow) {
                        cell.setText(MiogramLocale.get("АВАРІЙНИЙ АКАУНТ", "АВАРИЙНЫЙ АККАУНТ", "EMERGENCY ACCOUNT"));
                    } else if (position == headerChatsRow) {
                        cell.setText(MiogramLocale.get("ДОЗВОЛЕНІ ЧАТИ В ТРИВОЖНОМУ РЕЖИМІ", "РАЗРЕШЕННЫЕ ЧАТЫ В ТРЕВОЖНОМ РЕЖИМЕ", "WHITELISTED CHATS IN DURESS"));
                    } else if (position == headerPanicRow) {
                        cell.setText(MiogramLocale.get("ЕКСТРЕНІ ЗАХОДИ", "ЭКСТРЕННЫЕ МЕРЫ", "PANIC ACTIONS"));
                    } else if (position == headerAdvanceRow) {
                        cell.setText(MiogramLocale.get("ДОДАТКОВО", "ДОПОЛНИТЕЛЬНО", "ADVANCED"));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == statusRow) {
                        boolean active = MiogramDoubleBottomManager.isConfigured();
                        cell.setTextAndValue(
                                MiogramLocale.get("Подвійне сховище", "Двойное дно", "Double Bottom"),
                                active ? MiogramLocale.get("Налаштовано і захищено", "Настроено и защищено", "Active & Protected") :
                                         MiogramLocale.get("Потрібно задати коди", "Требуется настроить коды", "Setup required"),
                                false
                        );
                    } else if (position == realPinRow) {
                        cell.setTextAndValue(
                                MiogramLocale.get("Основний PIN (Повний доступ)", "Основной PIN (Полный доступ)", "Real PIN (Full access)"),
                                hasReal ? MiogramLocale.get("Встановлено", "Установлен", "Configured") :
                                          MiogramLocale.get("Не встановлено", "Не установлен", "Not set"),
                                true
                        );
                    } else if (position == duressPinRow) {
                        cell.setTextAndValue(
                                MiogramLocale.get("Аварійний PIN (Тривожний режим)", "Аварийный PIN (Тревожный режим)", "Emergency PIN (Duress)"),
                                hasDuress ? MiogramLocale.get("Встановлено", "Установлен", "Configured") :
                                            MiogramLocale.get("Не встановлено", "Не установлен", "Not set"),
                                false
                        );
                    } else if (position == decoyAccountRow) {
                        int decoy = MiogramDoubleBottomManager.getDecoyAccount();
                        String val = MiogramLocale.get("Залишати поточний", "Оставлять текущий", "Keep current");
                        if (decoy >= 0 && UserConfig.getInstance(decoy).isClientActivated()) {
                            TLRPC.User u = UserConfig.getInstance(decoy).getCurrentUser();
                            String name = UserObject.getUserName(u);
                            val = MiogramLocale.get("Акаунт ", "Аккаунт ", "Account ") + (decoy + 1) + (name != null ? " (" + name + ")" : "");
                        }
                        cell.setTextAndValue(MiogramLocale.get("Аварійний акаунт", "Аварийный аккаунт", "Decoy account"), val, false);
                    } else if (position == allowedChatsRow) {
                        String val = allowedCount > 0 ? (allowedCount + " " + MiogramLocale.get("чатів", "чатов", "chats")) :
                                                        MiogramLocale.get("Усі чати (без фільтра)", "Все чаты (без фильтра)", "All chats (unfiltered)");
                        cell.setTextAndValue(MiogramLocale.get("Вибрати дозволені чати", "Выбрать разрешенные чаты", "Select allowed chats"), val, false);
                    } else if (position == legacyAccountsRow) {
                        cell.setText(MiogramLocale.get("Окремі паролі для кожного акаунта", "Раздельные пароли для каждого аккаунта", "Independent passcodes per account"), true);
                    } else if (position == clearRow) {
                        cell.setText(MiogramLocale.get("Скинути налаштування сховища", "Сбросить настройки сейфа", "Reset Double Bottom"), false);
                        cell.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == panicLogoutRow) {
                        cell.setTextAndCheck(
                                MiogramLocale.get("Вийти з основних акаунтів при тривозі", "Выйти из основных аккаунтов при тревоге", "Panic logout other accounts"),
                                MiogramDoubleBottomManager.isPanicLogoutEnabled(),
                                false
                        );
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == statusInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "Подвійне сховище дозволяє відкривати різні профілі або приховувати секретні чати залежно від того, який PIN введено на екрані блокування.",
                                "Двойное дно позволяет открывать разные профили или скрывать секретные чаты в зависимости от того, какой PIN введен на экране блокировки.",
                                "Double Bottom reveals different profiles or filters sensitive chats based on which PIN is entered on the lockscreen."
                        ));
                    } else if (position == codesInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "Основний PIN відкриває повний доступ до всіх листувань. Аварійний PIN активує тривожний режим, показуючи лише дозволені чати.",
                                "Основной PIN открывает полный доступ ко всем перепискам. Аварийный PIN активирует тревожный режим, отображая только разрешенные чаты.",
                                "Real PIN grants unrestricted access to all chats. Emergency PIN unlocks into duress mode, showing only whitelisted chats."
                        ));
                    } else if (position == accountInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "Оберіть, який акаунт показувати під час перевірки. Додаток непомітно перемкнеться на нього при вводі аварійного коду.",
                                "Выберите, какой аккаунт показывать при проверке. Приложение незаметно переключится на него при вводе аварийного кода.",
                                "Select which account to display under duress. The app silently switches to it when emergency PIN is entered."
                        ));
                    } else if (position == chatsInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "Усі чати, які НЕ позначені у списку дозволених, будуть повністю невидимі у списку діалогів та глобальному пошуку при вході за аварійним PIN.",
                                "Все чаты, которые НЕ отмечены в списке разрешенных, будут полностью скрыты из списка диалогов и поиска при входе по аварийному коду.",
                                "Any chat NOT marked in the whitelist will be completely invisible from the dialog list and search when unlocked via emergency PIN."
                        ));
                    } else if (position == panicInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "При розблокуванні аварійним PIN додаток миттєво завершить сесії на сервері та видалить конфіденційні акаунти з пристрою.",
                                "При разблокировке аварийным PIN приложение мгновенно завершит сессии на сервере и удалит конфиденциальные аккаунты с устройства.",
                                "When unlocked via emergency PIN, all other accounts will immediately be logged out and wiped."
                        ));
                    } else if (position == advanceInfoRow) {
                        cell.setText(MiogramLocale.get(
                                "Усі налаштування зберігаються локально на пристрої та не передаються третім особам.",
                                "Все настройки сохраняются локально на устройстве и не передаются третьим лицам.",
                                "All settings are stored locally on your device."
                        ));
                    }
                    break;
                }
            }
        }
    }
}
