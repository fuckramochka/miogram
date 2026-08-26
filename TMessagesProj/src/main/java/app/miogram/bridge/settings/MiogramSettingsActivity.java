package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import tw.nekomimi.nekogram.settings.NekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;

import app.miogram.bridge.ui.MiogramAiSettingsActivity;
import app.miogram.bridge.ui.MiogramVaultActivity;
import app.miogram.bridge.ui.MiogramVisualsActivity;
import app.miogram.bridge.updater.MiogramUpdater;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import xyz.nextalone.nagram.NaConfig;

/**
 * Main Miogram Settings Hub:
 * - Zero-knowledge Vault & Duress Profiles
 * - Miogram AI (Gemini + Local Whisper)
 * - Authentic exteraGram Plugins Manager
 * - AGSL Liquid Glass Shaders
 * - In-app Miogram Updater (KPM style)
 * - Direct access to exteraless & Nagram feature suites
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    private int headerCoreRow;
    private int vaultRow;
    private int aiRow;
    private int pluginsRow;
    private int visualsRow;
    private int updaterRow;

    private int headerFeaturesRow;
    private int exteraPrefsRow;
    private int nagramPrefsRow;
    private int exteraToggleRow;
    private int nagramToggleRow;
    private int ayuToggleRow;

    @Override
    protected String getActionBarTitle() {
        return "Miogram";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerCoreRow = addRow();
        vaultRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        visualsRow = addRow();
        updaterRow = addRow();

        headerFeaturesRow = addRow();
        exteraPrefsRow = addRow();
        nagramPrefsRow = addRow();

        exteraToggleRow = addRow();
        nagramToggleRow = addRow();
        ayuToggleRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == vaultRow) {
            presentFragment(new MiogramVaultActivity());
        } else if (position == aiRow) {
            presentFragment(new MiogramAiSettingsActivity());
        } else if (position == pluginsRow) {
            presentFragment(new app.exteraless.plugins.ui.PluginsActivity());
        } else if (position == visualsRow) {
            presentFragment(new MiogramVisualsActivity());
        } else if (position == updaterRow) {
            MiogramUpdater.checkAndShowUpdate(this, true);
        } else if (position == exteraPrefsRow) {
            presentFragment(new app.exteraless.settings.OpenExteraSettingsActivity());
        } else if (position == nagramPrefsRow) {
            presentFragment(new NekoSettingsActivity());
        } else if (position == exteraToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showExteraFeatures();
            app.exteraless.general.GeneralConfig.setExteraFeatures(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            listAdapter.notifyItemChanged(exteraPrefsRow);
        } else if (position == nagramToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showNagramSettings();
            app.exteraless.general.GeneralConfig.setNagramSettingsVisible(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == ayuToggleRow) {
            boolean value = !app.exteraless.general.GeneralConfig.showAyuMoments();
            app.exteraless.general.GeneralConfig.setAyuMomentsVisible(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
        }
    }

    private class ListAdapter extends BaseListAdapter {

        private final int TYPE_HEADER = 4;
        private final int TYPE_SETTINGS = 2;
        private final int TYPE_CHECK = 3;

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerCoreRow || position == headerFeaturesRow) {
                return TYPE_HEADER;
            } else if (position == exteraToggleRow || position == nagramToggleRow || position == ayuToggleRow) {
                return TYPE_CHECK;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == vaultRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramVaultTitle), true);
                    } else if (position == aiRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramAITitle), true);
                    } else if (position == pluginsRow) {
                        cell.setText("Плагіни (exteraGram Plugins)", true);
                    } else if (position == visualsRow) {
                        cell.setText("Рідке скло (AGSL Shaders)", true);
                    } else if (position == updaterRow) {
                        cell.setText("Перевірити оновлення Miogram", false);
                    } else if (position == exteraPrefsRow) {
                        cell.setText("Налаштування exteraless", true);
                    } else if (position == nagramPrefsRow) {
                        cell.setText("Налаштування Nagram", false);
                    }
                    break;
                }
                case TYPE_CHECK: {
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
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerCoreRow) {
                        cell.setText("ФУНКЦІЇ MIOGRAM");
                    } else if (position == headerFeaturesRow) {
                        cell.setText("ДОДАТКОВІ МОЖЛИВОСТІ");
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
