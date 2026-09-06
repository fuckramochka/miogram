package app.miogram.bridge.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.lyrics.MiogramLyricsView;

/**
 * Modern Telegram-native Audio Player Layout.
 * Full-height immersive architecture with 3 fluid tabs:
 * 0. Track (Hero Album Art with Spring Scale + Live Karaoke Ticker)
 * 1. Lyrics & AI (Full-height synchronized karaoke + instant translation + AI speech transcription)
 * 2. Queue (Playlist tracks + search)
 */
public class MiogramModernPlayerLayout extends FrameLayout {

    public static final int TAB_TRACK = 0;
    public static final int TAB_LYRICS = 1;
    public static final int TAB_QUEUE = 2;

    private AudioPlayerAlert alert;
    private Theme.ResourcesProvider resourcesProvider;

    // Header
    private FrameLayout headerLayout;
    private ImageView collapseButton;
    private LinearLayout headerTitleLayout;
    private TextView headerSubtitleView;
    private TextView headerTitleView;
    private FrameLayout optionsContainer;

    // Tabs
    private FrameLayout tabsContainer;
    private View tabIndicator;
    private final TextView[] tabButtons = new TextView[3];
    private int currentTab = TAB_TRACK;
    private ValueAnimator tabAnimator;

    // Content Pages
    private FrameLayout pagesContainer;
    private final FrameLayout[] pages = new FrameLayout[3];

    // Page 0: Track
    private FrameLayout coverWrapper;
    private FrameLayout coverCard;
    private View coverView;
    private LinearLayout liveKaraokeTicker;
    private ImageView tickerIcon;
    private TextView tickerTextView;

    // Page 1: Lyrics
    private MiogramLyricsView lyricsView;

    // Page 2: Queue
    private View queueListView;

    // Bottom Controls Section
    private LinearLayout bottomSection;
    private FrameLayout infoRow;
    private FrameLayout titleContainer;
    private View titleSwitcher;
    private View authorSwitcher;
    private ImageView heartButton;

    private LinearLayout seekbarContainer;
    private View seekBarView;
    private FrameLayout timersRow;
    private View timeView;
    private View durationView;

    private LinearLayout mainControlsRow;
    private View shuffleButton;
    private View prevButton;
    private FrameLayout heroPlayButton;
    private View playButton;
    private View nextButton;
    private View repeatButton;

    private LinearLayout utilityRow;
    private TextView speedButton;
    private View castButton;
    private ImageView shareButton;

    private boolean isPlaying = false;
    private boolean isFavorite = false;
    private String lastKaraokeLine = "";

    public MiogramModernPlayerLayout(@NonNull Context context, AudioPlayerAlert alert, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.alert = alert;
        this.resourcesProvider = resourcesProvider;
        init();
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    private void init() {
        Context context = getContext();
        setClipChildren(false);

        // 1. Header (52dp)
        initHeader(context);

        // 2. Segmented Tabs (38dp)
        initTabs(context);

        // 3. Pages Container (center)
        initPages(context);

        // 4. Bottom Controls Section (docked at bottom)
        initBottomControls(context);

        // Layout arrangement
        addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.TOP, 0, 8, 0, 0));
        addView(tabsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP, 20, 64, 20, 0));
        addView(bottomSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 0, 0, 0, 16));

        // Pages container sits between tabs and bottomSection
        FrameLayout.LayoutParams pagesLp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL);
        pagesLp.topMargin = AndroidUtilities.dp(108);
        pagesLp.bottomMargin = AndroidUtilities.dp(218);
        addView(pagesContainer, pagesLp);
    }

    private void initHeader(Context context) {
        headerLayout = new FrameLayout(context);

        // Drag handle pill at the very top center
        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_sheet_scrollUp), 120));
        handleBg.setCornerRadius(AndroidUtilities.dp(2));
        dragHandle.setBackground(handleBg);
        headerLayout.addView(dragHandle, LayoutHelper.createFrame(38, 4, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

        // Dismiss / collapse chevron down button
        collapseButton = new ImageView(context);
        collapseButton.setImageResource(R.drawable.ic_ab_back);
        collapseButton.setRotation(270f);
        collapseButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_player_actionBarTitle), PorterDuff.Mode.SRC_IN));
        collapseButton.setScaleType(ImageView.ScaleType.CENTER);
        collapseButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(20)));
        collapseButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (alert != null) {
                alert.dismiss();
            }
        });
        headerLayout.addView(collapseButton, LayoutHelper.createFrame(44, 44, Gravity.LEFT | Gravity.CENTER_VERTICAL, 12, 4, 0, 0));

        // Header Title / Subtitle
        headerTitleLayout = new LinearLayout(context);
        headerTitleLayout.setOrientation(LinearLayout.VERTICAL);
        headerTitleLayout.setGravity(Gravity.CENTER);

        headerSubtitleView = new TextView(context);
        headerSubtitleView.setText(MiogramLocale.get("ВІДТВОРЕННЯ З ЧАТУ", "ВОСПРОИЗВЕДЕНИЕ ИЗ ЧАТА", "PLAYING FROM CHAT"));
        headerSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        headerSubtitleView.setTypeface(AndroidUtilities.bold());
        headerSubtitleView.setTextColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_player_actionBarTitle), 140));
        headerSubtitleView.setSingleLine(true);
        headerSubtitleView.setEllipsize(TextUtils.TruncateAt.END);
        headerTitleLayout.addView(headerSubtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        headerTitleView = new TextView(context);
        headerTitleView.setText(MiogramLocale.get("Miogram Аудіоплеєр", "Miogram Аудиоплеер", "Miogram Audio Player"));
        headerTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        headerTitleView.setTypeface(AndroidUtilities.bold());
        headerTitleView.setTextColor(getThemedColor(Theme.key_player_actionBarTitle));
        headerTitleView.setSingleLine(true);
        headerTitleView.setEllipsize(TextUtils.TruncateAt.END);
        headerTitleLayout.addView(headerTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        headerLayout.addView(headerTitleLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 56, 4, 56, 0));

        // Options menu container (right)
        optionsContainer = new FrameLayout(context);
        headerLayout.addView(optionsContainer, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 4, 12, 0));
    }

    private void initTabs(Context context) {
        tabsContainer = new FrameLayout(context);
        GradientDrawable tabsBg = new GradientDrawable();
        int baseBg = getThemedColor(Theme.key_player_actionBarTitle);
        tabsBg.setColor(ColorUtils.setAlphaComponent(baseBg, 22));
        tabsBg.setCornerRadius(AndroidUtilities.dp(19));
        tabsContainer.setBackground(tabsBg);

        // Indicator pill
        tabIndicator = new View(context);
        GradientDrawable indBg = new GradientDrawable();
        indBg.setColor(ColorUtils.setAlphaComponent(baseBg, 55));
        indBg.setCornerRadius(AndroidUtilities.dp(16));
        tabIndicator.setBackground(indBg);
        tabsContainer.addView(tabIndicator, LayoutHelper.createFrame(0, 32, Gravity.LEFT | Gravity.CENTER_VERTICAL, 3, 0, 0, 0));

        LinearLayout buttonsRow = new LinearLayout(context);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setWeightSum(3);

        String[] titles = new String[]{
            "🎵 " + MiogramLocale.get("Трек", "Трек", "Track"),
            "💬 " + MiogramLocale.get("Караоке", "Караоке", "Lyrics"),
            "📑 " + MiogramLocale.get("Черга", "Очередь", "Queue")
        };

        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView btn = new TextView(context);
            btn.setText(titles[i]);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            btn.setTypeface(AndroidUtilities.bold());
            btn.setGravity(Gravity.CENTER);
            btn.setTextColor(getThemedColor(Theme.key_player_actionBarTitle));
            btn.setAlpha(i == TAB_TRACK ? 1.0f : 0.55f);
            btn.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                switchToTab(index, true);
            });
            tabButtons[i] = btn;
            buttonsRow.addView(btn, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }

        tabsContainer.addView(buttonsRow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        tabsContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldL, oldT, oldR, oldB) -> {
            int w = (right - left - AndroidUtilities.dp(6)) / 3;
            if (w > 0) {
                ViewGroup.LayoutParams lp = tabIndicator.getLayoutParams();
                if (lp.width != w) {
                    lp.width = w;
                    tabIndicator.setLayoutParams(lp);
                }
                tabIndicator.setTranslationX(AndroidUtilities.dp(3) + currentTab * w);
            }
        });
    }

    private void initPages(Context context) {
        pagesContainer = new FrameLayout(context);
        pagesContainer.setClipChildren(false);

        for (int i = 0; i < 3; i++) {
            pages[i] = new FrameLayout(context);
            pages[i].setVisibility(i == TAB_TRACK ? VISIBLE : GONE);
            pages[i].setAlpha(i == TAB_TRACK ? 1.0f : 0.0f);
            pagesContainer.addView(pages[i], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        // Page 0: Track (Cover + Live Ticker)
        initPageTrack(context);

        // Page 1: Lyrics
        initPageLyrics(context);

        // Page 2: Queue
        initPageQueue(context);
    }

    private void initPageTrack(Context context) {
        FrameLayout page0 = pages[TAB_TRACK];

        LinearLayout centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        centerLayout.setGravity(Gravity.CENTER);

        coverWrapper = new FrameLayout(context);
        coverCard = new FrameLayout(context);
        if (Build.VERSION.SDK_INT >= 21) {
            coverCard.setElevation(AndroidUtilities.dp(12));
            coverCard.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(24));
                }
            });
            coverCard.setClipToOutline(true);
        }
        coverCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchToTab(TAB_LYRICS, true);
        });

        coverWrapper.addView(coverCard, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        centerLayout.addView(coverWrapper, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        // Live Karaoke Ticker Pill
        liveKaraokeTicker = new LinearLayout(context);
        liveKaraokeTicker.setOrientation(LinearLayout.HORIZONTAL);
        liveKaraokeTicker.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable tickerBg = new GradientDrawable();
        tickerBg.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_player_actionBarTitle), 25));
        tickerBg.setCornerRadius(AndroidUtilities.dp(18));
        liveKaraokeTicker.setBackground(tickerBg);
        liveKaraokeTicker.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

        tickerIcon = new ImageView(context);
        tickerIcon.setImageResource(R.drawable.ic_lyrics);
        tickerIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_player_actionBarTitle), PorterDuff.Mode.SRC_IN));
        liveKaraokeTicker.addView(tickerIcon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        tickerTextView = new TextView(context);
        tickerTextView.setText(MiogramLocale.get("Торкніться для перегляду караоке або ШІ-розшифровки ✨", "Коснитесь для караоке или ИИ-расшифровки ✨", "Tap for karaoke lyrics or AI transcription ✨"));
        tickerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tickerTextView.setTypeface(AndroidUtilities.bold());
        tickerTextView.setTextColor(getThemedColor(Theme.key_player_actionBarTitle));
        tickerTextView.setSingleLine(true);
        tickerTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tickerTextView.setSelected(true);
        liveKaraokeTicker.addView(tickerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        liveKaraokeTicker.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchToTab(TAB_LYRICS, true);
        });

        LinearLayout.LayoutParams tickerLp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
        tickerLp.topMargin = AndroidUtilities.dp(18);
        tickerLp.leftMargin = AndroidUtilities.dp(24);
        tickerLp.rightMargin = AndroidUtilities.dp(24);
        centerLayout.addView(liveKaraokeTicker, tickerLp);

        page0.addView(centerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
    }

    private void initPageLyrics(Context context) {
        // Will hold lyricsView
    }

    private void initPageQueue(Context context) {
        // Will hold queueListView
    }

    private void initBottomControls(Context context) {
        bottomSection = new LinearLayout(context);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setPadding(AndroidUtilities.dp(22), 0, AndroidUtilities.dp(22), 0);

        // 1. Info Row (Title, Artist, Like)
        infoRow = new FrameLayout(context);
        titleContainer = new FrameLayout(context);
        infoRow.addView(titleContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 0, 0, 48, 0));

        heartButton = new ImageView(context);
        heartButton.setImageResource(R.drawable.baseline_favorite_20);
        heartButton.setColorFilter(new PorterDuffColorFilter(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_player_actionBarTitle), 140), PorterDuff.Mode.SRC_IN));
        heartButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        heartButton.setScaleType(ImageView.ScaleType.CENTER);
        heartButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            isFavorite = !isFavorite;
            updateHeartVisual();
            if (alert != null) {
                alert.toggleFavorite();
            }
        });
        infoRow.addView(heartButton, LayoutHelper.createFrame(40, 40, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        bottomSection.addView(infoRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // 2. Seekbar Row
        seekbarContainer = new LinearLayout(context);
        seekbarContainer.setOrientation(LinearLayout.VERTICAL);
        timersRow = new FrameLayout(context);
        seekbarContainer.addView(timersRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        bottomSection.addView(seekbarContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3. Hero Controls Row (Shuffle, Prev, Play/Pause 60dp FAB, Next, Repeat)
        mainControlsRow = new LinearLayout(context);
        mainControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        mainControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        mainControlsRow.setWeightSum(5);

        // Circular Hero Play button container (60x60 dp)
        heroPlayButton = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable();
        int accent = getThemedColor(Theme.key_featuredStickers_addButton);
        heroBg.setColor(accent);
        heroBg.setShape(GradientDrawable.OVAL);
        heroPlayButton.setBackground(heroBg);
        if (Build.VERSION.SDK_INT >= 21) {
            heroPlayButton.setElevation(AndroidUtilities.dp(6));
        }

        bottomSection.addView(mainControlsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 68, 0, 6, 0, 4));

        // 4. Utility Row (Speed, Cast, Share)
        utilityRow = new LinearLayout(context);
        utilityRow.setOrientation(LinearLayout.HORIZONTAL);
        utilityRow.setGravity(Gravity.CENTER);

        speedButton = new TextView(context);
        speedButton.setText("1.0x");
        speedButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        speedButton.setTypeface(AndroidUtilities.bold());
        speedButton.setTextColor(getThemedColor(Theme.key_player_actionBarTitle));
        speedButton.setGravity(Gravity.CENTER);
        GradientDrawable speedBg = new GradientDrawable();
        speedBg.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_player_actionBarTitle), 25));
        speedBg.setCornerRadius(AndroidUtilities.dp(12));
        speedButton.setBackground(speedBg);
        speedButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        utilityRow.addView(speedButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        shareButton = new ImageView(context);
        shareButton.setImageResource(R.drawable.action_share);
        shareButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_player_actionBarTitle), PorterDuff.Mode.SRC_IN));
        shareButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(16)));
        shareButton.setScaleType(ImageView.ScaleType.CENTER);
        shareButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (alert != null) {
                alert.shareCurrentSong();
            }
        });
        utilityRow.addView(shareButton, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL));

        bottomSection.addView(utilityRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
    }

    public void setCoverView(View cover) {
        this.coverView = cover;
        if (coverCard != null && cover != null) {
            if (cover.getParent() instanceof ViewGroup) {
                ((ViewGroup) cover.getParent()).removeView(cover);
            }
            coverCard.removeAllViews();
            int size = Math.min(AndroidUtilities.dp(260), AndroidUtilities.displaySize.x - AndroidUtilities.dp(80));
            coverCard.addView(cover, LayoutHelper.createFrame(size, size, Gravity.CENTER));
        }
    }

    public void setLyricsView(MiogramLyricsView view) {
        this.lyricsView = view;
        if (pages[TAB_LYRICS] != null && view != null) {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            pages[TAB_LYRICS].removeAllViews();
            pages[TAB_LYRICS].addView(view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            view.setOnActiveLineChangeListener((text, translation, index) -> {
                if (TextUtils.isEmpty(text)) {
                    tickerTextView.setText(MiogramLocale.get("Торкніться для перегляду караоке або ШІ-розшифровки ✨", "Коснитесь для караоке или ИИ-расшифровки ✨", "Tap for karaoke lyrics or AI transcription ✨"));
                } else {
                    lastKaraokeLine = text;
                    tickerTextView.setText("🎵 " + text);
                }
            });
        }
    }

    public void setQueueListView(View listView) {
        this.queueListView = listView;
        if (pages[TAB_QUEUE] != null && listView != null) {
            if (listView.getParent() instanceof ViewGroup) {
                ((ViewGroup) listView.getParent()).removeView(listView);
            }
            pages[TAB_QUEUE].removeAllViews();
            pages[TAB_QUEUE].addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }
    }

    public void setOptionsButton(View optionsBtn) {
        if (optionsContainer != null && optionsBtn != null) {
            if (optionsBtn.getParent() instanceof ViewGroup) {
                ((ViewGroup) optionsBtn.getParent()).removeView(optionsBtn);
            }
            optionsContainer.removeAllViews();
            optionsContainer.addView(optionsBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        }
    }

    public void setTrackInfoViews(View titleSwitcher, View authorSwitcher) {
        this.titleSwitcher = titleSwitcher;
        this.authorSwitcher = authorSwitcher;
        if (titleContainer != null) {
            titleContainer.removeAllViews();
            if (titleSwitcher != null) {
                if (titleSwitcher.getParent() instanceof ViewGroup) {
                    ((ViewGroup) titleSwitcher.getParent()).removeView(titleSwitcher);
                }
                titleContainer.addView(titleSwitcher, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));
            }
            if (authorSwitcher != null) {
                if (authorSwitcher.getParent() instanceof ViewGroup) {
                    ((ViewGroup) authorSwitcher.getParent()).removeView(authorSwitcher);
                }
                titleContainer.addView(authorSwitcher, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 0, 24, 0, 0));
            }
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
            seekbarContainer.addView(seekBar, 0, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34));
        }

        if (timersRow != null) {
            timersRow.removeAllViews();
            if (time != null) {
                if (time.getParent() instanceof ViewGroup) {
                    ((ViewGroup) time.getParent()).removeView(time);
                }
                timersRow.addView(time, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));
            }
            if (duration != null) {
                if (duration.getParent() instanceof ViewGroup) {
                    ((ViewGroup) duration.getParent()).removeView(duration);
                }
                timersRow.addView(duration, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
            }
        }
    }

    public void setControlButtons(View shuffle, View prev, View play, View next, View repeat) {
        this.shuffleButton = shuffle;
        this.prevButton = prev;
        this.playButton = play;
        this.nextButton = next;
        this.repeatButton = repeat;

        if (mainControlsRow != null) {
            mainControlsRow.removeAllViews();

            // Slot 0: Shuffle
            FrameLayout slot0 = new FrameLayout(getContext());
            if (shuffle != null) {
                if (shuffle.getParent() instanceof ViewGroup) ((ViewGroup) shuffle.getParent()).removeView(shuffle);
                slot0.addView(shuffle, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            }
            mainControlsRow.addView(slot0, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 1: Prev
            FrameLayout slot1 = new FrameLayout(getContext());
            if (prev != null) {
                if (prev.getParent() instanceof ViewGroup) ((ViewGroup) prev.getParent()).removeView(prev);
                slot1.addView(prev, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot1, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 2: Hero Play/Pause FAB
            FrameLayout slot2 = new FrameLayout(getContext());
            if (play != null) {
                if (play.getParent() instanceof ViewGroup) ((ViewGroup) play.getParent()).removeView(play);
                heroPlayButton.removeAllViews();
                heroPlayButton.addView(play, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            }
            slot2.addView(heroPlayButton, LayoutHelper.createFrame(60, 60, Gravity.CENTER));
            mainControlsRow.addView(slot2, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 3: Next
            FrameLayout slot3 = new FrameLayout(getContext());
            if (next != null) {
                if (next.getParent() instanceof ViewGroup) ((ViewGroup) next.getParent()).removeView(next);
                slot3.addView(next, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot3, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 4: Repeat
            FrameLayout slot4 = new FrameLayout(getContext());
            if (repeat != null) {
                if (repeat.getParent() instanceof ViewGroup) ((ViewGroup) repeat.getParent()).removeView(repeat);
                slot4.addView(repeat, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            }
            mainControlsRow.addView(slot4, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }
    }

    public void setCastButton(View cast) {
        this.castButton = cast;
        if (utilityRow != null && cast != null) {
            if (cast.getParent() instanceof ViewGroup) {
                ((ViewGroup) cast.getParent()).removeView(cast);
            }
            utilityRow.addView(cast, 1, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));
        }
    }

    public void switchToTab(int newTab, boolean animated) {
        if (newTab < 0 || newTab > 2 || newTab == currentTab) return;

        final int oldTab = currentTab;
        currentTab = newTab;

        for (int i = 0; i < 3; i++) {
            tabButtons[i].animate().alpha(i == currentTab ? 1.0f : 0.55f).setDuration(200).start();
        }

        // Animate indicator
        int tabW = (tabsContainer.getWidth() - AndroidUtilities.dp(6)) / 3;
        if (tabW > 0) {
            float startX = tabIndicator.getTranslationX();
            float targetX = AndroidUtilities.dp(3) + currentTab * tabW;
            if (tabAnimator != null) tabAnimator.cancel();
            tabAnimator = ValueAnimator.ofFloat(startX, targetX);
            tabAnimator.setDuration(animated ? 250 : 0);
            tabAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            tabAnimator.addUpdateListener(a -> tabIndicator.setTranslationX((float) a.getAnimatedValue()));
            tabAnimator.start();
        }

        // Animate pages crossfade
        final FrameLayout outPage = pages[oldTab];
        final FrameLayout inPage = pages[newTab];

        if (animated) {
            outPage.animate().alpha(0f).translationY(AndroidUtilities.dp(8)).setDuration(180).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    outPage.setVisibility(GONE);
                }
            }).start();

            inPage.setVisibility(VISIBLE);
            inPage.setAlpha(0f);
            inPage.setTranslationY(-AndroidUtilities.dp(8));
            inPage.animate().alpha(1f).translationY(0f).setDuration(220).setListener(null).start();
        } else {
            outPage.setVisibility(GONE);
            inPage.setVisibility(VISIBLE);
            inPage.setAlpha(1.0f);
            inPage.setTranslationY(0f);
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (coverCard != null) {
            float targetScale = playing ? 1.0f : 0.92f;
            coverCard.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(350)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start();
        }
    }

    public void setHeaderSubtitle(String subtitle) {
        if (headerSubtitleView != null && !TextUtils.isEmpty(subtitle)) {
            headerSubtitleView.setText(subtitle.toUpperCase(java.util.Locale.ROOT));
        }
    }

    public void setHeaderTitle(String title) {
        if (headerTitleView != null && !TextUtils.isEmpty(title)) {
            headerTitleView.setText(title);
        }
    }

    public void setSpeedText(String speed) {
        if (speedButton != null && !TextUtils.isEmpty(speed)) {
            speedButton.setText(speed);
        }
    }

    public void setFavorite(boolean fav) {
        this.isFavorite = fav;
        updateHeartVisual();
    }

    private void updateHeartVisual() {
        if (heartButton != null) {
            if (isFavorite) {
                heartButton.setImageResource(R.drawable.baseline_favorite_20);
                heartButton.setColorFilter(new PorterDuffColorFilter(0xFFFF3B30, PorterDuff.Mode.SRC_IN));
            } else {
                heartButton.setImageResource(R.drawable.baseline_favorite_20);
                heartButton.setColorFilter(new PorterDuffColorFilter(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_player_actionBarTitle), 140), PorterDuff.Mode.SRC_IN));
            }
        }
    }

    public int getCurrentTab() {
        return currentTab;
    }
}
