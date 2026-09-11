package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.badge.MiogramSupabaseBridge;
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
 * Unified Main Miogram Settings Hub.
 * Streamlined into 3 clean, focused sections without bloated description blocks.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    // Section 1: Customization & Interface ໒꒱
    private int headerCustomizationRow;
    private int visualsRow;
    private int iconPacksRow;
    private int navigationRow;
    private int subfoldersRow;
    private int badgeStudioRow;

    // Section 2: Chats, Storage & Privacy 💬
    private int headerChatsPrivacyRow;
    private int multichatRow;
    private int chatsRow;
    private int cloudVaultRow;
    private int privacyRow;
    private int telemetryRow;
    private int translatorRow;
    private int localizerRow;

    // Section 3: AI, Ecosystem & System ✧
    private int headerSystemRow;
    private int aiRow;
    private int pluginsRow;
    private int performanceRow;
    private int generalRow;
    private int updaterRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Налаштування Miogram", "Настройки Miogram", "Miogram Settings");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        // 1. Customization & Interface
        headerCustomizationRow = addRow();
        visualsRow = addRow();
        iconPacksRow = addRow();
        navigationRow = addRow();
        subfoldersRow = addRow();
        badgeStudioRow = addRow();

        // 2. Chats, Storage & Privacy
        headerChatsPrivacyRow = addRow();
        multichatRow = addRow();
        chatsRow = addRow();
        cloudVaultRow = addRow();
        privacyRow = addRow();
        telemetryRow = addRow();
        translatorRow = addRow();
        localizerRow = addRow();

        // 3. AI, Ecosystem & System
        headerSystemRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        performanceRow = addRow();
        generalRow = addRow();
        updaterRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == visualsRow) {
            presentFragment(new MiogramVisualsActivity());
        } else if (position == iconPacksRow) {
            presentFragment(new app.exteraless.icons.IconPacksActivity());
        } else if (position == navigationRow) {
            presentFragment(new app.exteraless.settings.OpenExteraAppNavigationActivity());
        } else if (position == subfoldersRow) {
            presentFragment(new app.miogram.bridge.folders.MiogramSubfolderSettingsActivity());
        } else if (position == badgeStudioRow) {
            long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
            app.miogram.bridge.badge.MiogramBadgeBottomSheet.show(getParentActivity(), clientUserId);
        } else if (position == multichatRow) {
            presentFragment(new app.miogram.bridge.multichat.MiogramSplitChatActivity(0, 0));
        } else if (position == chatsRow) {
            presentFragment(new MiogramChatsSettingsActivity());
        } else if (position == cloudVaultRow) {
            presentFragment(new app.miogram.bridge.cloudvault.MiogramCloudVaultActivity());
        } else if (position == privacyRow) {
            presentFragment(new MiogramPrivacySettingsActivity());
        } else if (position == telemetryRow) {
            boolean nextState = !MiogramSupabaseBridge.isTelemetryEnabled();
            MiogramSupabaseBridge.setTelemetryEnabled(nextState);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(nextState);
            }
        } else if (position == translatorRow) {
            presentFragment(new NekoTranslatorSettingsActivity());
        } else if (position == localizerRow) {
            presentFragment(new app.miogram.bridge.localizer.MiogramLocalizerActivity());
        } else if (position == aiRow) {
            presentFragment(new MiogramAiSettingsActivity());
        } else if (position == pluginsRow) {
            presentFragment(new app.exteraless.plugins.ui.PluginsActivity());
        } else if (position == performanceRow) {
            presentFragment(new MiogramPerformanceActivity());
        } else if (position == generalRow) {
            presentFragment(new app.exteraless.settings.OpenExteraGeneralActivity());
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
            if (position == headerCustomizationRow || position == headerChatsPrivacyRow || position == headerSystemRow) {
                return TYPE_HEADER;
            } else if (position == telemetryRow) {
                return TYPE_CHECK;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == telemetryRow) {
                        cell.setTextAndCheck(
                                MiogramLocale.get("Хмарна синхронізація бейджів", "Облачная синхронизация бейджей", "Cloud Badge Synchronization"),
                                MiogramSupabaseBridge.isTelemetryEnabled(),
                                true
                        );
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerCustomizationRow) {
                        cell.setText(MiogramLocale.get("Кастомізація та інтерфейс ໒꒱", "Кастомизация и интерфейс ໒꒱", "Customization & Interface ໒꒱"));
                    } else if (position == headerChatsPrivacyRow) {
                        cell.setText(MiogramLocale.get("Чати, сховище та безпека 💬", "Чаты, хранилище и безопасность 💬", "Chats, Storage & Security 💬"));
                    } else if (position == headerSystemRow) {
                        cell.setText(MiogramLocale.get("AI, розширення та система ✧", "AI, расширения и система ✧", "AI, Ecosystem & System ✧"));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == visualsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Зовнішній вигляд та стиль", "Внешний вид и стиль", "Appearance & Style"), R.drawable.msg_theme, true);
                    } else if (position == iconPacksRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Паки іконок", "Паки иконок", "Icon Packs"), R.drawable.msg_sticker, true);
                    } else if (position == navigationRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Навігація та Меню", "Навигация и Меню", "Navigation & Menu"), R.drawable.msg_home_solar, true);
                    } else if (position == subfoldersRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Підпапки та Розумні фільтри", "Подпапки и Умные фильтры", "Subfolders & Smart Filters"), R.drawable.msg_folders, true);
                    } else if (position == badgeStudioRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Канонічні відзнаки Miogram", "Канонические отличия Miogram", "Miogram Canonical Badges"), R.drawable.msg_premium_badge, false);
                    } else if (position == multichatRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Мультичат (Split-Screen)", "Мультичат (Split-Screen)", "Multi-Chat (Split-Screen)"), R.drawable.msg_fave, true);
                    } else if (position == chatsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Чати та Медіа", "Чаты и Медиа", "Chats & Media"), R.drawable.msg_camera, true);
                    } else if (position == cloudVaultRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Хмарне сховище (Cloud Vault)", "Облачное хранилище (Cloud Vault)", "Encrypted Cloud Vault"), R.drawable.cloud_sync, true);
                    } else if (position == privacyRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Конфіденційність та Ghost Mode", "Конфиденциальность и Ghost Mode", "Privacy & Ghost Mode"), R.drawable.msg_secret, true);
                    } else if (position == translatorRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перекладач", "Переводчик", "Translator"), R.drawable.msg_translate, true);
                    } else if (position == localizerRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Локалізатор (Кастомні переклади)", "Локализатор (Кастомные переводы)", "Localizer (Custom Translations)"), R.drawable.msg_edit, false);
                    } else if (position == aiRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Miogram AI", "Miogram AI", "Miogram AI"), R.drawable.msg_bot, true);
                    } else if (position == pluginsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Плагіни Miogram (Python & Каталог)", "Плагины Miogram (Python & Каталог)", "Miogram Plugins (Python & Catalog)"), R.drawable.msg_plugins, true);
                    } else if (position == performanceRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Продуктивність", "Производительность", "Performance"), R.drawable.msg_speed, true);
                    } else if (position == generalRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Розширені налаштування", "Расширенные настройки", "Advanced Preferences"), R.drawable.msg_settings, true);
                    } else if (position == updaterRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перевірити оновлення Miogram", "Проверить обновления Miogram", "Check for Updates"), R.drawable.msg_retry, false);
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
