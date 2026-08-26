package app.miogram.bridge.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.Theme;

import app.miogram.bridge.MiogramFlags;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

/**
 * Native Telegram-style AGSL liquid-glass tuning.
 *
 * Rows: enable toggle, intensity cycler (20% steps), info shadow. The runtime
 * flag flips instantly and the choice persists into miogram_visuals prefs so
 * the decoration view restores it on next inflation.
 */
public class MiogramVisualsActivity extends BaseNekoSettingsActivity {

    private int decorationHeaderRow;
    private int decorationToggleRow;
    private int intensityRow;
    private int noteRow;
    private int shadowRow;

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.MiogramVisualsTitle);
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

    private boolean decorationEnabled() {
        return MiogramFlags.isSpatialDecoration()
                || MiogramVisualsPrefs.loadBool(getParentActivity(), "agsl_enabled", false);
    }

    private int intensityPercent() {
        return MiogramVisualsPrefs.loadInt(getParentActivity(), "liquid_glass_intensity", 60);
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == decorationToggleRow) {
            boolean enabled = !decorationEnabled();
            MiogramFlags.setSpatialDecoration(enabled);
            MiogramVisualsPrefs.saveBool(getParentActivity(), "agsl_enabled", enabled);
            listAdapter.notifyItemChanged(decorationToggleRow);
            listAdapter.notifyItemChanged(intensityRow);
            toast(enabled
                    ? LocaleController.getString(R.string.MiogramEnabled)
                    : LocaleController.getString(R.string.MiogramDisabled));
        } else if (position == intensityRow) {
            int next = ((intensityPercent() + 20) % 120);
            if (next == 0) next = 20;
            MiogramVisualsPrefs.saveInt(getParentActivity(), "liquid_glass_intensity", next);
            listAdapter.notifyItemChanged(intensityRow);
        }
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
            if (position == noteRow || position == shadowRow) return 7;
            return 2;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case 4 -> {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(position == decorationHeaderRow
                            ? LocaleController.getString(R.string.MiogramVisualsTitle)
                            : "");
                }
                case 7 -> {
                    holder.itemView.setBackground(Theme.getThemedDrawable(
                            mContext, R.drawable.greydivider_bottom,
                            Theme.key_windowBackgroundGrayShadow));
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText(position == noteRow
                            ? LocaleController.getString(R.string.MiogramVisualsNote)
                            : "");
                }
                default -> {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == decorationToggleRow) {
                        String stateText = decorationEnabled()
                                ? LocaleController.getString(R.string.MiogramEnabled)
                                : LocaleController.getString(R.string.MiogramDisabled);
                        cell.setText(LocaleController.getString(R.string.MiogramAgslToggle)
                                + ": " + stateText, false);
                        if (Build.VERSION.SDK_INT < 33) {
                            cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                        }
                    } else if (position == intensityRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramIntensity)
                                + ": " + intensityPercent() + "%", false);
                    }
                }
            }
        }
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void start(android.content.Context context) {
        context.startActivity(
            new android.content.Intent(context, MiogramVisualsActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }
}
