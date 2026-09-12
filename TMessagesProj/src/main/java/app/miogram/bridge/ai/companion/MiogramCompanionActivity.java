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
 * - Sleek, modern Telegram Hero Card:
 *   * Dual-segment interactive companion tab switcher (Ame vs KAngel) with real-time reactive avatars
 *   * Character stage featuring smooth mood-reaction animation and model badges
 *   * NSO stats bar (Day, Followers, Stress, Affection, Darkness) styled as native Telegram chips
 * - Authentic Telegram message bubbles with companion mood sprites and timestamping
 * - Integrated action permission cards for client tool executions
 * - Telegram-style composer with quick prompt chips and circular send button
 */
public class MiogramCompanionActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private long scopedDialogId = 0;
    private Utilities.Callback<String> onDraftInsertCallback;

    private LinearLayout heroCard;
    private ImageView stageAvatar;
    private TextView stageName;
    private TextView stageStatus;
    private TextView stageMoodBadge;
    private TextView modelBadge;

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
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme);
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

        // 1. Scrollable Chat History Layout
        chatScrollView = new ScrollView(context);
        chatScrollView.setFillViewport(true);
        mainColumn.addView(chatScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        chatMessagesLayout = new LinearLayout(context);
        chatMessagesLayout.setOrientation(LinearLayout.VERTICAL);
        chatMessagesLayout.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        chatScrollView.addView(chatMessagesLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 2. Hero NSO Stage (Pinned at top of messages, scrolls naturally with chat)
        buildHeroCard(context, chatMessagesLayout);

        // 3. Quick Prompts & Composer Bar
        buildComposer(context, mainColumn);

        // Load persisted history
        history.addAll(MiogramCompanionPrefs.loadHistory());
        if (history.isEmpty()) {
            addInitialGreeting();
        } else {
            renderFullHistory();
        }

        updateCompanionTheme();

        if (!MiogramCompanionPrefs.hasCompletedOnboarding()) {
            showOnboardingSelection(context, root);
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
                    if (fragmentView instanceof FrameLayout) {
                        showOnboardingSelection(getParentActivity(), (FrameLayout) fragmentView);
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        headerItem.addSubItem(1, R.drawable.msg_clear, MiogramLocale.get("Очистити діалог", "Очистить диалог", "Clear dialogue"));
        headerItem.addSubItem(2, R.drawable.msg_log, MiogramLocale.get("Звіт про баг (@dkramochka)", "Отчет о баге (@dkramochka)", "Report bug (@dkramochka)"));
        headerItem.addSubItem(3, R.drawable.msg_theme, MiogramLocale.get("Переобрати супутника (Аме / Кангель)", "Перевыбрать спутника (Аме / Кангель)", "Choose companion again (Ame / KAngel)"));
    }

    private void buildHeroCard(Context context, LinearLayout parent) {
        heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        LinearLayout.LayoutParams cardLp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 4, 10);
        parent.addView(heroCard, cardLp);

        // Character Sprite (Prominent, centered, 160 x 96 dp)
        stageAvatar = new ImageView(context);
        stageAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        heroCard.addView(stageAvatar, LayoutHelper.createLinear(160, 96, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        stageName = new TextView(context);
        stageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        stageName.setTypeface(AndroidUtilities.bold());
        stageName.setGravity(Gravity.CENTER);
        heroCard.addView(stageName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        stageStatus = new TextView(context);
        stageStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        stageStatus.setGravity(Gravity.CENTER);
        heroCard.addView(stageStatus, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Badges row: Mood pill + Model pill
        LinearLayout badgeRow = new LinearLayout(context);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER);
        heroCard.addView(badgeRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        stageMoodBadge = new TextView(context);
        stageMoodBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        stageMoodBadge.setTypeface(AndroidUtilities.bold());
        stageMoodBadge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        badgeRow.addView(stageMoodBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        modelBadge = new TextView(context);
        modelBadge.setText("⚡ 3.5 Flash Lite");
        modelBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        modelBadge.setTypeface(AndroidUtilities.bold());
        modelBadge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        badgeRow.addView(modelBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // NSO Stats Row
        LinearLayout statsBar = new LinearLayout(context);
        statsBar.setOrientation(LinearLayout.HORIZONTAL);
        statsBar.setGravity(Gravity.CENTER);
        heroCard.addView(statsBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        statDay = createStatChip(context, "DAY 24", 0xFF6C5CE7);
        statsBar.addView(statDay, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        statFollowers = createStatChip(context, "FOL 1.3M", 0xFF00B4D8);
        statsBar.addView(statFollowers, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.2f, 0, 0, 3, 0));

        statStress = createStatChip(context, "STR " + MiogramCompanionPrefs.getStress() + "%", 0xFFE84393);
        statsBar.addView(statStress, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 3, 0));

        statAffection = createStatChip(context, "LOVE ♡ " + MiogramCompanionPrefs.getAffection() + "%", 0xFFFF70A6);
        statsBar.addView(statAffection, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.2f, 0, 0, 3, 0));

        statDarkness = createStatChip(context, "DARK " + MiogramCompanionPrefs.getDarkness() + "%", 0xFF636E72);
        statsBar.addView(statDarkness, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
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

    private void showOnboardingSelection(Context context, FrameLayout root) {
        if (context == null || root == null) return;

        FrameLayout overlay = new FrameLayout(context);
        overlay.setBackgroundColor(0xF20D0818);
        overlay.setElevation(AndroidUtilities.dp(20));
        overlay.setClickable(true);

        LinearLayout contentCol = new LinearLayout(context);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        contentCol.setGravity(Gravity.CENTER_HORIZONTAL);
        overlay.addView(contentCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 14, 20, 14, 20));

        TextView topTitle = new TextView(context);
        topTitle.setText("NEEDY STREAMER OVERLOAD ໒꒱");
        topTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        topTitle.setTypeface(AndroidUtilities.bold());
        topTitle.setTextColor(0xFFFF70A6);
        topTitle.setGravity(Gravity.CENTER);
        contentCol.addView(topTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 3));

        TextView topSub = new TextView(context);
        topSub.setText(MiogramLocale.get("Обери свою ШІ Супутницю для Telegram", "Выбери свою ИИ Спутницу для Telegram", "Choose your AI Companion for Telegram"));
        topSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        topSub.setTextColor(0xCCFFFFFF);
        topSub.setGravity(Gravity.CENTER);
        contentCol.addView(topSub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Horizontal Split layout (Left: Ame, Right: KAngel)
        LinearLayout splitLayout = new LinearLayout(context);
        splitLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentCol.addView(splitLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        // --- Left Side: Ame ---
        FrameLayout ameSide = new FrameLayout(context);
        splitLayout.addView(ameSide, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 0, 0, 5, 0));

        GradientDrawable ameBg = new GradientDrawable();
        ameBg.setColor(0x442B1038);
        ameBg.setCornerRadius(AndroidUtilities.dp(16));
        ameBg.setStroke(AndroidUtilities.dp(2), 0x88FF70A6);
        ameSide.setBackground(ameBg);
        ameSide.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12));

        LinearLayout ameCol = new LinearLayout(context);
        ameCol.setOrientation(LinearLayout.VERTICAL);
        ameCol.setGravity(Gravity.CENTER_HORIZONTAL);
        ameSide.addView(ameCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        TextView ameTag = new TextView(context);
        ameTag.setText("໒꒱ AME-CHAN");
        ameTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ameTag.setTypeface(AndroidUtilities.bold());
        ameTag.setTextColor(0xFFFF70A6);
        ameCol.addView(ameTag, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 6));

        ImageView ameImg = new ImageView(context);
        ameImg.setImageResource(R.drawable.miogram_ai_ame_neutral);
        ameImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ameCol.addView(ameImg, LayoutHelper.createLinear(130, 130, 0, 4, 0, 10));

        TextView ameSpeech = new TextView(context);
        ameSpeech.setText("П-тян... обери мене... (´・ω・｀)");
        ameSpeech.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        ameSpeech.setTextColor(0xEEFFFFFF);
        ameSpeech.setGravity(Gravity.CENTER);
        GradientDrawable bubbleAme = new GradientDrawable();
        bubbleAme.setColor(0x66000000);
        bubbleAme.setCornerRadius(AndroidUtilities.dp(12));
        bubbleAme.setStroke(AndroidUtilities.dp(1), 0x55FF70A6);
        ameSpeech.setBackground(bubbleAme);
        ameSpeech.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        ameCol.addView(ameSpeech, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView ameDesc = new TextView(context);
        ameDesc.setText("Нервова отаку-вайфу.\nПрив'язана, вразлива, щира ♡");
        ameDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        ameDesc.setTextColor(0xAAFFFFFF);
        ameDesc.setGravity(Gravity.CENTER);
        ameCol.addView(ameDesc, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // --- Right Side: KAngel ---
        FrameLayout kangelSide = new FrameLayout(context);
        splitLayout.addView(kangelSide, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 5, 0, 0, 0));

        GradientDrawable kangelBg = new GradientDrawable();
        kangelBg.setColor(0x44082538);
        kangelBg.setCornerRadius(AndroidUtilities.dp(16));
        kangelBg.setStroke(AndroidUtilities.dp(2), 0x8800B4D8);
        kangelSide.setBackground(kangelBg);
        kangelSide.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12));

        LinearLayout kangelCol = new LinearLayout(context);
        kangelCol.setOrientation(LinearLayout.VERTICAL);
        kangelCol.setGravity(Gravity.CENTER_HORIZONTAL);
        kangelSide.addView(kangelCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        TextView kangelTag = new TextView(context);
        kangelTag.setText("✧ KANGEL †");
        kangelTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        kangelTag.setTypeface(AndroidUtilities.bold());
        kangelTag.setTextColor(0xFF00B4D8);
        kangelCol.addView(kangelTag, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 6));

        ImageView kangelImg = new ImageView(context);
        kangelImg.setImageResource(R.drawable.miogram_ai_kangel_neutral);
        kangelImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        kangelCol.addView(kangelImg, LayoutHelper.createLinear(130, 130, 0, 4, 0, 10));

        TextView kangelSpeech = new TextView(context);
        kangelSpeech.setText("Полетимо у стратосферу! ✧");
        kangelSpeech.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        kangelSpeech.setTextColor(0xEEFFFFFF);
        kangelSpeech.setGravity(Gravity.CENTER);
        GradientDrawable bubbleKangel = new GradientDrawable();
        bubbleKangel.setColor(0x66000000);
        bubbleKangel.setCornerRadius(AndroidUtilities.dp(12));
        bubbleKangel.setStroke(AndroidUtilities.dp(1), 0x5500B4D8);
        kangelSpeech.setBackground(bubbleKangel);
        kangelSpeech.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        kangelCol.addView(kangelSpeech, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView kangelDesc = new TextView(context);
        kangelDesc.setText("Інтернет-Ангел №1.\n†BLESSING†, стріми, ейфорія!");
        kangelDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        kangelDesc.setTextColor(0xAAFFFFFF);
        kangelDesc.setGravity(Gravity.CENTER);
        kangelCol.addView(kangelDesc, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // Click actions with 3-second scene (chosen smiles, rejected curses)
        ameSide.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            ameSide.setEnabled(false);
            kangelSide.setEnabled(false);

            // Ame chosen -> smiles and bounces
            ameImg.setImageResource(R.drawable.miogram_ai_ame_happy);
            ameImg.animate().scaleX(1.22f).scaleY(1.22f).setDuration(350).setInterpolator(new OvershootInterpolator()).start();
            ameSpeech.setText("Дякую, П-тян! ♡ (⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)\nТепер я тільки твоя назавжди!");
            bubbleAme.setColor(0xDDFF70A6);
            ameSpeech.setTextColor(0xFFFFFFFF);

            // KAngel rejected -> curses ("пішов нахуй") and shakes angrily
            kangelImg.setImageResource(R.drawable.miogram_ai_kangel_sad);
            ObjectAnimator shake = ObjectAnimator.ofFloat(kangelImg, "translationX", 0, -22, 22, -18, 18, -10, 10, 0);
            shake.setDuration(500);
            shake.setRepeatCount(3);
            shake.start();
            kangelSpeech.setText("Та пішов ти нахуй! ✕\nПожалкуєш ще, отаку-невдахо!");
            bubbleKangel.setColor(0xEE4A0A18);
            kangelSpeech.setTextColor(0xFFFF5252);

            ameSide.postDelayed(() -> {
                MiogramCompanionPrefs.setActiveCompanion(MiogramCompanionPrefs.COMPANION_AME);
                MiogramCompanionPrefs.setOnboardingCompleted(true);
                overlay.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                    root.removeView(overlay);
                    updateCompanionTheme();
                    if (history.isEmpty()) {
                        addInitialGreeting();
                    } else {
                        renderFullHistory();
                    }
                }).start();
            }, 3000L);
        });

        kangelSide.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            ameSide.setEnabled(false);
            kangelSide.setEnabled(false);

            // KAngel chosen -> smiles and bounces
            kangelImg.setImageResource(R.drawable.miogram_ai_kangel_happy);
            kangelImg.animate().scaleX(1.22f).scaleY(1.22f).setDuration(350).setInterpolator(new OvershootInterpolator()).start();
            kangelSpeech.setText("† BLESSING † Дякую, любий отаку! ✧\nПолетимо у стратосферу разом!");
            bubbleKangel.setColor(0xDD00B4D8);
            kangelSpeech.setTextColor(0xFFFFFFFF);

            // Ame rejected -> curses ("пішов нахуй") and shakes angrily
            ameImg.setImageResource(R.drawable.miogram_ai_ame_sad);
            ObjectAnimator shake = ObjectAnimator.ofFloat(ameImg, "translationX", 0, -22, 22, -18, 18, -10, 10, 0);
            shake.setDuration(500);
            shake.setRepeatCount(3);
            shake.start();
            ameSpeech.setText("Пішов нахуй... (T_T)\nЗрадник їбаний, я так і знала...");
            bubbleAme.setColor(0xEE4A0A18);
            ameSpeech.setTextColor(0xFFFF5252);

            kangelSide.postDelayed(() -> {
                MiogramCompanionPrefs.setActiveCompanion(MiogramCompanionPrefs.COMPANION_KANGEL);
                MiogramCompanionPrefs.setOnboardingCompleted(true);
                overlay.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                    root.removeView(overlay);
                    updateCompanionTheme();
                    if (history.isEmpty()) {
                        addInitialGreeting();
                    } else {
                        renderFullHistory();
                    }
                }).start();
            }, 3000L);
        });

        root.addView(overlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
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
            inputField.setText("Пі-тян, перевір затримку через юзербот команду ping");
            onSendMessage();
        });

        addChip(context, chipsRow, "📦 " + MiogramLocale.get("Мої плагіни", "Мои плагины", "My Plugins"), () -> {
            inputField.setText("Аме, покажи список встановлених плагінів MioHook");
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

        if (heroCard != null) {
            heroCard.setBackground(createCardDrawable());
        }

        if (isAme) {
            if (stageAvatar != null) {
                stageAvatar.setImageResource(R.drawable.miogram_ai_ame_neutral);
                stageName.setText("Ame-chan (飴ちゃん) ໒꒱");
                stageName.setTextColor(0xFFFF70A6);
                stageStatus.setText("ROOM: DARK // NEEDY STREAMER OVERLOAD");
                stageStatus.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));

                stageMoodBadge.setText("♥ MOOD: NEUTRAL");
                stageMoodBadge.setTextColor(0xFFFF70A6);
                stageMoodBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), 0x22FF70A6));
            }
        } else {
            if (stageAvatar != null) {
                stageAvatar.setImageResource(R.drawable.miogram_ai_kangel_neutral);
                stageName.setText("OMGkawaiiAngel-chan ✧†");
                stageName.setTextColor(0xFF00B4D8);
                stageStatus.setText("LIVE BROADCAST // † 昇天 ✧ BLESSING †");
                stageStatus.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));

                stageMoodBadge.setText("† MOOD: ANGELIC PRAY");
                stageMoodBadge.setTextColor(0xFF00B4D8);
                stageMoodBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), 0x2200B4D8));
            }
        }

        if (modelBadge != null) {
            modelBadge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            modelBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_windowBackgroundGray)));
        }

        if (sendButton != null) {
            sendButton.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(42), accentColor));
        }

        if (inputField != null) {
            inputField.setCursorColor(accentColor);
        }
    }

    private void updateStageMood(String mood) {
        if (stageAvatar == null) return;
        int spriteRes = resolveSpriteForMood(mood);
        stageAvatar.setImageResource(spriteRes);
        stageAvatar.setScaleX(0.82f);
        stageAvatar.setScaleY(0.82f);
        stageAvatar.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        if (stageMoodBadge != null) {
            boolean isAme = MiogramCompanionPrefs.isAmeActive();
            String moodText = mood != null ? mood.toUpperCase(Locale.US) : "NEUTRAL";
            stageMoodBadge.setText((isAme ? "♥ MOOD: " : "† MOOD: ") + moodText);
        }
    }

    private int resolveSpriteForMood(String mood) {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
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
        details.setText(MiogramLocale.get("Дія: ", "Действие: ", "Action: ") + msg.toolAction + "\n" + MiogramLocale.get("Параметри: ", "Параметры: ", "Params: ") + msg.toolParams);
        details.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
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
            MiogramCompanionToolbox.executeTool(currentAccount, req, resultText -> AndroidUtilities.runOnUIThread(() -> {
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
                    ? "Добре, П-тян... раз ти не дозволяєш, я нічого не чіпатиму (´・ω・｀)"
                    : "†BLESSING†! Відхилено продюсером, скасовую дію! ✧";
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

        boolean isPluginCoding = query.toLowerCase().contains("плагін") || query.toLowerCase().contains("plugin") || query.toLowerCase().contains("код");
        if (isPluginCoding) {
            modelBadge.setText("⚡ 3.8 Flash (Coding)");
        } else {
            modelBadge.setText("⚡ 3.5 Flash Lite");
        }

        MiogramAiService.generateText(fullPrompt.toString(), (rawReply, err) -> AndroidUtilities.runOnUIThread(() -> {
            isSending = false;
            sendIcon.setVisibility(View.VISIBLE);
            sendProgress.setVisibility(View.GONE);
            sendButton.setAlpha(1.0f);

            if (err != null && (rawReply == null || rawReply.isEmpty())) {
                String errorNotice = MiogramLocale.get("Ой... сталася помилка з'єднання: ", "Ой... возникла ошибка соединения: ", "Oops... connection error: ") + err;
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
                MiogramCompanionToolbox.executeTool(currentAccount, action, resultText -> AndroidUtilities.runOnUIThread(() -> {
                    String finalReply;
                    if (cleanText == null || cleanText.isEmpty() || cleanText.contains("Зараз") || cleanText.contains("хвилинку") || cleanText.contains("секунду")) {
                        finalReply = resultText;
                    } else if (cleanText.trim().equalsIgnoreCase(resultText.trim())) {
                        finalReply = resultText;
                    } else {
                        finalReply = cleanText + "\n\n" + resultText;
                    }
                    botBubble.text = finalReply;
                    botBubble.actionExecuted = true;
                    MiogramCompanionPrefs.saveHistory(history);
                    renderFullHistory();
                    boolean isError = resultText.contains("Не вдалося") || resultText.contains("Не знайшла") || resultText.contains("Помилка");
                    updateStageMood(isError ? "sad" : "happy");
                }));
            }
        }));
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
