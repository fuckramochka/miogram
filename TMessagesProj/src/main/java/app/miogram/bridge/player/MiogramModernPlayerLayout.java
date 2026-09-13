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
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.lyrics.MiogramLyricsView;
import app.miogram.bridge.ui.MiogramVisualsPrefs;
import app.miogram.bridge.ui.player.MiogramBassVisualizer;

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

    private final ImageView backgroundBlurView;

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
    private ImageView improveBtn;
    private ImageView topQueueBtn;

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

    private LinearLayout fullscreenCoverBox;
    private FrameLayout fullscreenCoverHolder;
    private TextView fullscreenTitleView;
    private TextView fullscreenAuthorView;
    private ImageView fullscreenFavoriteBtn;
    private MiogramBassVisualizer fullscreenBassVisualizer;
    private MiogramBassVisualizer compactBassVisualizer;
    private android.widget.VideoView customVideoView;
    private int photoLoadGen = 0;

    // Bottom Controls Section
    private final LinearLayout bottomSection;
    private final LinearLayout seekbarContainer;
    private View seekBarView;
    private final FrameLayout timersRow;
    private View timeView;
    private View durationView;

    private final LinearLayout mainControlsRow;
    private ImageView shuffleButton;
    private View repeatButton;
    private View prevButton;
    private FrameLayout heroPlayButton;
    private View playButton;
    private View nextButton;
    private ImageView queueButton;
    private TextView speedButton;

    private static boolean isExtraControlId(String id) {
        return "shuffle".equals(id) || "repeat".equals(id) || "queue".equals(id) || "speed".equals(id);
    }

    private static String controlIdForView(View v, View shuffle, View repeat, View queue, View speed) {
        if (v == null) return null;
        if (v == shuffle) return "shuffle";
        if (v == repeat) return "repeat";
        if (v == queue) return "queue";
        if (v == speed) return "speed";
        return null;
    }

    // Profile Button Container
    private final FrameLayout profileButtonContainer;
    private View saveToProfileButton;
    private View unsaveFromProfileButton;
    private boolean isFavoriteState = false;
    private ImageView floatCustomizeBtn;

    // Edit (jiggle) mode: tap element -> its own panel, drag extras to move.
    private boolean editMode = false;
    private final java.util.List<android.animation.Animator> jiggleAnims = new java.util.ArrayList<>();
    private View editHintBar;

    private MessageObject currentMessageObject;
    private boolean isPlaying = false;

    private final float[] ampBuffer = new float[16];
    private final MiogramPlayerPrefs.OnPrefsChangedListener prefsListener = this::applyCustomization;
    private final Runnable visualizerTicker = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(this);
            boolean vizOn = false;
            try {
                vizOn = MiogramPlayerPrefs.isVisualizerEnabled();
            } catch (Throwable ignore) {}
            boolean needViz = vizOn && isPlaying;
            try {
                if (needViz && MediaController.getInstance().isMessagePaused()) needViz = false;
            } catch (Throwable ignore) {
                needViz = false;
            }
            if (needViz) {
                long t = android.os.SystemClock.elapsedRealtime();
                for (int i = 0; i < ampBuffer.length; i++) {
                    float wave = 0.5f + 0.5f * (float) Math.sin(i * 0.9 + t * 0.005);
                    float detail = Math.abs((float) Math.sin(t * 0.007 + i * 1.7));
                    ampBuffer[i] = Math.min(1f, 0.12f + 0.30f * wave + 0.38f * detail);
                }
                if (compactBassVisualizer != null && compactBassVisualizer.getVisibility() == View.VISIBLE) {
                    compactBassVisualizer.updateAmplitudes(ampBuffer);
                }
                if (fullscreenBassVisualizer != null && fullscreenBassVisualizer.getVisibility() == View.VISIBLE) {
                    fullscreenBassVisualizer.updateAmplitudes(ampBuffer);
                }
                postDelayed(this, 64);
            } else {
                if (compactBassVisualizer != null) compactBassVisualizer.decayToIdle();
                if (fullscreenBassVisualizer != null) fullscreenBassVisualizer.decayToIdle();
            }
        }
    };

    public MiogramModernPlayerLayout(Context context, AudioPlayerAlert alert, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.alert = alert;
        this.resourcesProvider = resourcesProvider;

        // Background Blur Backdrop View
        backgroundBlurView = new ImageView(context);
        backgroundBlurView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(backgroundBlurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        captureBlurBackground();

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
        // Segmented control: frosted track + accent pill on the active segment.
        pageSwitcher = new LinearLayout(context);
        pageSwitcher.setOrientation(LinearLayout.HORIZONTAL);
        pageSwitcher.setGravity(Gravity.CENTER);
        GradientDrawable switcherTrack = new GradientDrawable();
        switcherTrack.setColor(0x1EFFFFFF);
        switcherTrack.setCornerRadius(AndroidUtilities.dp(18));
        pageSwitcher.setBackground(switcherTrack);
        pageSwitcher.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
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
        topControlsRow.addView(pageSwitcher, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(38), 1f));

        // Search music button
        ImageView searchBtn = new ImageView(context);
        searchBtn.setImageResource(R.drawable.search_music_filled);
        searchBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchBtn.setColorFilter(new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN));
        searchBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        searchBtn.setContentDescription(MiogramLocale.get("Пошук музики", "Поиск музыки", "Music Search"));
        searchBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (alert != null) {
                alert.openMusicSearch();
            }
        });
        topControlsRow.addView(searchBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 2, 0));

        // Improve: upgrades non-AI lyrics to precise AI transcription.
        // Visible only when current lyrics can be improved (replaces old pencil slot).
        improveBtn = new ImageView(context);
        improveBtn.setImageResource(R.drawable.baseline_stars_24);
        improveBtn.setScaleType(ImageView.ScaleType.CENTER);
        improveBtn.setColorFilter(new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN));
        improveBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        improveBtn.setContentDescription(MiogramLocale.get("Покращити текст", "Улучшить текст", "Enhance lyrics"));
        improveBtn.setVisibility(View.GONE);
        improveBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            try {
                if (lyricsView != null) lyricsView.upgradeLyricsToAi();
            } catch (Throwable ignore) {}
        });
        topControlsRow.addView(improveBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 2, 0));

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
        int surfaceColor = getThemedColor(Theme.key_player_background);
        if (surfaceColor == 0) surfaceColor = getThemedColor(Theme.key_windowBackgroundWhite);
        // Layered placeholder: accent-tinted fill + hairline accent stroke so the
        // empty state looks designed, not like a missing image.
        GradientDrawable placeholderBg = new GradientDrawable();
        placeholderBg.setColor(ColorUtils.blendARGB(surfaceColor, accentColor, 0.28f));
        placeholderBg.setCornerRadius(AndroidUtilities.dp(20));
        placeholderBg.setStroke(AndroidUtilities.dp(1), ColorUtils.setAlphaComponent(accentColor, 90));
        compactCoverWrapper.setBackground(placeholderBg);

        ImageView coverPlaceholder = new ImageView(context);
        coverPlaceholder.setImageResource(R.drawable.player_new_order);
        coverPlaceholder.setScaleType(ImageView.ScaleType.CENTER);
        coverPlaceholder.setColorFilter(new PorterDuffColorFilter(ColorUtils.setAlphaComponent(accentColor, 160), PorterDuff.Mode.SRC_IN));
        compactCoverWrapper.addView(coverPlaceholder, LayoutHelper.createFrame(48, 48, Gravity.CENTER));

        if (Build.VERSION.SDK_INT >= 21) {
            compactCoverWrapper.setElevation(AndroidUtilities.dp(8));
            compactCoverWrapper.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(20));
                }
            });
            compactCoverWrapper.setClipToOutline(true);
        }
        compactCoverWrapper.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            openFullscreenFromCover();
        });
        compactCoverWrapper.setContentDescription(MiogramLocale.get("Обкладинка, відкрити плеєр", "Обложка, открыть плеер", "Cover art, open player"));  
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

        fullscreenCoverBox = new LinearLayout(context);
        fullscreenCoverBox.setOrientation(LinearLayout.VERTICAL);
        fullscreenCoverBox.setGravity(Gravity.CENTER_HORIZONTAL);
        fullscreenCoverBox.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));

        fullscreenCoverHolder = new FrameLayout(context);
        if (Build.VERSION.SDK_INT >= 21) {
            fullscreenCoverHolder.setElevation(AndroidUtilities.dp(16));
            fullscreenCoverHolder.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(22));
                }
            });
            fullscreenCoverHolder.setClipToOutline(true);
        }
        fullscreenCoverBox.addView(fullscreenCoverHolder, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        // Fullscreen Track Title & Author & Favorite
        LinearLayout fullscreenTitleRow = new LinearLayout(context);
        fullscreenTitleRow.setOrientation(LinearLayout.HORIZONTAL);
        fullscreenTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        fullscreenTitleRow.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8), 0);

        LinearLayout fullscreenTextCol = new LinearLayout(context);
        fullscreenTextCol.setOrientation(LinearLayout.VERTICAL);

        fullscreenTitleView = new TextView(context);
        fullscreenTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        fullscreenTitleView.setTypeface(AndroidUtilities.bold());
        fullscreenTitleView.setTextColor(0xFFFFFFFF);
        fullscreenTitleView.setSingleLine(true);
        fullscreenTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        fullscreenTitleView.setSelected(true);
        fullscreenTitleView.setHorizontallyScrolling(true);
        fullscreenTextCol.addView(fullscreenTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        fullscreenAuthorView = new TextView(context);
        fullscreenAuthorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        fullscreenAuthorView.setTextColor(0xB3FFFFFF);
        fullscreenAuthorView.setSingleLine(true);
        fullscreenAuthorView.setEllipsize(TextUtils.TruncateAt.END);
        fullscreenTextCol.addView(fullscreenAuthorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        fullscreenTitleRow.addView(fullscreenTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        fullscreenFavoriteBtn = new ImageView(context);
        fullscreenFavoriteBtn.setImageResource(R.drawable.msg_fave);
        fullscreenFavoriteBtn.setScaleType(ImageView.ScaleType.CENTER);
        fullscreenFavoriteBtn.setColorFilter(new PorterDuffColorFilter(0x88FFFFFF, PorterDuff.Mode.SRC_IN));
        fullscreenFavoriteBtn.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        fullscreenFavoriteBtn.setContentDescription(MiogramLocale.get("В обране", "В избранное", "Favorite"));
        fullscreenFavoriteBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (alert != null) alert.toggleFavorite();
        });
        fullscreenTitleRow.addView(fullscreenFavoriteBtn, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        fullscreenCoverBox.addView(fullscreenTitleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        fullscreenBassVisualizer = new MiogramBassVisualizer(context);
        fullscreenBassVisualizer.setColor(accentColor);
        fullscreenBassVisualizer.setVisibility(MiogramPlayerPrefs.isVisualizerEnabled() ? View.VISIBLE : View.GONE);
        fullscreenCoverBox.addView(fullscreenBassVisualizer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0, 8, 0, 0));

        fullCoverWrapper.addView(fullscreenCoverBox, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        fullContentContainer.addView(fullCoverWrapper, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

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
        int bottomSolid = ColorUtils.setAlphaComponent(surface, 240);
        int bottomTop = ColorUtils.setAlphaComponent(surface, 210);
        bottomSection.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bottomTop, bottomSolid}));

        // Main Controls Row: left extras | CENTER TRIO (prev/play/next) | right extras
        // Trio is truly centered via equal-weight side boxes.
        mainControlsRow = new LinearLayout(context);
        mainControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        mainControlsRow.setGravity(Gravity.CENTER_VERTICAL);

        heroPlayButton = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ColorUtils.blendARGB(accentColor, 0xFFFFFFFF, 0.12f), ColorUtils.blendARGB(accentColor, 0xFF000000, 0.22f)});
        heroBg.setShape(GradientDrawable.OVAL);
        heroPlayButton.setBackground(heroBg);
        heroPlayButton.setContentDescription(MiogramLocale.get("Відтворити / Пауза", "Играть / Пауза", "Play / Pause"));
        if (Build.VERSION.SDK_INT >= 23) {
            heroPlayButton.setForeground(Theme.createSelectorDrawable(0x33FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        }
        // Tactile press squash: 0.92 scale while held, spring back on release.
        heroPlayButton.setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start();
            } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            }
            return false;
        });
        if (Build.VERSION.SDK_INT >= 21) {
            heroPlayButton.setElevation(AndroidUtilities.dp(6));
        }

        bottomSection.addView(mainControlsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 2, 0, 2));

        // Seekbar & Timestamps - AT BOTTOM below buttons
        seekbarContainer = new LinearLayout(context);
        seekbarContainer.setOrientation(LinearLayout.VERTICAL);
        timersRow = new FrameLayout(context);
        seekbarContainer.addView(timersRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        bottomSection.addView(seekbarContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 2));

        // Compact Bass Visualizer in bottom controls (hidden by default — "зайві полоски").
        compactBassVisualizer = new MiogramBassVisualizer(context);
        compactBassVisualizer.setColor(accentColor);
        compactBassVisualizer.setVisibility(MiogramPlayerPrefs.isVisualizerEnabled() ? View.VISIBLE : View.GONE);
        bottomSection.addView(compactBassVisualizer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 14, 0, 2, 0, 2));

        // Profile Button Container ("+ Додати в профіль") — compact centered pill, movable via prefs.
        profileButtonContainer = new FrameLayout(context);
        bottomSection.addView(profileButtonContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

        // Floating customize button (bottom-right, mini + fullscreen).
        // Opens jiggle edit mode — never the full sheet.
        floatCustomizeBtn = new ImageView(context);
        floatCustomizeBtn.setImageResource(R.drawable.msg_customize);
        floatCustomizeBtn.setScaleType(ImageView.ScaleType.CENTER);
        floatCustomizeBtn.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        GradientDrawable floatBg = new GradientDrawable();
        floatBg.setShape(GradientDrawable.OVAL);
        floatBg.setColor(accentColor);
        floatCustomizeBtn.setBackground(floatBg);
        if (Build.VERSION.SDK_INT >= 21) {
            floatCustomizeBtn.setElevation(AndroidUtilities.dp(6));
        }
        floatCustomizeBtn.setContentDescription(MiogramLocale.get("Налаштувати плеєр", "Настроить плеер", "Customize player"));
        floatCustomizeBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            setEditMode(true);
        });
        addView(bottomSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
        // Dock the floating button and adjust centerContainer dynamically based on bottomSection height
        bottomSection.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int h = v.getHeight();
            if (h > 0) {
                if (floatCustomizeBtn != null && floatCustomizeBtn.getLayoutParams() instanceof LayoutParams) {
                    LayoutParams lp = (LayoutParams) floatCustomizeBtn.getLayoutParams();
                    int want = h + AndroidUtilities.dp(10);
                    if (lp.bottomMargin != want) {
                        lp.bottomMargin = want;
                        floatCustomizeBtn.setLayoutParams(lp);
                    }
                }
                if (centerContainer != null && centerContainer.getLayoutParams() instanceof LayoutParams) {
                    LayoutParams clp = (LayoutParams) centerContainer.getLayoutParams();
                    int targetBottom = h + AndroidUtilities.dp(4);
                    if (clp.bottomMargin != targetBottom) {
                        clp.bottomMargin = targetBottom;
                        centerContainer.setLayoutParams(clp);
                    }
                }
            }
        });
        addView(floatCustomizeBtn, LayoutHelper.createFrame(46, 46, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, 14, 160));
        updateModeButtons();
        // Apply saved customization (visualizer OFF by default, centered trio, compact profile pill).
        try {
            MiogramPlayerPrefs.removeListener(prefsListener);
            MiogramPlayerPrefs.addListener(prefsListener);
        } catch (Throwable ignore) {}
        post(this::applyCustomization);
    }

    // Full player customization: background (cover/gradient/solid/glass/custom),
    // buttons (opacity/glow), lyrics (size/glow/opacity), visualizer + profile pill.
    // Called from pencil button (top bar, both mini + fullscreen) via MiogramPlayerCustomizeAlert.
    public void applyCustomization() {
        try {
            int accent = getThemeAccentColor();
            int mode = MiogramPlayerPrefs.getBackgroundMode();
            float opacity = MiogramPlayerPrefs.getBgOpacity();
            float brightness = MiogramPlayerPrefs.getBgBrightness();
            int alpha = (int) (255 * Math.max(0.1f, Math.min(1f, opacity)));

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            if (mode == MiogramPlayerPrefs.BG_MODE_GRADIENT) {
                stopCustomVideo();
                photoLoadGen++;
                int c1 = MiogramPlayerPrefs.getGradientColor1();
                int c2 = MiogramPlayerPrefs.getGradientColor2();
                int o = MiogramPlayerPrefs.getGradientOrientation();
                GradientDrawable.Orientation orient = GradientDrawable.Orientation.TOP_BOTTOM;
                if (o == 1) orient = GradientDrawable.Orientation.TL_BR;
                else if (o == 2) orient = GradientDrawable.Orientation.LEFT_RIGHT;
                bg = new GradientDrawable(orient, new int[]{
                        ColorUtils.setAlphaComponent(c1, alpha),
                        ColorUtils.setAlphaComponent(c2, alpha)});
            } else if (mode == MiogramPlayerPrefs.BG_MODE_SOLID) {
                stopCustomVideo();
                photoLoadGen++;
                bg.setColor(ColorUtils.setAlphaComponent(MiogramPlayerPrefs.getSolidColor(), alpha));
            } else if (mode == MiogramPlayerPrefs.BG_MODE_TRANSPARENT) {
                stopCustomVideo();
                photoLoadGen++;
                bg.setColor(ColorUtils.setAlphaComponent(0xFF000000, (int) (255 * 0.25f * opacity)));
            } else if (mode == MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO || mode == MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO) {
                String mediaPath = mode == MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO
                        ? MiogramPlayerPrefs.getCustomPhotoPath()
                        : MiogramPlayerPrefs.getCustomVideoPath();
                boolean valid = MiogramPlayerPrefs.isCustomMediaValid(mediaPath);
                int surface = getThemedColor(Theme.key_player_background);
                if (surface == 0) surface = getThemedColor(Theme.key_windowBackgroundWhite);
                if (surface == 0) surface = 0xFF13151D;
                int frosted = ColorUtils.setAlphaComponent(surface, Math.min(255, alpha));
                int top = ColorUtils.blendARGB(frosted, accent, 0.16f);
                int bottom = ColorUtils.blendARGB(frosted, 0xFF000000, 0.14f);
                bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, frosted, bottom});
                if (valid) {
                    loadCustomBackdropAsync(mediaPath, mode, brightness);
                } else {
                    // Missing/deleted file -> graceful fallback to frosted cover, keep pref for retry.
                    stopCustomVideo();
                    if (backgroundBlurView != null) {
                        backgroundBlurView.setAlpha(0.35f * brightness);
                    }
                }
            } else {
                // COVER_BLUR: frosted glass over cover art.
                stopCustomVideo();
                int surface = getThemedColor(Theme.key_player_background);
                if (surface == 0) surface = getThemedColor(Theme.key_windowBackgroundWhite);
                if (surface == 0) surface = 0xFF13151D;
                int frosted = ColorUtils.setAlphaComponent(surface, Math.min(255, alpha));
                int top = ColorUtils.blendARGB(frosted, accent, 0.16f);
                int bottom = ColorUtils.blendARGB(frosted, 0xFF000000, 0.14f);
                bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, frosted, bottom});
                stopCustomVideo();
            }
            float radius = AndroidUtilities.dp(24) * (1.0f - fullScreenProgress);
            bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            bg.setStroke(AndroidUtilities.dp(1), 0x28FFFFFF);
            setBackground(bg);

            // Blur strength (RenderEffect API 31+, alpha fallback below) + brightness.
            try {
                int blurPx = Math.max(0, Math.min(30, MiogramPlayerPrefs.getBgBlur()));
                if (android.os.Build.VERSION.SDK_INT >= 31 && backgroundBlurView != null) {
                    if (blurPx > 0) {
                        backgroundBlurView.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP));
                    } else {
                        backgroundBlurView.setRenderEffect(null);
                    }
                }
            } catch (Throwable ignore) {}
            if (backgroundBlurView != null) {
                if (mode == MiogramPlayerPrefs.BG_MODE_SOLID || mode == MiogramPlayerPrefs.BG_MODE_GRADIENT || mode == MiogramPlayerPrefs.BG_MODE_TRANSPARENT) {
                    backgroundBlurView.setVisibility(View.GONE);
                } else {
                    backgroundBlurView.setVisibility(View.VISIBLE);
                    if (mode == MiogramPlayerPrefs.BG_MODE_COVER_BLUR) {
                        backgroundBlurView.setAlpha(0.35f * brightness);
                    }
                }
            }

            // Buttons: opacity + neon glow on hero play.
            float btnOpacity = MiogramPlayerPrefs.getButtonOpacity();
            boolean glow = MiogramPlayerPrefs.isButtonGlowEnabled();
            if (shuffleButton != null) shuffleButton.setAlpha(btnOpacity);
            if (repeatButton != null) repeatButton.setAlpha(btnOpacity);
            if (prevButton != null) prevButton.setAlpha(btnOpacity);
            if (nextButton != null) nextButton.setAlpha(btnOpacity);
            if (queueButton != null) queueButton.setAlpha(btnOpacity);
            if (speedButton != null) speedButton.setAlpha(btnOpacity);
            if (heroPlayButton != null) {
                heroPlayButton.setAlpha(btnOpacity);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    heroPlayButton.setElevation(glow ? AndroidUtilities.dp(6) : 0);
                }
            }

            // Visualizer: user toggle (default OFF — removes "extra jumping stripes").
            boolean viz = MiogramPlayerPrefs.isVisualizerEnabled();
            if (compactBassVisualizer != null) {
                compactBassVisualizer.setVisibility(viz ? View.VISIBLE : View.GONE);
                if (!viz) compactBassVisualizer.decayToIdle();
            }
            if (fullscreenBassVisualizer != null) {
                fullscreenBassVisualizer.setVisibility(viz && playerMode == PlayerMode.COVER ? View.VISIBLE : View.GONE);
                if (!viz) fullscreenBassVisualizer.decayToIdle();
            }

            // Profile pill visibility.
            if (profileButtonContainer != null) {
                profileButtonContainer.setVisibility(MiogramPlayerPrefs.isProfileButtonEnabled() ? View.VISIBLE : View.GONE);
            }

            // Lyrics styling.
            if (lyricsView != null) lyricsView.reloadCustomization();
        } catch (Throwable ignore) {}
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
            view.setOnSourceChangedListener(this::refreshImproveButton);
            fullContentContainer.addView(view, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            view.setVisibility(playerMode == PlayerMode.LYRICS ? View.VISIBLE : View.GONE);
            view.setAlpha(playerMode == PlayerMode.LYRICS ? 1f : 0f);
            refreshImproveButton();
        }
    }

    /** Shows the Improve button only when current lyrics can be upgraded to AI. */
    public void refreshImproveButton() {
        try {
            boolean show = lyricsView != null && lyricsView.canImprove();
            if (improveBtn != null) {
                improveBtn.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Throwable ignore) {}
    }

    public void setCoverView(View cover) {
        this.coverView = cover;
        updateCoverAttachment();
    }

    private void openFullscreenFromCover() {
        if (alert != null) alert.setFullScreen(true, true);
    }

    private void updateCoverAttachment() {
        if (coverView == null) return;
        if (coverView.getParent() instanceof ViewGroup) {
            ((ViewGroup) coverView.getParent()).removeView(coverView);
        }
        if (!isFullScreen || fullScreenProgress < 0.5f) {
            if (compactCoverWrapper.getChildCount() > 1) {
                compactCoverWrapper.removeViews(1, compactCoverWrapper.getChildCount() - 1);
            }
            int size = AndroidUtilities.dp(115);
            compactCoverWrapper.addView(coverView, LayoutHelper.createFrame(size, size, Gravity.CENTER));
            coverView.setVisibility(View.VISIBLE);
        } else {
            // Fullscreen: keep the cover attached (and updating) even when the
            // Lyrics/Queue page is on top — otherwise art updates are lost and
            // the detached view leaks its bitmap. Hidden when not the COVER page.
            if (fullscreenCoverHolder != null) {
                fullscreenCoverHolder.removeAllViews();
                int maxByWidth = AndroidUtilities.displaySize.x - AndroidUtilities.dp(72);
                int maxByHeight = (int) (AndroidUtilities.displaySize.y * 0.35f);
                int size = Math.max(AndroidUtilities.dp(180), Math.min(AndroidUtilities.dp(280), Math.min(maxByWidth, maxByHeight)));
                fullscreenCoverHolder.addView(coverView, LayoutHelper.createFrame(size, size, Gravity.CENTER));
                coverView.setVisibility(playerMode == PlayerMode.COVER ? View.VISIBLE : View.GONE);
            }
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
            int buttonColor = getThemedColor(Theme.key_player_button);
            if (buttonColor == 0) buttonColor = 0xFF888888;
            int accentColor = getThemeAccentColor();
            float btnOpacity = MiogramPlayerPrefs.getButtonOpacity();

            // Build extra buttons first (shuffle/repeat/queue/speed) — placement
            // follows controls_order pref: extras before 'play' go left, after go right.
            // Center trio (prev/play/next) is always locked in the middle.
            java.util.List<String> order = MiogramPlayerPrefs.getControlsOrderList();
            int playIdx = order.indexOf("play");
            if (playIdx < 0) playIdx = 3;
            java.util.List<String> leftIds = new java.util.ArrayList<>();
            java.util.List<String> rightIds = new java.util.ArrayList<>();
            for (int i = 0; i < order.size(); i++) {
                String id = order.get(i);
                if (!isExtraControlId(id)) continue;
                if (MiogramPlayerPrefs.isControlHidden(id)) continue;
                if (i < playIdx) leftIds.add(id);
                else rightIds.add(id);
            }

            LinearLayout leftBox = new LinearLayout(getContext());
            leftBox.setOrientation(LinearLayout.HORIZONTAL);
            leftBox.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

            shuffleButton = new ImageView(getContext());
            shuffleButton.setImageResource(R.drawable.player_new_shuffle);
            shuffleButton.setScaleType(ImageView.ScaleType.CENTER);
            shuffleButton.setColorFilter(new PorterDuffColorFilter(SharedConfig.shuffleMusic ? accentColor : buttonColor, PorterDuff.Mode.SRC_IN));
            shuffleButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
            shuffleButton.setContentDescription(MiogramLocale.get("Випадковий порядок", "Случайный порядок", "Shuffle"));
            shuffleButton.setAlpha(btnOpacity);
            shuffleButton.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                MediaController.getInstance().setPlaybackOrderType(SharedConfig.shuffleMusic ? 0 : 2);
                updateShuffleButton();
            });

            if (repeat != null) {
                if (repeat.getParent() instanceof ViewGroup) ((ViewGroup) repeat.getParent()).removeView(repeat);
                repeat.setContentDescription(MiogramLocale.get("Повтор", "Повтор", "Repeat"));
                repeat.setAlpha(btnOpacity);
                repeat.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (alert != null) {
                        int mode = SharedConfig.repeatMode;
                        if (mode == 0) SharedConfig.setRepeatMode(1);
                        else if (mode == 1) SharedConfig.setRepeatMode(2);
                        else SharedConfig.setRepeatMode(0);
                        alert.updateRepeatButton();
                    }
                });
                repeat.setOnLongClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (v instanceof org.telegram.ui.ActionBar.ActionBarMenuItem) {
                        ((org.telegram.ui.ActionBar.ActionBarMenuItem) v).toggleSubMenu();
                        return true;
                    }
                    return false;
                });
            }
            // Speed lives in the row (empty spot near pause), not floating.
            speedButton = new TextView(getContext());
            speedButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            speedButton.setTypeface(AndroidUtilities.bold());
            speedButton.setGravity(Gravity.CENTER);
            speedButton.setSingleLine(true);
            try {
                speedButton.setText(String.format(java.util.Locale.US, "%.1fx", org.telegram.messenger.MediaController.getInstance().getPlaybackSpeed(true)));
            } catch (Throwable ignore) {
                speedButton.setText("1.0x");
            }
            speedButton.setTextColor(buttonColor);
            speedButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
            speedButton.setContentDescription(MiogramLocale.get("Швидкість відтворення", "Скорость воспроизведения", "Playback speed"));
            speedButton.setAlpha(btnOpacity);
            speedButton.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (alert != null) alert.cyclePlaybackSpeed();
            });

            // CENTER TRIO (wrap, truly centered): prev + hero play + next.
            LinearLayout centerBox = new LinearLayout(getContext());
            centerBox.setOrientation(LinearLayout.HORIZONTAL);
            centerBox.setGravity(Gravity.CENTER);

            if (prev != null) {
                if (prev.getParent() instanceof ViewGroup) ((ViewGroup) prev.getParent()).removeView(prev);
                prev.setContentDescription(MiogramLocale.get("Попередній трек", "Предыдущий трек", "Previous track"));
                prev.setAlpha(btnOpacity);
                prev.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playPreviousMessage();
                });
                centerBox.addView(prev, LayoutHelper.createLinear(46, 46, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
            }
            if (play != null) {
                if (play.getParent() instanceof ViewGroup) ((ViewGroup) play.getParent()).removeView(play);
                play.setContentDescription(MiogramLocale.get("Відтворити / Пауза", "Играть / Пауза", "Play / Pause"));
                play.setBackground(null);
                if (play instanceof ImageView) {
                    ((ImageView) play).setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
                }
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
            heroPlayButton.setAlpha(btnOpacity);
            centerBox.addView(heroPlayButton, LayoutHelper.createLinear(56, 56, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

            if (next != null) {
                if (next.getParent() instanceof ViewGroup) ((ViewGroup) next.getParent()).removeView(next);
                next.setContentDescription(MiogramLocale.get("Наступний трек", "Следующий трек", "Next track"));
                next.setAlpha(btnOpacity);
                next.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playNextMessage();
                });
                centerBox.addView(next, LayoutHelper.createLinear(46, 46, Gravity.CENTER_VERTICAL));
            }

            // RIGHT extras box (weight 1, right-aligned).
            LinearLayout rightBox = new LinearLayout(getContext());
            rightBox.setOrientation(LinearLayout.HORIZONTAL);
            rightBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            queueButton = new ImageView(getContext());
            queueButton.setImageResource(R.drawable.player_new_order);
            queueButton.setScaleType(ImageView.ScaleType.CENTER);
            queueButton.setColorFilter(new PorterDuffColorFilter(playerMode == PlayerMode.QUEUE ? accentColor : buttonColor, PorterDuff.Mode.SRC_IN));
            queueButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
            queueButton.setContentDescription(MiogramLocale.get("Черга", "Очередь", "Queue"));
            queueButton.setAlpha(btnOpacity);
            queueButton.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (!isFullScreen) {
                    showQueue(true, false);
                    if (alert != null) alert.setFullScreen(true, true);
                } else {
                    toggleQueue();
                }
            });
            // Generic placement: every visible extra goes left/right by order position.
            java.util.Map<String, View> extraViews = new java.util.HashMap<>();
            extraViews.put("shuffle", shuffleButton);
            if (repeat != null) extraViews.put("repeat", repeat);
            extraViews.put("queue", queueButton);
            if (speedButton != null) extraViews.put("speed", speedButton);
            for (String id : leftIds) {
                View ev = extraViews.get(id);
                if (ev != null && ev.getParent() == null) {
                    leftBox.addView(ev, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
                }
            }
            for (String id : rightIds) {
                View ev = extraViews.get(id);
                if (ev != null && ev.getParent() == null) {
                    rightBox.addView(ev, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));
                }
            }
            // Hidden extras stay detached (no force-add) — true hide.

            mainControlsRow.addView(leftBox, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
            mainControlsRow.addView(centerBox, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            mainControlsRow.addView(rightBox, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
            applyCustomization();
            if (editMode) {
                stopJiggle();
                attachEditTouchIfNeeded();
                startJiggle();
            }
        }
    }

    public void updateShuffleButton() {
        if (shuffleButton != null) {
            int accent = getThemeAccentColor();
            int defColor = getThemedColor(Theme.key_player_button);
            if (defColor == 0) defColor = 0xFF888888;
            shuffleButton.setColorFilter(new PorterDuffColorFilter(SharedConfig.shuffleMusic ? accent : defColor, PorterDuff.Mode.SRC_IN));
        }
    }

    public void setProfileButtons(View saveBtn, View unsaveBtn) {
        this.saveToProfileButton = saveBtn;
        this.unsaveFromProfileButton = unsaveBtn;

        if (profileButtonContainer != null) {
            profileButtonContainer.removeAllViews();
            // Compact pill — position (left/center/right) follows prefs, set by drag.
            int grav;
            int align = MiogramPlayerPrefs.getProfileAlign();
            if (align == MiogramPlayerPrefs.PROFILE_ALIGN_LEFT) grav = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            else if (align == MiogramPlayerPrefs.PROFILE_ALIGN_RIGHT) grav = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            else grav = Gravity.CENTER;
            if (saveBtn != null) {
                if (saveBtn.getParent() instanceof ViewGroup) ((ViewGroup) saveBtn.getParent()).removeView(saveBtn);
                profileButtonContainer.addView(saveBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, grav));
            }
            if (unsaveBtn != null) {
                if (unsaveBtn.getParent() instanceof ViewGroup) ((ViewGroup) unsaveBtn.getParent()).removeView(unsaveBtn);
                profileButtonContainer.addView(unsaveBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 36, grav));
            }
            applyFavoriteState();
            applyCustomization();
            attachEditTouchIfNeeded();
        }
    }

    /** Re-applies profile pill gravity without rebuilding buttons (drag snapping). */
    public void applyProfileAlign() {
        try {
            setProfileButtons(saveToProfileButton, unsaveFromProfileButton);
        } catch (Throwable ignore) {}
    }

    public void setFullScreen(boolean fullScreen, boolean animated) {
        this.isFullScreen = fullScreen;
        this.fullScreenProgress = fullScreen ? 1.0f : 0.0f;
        applyFullScreenVisualState(animated);
    }

    public void setFullScreenProgress(float progress) {
        this.fullScreenProgress = Math.max(0f, Math.min(1f, progress));
        this.isFullScreen = this.fullScreenProgress >= 0.5f;
        if (MiogramPlayerPrefs.getBackgroundMode() == MiogramPlayerPrefs.BG_MODE_COVER_BLUR) {
            updateBackgroundShape(this.fullScreenProgress);
        } else {
            applyCustomization();
        }

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
        this.fullScreenProgress = progress;
        applyCustomization();

        float radius = AndroidUtilities.dp(24) * (1.0f - progress);
        if (backgroundBlurView != null && Build.VERSION.SDK_INT >= 21) {
            backgroundBlurView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            backgroundBlurView.setClipToOutline(radius > 0);
        }
    }

    public void captureBlurBackground() {
        try {
            org.telegram.ui.Components.ScrimOptions.makeGlobalBlurBitmaps((bitmapBg, bitmapOptions) -> {
                if (bitmapBg != null && backgroundBlurView != null) {
                    backgroundBlurView.setImageBitmap(bitmapBg);
                    backgroundBlurView.setAlpha(0.88f);
                }
            });
        } catch (Throwable t) {
            org.telegram.messenger.FileLog.e(t);
        }
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

    /** Generation guard: only the latest transition may hide pages on animation end. */
    private int pageTransitionGen = 0;

    private void setPlayerMode(PlayerMode mode, boolean animated) {
        if (playerMode == mode) return;
        PlayerMode previousMode = playerMode;
        playerMode = mode;

        View target = mode == PlayerMode.QUEUE ? queueContainer : mode == PlayerMode.COVER ? fullCoverWrapper : lyricsView;
        View previous = previousMode == PlayerMode.QUEUE ? queueContainer : previousMode == PlayerMode.COVER ? fullCoverWrapper : lyricsView;

        if (queueButton != null) {
            int color = mode == PlayerMode.QUEUE ? getThemeAccentColor() : getThemedColor(Theme.key_player_button);
            queueButton.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        updateModeButtons();
        // Refresh fullscreen visualizer (only in COVER mode when enabled).
        if (fullscreenBassVisualizer != null) {
            boolean viz = MiogramPlayerPrefs.isVisualizerEnabled();
            fullscreenBassVisualizer.setVisibility(viz && mode == PlayerMode.COVER ? View.VISIBLE : View.GONE);
        }

        // Target view not attached yet (e.g. lyrics arrive later via setLyricsView,
        // which applies visibility from playerMode) — mode is already stored.
        if (target == null) {
            if (previous != null) setPageVisible(previous, false);
            return;
        }

        if (mode == PlayerMode.COVER) {
            updateCoverAttachment();
        }

        cancelPageAnimation(target);
        if (previous != null) cancelPageAnimation(previous);
        // Hide the third page immediately so rapid A->B->C taps can't stack
        // two visible pages when an end-listener fires late.
        hideInactivePages(target, previous);
        target.setVisibility(View.VISIBLE);

        if (!animated || previous == null || previous == target) {
            setPageVisible(target, true);
            if (previous != null && previous != target) setPageVisible(previous, false);
            return;
        }

        final int gen = ++pageTransitionGen;
        target.setAlpha(0f);
        target.setScaleX(0.985f);
        target.setScaleY(0.985f);
        target.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start();
        final PlayerMode expectedMode = mode;
        previous.animate().alpha(0f).scaleX(0.985f).scaleY(0.985f).setDuration(180).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (gen == pageTransitionGen && playerMode == expectedMode) setPageVisible(previous, false);
            }
        }).start();
    }

    private void hideInactivePages(View... keep) {
        View[] pages = new View[]{queueContainer, fullCoverWrapper, lyricsView};
        outer:
        for (View p : pages) {
            if (p == null) continue;
            for (View k : keep) {
                if (p == k) continue outer;
            }
            cancelPageAnimation(p);
            setPageVisible(p, false);
        }
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
        button.setTextColor(selected ? 0xFFFFFFFF : 0x99FFFFFF);
        // Crisp pill swap (no scale — scaling text blurs it on low-dpi screens).
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), selected ? ColorUtils.setAlphaComponent(accent, 200) : 0x00000000));
        if (button.getScaleX() != 1f || button.getScaleY() != 1f) {
            button.setScaleX(1f);
            button.setScaleY(1f);
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (compactCoverWrapper != null) {
            compactCoverWrapper.animate().scaleX(playing ? 1.0f : 0.94f).scaleY(playing ? 1.0f : 0.94f).setDuration(220).start();
        }
        if (fullscreenCoverHolder != null) {
            fullscreenCoverHolder.animate().scaleX(playing ? 1.0f : 0.94f).scaleY(playing ? 1.0f : 0.94f).setDuration(220).start();
        }
        removeCallbacks(visualizerTicker);
        if (playing) {
            post(visualizerTicker);
        } else {
            if (compactBassVisualizer != null) compactBassVisualizer.decayToIdle();
            if (fullscreenBassVisualizer != null) fullscreenBassVisualizer.decayToIdle();
        }
        if (playButton instanceof ImageView) {
            ((ImageView) playButton).setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        }
        updateShuffleButton();
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
            if (fullscreenTitleView != null) {
                fullscreenTitleView.setText(title != null ? title : "");
            }
            if (fullscreenAuthorView != null) {
                fullscreenAuthorView.setText(author != null ? author : "");
            }
        }
        if (lyricsView != null && messageObject != null) {
            lyricsView.setSong(messageObject);
        }
        refreshImproveButton();
    }

    private void updateActiveLyric(String text) {
        boolean enabled = MiogramVisualsPrefs.loadBool(getContext(), "player_active_lyric", true);
        boolean hasText = text != null && !text.trim().isEmpty();
        compactActiveLyricView.setText(hasText ? text.trim() : "");
        compactActiveLyricView.setVisibility(enabled && hasText ? View.VISIBLE : View.GONE);
    }

    public void setSpeedText(String speed) {
        if (speedButton != null && speed != null) {
            speedButton.setText(speed);
        }
    }

    public void setFavorite(boolean fav) {
        isFavoriteState = fav;
        applyFavoriteState();
    }

    private void applyFavoriteState() {
        if (saveToProfileButton != null) {
            saveToProfileButton.setVisibility(isFavoriteState ? View.GONE : View.VISIBLE);
        }
        if (unsaveFromProfileButton != null) {
            unsaveFromProfileButton.setVisibility(isFavoriteState ? View.VISIBLE : View.GONE);
        }
        if (fullscreenFavoriteBtn != null) {
            fullscreenFavoriteBtn.setColorFilter(new PorterDuffColorFilter(isFavoriteState ? 0xFFFF4081 : 0x88FFFFFF, PorterDuff.Mode.SRC_IN));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(visualizerTicker);
        try {
            MiogramPlayerPrefs.removeListener(prefsListener);
        } catch (Throwable ignore) {}
        try {
            stopCustomVideo();
        } catch (Throwable ignore) {}
    }

    private void loadCustomBackdropAsync(String path, int mode, float brightness) {
        final int gen = ++photoLoadGen;
        final float bright = Math.max(0.2f, Math.min(1.6f, brightness));
        if (mode == MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO) {
            try {
                org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                    android.graphics.Bitmap frame = null;
                    try {
                        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
                        mmr.setDataSource(path);
                        frame = mmr.getFrameAtTime(500000);
                        try {
                            mmr.release();
                        } catch (Throwable ignore) {}
                    } catch (Throwable ignore) {}
                    final android.graphics.Bitmap f = frame;
                    post(() -> {
                        if (gen != photoLoadGen) {
                            if (f != null && !f.isRecycled()) f.recycle();
                            return;
                        }
                        try {
                            ensureCustomVideoView();
                            if (f != null) {
                                backgroundBlurView.setImageBitmap(f);
                                backgroundBlurView.setAlpha(bright);
                            }
                            if (customVideoView != null && MiogramPlayerPrefs.isCustomMediaValid(path)) {
                                try {
                                    customVideoView.setVideoPath(path);
                                    customVideoView.setOnPreparedListener(mp -> {
                                        try {
                                            mp.setLooping(true);
                                            mp.setVolume(0f, 0f);
                                            customVideoView.setAlpha(Math.min(1f, bright));
                                            customVideoView.start();
                                        } catch (Throwable ignore) {}
                                    });
                                    customVideoView.setOnErrorListener((mp, what, extra) -> {
                                        stopCustomVideo();
                                        return true;
                                    });
                                    customVideoView.setVisibility(View.VISIBLE);
                                    if (backgroundBlurView != null) backgroundBlurView.setAlpha(bright * 0.55f);
                                } catch (Throwable ignore) {
                                    stopCustomVideo();
                                }
                            }
                        } catch (Throwable ignore) {}
                    });
                });
            } catch (Throwable ignore) {}
            return;
        }
        stopCustomVideo();
        try {
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                android.graphics.Bitmap bmp = null;
                try {
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFile(path, opts);
                    int maxDim = Math.max(opts.outWidth, opts.outHeight);
                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = 1;
                    while (maxDim / opts.inSampleSize > 1280 && opts.inSampleSize < 8) opts.inSampleSize *= 2;
                    bmp = android.graphics.BitmapFactory.decodeFile(path, opts);
                } catch (Throwable ignore) {}
                final android.graphics.Bitmap result = bmp;
                post(() -> {
                    if (gen != photoLoadGen) {
                        if (result != null && !result.isRecycled()) result.recycle();
                        return;
                    }
                    try {
                        if (result != null && backgroundBlurView != null) {
                            backgroundBlurView.setImageBitmap(result);
                            backgroundBlurView.setAlpha(bright);
                        } else if (backgroundBlurView != null) {
                            backgroundBlurView.setAlpha(0.35f * bright);
                        }
                    } catch (Throwable ignore) {}
                });
            });
        } catch (Throwable ignore) {}
    }

    private void ensureCustomVideoView() {
        try {
            if (customVideoView == null) {
                customVideoView = new android.widget.VideoView(getContext());
                addView(customVideoView, 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            } else if (customVideoView.getParent() == null) {
                addView(customVideoView, 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
        } catch (Throwable ignore) {}
    }

    private void stopCustomVideo() {
        try {
            if (customVideoView != null) {
                try {
                    customVideoView.stopPlayback();
                } catch (Throwable ignore) {}
                customVideoView.setVisibility(View.GONE);
            }
        } catch (Throwable ignore) {}
    }

    public void rebuildControlsForPrefs() {
        try {
            // Rebuild bottom row in order/hide prefs without losing native button views.
            // Native views are reparented, so collect them first.
            java.util.Map<String, View> map = new java.util.HashMap<>();
            if (shuffleButton != null) map.put("shuffle", shuffleButton);
            if (repeatButton != null) map.put("repeat", repeatButton);
            if (prevButton != null) map.put("prev", prevButton);
            if (playButton != null) map.put("play", playButton);
            if (nextButton != null) map.put("next", nextButton);
            if (queueButton != null) map.put("queue", queueButton);
            if (!map.containsKey("prev") || !map.containsKey("play") || !map.containsKey("next")) return;
            setControlButtons(map.get("repeat"), map.get("prev"), map.get("play"), map.get("next"));
            // shuffle/queue are recreated inside setControlButtons; re-apply hide below via applyCustomization.
            applyCustomization();
        } catch (Throwable ignore) {}
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    // =====================================================================
    // EDIT (JIGGLE) MODE — nothing opens by itself; every element shakes as
    // if clickable, tap opens ONLY that element's panel, extras can be
    // dragged to swap, profile pill dragged to snap left/center/right.
    // =====================================================================

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean enabled) {
        if (editMode == enabled) return;
        editMode = enabled;
        if (enabled) {
            if (floatCustomizeBtn != null) floatCustomizeBtn.setVisibility(View.GONE);
            attachEditTouchIfNeeded();
            startJiggle();
            showEditHint();
        } else {
            if (floatCustomizeBtn != null) floatCustomizeBtn.setVisibility(View.VISIBLE);
            stopJiggle();
            detachEditTouch();
            if (editHintBar != null) {
                try {
                    removeView(editHintBar);
                } catch (Throwable ignore) {}
                editHintBar = null;
            }
        }
    }

    private void showEditHint() {
        try {
            Context context = getContext();
            LinearLayout bar = new LinearLayout(context);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            int accent = getThemeAccentColor();
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xE614151E);
            bg.setCornerRadius(AndroidUtilities.dp(20));
            bg.setStroke(AndroidUtilities.dp(1), ColorUtils.setAlphaComponent(accent, 140));
            bar.setBackground(bg);
            bar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

            TextView label = new TextView(context);
            label.setText(MiogramLocale.get("Торкни елемент для налаштування", "Тронь элемент для настройки", "Tap an element to tune it"));
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            label.setTypeface(AndroidUtilities.bold());
            label.setTextColor(0xFFFFFFFF);
            bar.addView(label, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            TextView done = new TextView(context);
            done.setText(MiogramLocale.get("Готово", "Готово", "Done"));
            done.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            done.setTypeface(AndroidUtilities.bold());
            done.setTextColor(0xFFFFFFFF);
            done.setGravity(Gravity.CENTER);
            GradientDrawable doneBg = new GradientDrawable();
            doneBg.setColor(accent);
            doneBg.setCornerRadius(AndroidUtilities.dp(14));
            done.setBackground(doneBg);
            done.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(6));
            done.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                setEditMode(false);
            });
            bar.addView(done, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

            editHintBar = bar;
            addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 12, 12, 12, 0));
        } catch (Throwable ignore) {}
    }

    private java.util.List<View> editJiggleTargets() {
        java.util.List<View> out = new java.util.ArrayList<>();
        if (shuffleButton != null && shuffleButton.getParent() != null) out.add(shuffleButton);
        if (repeatButton != null && repeatButton.getParent() != null) out.add(repeatButton);
        if (prevButton != null && prevButton.getParent() != null) out.add(prevButton);
        if (heroPlayButton != null && heroPlayButton.getParent() != null) out.add(heroPlayButton);
        if (nextButton != null && nextButton.getParent() != null) out.add(nextButton);
        if (queueButton != null && queueButton.getParent() != null) out.add(queueButton);
        if (speedButton != null && speedButton.getParent() != null) out.add(speedButton);
        View profile = currentProfileButton();
        if (profile != null && profile.getParent() != null) out.add(profile);
        if (compactBassVisualizer != null && compactBassVisualizer.getVisibility() == View.VISIBLE) out.add(compactBassVisualizer);
        if (fullscreenBassVisualizer != null && fullscreenBassVisualizer.getVisibility() == View.VISIBLE) out.add(fullscreenBassVisualizer);
        return out;
    }

    private View currentProfileButton() {
        try {
            if (saveToProfileButton != null && saveToProfileButton.getVisibility() == View.VISIBLE) return saveToProfileButton;
            if (unsaveFromProfileButton != null && unsaveFromProfileButton.getVisibility() == View.VISIBLE) return unsaveFromProfileButton;
        } catch (Throwable ignore) {}
        return null;
    }

    private void startJiggle() {
        stopJiggle();
        try {
            java.util.List<View> targets = editJiggleTargets();
            for (int i = 0; i < targets.size(); i++) {
                View v = targets.get(i);
                android.animation.ObjectAnimator rot = android.animation.ObjectAnimator.ofFloat(v, "rotation", -3.4f, 3.4f);
                rot.setDuration(140);
                rot.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                rot.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                android.animation.ObjectAnimator sx = android.animation.ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.06f);
                sx.setDuration(300);
                sx.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                sx.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                android.animation.ObjectAnimator sy = android.animation.ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.06f);
                sy.setDuration(300);
                sy.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                sy.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                android.animation.AnimatorSet set = new android.animation.AnimatorSet();
                set.playTogether(rot, sx, sy);
                set.setStartDelay((i % 4) * 40L);
                set.start();
                jiggleAnims.add(set);
            }
        } catch (Throwable ignore) {}
    }

    private void stopJiggle() {
        try {
            for (android.animation.Animator a : jiggleAnims) {
                try {
                    a.cancel();
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        jiggleAnims.clear();
        try {
            java.util.List<View> targets = editJiggleTargets();
            for (View v : targets) {
                v.setRotation(0f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } catch (Throwable ignore) {}
    }

    /**
     * Direct tap routing, no coordinate overlays: row buttons + profile pill
     * get touch interceptors (clicks underneath stay intact), cover holders
     * and visualizers get plain click listeners, lyrics gets a tap interceptor.
     */
    private void attachEditTouchIfNeeded() {
        if (!editMode) return;
        try {
            attachExtraEditTouch(shuffleButton, "shuffle", "controls", true);
            attachExtraEditTouch(repeatButton, "repeat", "controls", true);
            attachExtraEditTouch(queueButton, "queue", "controls", true);
            attachExtraEditTouch(speedButton, "speed", "controls", true);
            attachExtraEditTouch(prevButton, "prev", "controls", false);
            attachExtraEditTouch(playButton, "play", "controls", false);
            attachExtraEditTouch(nextButton, "next", "controls", false);
            attachExtraEditTouch(saveToProfileButton, "profile", "profile", true);
            attachExtraEditTouch(unsaveFromProfileButton, "profile", "profile", true);
            attachZoneTap(backgroundBlurView, "background");
            attachZoneTap(compactCoverWrapper, "background");
            attachZoneTap(fullscreenCoverHolder, "background");
            attachZoneTap(compactBassVisualizer, "visualizer");
            attachZoneTap(fullscreenBassVisualizer, "visualizer");
            if (lyricsView != null) {
                lyricsView.setEditTapListener(() -> {
                    MiogramHaptic.select(lyricsView);
                    openSectionSheet("lyrics");
                });
            }
        } catch (Throwable ignore) {}
    }

    private void attachZoneTap(View zone, final String section) {
        if (zone == null) return;
        try {
            zone.setOnClickListener(v -> {
                if (!editMode) return;
                MiogramHaptic.select(v);
                openSectionSheet(section);
            });
        } catch (Throwable ignore) {}
    }

    private void detachEditTouch() {
        // Only edit-mode interceptors are removed — everything underneath stays intact.
        try {
            View[] views = new View[]{shuffleButton, repeatButton, queueButton, speedButton,
                    prevButton, playButton, nextButton, saveToProfileButton, unsaveFromProfileButton};
            for (View v : views) {
                if (v != null) {
                    try {
                        v.setOnTouchListener(null);
                    } catch (Throwable ignore) {}
                }
            }
            if (backgroundBlurView != null) {
                try {
                    backgroundBlurView.setOnClickListener(null);
                } catch (Throwable ignore) {}
            }
            if (compactCoverWrapper != null) {
                compactCoverWrapper.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    openFullscreenFromCover();
                });
            }
            if (fullscreenCoverHolder != null) {
                try {
                    fullscreenCoverHolder.setOnClickListener(null);
                } catch (Throwable ignore) {}
            }
            if (compactBassVisualizer != null) {
                try {
                    compactBassVisualizer.setOnClickListener(null);
                } catch (Throwable ignore) {}
            }
            if (fullscreenBassVisualizer != null) {
                try {
                    fullscreenBassVisualizer.setOnClickListener(null);
                } catch (Throwable ignore) {}
            }
            if (lyricsView != null) {
                try {
                    lyricsView.setEditTapListener(null);
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
    }

    private void attachExtraEditTouch(View v, final String id, final String section, final boolean draggable) {
        if (v == null || v.getParent() == null) return;
        v.setOnTouchListener(new OnTouchListener() {
            float downX;
            float downY;
            boolean moved;

            @Override
            public boolean onTouch(View view, android.view.MotionEvent e) {
                if (!editMode) return false;
                int action = e.getActionMasked();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX();
                    downY = e.getRawY();
                    moved = false;
                    return true;
                } else if (action == android.view.MotionEvent.ACTION_MOVE) {
                    float dx = e.getRawX() - downX;
                    float dy = e.getRawY() - downY;
                    if (dx * dx + dy * dy > (float) AndroidUtilities.dp(8) * AndroidUtilities.dp(8)) {
                        moved = true;
                    }
                    return true;
                } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                    if (!moved && action == android.view.MotionEvent.ACTION_UP) {
                        MiogramHaptic.select(view);
                        openSectionSheet(section);
                    } else if (moved && draggable && action == android.view.MotionEvent.ACTION_UP) {
                        handleEditDrop(id, e.getRawX(), e.getRawY());
                    }
                    return true;
                }
                return true;
            }
        });
    }

    /** Drop on another extra swaps order; profile pill snaps left/center/right. */
    private void handleEditDrop(String dragId, float rawX, float rawY) {
        try {
            if ("profile".equals(dragId)) {
                int[] rootPos = new int[2];
                getLocationOnScreen(rootPos);
                float relX = rawX - rootPos[0];
                float w = Math.max(1, getWidth());
                int align;
                if (relX < w / 3f) align = MiogramPlayerPrefs.PROFILE_ALIGN_LEFT;
                else if (relX > w * 2f / 3f) align = MiogramPlayerPrefs.PROFILE_ALIGN_RIGHT;
                else align = MiogramPlayerPrefs.PROFILE_ALIGN_CENTER;
                MiogramHaptic.select(this);
                MiogramPlayerPrefs.setProfileAlign(align);
                applyProfileAlign();
                stopJiggle();
                attachEditTouchIfNeeded();
                startJiggle();
                return;
            }
            View target = findExtraAt(rawX, rawY, dragId);
            if (target != null) {
                String targetId = controlIdForView(target, shuffleButton, repeatButton, queueButton, speedButton);
                if (targetId != null) {
                    MiogramHaptic.success(this);
                    MiogramPlayerPrefs.swapControls(dragId, targetId);
                    rebuildControlsForPrefs();
                    stopJiggle();
                    attachEditTouchIfNeeded();
                    startJiggle();
                }
            }
        } catch (Throwable ignore) {}
    }

    private View findExtraAt(float rawX, float rawY, String excludeId) {
        View[] candidates = new View[]{shuffleButton, repeatButton, queueButton, speedButton};
        String[] ids = new String[]{"shuffle", "repeat", "queue", "speed"};
        int[] pos = new int[2];
        for (int i = 0; i < candidates.length; i++) {
            View v = candidates[i];
            if (v == null || v.getVisibility() != View.VISIBLE || ids[i].equals(excludeId)) continue;
            try {
                v.getLocationOnScreen(pos);
                if (rawX >= pos[0] && rawX <= pos[0] + v.getWidth()
                        && rawY >= pos[1] && rawY <= pos[1] + v.getHeight()) {
                    return v;
                }
            } catch (Throwable ignore) {}
        }
        return null;
    }

    /** Opens ONLY the tapped element's panel — never the full sheet. */
    public void openSectionSheet(String section) {
        try {
            new MiogramPlayerSectionSheet(getContext(), resourcesProvider, MiogramModernPlayerLayout.this, section).show();
        } catch (Throwable ignore) {}
    }
}
