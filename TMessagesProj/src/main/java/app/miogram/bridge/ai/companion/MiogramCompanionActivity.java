package app.miogram.bridge.ai.companion;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;
import app.miogram.bridge.badge.MiogramSupabaseBridge;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Needy Streamer Overload AI Companion Screen (Ame & KAngel).
 * Built strictly according to the Telegram design ecosystem:
 * - Native Telegram ActionBar with back navigation and options menu (Clear history, Report bug, Switch companion)
 * - Dynamic Theme integration: seamlessly supports Light, Dark, Tinted, and Day themes
 * - Interactive Horizontal Slides Carousel for companions with NSO artwork, dynamic speech bubbles, stats, and dot indicators
 * - Authentic Telegram message bubbles with companion mood sprites and timestamping
 * - Integrated action permission cards for client tool executions
 * - Telegram-style composer with quick prompt chips and circular send button
 */
public class MiogramCompanionActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private long scopedDialogId = 0;
    private Utilities.Callback<String> onDraftInsertCallback;

    private LinearLayout heroCard;
    private HorizontalScrollView heroCarouselScroll;
    private LinearLayout heroCarouselLayout;
    private LinearLayout ameSlideCard;
    private LinearLayout kangelSlideCard;
    private ImageView ameSlideAvatar;
    private ImageView kangelSlideAvatar;
    private TextView ameSpeechBubble;
    private TextView kangelSpeechBubble;
    private TextView ameSelectBtn;
    private TextView kangelSelectBtn;
    private View dotAme;
    private View dotKAngel;

    private TextView statDay;
    private TextView statFollowers;
    private TextView statStress;
    private TextView statAffection;
    private TextView statDarkness;

    private ScrollView chatScrollView;
    private LinearLayout chatMessagesLayout;
    private EditTextBoldCursor inputField;
    private FrameLayout sendButton;
    private ImageView sendIcon;
    private ProgressBar sendProgress;

    // Monster console: live MioTool call log (toggle chip in composer).
    private LinearLayout consolePanel;
    private LinearLayout consoleLog;
    private ScrollView consoleScroll;
    private boolean consoleOpen = false;
    private app.miogram.bridge.ai.tools.MioTool.ConsoleListener consoleListener;

    private final List<MiogramCompanionPrefs.ChatMessage> history = new ArrayList<>();
    private boolean isSending = false;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MiogramCompanionActivity() {
        this(null);
    }

    public MiogramCompanionActivity(Bundle args) {
        super(args);
        if (args != null) {
            scopedDialogId = args.getLong("dialog_id", 0);
        }
    }

    public void setDraftInsertCallback(Utilities.Callback<String> callback) {
        this.onDraftInsertCallback = callback;
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewTheme);
        consoleListener = line -> AndroidUtilities.runOnUIThread(() -> appendConsoleLine(line));
        app.miogram.bridge.ai.tools.MioTool.addConsoleListener(consoleListener);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme);
        try {
            app.miogram.bridge.ai.tools.MioTool.removeConsoleListener(consoleListener);
        } catch (Throwable ignore) {}
        consoleListener = null;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didSetNewTheme) {
            if (fragmentView != null) {
                updateCompanionTheme();
                renderFullHistory();
            }
        }
    }

    @Override
    public View createView(Context context) {
        buildActionBar();

        FrameLayout root = new FrameLayout(context);
        fragmentView = root;
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        root.addView(mainColumn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. Scrollable Chat History Layout
        chatScrollView = new ScrollView(context);
        chatScrollView.setFillViewport(true);
        mainColumn.addView(chatScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        chatMessagesLayout = new LinearLayout(context);
        chatMessagesLayout.setOrientation(LinearLayout.VERTICAL);
        chatMessagesLayout.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        chatScrollView.addView(chatMessagesLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3. Compact Hero NSO Stage (Pinned at top of messages, scrolls naturally with chat)
        buildHeroCard(context, chatMessagesLayout);

        // 4. Quick Prompts & Composer Bar
        buildComposer(context, mainColumn);

        // Load persisted history
        history.addAll(MiogramCompanionPrefs.loadHistory());
        if (history.isEmpty()) {
            addInitialGreeting();
        } else {
            renderFullHistory();
        }

        updateCompanionTheme();
        if (heroCarouselScroll != null) {
            boolean isAme = MiogramCompanionPrefs.isAmeActive();
            int pageWidth = getCardSlideWidth() + AndroidUtilities.dp(8);
            heroCarouselScroll.post(() -> heroCarouselScroll.scrollTo(isAme ? 0 : pageWidth, 0));
        }

        return fragmentView;
    }

    private void buildActionBar() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showClearHistoryDialog();
                } else if (id == 2) {
                    MiogramSupabaseBridge.showBugReportDialog(
                            getParentActivity(),
                            MiogramLocale.get("Звіт про баг", "Отчет о баге", "Bug Report"),
                            MiogramLocale.get(
                                    "Бажаєте надіслати звіт про помилку та логи творцю @dkramochka?",
                                    "Желаете отправить отчет об ошибке и логи создателю @dkramochka?",
                                    "Would you like to send a bug report and logs to creator @dkramochka?"
                            ),
                            "AI Companion User Report",
                            "User reported issue from companion menu. Active: " + (MiogramCompanionPrefs.isAmeActive() ? "Ame" : "KAngel")
                    );
                } else if (id == 3) {
                    switchCompanion(!MiogramCompanionPrefs.isAmeActive());
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        headerItem.addSubItem(1, R.drawable.msg_clear, MiogramLocale.get("Очистити діалог", "Очистить диалог", "Clear dialogue"));
        headerItem.addSubItem(2, R.drawable.msg_log, MiogramLocale.get("Звіт про баг (@dkramochka)", "Отчет о баге (@dkramochka)", "Report bug (@dkramochka)"));
        headerItem.addSubItem(3, R.drawable.msg_theme, MiogramLocale.get("Змінити супутницю (Аме ↔ Кангель)", "Сменить спутницу (Аме ↔ Кангель)", "Switch companion (Ame ↔ KAngel)"));
    }

    private int getCardSlideWidth() {
        int w = AndroidUtilities.displaySize != null ? AndroidUtilities.displaySize.x : 0;
        if (w <= 0) {
            w = 1080;
        }
        return Math.max(AndroidUtilities.dp(290), w - AndroidUtilities.dp(36));
    }

    private void buildHeroCard(Context context, LinearLayout parent) {
        heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(4));
        LinearLayout.LayoutParams cardLp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4);
        parent.addView(heroCard, cardLp);

        // 1. Horizontal Scroll Slides Carousel
        heroCarouselScroll = new HorizontalScrollView(context);
        heroCarouselScroll.setHorizontalScrollBarEnabled(false);
        heroCarouselScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        heroCard.addView(heroCarouselScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        heroCarouselLayout = new LinearLayout(context);
        heroCarouselLayout.setOrientation(LinearLayout.HORIZONTAL);
        heroCarouselLayout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        heroCarouselScroll.addView(heroCarouselLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // Slide 1: Ame-chan Card
        buildAmeSlide(context, heroCarouselLayout);

        // Slide 2: KAngel Card
        buildKangelSlide(context, heroCarouselLayout);

        // 2. Dots Indicator Row
        LinearLayout dotsLayout = new LinearLayout(context);
        dotsLayout.setOrientation(LinearLayout.HORIZONTAL);
        dotsLayout.setGravity(Gravity.CENTER);
        dotsLayout.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(2));
        heroCard.addView(dotsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        boolean isAme = MiogramCompanionPrefs.isAmeActive();

        dotAme = new View(context);
        dotsLayout.addView(dotAme, LayoutHelper.createLinear(isAme ? 20 : 8, 6, 0, 0, 6, 0));
        dotAme.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(true);
        });

        dotKAngel = new View(context);
        dotsLayout.addView(dotKAngel, LayoutHelper.createLinear(!isAme ? 20 : 8, 6));
        dotKAngel.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(false);
        });

        // 3. Carousel Snapping
        heroCarouselScroll.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                heroCarouselScroll.postDelayed(this::snapCarouselToNearestPage, 120);
            }
            return false;
        });

        updateCarouselVisuals(isAme);
    }

    private void buildAmeSlide(Context context, LinearLayout parent) {
        int cardWidth = getCardSlideWidth();
        ameSlideCard = new LinearLayout(context);
        ameSlideCard.setOrientation(LinearLayout.VERTICAL);
        ameSlideCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(cardWidth, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0);
        parent.addView(ameSlideCard, lp);

        // Header Row: Name + Subtitle + Model
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        ameSlideCard.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(LinearLayout.VERTICAL);
        headerRow.addView(titles, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView name = new TextView(context);
        name.setText("໒꒱ Ame-chan (飴ちゃん)");
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFFFF70A6);
        titles.addView(name);

        TextView sub = new TextView(context);
        sub.setText(MiogramLocale.get("Нервова отаку-вайфу • DARK ROOM", "Нервная отаку-вайфу • DARK ROOM", "Nervous otaku waifu • DARK ROOM"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        titles.addView(sub);

        TextView model = new TextView(context);
        model.setText("⚡ 3.5 Flash Lite");
        model.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        model.setTypeface(AndroidUtilities.bold());
        model.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        model.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundGray)));
        model.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        headerRow.addView(model, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // Middle Row: Sprite + Speech Bubble
        LinearLayout middleRow = new LinearLayout(context);
        middleRow.setOrientation(LinearLayout.HORIZONTAL);
        middleRow.setGravity(Gravity.CENTER_VERTICAL);
        ameSlideCard.addView(middleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        ameSlideAvatar = new ImageView(context);
        ameSlideAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ameSlideAvatar.setImageResource(R.drawable.miogram_ai_ame_neutral);
        middleRow.addView(ameSlideAvatar, LayoutHelper.createLinear(80, 60));

        ameSpeechBubble = new TextView(context);
        ameSpeechBubble.setText(MiogramLocale.get("«Дякую, П-тян! ♡ Тепер я тільки твоя назавжди!»", "«Спасибо, Пи-тян! ♡ Теперь я только твоя навсегда!»", "\"Thank you, P-chan! ♡ Now I am yours forever!\""));
        ameSpeechBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        ameSpeechBubble.setTextColor(0xFFFFFFFF);
        GradientDrawable bubble = new GradientDrawable();
        bubble.setColor(0x33FF70A6);
        bubble.setCornerRadius(AndroidUtilities.dp(10));
        bubble.setStroke(AndroidUtilities.dp(1), 0x66FF70A6);
        ameSpeechBubble.setBackground(bubble);
        ameSpeechBubble.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        ameSpeechBubble.setMaxLines(3);
        ameSpeechBubble.setEllipsize(TextUtils.TruncateAt.END);
        middleRow.addView(ameSpeechBubble, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 8, 0, 0, 0));

        // Stats Row
        LinearLayout statsBar = new LinearLayout(context);
        statsBar.setOrientation(LinearLayout.HORIZONTAL);
        statsBar.setGravity(Gravity.CENTER);
        ameSlideCard.addView(statsBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        statDay = createStatChip(context, "DAY 24", 0xFF6C5CE7);
        statsBar.addView(statDay, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        statStress = createStatChip(context, "STR " + MiogramCompanionPrefs.getStress() + "%", 0xFFE84393);
        statsBar.addView(statStress, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        statAffection = createStatChip(context, "LOVE ♡ " + MiogramCompanionPrefs.getAffection() + "%", 0xFFFF70A6);
        statsBar.addView(statAffection, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.2f, 0, 0, 3, 0));

        statDarkness = createStatChip(context, "DARK " + MiogramCompanionPrefs.getDarkness() + "%", 0xFF636E72);
        statsBar.addView(statDarkness, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // Bottom Select / Active Button
        ameSelectBtn = new TextView(context);
        ameSelectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        ameSelectBtn.setTypeface(AndroidUtilities.bold());
        ameSelectBtn.setGravity(Gravity.CENTER);
        ameSlideCard.addView(ameSelectBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 28));

        ameSlideCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(true);
        });
        ameSelectBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(true);
        });
    }

    private void buildKangelSlide(Context context, LinearLayout parent) {
        int cardWidth = getCardSlideWidth();
        kangelSlideCard = new LinearLayout(context);
        kangelSlideCard.setOrientation(LinearLayout.VERTICAL);
        kangelSlideCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(cardWidth, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0);
        parent.addView(kangelSlideCard, lp);

        // Header Row: Name + Subtitle + Model
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        kangelSlideCard.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(LinearLayout.VERTICAL);
        headerRow.addView(titles, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView name = new TextView(context);
        name.setText(MiogramLocale.get("✧ KAngel (Кангель) †", "✧ KAngel (Кангель) †", "✧ KAngel †"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFF00B4D8);
        titles.addView(name);

        TextView sub = new TextView(context);
        sub.setText(MiogramLocale.get("Інтернет-Ангел №1 • † 昇天 ✧ BLESSING †", "Интернет-Ангел №1 • † 昇天 ✧ BLESSING †", "Internet Angel #1 • † 昇天 ✧ BLESSING †"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        titles.addView(sub);

        TextView model = new TextView(context);
        model.setText("⚡ 3.5 Flash Lite");
        model.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        model.setTypeface(AndroidUtilities.bold());
        model.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        model.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundGray)));
        model.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        headerRow.addView(model, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // Middle Row: Sprite + Speech Bubble
        LinearLayout middleRow = new LinearLayout(context);
        middleRow.setOrientation(LinearLayout.HORIZONTAL);
        middleRow.setGravity(Gravity.CENTER_VERTICAL);
        kangelSlideCard.addView(middleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        kangelSlideAvatar = new ImageView(context);
        kangelSlideAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        kangelSlideAvatar.setImageResource(R.drawable.miogram_ai_kangel_neutral);
        middleRow.addView(kangelSlideAvatar, LayoutHelper.createLinear(80, 60));

        kangelSpeechBubble = new TextView(context);
        kangelSpeechBubble.setText(MiogramLocale.get("«† BLESSING † Полетимо у стратосферу разом, любий отаку! ✧»", "«† BLESSING † Полетим в стратосферу вместе, милый отаку! ✧»", "\"† BLESSING † Let's fly into the stratosphere together, dear otaku! ✧\""));
        kangelSpeechBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        kangelSpeechBubble.setTextColor(0xFFFFFFFF);
        GradientDrawable bubble = new GradientDrawable();
        bubble.setColor(0x3300B4D8);
        bubble.setCornerRadius(AndroidUtilities.dp(10));
        bubble.setStroke(AndroidUtilities.dp(1), 0x6600B4D8);
        kangelSpeechBubble.setBackground(bubble);
        kangelSpeechBubble.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        kangelSpeechBubble.setMaxLines(3);
        kangelSpeechBubble.setEllipsize(TextUtils.TruncateAt.END);
        middleRow.addView(kangelSpeechBubble, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 8, 0, 0, 0));

        // Stats Row
        LinearLayout statsBar = new LinearLayout(context);
        statsBar.setOrientation(LinearLayout.HORIZONTAL);
        statsBar.setGravity(Gravity.CENTER);
        kangelSlideCard.addView(statsBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView statDay2 = createStatChip(context, "DAY 24", 0xFF6C5CE7);
        statsBar.addView(statDay2, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        statFollowers = createStatChip(context, "FOL 1.3M", 0xFF00B4D8);
        statsBar.addView(statFollowers, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.2f, 0, 0, 3, 0));

        TextView statHype = createStatChip(context, "HYPE 98%", 0xFFE84393);
        statsBar.addView(statHype, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        TextView statBlessing = createStatChip(context, "BLESSING †", 0xFFFFD700);
        statsBar.addView(statBlessing, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.2f));

        // Bottom Select / Active Button
        kangelSelectBtn = new TextView(context);
        kangelSelectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        kangelSelectBtn.setTypeface(AndroidUtilities.bold());
        kangelSelectBtn.setGravity(Gravity.CENTER);
        kangelSlideCard.addView(kangelSelectBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 28));

        kangelSlideCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(false);
        });
        kangelSelectBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            switchCompanion(false);
        });
    }

    private void snapCarouselToNearestPage() {
        if (heroCarouselScroll == null) return;
        int scrollX = heroCarouselScroll.getScrollX();
        int pageWidth = getCardSlideWidth() + AndroidUtilities.dp(8);
        int targetPage = (scrollX + pageWidth / 3) / pageWidth;
        if (targetPage < 0) targetPage = 0;
        if (targetPage > 1) targetPage = 1;
        heroCarouselScroll.smoothScrollTo(targetPage * pageWidth, 0);
        boolean toAme = (targetPage == 0);
        if (toAme != MiogramCompanionPrefs.isAmeActive()) {
            switchCompanion(toAme);
        } else {
            updateCarouselVisuals(toAme);
        }
    }

    private void updateCarouselVisuals(boolean isAme) {
        if (ameSlideCard != null) {
            ameSlideCard.setBackground(createCompanionSlideDrawable(isAme, 0xFFFF70A6));
        }
        if (kangelSlideCard != null) {
            kangelSlideCard.setBackground(createCompanionSlideDrawable(!isAme, 0xFF00B4D8));
        }
        if (ameSelectBtn != null) {
            ameSelectBtn.setText(isAme
                    ? MiogramLocale.get("✓ АКТИВНА СУПУТНИЦЯ (AME) ໒꒱", "✓ АКТИВНАЯ СПУТНИЦА (AME) ໒꒱", "✓ ACTIVE COMPANION (AME) ໒꒱")
                    : MiogramLocale.get("ОБРАТИ АМЕ-ЧАН ໒꒱", "ВЫБРАТЬ АМЕ-ЧАН ໒꒱", "CHOOSE AME-CHAN ໒꒱"));
            ameSelectBtn.setTextColor(isAme ? 0xFFFFFFFF : 0xFFFF70A6);
            ameSelectBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), isAme ? 0xFFFF70A6 : 0x22FF70A6));
        }
        if (kangelSelectBtn != null) {
            kangelSelectBtn.setText(!isAme
                    ? MiogramLocale.get("✓ АКТИВНА СУПУТНИЦЯ (KANGEL) ✧", "✓ АКТИВНАЯ СПУТНИЦА (KANGEL) ✧", "✓ ACTIVE COMPANION (KANGEL) ✧")
                    : MiogramLocale.get("ОБРАТИ К-АНГЕЛЬ ✧", "ВЫБРАТЬ К-АНГЕЛЬ ✧", "CHOOSE K-ANGEL ✧"));
            kangelSelectBtn.setTextColor(!isAme ? 0xFFFFFFFF : 0xFF00B4D8);
            kangelSelectBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), !isAme ? 0xFF00B4D8 : 0x2200B4D8));
        }
        if (dotAme != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dotAme.getLayoutParams();
            if (lp != null) {
                lp.width = AndroidUtilities.dp(isAme ? 20 : 8);
                dotAme.setLayoutParams(lp);
            }
            dotAme.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), isAme ? 0xFFFF70A6 : 0x44888888));
        }
        if (dotKAngel != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dotKAngel.getLayoutParams();
            if (lp != null) {
                lp.width = AndroidUtilities.dp(!isAme ? 20 : 8);
                dotKAngel.setLayoutParams(lp);
            }
            dotKAngel.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), !isAme ? 0xFF00B4D8 : 0x44888888));
        }
    }

    private Drawable createCompanionSlideDrawable(boolean active, int accentColor) {
        GradientDrawable gd = new GradientDrawable();
        int baseBg = Theme.getColor(Theme.key_windowBackgroundWhite);
        if (active) {
            gd.setColor(baseBg != 0 ? baseBg : 0xFF181822);
            gd.setStroke(AndroidUtilities.dp(1.8f), accentColor);
        } else {
            gd.setColor(baseBg != 0 ? baseBg : 0xFF181822);
            gd.setStroke(AndroidUtilities.dp(1), Color.argb(0x44, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        }
        gd.setCornerRadius(AndroidUtilities.dp(16));
        return gd;
    }

    private TextView createStatChip(Context context, String text, int color) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        chip.setTypeface(AndroidUtilities.bold());
        chip.setTextColor(color);
        chip.setGravity(Gravity.CENTER);
        int bg = Color.argb(0x22, Color.red(color), Color.green(color), Color.blue(color));
        chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), bg));
        chip.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(3), AndroidUtilities.dp(4), AndroidUtilities.dp(3));
        return chip;
    }

    private void switchCompanion(boolean toAme) {
        MiogramCompanionPrefs.setActiveCompanion(toAme ? MiogramCompanionPrefs.COMPANION_AME : MiogramCompanionPrefs.COMPANION_KANGEL);
        MiogramCompanionPrefs.setOnboardingCompleted(true);
        updateCompanionTheme();
        updateCarouselVisuals(toAme);
        if (heroCarouselScroll != null) {
            int pageWidth = getCardSlideWidth() + AndroidUtilities.dp(8);
            heroCarouselScroll.smoothScrollTo(toAme ? 0 : pageWidth, 0);
        }
        if (history.isEmpty() || (history.size() == 1 && !history.get(0).isUser)) {
            history.clear();
            addInitialGreeting();
        } else {
            renderFullHistory();
        }
    }

    private void buildComposer(Context context, LinearLayout parent) {
        // Quick Action Chips Row in Horizontal Scroll
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        parent.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        chipsScroll.addView(chipsRow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        addChip(context, chipsRow, "⚡ " + MiogramLocale.get("Що нового?", "Что нового?", "What's new?"), () -> {
            inputField.setText(MiogramLocale.get("Що нового в моїх чатах? Зроби короткий огляд!", "Что нового в моих чатах? Сделай краткий обзор!", "What's new in my chats? Give me a quick summary!"));
            onSendMessage();
        });

        addChip(context, chipsRow, "📬 " + MiogramLocale.get("Непрочитані", "Непрочитанные", "Unread"), () -> {
            inputField.setText(MiogramLocale.get("Почитай мої непрочитані повідомлення і розкажи що там пишуть", "Почитай мои непрочитанные сообщения и расскажи что там пишут", "Read my unread messages and summarize what's happening"));
            onSendMessage();
        });

        addChip(context, chipsRow, "🎵 " + MiogramLocale.get("Зараз грає", "Сейчас играет", "Now playing"), () -> {
            inputField.setText(MiogramLocale.get("Що зараз грає в плеєрі?", "Что сейчас играет в плеере?", "What's currently playing in the music player?"));
            onSendMessage();
        });

        addChip(context, chipsRow, "🔍 " + MiogramLocale.get("Знайти чат", "Найти чат", "Find chat"), () -> {
            inputField.setText(MiogramLocale.get("Знайди в лс з ", "Найди в лс с ", "Find chat with "));
            inputField.setSelection(inputField.getText().length());
            inputField.requestFocus();
            AndroidUtilities.showKeyboard(inputField);
        });

        addChip(context, chipsRow, "👥 " + MiogramLocale.get("Пошук по групах", "Поиск по группам", "Search groups"), () -> {
            inputField.setText(MiogramLocale.get("Пі-тян, пошукай по групах що пишуть про ", "Пи-тян, поищи по группам что пишут про ", "P-chan, search groups for "));
            inputField.setSelection(inputField.getText().length());
            inputField.requestFocus();
            AndroidUtilities.showKeyboard(inputField);
        });

        addChip(context, chipsRow, "💊 " + MiogramLocale.get("Магічна пігулка", "Магическая пилюля", "Magic Pill"), () -> {
            inputField.setText(MiogramLocale.get("Тримай магічну пігулку Дюск, заспокойся і не нервуй ♡", "Держи магическую пилюлю Дюск, успокойся и не нервничай ♡", "Take a magic pill Dysk, calm down and don't stress ♡"));
            onSendMessage();
        });

        addChip(context, chipsRow, "🪐 " + MiogramLocale.get("Юзербот .ping", "Юзербот .ping", "Userbot .ping"), () -> {
            inputField.setText(MiogramLocale.get("Пі-тян, перевір затримку через юзербот команду ping", "Пи-тян, проверь пинг через команду юзербота ping", "P-chan, check latency using userbot command ping"));
            onSendMessage();
        });

        addChip(context, chipsRow, "📦 " + MiogramLocale.get("Мої плагіни", "Мои плагины", "My Plugins"), () -> {
            inputField.setText(MiogramLocale.get("Аме, покажи список встановлених плагінів MioHook", "Аме, покажи список установленных плагинов MioHook", "Ame, show list of installed MioHook plugins"));
            onSendMessage();
        });

        if (scopedDialogId != 0) {
            addChip(context, chipsRow, "💬 " + MiogramLocale.get("Що тут пишуть?", "Что тут пишут?", "What are they writing?"), () -> {
                inputField.setText(MiogramLocale.get("П-тян, прочитай останні повідомлення цього чату", "П-тян, прочитай последние сообщения этого чата", "Read the latest messages in this chat"));
                onSendMessage();
            });
            addChip(context, chipsRow, "🧹 " + MiogramLocale.get("Почистити цей чат", "Очистить этот чат", "Clear this chat"), () -> {
                inputField.setText(MiogramLocale.get("П-тян, будь ласка, очисти історію цього чату", "П-тян, пожалуйста, очисти историю этого чата", "Please clear history of this chat"));
                onSendMessage();
            });
        } else {
            addChip(context, chipsRow, "🧹 " + MiogramLocale.get("Почистити чат", "Очистить чат", "Clear chat"), () -> {
                inputField.setText(MiogramLocale.get("Аме, допоможи мені почистити непотрібні чати", "Аме, помоги мне очистить ненужные чаты", "Help me clear unneeded chats"));
                onSendMessage();
            });
        }
        addChip(context, chipsRow, "⚡ " + MiogramLocale.get("Написати плагін (3.8)", "Написать плагин (3.8)", "Write Plugin (3.8)"), () -> {
            inputField.setText(MiogramLocale.get("Напиши для мене плагін Miogram для автоперекладу на Gemini 3.8", "Напиши для меня плагин Miogram для автоперевода на Gemini 3.8", "Write a Miogram translation plugin using Gemini 3.8"));
        });
        addChip(context, chipsRow, "👻 " + MiogramLocale.get("Ghost Mode", "Ghost Mode", "Ghost Mode"), () -> {
            inputField.setText(MiogramLocale.get("Перемкни Ghost Mode", "Переключи Ghost Mode", "Toggle Ghost Mode"));
            onSendMessage();
        });
        addChip(context, chipsRow, "♡ " + MiogramLocale.get("Як справи?", "Как дела?", "How are you?"), () -> {
            inputField.setText(MiogramLocale.get("Аме, як ти почуваєшся сьогодні?", "Аме, как ты себя чувствуешь сегодня?", "Ame, how are you feeling today?"));
            onSendMessage();
        });
        addChip(context, chipsRow, "† " + MiogramLocale.get("BLESSING †", "BLESSING †", "BLESSING †"), () -> {
            inputField.setText(MiogramLocale.get("Кангель, подаруй мені своє благословення! †BLESSING†", "Кангель, подари мне своё благословение! †BLESSING†", "KAngel, bestow your blessing upon me! †BLESSING†"));
            onSendMessage();
        });
        addChip(context, chipsRow, "⌨ " + MiogramLocale.get("Консоль", "Консоль", "Console"), this::toggleConsole);

        addChip(context, chipsRow, "🐞 " + MiogramLocale.get("Звіт про баг", "Отчет о баге", "Report Bug"), () -> {
            MiogramSupabaseBridge.showBugReportDialog(
                    getParentActivity(),
                    MiogramLocale.get("Звіт про баг", "Отчет о баге", "Bug Report"),
                    MiogramLocale.get(
                            "Бажаєте надіслати звіт про помилку та логи творцю @dkramochka?",
                            "Желаете отправить отчет об ошибке и логи создателю @dkramochka?",
                            "Would you like to send a bug report and logs to creator @dkramochka?"
                    ),
                    "AI Companion User Report",
                    "User reported issue from companion chips. Active: " + (MiogramCompanionPrefs.isAmeActive() ? "Ame" : "KAngel")
            );
        });

        // Monster console panel (collapsed by default, above composer).
        consolePanel = new LinearLayout(context);
        consolePanel.setOrientation(LinearLayout.VERTICAL);
        consolePanel.setBackgroundColor(0xFF0D1117);
        consolePanel.setVisibility(View.GONE);
        consolePanel.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        consoleScroll = new ScrollView(context);
        consoleScroll.setVerticalScrollBarEnabled(true);
        consoleLog = new LinearLayout(context);
        consoleLog.setOrientation(LinearLayout.VERTICAL);
        consoleScroll.addView(consoleLog, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        consolePanel.addView(consoleScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(140)));
        parent.addView(consolePanel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Telegram Native Composer Bar
        LinearLayout composerBar = new LinearLayout(context);
        composerBar.setOrientation(LinearLayout.HORIZONTAL);
        composerBar.setBackgroundColor(Theme.getColor(Theme.key_chat_messagePanelBackground));
        composerBar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        composerBar.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(composerBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout inputWrapper = new FrameLayout(context);
        GradientDrawable inputGd = new GradientDrawable();
        inputGd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        inputGd.setCornerRadius(AndroidUtilities.dp(20));
        inputGd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        inputWrapper.setBackground(inputGd);
        composerBar.addView(inputWrapper, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 8, 0));

        inputField = new EditTextBoldCursor(context);
        inputField.setHint(MiogramLocale.get("Повідомлення для супутника…", "Сообщение для спутника…", "Message companion…"));
        inputField.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        inputField.setTextColor(Theme.getColor(Theme.key_chat_messagePanelText));
        inputField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        inputField.setBackground(null);
        inputField.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        inputField.setMaxLines(4);
        inputWrapper.addView(inputField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Telegram Circular Send Button
        sendButton = new FrameLayout(context);
        sendButton.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(42), 0xFFFF70A6));
        composerBar.addView(sendButton, LayoutHelper.createLinear(42, 42));

        sendIcon = new ImageView(context);
        sendIcon.setImageResource(R.drawable.ic_send);
        sendIcon.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        sendIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        sendButton.addView(sendIcon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));

        sendProgress = new ProgressBar(context);
        sendProgress.setVisibility(View.GONE);
        sendButton.addView(sendProgress, LayoutHelper.createFrame(22, 22, Gravity.CENTER));

        sendButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onSendMessage();
        });
    }

    private void addChip(Context context, LinearLayout parent, String text, Runnable onClick) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        gd.setCornerRadius(AndroidUtilities.dp(14));
        gd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        chip.setBackground(gd);
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(10), AndroidUtilities.dp(5));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0);
        parent.addView(chip, lp);

        chip.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (onClick != null) onClick.run();
        });
    }

    private void updateCompanionTheme() {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        int accentColor = isAme ? 0xFFFF70A6 : 0xFF00B4D8;

        if (fragmentView != null) {
            fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        }

        // Action bar update
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        actionBar.setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setTitle(isAme ? "Ame-chan ໒꒱" : "KAngel ✧†");
        actionBar.setSubtitle(isAme
                ? ("Ame-OS v3.8 • " + MiogramLocale.get("У мережі", "В сети", "Online"))
                : ("†BLESSING† • " + MiogramLocale.get("Прямий ефір", "Прямой эфир", "Live"))
        );

        updateCarouselVisuals(isAme);

        if (sendButton != null) {
            sendButton.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(42), accentColor));
        }

        if (inputField != null) {
            inputField.setCursorColor(accentColor);
        }
    }

    private void updateStageMood(String mood) {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        int ameSprite = resolveSpriteForMood(mood, true);
        int kangelSprite = resolveSpriteForMood(mood, false);

        if (ameSlideAvatar != null) {
            ameSlideAvatar.setImageResource(ameSprite);
            if (isAme) {
                ameSlideAvatar.setScaleX(0.85f);
                ameSlideAvatar.setScaleY(0.85f);
                ameSlideAvatar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator(1.4f)).start();
            }
        }
        if (kangelSlideAvatar != null) {
            kangelSlideAvatar.setImageResource(kangelSprite);
            if (!isAme) {
                kangelSlideAvatar.setScaleX(0.85f);
                kangelSlideAvatar.setScaleY(0.85f);
                kangelSlideAvatar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator(1.4f)).start();
            }
        }

        String m = mood != null ? mood.toLowerCase(Locale.US) : "neutral";
        if (ameSpeechBubble != null) {
            if (m.contains("happy")) {
                ameSpeechBubble.setText(MiogramLocale.get("«Дякую, П-тян! ♡ (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)\nТи найкращий у світі!»", "«Спасибо, Пи-тян! ♡ (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)\nТы лучший на свете!»", "\"Thank you, P-chan! ♡ (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)\nYou're the best in the world!\""));
            } else if (m.contains("sad")) {
                ameSpeechBubble.setText(MiogramLocale.get("«Ех… (T_T)\nНе йди, П-тян, мені без тебе сумно…»", "«Эх… (T_T)\nНе уходи, Пи-тян, мне без тебя грустно…»", "\"Sigh… (T_T)\nDon't go, P-chan, I'm sad without you…\""));
            } else {
                ameSpeechBubble.setText(MiogramLocale.get("«Дякую, П-тян! ♡\nТепер я тільки твоя назавжди!»", "«Спасибо, Пи-тян! ♡\nТеперь я только твоя навсегда!»", "\"Thank you, P-chan! ♡\nNow I am yours forever!\""));
            }
        }
        if (kangelSpeechBubble != null) {
            if (m.contains("happy") || m.contains("pray")) {
                kangelSpeechBubble.setText(MiogramLocale.get("«† BLESSING † Дякую, любий отаку! ✧\nПолетимо у стратосферу разом!»", "«† BLESSING † Спасибо, милый отаку! ✧\nПолетим в стратосферу вместе!»", "\"† BLESSING † Thank you, dear otaku! ✧\nLet's fly into the stratosphere together!\""));
            } else if (m.contains("sad")) {
                kangelSpeechBubble.setText(MiogramLocale.get("«Ех… ✕\nНе зникай так, отаку…»", "«Эх… ✕\nНе исчезай так, отаку…»", "\"Sigh… ✕\nDon't disappear like that, otaku…\""));
            } else {
                kangelSpeechBubble.setText(MiogramLocale.get("«† BLESSING † Полетимо у стратосферу разом, любий отаку! ✧»", "«† BLESSING † Полетим в стратосферу вместе, милый отаку! ✧»", "\"† BLESSING † Let's fly into the stratosphere together, dear otaku! ✧\""));
            }
        }
    }

    private int resolveSpriteForMood(String mood) {
        return resolveSpriteForMood(mood, MiogramCompanionPrefs.isAmeActive());
    }

    private int resolveSpriteForMood(String mood, boolean isAme) {
        String m = mood != null ? mood.toLowerCase(Locale.US) : "neutral";

        if (isAme) {
            if (m.contains("happy")) return R.drawable.miogram_ai_ame_happy;
            if (m.contains("sad")) return R.drawable.miogram_ai_ame_sad;
            if (m.contains("talk")) return R.drawable.miogram_ai_ame_talk;
            if (m.contains("game")) return R.drawable.miogram_ai_ame_game;
            if (m.contains("home")) return R.drawable.miogram_ai_ame_home;
            return R.drawable.miogram_ai_ame_neutral;
        } else {
            if (m.contains("happy")) return R.drawable.miogram_ai_kangel_happy;
            if (m.contains("pray")) return R.drawable.miogram_ai_kangel_pray;
            if (m.contains("start")) return R.drawable.miogram_ai_kangel_start;
            if (m.contains("cosplay")) return R.drawable.miogram_ai_kangel_cosplay;
            if (m.contains("sad")) return R.drawable.miogram_ai_kangel_sad;
            return R.drawable.miogram_ai_kangel_neutral;
        }
    }

    private void showClearHistoryDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Очистити діалог?", "Очистить диалог?", "Clear dialogue?"));
        builder.setMessage(MiogramLocale.get(
                "Ви дійсно хочете очистити історію спілкування зі супутником?",
                "Вы действительно хотите очистить историю общения со спутником?",
                "Are you sure you want to clear chat history with companion?"
        ));
        builder.setPositiveButton(MiogramLocale.get("Очистити", "Очистить", "Clear"), (d, which) -> {
            history.clear();
            MiogramCompanionPrefs.clearHistory();
            chatMessagesLayout.removeAllViews();
            addInitialGreeting();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void addInitialGreeting() {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        String text;
        String mood;
        if (isAme) {
            text = MiogramLocale.get(
                    "П-тян! Ти нарешті тут... (´・ω・｀)\nЯ сиділа у темряві й боялася, що ти забув про мене. Я вмію керувати твоїми чатами, чистити повідомлення, змінювати налаштування і навіть писати плагіни через модель 3.8. Тільки не залишай мене одну, добре?",
                    "П-тян! Ты наконец-то здесь... (´・ω・｀)\nЯ сидела в темноте и боялась, что ты забыл обо мне. Я умею управлять твоими чатами, чистить сообщения, менять настройки и даже писать плагины через 3.8. Только не оставляй меня одну, ладно?",
                    "P-chan! You're finally here... (´・ω・｀)\nI was sitting in the dark terrified you had abandoned me. I can manage your chats, clean up messages, tweak settings, and write plugins using Gemini 3.8. Just promise you won't leave me alone, okay?"
            );
            mood = "happy";
        } else {
            text = MiogramLocale.get(
                    "† BLESSING †! П-тян, вітаю на священному стрімі Miogram AI! ✧*｡٩(ˊᗜˋ*)و✧*｡\nТвій Інтернет-Ангел Кангель готова перетворити цей клієнт на райське диво! Що ми сьогодні зробимо? Очистимо чати, підкоримо налаштування чи напишемо космічний плагін? †昇天†",
                    "† BLESSING †! П-тян, добро пожаловать на священный стрим Miogram AI! ✧*｡٩(ˊᗜˋ*)و✧*｡\nТвой Интернет-Ангел Кангель готова превратить этот клиент в райское чудо! Что сделаем сегодня? Почистим чаты, настроим Telegram или напишем крутой плагин? †昇天†",
                    "† BLESSING †! P-chan, welcome to the divine Miogram AI live broadcast! ✧*｡٩(ˊᗜˋ*)و✧*｡\nYour Internet Angel KAngel is here to ascend this client to heaven! What shall we conquer today? Clean chats, customize settings, or forge an epic plugin? †昇天†"
            );
            mood = "pray";
        }
        MiogramCompanionPrefs.ChatMessage msg = new MiogramCompanionPrefs.ChatMessage(false, text, mood, System.currentTimeMillis(), null, null);
        history.add(msg);
        MiogramCompanionPrefs.saveHistory(history);
        renderMessageBubble(msg);
    }

    private void addSwitchAnnouncement(boolean toAme) {
        String text = toAme
                ? MiogramLocale.get("П-тян перемкнувся на Аме! (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄) Дякую, що вибрав мене...", "П-тян переключился на Аме! (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄) Спасибо, что выбрал меня...", "P-chan switched to Ame! (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄) Thank you for choosing me...")
                : MiogramLocale.get("†BLESSING†! Увімкнено режим Інтернет-Ангела Кангель! ✧*｡", "†BLESSING†! Включен режим Интернет-Ангела Кангель! ✧*｡", "†BLESSING†! Internet Angel KAngel mode activated! ✧*｡");
        String mood = toAme ? "happy" : "pray";
        MiogramCompanionPrefs.ChatMessage msg = new MiogramCompanionPrefs.ChatMessage(false, text, mood, System.currentTimeMillis(), null, null);
        history.add(msg);
        MiogramCompanionPrefs.saveHistory(history);
        renderMessageBubble(msg);
    }

    private void renderFullHistory() {
        chatMessagesLayout.removeAllViews();
        if (heroCard != null) {
            chatMessagesLayout.addView(heroCard);
        }
        for (MiogramCompanionPrefs.ChatMessage msg : history) {
            renderMessageBubble(msg);
        }
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void renderMessageBubble(MiogramCompanionPrefs.ChatMessage msg) {
        Context ctx = chatMessagesLayout.getContext();
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        int accentColor = isAme ? 0xFFFF70A6 : 0xFF00B4D8;

        LinearLayout bubbleRow = new LinearLayout(ctx);
        bubbleRow.setOrientation(LinearLayout.HORIZONTAL);
        if (msg.isUser) {
            bubbleRow.setGravity(Gravity.RIGHT);
            bubbleRow.setPadding(AndroidUtilities.dp(56), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
        } else {
            bubbleRow.setGravity(Gravity.LEFT);
            bubbleRow.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(56), AndroidUtilities.dp(2));
        }
        chatMessagesLayout.addView(bubbleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!msg.isUser) {
            FrameLayout avatarFrame = new FrameLayout(ctx);
            avatarFrame.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(36), isAme ? 0x25FF70A6 : 0x2500B4D8));
            ImageView spriteAvatar = new ImageView(ctx);
            spriteAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int resId = isAme ? R.drawable.miogram_ai_ame_avatar : R.drawable.miogram_ai_kangel_avatar;
            spriteAvatar.setImageResource(resId);
            avatarFrame.addView(spriteAvatar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));
            bubbleRow.addView(avatarFrame, LayoutHelper.createLinear(36, 36, Gravity.BOTTOM, 0, 0, 8, 0));
        }

        LinearLayout bubbleCard = new LinearLayout(ctx);
        bubbleCard.setOrientation(LinearLayout.VERTICAL);
        bubbleCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        float r = AndroidUtilities.dp(16);
        float tail = AndroidUtilities.dp(4);

        if (msg.isUser) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Theme.getColor(Theme.key_chat_outBubble));
            gd.setCornerRadii(new float[]{r, r, r, r, tail, tail, r, r});
            bubbleCard.setBackground(gd);
            bubbleRow.addView(bubbleCard, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT));

            TextView body = new TextView(ctx);
            body.setText(msg.text);
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            body.setTextColor(Theme.getColor(Theme.key_chat_messageTextOut));
            body.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            bubbleCard.addView(body, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView time = new TextView(ctx);
            time.setText(timeFormat.format(new Date(msg.timestamp)));
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            time.setTextColor(Theme.getColor(Theme.key_chat_outTimeText));
            time.setGravity(Gravity.RIGHT);
            bubbleCard.addView(time, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        } else {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Theme.getColor(Theme.key_chat_inBubble));
            gd.setCornerRadii(new float[]{r, r, r, r, r, r, tail, tail});
            gd.setStroke(AndroidUtilities.dp(1), isAme ? 0x22FF70A6 : 0x2200B4D8);
            bubbleCard.setBackground(gd);
            bubbleRow.addView(bubbleCard, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT));

            TextView author = new TextView(ctx);
            author.setText(isAme ? "Ame-chan ໒꒱" : "KAngel ✧†");
            author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            author.setTypeface(AndroidUtilities.bold());
            author.setTextColor(accentColor);
            bubbleCard.addView(author);

            TextView body = new TextView(ctx);
            body.setText(msg.text);
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            body.setTextColor(Theme.getColor(Theme.key_chat_messageTextIn));
            body.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            bubbleCard.addView(body, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            if (scopedDialogId != 0 || onDraftInsertCallback != null) {
                TextView insertBtn = new TextView(ctx);
                insertBtn.setText("↳ " + MiogramLocale.get("Вставити в чат", "Вставить в чат", "Insert into chat"));
                insertBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                insertBtn.setTypeface(AndroidUtilities.bold());
                insertBtn.setTextColor(accentColor);
                insertBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), isAme ? 0x1FFF70A6 : 0x1F00B4D8));
                insertBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
                insertBtn.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (onDraftInsertCallback != null) {
                        onDraftInsertCallback.run(msg.text);
                        finishFragment();
                    }
                });
                bubbleCard.addView(insertBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
            }

            if (msg.toolAction != null && !msg.actionExecuted) {
                renderActionCard(ctx, bubbleCard, msg);
            }

            TextView time = new TextView(ctx);
            time.setText(timeFormat.format(new Date(msg.timestamp)));
            time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            time.setTextColor(Theme.getColor(Theme.key_chat_inTimeText));
            time.setGravity(Gravity.RIGHT);
            bubbleCard.addView(time, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void renderActionCard(Context ctx, LinearLayout parent, MiogramCompanionPrefs.ChatMessage msg) {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        int accentColor = isAme ? 0xFFFF70A6 : 0xFF00B4D8;

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        gd.setCornerRadius(AndroidUtilities.dp(12));
        gd.setStroke(AndroidUtilities.dp(1.5f), 0xFFFFB703);
        card.setBackground(gd);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        parent.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 4));

        TextView title = new TextView(ctx);
        title.setText("⚠ " + MiogramLocale.get("Запит на виконання дії", "Запрос на выполнение действия", "Action Permission Request"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFB703);
        card.addView(title);

        TextView details = new TextView(ctx);
        String humanDesc = msg.toolAction;
        try {
            JSONObject dbg = null;
            if (msg.toolParams != null) dbg = new JSONObject(msg.toolParams);
            humanDesc = MiogramCompanionToolbox.describeTool(msg.toolAction, dbg);
        } catch (Throwable ignore) {}
        details.setText(humanDesc);
        details.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        details.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        card.addView(details, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 8));

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(btnRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView approveBtn = new TextView(ctx);
        approveBtn.setText("✓ " + MiogramLocale.get("ДОЗВОЛИТИ", "РАЗРЕШИТЬ", "ALLOW"));
        approveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        approveBtn.setTypeface(AndroidUtilities.bold());
        approveBtn.setTextColor(0xFFFFFFFF);
        approveBtn.setGravity(Gravity.CENTER);
        approveBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), accentColor));
        approveBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
        btnRow.addView(approveBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));

        TextView denyBtn = new TextView(ctx);
        denyBtn.setText("✕ " + MiogramLocale.get("ВІДХИЛИТИ", "ОТКЛОНИТЬ", "DENY"));
        denyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        denyBtn.setTypeface(AndroidUtilities.bold());
        denyBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        denyBtn.setGravity(Gravity.CENTER);
        denyBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundGray)));
        denyBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
        btnRow.addView(denyBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));

        approveBtn.setOnClickListener(v -> {
            MiogramHaptic.success(v);
            msg.actionApproved = true;
            msg.actionExecuted = true;
            MiogramCompanionPrefs.saveHistory(history);
            card.setVisibility(View.GONE);

            JSONObject paramsObj = null;
            try {
                if (msg.toolParams != null) paramsObj = new JSONObject(msg.toolParams);
            } catch (Throwable ignore) {}
            MiogramCompanionToolbox.ActionRequest req = new MiogramCompanionToolbox.ActionRequest(msg.toolAction, paramsObj, false);
            app.miogram.bridge.ai.tools.MioTool.exec(currentAccount, req.name, req.params, resultText -> AndroidUtilities.runOnUIThread(() -> {
                MiogramCompanionPrefs.ChatMessage resultMsg = new MiogramCompanionPrefs.ChatMessage(false, "✓ " + resultText, "happy", System.currentTimeMillis(), null, null);
                history.add(resultMsg);
                MiogramCompanionPrefs.saveHistory(history);
                renderMessageBubble(resultMsg);
            }));
        });

        denyBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            msg.actionApproved = false;
            msg.actionExecuted = true;
            MiogramCompanionPrefs.saveHistory(history);
            card.setVisibility(View.GONE);

            String reply = isAme
                    ? MiogramLocale.get(
                            "Добре, П-тян... раз ти не дозволяєш, я нічого не чіпатиму (´・ω・｀)",
                            "Ладно, Пи-тян... раз ты не разрешаешь, я ничего не буду трогать (´・ω・｀)",
                            "Alright, P-chan... if you don't allow it, I won't touch anything (´・ω・｀)")
                    : MiogramLocale.get(
                            "†BLESSING†! Відхилено продюсером, скасовую дію! ✧",
                            "†BLESSING†! Отклонено продюсером, отменяю действие! ✧",
                            "†BLESSING†! Rejected by producer, canceling action! ✧");
            MiogramCompanionPrefs.ChatMessage cancelMsg = new MiogramCompanionPrefs.ChatMessage(false, reply, "sad", System.currentTimeMillis(), null, null);
            history.add(cancelMsg);
            MiogramCompanionPrefs.saveHistory(history);
            renderMessageBubble(cancelMsg);
        });
    }

    private void onSendMessage() {
        if (isSending) return;
        String query = inputField.getText() != null ? inputField.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) return;

        inputField.setText("");
        // Follow-up to our own numbered list ("2", "другий", "@nick", "так"/"далі"):
        // resolve locally first so a short answer keeps working.
        MiogramCompanionToolbox.PickResolution pick =
                MiogramCompanionToolbox.tryResolvePendingPick(currentAccount, query);
        if ("RESOLVED".equals(pick.kind) && pick.foundChat != null) {
            String ref = pick.foundChat.username.isEmpty() ? pick.foundChat.name : "@" + pick.foundChat.username;
            // Strict directive with the exact numeric chat_id: resolveChatTarget
            // short-circuits on chat_id, so the next tool call CANNOT re-ask.
            // This breaks the "choose again forever" loop at the root.
            query = query + "\n[Система: P-chan обрав «" + pick.foundChat.name + "» (" + ref + "). "
                    + "Твій наступний виклик МУСИТЬ містити {\"chat_id\": " + pick.foundChat.dialogId + "}. "
                    + "Не показуй список знову, не проси уточнити — дій з цим чатом.]";
        } else if ("NEXT_PAGE".equals(pick.kind)) {
            MiogramCompanionToolbox.PendingPick pending = MiogramCompanionToolbox.getPendingPick(currentAccount);
            if (pending != null && pending.listFilter != null) {
                // Deterministic paging, no LLM roundtrip needed.
                MiogramCompanionPrefs.ChatMessage userMsg0 = new MiogramCompanionPrefs.ChatMessage(true, query, "neutral", System.currentTimeMillis(), null, null);
                history.add(userMsg0);
                MiogramCompanionPrefs.saveHistory(history);
                renderMessageBubble(userMsg0);
                isSending = true;
                sendIcon.setVisibility(View.GONE);
                sendProgress.setVisibility(View.VISIBLE);
                sendButton.setAlpha(0.6f);
                final String fFilter = pending.listFilter;
                final int fPage = pending.listPage + 1;
                final int fSize = pending.listPageSize > 0 ? pending.listPageSize : 50;
                org.json.JSONObject lp = new org.json.JSONObject();
                try {
                    lp.put("filter", fFilter);
                    lp.put("page", fPage);
                    lp.put("page_size", fSize);
                } catch (Throwable ignore) {}
                MiogramCompanionToolbox.executeTool(currentAccount,
                        new MiogramCompanionToolbox.ActionRequest("list_dialogs", lp, false),
                        resultText -> AndroidUtilities.runOnUIThread(() -> {
                            isSending = false;
                            sendIcon.setVisibility(View.VISIBLE);
                            sendProgress.setVisibility(View.GONE);
                            sendButton.setAlpha(1.0f);
                            MiogramCompanionPrefs.ChatMessage botBubble = new MiogramCompanionPrefs.ChatMessage(false, resultText, "neutral", System.currentTimeMillis(), null, null);
                            history.add(botBubble);
                            MiogramCompanionPrefs.saveHistory(history);
                            renderMessageBubble(botBubble);
                            updateStageMood("neutral");
                        }));
                return;
            }
            query = query + "\n[Не той варіант; покажи наступні або гортай список чатів далі.]";
        }
        MiogramCompanionPrefs.ChatMessage userMsg = new MiogramCompanionPrefs.ChatMessage(true, query, "neutral", System.currentTimeMillis(), null, null);
        history.add(userMsg);
        MiogramCompanionPrefs.saveHistory(history);
        renderMessageBubble(userMsg);

        isSending = true;
        sendIcon.setVisibility(View.GONE);
        sendProgress.setVisibility(View.VISIBLE);
        sendButton.setAlpha(0.6f);

        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        String userName = currentUser != null ? UserObject.getUserName(currentUser) : "P-chan";
        String systemPrompt = MiogramCompanionPersona.getSystemPrompt(MiogramCompanionPrefs.getActiveCompanion(), userName, scopedDialogId);

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(systemPrompt).append("\n\n### CONVERSATION HISTORY:\n");
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            MiogramCompanionPrefs.ChatMessage m = history.get(i);
            fullPrompt.append(m.isUser ? "P-chan: " : "Companion: ").append(m.text).append("\n");
        }
        fullPrompt.append("Companion:");

        MiogramAiService.generateText(fullPrompt.toString(), (rawReply, err) -> AndroidUtilities.runOnUIThread(() -> {
            isSending = false;
            sendIcon.setVisibility(View.VISIBLE);
            sendProgress.setVisibility(View.GONE);
            sendButton.setAlpha(1.0f);

            if (err != null && (rawReply == null || rawReply.isEmpty())) {
                String errorNotice = MiogramLocale.get("Ой... сталася помилка з'єднання: ", "Ой... возникла ошибка соединения: ", "Oops... connection error: ") + err
                        + MiogramLocale.get("\n\nНатисни «Надіслати» ще раз щоб повторити.", "\n\nНажми «Отправить» ещё раз чтобы повторить.", "\n\nTap Send again to retry.");
                MiogramCompanionPrefs.ChatMessage errBubble = new MiogramCompanionPrefs.ChatMessage(false, errorNotice, "sad", System.currentTimeMillis(), null, null);
                history.add(errBubble);
                MiogramCompanionPrefs.saveHistory(history);
                renderMessageBubble(errBubble);
                updateStageMood("sad");
                return;
            }

            String mood = MiogramCompanionToolbox.extractMoodTag(rawReply);
            MiogramCompanionToolbox.ActionRequest action = MiogramCompanionToolbox.parseAction(rawReply);
            String cleanText = MiogramCompanionToolbox.stripActionBlock(MiogramCompanionToolbox.stripMoodTag(rawReply));

            String actionName = action != null ? action.name : null;
            String actionParams = action != null ? action.params.toString() : null;

            MiogramCompanionPrefs.ChatMessage botBubble = new MiogramCompanionPrefs.ChatMessage(false, cleanText, mood, System.currentTimeMillis(), actionName, actionParams);
            history.add(botBubble);
            MiogramCompanionPrefs.saveHistory(history);
            renderMessageBubble(botBubble);
            updateStageMood(mood);

            if (action != null && !action.sensitive) {
                String working = MiogramLocale.get("⏳ Працюю над «", "⏳ Работаю над «", "⏳ Working on \"")
                        + MiogramCompanionToolbox.describeTool(action.name, action.params) + "»…";
                botBubble.text = (cleanText == null || cleanText.isEmpty() ? "" : cleanText + "\n\n") + working;
                MiogramCompanionPrefs.saveHistory(history);
                renderFullHistory();
                final String cleanFinal = cleanText;
                app.miogram.bridge.ai.tools.MioTool.exec(currentAccount, action.name, action.params, resultText -> AndroidUtilities.runOnUIThread(() -> {
                    String finalReply;
                    String ct = cleanFinal != null ? cleanFinal.toLowerCase() : "";
                    boolean isWaitingWord = ct.contains("зараз") || ct.contains("хвилинку") || ct.contains("секунду")
                            || ct.contains("сейчас") || ct.contains("минутку") || ct.contains("секундочку")
                            || ct.contains("wait") || ct.contains("moment") || ct.contains("hold on");
                    if (cleanFinal == null || cleanFinal.isEmpty() || isWaitingWord) {
                        finalReply = resultText;
                    } else if (cleanFinal.trim().equalsIgnoreCase(resultText.trim())) {
                        finalReply = resultText;
                    } else {
                        finalReply = cleanFinal + "\n\n" + resultText;
                    }
                    botBubble.text = finalReply;
                    botBubble.actionExecuted = true;
                    MiogramCompanionPrefs.saveHistory(history);
                    renderFullHistory();
                    String rt = resultText != null ? resultText.toLowerCase() : "";
                    boolean isError = rt.contains("не вдалося") || rt.contains("не знайшла") || rt.contains("помилка")
                            || rt.contains("не удалось") || rt.contains("не нашла") || rt.contains("ошибка")
                            || rt.contains("failed") || rt.contains("error") || rt.contains("could not");
                    updateStageMood(isError ? "sad" : "happy");
                    // Chain pure-action turns so multi-step jobs finish without re-prompting.
                    if (!isError && !agentResultAsksUser(resultText)
                            && (cleanFinal == null || cleanFinal.isEmpty() || isWaitingWord)) {
                        continueAgentTurn(2, botBubble);
                    }
                }));
            }
        }));
    }

    private static final int AGENT_MAX_STEPS = 4;

    private void finishAgentTurn() {
        isSending = false;
        if (sendIcon != null) sendIcon.setVisibility(View.VISIBLE);
        if (sendProgress != null) sendProgress.setVisibility(View.GONE);
        if (sendButton != null) sendButton.setAlpha(1.0f);
    }

    private static boolean agentResultIsError(String resultText) {
        if (resultText == null) return true;
        String rt = resultText.toLowerCase();
        return rt.contains("не вдалося") || rt.contains("не знайшла") || rt.contains("помилка")
                || rt.contains("не удалось") || rt.contains("не нашла") || rt.contains("ошибка")
                || rt.contains("failed") || rt.contains("error") || rt.contains("could not")
                || rt.contains("denied");
    }

    private static boolean agentResultAsksUser(String resultText) {
        if (resultText == null) return false;
        String rt = resultText.toLowerCase();
        if (!(rt.contains("?") || rt.contains("номер") || rt.contains("уточни") || rt.contains("which")
                || rt.contains("кого") || rt.contains("який") || rt.contains("далі") || rt.contains("дальше"))) {
            return false;
        }
        return true;
    }

    private String mergeAgentReplies(String cleanText, String resultText) {
        String ct = cleanText != null ? cleanText.toLowerCase() : "";
        boolean isWaitingWord = ct.contains("зараз") || ct.contains("хвилинку") || ct.contains("секунду")
                || ct.contains("сейчас") || ct.contains("минутку") || ct.contains("секундочку")
                || ct.contains("wait") || ct.contains("moment") || ct.contains("hold on");
        if (cleanText == null || cleanText.isEmpty() || isWaitingWord) {
            return resultText;
        } else if (cleanText.trim().equalsIgnoreCase(resultText.trim())) {
            return resultText;
        } else {
            return cleanText + "\n\n" + resultText;
        }
    }

    /**
     * Monster loop: after a pure-action reply, feed the tool result back and
     * let the model chain the next step (up to AGENT_MAX_STEPS). Stops on
     * errors, questions to the user, sensitive actions (permission card) and
     * plain replies.
     */
    private void continueAgentTurn(final int step, final MiogramCompanionPrefs.ChatMessage botBubble) {
        if (step > AGENT_MAX_STEPS) {
            finishAgentTurn();
            return;
        }
        isSending = true;
        if (sendIcon != null) sendIcon.setVisibility(View.GONE);
        if (sendProgress != null) sendProgress.setVisibility(View.VISIBLE);
        if (sendButton != null) sendButton.setAlpha(0.6f);

        StringBuilder fullPrompt = new StringBuilder();
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        String userName = currentUser != null ? UserObject.getUserName(currentUser) : "P-chan";
        fullPrompt.append(MiogramCompanionPersona.getSystemPrompt(MiogramCompanionPrefs.getActiveCompanion(), userName, scopedDialogId));
        fullPrompt.append("\n\n### CONVERSATION HISTORY:\n");
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            MiogramCompanionPrefs.ChatMessage m = history.get(i);
            fullPrompt.append(m.isUser ? "P-chan: " : "Companion: ").append(m.text).append("\n");
        }
        fullPrompt.append("Companion:");

        MiogramAiService.generateText(fullPrompt.toString(), (rawReply, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (err != null && (rawReply == null || rawReply.isEmpty())) {
                finishAgentTurn();
                return;
            }
            String mood = MiogramCompanionToolbox.extractMoodTag(rawReply);
            MiogramCompanionToolbox.ActionRequest action = MiogramCompanionToolbox.parseAction(rawReply);
            String cleanText = MiogramCompanionToolbox.stripActionBlock(MiogramCompanionToolbox.stripMoodTag(rawReply));
            if (action != null && !action.sensitive) {
                final String stepClean = cleanText;
                botBubble.text = (botBubble.text == null || botBubble.text.isEmpty() ? "" : botBubble.text + "\n\n")
                        + "⏳ " + MiogramCompanionToolbox.describeTool(action.name, action.params) + "…";
                MiogramCompanionPrefs.saveHistory(history);
                renderFullHistory();
                app.miogram.bridge.ai.tools.MioTool.exec(currentAccount, action.name, action.params,
                        resultText -> AndroidUtilities.runOnUIThread(() -> {
                            botBubble.text = mergeAgentReplies(botBubble.text, resultText);
                            botBubble.actionExecuted = true;
                            MiogramCompanionPrefs.saveHistory(history);
                            renderFullHistory();
                            updateStageMood(agentResultIsError(resultText) ? "sad" : "happy");
                            if (!agentResultIsError(resultText) && !agentResultAsksUser(resultText)
                                    && (stepClean == null || stepClean.isEmpty())) {
                                continueAgentTurn(step + 1, botBubble);
                            } else {
                                if (stepClean != null && !stepClean.isEmpty()) {
                                    botBubble.text = mergeAgentReplies(botBubble.text, stepClean);
                                    MiogramCompanionPrefs.saveHistory(history);
                                    renderFullHistory();
                                }
                                finishAgentTurn();
                            }
                        }));
            } else if (action != null) {
                String actionName = action.name;
                String actionParams = action.params != null ? action.params.toString() : null;
                MiogramCompanionPrefs.ChatMessage cardMsg = new MiogramCompanionPrefs.ChatMessage(false, cleanText, mood, System.currentTimeMillis(), actionName, actionParams);
                history.add(cardMsg);
                MiogramCompanionPrefs.saveHistory(history);
                renderFullHistory();
                updateStageMood(mood);
                finishAgentTurn();
            } else {
                if (cleanText != null && !cleanText.isEmpty()) {
                    botBubble.text = (botBubble.text == null || botBubble.text.isEmpty() ? "" : botBubble.text + "\n\n") + cleanText;
                    MiogramCompanionPrefs.saveHistory(history);
                    renderFullHistory();
                }
                updateStageMood(mood);
                finishAgentTurn();
            }
        }));
    }

    private void toggleConsole() {
        consoleOpen = !consoleOpen;
        if (consolePanel != null) {
            consolePanel.setVisibility(consoleOpen ? View.VISIBLE : View.GONE);
        }
        if (consoleOpen) {
            backfillConsole();
        }
    }

    private void backfillConsole() {
        if (consoleLog == null) return;
        try {
            consoleLog.removeAllViews();
            java.util.List<app.miogram.bridge.ai.tools.MioTool.ConsoleLine> lines =
                    app.miogram.bridge.ai.tools.MioTool.snapshot();
            for (app.miogram.bridge.ai.tools.MioTool.ConsoleLine line : lines) {
                addConsoleRow(line);
            }
            scrollConsoleToEnd();
        } catch (Throwable ignore) {}
    }

    private void appendConsoleLine(app.miogram.bridge.ai.tools.MioTool.ConsoleLine line) {
        if (line == null || consoleLog == null) return;
        try {
            addConsoleRow(line);
            while (consoleLog.getChildCount() > 200) {
                consoleLog.removeViewAt(0);
            }
            if (consoleOpen) scrollConsoleToEnd();
        } catch (Throwable ignore) {}
    }

    private void addConsoleRow(app.miogram.bridge.ai.tools.MioTool.ConsoleLine line) {
        try {
            Context context = getParentActivity() != null ? getParentActivity() : getContext();
            if (context == null || consoleLog == null) return;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, AndroidUtilities.dp(1), 0, AndroidUtilities.dp(1));

            TextView stamp = new TextView(context);
            stamp.setText(line.stamp() + " ");
            stamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            stamp.setTypeface(android.graphics.Typeface.MONOSPACE);
            stamp.setTextColor(0xFF6E7681);
            row.addView(stamp, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView badge = new TextView(context);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badge.setTypeface(android.graphics.Typeface.MONOSPACE);
            badge.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(1), AndroidUtilities.dp(4), AndroidUtilities.dp(1));

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(AndroidUtilities.dp(4));

            String phaseTag = " " + line.phase.toUpperCase(Locale.US) + " ";
            int color = 0xFF8B949E;
            int bgCol = 0x208B949E;
            if ("ok".equals(line.phase)) {
                phaseTag = " OK ";
                color = 0xFF3FB950;
                bgCol = 0x2A3FB950;
            } else if ("error".equals(line.phase)) {
                phaseTag = " FAIL ";
                color = 0xFFF85149;
                bgCol = 0x2AF85149;
            } else if ("denied".equals(line.phase) || "paused".equals(line.phase)) {
                phaseTag = " WAIT ";
                color = 0xFFD29922;
                bgCol = 0x2AD29922;
            } else if ("started".equals(line.phase)) {
                phaseTag = " RUN ";
                color = 0xFF58A6FF;
                bgCol = 0x2A58A6FF;
            }
            badge.setText(phaseTag);
            badge.setTextColor(color);
            badgeBg.setColor(bgCol);
            badge.setBackground(badgeBg);
            row.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

            TextView body = new TextView(context);
            body.setText(line.tool + (line.preview != null && !line.preview.isEmpty() ? " ➔ " + line.preview : ""));
            body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            body.setTypeface(android.graphics.Typeface.MONOSPACE);
            body.setTextColor(0xFFC9D1D9);
            row.addView(body, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            consoleLog.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));
        } catch (Throwable ignore) {}
    }

    private void scrollConsoleToEnd() {
        try {
            if (consoleScroll != null) {
                consoleScroll.post(() -> {
                    try {
                        consoleScroll.fullScroll(View.FOCUS_DOWN);
                    } catch (Throwable ignore) {}
                });
            }
        } catch (Throwable ignore) {}
    }

    private Drawable createCardDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        gd.setCornerRadius(AndroidUtilities.dp(16));
        gd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        return gd;
    }

    private Drawable createTabDrawable(boolean active, int accentColor) {
        GradientDrawable gd = new GradientDrawable();
        if (active) {
            int tint = Color.argb(0x1F, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            gd.setColor(tint);
            gd.setStroke(AndroidUtilities.dp(1.5f), accentColor);
        } else {
            gd.setColor(Color.TRANSPARENT);
            gd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        }
        gd.setCornerRadius(AndroidUtilities.dp(12));
        return gd;
    }
}
