package app.miogram.bridge.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * 100% Native Hardcoded Custom Profile Settings for Miogram:
 * Replaces all Python plugin shims with high-performance native Android UI.
 */
public class MiogramCustomProfileActivity extends BaseNekoSettingsActivity {

    private int headerProfileRow;
    private int editorRow;
    private int bubbleRow;
    private int langRow;
    private int profileInfoRow;

    private int headerBatteryRow;
    private int chatDecorRow;
    private int chatBubblesRow;
    private int bakeOutsideRow;
    private int bakeProfileRow;
    private int batteryInfoRow;

    private int headerAboutRow;
    private int thanksRow;
    private int channelRow;
    private int aboutInfoRow;

    private int headerDevRow;
    private int pingRow;
    private int logRow;
    private int eggRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Оформлення профілю", "Оформление профиля", "Custom Profile");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerProfileRow = addRow();
        editorRow = addRow();
        bubbleRow = addRow();
        langRow = addRow();
        profileInfoRow = addRow();

        headerBatteryRow = addRow();
        chatDecorRow = addRow();
        chatBubblesRow = addRow();
        bakeOutsideRow = addRow();
        bakeProfileRow = addRow();
        batteryInfoRow = addRow();

        headerAboutRow = addRow();
        thanksRow = addRow();
        channelRow = addRow();
        aboutInfoRow = addRow();

        if (CustomProfileEngine.isDevMode()) {
            headerDevRow = addRow();
            pingRow = addRow();
            logRow = addRow();
            eggRow = addRow();
        } else {
            headerDevRow = -1;
            pingRow = -1;
            logRow = -1;
            eggRow = -1;
        }
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == editorRow || position == bubbleRow) {
            presentFragment(new MiogramCustomProfileEditorActivity());
        } else if (position == langRow) {
            showLanguageDialog();
        } else if (position == chatDecorRow) {
            boolean next = !CustomProfileEngine.flagOf("chat_decor", true);
            CustomProfileEngine.flagSet("chat_decor", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == chatBubblesRow) {
            boolean next = !CustomProfileEngine.flagOf("chat_bubbles", true);
            CustomProfileEngine.flagSet("chat_bubbles", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == bakeOutsideRow) {
            boolean next = !CustomProfileEngine.flagOf("bake_outside", false);
            CustomProfileEngine.flagSet("bake_outside", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == bakeProfileRow) {
            boolean next = !CustomProfileEngine.flagOf("bake_profile", false);
            CustomProfileEngine.flagSet("bake_profile", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == thanksRow) {
            CustomProfileEngine.openThanks();
        } else if (position == channelRow) {
            try {
                getParentActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=RoflPlugins")));
            } catch (Throwable ignore) {}
        } else if (position == pingRow) {
            CustomProfileEngine.pingServers();
        } else if (position == logRow) {
            CustomProfileEngine.openLog();
        } else if (position == eggRow) {
            CustomProfileEngine.openEgg();
        }
    }

    private void showLanguageDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Авто", "Авто", "Auto"),
                "Русский",
                "English"
        };
        String[] values = new String[]{"auto", "ru", "en"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Мова оформлення", "Язык плагина", "Language"));
        builder.setItems(options, (dialog, which) -> {
            CustomProfileEngine.setLangMode(values[which]);
            listAdapter.notifyItemChanged(langRow);
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
            if (position == headerProfileRow || position == headerBatteryRow || position == headerAboutRow || position == headerDevRow) {
                return TYPE_HEADER;
            } else if (position == chatDecorRow || position == chatBubblesRow || position == bakeOutsideRow || position == bakeProfileRow) {
                return TYPE_CHECK;
            } else if (position == profileInfoRow || position == batteryInfoRow || position == aboutInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerProfileRow) {
                        cell.setText(MiogramLocale.get("Оформлення та редактор", "Оформление и редактор", "Profile Decoration"));
                    } else if (position == headerBatteryRow) {
                        cell.setText(MiogramLocale.get("Витрата батареї", "Расход батареи", "Battery & Performance"));
                    } else if (position == headerAboutRow) {
                        cell.setText(MiogramLocale.get("Про розробників", "О плагине", "About & Community"));
                    } else if (position == headerDevRow) {
                        cell.setText(MiogramLocale.get("Розробка та діагностика", "Разработка", "Development"));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == chatDecorRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Рамки та прикраси в чатах", "Видеть рамки вне профиля", "Show frames outside profiles"), CustomProfileEngine.flagOf("chat_decor", true), true);
                    } else if (position == chatBubblesRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Кастомні бульбашки повідомлень", "Кастомные пузырьки сообщений", "Custom message bubbles"), CustomProfileEngine.flagOf("chat_bubbles", true), true);
                    } else if (position == bakeOutsideRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Запікання поза профілем", "Запекание вне профиля", "Baking outside profiles"), CustomProfileEngine.flagOf("bake_outside", false), true);
                    } else if (position == bakeProfileRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Запікання в профілі", "Запекание в профиле", "Baking in profiles"), CustomProfileEngine.flagOf("bake_profile", false), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == editorRow) {
                        cell.setText(MiogramLocale.get("Редактор оформлення профілю", "Редактор оформления профиля", "Profile Banner Editor"), true);
                    } else if (position == bubbleRow) {
                        cell.setText(MiogramLocale.get("Пухирець повідомлень (Sheet)", "Пузырёк сообщений", "Message Bubble Sheet"), true);
                    } else if (position == langRow) {
                        String mode = CustomProfileEngine.getLangMode();
                        String val = "en".equals(mode) ? "English" : ("ru".equals(mode) ? "Русский" : MiogramLocale.get("Авто", "Авто", "Auto"));
                        cell.setTextAndValue(MiogramLocale.get("Мова інтерфейсу", "Язык плагина", "Language"), val, false);
                    } else if (position == thanksRow) {
                        cell.setText(MiogramLocale.get("Список подяк (Автори)", "Спасибо (Те, кто поддержал)", "Thanks & Supporters"), true);
                    } else if (position == channelRow) {
                        cell.setText(MiogramLocale.get("Канал розробників @RoflPlugins", "Канал плагина @RoflPlugins", "Channel @RoflPlugins"), false);
                    } else if (position == pingRow) {
                        cell.setText(MiogramLocale.get("Перевірити зв'язок із серверами", "Проверить связь", "Check servers"), true);
                    } else if (position == logRow) {
                        cell.setText(MiogramLocale.get("Журнал діагностики", "Журнал плагина", "Plugin log"), true);
                    } else if (position == eggRow) {
                        cell.setText(MiogramLocale.get("Відкрити пасхалку", "Открыть пасхалку", "Open the easter egg"), false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == profileInfoRow) {
                        cell.setText(MiogramLocale.get("Налаштування банерів, анімованих обкладинок, градієнтних сіток, світяться імен та форм аватарок.",
                                "Настройка баннеров, анимированных обложек, градиентных сеток, светящихся имен и форм аватарок.",
                                "Configure banners, animated covers, mesh gradients, glowing text, and avatar geometry."));
                    } else if (position == batteryInfoRow) {
                        cell.setText(MiogramLocale.get("Запікання чужого оформлення в одну текстуру знижує навантаження на акумулятор.",
                                "Запекание чужого оформления в одну текстуру снижает нагрузку на аккумулятор.",
                                "Baking decorations into static textures conserves battery and GPU cycles."));
                    } else if (position == aboutInfoRow) {
                        cell.setText(MiogramLocale.get("Вбудований нативний рушій Custom Profile v1.8.1 для Miogram.",
                                "Встроенный нативный движок Custom Profile v1.8.1 для Miogram.",
                                "Built-in native Custom Profile v1.8.1 engine for Miogram."));
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