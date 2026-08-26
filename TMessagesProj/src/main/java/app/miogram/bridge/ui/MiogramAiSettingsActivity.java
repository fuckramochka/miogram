package app.miogram.bridge.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;

import java.io.File;

import app.miogram.bridge.ai.GeminiCloudClient;
import app.miogram.bridge.ai.MiogramAiFacade;
import app.miogram.bridge.ai.MiogramAiRuntime;
import app.miogram.bridge.ai.LocalSttEngine;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import xyz.nextalone.nagram.NaConfig;

/**
 * Native Telegram-style AI settings: BYOK key vault, cloud model picker,
 * privacy shield, metered guard and on-device Whisper STT manager.
 */
public class MiogramAiSettingsActivity extends BaseNekoSettingsActivity {

    private static final String PREFS = "miogram_ai_prefs";
    private static final int PICK_WHISPER_FILE = 77;

    private int headerRow;
    private int keyRow;
    private int modelRow;
    private int piiRow;
    private int meteredRow;
    private int whisperHeaderRow;
    private int whisperModelRow;
    private int whisperDownloadRow;
    private int offlineRow;
    private int shadowRow;

    private MiogramAiFacade facade;
    private LocalSttEngine stt;
    private GeminiCloudClient client;

    private long activeDownloadId = -1;
    private String activeDownloadModelId;
    private BroadcastReceiver downloadReceiver;

    @Override
    protected String getActionBarTitle() {
        return "Miogram AI";
    }

    @Override
    public boolean onFragmentCreate() {
        facade = MiogramAiRuntime.get(getParentActivity() != null ? getParentActivity() : null);
        if (facade == null) {
            // parent activity can be null only in exotic previews; retry later
            return super.onFragmentCreate();
        }
        stt = MiogramAiRuntime.stt(getParentActivity());
        client = new GeminiCloudClient();

        registerDownloadReceiver();
        return super.onFragmentCreate();
    }


    // --- prefs ---------------------------------------------------------------

    private android.content.SharedPreferences prefs() {
        return getParentActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String savedKey() {
        return prefs().getString("gemini_api_key", "");
    }

    private String savedModel() {
        return prefs().getString("gen_model", GeminiCloudClient.DEFAULT_MODEL);
    }

    private boolean piiMaskEnabled() {
        return prefs().getBoolean("pii_mask", true);
    }

    private void saveKey(String key) {
        prefs().edit().putString("gemini_api_key", key.trim()).apply();
        listAdapter.notifyItemChanged(keyRow);
    }

    private void saveModel(String model) {
        prefs().edit().putString("gen_model", model).apply();
        listAdapter.notifyItemChanged(modelRow);
    }

    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return LocaleController.getString(R.string.MiogramKeyNotSet);
        }
        return key.substring(0, Math.min(6, key.length())) + "…••••";
    }

    // --- dialogs ---------------------------------------------------------------

    private void showKeyDialog() {
        Context ctx = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setText(savedKey());
        input.setHint("AIzaSy…");
        input.setTextSize(16);

        android.widget.LinearLayout container = new android.widget.LinearLayout(ctx);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        builder.setTitle(LocaleController.getString(R.string.MiogramKeyDialogTitle));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (d, w) -> saveKey(input.getText().toString()));
        builder.setNeutralButton(LocaleController.getString(R.string.MiogramPingTest), (d, w) -> {
            saveKey(input.getText().toString());
            runPingTest(input.getText().toString().trim());
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showModelDialog() {
        Context ctx = getParentActivity();
        final String[] ids = {
                "gemini-2.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-2.5-pro",
        };
        final String[] labels = {
                LocaleController.getString(R.string.MiogramModelFlash),
                LocaleController.getString(R.string.MiogramModelFlashLite),
                LocaleController.getString(R.string.MiogramModelPro),
        };
        final String customLabel = LocaleController.getString(R.string.MiogramModelCustom);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString(R.string.MiogramModelDialogTitle));

        builder.setItems(concat(labels, new String[]{customLabel}), (d, which) -> {
            if (which < labels.length) {
                saveModel(ids[which]);
            } else {
                showCustomModelDialog();
            }
        });
        showDialog(builder.create());
    }

    private static String[] concat(String[] a, String[] b) {
        String[] out = new String[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private void showCustomModelDialog() {
        Context ctx = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString(R.string.MiogramModelCustomTitle));

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setText(savedModel());
        input.setTextSize(16);
        android.widget.LinearLayout container = new android.widget.LinearLayout(ctx);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (14 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (d, w) -> saveModel(input.getText().toString().trim()));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    // --- ping ------------------------------------------------------------------

    private void runPingTest(String apiKey) {
        if (apiKey.isEmpty()) {
            toast(LocaleController.getString(R.string.MiogramPingNoKey));
            return;
        }
        toast(LocaleController.getString(R.string.MiogramPingRunning));
        new Thread(() -> {
            GeminiCloudClient.Result result = client.completeBlocking(
                    new GeminiCloudClient.Config(GeminiCloudClient.DEFAULT_MODEL, apiKey),
                    "You are a connectivity probe.",
                    "Reply with exactly: OK");
            String ui;
            if (result instanceof GeminiCloudClient.Result.Success) {
                ui = "✓ " + ((GeminiCloudClient.Result.Success) result).getText();
            } else if (result instanceof GeminiCloudClient.Result.ApiError) {
                ui = "✗ " + ((GeminiCloudClient.Result.ApiError) result).getCode() + ": "
                        + ((GeminiCloudClient.Result.ApiError) result).getMessage();
            } else if (result instanceof GeminiCloudClient.Result.Blocked) {
                ui = "✗ blocked";
            } else {
                ui = "✗ network";
            }
            final String text = ui;
            AndroidUtilities.runOnUIThread(() -> toast(text));
        }, "miogram-ping").start();
    }

    // --- Whisper ---------------------------------------------------------------

    private File modelsDir() {
        File dir = new File(getParentActivity().getFilesDir(), "models");
        dir.mkdirs();
        return dir;
    }

    private LocalSttEngine.ModelInfo selectedWhisperModel() {
        for (LocalSttEngine.ModelInfo info : stt.models()) {
            if (info.getId().equals(stt.getSelectedModelId())) return info;
        }
        return stt.models().get(0);
    }

    private void showWhisperModelDialog() {
        Context ctx = getParentActivity();
        var models = stt.models().toArray(new LocalSttEngine.ModelInfo[0]);
        String[] labels = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            labels[i] = models[i].getDisplayName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString(R.string.MiogramWhisperPickerTitle));
        builder.setItems(labels, (d, which) -> {
            stt.selectModel(models[which].getId());
            listAdapter.notifyItemChanged(whisperModelRow);
            listAdapter.notifyItemChanged(whisperDownloadRow);
        });
        showDialog(builder.create());
    }

    private void startWhisperDownload() {
        LocalSttEngine.ModelInfo model = selectedWhisperModel();
        DownloadManager dm = (DownloadManager) getParentActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            toast("DownloadManager unavailable");
            return;
        }
        File part = new File(modelsDir(), model.getId() + ".part");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(model.getDownloadUrl()));
        request.setTitle("Miogram · " + model.getDisplayName());
        request.setDestinationUri(Uri.fromFile(part));
        activeDownloadId = dm.enqueue(request);
        activeDownloadModelId = model.getId();
        listAdapter.notifyItemChanged(whisperDownloadRow);
    }

    private void handleDownloadCompleted(long finishedId) {
        if (activeDownloadId != finishedId || activeDownloadModelId == null) return;
        LocalSttEngine.ModelInfo expected = null;
        for (LocalSttEngine.ModelInfo info : stt.models()) {
            if (info.getId().equals(activeDownloadModelId)) expected = info;
        }
        if (expected == null) return;

        File part = new File(modelsDir(), activeDownloadModelId + ".part");
        File target = new File(modelsDir(), activeDownloadModelId + ".onnx");
        if (part.renameTo(target)) {
            boolean ok = stt.registerDownloadedFile(expected);
            toast(ok
                    ? LocaleController.getString(R.string.MiogramWhisperReady)
                    : LocaleController.getString(R.string.MiogramWhisperInvalid));
        } else {
            toast("rename failed");
        }
        listAdapter.notifyItemChanged(whisperModelRow);
        listAdapter.notifyItemChanged(whisperDownloadRow);
        activeDownloadId = -1;
        activeDownloadModelId = null;
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                handleDownloadCompleted(id);
            }
        };
        getParentActivity().registerReceiver(
                downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    // --- rows ------------------------------------------------------------------

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == keyRow) {
            showKeyDialog();
        } else if (position == modelRow) {
            showModelDialog();
        } else if (position == whisperModelRow) {
            showWhisperModelDialog();
        } else if (position == whisperDownloadRow) {
            startWhisperDownload();
        } else if (position == piiRow || position == meteredRow || position == offlineRow) {
            TextCheckCell cell = view instanceof TextCheckCell ? (TextCheckCell) view : null;
            if (position == piiRow) {
                boolean v = !prefs().getBoolean("pii_mask", true);
                prefs().edit().putBoolean("pii_mask", v).apply();
                if (cell != null) cell.setChecked(v);
            } else if (position == meteredRow) {
                boolean v = !facade.getPreferences().getCloudAllowedOnMeteredNetwork();
                facade.setPreferences(facade.getPreferences().withCloudMetered(v));
                if (cell != null) cell.setChecked(v);
            } else {
                boolean v = !prefs().getBoolean("stt_offline_only", false);
                prefs().edit().putBoolean("stt_offline_only", v).apply();
                if (cell != null) cell.setChecked(v);
            }
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerRow = addRow();
        keyRow = addRow();
        modelRow = addRow();
        piiRow = addRow();
        meteredRow = addRow();
        whisperHeaderRow = addRow();
        whisperModelRow = addRow();
        whisperDownloadRow = addRow();
        offlineRow = addRow();
        shadowRow = addRow();
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
            if (position == headerRow || position == whisperHeaderRow) return 4;
            if (position == piiRow || position == meteredRow || position == offlineRow) return 3;
            return 2;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case 4: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setText(position == headerRow
                            ? "ХМАРА (GEMINI)"
                            : "ЛОКАЛЬНИЙ WHISPER STT");
                    break;
                }
                case 3: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == piiRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramPiiMask),
                                prefs().getBoolean("pii_mask", true), false);
                    } else if (position == meteredRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramMeteredGuard),
                                facade.getPreferences().getCloudAllowedOnMeteredNetwork(), false);
                    } else if (position == offlineRow) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MiogramOfflineOnly),
                                prefs().getBoolean("stt_offline_only", false), false);
                    }
                    break;
                }
                default: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == keyRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramKeyRow)
                                + " · " + maskKey(savedKey()), false);
                        cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    } else if (position == modelRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramModelRow)
                                + " · " + savedModel(), false);
                    } else if (position == whisperModelRow) {
                        String wStatus = stt.isDownloaded()
                                ? stt.getSelectedModelId() + " ✓"
                                : stt.getSelectedModelId() + " (не завантажена)";
                        cell.setText(LocaleController.getString(R.string.MiogramWhisperModelRow)
                                + ": " + wStatus, false);
                    } else if (position == whisperDownloadRow) {
                        cell.setText(LocaleController.getString(R.string.MiogramWhisperDownload), false);
                    }
                    break;
                }
            }
        }
    }

    private void toast(String message) {
        if (getParentActivity() != null) {
            android.widget.Toast.makeText(getParentActivity(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFragmentDestroy() {
        if (downloadReceiver != null && getParentActivity() != null) {
            try {
                getParentActivity().unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {}
            downloadReceiver = null;
        }
        super.onFragmentDestroy();
    }
}
