package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.companion.MiogramCompanionActivity;
import app.miogram.bridge.ai.companion.MiogramCompanionPrefs;
import app.miogram.bridge.badge.MiogramSupabaseBridge;
import app.miogram.bridge.ui.MiogramAiSettingsActivity;
import app.miogram.bridge.ui.MiogramChatsSettingsActivity;
import app.miogram.bridge.ui.MiogramPerformanceActivity;
import app.miogram.bridge.ui.MiogramPrivacySettingsActivity;
import app.miogram.bridge.ui.MiogramVisualsActivity;
import app.miogram.bridge.updater.MiogramUpdater;
import app.miogram.bridge.userbot.MiogramHerokuActivity;
import app.miogram.bridge.userbot.MiogramHerokuManager;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoTranslatorSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Clean, Streamlined Main Miogram Settings Hub.
 * Beautifully organized into 5 structured sections with dynamic status subtitles.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    // 1. Aesthetics & Atmosphere ໒꒱
    private int headerCustomizationRow;
    private int visualsRow;
    private int iconPacksRow;
    private int navigationRow;
    private int subfoldersRow;
    private int badgeStudioRow;

    // 2. Chats, Multichat & Private Vault 💬
    private int headerChatsPrivacyRow;
    private int multichatRow;
    private int chatsRow;
    private int cloudVaultRow;
    private int privacyRow;
    private int translatorRow;
    private int localizerRow;

    // 3. AI & Companions ✧
    private int headerAiRow;
    private int companionRow;
    private int aiEngineRow;

    // 4. Heroku Userbot & Automation 🪐
    private int headerUserbotRow;
    private int userbotHubRow;

    // 5. System, Plugins & Updates ⚙
    private int headerSystemRow;
    private int pluginsRow;
    private int performanceRow;
    private int telemetryRow;
    private int generalRow;
    private int updaterRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Налаштування Miogram", "Настройки Miogram", "Miogram Settings");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        // 1. Aesthetics & Atmosphere
        headerCustomizationRow = addRow();
        visualsRow = addRow();
        iconPacksRow = addRow();
        navigationRow = addRow();
        subfoldersRow = addRow();
        badgeStudioRow = addRow();

        // 2. Chats, Multichat & Private Vault
        headerChatsPrivacyRow = addRow();
        multichatRow = addRow();
        chatsRow = addRow();
        cloudVaultRow = addRow();
        privacyRow = addRow();
        translatorRow = addRow();
        localizerRow = addRow();

        // 3. AI & Companions
        headerAiRow = addRow();
        companionRow = addRow();
        aiEngineRow = addRow();

        // 4. Heroku Userbot & Automation
        headerUserbotRow = addRow();
        userbotHubRow = addRow();

        // 5. System, Plugins & Updates
        headerSystemRow = addRow();
        pluginsRow = addRow();
        performanceRow = addRow();
        telemetryRow = addRow();
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
        } else if (position == translatorRow) {
            presentFragment(new NekoTranslatorSettingsActivity());
        } else if (position == localizerRow) {
            presentFragment(new app.miogram.bridge.localizer.MiogramLocalizerActivity());
        } else if (position == companionRow) {
            presentFragment(new MiogramCompanionActivity());
        } else if (position == aiEngineRow) {
            presentFragment(new MiogramAiSettingsActivity());
        } else if (position == userbotHubRow) {
            presentFragment(new MiogramHerokuActivity());
        } else if (position == pluginsRow) {
            presentFragment(new app.exteraless.plugins.ui.PluginsActivity());
        } else if (position == performanceRow) {
            presentFragment(new MiogramPerformanceActivity());
        } else if (position == telemetryRow) {
            boolean nextState = !MiogramSupabaseBridge.isTelemetryEnabled();
            MiogramSupabaseBridge.setTelemetryEnabled(nextState);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(nextState);
            }
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
            if (position == headerCustomizationRow || position == headerChatsPrivacyRow ||
                    position == headerAiRow || position == headerUserbotRow || position == headerSystemRow) {
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
                        cell.setText(MiogramLocale.get("Кастомізація та дизайн ໒꒱", "Кастомизация и дизайн ໒꒱", "Customization & Design ໒꒱"));
                    } else if (position == headerChatsPrivacyRow) {
                        cell.setText(MiogramLocale.get("Чати, мультичат та сховище 💬", "Чаты, мультичат и хранилище 💬", "Chats, Multichat & Vault 💬"));
                    } else if (position == headerAiRow) {
                        cell.setText(MiogramLocale.get("Штучний інтелект та супутники ✧", "Искусственный интеллект и спутники ✧", "AI & Companions ✧"));
                    } else if (position == headerUserbotRow) {
                        cell.setText(MiogramLocale.get("Heroku Юзербот та автоматизація 🪐", "Heroku Юзербот и автоматизация 🪐", "Heroku Userbot & Automation 🪐"));
                    } else if (position == headerSystemRow) {
                        cell.setText(MiogramLocale.get("Система, плагіни та екосистема ⚙", "Система, плагины и экосистема ⚙", "System, Plugins & Ecosystem ⚙"));
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    // Section 1
                    if (position == visualsRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Візуальні стилі та пресети", "Визуальные стили и пресеты", "Visual Styles & Presets"),
                                MiogramLocale.get("iOS, Discord, MD3, Анімації", "iOS, Discord, MD3, Анимации", "iOS, Discord, MD3, FX"),
                                R.drawable.msg_theme,
                                true
                        );
                    } else if (position == iconPacksRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Паки іконок", "Паки иконок", "Icon Packs"),
                                MiogramLocale.get("Miogram, Cyber, Retro", "Miogram, Cyber, Retro", "Miogram, Cyber, Retro"),
                                R.drawable.msg_customize,
                                true
                        );
                    } else if (position == navigationRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Навігація та панель вкладок", "Навигация и панель вкладок", "Navigation & Tab Bar"),
                                MiogramLocale.get("Стиль панелі, жести, pills", "Стиль панели, жесты, pills", "Bar style, gestures, pills"),
                                R.drawable.msg_folders,
                                true
                        );
                    } else if (position == subfoldersRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Підпапки чатів", "Подпапки чатов", "Chat Subfolders"),
                                MiogramLocale.get("Вкладені теки діалогів", "Вложенные папки диалогов", "Nested chat folders"),
                                R.drawable.msg_folder,
                                true
                        );
                    } else if (position == badgeStudioRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Студія бейджів спільноти", "Студия бейджей сообщества", "Community Badge Studio"),
                                MiogramLocale.get("3D / Анімовані бейджі", "3D / Анимированные бейджи", "3D / Animated badges"),
                                R.drawable.baseline_stars_24,
                                false
                        );
                    }
                    // Section 2
                    else if (position == multichatRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Мультичат (Split Screen)", "Мультичат (Split Screen)", "Multichat (Split Screen)"),
                                MiogramLocale.get("Два чати поруч", "Два чата рядом", "Two chats side-by-side"),
                                R.drawable.msg_openin,
                                true
                        );
                    } else if (position == chatsRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Налаштування чатів", "Настройки чатов", "Chat Settings"),
                                MiogramLocale.get("Бабли, відео-кружки, жести", "Бабблы, видео-кружки, жесты", "Bubbles, video circles, gestures"),
                                R.drawable.msg_message,
                                true
                        );
                    } else if (position == cloudVaultRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Miogram Cloud Vault", "Miogram Cloud Vault", "Miogram Cloud Vault"),
                                MiogramLocale.get("Приватне подвійне сховище", "Приватное двойное хранилище", "Private dual storage"),
                                R.drawable.msg_saved,
                                true
                        );
                    } else if (position == privacyRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Приватність та Ghost Mode", "Приватность и Ghost Mode", "Privacy & Ghost Mode"),
                                MiogramLocale.get("Прихований онлайн, статус прочитання", "Скрытый онлайн, статус прочтения", "Hidden online, read status"),
                                R.drawable.msg_secret,
                                true
                        );
                    } else if (position == translatorRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Вбудований перекладач", "Встроенный переводчик", "Built-in Translator"),
                                MiogramLocale.get("Google, DeepL, Telegram", "Google, DeepL, Telegram", "Google, DeepL, Telegram"),
                                R.drawable.msg2_language,
                                true
                        );
                    } else if (position == localizerRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Локалізатор застосунку", "Локализатор приложения", "App Localizer"),
                                MiogramLocale.get("Користувацькі переклади", "Пользовательские переводы", "Custom translations"),
                                R.drawable.msg_language,
                                false
                        );
                    }
                    // Section 3
                    else if (position == companionRow) {
                        String companionName = MiogramCompanionPrefs.isAmeActive() ? "Ame-chan (飴ちゃん)" : "KAngel (超てんちゃん)";
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("ШІ Супутник ໒꒱", "ИИ Спутник ໒꒱", "AI Companion ໒꒱"),
                                companionName,
                                R.drawable.baseline_stars_24,
                                true
                        );
                    } else if (position == aiEngineRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Налаштування ШІ та Ключі", "Настройки ИИ и Ключи", "AI Engine & Keyring"),
                                MiogramLocale.get("Gemini 3.5, Keyring, Голос у текст", "Gemini 3.5, Keyring, Голос в текст", "Gemini 3.5, Keyring, Voice to text"),
                                R.drawable.msg_bot,
                                false
                        );
                    }
                    // Section 4
                    else if (position == userbotHubRow) {
                        boolean ubOn = MiogramHerokuManager.getInstance().isEnabled();
                        boolean hasBot = MiogramHerokuManager.getInstance().hasConfiguredBot();
                        String ubStatus = !ubOn ? "Вимкнено" : (hasBot ? "@" + MiogramHerokuManager.getInstance().getBotUsername() : "Префікс: " + MiogramHerokuManager.getInstance().getPrefix());
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Heroku Userbot Менеджер", "Heroku Userbot Менеджер", "Heroku Userbot Manager"),
                                ubStatus,
                                R.drawable.msg_bot,
                                false
                        );
                    }
                    // Section 5
                    else if (position == pluginsRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Плагіни MioHook та exteraGram", "Плагины MioHook и exteraGram", "MioHook & exteraGram Plugins"),
                                MiogramLocale.get("Каталог Python та WASM модулів", "Каталог Python и WASM модулей", "Python & WASM modules catalog"),
                                R.drawable.msg_plugins,
                                true
                        );
                    } else if (position == performanceRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Продуктивність та оптимізація", "Производительность и оптимизация", "Performance & Optimization"),
                                MiogramLocale.get("Кеш, GPU прискорення, RAM", "Кэш, GPU ускорение, RAM", "Cache, GPU acceleration, RAM"),
                                R.drawable.msg_round_speed,
                                true
                        );
                    } else if (position == generalRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Розширені налаштування клієнта", "Расширенные настройки клиента", "Advanced Client Settings"),
                                MiogramLocale.get("Діагностика, пуші, пам'ять", "Диагностика, пуши, память", "Diagnostics, push, storage"),
                                R.drawable.msg_settings,
                                true
                        );
                    } else if (position == updaterRow) {
                        cell.setTextAndValueAndIcon(
                                MiogramLocale.get("Перевірити оновлення", "Проверить обновления", "Check for Updates"),
                                "v" + org.telegram.messenger.BuildVars.BUILD_VERSION_STRING,
                                R.drawable.msg_arrow_down,
                                false
                        );
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
