package app.miogram.bridge.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.exteraless.general.GeneralConfig;
import app.exteraless.utils.UtilsConfig;
import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Performance & Network Settings with dynamic multilingual localization.
 */
public class MiogramPerformanceActivity extends BaseNekoSettingsActivity {

    private int headerNetworkRow;
    private int downloadBoostRow;
    private int networkInfoRow;

    private int headerMotionRow;
    private int springAnimationRow;
    private int backSensitivityRow;
    private int motionInfoRow;

    private int headerMediaRow;
    private int autoPauseVideoRow;
    private int disableVibrationRow;
    private int mediaInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Продуктивність та Мережа", "Производительность и Сеть", "Performance & Network");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerNetworkRow = addRow();
        downloadBoostRow = addRow();
        networkInfoRow = addRow();

        headerMotionRow = addRow();
        springAnimationRow = addRow();
        backSensitivityRow = addRow();
        motionInfoRow = addRow();

        headerMediaRow = addRow();
        autoPauseVideoRow = addRow();
        disableVibrationRow = addRow();
        mediaInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == downloadBoostRow) {
            showDownloadBoostDialog();
        } else if (position == springAnimationRow) {
            int cur = NaConfig.INSTANCE.getBackAnimationStyle().Int();
            int next = cur == 1 ? 0 : 1; // 1 = SPRING, 0 = CLASSIC
            NaConfig.INSTANCE.getBackAnimationStyle().setConfigInt(next);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(next == 1);
        } else if (position == backSensitivityRow) {
            showSensitivityDialog();
        } else if (position == autoPauseVideoRow) {
            boolean v = !NekoConfig.autoPauseVideo.Bool();
            NekoConfig.autoPauseVideo.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == disableVibrationRow) {
            boolean v = !NekoConfig.disableVibration.Bool();
            NekoConfig.disableVibration.setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        }
    }

    private void showDownloadBoostDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        String[] options = new String[]{
                MiogramLocale.get("Звичайний", "Обычный", "Normal"),
                MiogramLocale.get("Швидкий (покращений завантажувач)", "Быстрый (улучшенный загрузчик)", "Fast (Enhanced Downloader)"),
                MiogramLocale.get("Максимальний (12 потоків, 1 MB чанки)", "Максимальный (12 потоков, 1 MB чанки)", "Maximum (12 Threads, 1 MB Chunks)")
        };
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(MiogramLocale.get("Прискорення завантаження файлів", "Ускорение загрузки файлов", "Download Speed Boost"));
        b.setItems(options, (dialog, which) -> {
            GeneralConfig.downloadSpeedBoost.setConfigInt(which);
            listAdapter.notifyItemChanged(downloadBoostRow);
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void showSensitivityDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Чутливість жесту «Назад»", "Чувствительность жеста «Назад»", "Back Gesture Sensitivity"));

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        int cur = UtilsConfig.predictiveBackIntensity.Int();
        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(cur + "%");
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueLabel.setTextSize(16);
        valueLabel.setPadding(0, 0, 0, AndroidUtilities.dp(8));

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(100);
        seekBar.setProgress(cur);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                valueLabel.setText(progress + "%");
                UtilsConfig.predictiveBackIntensity.setConfigInt(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        container.addView(valueLabel);
        container.addView(seekBar);
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            listAdapter.notifyItemChanged(backSensitivityRow);
        });
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerNetworkRow || position == headerMotionRow || position == headerMediaRow) {
                return TYPE_HEADER;
            } else if (position == springAnimationRow || position == autoPauseVideoRow || position == disableVibrationRow) {
                return TYPE_CHECK;
            } else if (position == networkInfoRow || position == motionInfoRow || position == mediaInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerNetworkRow) {
                        cell.setText(MiogramLocale.get("Швидкість завантаження файлів", "Скорость загрузки файлов", "Download Speed"));
                    } else if (position == headerMotionRow) {
                        cell.setText(MiogramLocale.get("Пружинні анімації та жести", "Пружинные анимации и жесты", "Spring Animations & Gestures"));
                    } else if (position == headerMediaRow) {
                        cell.setText(MiogramLocale.get("Медіа та вібровідгук", "Медиа и виброотклик", "Media & Haptics"));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == springAnimationRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Пружинний плавний перехід екранів", "Пружинный плавный переход экранов", "Physics-based spring transition"), NaConfig.INSTANCE.getBackAnimationStyle().Int() == 1, true);
                    } else if (position == autoPauseVideoRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Ставити відео на паузу при згортанні", "Ставить видео на паузу при сворачивании", "Pause video when minimized"), NekoConfig.autoPauseVideo.Bool(), true);
                    } else if (position == disableVibrationRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Вимкнути всі системні вібрації в додатку", "Выключить все системные вибрации в приложении", "Disable all in-app vibrations"), NekoConfig.disableVibration.Bool(), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == downloadBoostRow) {
                        int b = GeneralConfig.downloadSpeedBoost.Int();
                        String val = b == 2 ? MiogramLocale.get("Максимальний (12 потоків)", "Максимальный (12 потоков)", "Maximum (12 Threads)") : (b == 1 ? MiogramLocale.get("Швидкий", "Быстрый", "Fast") : MiogramLocale.get("Звичайний", "Обычный", "Normal"));
                        cell.setTextAndValue(MiogramLocale.get("Режим завантаження", "Режим загрузки", "Download Boost Mode"), val, false);
                    } else if (position == backSensitivityRow) {
                        cell.setTextAndValue(MiogramLocale.get("Чутливість жесту «Назад»", "Чувствительность жеста «Назад»", "Back Gesture Sensitivity"), UtilsConfig.predictiveBackIntensity.Int() + "%", false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == networkInfoRow) {
                        cell.setText(MiogramLocale.get("Багатопотоковий завантажувач використовує максимальну пропускну здатність вашого інтернет-з'єднання.",
                                "Многопоточный загрузчик использует максимальную пропускную способность вашего интернет-соединения.",
                                "Multi-threaded downloader utilizes maximum bandwidth of your connection."));
                    } else if (position == motionInfoRow) {
                        cell.setText(MiogramLocale.get("Керування фізикою пружинних переходів та жестом повернення назад.",
                                "Управление физикой пружинных переходов и жестом возврата назад.",
                                "Controls spring animation physics and predictive back gestures."));
                    } else if (position == mediaInfoRow) {
                        cell.setText(MiogramLocale.get("Параметри взаємодії з відеоплеєром та тактильним відгуком.",
                                "Параметры взаимодействия с видеоплеером и тактильным откликом.",
                                "Settings for video player auto-pause and haptic vibration feedback."));
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
