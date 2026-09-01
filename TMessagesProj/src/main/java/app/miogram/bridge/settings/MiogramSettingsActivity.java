package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ui.MiogramAiSettingsActivity;
import app.miogram.bridge.ui.MiogramChatsSettingsActivity;
import app.miogram.bridge.ui.MiogramPerformanceActivity;
import app.miogram.bridge.ui.MiogramPrivacySettingsActivity;
import app.miogram.bridge.ui.MiogramVisualsActivity;
import app.miogram.bridge.updater.MiogramUpdater;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoTranslatorSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Unified Main Miogram Settings Hub with dynamic multilingual localization.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    private int headerPresetRow;
    private int presetRow;
    private int presetInfoRow;

    private int headerCategoriesRow;
    private int visualsRow;
    private int customProfileRow;
    private int chatsRow;
    private int privacyRow;
    private int translatorRow;
    private int performanceRow;
    private int categoriesInfoRow;

    private int headerAdvancedRow;
    private int aiRow;
    private int pluginsRow;
    private int updaterRow;
    private int advancedInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Налаштування Miogram", "Настройки Miogram", "Miogram Settings");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerPresetRow = addRow();
        presetRow = addRow();
        presetInfoRow = addRow();

        headerCategoriesRow = addRow();
        visualsRow = addRow();
        customProfileRow = addRow();
        chatsRow = addRow();
        privacyRow = addRow();
        translatorRow = addRow();
        performanceRow = addRow();
        categoriesInfoRow = addRow();

        headerAdvancedRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        updaterRow = addRow();
        advancedInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == presetRow) {
            showPresetDialog();
        } else if (position == visualsRow) {
            presentFragment(new MiogramVisualsActivity());
        } else if (position == customProfileRow) {
            presentFragment(new app.miogram.bridge.profile.MiogramCustomProfileActivity());
        } else if (position == chatsRow) {
            presentFragment(new MiogramChatsSettingsActivity());
        } else if (position == privacyRow) {
            presentFragment(new MiogramPrivacySettingsActivity());
        } else if (position == translatorRow) {
            presentFragment(new NekoTranslatorSettingsActivity());
        } else if (position == performanceRow) {
            presentFragment(new MiogramPerformanceActivity());
        } else if (position == aiRow) {
            presentFragment(new MiogramAiSettingsActivity());
        } else if (position == pluginsRow) {
            presentFragment(new app.exteraless.plugins.ui.PluginsActivity());
        } else if (position == updaterRow) {
            MiogramUpdater.checkAndShowUpdate(this, true);
        }
    }

    private void showPresetDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        final app.miogram.bridge.divine.MiogramDivineEngine.Preset[] presets = app.miogram.bridge.divine.MiogramDivineEngine.Preset.values();
        String[] titles = new String[presets.length];
        for (int i = 0; i < presets.length; i++) {
            titles[i] = app.miogram.bridge.divine.MiogramDivineEngine.getPresetTitle(presets[i]);
        }

        app.miogram.bridge.divine.MiogramDivineEngine.Preset current = app.miogram.bridge.divine.MiogramDivineEngine.getCurrentPreset(ctx);
        int selectedIndex = 0;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == current) {
                selectedIndex = i;
                break;
            }
        }

        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Глобальні пресети макета", "Глобальные пресеты макета", "Miogram Layout Presets"));
        builder.setItems(titles, (dialog, which) -> {
            app.miogram.bridge.divine.MiogramDivineEngine.applyPreset(ctx, presets[which]);
            dialog.dismiss();
            if (listView != null && listView.getAdapter() != null) {
                listView.getAdapter().notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerPresetRow || position == headerCategoriesRow || position == headerAdvancedRow) {
                return TYPE_HEADER;
            } else if (position == presetInfoRow || position == categoriesInfoRow || position == advancedInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerPresetRow) {
                        cell.setText(MiogramLocale.get("✨ Пресети макета інтерфейсу ໒꒱", "✨ Пресеты макета интерфейса ໒꒱", "✨ UI Layout Presets ໒꒱"));
                    } else if (position == headerCategoriesRow) {
                        cell.setText(MiogramLocale.get("💖 Основні розділи ໒꒱", "💖 Основные разделы ໒꒱", "💖 Main Sections ໒꒱"));
                    } else if (position == headerAdvancedRow) {
                        cell.setText(MiogramLocale.get("✨ Інтелект та Інструменти ໒꒱", "✨ Интеллект и Инструменты ໒꒱", "✨ AI & Developer Tools ໒꒱"));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == presetRow) {
                        app.miogram.bridge.divine.MiogramDivineEngine.Preset current = app.miogram.bridge.divine.MiogramDivineEngine.getCurrentPreset(getParentActivity());
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Активний макет інтерфейсу", "Активный макет интерфейса", "Active Layout"),
                                app.miogram.bridge.divine.MiogramDivineEngine.getPresetTitle(current),
                                R.drawable.msg_theme,
                                false
                        );
                    } else if (position == visualsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Зовнішній вигляд", "Внешний вид", "Appearance"), R.drawable.msg_theme, true);
                    } else if (position == customProfileRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Оформлення профілю", "Оформление профиля", "Custom Profile"), R.drawable.msg_customize, true);
                    } else if (position == chatsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Чати та Медіа", "Чаты и Медиа", "Chats & Media"), R.drawable.msg_camera, true);
                    } else if (position == privacyRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Конфіденційність та Ghost Mode", "Конфиденциальность и Ghost Mode", "Privacy & Ghost Mode"), R.drawable.msg_secret, true);
                    } else if (position == translatorRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перекладач", "Переводчик", "Translator"), R.drawable.msg_translate, true);
                    } else if (position == performanceRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Продуктивність", "Производительность", "Performance"), R.drawable.msg_speed, false);
                    } else if (position == aiRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Miogram AI", "Miogram AI", "Miogram AI"), R.drawable.msg_bot, true);
                    } else if (position == pluginsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Плагіни Miogram", "Плагины Miogram", "Plugins"), R.drawable.msg_folders, true);
                    } else if (position == updaterRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перевірити оновлення Miogram", "Проверить обновления Miogram", "Check for Updates"), R.drawable.msg_retry, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == presetInfoRow) {
                        cell.setText(MiogramLocale.get("Клієнт використовує стилізацію Ame-chan. Тут ви можете обрати режим макета (Discord, iOS Glass, Classic TG, Minimalist).", "Клиент использует стилизацию Ame-chan. Здесь вы можете выбрать режим макета (Discord, iOS Glass, Classic TG, Minimalist).", "Miogram uses omnipresent Ame-chan aesthetic. Here you can choose your preferred layout (Discord, iOS Glass, Classic TG, Minimalist)."));
                    } else if (position == categoriesInfoRow) {
                        cell.setText(MiogramLocale.get("Повний набір функцій exteraless та Nagram, оптимізований та об'єднаний у Miogram.", "Полный набор функций exteraless и Nagram, оптимизированный и объединённый в Miogram.", "Full suite of exteraless and Nagram features, unified and optimized for Miogram."));
                    } else if (position == advancedInfoRow) {
                        cell.setText(MiogramLocale.get("Нативні плагіни, голосовий ШІ Gemini Live та система безшовних оновлень.", "Нативные плагины, голосовой ИИ Gemini Live и система бесшовных обновлений.", "Native plugins, Gemini Live multimodal voice AI, and seamless in-app updater."));
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
