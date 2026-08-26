package app.miogram.bridge.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.miogram.core.plugins.MiogramPluginEngine;
import app.miogram.core.plugins.InMemoryPluginRepository;
import app.miogram.core.plugins.InMemoryTrustAnchors;
import app.miogram.core.plugins.InstalledPlugin;
import app.miogram.core.plugins.PluginState;
import app.miogram.bridge.plugins.WamrWasmRuntime;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

/**
 * Native Telegram-style WASM plugin manager.
 *
 * Install flow: pick code (.wasm) and an Ed25519-signed manifest (.manifest),
 * then install — unsigned modules are rejected by the engine before any guest
 * byte executes.
 *
 * Row model: fixed rows first, then per-plugin triplets
 * [toggle · ping · uninstall] rebuilt on every updateRows().
 */
public class MiogramPluginsActivity extends BaseNekoSettingsActivity {

    private static final int PICK_CODE = 61;
    private static final int PICK_MANIFEST = 62;

    private final InMemoryPluginRepository repository = new InMemoryPluginRepository();
    private final MiogramPluginEngine engine = new MiogramPluginEngine(
            repository, WamrWasmRuntime.INSTANCE, new InMemoryTrustAnchors());

    private byte[] pendingCode;
    private byte[] pendingManifest;

    private int headerRow;
    private int runtimeInfoRow;
    private int pickCodeRow;
    private int pickManifestRow;
    private int installRow;
    private int installedHeaderRow;
    private int shadowRow;
    private int firstPluginRow = Integer.MAX_VALUE;

    /** position -> pluginId|verb for dynamic plugin section */
    private final HashMap<Integer, String> actionByRow = new HashMap<>();
    /** position -> pluginId for the title row of each triplet */
    private final HashMap<Integer, String> titleByRow = new HashMap<>();

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.MiogramPluginsTitle);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerRow = rowCount++;
        runtimeInfoRow = rowCount++;
        pickCodeRow = rowCount++;
        pickManifestRow = rowCount++;
        installRow = rowCount++;
        installedHeaderRow = rowCount++;

        actionByRow.clear();
        titleByRow.clear();

        List<InstalledPlugin> plugins = repository.list();
        for (InstalledPlugin p : plugins) {
            int titleRow = rowCount++;
            int toggleRow = rowCount++;
            int pingRow = rowCount++;
            int uninstallRow = rowCount++;
            titleByRow.put(titleRow, p.getPluginId());
            actionByRow.put(toggleRow, pluginId(p) + "|toggle");
            actionByRow.put(pingRow, pluginId(p) + "|ping");
            actionByRow.put(uninstallRow, pluginId(p) + "|uninstall");
        }
        shadowRow = rowCount++;
        firstPluginRow = installedHeaderRow + 1;
    }

    private static String pluginId(InstalledPlugin p) {
        return p.getPluginId();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == pickCodeRow) {
            pick(PICK_CODE);
        } else if (position == pickManifestRow) {
            pick(PICK_MANIFEST);
        } else if (position == installRow) {
            installPicked();
            return;
        }

        String action = actionByRow.get(position);
        if (action == null) return;
        String pluginId = action.substring(0, action.indexOf('|'));
        String verb = action.substring(action.indexOf('|') + 1);

        switch (verb) {
            case "toggle" -> {
                InstalledPlugin p = findPlugin(pluginId);
                if (p == null) return;
                if (p.getState() == PluginState.ENABLED) {
                    engine.disable(pluginId);
                } else {
                    var res = engine.enable(pluginId);
                    if (res instanceof MiogramPluginEngine.EnableResult.Failed failed) {
                        toast(failed.getReason());
                    }
                }
                reload();
            }
            case "ping" -> runPing(pluginId);
            case "uninstall" -> {
                engine.uninstall(pluginId);
                reload();
            }
        }
    }

    private void runPing(String pluginId) {
        long t0 = System.nanoTime();
        var outcome = engine.dispatch(pluginId, "ping", null);
        long us = (System.nanoTime() - t0) / 1000;
        if (outcome instanceof MiogramPluginEngine.DispatchOutcome.Ok) {
            toast("pong · " + us + " µs");
        } else if (outcome instanceof MiogramPluginEngine.DispatchOutcome.Denied denied) {
            toast("denied: " + denied.getReason());
        } else {
            toast("fault");
        }
        reload();
    }

    private void reload() {
        updateRows();
        if (listAdapter != null) listAdapter.notifyDataSetChanged();
    }

    // --- SAF -----------------------------------------------------------------

    private void pick(int which) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        getParentActivity().startActivityForResult(intent, which);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        try (var in = getParentActivity().getContentResolver().openInputStream(data.getData())) {
            byte[] bytes = in != null ? in.readAllBytes() : new byte[0];
            if (requestCode == PICK_CODE) pendingCode = bytes;
            if (requestCode == PICK_MANIFEST) pendingManifest = bytes;
            toast(LocaleController.getString(R.string.MiogramFileLoaded));
        } catch (Exception e) {
            toast("read error");
        }
    }

    private void installPicked() {
        if (pendingCode == null || pendingManifest == null) {
            toast(LocaleController.getString(R.string.MiogramPickBothFirst));
            return;
        }
        var verdict = engine.install(pendingManifest, pendingCode);
        if (verdict instanceof MiogramPluginEngine.InstallResult.Installed installed) {
            toast(LocaleController.getString(R.string.MiogramInstalled));
            reload();
        } else if (verdict instanceof MiogramPluginEngine.InstallResult.Rejected rejected) {
            toast(rejected.getReason() + (rejected.getDetail() != null ? ": " + rejected.getDetail() : ""));
        }
    }

    private InstalledPlugin findPlugin(String id) {
        for (InstalledPlugin p : repository.list()) {
            if (p.getPluginId().equals(id)) return p;
        }
        return null;
    }

    // --- adapter ---------------------------------------------------------------

    private void toast(String message) {
        if (getParentActivity() != null) {
            android.widget.Toast.makeText(getParentActivity(), message,
                    android.widget.Toast.LENGTH_SHORT).show();
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
            if (position == headerRow || position == installedHeaderRow) return 4;
            if (position == runtimeInfoRow || position == shadowRow) return 7;
            return 2;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (getItemViewType(position)) {
                case 7 -> {
                    holder.itemView.setBackground(Theme.getThemedDrawable(
                            mContext, R.drawable.greydivider_bottom,
                            Theme.key_windowBackgroundGrayShadow));
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setText(WamrWasmRuntime.INSTANCE.isAvailable()
                            ? LocaleController.getString(R.string.MiogramRuntimeReady)
                            : LocaleController.getString(R.string.MiogramRuntimeMissing));
                }
                case 4 -> {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(position == headerRow
                            ? LocaleController.getString(R.string.MiogramInstallHeader)
                            : LocaleController.getString(R.string.MiogramInstalledHeader));
                }
                default -> bindSettingsCell((TextSettingsCell) holder.itemView, position);
            }
        }

        private void bindSettingsCell(TextSettingsCell cell, int position) {
            cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

            if (position == pickCodeRow) {
                cell.setText(LocaleController.getString(R.string.MiogramPickCode), false);
            } else if (position == pickManifestRow) {
                cell.setText(LocaleController.getString(R.string.MiogramPickManifest), false);
            } else if (position == installRow) {
                cell.setText(LocaleController.getString(R.string.MiogramInstall), true);
                return;
            }

            String titleByRowId = titleByRow.get(position);
            if (titleByRowId != null) {
                InstalledPlugin p = findPlugin(titleByRowId);
                if (p != null) {
                    cell.setText(p.getDisplayName() + " · v" + p.getVersionCode()
                            + "  [" + p.getState() + "]", true);
                }
                return;
            }

            String action = actionByRow.get(position);
            if (action == null) return;
            InstalledPlugin p = findPlugin(pluginIdOf(action));
            if (p == null) return;
            String verb = verbOf(action);

            switch (verb) {
                case "toggle" -> cell.setText(
                        p.getState() == PluginState.ENABLED
                                ? LocaleController.getString(R.string.Disable)
                                : LocaleController.getString(R.string.Enable),
                        false);
                case "ping" -> cell.setText("Ping / Echo", false);
                case "uninstall" -> {
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
                    cell.setText(LocaleController.getString(R.string.Delete), false);
                }
            }
        }

        private String pluginIdOf(String action) {
            return action.substring(0, action.indexOf('|'));
        }

        private String verbOf(String action) {
            return action.substring(action.indexOf('|') + 1);
        }
    }
}
