package app.miogram.bridge.updater;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ui.MiogramUpdateBottomSheet;

/**
 * Centralized Update Download Controller for Miogram:
 * - Prevents duplicate downloads
 * - Coordinates global in-app floating update progress bar
 * - Auto-polls download status
 * - Automatically triggers APK package installer on finish
 */
public class MiogramDownloadManager {

    private static volatile MiogramDownloadManager instance;

    public interface DownloadListener {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(File apkFile);
        void onError(String error);
    }

    private boolean isDownloading = false;
    private long downloadId = -1;
    private int currentPercent = 0;
    private long bytesDownloaded = 0;
    private long bytesTotal = 0;
    private String currentVersion = "";
    private String currentChangelog = "";
    private String currentDownloadUrl = "";
    private File currentApkFile = null;

    private BroadcastReceiver receiver;
    private Handler pollHandler;
    private Runnable pollRunnable;

    private final List<DownloadListener> listeners = new ArrayList<>();

    public static MiogramDownloadManager getInstance() {
        if (instance == null) {
            synchronized (MiogramDownloadManager.class) {
                if (instance == null) {
                    instance = new MiogramDownloadManager();
                }
            }
        }
        return instance;
    }

    public synchronized void addListener(DownloadListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (isDownloading) {
                listener.onProgress(currentPercent, bytesDownloaded, bytesTotal);
            }
        }
    }

    public synchronized void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public int getCurrentPercent() {
        return currentPercent;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getCurrentChangelog() {
        return currentChangelog;
    }

    public String getCurrentDownloadUrl() {
        return currentDownloadUrl;
    }

    public synchronized boolean startDownload(Context context, String apkUrl, String version, String changelog) {
        if (isDownloading) {
            Toast.makeText(context, MiogramLocale.get("Оновлення вже завантажується...", "Обновление уже загружается...", "Update is already downloading..."), Toast.LENGTH_SHORT).show();
            MiogramUpdateBar.showGlobalBar();
            return true;
        }

        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(context, MiogramLocale.get("Посилання на оновлення відсутнє", "Ссылка на обновление отсутствует", "Update URL is missing"), Toast.LENGTH_SHORT).show();
            return false;
        }

        this.currentDownloadUrl = apkUrl;
        this.currentVersion = version;
        this.currentChangelog = changelog;
        this.isDownloading = true;
        this.currentPercent = 0;
        this.bytesDownloaded = 0;
        this.bytesTotal = 0;

        Context ctx = context != null ? context.getApplicationContext() : ApplicationLoader.applicationContext;
        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                isDownloading = false;
                return false;
            }

            File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = ctx.getFilesDir();
            File apkFile = new File(dir, "miogram_update_v" + version + ".apk");
            if (apkFile.exists()) apkFile.delete();
            this.currentApkFile = apkFile;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Miogram v" + version);
            request.setDescription(MiogramLocale.get("Завантаження оновлення Miogram...", "Загрузка обновления Miogram...", "Downloading Miogram update..."));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(apkFile));

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        stopPolling();
                        try {
                            ctx.unregisterReceiver(this);
                            receiver = null;
                        } catch (Exception ignored) {}
                        isDownloading = false;
                        currentPercent = 100;
                        notifyComplete(currentApkFile);
                        promptInstall(ctx, currentApkFile);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }

            downloadId = dm.enqueue(request);
            startPolling(dm, downloadId);
            MiogramUpdateBar.showGlobalBar();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            isDownloading = false;
            Toast.makeText(ctx, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            notifyError(e.getMessage());
            return false;
        }
    }

    private void startPolling(DownloadManager dm, long id) {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(id);
                    Cursor cursor = dm.query(q);
                    if (cursor != null && cursor.moveToFirst()) {
                        bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (bytesTotal > 0) {
                            currentPercent = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            notifyProgress(currentPercent, bytesDownloaded, bytesTotal);
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}
                if (pollHandler != null) {
                    pollHandler.postDelayed(this, 400);
                }
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollHandler = null;
        }
    }

    public synchronized void cancelDownload() {
        if (!isDownloading) return;
        isDownloading = false;
        stopPolling();
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (receiver != null) {
                ctx.unregisterReceiver(receiver);
                receiver = null;
            }
            if (downloadId != -1) {
                DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) dm.remove(downloadId);
            }
        } catch (Exception ignored) {}
        MiogramUpdateBar.hideGlobalBar();
    }

    private synchronized void notifyProgress(int percent, long down, long total) {
        for (DownloadListener l : listeners) {
            try { l.onProgress(percent, down, total); } catch (Exception ignored) {}
        }
    }

    private synchronized void notifyComplete(File apk) {
        for (DownloadListener l : listeners) {
            try { l.onComplete(apk); } catch (Exception ignored) {}
        }
        MiogramUpdateBar.hideGlobalBar();
    }

    private synchronized void notifyError(String error) {
        for (DownloadListener l : listeners) {
            try { l.onError(error); } catch (Exception ignored) {}
        }
        MiogramUpdateBar.hideGlobalBar();
    }

    public static void promptInstall(Context ctx, File file) {
        if (file == null || !file.exists()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!ctx.getPackageManager().canRequestPackageInstalls()) {
                    Intent permIntent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    permIntent.setData(Uri.parse("package:" + ctx.getPackageName()));
                    permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(permIntent);
                    Toast.makeText(ctx, MiogramLocale.get("Увімкніть дозвіл на встановлення додатків для Miogram", "Включите разрешение на установку приложений для Miogram", "Enable install apps permission for Miogram"), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(file);
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(ctx, "Install error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
