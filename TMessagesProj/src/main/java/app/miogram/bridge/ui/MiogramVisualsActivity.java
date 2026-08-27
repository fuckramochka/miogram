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
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Clean & unified Miogram Appearance Settings:
 * - Liquid Frosted Glass (AGSL Shaders)
 * - Avatar Corner Radius & Single Corner for forums
 * - Mini-avatars in chat list
 * - Squircle / Square FAB button
 * - Chat list title mode (App name / Username / Name / "Chats")
 * - Monet & Material You dynamic themes
 */
public class MiogramVisualsActivity extends BaseNekoSettingsActivity {

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

    @Override
    protected String getActionBarTitle() {
        return "Зовнішній вигляд";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerGlassRow = addRow();
        glassToggleRow = addRow();
        glassIntensityRow = addRow();
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

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == glassToggleRow) {
            boolean enabled = !decorationEnabled();
            MiogramFlags.setSpatialDecoration(enabled);
            MiogramVisualsPrefs.saveBool(getSafeContext(), "agsl_enabled", enabled);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            listAdapter.notifyItemChanged(glassIntensityRow);
            getNotificationCenter().postNotificationName(NotificationCenter.needSetDayNightTheme);
        } else if (position == glassIntensityRow) {
            showIntensitySliderDialog();
        } else if (position == avatarCornersRow) {
            showAvatarCornerDialog();
        } else if (position == singleCornerRadiusRow) {
            boolean value = !AppearanceConfig.singleCornerRadius.Bool();
            AppearanceConfig.singleCornerRadius.setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == senderMiniAvatarsRow) {
            boolean value = !AppearanceConfig.senderMiniAvatars.Bool();
            AppearanceConfig.senderMiniAvatars.setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == squareFabRow) {
            boolean value = !AppearanceConfig.squareFab.Bool();
            AppearanceConfig.squareFab.setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (position == titleTextRow) {
            showTitleTextDialog();
        } else if (position == monetStyleRow) {
            int cur = AppearanceConfig.monetStyle.Int();
            int next = (cur == AppearanceConfig.MONET_STYLE_TELEMONE)
                    ? AppearanceConfig.MONET_STYLE_CLASSIC
                    : AppearanceConfig.MONET_STYLE_TELEMONE;
            AppearanceConfig.monetStyle.setConfigInt(next);
            listAdapter.notifyItemChanged(monetStyleRow);
            getNotificationCenter().postNotificationName(NotificationCenter.needSetDayNightTheme);
        }
    }

    private void showIntensitySliderDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Інтенсивність рідкого скла");

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(intensityPercent() + "%");
        valueLabel.setTextSize(16);
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(100);
        seekBar.setProgress(intensityPercent());
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

    private void showAvatarCornerDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Закруглення аватарок");

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        int current = AppearanceConfig.avatarCorners.Int();
        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(current == AppearanceConfig.AVATAR_CORNERS_MAX ? "Круглі (за замовчуванням)" : (current == 0 ? "Квадратні" : current + " dp"));
        valueLabel.setTextSize(16);
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(AppearanceConfig.AVATAR_CORNERS_MAX);
        seekBar.setProgress(current);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                valueLabel.setText(progress == AppearanceConfig.AVATAR_CORNERS_MAX ? "Круглі" : (progress == 0 ? "Квадратні" : progress + " dp"));
                AppearanceConfig.avatarCorners.setConfigInt(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        container.addView(valueLabel);
        container.addView(seekBar);
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            listAdapter.notifyItemChanged(avatarCornersRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        showDialog(builder.create());
    }

    private void showTitleTextDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] options = new String[]{
                "Miogram",
                "Ім'я користувача (@username)",
                "Ваше ім'я",
                "Чати"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Заголовок списку чатів");
        builder.setItems(options, (dialog, which) -> {
            AppearanceConfig.titleText.setConfigInt(which);
            listAdapter.notifyItemChanged(titleTextRow);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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
            if (position == headerGlassRow || position == headerAvatarsRow || position == headerUiRow) {
                return TYPE_HEADER;
            } else if (position == glassToggleRow || position == singleCornerRadiusRow
                    || position == senderMiniAvatarsRow || position == squareFabRow) {
                return TYPE_CHECK;
            } else if (position == glassInfoRow || position == avatarsInfoRow || position == uiInfoRow) {
                return TYPE_INFO;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerGlassRow) {
                        cell.setText("Рідке скло (Liquid Frosted Glass)");
                    } else if (position == headerAvatarsRow) {
                        cell.setText("Аватарки та список чатів");
                    } else if (position == headerUiRow) {
                        cell.setText("Елементи інтерфейсу та Теми");
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == glassToggleRow) {
                        cell.setTextAndCheck("Ефект рідкого скла на AGSL", decorationEnabled(), true);
                    } else if (position == singleCornerRadiusRow) {
                        cell.setTextAndCheck("Єдине скруглення для форумів", AppearanceConfig.singleCornerRadius.Bool(), true);
                    } else if (position == senderMiniAvatarsRow) {
                        cell.setTextAndCheck("Міні-аватарки відправників у чатах", AppearanceConfig.senderMiniAvatars.Bool(), false);
                    } else if (position == squareFabRow) {
                        cell.setTextAndCheck("Квадратна («Squircle») плаваюча кнопка", AppearanceConfig.squareFab.Bool(), true);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == glassIntensityRow) {
                        cell.setTextAndValue("Інтенсивність скла", intensityPercent() + "%", false);
                    } else if (position == avatarCornersRow) {
                        int current = AppearanceConfig.avatarCorners.Int();
                        String val = current == AppearanceConfig.AVATAR_CORNERS_MAX ? "Круглі" : (current == 0 ? "Квадратні" : current + " dp");
                        cell.setTextAndValue("Форма та скруглення аватарок", val, true);
                    } else if (position == titleTextRow) {
                        int t = AppearanceConfig.titleText.Int();
                        String val = t == 1 ? "@username" : (t == 2 ? "Ім'я" : (t == 3 ? "Чати" : "Miogram"));
                        cell.setTextAndValue("Заголовок головного екрана", val, true);
                    } else if (position == monetStyleRow) {
                        String val = AppearanceConfig.monetStyle.Int() == AppearanceConfig.MONET_STYLE_TELEMONE ? "Telemone (Material 3)" : "Класичний";
                        cell.setTextAndValue("Стиль Monet теми", val, false);
                    }
                    break;
                }
                case TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == glassInfoRow) {
                        cell.setText("Рідке скло накладає матовий світловий блік та люмінесцентну грань на панель заголовка.");
                    } else if (position == avatarsInfoRow) {
                        cell.setText("Налаштовує геометрію аватарок користувачів та каналів у всіх списках додатку.");
                    } else if (position == uiInfoRow) {
                        cell.setText("Керує динамічним кольоровим оформленням Material You та кнопками дії.");
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
                case TYPE_SETTINGS: view = new TextSettingsCell(mContext); break;
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
