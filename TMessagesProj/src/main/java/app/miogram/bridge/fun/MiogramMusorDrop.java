package app.miogram.bridge.fun;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Easter egg: tapping a {@code tg://musor_drop} link blacks out everything
 * and plays the bundled drop (video if present, audio otherwise).
 * When playback ends (or the user taps), the overlay dismisses and the app
 * returns exactly where it was.
 *
 * <p>Media lookup order: app assets {@code musordrop.mp4} / {@code musordrop.mp3},
 * then {@code Downloads/musordrop.*}, then app files dir. Missing file =
 * silent no-op (returns false so the link falls through to normal handling).
 */
public final class MiogramMusorDrop {

    public static final String TRIGGER_URL = "tg://musor_drop";

    private MiogramMusorDrop() {}

    public static boolean isTrigger(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(java.util.Locale.US);
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u.equals(TRIGGER_URL) || u.equals("tg:musor_drop");
    }

    /** @return true when the egg was shown (caller must skip normal handling). */
    public static boolean tryHandle(Context context) {
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (ApplicationLoader.applicationContext instanceof Activity) {
            activity = (Activity) ApplicationLoader.applicationContext;
        }
        if (activity == null || activity.isFinishing()) return false;

        File video = locate("musordrop.mp4");
        File audio = video != null ? null : locate("musordrop.mp3");
        File media = video != null ? video : audio;

        final Activity act = activity;
        if (media == null || !media.isFile()) {
            // Trigger works, media missing: blackout with a hint instead of silence.
            AndroidUtilities.runOnUIThread(() -> showMissingOverlay(act));
            return true;
        }
        final boolean isVideo = video != null;
        AndroidUtilities.runOnUIThread(() -> showOverlay(act, media, isVideo));
        return true;
    }

    private static void showMissingOverlay(Activity activity) {
        try {
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            FrameLayout root = new FrameLayout(activity);
            root.setBackgroundColor(Color.BLACK);
            android.widget.TextView hint = new android.widget.TextView(activity);
            hint.setText("MUSOR NOT FOUND\nkin' musordrop.mp3 v assets/Downloads");
            hint.setTextColor(Color.WHITE);
            hint.setTextSize(16);
            hint.setGravity(Gravity.CENTER);
            root.addView(hint, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            root.setOnClickListener(v -> safeDismiss(dialog));
            dialog.setContentView(root);
            android.view.Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
            dialog.show();
            root.postDelayed(() -> safeDismiss(dialog), 2200);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static File locate(String name) {
        // 1. App files dir (user can drop it there via any file manager).
        try {
            File f = new File(ApplicationLoader.applicationContext.getFilesDir(), name);
            if (f.isFile() && f.length() > 0) return f;
        } catch (Throwable ignored) {}
        // 2. Downloads.
        try {
            File downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            File f = new File(downloads, name);
            if (f.isFile() && f.length() > 0) return f;
        } catch (Throwable ignored) {}
        // 3. Bundled asset -> copy to cache on first use.
        try {
            Context ctx = ApplicationLoader.applicationContext;
            File out = new File(ctx.getCacheDir(), name);
            if (out.isFile() && out.length() > 0) return out;
            try (InputStream in = ctx.getAssets().open(name)) {
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.flush();
                }
            } catch (Throwable assetMissing) {
                return null;
            }
            if (out.isFile() && out.length() > 0) return out;
        } catch (Throwable ignored) {}
        return null;
    }

    private static void showOverlay(Activity activity, File media, boolean isVideo) {
        try {
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            FrameLayout root = new FrameLayout(activity);
            root.setBackgroundColor(Color.BLACK);

            final MediaPlayer[] playerBox = new MediaPlayer[1];
            VideoView videoView = null;
            if (isVideo) {
                videoView = new VideoView(activity);
                videoView.setVideoPath(media.getAbsolutePath());
                root.addView(videoView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
                videoView.setOnCompletionListener(mp -> safeDismiss(dialog));
                videoView.setOnErrorListener((mp, what, extra) -> {
                    safeDismiss(dialog);
                    return true;
                });
            } else {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(media.getAbsolutePath());
                    mp.setOnCompletionListener(m -> {
                        m.release();
                        safeDismiss(dialog);
                    });
                    mp.setOnErrorListener((m, what, extra) -> {
                        try {
                            m.release();
                        } catch (Throwable ignored) {}
                        safeDismiss(dialog);
                        return true;
                    });
                    mp.prepare();
                    playerBox[0] = mp;
                } catch (Throwable t) {
                    FileLog.e(t);
                    return;
                }
            }

            dialog.setOnDismissListener(d -> {
                try {
                    if (playerBox[0] != null) {
                        playerBox[0].release();
                        playerBox[0] = null;
                    }
                } catch (Throwable ignored) {}
            });
            // Tap anywhere to bail out early.
            root.setOnClickListener(v -> safeDismiss(dialog));

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            // True blackout: hide status/nav bars while the egg is up.
            root.setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            dialog.show();
            if (isVideo && videoView != null) {
                videoView.start();
            } else if (playerBox[0] != null) {
                playerBox[0].start();
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void safeDismiss(Dialog dialog) {
        try {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        } catch (Throwable ignored) {}
    }
}
