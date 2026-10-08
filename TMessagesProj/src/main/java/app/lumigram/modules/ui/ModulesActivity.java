package app.lumigram.modules.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.List;

import app.lumigram.api.ModuleInfo;
import app.lumigram.modules.ModuleManager;
import app.lumigram.modules.ModuleRegistry;
import app.lumigram.modules.ModulesCatalog;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Екран «Модулі» LumiGram: встановлені (увімкнути/видалити),
 * екрани модулів (відкрити), каталог з хмари (скачати).
 * Без модулів ядро — майже чистий TG: цей екран порожній, і це нормально.
 */
public class ModulesActivity extends BaseNekoSettingsActivity {

    private int installedHeaderRow = -1;
    private int installedStartRow = -1;
    private int installedEndRow = -1;
    private int installedEmptyRow = -1;
    private int removeRow = -1;
    private int divider1Row = -1;

    private int screensHeaderRow = -1;
    private int screensStartRow = -1;
    private int screensEndRow = -1;
    private int screensEmptyRow = -1;
    private int divider2Row = -1;

    private int catalogHeaderRow = -1;
    private int checkRow = -1;
    private int catalogStartRow = -1;
    private int catalogEndRow = -1;
    private int catalogInfoRow = -1;

    private final List<ModuleInfo> installed = new ArrayList<>();
    private final List<ModulesCatalog.Entry> catalog = new ArrayList<>();
    private String status = null;
    private boolean fetching = false;

    @Override
    public boolean onFragmentCreate() {
        reloadInstalled();
        ModuleManager mgr = ModuleManager.getInstance();
        if (mgr == null) {
            try {
                mgr = ModuleManager.init(getParentActivity());
            } catch (Exception ignored) {
            }
        }
        return super.onFragmentCreate();
    }

    private void reloadInstalled() {
        installed.clear();
        ModuleManager mgr = ModuleManager.getInstance();
        if (mgr != null) {
            installed.addAll(mgr.installed());
        }
    }

    private void refresh() {
        reloadInstalled();
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private boolean isInstalledRow(int position) {
        return installedStartRow >= 0 && position >= installedStartRow && position < installedStartRow + installed.size();
    }

    private boolean isCatalogRow(int position) {
        return catalogStartRow >= 0 && position >= catalogStartRow && position < catalogStartRow + catalog.size();
    }

    private boolean isScreenRow(int position) {
        List<ModuleRegistry.ScreenEntry> screens = ModuleRegistry.screensSnapshot();
        return screensStartRow >= 0 && position >= screensStartRow && position < screensStartRow + screens.size();
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        installedHeaderRow = addRow("installedHeader");
        installedStartRow = nextRowIndex();
        for (int i = 0; i < installed.size(); i++) {
            addRow("installed_" + installed.get(i).id);
        }
        installedEndRow = nextRowIndex();
        if (installed.isEmpty()) {
            installedEmptyRow = addRow("installedEmpty");
        } else {
            installedEmptyRow = -1;
        }
        removeRow = installed.isEmpty() ? -1 : addRow("remove");
        divider1Row = addRow();

        List<ModuleRegistry.ScreenEntry> screens = ModuleRegistry.screensSnapshot();
        screensHeaderRow = addRow("screensHeader");
        screensStartRow = nextRowIndex();
        for (ModuleRegistry.ScreenEntry s : screens) {
            addRow("screen_" + s.screenId);
        }
        screensEndRow = nextRowIndex();
        if (screens.isEmpty()) {
            screensEmptyRow = addRow("screensEmpty");
        } else {
            screensEmptyRow = -1;
        }
        divider2Row = addRow();

        catalogHeaderRow = addRow("catalogHeader");
        checkRow = addRow("check");
        catalogStartRow = nextRowIndex();
        for (ModulesCatalog.Entry e : catalog) {
            addRow("catalog_" + e.id);
        }
        catalogEndRow = nextRowIndex();
        catalogInfoRow = addRow("catalogInfo");
    }

    /** Наступний індекс рядка (поле rowCount веде Base). */
    private int nextRowIndex() {
        return rowCount;
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.LumiModules);
    }

    @Override
    public int getSearchGuid() {
        return 26000;
    }

    @Override
    public int getSearchIcon() {
        return R.drawable.msg_download;
    }

    @Override
    public String getSearchPrefix() {
        return "LumiModules";
    }

    @Override
    protected String getKey() {
        return "lumigram_modules";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        ModuleManager mgr = ModuleManager.getInstance();
        if (isInstalledRow(position)) {
            if (mgr == null) {
                return;
            }
            ModuleInfo info = installed.get(position - installedStartRow);
            boolean enabled = !mgr.isEnabled(info.id);
            mgr.setEnabled(info.id, enabled);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            refresh();
        } else if (position == removeRow) {
            if (mgr == null || installed.isEmpty()) {
                return;
            }
            CharSequence[] items = new CharSequence[installed.size()];
            for (int i = 0; i < installed.size(); i++) {
                items[i] = installed.get(i).title + " · " + installed.get(i).version;
            }
            new AlertDialog.Builder(getParentActivity())
                    .setTitle(getString(R.string.LumiModulesRemove))
                    .setItems(items, (dialog, which) -> {
                        mgr.uninstall(installed.get(which).id);
                        refresh();
                    })
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .show();
        } else if (isScreenRow(position)) {
            List<ModuleRegistry.ScreenEntry> screens = ModuleRegistry.screensSnapshot();
            ModuleRegistry.ScreenEntry s = screens.get(position - screensStartRow);
            try {
                presentFragment(s.factory.create());
            } catch (Exception e) {
                BulletinFactory.of(this).createErrorBulletin(s.title).show();
            }
        } else if (position == checkRow) {
            if (fetching) {
                return;
            }
            fetching = true;
            status = getString(R.string.LumiModulesChecking);
            listAdapter.notifyDataSetChanged();
            ModulesCatalog.fetch(null, (entries, failed) -> {
                fetching = false;
                catalog.clear();
                if (!failed) {
                    catalog.addAll(entries);
                }
                status = failed ? getString(R.string.LumiModulesCheckFailed)
                        : entries.isEmpty() ? getString(R.string.LumiModulesEmpty)
                        : null;
                refresh();
            });
        } else if (isCatalogRow(position)) {
            if (mgr == null) {
                return;
            }
            ModulesCatalog.Entry e = catalog.get(position - catalogStartRow);
            if (mgr.isInstalled(e.id) && e.version.equals(mgr.installedVersion(e.id))) {
                return;
            }
            status = e.title + " …";
            listAdapter.notifyDataSetChanged();
            mgr.downloadAndInstall(e, new ModuleManager.DownloadListener() {
                @Override
                public void onProgress(long done, long total) {
                    status = AndroidUtilities.formatFileSize(done) + " / " + AndroidUtilities.formatFileSize(total);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFinished(boolean success, boolean canceled) {
                    status = success ? getString(R.string.LumiModulesInstalled)
                            : canceled ? null : getString(R.string.LumiModulesInstallFailed);
                    refresh();
                }
            });
        }
    }

    private class ListAdapter extends BaseListAdapter {

        ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == installedHeaderRow) {
                        cell.setText(getString(R.string.LumiModulesInstalledHeader));
                    } else if (position == screensHeaderRow) {
                        cell.setText(getString(R.string.LumiModulesScreens));
                    } else if (position == catalogHeaderRow) {
                        cell.setText(getString(R.string.LumiModulesCatalog));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (isInstalledRow(position)) {
                        ModuleManager mgr = ModuleManager.getInstance();
                        ModuleInfo info = installed.get(position - installedStartRow);
                        boolean enabled = mgr != null && mgr.isEnabled(info.id);
                        cell.setTextAndCheck(info.title, info.version, enabled, true);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    cell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    if (position == removeRow) {
                        cell.setTextAndIcon(getString(R.string.LumiModulesRemove), R.drawable.msg_delete, false);
                    } else if (isScreenRow(position)) {
                        ModuleRegistry.ScreenEntry s = ModuleRegistry.screensSnapshot().get(position - screensStartRow);
                        cell.setTextAndIcon(s.title, R.drawable.msg_settings_old, true);
                    } else if (position == checkRow) {
                        cell.setTextAndIcon(getString(R.string.LumiModulesCheck), R.drawable.msg_download, false);
                    } else if (isCatalogRow(position)) {
                        ModulesCatalog.Entry e = catalog.get(position - catalogStartRow);
                        ModuleManager mgr = ModuleManager.getInstance();
                        String current = mgr == null ? null : mgr.installedVersion(e.id);
                        String value = current != null && current.equals(e.version)
                                ? getString(R.string.LumiModulesUpToDate)
                                : e.version;
                        cell.setTextAndValueAndIcon(e.title, value, R.drawable.msg_download, true);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    String text;
                    if (position == installedEmptyRow) {
                        text = getString(R.string.LumiModulesNoneInfo);
                    } else if (position == screensEmptyRow) {
                        text = getString(R.string.LumiModulesScreensEmpty);
                    } else if (position == catalogInfoRow) {
                        text = status != null ? status : getString(R.string.LumiModulesInfo);
                    } else {
                        text = "";
                    }
                    cell.setText(text);
                    cell.setBackground(Theme.getThemedDrawable(mContext,
                            R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == installedHeaderRow || position == screensHeaderRow || position == catalogHeaderRow) {
                return TYPE_HEADER;
            }
            if (isInstalledRow(position)) {
                return TYPE_CHECK;
            }
            if (position == catalogInfoRow || position == installedEmptyRow || position == screensEmptyRow
                    || position == divider1Row || position == divider2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == divider1Row || position == divider2Row
                    || position == installedEmptyRow || position == screensEmptyRow
                    || position == catalogInfoRow) {
                return false;
            }
            return super.isEnabled(holder);
        }
    }
}
