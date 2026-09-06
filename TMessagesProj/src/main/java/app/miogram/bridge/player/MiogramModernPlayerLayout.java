package app.miogram.bridge.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.lyrics.MiogramLyricsView;
import app.miogram.bridge.ui.MiogramVisualsPrefs;

/**
 * Modern High-Fidelity Telegram Audio Player Layout.
 * 1. Compact Sheet (~440dp):
 *    - Rounded top corners (24dp), drag handle
 *    - 115x115dp album art, marquee title/artist, active lyric pill
 *    - Seekbar & playback controls
 * 2. Full-Screen:
 *    - Status-bar inset padding (prevents overlap)
 *    - Clean mode tabs: [ Lyrics | Cover | Queue ]
 *    - Full-height lyrics view with karaoke, translation & source options
 *    - Synchronized with active Telegram theme accent (red, blue, purple, etc.)
 */
public class MiogramModernPlayerLayout extends FrameLayout {

    public enum PlayerMode {
        LYRICS,
        COVER,
        QUEUE
    }

    private final AudioPlayerAlert alert;
    private final Theme.ResourcesProvider resourcesProvider;

    private boolean isFullScreen = false;
    private float fullScreenProgress = 0f;
    private PlayerMode playerMode = PlayerMode.LYRICS;

    // Header / Top section
    private final LinearLayout topSection;
    private final View dragHandle;
    private final LinearLayout topControlsRow;
    private final ImageView collapseBtn;
    private final LinearLayout pageSwitcher;
    private final TextView lyricsModeButton;
    private final TextView coverModeButton;
    private final TextView queueModeButton;
    private final ImageView expandOrCloseBtn;

    // Center Section: Compact Info vs Full Content
    private final FrameLayout centerContainer;
    private final LinearLayout compactInfoContainer;
    private final FrameLayout compactCoverWrapper;
    private final TextView compactTitleView;
    private final TextView compactAuthorView;
    private final TextView compactActiveLyricView;

    private final FrameLayout fullContentContainer;
    private final FrameLayout fullCoverWrapper;
    private final FrameLayout queueContainer;
    private MiogramLyricsView lyricsView;
    private View queueListView;
    private View coverView;

    // Bottom Controls Section
    private final LinearLayout bottomSection;
    private final LinearLayout seekbarContainer;
    private View seekBarView;
    private final FrameLayout timersRow;
    private View timeView;
    private View durationView;

    private final LinearLayout mainControlsRow;
    private View repeatButton;
    private View prevButton;
    private FrameLayout heroPlayButton;
    private View playButton;
    private View nextButton;
    private ImageView queueButton;

    // Profile Button Container
    private final FrameLayout profileButtonContainer;
    private View saveToProfileButton;
    private View unsaveFromProfileButton;

    private MessageObject currentMessageObject;
    private boolean isPlaying = false;

    public MiogramModernPlayerLayout(Context context, AudioPlayerAlert alert, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.alert = alert;
        this.resourcesProvider = resourcesProvider;

        updateBackgroundShape(0f);

        int accentColor = getThemeAccentColor();
        int buttonColor = getThemedColor(Theme.key_player_button);
        if (buttonColor == 0) buttonColor = 0xFF888888;

        // --- 1. Top Section (Drag Handle + Header Controls) ---
        topSection = new LinearLayout(context);
        topSection.setOrientation(LinearLayout.VERTICAL);
        topSection.setGravity(Gravity.CENTER_HORIZONTAL);
        topSection.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        dragHandle = new View(context);
        dragHandle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0x44888888));
        topSection.addView(dragHandle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        topControlsRow = new LinearLayout(context);
        topControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        topControlsRow.setGravity(Gravity.CENTER_VERTICAL);

        // Left button: Collapse / Close down chevron
        collapseBtn = new ImageView(context);
        collapseBtn.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);
        collapseBtn.setScaleType(ImageView.ScaleType.CENTER);
        collapseBtn.setColorFilter(new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN));
        collapseBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        collapseBtn.setContentDescription(MiogramLocale.get("Згорнути", "Свернуть", "Collapse"));
        collapseBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (isFullScreen) {
                if (alert != null) alert.setFullScreen(false, true);
            } else {
                if (alert != null) alert.dismiss();
            }
        });
        topControlsRow.addView(collapseBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL));

        // Center Switcher: [ Lyrics | Cover | Queue ] (Visible only in FullScreen)
        pageSwitcher = new LinearLayout(context);
        pageSwitcher.setOrientation(LinearLayout.HORIZONTAL);
        pageSwitcher.setGravity(Gravity.CENTER);
        pageSwitcher.setPadding(AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3));
        pageSwitcher.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), ColorUtils.setAlphaComponent(accentColor, 32)));
        pageSwitcher.setVisibility(View.GONE);

        lyricsModeButton = createModeButton(MiogramLocale.get("Текст", "Текст", "Lyrics"));
        coverModeButton = createModeButton(MiogramLocale.get("Обкладинка", "Обложка", "Cover"));
        queueModeButton = createModeButton(MiogramLocale.get("Черга", "Очередь", "Queue"));

        lyricsModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showLyrics(true);
        });
        coverModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showCover(true);
        });
        queueModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showQueue(true, true);
        });

        pageSwitcher.addView(lyricsModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(30), 1f));
        pageSwitcher.addView(coverModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(30), 1f));
        pageSwitcher.addView(queueModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(30), 1f));
        topControlsRow.addView(pageSwitcher, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(36), 1f));

        // Right button: Expand to fullscreen (in compact) or Dismiss 'X' (in fullscreen)
        expandOrCloseBtn = new ImageView(context);
        expandOrCloseBtn.setImageResource(R.drawable.baseline_fullscreen_24);
        expandOrCloseBtn.setScaleType(ImageView.ScaleType.CENTER);
        expandOrCloseBtn.setColorFilter(new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN));
        expandOrCloseBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        expandOrCloseBtn.setContentDescription(MiogramLocale.get("Розгорнути", "Развернуть", "Expand"));
        expandOrCloseBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (!isFullScreen) {
                if (alert != null) alert.setFullScreen(true, true);
            } else {
                if (alert != null) alert.dismiss();
            }
        });
        topControlsRow.addView(expandOrCloseBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL));

        topSection.addView(topControlsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addView(topSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // --- 2. Center Container ---
        centerContainer = new FrameLayout(context);
        addView(centerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 56, 0, 142));

        // 2A: Compact Info Container
        compactInfoContainer = new LinearLayout(context);
        compactInfoContainer.setOrientation(LinearLayout.VERTICAL);
        compactInfoContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        compactInfoContainer.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), 0);

        compactCoverWrapper = new FrameLayout(context);
        if (Build.VERSION.SDK_INT >= 21) {
            compactCoverWrapper.setElevation(AndroidUtilities.dp(8));
            compactCoverWrapper.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(16));
                }
            });
            compactCoverWrapper.setClipToOutline(true);
        }
        compactCoverWrapper.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (alert != null) alert.setFullScreen(true, true);
        });
        compactInfoContainer.addView(compactCoverWrapper, LayoutHelper.createLinear(115, 115, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        compactTitleView = new TextView(context);
        compactTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        compactTitleView.setTypeface(AndroidUtilities.bold());
        compactTitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        compactTitleView.setGravity(Gravity.CENTER);
        compactTitleView.setSingleLine(true);
        compactTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        compactTitleView.setSelected(true);
        compactTitleView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        compactTitleView.setOnClickListener(v -> {
            if (alert != null) alert.setFullScreen(true, true);
        });
        compactInfoContainer.addView(compactTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        compactAuthorView = new TextView(context);
        compactAuthorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        compactAuthorView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        compactAuthorView.setGravity(Gravity.CENTER);
        compactAuthorView.setSingleLine(true);
        compactAuthorView.setEllipsize(TextUtils.TruncateAt.END);
        compactAuthorView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        compactInfoContainer.addView(compactAuthorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        compactActiveLyricView = new TextView(context);
        compactActiveLyricView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        compactActiveLyricView.setTypeface(AndroidUtilities.bold());
        compactActiveLyricView.setTextColor(accentColor);
        compactActiveLyricView.setGravity(Gravity.CENTER);
        compactActiveLyricView.setSingleLine(true);
        compactActiveLyricView.setEllipsize(TextUtils.TruncateAt.END);
        compactActiveLyricView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(4), AndroidUtilities.dp(14), AndroidUtilities.dp(4));
        compactActiveLyricView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), ColorUtils.setAlphaComponent(accentColor, 26)));
        compactActiveLyricView.setVisibility(View.GONE);
        compactActiveLyricView.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showLyrics(false);
            if (alert != null) alert.setFullScreen(true, true);
        });
        compactInfoContainer.addView(compactActiveLyricView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        centerContainer.addView(compactInfoContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // 2B: Full Content Container
        fullContentContainer = new FrameLayout(context);
        fullContentContainer.setVisibility(View.GONE);

        fullCoverWrapper = new FrameLayout(context);
        fullCoverWrapper.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= 21) {
            fullCoverWrapper.setElevation(AndroidUtilities.dp(12));
            fullCoverWrapper.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(22));
                }
            });
            fullCoverWrapper.setClipToOutline(true);
        }
        fullContentContainer.addView(fullCoverWrapper, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        queueContainer = new FrameLayout(context);
        queueContainer.setVisibility(View.GONE);
        int surface = getThemedColor(Theme.key_player_background);
        if (surface == 0) surface = getThemedColor(Theme.key_windowBackgroundWhite);
        queueContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.blendARGB(surface, accentColor, 0.08f), surface}));
        fullContentContainer.addView(queueContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        centerContainer.addView(fullContentContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // --- 3. Bottom Controls Section ---
        bottomSection = new LinearLayout(context);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setGravity(Gravity.BOTTOM);
        bottomSection.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        bottomSection.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.setAlphaComponent(surface, 0), surface}));

        // Seekbar & Timestamps
        seekbarContainer = new LinearLayout(context);
        seekbarContainer.setOrientation(LinearLayout.VERTICAL);
        timersRow = new FrameLayout(context);
        seekbarContainer.addView(timersRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        bottomSection.addView(seekbarContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        // Main Controls Row (Repeat, Prev, Hero Play/Pause, Next, Queue)
        mainControlsRow = new LinearLayout(context);
        mainControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        mainControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        mainControlsRow.setWeightSum(5);

        heroPlayButton = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable();
        heroBg.setColor(accentColor);
        heroBg.setShape(GradientDrawable.OVAL);
        heroPlayButton.setBackground(heroBg);
        if (Build.VERSION.SDK_INT >= 21) {
            heroPlayButton.setElevation(AndroidUtilities.dp(4));
        }

        bottomSection.addView(mainControlsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 2, 0, 4));

        // Profile Button Container ("+ Додати в профіль")
        profileButtonContainer = new FrameLayout(context);
        bottomSection.addView(profileButtonContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        addView(bottomSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
        updateModeButtons();
    }

    private TextView createModeButton(String text) {
        TextView button = new TextView(getContext());
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setContentDescription(text);
        return button;
    }

    public void setLyricsView(MiogramLyricsView view) {
        this.lyricsView = view;
        if (view != null) {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            view.setOnCloseClickListener(() -> {
                if (alert != null) {
                    if (isFullScreen) {
                        alert.setFullScreen(false, true);
                    } else {
                        alert.dismiss();
                    }
                }
            });
            view.setOnActiveLineChangeListener((text, translation, index) -> updateActiveLyric(text));
            fullContentContainer.addView(view, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            view.setVisibility(playerMode == PlayerMode.LYRICS ? View.VISIBLE : View.GONE);
            view.setAlpha(playerMode == PlayerMode.LYRICS ? 1f : 0f);
        }
    }

    public void setCoverView(View cover) {
        this.coverView = cover;
        updateCoverAttachment();
    }

    private void updateCoverAttachment() {
        if (coverView == null) return;
        if (coverView.getParent() instanceof ViewGroup) {
            ((ViewGroup) coverView.getParent()).removeView(coverView);
        }
        if (!isFullScreen || fullScreenProgress < 0.5f) {
            compactCoverWrapper.removeAllViews();
            int size = AndroidUtilities.dp(115);
            compactCoverWrapper.addView(coverView, LayoutHelper.createFrame(size, size, Gravity.CENTER));
        } else if (playerMode == PlayerMode.COVER) {
            fullCoverWrapper.removeAllViews();
            int size = Math.min(AndroidUtilities.dp(250), AndroidUtilities.displaySize.x - AndroidUtilities.dp(64));
            fullCoverWrapper.addView(coverView, LayoutHelper.createFrame(size, size, Gravity.CENTER));
        }
    }

    public void setQueueListView(View listView) {
        this.queueListView = listView;
        if (queueContainer != null && listView != null) {
            if (listView.getParent() instanceof ViewGroup) {
                ((ViewGroup) listView.getParent()).removeView(listView);
            }
            queueContainer.removeAllViews();
            listView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
            if (listView instanceof ViewGroup) {
                ((ViewGroup) listView).setClipToPadding(false);
            }
            queueContainer.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }
    }

    public void setSeekBarViews(View seekBar, View time, View duration) {
        this.seekBarView = seekBar;
        this.timeView = time;
        this.durationView = duration;

        if (seekbarContainer != null && seekBar != null) {
            if (seekBar.getParent() instanceof ViewGroup) {
                ((ViewGroup) seekBar.getParent()).removeView(seekBar);
            }
            seekbarContainer.addView(seekBar, 0, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 30));
        }

        if (timersRow != null) {
            timersRow.removeAllViews();
            if (time != null) {
                if (time.getParent() instanceof ViewGroup) {
                    ((ViewGroup) time.getParent()).removeView(time);
                }
                timersRow.addView(time, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 4, 0, 0, 0));
            }
            if (duration != null) {
                if (duration.getParent() instanceof ViewGroup) {
                    ((ViewGroup) duration.getParent()).removeView(duration);
                }
                timersRow.addView(duration, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
            }
        }
    }

    public void setControlButtons(View repeat, View prev, View play, View next) {
        this.repeatButton = repeat;
        this.prevButton = prev;
        this.playButton = play;
        this.nextButton = next;

        if (mainControlsRow != null) {
            mainControlsRow.removeAllViews();

            // Slot 0: Repeat
            FrameLayout slot0 = new FrameLayout(getContext());
            if (repeat != null) {
                if (repeat.getParent() instanceof ViewGroup) ((ViewGroup) repeat.getParent()).removeView(repeat);
                repeat.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (alert != null) {
                        int mode = org.telegram.messenger.SharedConfig.repeatMode;
                        if (mode == 0) org.telegram.messenger.SharedConfig.setRepeatMode(1);
                        else if (mode == 1) org.telegram.messenger.SharedConfig.setRepeatMode(2);
                        else org.telegram.messenger.SharedConfig.setRepeatMode(0);
                        alert.updateRepeatButton();
                    }
                });
                slot0.addView(repeat, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            }
            mainControlsRow.addView(slot0, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 1: Previous
            FrameLayout slot1 = new FrameLayout(getContext());
            if (prev != null) {
                if (prev.getParent() instanceof ViewGroup) ((ViewGroup) prev.getParent()).removeView(prev);
                prev.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playPreviousMessage();
                });
                slot1.addView(prev, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot1, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 2: Hero Play/Pause Button (54dp)
            FrameLayout slot2 = new FrameLayout(getContext());
            if (play != null) {
                if (play.getParent() instanceof ViewGroup) ((ViewGroup) play.getParent()).removeView(play);
                play.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (MediaController.getInstance().isDownloadingCurrentMessage()) return;
                    if (MediaController.getInstance().isMessagePaused()) {
                        MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
                    } else {
                        MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
                    }
                });
                heroPlayButton.removeAllViews();
                heroPlayButton.addView(play, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            }
            slot2.addView(heroPlayButton, LayoutHelper.createFrame(54, 54, Gravity.CENTER));
            mainControlsRow.addView(slot2, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 3: Next
            FrameLayout slot3 = new FrameLayout(getContext());
            if (next != null) {
                if (next.getParent() instanceof ViewGroup) ((ViewGroup) next.getParent()).removeView(next);
                next.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playNextMessage();
                });
                slot3.addView(next, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot3, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 4: Queue / Mode Toggle Button
            FrameLayout slot4 = new FrameLayout(getContext());
            queueButton = new ImageView(getContext());
            queueButton.setImageResource(R.drawable.player_new_order);
            queueButton.setScaleType(ImageView.ScaleType.CENTER);
            int buttonColor = getThemedColor(Theme.key_player_button);
            if (buttonColor == 0) buttonColor = 0xFF888888;
            queueButton.setColorFilter(new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN));
            queueButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
            queueButton.setContentDescription(MiogramLocale.get("Черга", "Очередь", "Queue"));
            queueButton.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (!isFullScreen) {
                    showQueue(true, false);
                    if (alert != null) alert.setFullScreen(true, true);
                } else {
                    toggleQueue();
                }
            });
            slot4.addView(queueButton, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            mainControlsRow.addView(slot4, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }
    }

    public void setProfileButtons(View saveBtn, View unsaveBtn) {
        this.saveToProfileButton = saveBtn;
        this.unsaveFromProfileButton = unsaveBtn;

        if (profileButtonContainer != null) {
            profileButtonContainer.removeAllViews();
            if (saveBtn != null) {
                if (saveBtn.getParent() instanceof ViewGroup) ((ViewGroup) saveBtn.getParent()).removeView(saveBtn);
                profileButtonContainer.addView(saveBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.CENTER));
            }
            if (unsaveBtn != null) {
                if (unsaveBtn.getParent() instanceof ViewGroup) ((ViewGroup) unsaveBtn.getParent()).removeView(unsaveBtn);
                profileButtonContainer.addView(unsaveBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.CENTER));
            }
        }
    }

    public void setFullScreen(boolean fullScreen, boolean animated) {
        this.isFullScreen = fullScreen;
        this.fullScreenProgress = fullScreen ? 1.0f : 0.0f;
        applyFullScreenVisualState(animated);
    }

    public void setFullScreenProgress(float progress) {
        this.fullScreenProgress = Math.max(0f, Math.min(1f, progress));
        this.isFullScreen = this.fullScreenProgress >= 0.5f;
        updateBackgroundShape(this.fullScreenProgress);

        int statusBar = AndroidUtilities.statusBarHeight;
        int topPadding = AndroidUtilities.dp(8) + (int) (statusBar * this.fullScreenProgress);
        topSection.setPadding(AndroidUtilities.dp(16), topPadding, AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        int centerTopMargin = AndroidUtilities.dp(56) + (int) (statusBar * this.fullScreenProgress);
        if (centerContainer.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) centerContainer.getLayoutParams();
            if (clp.topMargin != centerTopMargin) {
                clp.topMargin = centerTopMargin;
                centerContainer.setLayoutParams(clp);
            }
        }

        dragHandle.setAlpha(Math.max(0f, 1.0f - this.fullScreenProgress * 2f));
        dragHandle.setVisibility(this.fullScreenProgress >= 0.9f ? View.GONE : View.VISIBLE);

        compactInfoContainer.setAlpha(Math.max(0f, 1.0f - this.fullScreenProgress * 2.2f));
        fullContentContainer.setAlpha(Math.max(0f, (this.fullScreenProgress - 0.2f) / 0.8f));
        pageSwitcher.setAlpha(Math.max(0f, (this.fullScreenProgress - 0.35f) / 0.65f));

        if (this.fullScreenProgress <= 0.05f) {
            compactInfoContainer.setVisibility(View.VISIBLE);
            fullContentContainer.setVisibility(View.GONE);
            pageSwitcher.setVisibility(View.GONE);
            expandOrCloseBtn.setImageResource(R.drawable.baseline_fullscreen_24);
            expandOrCloseBtn.setContentDescription(MiogramLocale.get("Розгорнути", "Развернуть", "Expand"));
            updateCoverAttachment();
        } else if (this.fullScreenProgress >= 0.95f) {
            compactInfoContainer.setVisibility(View.GONE);
            fullContentContainer.setVisibility(View.VISIBLE);
            pageSwitcher.setVisibility(View.VISIBLE);
            expandOrCloseBtn.setImageResource(R.drawable.ic_ab_close);
            expandOrCloseBtn.setContentDescription(MiogramLocale.get("Закрити", "Закрыть", "Close"));
            updateCoverAttachment();
        } else {
            compactInfoContainer.setVisibility(View.VISIBLE);
            fullContentContainer.setVisibility(View.VISIBLE);
            pageSwitcher.setVisibility(View.VISIBLE);
        }
    }

    private void applyFullScreenVisualState(boolean animated) {
        updateBackgroundShape(fullScreenProgress);

        int statusBar = AndroidUtilities.statusBarHeight;
        int topPadding = isFullScreen ? (statusBar + AndroidUtilities.dp(6)) : AndroidUtilities.dp(8);
        topSection.setPadding(AndroidUtilities.dp(16), topPadding, AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        int centerTopMargin = isFullScreen ? (statusBar + AndroidUtilities.dp(56)) : AndroidUtilities.dp(56);
        if (centerContainer.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) centerContainer.getLayoutParams();
            clp.topMargin = centerTopMargin;
            centerContainer.setLayoutParams(clp);
        }

        pageSwitcher.setVisibility(isFullScreen ? View.VISIBLE : View.GONE);
        pageSwitcher.setAlpha(isFullScreen ? 1f : 0f);

        dragHandle.setVisibility(isFullScreen ? View.GONE : View.VISIBLE);
        dragHandle.setAlpha(isFullScreen ? 0f : 1f);

        expandOrCloseBtn.setImageResource(isFullScreen ? R.drawable.ic_ab_close : R.drawable.baseline_fullscreen_24);
        expandOrCloseBtn.setContentDescription(isFullScreen
                ? MiogramLocale.get("Закрити", "Закрыть", "Close")
                : MiogramLocale.get("Розгорнути", "Развернуть", "Expand"));

        compactInfoContainer.setVisibility(isFullScreen ? View.GONE : View.VISIBLE);
        compactInfoContainer.setAlpha(isFullScreen ? 0f : 1f);

        fullContentContainer.setVisibility(isFullScreen ? View.VISIBLE : View.GONE);
        fullContentContainer.setAlpha(isFullScreen ? 1f : 0f);

        updateCoverAttachment();
        updateModeButtons();
    }

    private void updateBackgroundShape(float progress) {
        int surface = getThemedColor(Theme.key_player_background);
        if (surface == 0) surface = getThemedColor(Theme.key_windowBackgroundWhite);
        int accentColor = getThemeAccentColor();

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ColorUtils.blendARGB(surface, accentColor, 0.12f), surface, ColorUtils.blendARGB(surface, 0xFF000000, 0.05f)});

        float radius = AndroidUtilities.dp(24) * (1.0f - progress);
        background.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        setBackground(background);
    }

    public int getThemeAccentColor() {
        int color = getThemedColor(Theme.key_player_progress);
        if (color == 0 || color == 0xFF3390EC) {
            int active = getThemedColor(Theme.key_player_buttonActive);
            if (active != 0 && active != 0xFF3390EC) return active;
            int chats = getThemedColor(Theme.key_chats_actionBackground);
            if (chats != 0 && chats != 0xFF3390EC) return chats;
            if (active != 0) return active;
        }
        return color != 0 ? color : 0xFF3390EC;
    }

    public void toggleQueue() {
        showQueue(!isQueueVisible(), true);
    }

    public boolean isQueueVisible() {
        return playerMode == PlayerMode.QUEUE;
    }

    public void showQueue(boolean show, boolean animated) {
        setPlayerMode(show ? PlayerMode.QUEUE : PlayerMode.LYRICS, animated);
    }

    public void showLyrics(boolean animated) {
        setPlayerMode(PlayerMode.LYRICS, animated);
    }

    public void showCover(boolean animated) {
        setPlayerMode(PlayerMode.COVER, animated);
    }

    private void setPlayerMode(PlayerMode mode, boolean animated) {
        if (playerMode == mode) return;
        PlayerMode previousMode = playerMode;
        playerMode = mode;

        if (mode == PlayerMode.COVER) {
            updateCoverAttachment();
        }

        View target = mode == PlayerMode.QUEUE ? queueContainer : mode == PlayerMode.COVER ? fullCoverWrapper : lyricsView;
        View previous = previousMode == PlayerMode.QUEUE ? queueContainer : previousMode == PlayerMode.COVER ? fullCoverWrapper : lyricsView;
        if (target == null) return;

        if (queueButton != null) {
            int color = mode == PlayerMode.QUEUE ? getThemeAccentColor() : getThemedColor(Theme.key_player_button);
            queueButton.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        updateModeButtons();

        cancelPageAnimation(target);
        if (previous != null) cancelPageAnimation(previous);
        target.setVisibility(View.VISIBLE);

        if (!animated || previous == null || previous == target) {
            setPageVisible(target, true);
            if (previous != null && previous != target) setPageVisible(previous, false);
            return;
        }

        target.setAlpha(0f);
        target.setScaleX(0.985f);
        target.setScaleY(0.985f);
        target.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start();
        final PlayerMode expectedMode = mode;
        previous.animate().alpha(0f).scaleX(0.985f).scaleY(0.985f).setDuration(180).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (playerMode == expectedMode) setPageVisible(previous, false);
            }
        }).start();
    }

    private void cancelPageAnimation(View page) {
        page.animate().setListener(null);
        page.animate().cancel();
    }

    private void setPageVisible(View page, boolean visible) {
        page.setVisibility(visible ? View.VISIBLE : View.GONE);
        page.setAlpha(visible ? 1f : 0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
    }

    private void updateModeButtons() {
        updateModeButton(lyricsModeButton, playerMode == PlayerMode.LYRICS);
        updateModeButton(coverModeButton, playerMode == PlayerMode.COVER);
        updateModeButton(queueModeButton, playerMode == PlayerMode.QUEUE);
    }

    private void updateModeButton(TextView button, boolean selected) {
        if (button == null) return;
        int accent = getThemeAccentColor();
        button.setTextColor(selected ? 0xFFFFFFFF : 0xB3FFFFFF);
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), selected ? accent : 0x00000000));
        button.setScaleX(selected ? 1f : 0.96f);
        button.setScaleY(selected ? 1f : 0.96f);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (compactCoverWrapper != null) {
            compactCoverWrapper.animate().scaleX(playing ? 1.0f : 0.94f).scaleY(playing ? 1.0f : 0.94f).setDuration(220).start();
        }
        if (fullCoverWrapper != null) {
            fullCoverWrapper.animate().scaleX(playing ? 1.0f : 0.94f).scaleY(playing ? 1.0f : 0.94f).setDuration(220).start();
        }
    }

    public void setSong(MessageObject messageObject) {
        this.currentMessageObject = messageObject;
        updateActiveLyric(null);
        if (messageObject != null) {
            String title = messageObject.getMusicTitle();
            String author = messageObject.getMusicAuthor();
            if (TextUtils.isEmpty(title)) {
                title = messageObject.getDocumentName();
            }
            compactTitleView.setText(title != null ? title : "");
            compactAuthorView.setText(author != null ? author : "");
        }
        if (lyricsView != null && messageObject != null) {
            lyricsView.setSong(messageObject);
        }
    }

    private void updateActiveLyric(String text) {
        boolean enabled = MiogramVisualsPrefs.loadBool(getContext(), "player_active_lyric", true);
        boolean hasText = text != null && !text.trim().isEmpty();
        compactActiveLyricView.setText(hasText ? ("🎵 " + text.trim()) : "");
        compactActiveLyricView.setVisibility(enabled && hasText ? View.VISIBLE : View.GONE);
    }

    public void setSpeedText(String speed) {
        // Handled in audio player state
    }

    public void setFavorite(boolean fav) {
        // Handled in audio player state
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}
