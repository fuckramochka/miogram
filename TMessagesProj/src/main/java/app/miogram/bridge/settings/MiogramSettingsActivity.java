package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextCheckCell;
import app.miogram.bridge.badge.MiogramSupabaseBridge;

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

    // Category 1: Customization ໒꒱
    private int headerCustomizationRow;
    private int visualsRow;
    private int iconPacksRow;
    private int navigationRow;
    private int subfoldersRow;
    private int badgeStudioRow;
    private int customizationInfoRow;

    // Category 2: Security & Privacy 🛡️
    private int headerSecurityRow;
    private int privacyRow;
    private int telemetryRow;
    private int securityInfoRow;

    // Category 3: Cloud Vault ☁️
    private int headerCloudRow;
    private int cloudVaultRow;
    private int cloudInfoRow;

    // Category 4: Chats & Media 💬
    private int headerChatsRow;
    private int multichatRow;
    private int chatsRow;
    private int translatorRow;
    private int localizerRow;
    private int chatsInfoRow;

    // Category 5: AI & Plugins 🧠
    private int headerAiPluginsRow;
    private int aiRow;
    private int pluginsRow;
    private int aiPluginsInfoRow;

    // Category 6: System & Performance ✧
    private int headerSystemRow;
    private int performanceRow;
    private int generalRow;
    private int updaterRow;
    private int systemInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Налаштування Miogram", "Настройки Miogram", "Miogram Settings");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        // 1. Customization
        headerCustomizationRow = addRow();
        visualsRow = addRow();
        iconPacksRow = addRow();
        navigationRow = addRow();
        subfoldersRow = addRow();
        badgeStudioRow = addRow();
        customizationInfoRow = addRow();

        // 2. Security & Privacy
        headerSecurityRow = addRow();
        privacyRow = addRow();
        telemetryRow = addRow();
        securityInfoRow = addRow();

        // 3. Cloud Vault
        headerCloudRow = addRow();
        cloudVaultRow = addRow();
        cloudInfoRow = addRow();

        // 4. Chats & Media
        headerChatsRow = addRow();
        multichatRow = addRow();
        chatsRow = addRow();
        translatorRow = addRow();
        localizerRow = addRow();
        chatsInfoRow = addRow();

        // 5. AI & Plugins
        headerAiPluginsRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        aiPluginsInfoRow = addRow();

        // 6. System & Performance
        headerSystemRow = addRow();
        performanceRow = addRow();
        generalRow = addRow();
        updaterRow = addRow();
        systemInfoRow = addRow();
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
        } else if (position == privacyRow) {
            presentFragment(new MiogramPrivacySettingsActivity());
        } else if (position == telemetryRow) {
            boolean nextState = !MiogramSupabaseBridge.isTelemetryEnabled();
            MiogramSupabaseBridge.setTelemetryEnabled(nextState);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(nextState);
            }
        } else if (position == cloudVaultRow) {
            presentFragment(new app.miogram.bridge.cloudvault.MiogramCloudVaultActivity());
        } else if (position == multichatRow) {
            presentFragment(new app.miogram.bridge.multichat.MiogramSplitChatActivity(0, 0));
        } else if (position == chatsRow) {
            presentFragment(new MiogramChatsSettingsActivity());
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
            if (position == headerCustomizationRow || position == headerSecurityRow
                    || position == headerCloudRow || position == headerChatsRow
                    || position == headerAiPluginsRow || position == headerSystemRow) {
                return TYPE_HEADER;
            } else if (position == telemetryRow) {
                return TYPE_CHECK;
            } else if (position == customizationInfoRow || position == securityInfoRow
                    || position == cloudInfoRow || position == chatsInfoRow
                    || position == aiPluginsInfoRow || position == systemInfoRow) {
                return TYPE_INFO_PRIVACY;
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
                                MiogramLocale.get("Хмарна телеметрія та бейджі спільноти", "Облачная телеметрия и бейджи сообщества", "Community Cloud Telemetry & Badges"),
                                MiogramSupabaseBridge.isTelemetryEnabled(),
                                false
                        );
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerCustomizationRow) {
                        cell.setText(MiogramLocale.get("Кастомізація та стиль ໒꒱", "Кастомизация и стиль ໒꒱", "Customization & Style ໒꒱"));
                    } else if (position == headerSecurityRow) {
                        cell.setText(MiogramLocale.get("Безпека та конфіденційність 🛡️", "Безопасность и приватность 🛡️", "Security & Privacy 🛡️"));
                    } else if (position == headerCloudRow) {
                        cell.setText(MiogramLocale.get("Хмарне сховище ☁️", "Облачное хранилище ☁️", "Cloud Vault ☁️"));
                    } else if (position == headerChatsRow) {
                        cell.setText(MiogramLocale.get("Чати та спілкування 💬", "Чаты и общение 💬", "Chats & Communication 💬"));
                    } else if (position == headerAiPluginsRow) {
                        cell.setText(MiogramLocale.get("Штучний інтелект та плагіни 🧠", "Искусственный интеллект и плагины 🧠", "AI & Plugins 🧠"));
                    } else if (position == headerSystemRow) {
                        cell.setText(MiogramLocale.get("Система та продуктивність ✧", "Система и производительность ✧", "System & Performance ✧"));
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
                    } else if (position == privacyRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Конфіденційність та Ghost Mode", "Конфиденциальность и Ghost Mode", "Privacy & Ghost Mode"), R.drawable.msg_secret, true);
                    } else if (position == cloudVaultRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Хмарне сховище (Cloud Vault)", "Облачное хранилище (Cloud Vault)", "Encrypted Cloud Vault"), R.drawable.cloud_sync, false);
                    } else if (position == multichatRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Мультичат (Split-Screen)", "Мультичат (Split-Screen)", "Multi-Chat (Split-Screen)"), R.drawable.msg_fave, true);
                    } else if (position == chatsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Чати та Медіа", "Чаты и Медиа", "Chats & Media"), R.drawable.msg_camera, true);
                    } else if (position == translatorRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перекладач", "Переводчик", "Translator"), R.drawable.msg_translate, true);
                    } else if (position == localizerRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Локалізатор (Кастомні переклади)", "Локализатор (Кастомные переводы)", "Localizer (Custom Translations)"), R.drawable.msg_edit, false);
                    } else if (position == aiRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Miogram AI", "Miogram AI", "Miogram AI"), R.drawable.msg_bot, true);
                    } else if (position == pluginsRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Плагіни Miogram (Python & Каталог)", "Плагины Miogram (Python & Каталог)", "Miogram Plugins (Python & Catalog)"), R.drawable.msg_plugins, false);
                    } else if (position == performanceRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Продуктивність", "Производительность", "Performance"), R.drawable.msg_speed, true);
                    } else if (position == generalRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Розширені налаштування", "Расширенные настройки", "Advanced Preferences"), R.drawable.msg_settings, true);
                    } else if (position == updaterRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Перевірити оновлення Miogram", "Проверить обновления Miogram", "Check for Updates"), R.drawable.msg_retry, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == customizationInfoRow) {
                        cell.setText(MiogramLocale.get("Теми, іконки додатку, нижня панель навігації та канонічні відзнаки профілю.", "Темы, иконки приложения, нижняя панель навигации и канонические бейджи профиля.", "Themes, app icons, bottom navigation bar, and canonical profile badges."));
                    } else if (position == securityInfoRow) {
                        cell.setText(MiogramLocale.get("Режими прихованого читання, заборона відстеження та синхронізація відзнак через безпечний сервер.", "Режимы скрытого чтения, запрет отслеживания и синхронизация бейджей через безопасный сервер.", "Ghost mode, anti-tracking, and badge synchronization through a secure server."));
                    } else if (position == cloudInfoRow) {
                        cell.setText(MiogramLocale.get("Зашифрований хмарний диск без обмежень на базі форум-супергрупи з автоматичною нарізкою великих файлів.", "Зашифрованный облачный диск без ограничений на базе форум-супергруппы с автоматической нарезкой больших файлов.", "Encrypted cloud disk without limits based on forum supergroups with automatic chunking of large files."));
                    } else if (position == chatsInfoRow) {
                        cell.setText(MiogramLocale.get("Паралельне відображення двох чатів на одному екрані, переклади повідомлень та медіа-налаштування.", "Параллельное отображение двух чатов на одном экране, переводы сообщений и медиа-настройки.", "Split-screen parallel view of two chats, inline translations, and media settings."));
                    } else if (position == aiPluginsInfoRow) {
                        cell.setText(MiogramLocale.get("Інтеграція нейромереж, обробка відповідей та екосистема розширень.", "Интеграция нейросетей, обработка ответов и экосистема расширений.", "Neural network integration, response processing, and extensions ecosystem."));
                    } else if (position == systemInfoRow) {
                        cell.setText(MiogramLocale.get("Оптимізація пам'яті, кешування та автоматична перевірка офіційних релізів Miogram.", "Оптимизация памяти, кэширование и автоматическая проверка официальных релизов Miogram.", "Memory optimization, caching, and automatic check for official Miogram releases."));
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
