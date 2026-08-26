package app.miogram.bridge.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;

/**
 * Native Telegram BottomSheet in the style of Kangel Plugins Manager (KPM)
 * for seamless in-app Miogram updates and changelog review.
 */
public class MiogramUpdateBottomSheet extends BottomSheet {

    private final String versionName;
    private final String changelog;
    private final String apkDownloadUrl;

    private TextView installButton;
    private TextView statusTextView;
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;

    public MiogramUpdateBottomSheet(BaseFragment fragment, String versionName, String changelog, String apkDownloadUrl) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
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

        SimpleTextView title = new SimpleTextView(ctx);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(20);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setText("Доступне оновлення Miogram");
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 16, 24, 16, 4));

        TextView versionBadge = new TextView(ctx);
        versionBadge.setText("Версія " + versionName);
        versionBadge.setTextSize(14);
        versionBadge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        versionBadge.setGravity(Gravity.CENTER);
        root.addView(versionBadge, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 16, 0, 16, 12));

        View divider = new View(ctx);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        root.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 12));

        TextView notesTitle = new TextView(ctx);
        notesTitle.setText("Що нового:");
        notesTitle.setTypeface(AndroidUtilities.bold());
        notesTitle.setTextSize(15);
        notesTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        root.addView(notesTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 6));

        TextView notes = new TextView(ctx);
        notes.setText(changelog != null && !changelog.isEmpty() ? changelog : "• Оновлення компонентів ШІ\n• Покращення стабільності та безпеки\n• Оновлення налаштувань рідкого скла та сховища");
        notes.setTextSize(14);
        notes.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        notes.setLineSpacing(AndroidUtilities.dp(3), 1f);
        root.addView(notes, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 16));

        statusTextView = new TextView(ctx);
        statusTextView.setVisibility(View.GONE);
        statusTextView.setTextSize(13);
        statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        statusTextView.setGravity(Gravity.CENTER);
        root.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 8));

        installButton = new TextView(ctx);
        installButton.setText("Встановити оновлення");
        installButton.setTextSize(16);
        installButton.setTypeface(AndroidUtilities.bold());
        installButton.setGravity(Gravity.CENTER);
        installButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButton)));
        installButton.setOnClickListener(v -> startDownloadAndInstall(fragment));
        root.addView(installButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP, 16, 4, 16, 20));

        FrameLayout fl = new FrameLayout(ctx);
        fl.addView(root);

        NestedScrollView sv = new NestedScrollView(ctx);
        sv.addView(fl);
        setCustomView(sv);
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
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText("Завантаження APK у фоні...");

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
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(ctx, "Помилка завантаження: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            installButton.setEnabled(true);
            installButton.setAlpha(1f);
        }
    }

    private void promptInstallApk(Context ctx, File file) {
        try {
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
        try {
            if (downloadReceiver != null) {
                ApplicationLoader.applicationContext.unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        } catch (Exception ignored) {}
        super.dismiss();
    }
}
