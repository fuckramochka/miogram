package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;

import app.miogram.bridge.ui.MiogramAiSettingsActivity;
import app.miogram.bridge.plugins.WamrWasmRuntime;
import app.miogram.bridge.ui.MiogramPluginsActivity;
import app.miogram.bridge.ui.MiogramVaultSetupActivity;
import app.miogram.bridge.ui.MiogramVisualsActivity;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import xyz.nextalone.nagram.NaConfig;

/**
 * Etalon Telegram-style settings screen (BaseNekoSettingsActivity skeleton:
 * HeaderCell / TextSettingsCell / TextCheckCell / grey info rows).
 *
 * Entry point: SettingsActivity row 102 («Miogram»), replacing the old
 * exteraless Preferences entry — which now lives inside this screen.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    private int headerRow;
    private int vaultRow;
    private int aiRow;
    private int pluginsRow;
    private int visualsRow;
    private int exteraToggleRow;
    private int nagramToggleRow;
    private int ayuToggleRow;
    private int exteraPrefsRow;

    @Override
    protected String getActionBarTitle() {
        return "Miogram";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerRow = addRow();
        vaultRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        visualsRow = addRow();

        exteraToggleRow = addRow();
        nagramToggleRow = addRow();
        ayuToggleRow = addRow();

        exteraPrefsRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == vaultRow) {
            MiogramVaultSetupActivity.start(getParentActivity());
        } else if (position == aiRow) {
            MiogramAiSettingsActivity.start(getParentActivity());
        } else if (position == pluginsRow) {
            MiogramPluginsActivity.start(getParentActivity());
        } else if (position == visualsRow) {
            MiogramVisualsActivity.start(getParentActivity());
        } else if (position == exteraToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showExteraFeatures();
            app.exteraless.general.GeneralConfig.INSTANCE.getShowExteraFeatures().setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            listAdapter.notifyItemChanged(exteraPrefsRow);
        } else if (position == nagramToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showNagramSettings();
            app.exteraless.general.GeneralConfig.INSTANCE.getShowNagramSettings().setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == ayuToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showAyuMoments();
            app.exteraless.general.GeneralConfig.INSTANCE.getShowAyuMoments().setConfigBool(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
        } else if (position == exteraPrefsRow) {
            presentFragment(new app.exteraless.settings.OpenExteraSettingsActivity());
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 2: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == vaultRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramVaultTitle),
                                LocaleController.getString(R.string.MiogramVaultStatus), true);
                    } else if (position == aiRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramAITitle), true);
                    } else if (position == pluginsRow) {
                        cell.setText(WamrWasmRuntime.isAvailable() ? LocaleController.getString(R.string.MiogramPluginsTitle) : LocaleController.getString(R.string.MiogramPluginsNoRuntime), true);
                    } else if (position == visualsRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramVisualsTitle), false);
                    } else if (position == exteraPrefsRow) {
                        cell.setText("exteraless Preferences", false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == exteraToggleRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramToggleExtera),
                                app.exteraless.general.GeneralConfig.showExteraFeatures(), false);
                    } else if (position == nagramToggleRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramToggleNagram),
                                app.exteraless.general.GeneralConfig.showNagramSettings(), true);
                    } else if (position == ayuToggleRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramToggleAyu),
                                app.exteraless.general.GeneralConfig.showAyuMoments(), false);
                    }
                    break;
                }
                case 4: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerRow) {
                        cell.setText("Miogram");
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
