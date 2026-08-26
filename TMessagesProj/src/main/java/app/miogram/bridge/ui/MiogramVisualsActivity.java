package app.miogram.bridge.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import app.miogram.bridge.MiogramFlags;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Native Telegram-style AGSL liquid-glass tuning with smooth percentage SeekBar.
 */
public class MiogramVisualsActivity extends BaseNekoSettingsActivity {

    private int decorationHeaderRow;
    private int decorationToggleRow;
    private int intensityRow;
    private int noteRow;
    private int shadowRow;

    @Override
    protected String getActionBarTitle() {
        return "Рідке скло (AGSL)";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        decorationHeaderRow = rowCount++;
        decorationToggleRow = rowCount++;
        intensityRow = rowCount++;
        noteRow = rowCount++;
        shadowRow = rowCount++;
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
        if (position == decorationToggleRow) {
            boolean enabled = !decorationEnabled();
            MiogramFlags.setSpatialDecoration(enabled);
            MiogramVisualsPrefs.saveBool(getSafeContext(), "agsl_enabled", enabled);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            listAdapter.notifyItemChanged(intensityRow);
            toast(enabled ? "Рідке скло увімкнено" : "Рідке скло вимкнено");
        } else if (position == intensityRow) {
            showIntensitySliderDialog();
        }
    }

    private void showIntensitySliderDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Інтенсивність рідкого скла");

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        TextView valueLabel = new TextView(ctx);
        valueLabel.setText(intensityPercent() + "%");
        valueLabel.setTextSize(18);
        valueLabel.setTypeface(null, Typeface.BOLD);
        valueLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        valueLabel.setGravity(Gravity.CENTER);

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(100);
        seekBar.setProgress(intensityPercent());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                valueLabel.setText(progress + "%");
                MiogramVisualsPrefs.saveInt(getSafeContext(), "liquid_glass_intensity", progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        container.addView(valueLabel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(seekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        builder.setView(container);
        builder.setPositiveButton("Готово", (d, w) -> {
            listAdapter.notifyItemChanged(intensityRow);
        });
        showDialog(builder.create());
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == decorationHeaderRow) return 4;
            if (position == decorationToggleRow) return 3;
            if (position == noteRow || position == shadowRow) return 7;
            return 2;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (getItemViewType(position)) {
                case 4 -> {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(position == decorationHeaderRow ? "НАЛАШТУВАННЯ ДИЗАЙНУ" : "");
                }
                case 3 -> {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    cell.setTextAndCheck("Увімкнути ефект рідкого скла", decorationEnabled(), false);
                }
                case 7 -> {
                    holder.itemView.setBackground(Theme.getThemedDrawable(
                            mContext, R.drawable.greydivider_bottom,
                            Theme.key_windowBackgroundGrayShadow));
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText("Ефект рідкого скла використовує апаратні шейдери AGSL (доступні на Android 13+). Створює реалістичне заломлення світла та глибину інтерфейсу.");
                }
                default -> {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == intensityRow) {
                        cell.setText("Інтенсивність скла · " + intensityPercent() + "%", false);
                    }
                }
            }
        }
    }

    private void toast(String message) {
        if (getParentActivity() != null) {
            android.widget.Toast.makeText(getParentActivity(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
