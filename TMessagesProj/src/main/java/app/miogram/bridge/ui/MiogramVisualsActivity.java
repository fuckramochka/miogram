package app.miogram.bridge.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.exteraless.appearance.AppearanceConfig;
import app.miogram.bridge.MiogramFlags;
import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ui.discord.MiogramDiscordLayout;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Clean & unified Miogram Appearance Settings with dynamic multilingual localization.
 * Features:
 * - Interface Mode (Telegram Classic vs Discord Style)
 * - Needy Streamer Overload / Ame-chan Aesthetic
 * - Apple Music 1:1 Player & Live Mini-Bass Visualizer
 * - Liquid Frosted Glass (AGSL) & Avatar Geometry
 */
public class MiogramVisualsActivity extends BaseNekoSettingsActivity {

    private int headerModeRow;
    private int discordUiRow;
    private int ameVibeRow;
    private int appleMusicRow;
    private int miniBassRow;
    private int modeInfoRow;

    private int headerGlassRow;
    private int glassToggleRow;
    private int glassIntensityRow;
    private int glassInfoRow;

    private int headerAvatarsRow;
    private int avatarCornersRow;
    private int singleCornerRadiusRow;
    private int senderMiniAvatarsRow;
    private int avatarsInfoRow;

    private int headerUiRow;
    private int squareFabRow;
    private int titleTextRow;
    private int monetStyleRow;
    private int uiInfoRow;

    private int headerCustomProfileRow;
    private int customProfileEditorRow;
    private int customProfileBubbleRow;
    private int customProfileChatDecorRow;
    private int customProfileBakeRow;
    private int customProfileInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Зовнішній вигляд", "Внешний вид", "Appearance & Design");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerModeRow = addRow();
        discordUiRow = addRow();
        ameVibeRow = addRow();
        appleMusicRow = addRow();
        if (appleMusicEnabled()) {
            miniBassRow = addRow();
        } else {
            miniBassRow = -1;
        }
        modeInfoRow = addRow();

        headerGlassRow = addRow();
        glassToggleRow = addRow();
        if (decorationEnabled()) {
            glassIntensityRow = addRow();
        } else {
            glassIntensityRow = -1;
        }
        glassInfoRow = addRow();

        headerAvatarsRow = addRow();
        avatarCornersRow = addRow();
        singleCornerRadiusRow = addRow();
        senderMiniAvatarsRow = addRow();
        avatarsInfoRow = addRow();

        headerUiRow = addRow();
        squareFabRow = addRow();
        titleTextRow = addRow();
        monetStyleRow = addRow();
        uiInfoRow = addRow();

        headerCustomProfileRow = addRow();
        customProfileEditorRow = addRow();
        customProfileBubbleRow = addRow();
        customProfileChatDecorRow = addRow();
        customProfileBakeRow = addRow();
        customProfileInfoRow = addRow();
    }

    private Context getSafeContext() {
        return getParentActivity() != null ? getParentActivity() : ApplicationLoader.applicationContext;
    }

    private boolean decorationEnabled() {
        return MiogramFlags.isSpatialDecoration()
                || MiogramVisualsPrefs.loadBool(getSafeContext(), "agsl_enabled", false);
    }

    private int intensityPercent() {
        return MiogramVisualsPrefs.loadInt(getSafeContext(), "liquid_glass_intensity", 60);
    }

    private boolean ameVibeEnabled() {
        return MiogramVisualsPrefs.loadBool(getSafeContext(), "ame_vibe_enabled", true);
    }

    private boolean appleMusicEnabled() {
        return MiogramVisualsPrefs.loadBool(getSafeContext(), "apple_music_player", true);
    }

    private boolean miniBassEnabled() {
        return MiogramVisualsPrefs.loadBool(getSafeContext(), "mini_bass_glow", true);
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == discordUiRow) {
            showLayoutModeDialog();
        } else if (position == ameVibeRow) {
            boolean next = !ameVibeEnabled();
            MiogramVisualsPrefs.saveBool(getSafeContext(), "ame_vibe_enabled", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == appleMusicRow) {
            boolean next = !appleMusicEnabled();
            MiogramVisualsPrefs.saveBool(getSafeContext(), "apple_music_player", next);
            updateRows();
            listAdapter.notifyDataSetChanged();
        } else if (position == miniBassRow) {
            boolean next = !miniBassEnabled();
            MiogramVisualsPrefs.saveBool(getSafeContext(), "mini_bass_glow", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == glassToggleRow) {
            boolean enabled = !decorationEnabled();
            MiogramFlags.setSpatialDecoration(enabled);
            MiogramVisualsPrefs.saveBool(getSafeContext(), "agsl_enabled", enabled);
            updateRows();
            listAdapter.notifyDataSetChanged();
            getNotificationCenter().postNotificationName(NotificationCenter.needSetDayNightTheme);
        } else if (position == glassIntensityRow) {
            showIntensityDialog();
        } else if (position == avatarCornersRow) {
            showAvatarCornersDialog();
        } else if (position == singleCornerRadiusRow) {
            boolean next = !AppearanceConfig.singleCornerRadius.Bool();
            AppearanceConfig.singleCornerRadius.setConfigBool(next);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == senderMiniAvatarsRow) {
            boolean next = !AppearanceConfig.senderMiniAvatars.Bool();
            AppearanceConfig.senderMiniAvatars.setConfigBool(next);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == squareFabRow) {
            boolean next = !AppearanceConfig.squareFab.Bool();
            AppearanceConfig.squareFab.setConfigBool(next);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == titleTextRow) {
            showTitleTextDialog();
        } else if (position == monetStyleRow) {
            int cur = AppearanceConfig.monetStyle.Int();
            int next = cur == AppearanceConfig.MONET_STYLE_TELEMONE ? AppearanceConfig.MONET_STYLE_CLASSIC : AppearanceConfig.MONET_STYLE_TELEMONE;
            AppearanceConfig.monetStyle.setConfigInt(next);
            listAdapter.notifyItemChanged(monetStyleRow);
            getNotificationCenter().postNotificationName(NotificationCenter.needSetDayNightTheme);
        } else if (position == customProfileEditorRow) {
            app.miogram.bridge.profile.CustomProfileEngine.openEditor();
        } else if (position == customProfileBubbleRow) {
            app.miogram.bridge.profile.CustomProfileEngine.openBubbleSheet();
        } else if (position == customProfileChatDecorRow) {
            boolean next = !app.miogram.bridge.profile.CustomProfileEngine.flagOf("chat_decor", true);
            app.miogram.bridge.profile.CustomProfileEngine.flagSet("chat_decor", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        } else if (position == customProfileBakeRow) {
            boolean next = !app.miogram.bridge.profile.CustomProfileEngine.flagOf("bake_outside", false);
            app.miogram.bridge.profile.CustomProfileEngine.flagSet("bake_outside", next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next);
        }
    }

    private void showLayoutModeDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Telegram Класичний", "Telegram Классический", "Telegram Classic"),
                MiogramLocale.get("Discord Стиль (Сервери, Папки та Канали)", "Discord Стиль (Сервера, Папки и Каналы)", "Discord Style (Servers & Channels)")
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Режим інтерфейсу", "Режим интерфейса", "Interface Layout Mode"));
        builder.setItems(options, (dialog, which) -> {
            MiogramDiscordLayout.setUiMode(which);
            listAdapter.notifyItemChanged(discordUiRow);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showIntensityDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Інтенсивність рідкого скла", "Интенсивность жидкого стекла", "Liquid Glass Intensity"));

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        int current = intensityPercent();
        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(current + "%");
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueLabel.setTextSize(16);
        valueLabel.setPadding(0, 0, 0, AndroidUtilities.dp(8));

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(100);
        seekBar.setProgress(current);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                valueLabel.setText(progress + "%");
                MiogramVisualsPrefs.saveInt(getSafeContext(), "liquid_glass_intensity", progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        container.addView(valueLabel);
        container.addView(seekBar);
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            listAdapter.notifyItemChanged(glassIntensityRow);
            getNotificationCenter().postNotificationName(NotificationCenter.needSetDayNightTheme);
        });
        showDialog(builder.create());
    }

    private void showAvatarCornersDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                MiogramLocale.get("Круглі (За замовчуванням)", "Круглые (По умолчанию)", "Round (Default)"),
                MiogramLocale.get("Скруглений квадрат (14 dp)", "Скругленный квадрат (14 dp)", "Rounded Squircle (14 dp)"),
                MiogramLocale.get("М'який квадрат (8 dp)", "Мягкий квадрат (8 dp)", "Soft Square (8 dp)"),
                MiogramLocale.get("Квадратні (0 dp)", "Квадратные (0 dp)", "Sharp Square (0 dp)")
        };
        int[] values = new int[]{AppearanceConfig.AVATAR_CORNERS_MAX, 14, 8, 0};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Форма аватарок", "Форма аватарок", "Avatar Shape"));
        builder.setItems(options, (dialog, which) -> {
            AppearanceConfig.avatarCorners.setConfigInt(values[which]);
            listAdapter.notifyItemChanged(avatarCornersRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTitleTextDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                "Miogram",
                MiogramLocale.get("Ім'я користувача (@username)", "Имя пользователя (@username)", "Username (@username)"),
                MiogramLocale.get("Ваше ім'я", "Ваше имя", "Your Name"),
                MiogramLocale.get("Чати", "Чаты", "Chats")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Заголовок списку чатів", "Заголовок списка чатов", "Chat List Header"));
        builder.setItems(options, (dialog, which) -> {
            AppearanceConfig.titleText.setConfigInt(which);
            listAdapter.notifyItemChanged(titleTextRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
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
            if (position == headerModeRow || position == headerGlassRow || position == headerAvatarsRow || position == headerUiRow || position == headerCustomProfileRow) {
                return TYPE_HEADER;
            } else if (position == ameVibeRow || position == appleMusicRow || position == miniBassRow
                    || position == glassToggleRow || position == singleCornerRadiusRow
                    || position == senderMiniAvatarsRow || position == squareFabRow
                    || position == customProfileChatDecorRow || position == customProfileBakeRow) {
                return TYPE_CHECK;
            } else if (position == modeInfoRow || position == glassInfoRow || position == avatarsInfoRow || position == uiInfoRow || position == customProfileInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerModeRow) {
                        cell.setText(MiogramLocale.get("Стиль інтерфейсу та Вайб", "Стиль интерфейса и Вайб", "Interface Style & Aesthetics"));
                    } else if (position == headerGlassRow) {
                        cell.setText(MiogramLocale.get("Рідке скло (Liquid Frosted Glass)", "Жидкое стекло (Liquid Frosted Glass)", "Liquid Frosted Glass (AGSL)"));
                    } else if (position == headerAvatarsRow) {
                        cell.setText(MiogramLocale.get("Аватарки та список чатів", "Аватарки и список чатов", "Avatars & Chat List"));
                    } else if (position == headerUiRow) {
                        cell.setText(MiogramLocale.get("Елементи інтерфейсу та Теми", "Элементы интерфейса и Темы", "UI & Themes"));
                    } else if (position == headerCustomProfileRow) {
                        cell.setText(MiogramLocale.get("Кастомізація профілю (Custom Profile)", "Кастомизация профиля (Custom Profile)", "Custom Profile & Themes"));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == ameVibeRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Вайб Needy Streamer Overload (Ame-chan)", "Вайб Needy Streamer Overload (Ame-chan)", "Needy Streamer Overload Aesthetic (Ame-chan)"), ameVibeEnabled(), true);
                    } else if (position == appleMusicRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Музичний плеєр Apple Music + Spotify", "Музыкальный плеер Apple Music + Spotify", "Apple Music Design + Spotify Ergonomics"), appleMusicEnabled(), true);
                    } else if (position == miniBassRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Живий візуалізатор та міні-баси інтерфейсу", "Живой визуализатор и мини-басы интерфейса", "Live Mini-Bass UI Visualizer"), miniBassEnabled(), false);
                    } else if (position == glassToggleRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Ефект рідкого скла на AGSL", "Эффект жидкого стекла на AGSL", "Liquid glass effect via AGSL"), decorationEnabled(), true);
                    } else if (position == singleCornerRadiusRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Єдине скруглення для форумів", "Единое скругление для форумов", "Uniform forum corner radius"), AppearanceConfig.singleCornerRadius.Bool(), true);
                    } else if (position == senderMiniAvatarsRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Міні-аватарки відправників у чатах", "Мини-аватарки отправителей в чатах", "Mini-avatars of message senders"), AppearanceConfig.senderMiniAvatars.Bool(), false);
                    } else if (position == squareFabRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Квадратна («Squircle») плаваюча кнопка", "Квадратная («Squircle») плавающая кнопка", "Squircle Floating Action Button"), AppearanceConfig.squareFab.Bool(), true);
                    } else if (position == customProfileChatDecorRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Рамки та прикраси в чатах", "Рамки и украшения в чатах", "Frames & decor in chats"), app.miogram.bridge.profile.CustomProfileEngine.flagOf("chat_decor", true), true);
                    } else if (position == customProfileBakeRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Запікання текстур (Baking)", "Запекание текстур (Baking)", "Baking outside profiles"), app.miogram.bridge.profile.CustomProfileEngine.flagOf("bake_outside", false), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == discordUiRow) {
                        String val = MiogramDiscordLayout.getUiMode() == MiogramDiscordLayout.UI_MODE_DISCORD ? "Discord" : "Telegram";
                        cell.setTextAndValue(MiogramLocale.get("Варіант інтерфейсу", "Вариант интерфейса", "Interface Layout Mode"), val, true);
                    } else if (position == glassIntensityRow) {
                        cell.setTextAndValue(MiogramLocale.get("Інтенсивність скла", "Интенсивность стекла", "Glass Intensity"), intensityPercent() + "%", false);
                    } else if (position == avatarCornersRow) {
                        int current = AppearanceConfig.avatarCorners.Int();
                        String val = current == AppearanceConfig.AVATAR_CORNERS_MAX
                                ? MiogramLocale.get("Круглі", "Круглые", "Round")
                                : (current == 0 ? MiogramLocale.get("Квадратні", "Квадратные", "Square") : current + " dp");
                        cell.setTextAndValue(MiogramLocale.get("Форма та скруглення аватарок", "Форма и скругление аватарок", "Avatar Shape & Radius"), val, true);
                    } else if (position == titleTextRow) {
                        int t = AppearanceConfig.titleText.Int();
                        String val = t == 1 ? "@username" : (t == 2 ? MiogramLocale.get("Ім'я", "Имя", "Name") : (t == 3 ? MiogramLocale.get("Чати", "Чаты", "Chats") : "Miogram"));
                        cell.setTextAndValue(MiogramLocale.get("Заголовок головного екрана", "Заголовок главного экрана", "Main Screen Title"), val, true);
                    } else if (position == monetStyleRow) {
                        String val = AppearanceConfig.monetStyle.Int() == AppearanceConfig.MONET_STYLE_TELEMONE
                                ? "Telemone (Material 3)"
                                : MiogramLocale.get("Класичний", "Классический", "Classic");
                        cell.setTextAndValue(MiogramLocale.get("Стиль Monet теми", "Стиль Monet темы", "Monet Theme Style"), val, false);
                    } else if (position == customProfileEditorRow) {
                        cell.setText(MiogramLocale.get("Редактор оформлення профілю", "Редактор оформления профиля", "Profile Banner & Mesh Editor"), true);
                    } else if (position == customProfileBubbleRow) {
                        cell.setText(MiogramLocale.get("Кастомні бульбашки повідомлень", "Кастомные пузырьки сообщений", "Custom Message Bubbles (Sheet)"), false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == modeInfoRow) {
                        cell.setText(MiogramLocale.get("Перемикання між Telegram та Discord структурою (ліва панель гільдій, канали та естетика Ame-chan).",
                                "Переключение между Telegram и Discord структурой (левая панель гильдий, каналы и эстетика Ame-chan).",
                                "Switch between Telegram Classic and Discord layouts with Needy Streamer Overload aesthetic."));
                    } else if (position == glassInfoRow) {
                        cell.setText(MiogramLocale.get("Рідке скло накладає матовий світловий блік та люмінесцентну грань на панель заголовка.",
                                "Жидкое стекло накладывает матовый световой блик и люминесцентную грань на панель заголовка.",
                                "Liquid glass applies a frosted highlight and luminescent edge to top navigation bars."));
                    } else if (position == avatarsInfoRow) {
                        cell.setText(MiogramLocale.get("Налаштовує геометрію аватарок користувачів та каналів у всіх списках додатку.",
                                "Настраивает геометрию аватарок пользователей и каналов во всех списках приложения.",
                                "Configures avatar corner geometry for chats and channels across the entire app."));
                    } else if (position == uiInfoRow) {
                        cell.setText(MiogramLocale.get("Керує динамічним кольоровим оформленням Material You та кнопками дії.",
                                "Управляет динамическим цветовым оформлением Material You и кнопками действия.",
                                "Controls dynamic Material You coloring and action buttons."));
                    } else if (position == customProfileInfoRow) {
                        cell.setText(MiogramLocale.get("Повне налаштування банерів профілю, сіток градієнтів, світяться імен, бульбашок та публікацій у Воркшопі.",
                                "Полная настройка баннеров профиля, сеток градиентов, светящихся имен, пузырьков и публикаций в Воркшопе.",
                                "Customize profile banners, mesh gradients, glowing names, bubble styles, and Cloud Workshop."));
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
