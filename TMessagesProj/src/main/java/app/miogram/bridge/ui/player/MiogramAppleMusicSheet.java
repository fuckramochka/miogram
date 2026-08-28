package app.miogram.bridge.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;

/**
 * Apple Music style player sheet:
 * - Cover-art backdrop with a dark scrim behind the content
 * - Custom thin scrubber with a scale-on-touch knob (no platform SeekBar)
 * - Vector playback controls and a live mini bass visualizer
 */
public class MiogramAppleMusicSheet extends BottomSheet implements NotificationCenter.NotificationCenterDelegate {

    private BackupImageView backdropView;
    private BackupImageView albumArtView;
    private TextView titleView;
    private TextView artistView;
    private AppleSeekBar progressBar;
    private TextView timeElapsedView;
    private TextView timeRemainingView;
    private PlayPauseVectorButton playPauseBtn;
    private HeartVectorButton heartBtn;
    private ImageView shuffleBtn;
    private ImageView repeatBtn;
    private MiogramBassVisualizer bassVisualizer;

    private boolean isSeeking = false;
    private boolean isFavorite = false;
    private final int currentAccount = UserConfig.selectedAccount;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null) {
                if (!isSeeking) {
                    float progress = playing.audioProgress;
                    progressBar.setProgressFraction(progress);

                    int total = (int) Math.round(playing.getDuration());
                    int cur = (int) (total * progress);
                    timeElapsedView.setText(AndroidUtilities.formatShortDuration(cur));
                    timeRemainingView.setText("-" + AndroidUtilities.formatShortDuration(Math.max(0, total - cur)));
                }
                // Keep the visualizer alive even while the user scrubs.
                bassVisualizer.updateAmplitudes(syntheticAmplitudes());
            }
            if (!MediaController.getInstance().isMessagePaused()) {
                progressHandler.postDelayed(this, 16);
            }
        }
    };

    private final float[] ampBuffer = new float[16];

    /** Music files carry no waveform, so the bars ride a smooth time-based wave. */
    private float[] syntheticAmplitudes() {
        long t = android.os.SystemClock.elapsedRealtime();
        for (int i = 0; i < ampBuffer.length; i++) {
            float wave = 0.5f + 0.5f * (float) Math.sin(i * 0.9 + t * 0.004);
            float detail = Math.abs((float) Math.sin(t * 0.006 + i * 1.7));
            ampBuffer[i] = Math.min(1f, 0.12f + 0.30f * wave + 0.38f * detail);
        }
        return ampBuffer;
    }

    public MiogramAppleMusicSheet(BaseFragment fragment) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        Context ctx = fragment.getParentActivity();
        if (ctx == null) ctx = ApplicationLoader.applicationContext;

        initUi(ctx);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        updateTrackInfo();
        startProgressTicker();
    }

    private void initUi(Context ctx) {
        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(0xFF141318);

        // Cover-art backdrop, zoomed and dimmed.
        backdropView = new BackupImageView(ctx);
        backdropView.setScaleX(1.35f);
        backdropView.setScaleY(1.35f);
        root.addView(backdropView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        View scrim = new View(ctx);
        scrim.setBackgroundColor(0xE6141318);
        root.addView(scrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        // Top drag handle
        View handle = new View(ctx);
        handle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), 0x44FFFFFF));
        content.addView(handle, LayoutHelper.createLinear(40, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 1. Giant Rounded Album Art
        albumArtView = new BackupImageView(ctx);
        albumArtView.setRoundRadius(AndroidUtilities.dp(22));
        albumArtView.setElevation(AndroidUtilities.dp(18));
        content.addView(albumArtView, LayoutHelper.createLinear(270, 270, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 20));

        // 2. Track Title & Artist
        LinearLayout titleBox = new LinearLayout(ctx);
        titleBox.setOrientation(LinearLayout.HORIZONTAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textGroup = new LinearLayout(ctx);
        textGroup.setOrientation(LinearLayout.VERTICAL);

        titleView = new TextView(ctx);
        titleView.setTextSize(21);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setSingleLine(true);
        textGroup.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        artistView = new TextView(ctx);
        artistView.setTextSize(15.5f);
        artistView.setTextColor(0xAAFFFFFF);
        artistView.setSingleLine(true);
        textGroup.addView(artistView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        titleBox.addView(textGroup, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        heartBtn = new HeartVectorButton(ctx);
        heartBtn.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            heartBtn.setFavorite(isFavorite);
        });
        titleBox.addView(heartBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        content.addView(titleBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 3. Custom Apple-style scrubber
        progressBar = new AppleSeekBar(ctx);
        progressBar.setListener(new AppleSeekBar.Listener() {
            @Override
            public void onSeekChanged(float fraction) {
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) {
                    int dur = (int) Math.round(playing.getDuration());
                    timeElapsedView.setText(AndroidUtilities.formatShortDuration((int) (dur * fraction)));
                    timeRemainingView.setText("-" + AndroidUtilities.formatShortDuration((int) (dur * (1.0f - fraction))));
                }
            }

            @Override
            public void onSeekStarted() {
                isSeeking = true;
            }

            @Override
            public void onSeekFinished(float fraction) {
                isSeeking = false;
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) {
                    MediaController.getInstance().seekToProgress(playing, fraction);
                }
                startProgressTicker();
            }
        });
        content.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(28), 0, 0, 0, 0));

        LinearLayout timeBox = new LinearLayout(ctx);
        timeBox.setOrientation(LinearLayout.HORIZONTAL);

        timeElapsedView = new TextView(ctx);
        timeElapsedView.setText("0:00");
        timeElapsedView.setTextSize(12);
        timeElapsedView.setTextColor(0x77FFFFFF);
        timeBox.addView(timeElapsedView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        timeRemainingView = new TextView(ctx);
        timeRemainingView.setText("-0:00");
        timeRemainingView.setTextSize(12);
        timeRemainingView.setTextColor(0x77FFFFFF);
        timeRemainingView.setGravity(Gravity.RIGHT);
        timeBox.addView(timeRemainingView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        content.addView(timeBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 4. Vector playback controls
        LinearLayout controls = new LinearLayout(ctx);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        shuffleBtn = new ImageView(ctx);
        shuffleBtn.setImageResource(R.drawable.player_new_shuffle);
        shuffleBtn.setColorFilter(SharedConfig.shuffleMusic ? 0xFFFF4081 : 0x88FFFFFF);
        shuffleBtn.setOnClickListener(v -> {
            MediaController.getInstance().setPlaybackOrderType(SharedConfig.shuffleMusic ? 0 : 2);
            shuffleBtn.setColorFilter(SharedConfig.shuffleMusic ? 0xFFFF4081 : 0x88FFFFFF);
        });
        controls.addView(shuffleBtn, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        SkipVectorButton prevBtn = new SkipVectorButton(ctx, true);
        prevBtn.setOnClickListener(v -> MediaController.getInstance().playPreviousMessage());
        controls.addView(prevBtn, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        playPauseBtn = new PlayPauseVectorButton(ctx);
        playPauseBtn.setOnClickListener(v -> {
            MessageObject current = MediaController.getInstance().getPlayingMessageObject();
            if (current != null) {
                if (MediaController.getInstance().isMessagePaused()) {
                    MediaController.getInstance().playMessage(current);
                    startProgressTicker();
                } else {
                    MediaController.getInstance().pauseMessage(current);
                    stopProgressTicker();
                }
                updatePlayPauseState();
            }
        });
        controls.addView(playPauseBtn, LayoutHelper.createLinear(68, 68, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        SkipVectorButton nextBtn = new SkipVectorButton(ctx, false);
        nextBtn.setOnClickListener(v -> MediaController.getInstance().playNextMessage());
        controls.addView(nextBtn, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        repeatBtn = new ImageView(ctx);
        repeatBtn.setImageResource(R.drawable.player_new_repeatall);
        repeatBtn.setColorFilter(SharedConfig.repeatMode > 0 ? 0xFFFF4081 : 0x88FFFFFF);
        repeatBtn.setOnClickListener(v -> {
            SharedConfig.setRepeatMode((SharedConfig.repeatMode + 1) % 3);
            repeatBtn.setColorFilter(SharedConfig.repeatMode > 0 ? 0xFFFF4081 : 0x88FFFFFF);
        });
        controls.addView(repeatBtn, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        content.addView(controls, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // 5. Live mini-bass visualizer
        bassVisualizer = new MiogramBassVisualizer(ctx);
        content.addView(bassVisualizer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 26, 0, 6, 0, 0));

        root.addView(content);
        setCustomView(root);
    }

    private void startProgressTicker() {
        progressHandler.removeCallbacks(progressTicker);
        progressHandler.post(progressTicker);
    }

    private void stopProgressTicker() {
        progressHandler.removeCallbacks(progressTicker);
    }

    private void updateTrackInfo() {
        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (playing == null) {
            titleView.setText(MiogramLocale.get("Не відтворюється", "Не воспроизводится", "Not Playing"));
            artistView.setText("");
            return;
        }

        String title = playing.getMusicTitle();
        String artist = playing.getMusicAuthor();

        titleView.setText(title != null && !title.isEmpty() ? title : "Audio Track");
        artistView.setText(artist != null && !artist.isEmpty() ? artist : "Miogram Music");

        AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
        if (audioInfo != null && audioInfo.getCover() != null) {
            albumArtView.setImageBitmap(audioInfo.getCover());
            backdropView.setImageBitmap(audioInfo.getCover());
        } else {
            String artworkUrl = playing.getArtworkUrl(false);
            TLRPC.Document doc = playing.getDocument();
            TLRPC.PhotoSize thumb = doc != null ? FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 360) : null;
            if (artworkUrl != null && !artworkUrl.isEmpty()) {
                albumArtView.setImage(ImageLocation.getForPath(artworkUrl), "270_270", thumb != null ? ImageLocation.getForDocument(thumb, doc) : null, null, null, 0, 1, playing);
                backdropView.setImage(ImageLocation.getForPath(artworkUrl), "420_420", null, null, playing);
            } else if (thumb != null) {
                albumArtView.setImage(ImageLocation.getForDocument(thumb, doc), "270_270", null, null, playing);
                backdropView.setImage(ImageLocation.getForDocument(thumb, doc), "420_420", null, null, playing);
            }
        }

        updatePlayPauseState();
    }

    private void updatePlayPauseState() {
        boolean isPaused = MediaController.getInstance().isMessagePaused();
        if (playPauseBtn != null) {
            playPauseBtn.setPlaying(!isPaused);
        }
        if (isPaused) {
            stopProgressTicker();
        } else {
            startProgressTicker();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
            updateTrackInfo();
        } else if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            if (!isSeeking) {
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null && playing.audioProgressMs > 0) {
                    progressBar.setProgressFraction(playing.audioProgress);

                    int total = (int) Math.round(playing.getDuration());
                    int cur = (int) (total * playing.audioProgress);
                    timeElapsedView.setText(AndroidUtilities.formatShortDuration(cur));
                    timeRemainingView.setText("-" + AndroidUtilities.formatShortDuration(Math.max(0, total - cur)));
                }
            }
        }
    }

    @Override
    public void dismiss() {
        stopProgressTicker();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        super.dismiss();
    }

    /**
     * Thin Apple Music scrubber: 4dp track, white active line and a circular
     * knob that scales up while dragging. Pure vector, no platform styling.
     */
    private static class AppleSeekBar extends View {

        public interface Listener {
            void onSeekChanged(float fraction);

            void onSeekStarted();

            void onSeekFinished(float fraction);
        }

        private float fraction = 0f;
        private boolean dragging = false;
        private float knobScale = 1f;
        private Listener listener;

        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public AppleSeekBar(Context context) {
            super(context);
            trackPaint.setColor(0x33FFFFFF);
            activePaint.setColor(0xFFFFFFFF);
            knobPaint.setColor(0xFFFFFFFF);
        }

        public void setListener(Listener l) {
            listener = l;
        }

        public void setProgressFraction(float f) {
            if (dragging) return;
            fraction = Math.max(0f, Math.min(1f, f));
            invalidate();
        }

        public float getProgressFraction() {
            return fraction;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cy = getHeight() / 2f;
            float radius = AndroidUtilities.dp(2);
            canvas.drawRoundRect(new RectF(0, cy - radius, getWidth(), cy + radius), radius, radius, trackPaint);

            float knobX = Math.max(AndroidUtilities.dp(6), Math.min(getWidth() - AndroidUtilities.dp(6), getWidth() * fraction));
            canvas.drawRoundRect(new RectF(0, cy - radius, knobX, cy + radius), radius, radius, activePaint);

            float kr = AndroidUtilities.dp(6) * knobScale;
            knobPaint.setAlpha(dragging ? 255 : 235);
            canvas.drawCircle(knobX, cy, kr, knobPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = Math.max(0f, Math.min(getWidth(), event.getX()));
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = true;
                    animateKnob(1.35f);
                    if (listener != null) listener.onSeekStarted();
                    updateFraction(x);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragging) updateFraction(x);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (dragging) {
                        dragging = false;
                        animateKnob(1f);
                        float f = fraction;
                        if (listener != null) listener.onSeekFinished(f);
                    }
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private void updateFraction(float x) {
            fraction = getWidth() > 0 ? x / getWidth() : 0f;
            invalidate();
            if (listener != null) listener.onSeekChanged(fraction);
        }

        private void animateKnob(float target) {
            knobScale = target;
            invalidate();
        }
    }

    /**
     * Vector Apple Music Skip Button
     */
    private static class SkipVectorButton extends View {
        private final boolean isPrev;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SkipVectorButton(Context context, boolean isPrev) {
            super(context);
            this.isPrev = isPrev;
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float size = AndroidUtilities.dp(14);

            Path path = new Path();
            if (isPrev) {
                path.moveTo(cx, cy - size / 2f);
                path.lineTo(cx - size / 2f, cy);
                path.lineTo(cx, cy + size / 2f);
                path.close();

                path.moveTo(cx + size / 2f, cy - size / 2f);
                path.lineTo(cx, cy);
                path.lineTo(cx + size / 2f, cy + size / 2f);
                path.close();
            } else {
                path.moveTo(cx - size / 2f, cy - size / 2f);
                path.lineTo(cx, cy);
                path.lineTo(cx - size / 2f, cy + size / 2f);
                path.close();

                path.moveTo(cx, cy - size / 2f);
                path.lineTo(cx + size / 2f, cy);
                path.lineTo(cx, cy + size / 2f);
                path.close();
            }
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Vector Apple Music Play/Pause Round Button
     */
    private static class PlayPauseVectorButton extends View {
        private boolean isPlaying = false;
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public PlayPauseVectorButton(Context context) {
            super(context);
            bgPaint.setColor(0x28FFFFFF);
            bgPaint.setStyle(Paint.Style.FILL);
            iconPaint.setColor(0xFFFFFFFF);
            iconPaint.setStyle(Paint.Style.FILL);
        }

        public void setPlaying(boolean playing) {
            if (this.isPlaying != playing) {
                this.isPlaying = playing;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - AndroidUtilities.dp(2);

            canvas.drawCircle(cx, cy, r, bgPaint);

            if (isPlaying) {
                float barW = AndroidUtilities.dp(5);
                float barH = AndroidUtilities.dp(18);
                float gap = AndroidUtilities.dp(5);

                RectF leftBar = new RectF(cx - gap / 2f - barW, cy - barH / 2f, cx - gap / 2f, cy + barH / 2f);
                RectF rightBar = new RectF(cx + gap / 2f, cy - barH / 2f, cx + gap / 2f + barW, cy + barH / 2f);
                canvas.drawRoundRect(leftBar, AndroidUtilities.dp(2), AndroidUtilities.dp(2), iconPaint);
                canvas.drawRoundRect(rightBar, AndroidUtilities.dp(2), AndroidUtilities.dp(2), iconPaint);
            } else {
                float triH = AndroidUtilities.dp(20);
                float triW = AndroidUtilities.dp(16);

                Path playPath = new Path();
                playPath.moveTo(cx - triW / 3f, cy - triH / 2f);
                playPath.lineTo(cx + triW * 2f / 3f, cy);
                playPath.lineTo(cx - triW / 3f, cy + triH / 2f);
                playPath.close();
                canvas.drawPath(playPath, iconPaint);
            }
        }
    }

    /**
     * Vector Heart Button
     */
    private static class HeartVectorButton extends View {
        private boolean isFavorite = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HeartVectorButton(Context context) {
            super(context);
            paint.setStyle(Paint.Style.FILL);
        }

        public void setFavorite(boolean fav) {
            this.isFavorite = fav;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setColor(isFavorite ? 0xFFFF4081 : 0x77FFFFFF);

            Path heart = new Path();
            float w = AndroidUtilities.dp(16);
            float h = AndroidUtilities.dp(14);

            heart.moveTo(cx, cy + h / 2f);
            heart.cubicTo(cx - w, cy - h / 3f, cx - w / 2f, cy - h, cx, cy - h / 3f);
            heart.cubicTo(cx + w / 2f, cy - h, cx + w, cy - h / 3f, cx, cy + h / 2f);
            heart.close();

            canvas.drawPath(heart, paint);
        }
    }
}
