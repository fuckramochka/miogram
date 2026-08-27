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
import org.telegram.ui.Components.RecyclerListView;

import app.exteraless.general.GeneralConfig;
import app.exteraless.utils.UtilsConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram Performance & Network Settings:
 * - Multi-threaded Download Speed Boost (12 streams)
 * - Predictive Back Gesture sensitivity & Spring physics
 * - Audio playback auto-pause during recording
 * - Tactile vibration feedback
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
    private int autoPauseMusicRow;
    private int disableVibrationRow;
    private int mediaInfoRow;

    @Override
    protected String getActionBarTitle() {
        return "Продуктивність та Мережа";
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
        autoPauseMusicRow = addRow();
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
        } else if (position == autoPauseMusicRow) {
            boolean v = !NaConfig.INSTANCE.getPauseMusicOnRecord().Bool();
            NaConfig.INSTANCE.getPauseMusicOnRecord().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == disableVibrationRow) {
            boolean v = !NaConfig.INSTANCE.getDisableVibration().Bool();
            NaConfig.INSTANCE.getDisableVibration().setConfigBool(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        }
    }

    private void showDownloadBoostDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        String[] options = new String[]{"Звичайний", "Швидкий (покращений завантажувач)", "Максимальний (12 потоків, 1 MB чанки)"};
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Прискорення завантаження файлів");
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
        builder.setTitle("Чутливість жесту «Назад»");

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        int current = UtilsConfig.predictiveBackIntensity.Int();
        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(current + "%");
        valueLabel.setTextSize(16);
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(200);
        seekBar.setProgress(current);
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

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_CHECK = 1;
        private static final int TYPE_SETTINGS = 2;
        private static final int TYPE_INFO = 3;

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerNetworkRow || position == headerMotionRow || position == headerMediaRow) {
                return TYPE_HEADER;
            } else if (position == springAnimationRow || position == autoPauseMusicRow || position == disableVibrationRow) {
                return TYPE_CHECK;
            } else if (position == networkInfoRow || position == motionInfoRow || position == mediaInfoRow) {
                return TYPE_INFO;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerNetworkRow) {
                        cell.setText("Швидкість завантаження файлів");
                    } else if (position == headerMotionRow) {
                        cell.setText("Пружинні анімації та жести");
                    } else if (position == headerMediaRow) {
                        cell.setText("Медіа та вібровідгук");
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == springAnimationRow) {
                        cell.setTextAndCheck("Пружинний плавний перехід екранів", NaConfig.INSTANCE.getBackAnimationStyle().Int() == 1, true);
                    } else if (position == autoPauseMusicRow) {
                        cell.setTextAndCheck("Ставити музику на паузу при записі голосу", NaConfig.INSTANCE.getPauseMusicOnRecord().Bool(), true);
                    } else if (position == disableVibrationRow) {
                        cell.setTextAndCheck("Вимкнути всі системні вібрації в додатку", NaConfig.INSTANCE.getDisableVibration().Bool(), false);
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == downloadBoostRow) {
                        int b = GeneralConfig.downloadSpeedBoost.Int();
                        String val = b == 2 ? "Максимальний (12 потоків)" : (b == 1 ? "Швидкий" : "Звичайний");
                        cell.setTextAndValue("Режим завантаження", val, false);
                    } else if (position == backSensitivityRow) {
                        cell.setTextAndValue("Чутливість жесту «Назад»", UtilsConfig.predictiveBackIntensity.Int() + "%", false);
                    }
                    break;
                }
                case TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == networkInfoRow) {
                        cell.setText("Багатопотоковий завантажувач використовує максимальну пропускну здатність вашого інтернет-з'єднання.");
                    } else if (position == motionInfoRow) {
                        cell.setText("Керування фізикою пружинних переходів та жестом повернення назад.");
                    } else if (position == mediaInfoRow) {
                        cell.setText("Параметри взаємодії з аудіоплеєром та тактильним відгуком.");
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
