package app.miogram.bridge.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;

/**
 * Compact and stylish Miogram Update Dialog with anime state reaction illustrations:
 * - Top title & version badge
 * - State illustration (Happy Kangel on update / Sad Ame-chan on latest)
 * - Changelog & Question
 * - Compact horizontal progress bar during download
 * - Action buttons
 */
public class MiogramUpdateBottomSheet extends BottomSheet {

    private final boolean hasUpdate;
    private final String versionName;
    private final String changelog;
    private final String apkDownloadUrl;

    private TextView installButton;
    private TextView cancelButton;
    private LinearLayout progressContainer;
    private ProgressBar progressBar;
    private TextView progressTextView;

    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;
    private Handler progressHandler;
    private Runnable progressRunnable;

    public MiogramUpdateBottomSheet(BaseFragment fragment, boolean hasUpdate, String versionName, String changelog, String apkDownloadUrl) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        this.hasUpdate = hasUpdate;
        this.versionName = versionName;
        this.changelog = changelog;
        this.apkDownloadUrl = apkDownloadUrl;

        setApplyBottomPadding(false);
        setApplyTopPadding(false);
        fixNavigationBar(Theme.getColor(Theme.key_windowBackgroundWhite));

        Context ctx = fragment.getParentActivity();
        if (ctx == null) ctx = ApplicationLoader.applicationContext;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClickable(true);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        // 1. Title
        SimpleTextView title = new SimpleTextView(ctx);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(18);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setText(hasUpdate ? "Вийшло нове оновлення!" : "Встановлена остання версія");
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 2));

        // 2. Version badge
        TextView versionBadge = new TextView(ctx);
        versionBadge.setText(hasUpdate ? "Доступна версія: v" + versionName : "Версія: v" + versionName + " (Актуальна)");
        versionBadge.setTextSize(13);
        versionBadge.setTextColor(hasUpdate ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueText) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        versionBadge.setGravity(Gravity.CENTER);
        root.addView(versionBadge, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // 3. Illustration (Happy on update / Sad on latest)
        ImageView illustrationView = new ImageView(ctx);
        illustrationView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int imageRes = hasUpdate ? R.drawable.img_update_available : R.drawable.img_update_none;
        try {
            Bitmap raw = BitmapFactory.decodeResource(ctx.getResources(), imageRes);
            if (raw != null) {
                illustrationView.setImageBitmap(getRoundedCornerBitmap(raw, AndroidUtilities.dp(14)));
            } else {
                illustrationView.setImageResource(imageRes);
            }
        } catch (Throwable t) {
            illustrationView.setImageResource(imageRes);
        }
        root.addView(illustrationView, LayoutHelper.createLinear(110, 110, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // 4. Description / Changelog
        TextView descriptionView = new TextView(ctx);
        if (hasUpdate) {
            String noteText = (changelog != null && !changelog.trim().isEmpty())
                    ? changelog.trim()
                    : "• Оновлено Miogram AI (Gemini 2.5/3.5 Flash Lite)\n• Нативна розшифровка голосових повідомлень\n• Оптимізація та прискорення роботи додатку";
            descriptionView.setText("Що нового:\n" + noteText + "\n\nБажаєте встановити оновлення?");
        } else {
            descriptionView.setText("У вас вже встановлено найновішу збірку Miogram. Нових оновлень поки немає.");
        }
        descriptionView.setTextSize(13);
        descriptionView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        descriptionView.setLineSpacing(AndroidUtilities.dp(2), 1f);
        root.addView(descriptionView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 12));

        // 5. Progress Container (Hidden by default)
        progressContainer = new LinearLayout(ctx);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setVisibility(View.GONE);

        progressBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8, 0, 0, 0, 4));

        progressTextView = new TextView(ctx);
        progressTextView.setText("Завантаження: 0%");
        progressTextView.setTextSize(12);
        progressTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        progressTextView.setGravity(Gravity.CENTER);
        progressContainer.addView(progressTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        root.addView(progressContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // 6. Action Buttons
        installButton = new TextView(ctx);
        installButton.setText(hasUpdate ? "Оновити зараз" : "Чудово");
        installButton.setTextSize(15);
        installButton.setTypeface(AndroidUtilities.bold());
        installButton.setGravity(Gravity.CENTER);
        installButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButton)));
        installButton.setTextColor(0xFFFFFFFF);
        if (hasUpdate) {
            installButton.setOnClickListener(v -> startDownloadAndInstall(fragment));
        } else {
            installButton.setOnClickListener(v -> dismiss());
        }
        root.addView(installButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, Gravity.TOP, 0, 0, 0, hasUpdate ? 6 : 0));

        if (hasUpdate) {
            cancelButton = new TextView(ctx);
            cancelButton.setText("Пізніше");
            cancelButton.setTextSize(14);
            cancelButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            cancelButton.setGravity(Gravity.CENTER);
            cancelButton.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
            cancelButton.setOnClickListener(v -> dismiss());
            root.addView(cancelButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        }

        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(root);

        NestedScrollView sv = new NestedScrollView(ctx);
        sv.addView(fl);
        setCustomView(sv);
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private void startDownloadAndInstall(BaseFragment fragment) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) ctx = ApplicationLoader.applicationContext;

        if (apkDownloadUrl == null || apkDownloadUrl.isEmpty()) {
            Toast.makeText(ctx, "Посилання для завантаження недоступне", Toast.LENGTH_SHORT).show();
            return;
        }

        installButton.setEnabled(false);
        installButton.setAlpha(0.6f);
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        progressContainer.setVisibility(View.VISIBLE);
        progressTextView.setText("Завантаження: 0%...");

        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(ctx, "DownloadManager недоступний", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = ctx.getFilesDir();
            File apkFile = new File(dir, "miogram_update_" + versionName + ".apk");
            if (apkFile.exists()) apkFile.delete();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkDownloadUrl));
            request.setTitle("Miogram v" + versionName);
            request.setDescription("Завантаження оновлення...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(apkFile));

            final Context finalCtx = ctx;
            final File finalApk = apkFile;

            downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        stopProgressPolling();
                        try {
                            finalCtx.unregisterReceiver(this);
                        } catch (Exception ignored) {}
                        dismiss();
                        promptInstallApk(finalCtx, finalApk);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }

            downloadId = dm.enqueue(request);
            startProgressPolling(dm, downloadId);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(ctx, "Помилка завантаження: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            installButton.setEnabled(true);
            installButton.setAlpha(1f);
            if (cancelButton != null) cancelButton.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.GONE);
        }
    }

    private void startProgressPolling(DownloadManager dm, long id) {
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(id);
                    Cursor cursor = dm.query(q);
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (bytesTotal > 0) {
                            int percent = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            progressBar.setProgress(percent);
                            long dlMb = bytesDownloaded / (1024 * 1024);
                            long totalMb = bytesTotal / (1024 * 1024);
                            progressTextView.setText("Завантаження: " + percent + "% (" + dlMb + "MB / " + totalMb + "MB)");
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) {}
                if (progressHandler != null) {
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressPolling() {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressHandler = null;
        }
    }

    private void promptInstallApk(Context ctx, File file) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!ctx.getPackageManager().canRequestPackageInstalls()) {
                    Intent permIntent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    permIntent.setData(Uri.parse("package:" + ctx.getPackageName()));
                    permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(permIntent);
                    Toast.makeText(ctx, "Увімкніть дозвіл на встановлення додатків для Miogram", Toast.LENGTH_LONG).show();
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
            Toast.makeText(ctx, "Помилка відкриття інсталятора: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void dismiss() {
        stopProgressPolling();
        try {
            if (downloadReceiver != null) {
                ApplicationLoader.applicationContext.unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        } catch (Exception ignored) {}
        super.dismiss();
    }
}
