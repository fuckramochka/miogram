package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
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
 * Exposes full styling, navigation, icon packs, chats, privacy, cloud badges, and system controls.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    private int headerMiogramFeaturesRow;
    private int customUiRow;
    private int multichatRow;
    private int badgeStudioRow;
    private int miogramFeaturesInfoRow;

    private int headerCategoriesRow;
    private int visualsRow;
    private int navigationRow;
    private int iconPacksRow;
    private int chatsRow;
    private int privacyRow;
    private int generalRow;
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

        headerMiogramFeaturesRow = addRow();
        customUiRow = addRow();
        multichatRow = addRow();
        badgeStudioRow = addRow();
        miogramFeaturesInfoRow = addRow();

        headerCategoriesRow = addRow();
        visualsRow = addRow();
        navigationRow = addRow();
        iconPacksRow = addRow();
        chatsRow = addRow();
        privacyRow = addRow();
        generalRow = addRow();
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
        if (position == customUiRow) {
            presentFragment(new app.miogram.bridge.customui.MiogramCustomUiActivity());
        } else if (position == multichatRow) {
            presentFragment(new app.miogram.bridge.multichat.MiogramSplitChatActivity(0, 0));
        } else if (position == badgeStudioRow) {
            app.miogram.bridge.badge.MiogramBadgeBottomSheet.show(getParentActivity(), currentAccount);
        } else if (position == visualsRow) {
            presentFragment(new MiogramVisualsActivity());
        } else if (position == navigationRow) {
            presentFragment(new app.exteraless.settings.OpenExteraAppNavigationActivity());
        } else if (position == iconPacksRow) {
            presentFragment(new app.exteraless.icons.IconPacksActivity());
        } else if (position == chatsRow) {
            presentFragment(new MiogramChatsSettingsActivity());
        } else if (position == privacyRow) {
            presentFragment(new MiogramPrivacySettingsActivity());
        } else if (position == generalRow) {
            presentFragment(new app.exteraless.settings.OpenExteraGeneralActivity());
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

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerMiogramFeaturesRow || position == headerCategoriesRow || position == headerAdvancedRow) {
                return TYPE_HEADER;
            } else if (position == miogramFeaturesInfoRow || position == categoriesInfoRow || position == advancedInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerMiogramFeaturesRow) {
                        cell.setText(MiogramLocale.get("Ексклюзивні функції Miogram ໒꒱", "Эксклюзивные функции Miogram ໒꒱", "Miogram Exclusive Features ໒꒱"));
                    } else if (position == headerCategoriesRow) {
                        cell.setText(MiogramLocale.get("Зовнішній вигляд та функції", "Внешний вид и функции", "Appearance & Features"));
                    } else if (position == headerAdvancedRow) {
                        cell.setText(MiogramLocale.get("Система та Інструменти", "Система и Инструменты", "System & Tools"));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == customUiRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Додаткові функції ໒꒱ (Кастомне оформлення)", "Дополнительные функции ໒꒱ (Кастомное оформление)", "Extra Features ໒꒱ (Custom UI)"), R.drawable.msg_theme, true);
                    } else if (position == multichatRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Мультичат (Split-Screen) 🪟", "Мультичат (Split-Screen) 🪟", "Multi-Chat (Split-Screen) 🪟"), R.drawable.msg_fave, true);
                    } else if (position == badgeStudioRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Канонічні відзнаки Miogram ໒꒱", "Канонические отличия Miogram ໒꒱", "Miogram Canonical Badges ໒꒱"), R.drawable.msg_premium_badge, true);
                    } else if (position == visualsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Зовнішній вигляд та стиль", "Внешний вид и стиль", "Appearance & Style"), R.drawable.msg_theme, true);
                    } else if (position == navigationRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Навігація та Меню", "Навигация и Меню", "Navigation & Menu"), R.drawable.msg_folders, true);
                    } else if (position == iconPacksRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Паки іконок", "Паки иконок", "Icon Packs"), R.drawable.msg_sticker, true);
                    } else if (position == chatsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Чати та Медіа", "Чаты и Медиа", "Chats & Media"), R.drawable.msg_camera, true);
                    } else if (position == privacyRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Конфіденційність та Ghost Mode", "Конфиденциальность и Ghost Mode", "Privacy & Ghost Mode"), R.drawable.msg_secret, true);
                    } else if (position == generalRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Розширені налаштування", "Расширенные настройки", "Advanced Preferences"), R.drawable.msg_settings, true);
                    } else if (position == translatorRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перекладач", "Переводчик", "Translator"), R.drawable.msg_translate, true);
                    } else if (position == performanceRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Продуктивність", "Производительность", "Performance"), R.drawable.msg_speed, false);
                    } else if (position == aiRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Miogram AI", "Miogram AI", "Miogram AI"), R.drawable.msg_bot, true);
                    } else if (position == pluginsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Плагіни Miogram", "Плагины Miogram", "Plugins"), R.drawable.msg_plugins, true);
                    } else if (position == updaterRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перевірити оновлення Miogram", "Проверить обновления Miogram", "Check for Updates"), R.drawable.msg_retry, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == miogramFeaturesInfoRow) {
                        cell.setText(MiogramLocale.get("Розумна стрічка каналів з очищенням від реклами через Gemini AI, подвійні чати, канбан та 10 канонічних відзнак.", "Умная лента каналов с очисткой от рекламы через Gemini AI, двойные чаты, канбан и 10 канонических бейджей.", "Smart Feed with Gemini AI ad-filtering, dual split-screen chats, kanban organizer, and 10 canonical badges."));
                    } else if (position == categoriesInfoRow) {
                        cell.setText(MiogramLocale.get("Зовнішній вигляд, персоналізація, навігація, паки іконок та розширені параметри чатів.", "Внешний вид, персонализация, навигация, паки иконок и расширенные параметры чатов.", "Appearance, customization, navigation, icon packs, and enhanced chat options."));
                    } else if (position == advancedInfoRow) {
                        cell.setText(MiogramLocale.get("Керування плагінами, сервісами штучного інтелекту та оновленнями Miogram.", "Управление плагинами, сервисами искусственного интеллекта и обновлениями Miogram.", "Manage plugins, AI services, and Miogram updates."));
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
