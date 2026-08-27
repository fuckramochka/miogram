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
import android.text.TextUtils;
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

import app.miogram.bridge.MiogramLocale;

/**
 * Premium 16:9 Anime Art In-App Update Dialog for Miogram.
 * Features:
 * - 16:9 smooth anti-aliased banner artwork (Happy Kangel on update / Sad Ame-chan on latest)
 * - Modern typography and pill badges
 * - Soft rounded translucent card for changelog
 * - Smooth horizontal download progress bar
 * - Multilingual support (Ukrainian, Russian, English)
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
        root.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(16), AndroidUtilities.dp(22), AndroidUtilities.dp(20));

        // 1. Title
        SimpleTextView title = new SimpleTextView(ctx);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(19);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setText(hasUpdate
                ? MiogramLocale.get("Вийшло нове оновлення!", "Вышло новое обновление!", "New Update Available!")
                : MiogramLocale.get("Встановлена остання версія", "Установлена последняя версия", "Latest Version Installed"));
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

        // 2. Version Pill Badge
        TextView versionBadge = new TextView(ctx);
        versionBadge.setText(hasUpdate
                ? MiogramLocale.format("Доступна версія: v%s", "Доступная версия: v%s", "Available version: v%s", versionName)
                : MiogramLocale.format("Версія: v%s (Актуальна)", "Версия: v%s (Актуальная)", "Version: v%s (Up to date)", versionName));
        versionBadge.setTextSize(13);
        versionBadge.setTypeface(AndroidUtilities.bold());
        versionBadge.setTextColor(hasUpdate ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueText) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        versionBadge.setGravity(Gravity.CENTER);
        root.addView(versionBadge, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // 3. 16:9 Banner Illustration (Smooth rounded corners 16dp)
        ImageView illustrationView = new ImageView(ctx);
        illustrationView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int imageRes = hasUpdate ? R.drawable.img_update_available : R.drawable.img_update_none;
        try {
            Bitmap raw = BitmapFactory.decodeResource(ctx.getResources(), imageRes);
            if (raw != null) {
                // Draw 16:9 smooth rounded image
                illustrationView.setImageBitmap(getSmoothRounded16by9Bitmap(raw, AndroidUtilities.dp(16)));
            } else {
                illustrationView.setImageResource(imageRes);
            }
        } catch (Throwable t) {
            illustrationView.setImageResource(imageRes);
        }
        // 16:9 Ratio: Match Parent width, ~146dp height
        root.addView(illustrationView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 146, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // 4. Soft Card Container for Changelog & Description
        LinearLayout cardLayout = new LinearLayout(ctx);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        cardLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        TextView descriptionView = new TextView(ctx);
        if (hasUpdate) {
            String noteText = (!TextUtils.isEmpty(changelog))
                    ? changelog.trim()
                    : MiogramLocale.get("• Оновлено Miogram AI (Gemini 3.5 Flash Lite)\n• Нативна розшифровка голосових повідомлень\n• Оптимізація та прискорення роботи",
                                        "• Обновлен Miogram AI (Gemini 3.5 Flash Lite)\n• Нативная расшифровка голосовых сообщений\n• Оптимизация и ускорение работы",
                                        "• Updated Miogram AI (Gemini 3.5 Flash Lite)\n• Native voice message transcription\n• Performance optimizations");
            descriptionView.setText(MiogramLocale.get("Що нового:\n", "Что нового:\n", "What's new:\n") + noteText + "\n\n" + MiogramLocale.get("Бажаєте встановити оновлення?", "Желаете установить обновление?", "Would you like to install the update?"));
        } else {
            descriptionView.setText(MiogramLocale.get("У вас вже встановлено найновішу збірку Miogram. Нових оновлень поки немає.",
                    "У вас уже установлена самая новая сборка Miogram. Новых обновлений пока нет.",
                    "You already have the latest build of Miogram installed. No new updates found."));
        }
        descriptionView.setTextSize(13.5f);
        descriptionView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        descriptionView.setLineSpacing(AndroidUtilities.dp(3), 1.15f);
        cardLayout.addView(descriptionView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(cardLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 5. Progress Container
        progressContainer = new LinearLayout(ctx);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setVisibility(View.GONE);

        progressBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8, 0, 0, 0, 4));

        progressTextView = new TextView(ctx);
        progressTextView.setText(MiogramLocale.get("Завантаження: 0%", "Загрузка: 0%", "Downloading: 0%"));
        progressTextView.setTextSize(12);
        progressTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        progressTextView.setGravity(Gravity.CENTER);
        progressContainer.addView(progressTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        root.addView(progressContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // 6. Action Buttons
        installButton = new TextView(ctx);
        installButton.setText(hasUpdate
                ? MiogramLocale.get("Оновити зараз", "Обновить сейчас", "Update Now")
                : MiogramLocale.get("Чудово", "Отлично", "Great"));
        installButton.setTextSize(15);
        installButton.setTypeface(AndroidUtilities.bold());
        installButton.setGravity(Gravity.CENTER);
        installButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButton)));
        installButton.setTextColor(0xFFFFFFFF);
        if (hasUpdate) {
            installButton.setOnClickListener(v -> startDownloadAndInstall(fragment));
        } else {
            installButton.setOnClickListener(v -> dismiss());
        }
        root.addView(installButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, Gravity.TOP, 0, 0, 0, hasUpdate ? 8 : 0));

        if (hasUpdate) {
            cancelButton = new TextView(ctx);
            cancelButton.setText(MiogramLocale.get("Пізніше", "Позже", "Later"));
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

    private Bitmap getSmoothRounded16by9Bitmap(Bitmap bitmap, int pixels) {
        int width = bitmap.getWidth();
        int targetHeight = (width * 9) / 16;
        if (targetHeight > bitmap.getHeight()) {
            targetHeight = bitmap.getHeight();
        }

        Bitmap cropped = Bitmap.createBitmap(bitmap, 0, Math.max(0, (bitmap.getHeight() - targetHeight) / 2), width, targetHeight);
        Bitmap output = Bitmap.createBitmap(cropped.getWidth(), cropped.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Rect rect = new Rect(0, 0, cropped.getWidth(), cropped.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(cropped, rect, rect, paint);

        return output;
    }

    private void startDownloadAndInstall(BaseFragment fragment) {
        Context ctx = fragment.getParentActivity();
        if (ctx == null) ctx = ApplicationLoader.applicationContext;

        if (apkDownloadUrl == null || apkDownloadUrl.isEmpty()) {
            Toast.makeText(ctx, MiogramLocale.get("Посилання для завантаження недоступне", "Ссылка для загрузки недоступна", "Download link is not available"), Toast.LENGTH_SHORT).show();
            return;
        }

        installButton.setEnabled(false);
        installButton.setAlpha(0.6f);
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        progressContainer.setVisibility(View.VISIBLE);
        progressTextView.setText(MiogramLocale.get("Завантаження: 0%...", "Загрузка: 0%...", "Downloading: 0%..."));

        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(ctx, "DownloadManager error", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = ctx.getFilesDir();
            File apkFile = new File(dir, "miogram_update_" + versionName + ".apk");
            if (apkFile.exists()) apkFile.delete();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkDownloadUrl));
            request.setTitle("Miogram v" + versionName);
            request.setDescription(MiogramLocale.get("Завантаження оновлення...", "Загрузка обновления...", "Downloading update..."));
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
            Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            progressTextView.setText(MiogramLocale.format("Завантаження: %d%% (%dMB / %dMB)", "Загрузка: %d%% (%dMB / %dMB)", "Downloading: %d%% (%dMB / %dMB)", percent, dlMb, totalMb));
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
                    Toast.makeText(ctx, MiogramLocale.get("Увімкніть дозвіл на встановлення додатків для Miogram", "Включите разрешение на установку приложений для Miogram", "Enable install unknown apps permission for Miogram"), Toast.LENGTH_LONG).show();
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
            Toast.makeText(ctx, "Install Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
