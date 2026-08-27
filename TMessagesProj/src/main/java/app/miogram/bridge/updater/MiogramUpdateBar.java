package app.miogram.bridge.updater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import java.io.File;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ui.MiogramUpdateBottomSheet;

/**
 * Global In-App Floating Update Progress Bar for Miogram:
 * - Appears seamlessly across the entire app while an update is downloading
 * - Displays live percentage, download sizes, and interactive controls
 * - Tapping opens full update sheet with 16:9 art
 */
public class MiogramUpdateBar extends FrameLayout implements MiogramDownloadManager.DownloadListener {

    private static MiogramUpdateBar currentBarInstance;

    private TextView titleView;
    private TextView subtitleView;
    private ProgressBar progressBar;
    private ImageView iconView;
    private ImageView closeView;

    public MiogramUpdateBar(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setClickable(true);
        setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        setElevation(AndroidUtilities.dp(8));
        setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);

        // Icon
        iconView = new ImageView(context);
        iconView.setImageResource(R.drawable.msg_retry);
        iconView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        content.addView(iconView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        // Texts and Progress
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        titleView = new TextView(context);
        titleView.setText("Miogram Update");
        titleView.setTextSize(13);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get("Завантаження: 0%", "Загрузка: 0%", "Downloading: 0%"));
        subtitleView.setTextSize(11.5f);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        textContainer.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 3));

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        textContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 4));

        content.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        // Close / Cancel button
        closeView = new ImageView(context);
        closeView.setImageResource(R.drawable.msg_close);
        closeView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        closeView.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        closeView.setOnClickListener(v -> {
            MiogramDownloadManager.getInstance().cancelDownload();
            hideGlobalBar();
        });
        content.addView(closeView, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setOnClickListener(v -> {
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    MiogramDownloadManager dm = MiogramDownloadManager.getInstance();
                    MiogramUpdateBottomSheet sheet = new MiogramUpdateBottomSheet(
                            frag, true, dm.getCurrentVersion(), dm.getCurrentChangelog(), dm.getCurrentDownloadUrl()
                    );
                    sheet.show();
                }
            }
        });
    }

    public static void showGlobalBar() {
        new Handler(Looper.getMainLooper()).post(() -> {
            LaunchActivity act = LaunchActivity.instance;
            if (act == null || act.isFinishing()) return;

            ViewGroup root = (ViewGroup) act.findViewById(android.R.id.content);
            if (root == null) return;

            if (currentBarInstance == null) {
                currentBarInstance = new MiogramUpdateBar(act);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL
                );
                lp.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(48), AndroidUtilities.dp(16), 0);
                root.addView(currentBarInstance, lp);
                MiogramDownloadManager.getInstance().addListener(currentBarInstance);
            }
            currentBarInstance.setVisibility(View.VISIBLE);
        });
    }

    public static void hideGlobalBar() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (currentBarInstance != null) {
                MiogramDownloadManager.getInstance().removeListener(currentBarInstance);
                ViewGroup parent = (ViewGroup) currentBarInstance.getParent();
                if (parent != null) {
                    parent.removeView(currentBarInstance);
                }
                currentBarInstance = null;
            }
        });
    }

    @Override
    public void onProgress(int percent, long downloadedBytes, long totalBytes) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (progressBar != null) progressBar.setProgress(percent);
            if (titleView != null) {
                String ver = MiogramDownloadManager.getInstance().getCurrentVersion();
                titleView.setText("Miogram v" + ver);
            }
            if (subtitleView != null) {
                long dlMb = downloadedBytes / (1024 * 1024);
                long totalMb = totalBytes / (1024 * 1024);
                subtitleView.setText(MiogramLocale.format("Завантаження: %d%% (%dMB / %dMB)", "Загрузка: %d%% (%dMB / %dMB)", "Downloading: %d%% (%dMB / %dMB)", percent, dlMb, totalMb));
            }
        });
    }

    @Override
    public void onComplete(File apkFile) {
        new Handler(Looper.getMainLooper()).post(MiogramUpdateBar::hideGlobalBar);
    }

    @Override
    public void onError(String error) {
        new Handler(Looper.getMainLooper()).post(MiogramUpdateBar::hideGlobalBar);
    }
}
