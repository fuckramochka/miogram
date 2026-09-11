package app.miogram.bridge.ai.companion;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Needy Streamer Overload Cyberpunk / Vaporwave Companion UI.
 * Features:
 * - Authentic Windows 95/98 retro anime aesthetic (beveled title bars, pixel cards)
 * - NSO Top Status Bar: Day, Followers, Stress, Affection, Darkness
 * - Reactive Companion Switcher (Ame vs KAngel) with real-time mood-changing sprites
 * - Dynamic mood-reactive dialogue with Gemini 3.5 Flash Lite & Gemini 3.8 Flash plugin engine
 * - Interactive Permission / Access Request Cards for sensitive client actions
 */
public class MiogramCompanionActivity extends BaseFragment {

    private long scopedDialogId = 0;
    private Utilities.Callback<String> onDraftInsertCallback;

    private FrameLayout stageCard;
    private ImageView stageAvatar;
    private TextView stageName;
    private TextView stageStatus;
    private TextView stageMoodBadge;

    private ImageView ameSelectCardAvatar;
    private ImageView kangelSelectCardAvatar;
    private FrameLayout ameSelectCard;
    private FrameLayout kangelSelectCard;

    private TextView statDay;
    private TextView statFollowers;
    private TextView statStress;
    private TextView statAffection;
    private TextView statDarkness;

    private ScrollView chatScrollView;
    private LinearLayout chatMessagesLayout;
    private EditTextBoldCursor inputField;
    private View sendButton;
    private ProgressBar sendProgress;
    private TextView modelBadge;

    private final List<MiogramCompanionPrefs.ChatMessage> history = new ArrayList<>();
    private boolean isSending = false;

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
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("ШІ Супутник ໒꒱", "ИИ Спутник ໒꒱", "AI Companion ໒꒱"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        fragmentView = root;
        // Cyberpunk retro dark purple background
        root.setBackgroundColor(0xFF0F081D);

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        root.addView(mainColumn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 1. Retro Title Bar & NSO Stats
        buildNsoHeader(context, mainColumn);

        // 2. Companion Selector Bar with Reactive Avatars
        buildCompanionSelector(context, mainColumn);

        // 2.5 Live NSO Character Stage
        buildCharacterStage(context, mainColumn);

        // 3. Scrollable Chat History Layout
        chatScrollView = new ScrollView(context);
        chatScrollView.setFillViewport(true);
        mainColumn.addView(chatScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        chatMessagesLayout = new LinearLayout(context);
        chatMessagesLayout.setOrientation(LinearLayout.VERTICAL);
        chatMessagesLayout.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        chatScrollView.addView(chatMessagesLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

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
        return fragmentView;
    }

    private void buildNsoHeader(Context context, LinearLayout parent) {
        // Top Retro Windows Bar
        LinearLayout retroBar = new LinearLayout(context);
        retroBar.setOrientation(LinearLayout.HORIZONTAL);
        retroBar.setBackground(createBevelDrawable(0xFF24143D, 0xFF7952B3, 0xFF130724));
        retroBar.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        retroBar.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(retroBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView osLabel = new TextView(context);
        osLabel.setText("★ AME-OS // MIOGRAM AI v3.8");
        osLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        osLabel.setTypeface(AndroidUtilities.bold());
        osLabel.setTextColor(0xFFFF70A6);
        retroBar.addView(osLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        modelBadge = new TextView(context);
        modelBadge.setText("⚡ 3.5 Flash Lite");
        modelBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        modelBadge.setTypeface(AndroidUtilities.bold());
        modelBadge.setTextColor(0xFF00F0FF);
        modelBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x3300F0FF));
        modelBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        retroBar.addView(modelBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

        // Window Controls: Clear, Minimize, Maximize, Close
        LinearLayout winControls = new LinearLayout(context);
        winControls.setOrientation(LinearLayout.HORIZONTAL);
        winControls.setGravity(Gravity.CENTER_VERTICAL);
        retroBar.addView(winControls, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView btnClear = createRetroWinButton(context, "🗑");
        btnClear.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showClearHistoryDialog();
        });
        winControls.addView(btnClear);

        TextView btnMin = createRetroWinButton(context, "_");
        btnMin.setOnClickListener(v -> MiogramHaptic.tap(v));
        winControls.addView(btnMin);

        TextView btnMax = createRetroWinButton(context, "□");
        btnMax.setOnClickListener(v -> MiogramHaptic.tap(v));
        winControls.addView(btnMax);

        TextView btnClose = createRetroWinButton(context, "✕");
        btnClose.setOnClickListener(v -> finishFragment());
        winControls.addView(btnClose);

        // NSO Game Stats Bar: Day, Followers, Stress, Affection, Darkness
        LinearLayout statsBar = new LinearLayout(context);
        statsBar.setOrientation(LinearLayout.HORIZONTAL);
        statsBar.setBackgroundColor(0xFF190C30);
        statsBar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        statsBar.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(statsBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        statDay = createStatItem(context, statsBar, "DAY", "24", 0xFFFFD166);
        statFollowers = createStatItem(context, statsBar, "FOL", "1.3M", 0xFF00F0FF);
        statStress = createStatItem(context, statsBar, "STR", MiogramCompanionPrefs.getStress() + "%", 0xFFFF5C8A);
        statAffection = createStatItem(context, statsBar, "LOVE", "♡ " + MiogramCompanionPrefs.getAffection() + "%", 0xFFFF70A6);
        statDarkness = createStatItem(context, statsBar, "DARK", MiogramCompanionPrefs.getDarkness() + "%", 0xFFB388FF);
    }

    private TextView createStatItem(Context context, LinearLayout parent, String label, String value, int color) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        parent.addView(item, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView lbl = new TextView(context);
        lbl.setText(label);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        lbl.setTextColor(0x88FFFFFF);
        item.addView(lbl);

        TextView val = new TextView(context);
        val.setText(value);
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        val.setTypeface(AndroidUtilities.bold());
        val.setTextColor(color);
        item.addView(val);

        return val;
    }

    private void buildCompanionSelector(Context context, LinearLayout parent) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        row.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 1. Ame Card
        ameSelectCard = new FrameLayout(context);
        row.addView(ameSelectCard, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));

        LinearLayout ameContent = new LinearLayout(context);
        ameContent.setOrientation(LinearLayout.HORIZONTAL);
        ameContent.setGravity(Gravity.CENTER_VERTICAL);
        ameContent.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        ameSelectCard.addView(ameContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ameSelectCardAvatar = new ImageView(context);
        ameSelectCardAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ameContent.addView(ameSelectCardAvatar, LayoutHelper.createLinear(44, 44, 0, 0, 8, 0));

        LinearLayout ameTextCol = new LinearLayout(context);
        ameTextCol.setOrientation(LinearLayout.VERTICAL);
        ameContent.addView(ameTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView ameTitle = new TextView(context);
        ameTitle.setText("Ame-chan ໒꒱");
        ameTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ameTitle.setTypeface(AndroidUtilities.bold());
        ameTitle.setTextColor(0xFFFF70A6);
        ameTextCol.addView(ameTitle);

        TextView ameSub = new TextView(context);
        ameSub.setText(MiogramLocale.get("Отаку-дівчина • П-тян", "Отаку-девушка • П-тян", "Otaku Girl • P-chan"));
        ameSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ameSub.setTextColor(0xAAFFFFFF);
        ameTextCol.addView(ameSub);

        ameSelectCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            MiogramCompanionPrefs.setActiveCompanion(MiogramCompanionPrefs.COMPANION_AME);
            updateCompanionTheme();
            addSwitchAnnouncement(true);
        });

        // 2. KAngel Card
        kangelSelectCard = new FrameLayout(context);
        row.addView(kangelSelectCard, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));

        LinearLayout kangelContent = new LinearLayout(context);
        kangelContent.setOrientation(LinearLayout.HORIZONTAL);
        kangelContent.setGravity(Gravity.CENTER_VERTICAL);
        kangelContent.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        kangelSelectCard.addView(kangelContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        kangelSelectCardAvatar = new ImageView(context);
        kangelSelectCardAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        kangelContent.addView(kangelSelectCardAvatar, LayoutHelper.createLinear(44, 44, 0, 0, 8, 0));

        LinearLayout kangelTextCol = new LinearLayout(context);
        kangelTextCol.setOrientation(LinearLayout.VERTICAL);
        kangelContent.addView(kangelTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView kangelTitle = new TextView(context);
        kangelTitle.setText("KAngel ✧†");
        kangelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        kangelTitle.setTypeface(AndroidUtilities.bold());
        kangelTitle.setTextColor(0xFF00F0FF);
        kangelTextCol.addView(kangelTitle);

        TextView kangelSub = new TextView(context);
        kangelSub.setText(MiogramLocale.get("Інтернет-Ангел • †BLESSING†", "Интернет-Ангел • †BLESSING†", "Internet Angel • †BLESSING†"));
        kangelSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        kangelSub.setTextColor(0xAAFFFFFF);
        kangelTextCol.addView(kangelSub);

        kangelSelectCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            MiogramCompanionPrefs.setActiveCompanion(MiogramCompanionPrefs.COMPANION_KANGEL);
            updateCompanionTheme();
            addSwitchAnnouncement(false);
        });
    }

    private TextView createRetroWinButton(Context context, String symbol) {
        TextView btn = new TextView(context);
        btn.setText(symbol);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(0xFFE0D0F0);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(createBevelDrawable(0xFF331D54, 0xFF7A4EB5, 0xFF1C0D30));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(22, 22, 0, 0, 3, 0);
        btn.setLayoutParams(lp);
        return btn;
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

    private void buildCharacterStage(Context context, LinearLayout parent) {
        stageCard = new FrameLayout(context);
        stageCard.setBackground(createBevelDrawable(0xFF160B29, 0xFF4A2574, 0xFF0B0414));
        stageCard.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 10, 0, 10, 4);
        parent.addView(stageCard, lp);

        LinearLayout stageContent = new LinearLayout(context);
        stageContent.setOrientation(LinearLayout.HORIZONTAL);
        stageContent.setGravity(Gravity.CENTER_VERTICAL);
        stageCard.addView(stageContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        stageAvatar = new ImageView(context);
        stageAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stageContent.addView(stageAvatar, LayoutHelper.createLinear(64, 64, 0, 0, 10, 0));

        LinearLayout stageInfo = new LinearLayout(context);
        stageInfo.setOrientation(LinearLayout.VERTICAL);
        stageContent.addView(stageInfo, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        stageName = new TextView(context);
        stageName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        stageName.setTypeface(AndroidUtilities.bold());
        stageInfo.addView(stageName);

        stageStatus = new TextView(context);
        stageStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        stageStatus.setTextColor(0xAAFFFFFF);
        stageInfo.addView(stageStatus);

        stageMoodBadge = new TextView(context);
        stageMoodBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        stageMoodBadge.setTypeface(AndroidUtilities.bold());
        stageMoodBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        LinearLayout.LayoutParams badgeLp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0);
        stageInfo.addView(stageMoodBadge, badgeLp);
    }

    private void updateStageMood(String mood) {
        if (stageAvatar == null) return;
        int spriteRes = resolveSpriteForMood(mood);
        stageAvatar.setImageResource(spriteRes);
        stageAvatar.setScaleX(0.82f);
        stageAvatar.setScaleY(0.82f);
        stageAvatar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(280).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        if (stageMoodBadge != null) {
            String moodText = mood != null ? mood.toUpperCase(java.util.Locale.US) : "NEUTRAL";
            stageMoodBadge.setText((MiogramCompanionPrefs.isAmeActive() ? "♥ MOOD: " : "† MOOD: ") + moodText);
        }
    }

    private void updateCompanionTheme() {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();

        if (isAme) {
            // Ame is chosen: Ame is HAPPY, KAngel is SAD!
            ameSelectCardAvatar.setImageResource(R.drawable.miogram_ai_ame_happy);
            kangelSelectCardAvatar.setImageResource(R.drawable.miogram_ai_kangel_sad);

            ameSelectCard.setBackground(createBevelDrawable(0xFF3B155B, 0xFFFF70A6, 0xFF240A38));
            kangelSelectCard.setBackground(createBevelDrawable(0xFF1B112B, 0x3300F0FF, 0xFF0E071A));

            ameSelectCard.setAlpha(1.0f);
            kangelSelectCard.setAlpha(0.65f);

            if (stageCard != null) {
                stageCard.setBackground(createBevelDrawable(0xFF2B1044, 0xFFFF70A6, 0xFF140620));
                stageAvatar.setImageResource(R.drawable.miogram_ai_ame_neutral);
                stageName.setText("Ame-chan (飴ちゃん) ໒꒱");
                stageName.setTextColor(0xFFFF70A6);
                stageStatus.setText("ROOM: DARK // NEEDY STREAMER OVERLOAD");
                stageMoodBadge.setText("♥ MOOD: NEUTRAL");
                stageMoodBadge.setTextColor(0xFFFF70A6);
                stageMoodBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x33FF70A6));
            }
        } else {
            // KAngel is chosen: KAngel is HAPPY, Ame is SAD!
            ameSelectCardAvatar.setImageResource(R.drawable.miogram_ai_ame_sad);
            kangelSelectCardAvatar.setImageResource(R.drawable.miogram_ai_kangel_happy);

            ameSelectCard.setBackground(createBevelDrawable(0xFF1B112B, 0x33FF70A6, 0xFF0E071A));
            kangelSelectCard.setBackground(createBevelDrawable(0xFF0E384D, 0xFF00F0FF, 0xFF061E2B));

            ameSelectCard.setAlpha(0.65f);
            kangelSelectCard.setAlpha(1.0f);

            if (stageCard != null) {
                stageCard.setBackground(createBevelDrawable(0xFF0C2B38, 0xFF00F0FF, 0xFF04151C));
                stageAvatar.setImageResource(R.drawable.miogram_ai_kangel_neutral);
                stageName.setText("OMGkawaiiAngel-chan ✧†");
                stageName.setTextColor(0xFF00F0FF);
                stageStatus.setText("LIVE BROADCAST // † 昇天 ✧ BLESSING †");
                stageMoodBadge.setText("† MOOD: ANGELIC PRAY");
                stageMoodBadge.setTextColor(0xFF00F0FF);
                stageMoodBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x3300F0FF));
            }
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
            inputField.setText(MiogramLocale.get("У мене сталася помилка, надішли звіт із логами творцю @dkramochka", "У меня произошла ошибка, отправь отчет с логами создателю @dkramochka", "An error occurred, send a report with logs to creator @dkramochka"));
            onSendMessage();
        });

        // Retro Input Bar
        LinearLayout composerBar = new LinearLayout(context);
        composerBar.setOrientation(LinearLayout.HORIZONTAL);
        composerBar.setBackgroundColor(0xFF170C2D);
        composerBar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        composerBar.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(composerBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout inputWrapper = new FrameLayout(context);
        inputWrapper.setBackground(createBevelDrawable(0xFF100722, 0x44FF70A6, 0xFF05010B));
        composerBar.addView(inputWrapper, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 8, 0));

        inputField = new EditTextBoldCursor(context);
        inputField.setHint(MiogramLocale.get("Напишіть щось для П-тян…", "Напишите что-нибудь для П-тян…", "Message P-chan…"));
        inputField.setHintTextColor(0x66FFFFFF);
        inputField.setTextColor(0xFFFFFFFF);
        inputField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        inputField.setBackground(null);
        inputField.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        inputField.setMaxLines(4);
        inputWrapper.addView(inputField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Retro Send Button with Pink/Cyan Neon Bevel
        sendButton = new FrameLayout(context);
        sendButton.setBackground(createBevelDrawable(0xFFFF70A6, 0xFFFFFFFF, 0xFFB3306B));
        sendButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        composerBar.addView(sendButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView sendLabel = new TextView(context);
        sendLabel.setText("♡ SEND");
        sendLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sendLabel.setTypeface(AndroidUtilities.bold());
        sendLabel.setTextColor(0xFF1A072E);
        ((FrameLayout) sendButton).addView(sendLabel, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        sendProgress = new ProgressBar(context);
        sendProgress.setVisibility(View.GONE);
        ((FrameLayout) sendButton).addView(sendProgress, LayoutHelper.createFrame(20, 20, Gravity.CENTER));

        sendButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onSendMessage();
        });
    }

    private void addChip(Context context, LinearLayout parent, String text, Runnable onClick) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setTextColor(0xCCFFFFFF);
        chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0x22FFFFFF));
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0);
        parent.addView(chip, lp);

        chip.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (onClick != null) onClick.run();
        });
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
        for (MiogramCompanionPrefs.ChatMessage msg : history) {
            renderMessageBubble(msg);
        }
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void renderMessageBubble(MiogramCompanionPrefs.ChatMessage msg) {
        Context ctx = chatMessagesLayout.getContext();

        LinearLayout bubbleRow = new LinearLayout(ctx);
        bubbleRow.setOrientation(LinearLayout.HORIZONTAL);
        bubbleRow.setGravity(msg.isUser ? Gravity.RIGHT : Gravity.LEFT);
        bubbleRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        chatMessagesLayout.addView(bubbleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!msg.isUser) {
            // Mood sprite avatar next to the message
            ImageView spriteAvatar = new ImageView(ctx);
            spriteAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int resId = resolveSpriteForMood(msg.mood);
            spriteAvatar.setImageResource(resId);
            bubbleRow.addView(spriteAvatar, LayoutHelper.createLinear(40, 40, Gravity.BOTTOM, 0, 0, 8, 0));
        }

        LinearLayout bubbleCard = new LinearLayout(ctx);
        bubbleCard.setOrientation(LinearLayout.VERTICAL);

        if (msg.isUser) {
            bubbleCard.setBackground(createBevelDrawable(0xFF381A5E, 0xFFFF70A6, 0xFF1C0A33));
            bubbleCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            bubbleRow.addView(bubbleCard, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT));

            TextView author = new TextView(ctx);
            author.setText("P-chan (You)");
            author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            author.setTypeface(AndroidUtilities.bold());
            author.setTextColor(0xFFFFD166);
            bubbleCard.addView(author);
        } else {
            boolean isAme = MiogramCompanionPrefs.isAmeActive();
            int bg = isAme ? 0xFF24103B : 0xFF0F2B3B;
            int border = isAme ? 0xFF9C41DE : 0xFF00F0FF;
            bubbleCard.setBackground(createBevelDrawable(bg, border, 0xFF080410));
            bubbleCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            bubbleRow.addView(bubbleCard, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT));

            TextView author = new TextView(ctx);
            author.setText(isAme ? "Ame-chan ໒꒱" : "KAngel ✧†");
            author.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            author.setTypeface(AndroidUtilities.bold());
            author.setTextColor(isAme ? 0xFFFF70A6 : 0xFF00F0FF);
            bubbleCard.addView(author);
        }

        TextView body = new TextView(ctx);
        body.setText(msg.text);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        body.setTextColor(0xFFFFFFFF);
        body.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        bubbleCard.addView(body, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        if (!msg.isUser && (scopedDialogId != 0 || onDraftInsertCallback != null)) {
            TextView insertBtn = new TextView(ctx);
            insertBtn.setText("↳ " + MiogramLocale.get("Вставити в чат", "Вставить в чат", "Insert into chat"));
            insertBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            insertBtn.setTypeface(AndroidUtilities.bold());
            insertBtn.setTextColor(0xFF00F0FF);
            insertBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x2200F0FF));
            insertBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
            insertBtn.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (onDraftInsertCallback != null) {
                    onDraftInsertCallback.run(msg.text);
                    finishFragment();
                }
            });
            bubbleCard.addView(insertBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }

        // If message contains an action request, render the interactive permission card
        if (msg.toolAction != null && !msg.actionExecuted) {
            renderActionCard(ctx, bubbleCard, msg);
        }

        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void renderActionCard(Context ctx, LinearLayout parent, MiogramCompanionPrefs.ChatMessage msg) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createBevelDrawable(0xFF1F0C33, 0xFFFFD166, 0xFF0D0317));
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        parent.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView title = new TextView(ctx);
        title.setText("⚠ ACCESS PERMISSION REQUEST");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFD166);
        card.addView(title);

        TextView details = new TextView(ctx);
        details.setText("Action: " + msg.toolAction + "\nParams: " + msg.toolParams);
        details.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        details.setTextColor(0xAAFFFFFF);
        card.addView(details, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 6));

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(btnRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Approve Button
        TextView approveBtn = new TextView(ctx);
        approveBtn.setText("✓ " + MiogramLocale.get("ДОЗВОЛИТИ", "РАЗРЕШИТЬ", "ALLOW"));
        approveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        approveBtn.setTypeface(AndroidUtilities.bold());
        approveBtn.setTextColor(0xFF000000);
        approveBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0xFF00F0FF));
        approveBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        btnRow.addView(approveBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));

        // Deny Button
        TextView denyBtn = new TextView(ctx);
        denyBtn.setText("✕ " + MiogramLocale.get("ВІДХИЛИТИ", "ОТКЛОНИТЬ", "DENY"));
        denyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        denyBtn.setTypeface(AndroidUtilities.bold());
        denyBtn.setTextColor(0xFFFFFFFF);
        denyBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x33FFFFFF));
        denyBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        btnRow.addView(denyBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 4, 0, 0, 0));

        approveBtn.setOnClickListener(v -> {
            MiogramHaptic.success(v);
            msg.actionApproved = true;
            msg.actionExecuted = true;
            MiogramCompanionPrefs.saveHistory(history);
            card.setVisibility(View.GONE);

            // Execute the action
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

            String reply = MiogramCompanionPrefs.isAmeActive()
                    ? "Добре, П-тян... раз ти не дозволяєш, я нічого не чіпатиму (´・ω・｀)"
                    : "†BLESSING†! Відхилено продюсером, скасовую дію! ✧";
            MiogramCompanionPrefs.ChatMessage cancelMsg = new MiogramCompanionPrefs.ChatMessage(false, reply, "sad", System.currentTimeMillis(), null, null);
            history.add(cancelMsg);
            MiogramCompanionPrefs.saveHistory(history);
            renderMessageBubble(cancelMsg);
        });
    }

    private int resolveSpriteForMood(String mood) {
        boolean isAme = MiogramCompanionPrefs.isAmeActive();
        String m = mood != null ? mood.toLowerCase(java.util.Locale.US) : "neutral";

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
        sendProgress.setVisibility(View.VISIBLE);
        sendButton.setAlpha(0.5f);

        // Build conversation prompt
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

        // Check if user is asking for plugin code generation -> promote to 3.8
        boolean isPluginCoding = query.toLowerCase().contains("плагін") || query.toLowerCase().contains("plugin") || query.toLowerCase().contains("код");
        if (isPluginCoding) {
            modelBadge.setText("⚡ 3.8 Flash (Coding)");
        } else {
            modelBadge.setText("⚡ 3.5 Flash Lite");
        }

        MiogramAiService.generateText(fullPrompt.toString(), (rawReply, err) -> AndroidUtilities.runOnUIThread(() -> {
            isSending = false;
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

            // If action is NOT sensitive, execute immediately
            if (action != null && !action.sensitive) {
                MiogramCompanionToolbox.executeTool(currentAccount, action, resultText -> AndroidUtilities.runOnUIThread(() -> {
                    MiogramCompanionPrefs.ChatMessage autoResult = new MiogramCompanionPrefs.ChatMessage(false, "✓ " + resultText, "happy", System.currentTimeMillis(), null, null);
                    history.add(autoResult);
                    MiogramCompanionPrefs.saveHistory(history);
                    renderMessageBubble(autoResult);
                }));
            }
        }));
    }

    private GradientDrawable createBevelDrawable(int background, int lightBorder, int darkBorder) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(background);
        gd.setStroke(AndroidUtilities.dp(2), lightBorder);
        gd.setCornerRadius(AndroidUtilities.dp(6));
        return gd;
    }
}
