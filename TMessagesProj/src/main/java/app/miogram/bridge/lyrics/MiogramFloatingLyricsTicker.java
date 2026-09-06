package app.miogram.bridge.lyrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;

import app.miogram.bridge.ui.MiogramVisualsPrefs;

/**
 * Floating non-clickable active lyrics ticker (bottom subtitle pill).
 * Displays the current line of the playing song in sync with playback.
 * 100% touch-transparent: touches pass straight through to input fields, buttons, and chat lists.
 */
public class MiogramFloatingLyricsTicker implements NotificationCenter.NotificationCenterDelegate {

    private static volatile MiogramFloatingLyricsTicker instance;

    public static MiogramFloatingLyricsTicker getInstance() {
        if (instance == null) {
            synchronized (MiogramFloatingLyricsTicker.class) {
                if (instance == null) {
                    instance = new MiogramFloatingLyricsTicker();
                }
            }
        }
        return instance;
    }

    private FloatingTickerLayout tickerLayout;
    private LaunchActivity currentActivity;
    private String currentText = "";
    private boolean isVisible = false;

    private MiogramFloatingLyricsTicker() {}

    public void attach(LaunchActivity activity) {
        if (activity == null || activity.isFinishing()) return;
        this.currentActivity = activity;

        if (tickerLayout == null || tickerLayout.getContext() != activity) {
            tickerLayout = new FloatingTickerLayout(activity);
        }

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        if (tickerLayout.getParent() != null) {
            ((ViewGroup) tickerLayout.getParent()).removeView(tickerLayout);
        }
        decor.addView(tickerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingDidStart);

        updateLyricsState();
    }

    public void detach() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingDidStart);

        if (tickerLayout != null && tickerLayout.getParent() != null) {
            ((ViewGroup) tickerLayout.getParent()).removeView(tickerLayout);
        }
        tickerLayout = null;
        currentActivity = null;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingProgressDidChanged
                || id == NotificationCenter.messagePlayingPlayStateChanged
                || id == NotificationCenter.messagePlayingDidStart) {
            updateLyricsState();
        } else if (id == NotificationCenter.messagePlayingDidReset) {
            hideTicker(true);
        }
    }

    private int lastRequestedMessageId = -1;

    public void updateLyricsState() {
        if (currentActivity == null || tickerLayout == null) return;

        Context context = currentActivity;
        boolean enabled = MiogramVisualsPrefs.loadBool(context, "player_active_lyric", true);
        if (!enabled) {
            hideTicker(true);
            return;
        }

        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (playing == null || MediaController.getInstance().isMessagePaused()) {
            hideTicker(true);
            return;
        }

        MiogramLrcModel.LrcSong song = MiogramLyricsEngine.getInstance().getCachedSong(playing);
        if (song == null) {
            if (lastRequestedMessageId != playing.getId()) {
                lastRequestedMessageId = playing.getId();
                MiogramLyricsEngine.getInstance().fetchLyrics(playing, new MiogramLyricsEngine.LyricsCallback() {
                    @Override
                    public void onLyricsLoaded(MiogramLrcModel.LrcSong s) {
                        AndroidUtilities.runOnUIThread(() -> updateLyricsState());
                    }

                    @Override
                    public void onLyricsError(String error) {
                    }
                });
            }
            hideTicker(true);
            return;
        }

        if (song.lines.isEmpty()) {
            hideTicker(true);
            return;
        }

        long progressMs = playing.audioProgressMs > 0 ? playing.audioProgressMs : (long) (playing.audioProgress * playing.getDuration() * 1000f);
        int activeIdx = song.findLineIndex(progressMs);
        if (activeIdx < 0 || activeIdx >= song.lines.size()) {
            hideTicker(true);
            return;
        }

        String line = song.lines.get(activeIdx).text;
        if (TextUtils.isEmpty(line)) {
            hideTicker(true);
            return;
        }

        showText(line);
    }

    private void showText(String text) {
        if (tickerLayout == null) return;
        if (text.equals(currentText) && isVisible) return;
        this.currentText = text;

        tickerLayout.updatePosition(currentActivity);
        tickerLayout.setText(text);

        if (!isVisible) {
            isVisible = true;
            tickerLayout.setVisibility(View.VISIBLE);
            tickerLayout.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(220).setListener(null).start();
        }
    }

    public void hideTicker(boolean animated) {
        if (!isVisible || tickerLayout == null) return;
        isVisible = false;
        currentText = "";

        if (animated) {
            tickerLayout.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(180)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!isVisible && tickerLayout != null) {
                                tickerLayout.setVisibility(View.GONE);
                            }
                        }
                    }).start();
        } else {
            tickerLayout.setVisibility(View.GONE);
            tickerLayout.setAlpha(0f);
        }
    }

    /**
     * Completely non-clickable full-screen overlay containing the centered floating pill.
     * Overrides all touch handling to return false so taps go directly to chat, keyboard, or lists.
     */
    public static class FloatingTickerLayout extends FrameLayout {

        private final LinearLayout pillContainer;
        private final TextView noteIconView;
        private final TextView lyricTextView;

        public FloatingTickerLayout(Context context) {
            super(context);
            setFocusable(false);
            setClickable(false);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            setVisibility(View.GONE);
            setAlpha(0f);

            pillContainer = new LinearLayout(context);
            pillContainer.setOrientation(LinearLayout.HORIZONTAL);
            pillContainer.setGravity(Gravity.CENTER_VERTICAL);
            pillContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(0xD80D111A); // Deep midnight frosted glass
            pillBg.setCornerRadius(AndroidUtilities.dp(17));
            pillBg.setStroke(AndroidUtilities.dp(1), 0x2EFFFFFF);
            pillContainer.setBackground(pillBg);
            if (Build.VERSION.SDK_INT >= 21) {
                pillContainer.setElevation(AndroidUtilities.dp(6));
            }

            noteIconView = new TextView(context);
            noteIconView.setText("🎵");
            noteIconView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            noteIconView.setTextColor(0xFFFFFFFF);
            pillContainer.addView(noteIconView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

            lyricTextView = new TextView(context);
            lyricTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            lyricTextView.setTypeface(AndroidUtilities.bold());
            lyricTextView.setTextColor(0xFFFFFFFF);
            lyricTextView.setShadowLayer(AndroidUtilities.dp(2), 0, 1, 0xAA000000);
            lyricTextView.setSingleLine(true);
            lyricTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            lyricTextView.setSelected(true);
            pillContainer.addView(lyricTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    AndroidUtilities.dp(34),
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );
            lp.bottomMargin = AndroidUtilities.dp(64);
            addView(pillContainer, lp);
        }

        public void setText(String text) {
            lyricTextView.setText(text);
        }

        public void updatePosition(LaunchActivity activity) {
            if (activity == null) return;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pillContainer.getLayoutParams();
            if (lp == null) return;

            BaseFragment fragment = activity.getActionBarLayout() != null ? activity.getActionBarLayout().getLastFragment() : null;
            int bottomMargin = AndroidUtilities.dp(64);

            if (fragment instanceof ChatActivity) {
                ChatActivity chat = (ChatActivity) fragment;
                View enterView = chat.getChatActivityEnterView();
                if (enterView != null && enterView.getVisibility() == View.VISIBLE && enterView.getHeight() > 0) {
                    bottomMargin = enterView.getHeight() + AndroidUtilities.dp(10);
                } else {
                    bottomMargin = AndroidUtilities.dp(24);
                }
            } else if (fragment instanceof DialogsActivity) {
                bottomMargin = AndroidUtilities.dp(68);
            }

            if (lp.bottomMargin != bottomMargin) {
                lp.bottomMargin = bottomMargin;
                pillContainer.setLayoutParams(lp);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false; // Passthrough to underlying views
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return false; // Never intercept
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            return false; // Pass through completely
        }
    }
}
