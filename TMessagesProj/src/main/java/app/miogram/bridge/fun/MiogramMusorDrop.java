package app.miogram.bridge.fun;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Easter egg: triggering musordrop (via tg://musor_drop link, /musordrop command,
 * or settings shortcut) blacks out the screen and plays the drop video (or audio).
 * Uses hardware TextureView + MediaPlayer with automatic aspect-ratio scaling.
 */
public final class MiogramMusorDrop {

    public static final String TRIGGER_URL = "tg://musor_drop";

    private MiogramMusorDrop() {}

    /**
     * Checks if the given URL triggers the musordrop easter egg.
     * Strictly restricted to original tg://musor_drop.
     */
    public static boolean isTrigger(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(Locale.US);
        while (u.endsWith("/") || u.endsWith(" ") || u.endsWith("?")) {
            u = u.substring(0, u.length() - 1);
        }
        return u.equals("tg://musor_drop") || u.equals("tg:musor_drop");
    }

    public static final class MediaSource {
        public final AssetFileDescriptor afd;
        public final File file;
        public final boolean isVideo;

        public MediaSource(AssetFileDescriptor afd, boolean isVideo) {
            this.afd = afd;
            this.file = null;
            this.isVideo = isVideo;
        }

        public MediaSource(File file, boolean isVideo) {
            this.afd = null;
            this.file = file;
            this.isVideo = isVideo;
        }

        public void applyTo(MediaPlayer mp) throws Exception {
            if (afd != null) {
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } else if (file != null) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    mp.setDataSource(fis.getFD());
                }
            }
        }

        public void close() {
            if (afd != null) {
                try { afd.close(); } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Attempts to handle and display the easter egg.
     * @return true when handled (caller should suppress default navigation/sending).
     */
    public static boolean tryHandle(Context context) {
        Activity activity = null;
        Context c = context;
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) {
                activity = (Activity) c;
                break;
            }
            c = ((ContextWrapper) c).getBaseContext();
        }
        if (activity == null || activity.isFinishing()) {
            if (LaunchActivity.instance != null && !LaunchActivity.instance.isFinishing()) {
                activity = LaunchActivity.instance;
            }
        }
        if (activity == null || activity.isFinishing()) {
            return false;
        }

        final Activity act = activity;
        AndroidUtilities.runOnUIThread(() -> {
            MediaSource source = locateMedia();
            if (source == null) {
                showMissingOverlay(act);
            } else {
                showOverlay(act, source);
            }
        });
        return true;
    }

    private static MediaSource locateMedia() {
        Context ctx = ApplicationLoader.applicationContext;

        // 1. Try bundled asset musordrop.mp4 (direct openFd)
        if (ctx != null) {
            try {
                AssetFileDescriptor afd = ctx.getAssets().openFd("musordrop.mp4");
                if (afd != null && afd.getLength() > 0) {
                    return new MediaSource(afd, true);
                }
            } catch (Throwable ignored) {}

            // Extract bundled asset musordrop.mp4 to files dir if openFd is unsupported or compressed
            try {
                File out = new File(ctx.getFilesDir(), "musordrop.mp4");
                if (!out.isFile() || out.length() == 0) {
                    try (InputStream in = ctx.getAssets().open("musordrop.mp4")) {
                        File tmp = new File(ctx.getFilesDir(), "musordrop.mp4.tmp");
                        try (FileOutputStream fos = new FileOutputStream(tmp)) {
                            byte[] buf = new byte[65536];
                            int n;
                            while ((n = in.read(buf)) > 0) {
                                fos.write(buf, 0, n);
                            }
                            fos.flush();
                        }
                        if (tmp.length() > 0) {
                            tmp.renameTo(out);
                        }
                    }
                }
                if (out.isFile() && out.length() > 0) {
                    return new MediaSource(out, true);
                }
            } catch (Throwable ignored) {}
        }

        // 2. Try external/shared storage locations for musordrop.mp4
        File videoFile = findLocalFile("musordrop.mp4");
        if (videoFile != null) {
            return new MediaSource(videoFile, true);
        }

        return null;
    }

    private static File findLocalFile(String name) {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx != null) {
            try {
                File f = new File(ctx.getFilesDir(), name);
                if (f.isFile() && f.length() > 0) return f;
            } catch (Throwable ignored) {}

            try {
                File f = new File(ctx.getCacheDir(), name);
                if (f.isFile() && f.length() > 0) return f;
            } catch (Throwable ignored) {}

            try {
                File extFiles = ctx.getExternalFilesDir(null);
                if (extFiles != null) {
                    File f = new File(extFiles, name);
                    if (f.isFile() && f.length() > 0) return f;
                }
            } catch (Throwable ignored) {}
        }

        try {
            File downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            File f = new File(downloads, name);
            if (f.isFile() && f.length() > 0) return f;
        } catch (Throwable ignored) {}

        try {
            File ext = android.os.Environment.getExternalStorageDirectory();
            if (ext != null) {
                File f = new File(new File(ext, "Download"), name);
                if (f.isFile() && f.length() > 0) return f;
                f = new File(new File(ext, "Downloads"), name);
                if (f.isFile() && f.length() > 0) return f;
                f = new File(new File(ext, "Telegram"), name);
                if (f.isFile() && f.length() > 0) return f;
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static void showOverlay(Activity activity, MediaSource mediaSource) {
        try {
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            final FrameLayout root = new FrameLayout(activity);
            root.setBackgroundColor(Color.BLACK);

            final MediaPlayer mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

            if (mediaSource.isVideo) {
                final TextureView textureView = new TextureView(activity);
                root.addView(textureView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

                textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                        try {
                            mp.setSurface(new Surface(surface));
                            mediaSource.applyTo(mp);
                            mp.prepareAsync();
                        } catch (Throwable t) {
                            FileLog.e(t);
                            safeDismiss(dialog);
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        try {
                            mp.setSurface(null);
                        } catch (Throwable ignored) {}
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
                });

                mp.setOnPreparedListener(player -> {
                    try {
                        int vw = player.getVideoWidth();
                        int vh = player.getVideoHeight();
                        if (vw > 0 && vh > 0) {
                            int tw = textureView.getWidth();
                            int th = textureView.getHeight();
                            if (tw > 0 && th > 0) {
                                Matrix matrix = new Matrix();
                                float scaleX = 1.0f;
                                float scaleY = 1.0f;
                                float videoRatio = (float) vw / vh;
                                float screenRatio = (float) tw / th;
                                if (videoRatio > screenRatio) {
                                    scaleY = (float) (tw / videoRatio) / th;
                                } else {
                                    scaleX = (float) (th * videoRatio) / tw;
                                }
                                matrix.setScale(scaleX, scaleY, tw / 2f, th / 2f);
                                textureView.setTransform(matrix);
                            }
                        }
                        player.start();
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                });
            } else {
                try {
                    mediaSource.applyTo(mp);
                    mp.prepareAsync();
                    mp.setOnPreparedListener(MediaPlayer::start);
                } catch (Throwable t) {
                    FileLog.e(t);
                    safeDismiss(dialog);
                    return;
                }
            }

            mp.setOnCompletionListener(player -> safeDismiss(dialog));
            mp.setOnErrorListener((player, what, extra) -> {
                safeDismiss(dialog);
                return true;
            });

            dialog.setOnDismissListener(d -> {
                try {
                    if (mp.isPlaying()) {
                        mp.stop();
                    }
                    mp.release();
                } catch (Throwable ignored) {}
                mediaSource.close();
            });

            root.setOnClickListener(v -> safeDismiss(dialog));

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            root.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            dialog.show();
        } catch (Throwable t) {
            FileLog.e(t);
            mediaSource.close();
        }
    }

    private static void showMissingOverlay(Activity activity) {
        try {
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            FrameLayout root = new FrameLayout(activity);
            root.setBackgroundColor(Color.BLACK);
            TextView hint = new TextView(activity);
            hint.setText("🗑️ МУСОРДРОП НЕ ЗНАЙДЕНО 🗑️\n\nПокладіть musordrop.mp4\nу папку Завантаження (Downloads)");
            hint.setTextColor(Color.WHITE);
            hint.setTextSize(16);
            hint.setGravity(Gravity.CENTER);
            root.addView(hint, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            root.setOnClickListener(v -> safeDismiss(dialog));
            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
            dialog.show();
            root.postDelayed(() -> safeDismiss(dialog), 3500);
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
