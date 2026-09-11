package app.miogram.bridge.updater;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;

/**
 * Resumable Update Download Controller for Miogram:
 * - Direct HTTP download supporting 'Range: bytes=...' for resuming interrupted downloads
 * - Writes to .part file and renames to .apk upon completion
 * - Checks if completed APK is already cached in disk and offers instant install without re-downloading
 * - Coordinates global in-app floating update progress bar
 * - Auto-triggers APK package installer on finish
 */
public class MiogramDownloadManager {

    private static volatile MiogramDownloadManager instance;

    public interface DownloadListener {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(File apkFile);
        void onError(String error);
    }

    private volatile boolean isDownloading = false;
    private volatile boolean isCancelled = false;
    private Thread downloadThread = null;

    private int currentPercent = 0;
    private long bytesDownloaded = 0;
    private long bytesTotal = 0;
    private String currentVersion = "";
    private String currentChangelog = "";
    private String currentDownloadUrl = "";
    private File currentApkFile = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
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

    public static File getCachedApk(Context ctx, String version) {
        if (ctx == null) ctx = ApplicationLoader.applicationContext;
        File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = ctx.getFilesDir();
        File apkFile = new File(dir, "miogram_update_v" + version + ".apk");
        if (apkFile.exists() && apkFile.length() > 20 * 1024 * 1024) {
            return apkFile;
        }
        return null;
    }

    public static boolean isApkCached(Context ctx, String version) {
        return getCachedApk(ctx, version) != null;
    }

    public synchronized boolean startDownload(Context context, String apkUrl, String version, String changelog) {
        final Context ctx = context != null ? context.getApplicationContext() : ApplicationLoader.applicationContext;

        // Check if APK already fully downloaded in cache
        File cached = getCachedApk(ctx, version);
        if (cached != null) {
            this.currentApkFile = cached;
            this.currentVersion = version;
            this.currentPercent = 100;
            notifyComplete(cached);
            promptInstall(ctx, cached);
            return true;
        }

        if (isDownloading) {
            Toast.makeText(ctx, MiogramLocale.get("Оновлення вже завантажується...", "Обновление уже загружается...", "Update is already downloading..."), Toast.LENGTH_SHORT).show();
            MiogramUpdateBar.showGlobalBar();
            return true;
        }

        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(ctx, MiogramLocale.get("Посилання на оновлення відсутнє", "Ссылка на обновление отсутствует", "Update URL is missing"), Toast.LENGTH_SHORT).show();
            return false;
        }

        this.currentDownloadUrl = apkUrl;
        this.currentVersion = version;
        this.currentChangelog = changelog;
        this.isDownloading = true;
        this.isCancelled = false;
        this.currentPercent = 0;
        this.bytesDownloaded = 0;
        this.bytesTotal = 0;

        File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = ctx.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        final File apkFile = new File(dir, "miogram_update_v" + version + ".apk");
        final File partFile = new File(dir, "miogram_update_v" + version + ".apk.part");
        this.currentApkFile = apkFile;

        MiogramUpdateBar.showGlobalBar();

        downloadThread = new Thread(() -> executeDownload(ctx, apkUrl, apkFile, partFile), "MiogramUpdaterThread");
        downloadThread.start();
        return true;
    }

    private void executeDownload(Context ctx, String initialUrl, File targetApk, File partFile) {
        InputStream in = null;
        FileOutputStream out = null;
        HttpURLConnection conn = null;

        try {
            long existingBytes = 0;
            if (partFile.exists()) {
                existingBytes = partFile.length();
            }

            String targetUrl = initialUrl;
            int redirects = 0;
            while (redirects < 6) {
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "Miogram/" + currentVersion);

                if (existingBytes > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existingBytes + "-");
                }

                conn.connect();
                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    String newLoc = conn.getHeaderField("Location");
                    if (newLoc != null) {
                        conn.disconnect();
                        targetUrl = newLoc;
                        redirects++;
                        continue;
                    }
                }
                break;
            }

            if (conn == null) throw new Exception("Failed to establish connection");

            int responseCode = conn.getResponseCode();
            boolean isResume = (responseCode == HttpURLConnection.HTTP_PARTIAL);

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                if (existingBytes > 0 && responseCode == 416) {
                    partFile.delete();
                    existingBytes = 0;
                    conn.disconnect();
                    URL url = new URL(targetUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "Miogram/" + currentVersion);
                    conn.connect();
                    responseCode = conn.getResponseCode();
                    isResume = false;
                } else {
                    throw new Exception("HTTP server error " + responseCode);
                }
            }

            long contentLength = conn.getContentLengthLong();
            if (isResume) {
                bytesTotal = existingBytes + contentLength;
                bytesDownloaded = existingBytes;
                out = new FileOutputStream(partFile, true);
            } else {
                bytesTotal = contentLength > 0 ? contentLength : 0;
                bytesDownloaded = 0;
                out = new FileOutputStream(partFile, false);
            }

            in = conn.getInputStream();
            byte[] buffer = new byte[64 * 1024];
            int read;
            long lastNotifyTime = 0;

            while ((read = in.read(buffer)) != -1) {
                if (isCancelled) {
                    break;
                }
                out.write(buffer, 0, read);
                bytesDownloaded += read;

                if (bytesTotal > 0) {
                    currentPercent = (int) ((bytesDownloaded * 100L) / bytesTotal);
                }

                long now = System.currentTimeMillis();
                if (now - lastNotifyTime > 150) {
                    lastNotifyTime = now;
                    postProgress(currentPercent, bytesDownloaded, bytesTotal);
                }
            }

            out.flush();

            if (isCancelled) {
                isDownloading = false;
                return;
            }

            if (bytesTotal > 0 && partFile.length() < bytesTotal) {
                throw new Exception("Incomplete download (" + partFile.length() + "/" + bytesTotal + " bytes)");
            }

            if (targetApk.exists()) targetApk.delete();
            if (!partFile.renameTo(targetApk)) {
                AndroidUtilities.copyFile(partFile, targetApk);
                partFile.delete();
            }

            isDownloading = false;
            currentPercent = 100;
            currentApkFile = targetApk;

            mainHandler.post(() -> {
                notifyComplete(targetApk);
                promptInstall(ctx, targetApk);
            });

        } catch (Exception e) {
            FileLog.e(e);
            if (!isCancelled) {
                isDownloading = false;
                final String err = e.getMessage() != null ? e.getMessage() : "Download error";
                mainHandler.post(() -> {
                    Toast.makeText(ctx, MiogramLocale.get("Помилка завантаження", "Ошибка загрузки", "Download error") + ": " + err, Toast.LENGTH_SHORT).show();
                    notifyError(err);
                });
            }
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void postProgress(int percent, long downloaded, long total) {
        mainHandler.post(() -> notifyProgress(percent, downloaded, total));
    }

    public synchronized void cancelDownload() {
        if (!isDownloading) return;
        isCancelled = true;
        isDownloading = false;
        if (downloadThread != null) {
            downloadThread.interrupt();
            downloadThread = null;
        }
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

            // Variant B: Android 12+ (API 31+) Unattended Background Session
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (tryUnattendedInstall(ctx, file)) {
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
            Toast.makeText(ctx, MiogramLocale.get("Помилка встановлення", "Ошибка установки", "Install error") + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
        }
    }

    private static boolean tryUnattendedInstall(Context ctx, File file) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        try {
            PackageInstaller packageInstaller = ctx.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(ctx.getPackageName());
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);

            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            try (OutputStream out = session.openWrite("miogram_apk", 0, file.length());
                 InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[65536];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
            }

            Intent intent = new Intent(ctx.getPackageName() + MiogramInstallReceiver.ACTION_INSTALL_STATUS);
            intent.setPackage(ctx.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    ctx,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
            session.commit(pendingIntent.getIntentSender());
            session.close();
            FileLog.d("MiogramDownloadManager: committed unattended session " + sessionId);
            Toast.makeText(ctx, MiogramLocale.get("Оновлення встановлюється у фоні...", "Обновление устанавливается в фоне...", "Update is installing in background..."), Toast.LENGTH_SHORT).show();
            return true;
        } catch (Throwable t) {
            FileLog.e("MiogramDownloadManager: unattended install failed, falling back to intent", t);
            return false;
        }
    }
}
