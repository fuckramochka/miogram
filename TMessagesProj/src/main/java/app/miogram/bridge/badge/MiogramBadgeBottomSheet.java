package app.miogram.bridge.badge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;

/**
 * Premium Needy Streamer Overload & Cyberpunk Miogram Badge Experience:
 * - Mode A (Reason of Award / "За що отримана стрілочка"):
 *     Official holographic Certificate of Distinction, award citation, verification credentials,
 *     and aesthetic lore symbolism.
 * - Mode B (Arrow Selection Interface / "Інтерфейс вибору стрілочки"):
 *     Rich 10-style card carousel with live animated badges, category badges, dynamic real-time lore inspection,
 *     and instant Supabase cloud synchronization.
 */
public class MiogramBadgeBottomSheet extends BottomSheet {

    private final long targetUserId;
    private final boolean isSelf;
    private MiogramBadgeType selectedBadge;
    private ImageView badgePreviewView;
    private TextView badgeNameView;
    private TextView badgeTagView;
    private TextView dynamicLoreView;
    private GradientDrawable previewBgDrawable;
    private final List<View> carouselCards = new ArrayList<>();

    public MiogramBadgeBottomSheet(BaseFragment fragment, long userId) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        this.targetUserId = userId;
        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        this.isSelf = (userId == currentUserId || userId == 0);
        this.selectedBadge = MiogramBadgeManager.getBadgeType(userId);
        init(fragment.getParentActivity());
    }

    public MiogramBadgeBottomSheet(Context context, long userId) {
        super(context, false);
        this.targetUserId = userId;
        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        this.isSelf = (userId == currentUserId || userId == 0);
        this.selectedBadge = MiogramBadgeManager.getBadgeType(userId);
        init(context);
    }

    private void init(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }

        setApplyBottomPadding(false);
        setApplyTopPadding(false);
        fixNavigationBar(Theme.getColor(Theme.key_dialogBackground));

        final Context finalContext = context;
        final MiogramSupabaseBridge.BadgeRecord record = MiogramBadgeManager.getBadgeRecord(targetUserId);
        final boolean isFounder = (targetUserId == MiogramBadgeManager.FOUNDER_USER_ID);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(26));

        // 1. Sleek Drag Handle
        ImageView dragHandle = new ImageView(context);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setShape(GradientDrawable.RECTANGLE);
        handleDrawable.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handleDrawable.setCornerRadius(AndroidUtilities.dp(4));
        dragHandle.setImageDrawable(handleDrawable);
        root.addView(dragHandle, LayoutHelper.createLinear(38, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // 2. Window / Modal Category Tag
        TextView categoryTag = new TextView(context);
        categoryTag.setTextSize(11.5f);
        categoryTag.setTypeface(AndroidUtilities.bold());
        categoryTag.setTextColor(Color.parseColor("#FF70A6"));
        categoryTag.setGravity(Gravity.CENTER);
        categoryTag.setText(isSelf
                ? MiogramLocale.get("✦ ПЕРСОНАЛІЗАЦІЯ СТРІЛОЧКИ MIOGRAM ✦", "✦ ПЕРСОНАЛИЗАЦИЯ СТРЕЛОЧКИ MIOGRAM ✦", "✦ MIOGRAM ARROW CUSTOMIZER ✦")
                : (isFounder
                    ? MiogramLocale.get("👑 ОФІЦІЙНИЙ СЕРТИФІКАТ ЗАСНОВНИКА 👑", "👑 ОФИЦИАЛЬНЫЙ СЕРТИФИКАТ СОЗДАТЕЛЯ 👑", "👑 OFFICIAL FOUNDER CERTIFICATE 👑")
                    : MiogramLocale.get("📜 ОФІЦІЙНИЙ СЕРТИФІКАТ ВІДЗНАКИ ໒꒱", "📜 ОФИЦИАЛЬНЫЙ СЕРТИФИКАТ ОТЛИЧИЯ ໒꒱", "📜 OFFICIAL BADGE CERTIFICATE ໒꒱")));
        root.addView(categoryTag, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // 3. Main Modal Title
        TextView titleView = new TextView(context);
        titleView.setTextSize(21);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);
        if (isSelf) {
            titleView.setText(MiogramLocale.get("Вибір стилю відзнаки ໒꒱", "Выбор стиля отличия ໒꒱", "Badge Style Studio ໒꒱"));
        } else if (isFounder) {
            titleView.setText(MiogramLocale.get("Засновник & Архітектор ໒꒱", "Создатель & Архитектор ໒꒱", "Founder & Architect ໒꒱"));
        } else {
            String customTitle = (record != null && !TextUtils.isEmpty(record.title)) ? record.title : "Відзнака Спільноти Miogram ໒꒱";
            titleView.setText(customTitle);
        }
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 4. Holographic Showcase Box (Large Animated Preview Chamber)
        LinearLayout showcaseCard = new LinearLayout(context);
        showcaseCard.setOrientation(LinearLayout.VERTICAL);
        showcaseCard.setGravity(Gravity.CENTER);

        previewBgDrawable = new GradientDrawable();
        previewBgDrawable.setShape(GradientDrawable.RECTANGLE);
        previewBgDrawable.setCornerRadius(AndroidUtilities.dp(22));
        updateShowcaseBorder(selectedBadge);
        showcaseCard.setBackground(previewBgDrawable);
        showcaseCard.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        badgePreviewView = new ImageView(context);
        badgePreviewView.setImageDrawable(new MiogramArrowDrawable(88, selectedBadge));
        showcaseCard.addView(badgePreviewView, LayoutHelper.createLinear(110, 88, Gravity.CENTER));

        badgeNameView = new TextView(context);
        badgeNameView.setTextSize(16);
        badgeNameView.setTypeface(AndroidUtilities.bold());
        badgeNameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        badgeNameView.setGravity(Gravity.CENTER);
        badgeNameView.setText(getBadgeDisplayName(selectedBadge));
        showcaseCard.addView(badgeNameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 2));

        badgeTagView = new TextView(context);
        badgeTagView.setTextSize(12);
        badgeTagView.setTypeface(AndroidUtilities.bold());
        badgeTagView.setTextColor(getBadgeAccentColor(selectedBadge));
        badgeTagView.setGravity(Gravity.CENTER);
        badgeTagView.setText(getBadgeSubTag(selectedBadge));
        showcaseCard.addView(badgeTagView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        root.addView(showcaseCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // =============================================================
        // BRANCH A: INSPECTION MODE ("За що отримана стрілочка")
        // =============================================================
        if (!isSelf) {
            // Certificate Citation Card (За що надано)
            LinearLayout citationCard = new LinearLayout(context);
            citationCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable citationBg = new GradientDrawable();
            citationBg.setShape(GradientDrawable.RECTANGLE);
            citationBg.setCornerRadius(AndroidUtilities.dp(16));
            citationBg.setColor(Color.argb(22, 255, 112, 166));
            citationBg.setStroke(AndroidUtilities.dp(1.5f), Color.argb(120, 255, 112, 166));
            citationCard.setBackground(citationBg);
            citationCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

            LinearLayout citHeaderRow = new LinearLayout(context);
            citHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
            citHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView citIcon = new TextView(context);
            citIcon.setText("🎯 ");
            citIcon.setTextSize(14);
            citHeaderRow.addView(citIcon);

            TextView citTitle = new TextView(context);
            citTitle.setText(MiogramLocale.get("ОБҐРУНТУВАННЯ НАДАННЯ ВІДЗНАКИ", "ОБОСНОВАНИЕ ВЫДАЧИ ОТЛИЧИЯ", "AWARD CITATION & REASON"));
            citTitle.setTextSize(12);
            citTitle.setTypeface(AndroidUtilities.bold());
            citTitle.setTextColor(Color.parseColor("#FF70A6"));
            citHeaderRow.addView(citTitle);
            citationCard.addView(citHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView reasonView = new TextView(context);
            reasonView.setTextSize(14);
            reasonView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            reasonView.setLineSpacing(AndroidUtilities.dp(3), 1.0f);

            String reason = (record != null && !TextUtils.isEmpty(record.obtainedReason)) ? record.obtainedReason :
                    (isFounder
                            ? MiogramLocale.get(
                                    "Особиста відзнака засновника та головного архітектора екосистеми Miogram (@fuckramochka). Надана при заснуванні проекту як символ найвищого статусу розробника та підтвердження офіційної автентичності клієнта.",
                                    "Личное отличие создателя и главного архитектора экосистемы Miogram (@fuckramochka). Предоставлено при основании проекта как символ высшего статуса разработчика и подтверждения официальной подлинности клиента.",
                                    "Personal distinction of the Founder & Chief Architect of Miogram (@fuckramochka). Granted upon project genesis as a symbol of supreme developer status and official client authenticity.")
                            : MiogramLocale.get(
                                    "Офіційно верифікований учасник хмарної екосистеми Miogram. Відзнаку надано за вагомий внесок у тестування, підтримку спільноти та активний розвиток клієнта.",
                                    "Официально верифицированный участник облачной экосистемы Miogram. Отличие предоставлено за весомый вклад в тестирование, поддержку сообщества и активное развитие клиента.",
                                    "Officially verified member of the Miogram Cloud ecosystem. Awarded for meaningful contributions to testing, community support, and client development."));

            reasonView.setText("“ " + reason + " ”");
            citationCard.addView(reasonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            root.addView(citationCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

            // Credentials & Verification Grid
            LinearLayout credGrid = new LinearLayout(context);
            credGrid.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable credBg = new GradientDrawable();
            credBg.setShape(GradientDrawable.RECTANGLE);
            credBg.setCornerRadius(AndroidUtilities.dp(16));
            credBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            credGrid.setBackground(credBg);
            credGrid.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

            TextView credHeader = new TextView(context);
            credHeader.setText(MiogramLocale.get("✦ ДЕТАЛІ АВТЕНТИФІКАЦІЇ", "✦ ДЕТАЛИ АУТЕНТИФИКАЦИИ", "✦ AUTHENTICATION DETAILS"));
            credHeader.setTextSize(11.5f);
            credHeader.setTypeface(AndroidUtilities.bold());
            credHeader.setTextColor(Color.parseColor("#00E5FF"));
            credGrid.addView(credHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            String obtainDate = (record != null && !TextUtils.isEmpty(record.obtainedAt)) ? record.obtainedAt : "01.09.2026";
            if (obtainDate.length() > 10) {
                obtainDate = obtainDate.substring(0, 10);
            }

            credGrid.addView(createDetailRow(context,
                    MiogramLocale.get("📅 Дата нагородження:", "📅 Дата выдачи:", "📅 Date Granted:"),
                    obtainDate));
            credGrid.addView(createDetailRow(context,
                    MiogramLocale.get("🛡️ Хмарна верифікація:", "🛡️ Облачная верификация:", "🛡️ Cloud Status:"),
                    MiogramLocale.get("Supabase Verified ✓", "Supabase Verified ✓", "Supabase Verified ✓")));
            credGrid.addView(createDetailRow(context,
                    MiogramLocale.get("🆔 Ідентифікатор користувача:", "🆔 Идентификатор пользователя:", "🆔 User Identifier:"),
                    "UID " + targetUserId));
            credGrid.addView(createDetailRow(context,
                    MiogramLocale.get("🎨 Канонічний стиль:", "🎨 Канонический стиль:", "🎨 Canonical Style:"),
                    selectedBadge.getTitle()));

            root.addView(credGrid, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

            // Artistic Lore & Symbolism
            LinearLayout loreCard = new LinearLayout(context);
            loreCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable loreBg = new GradientDrawable();
            loreBg.setShape(GradientDrawable.RECTANGLE);
            loreBg.setCornerRadius(AndroidUtilities.dp(16));
            loreBg.setColor(Color.argb(20, 0, 229, 255));
            loreBg.setStroke(AndroidUtilities.dp(1), Color.argb(70, 0, 229, 255));
            loreCard.setBackground(loreBg);
            loreCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

            TextView loreTitle = new TextView(context);
            loreTitle.setText(MiogramLocale.get("✦ ХУДОЖНІЙ ЗМІСТ ТА СИМВОЛІЗМ", "✦ ХУДОЖЕСТВЕННЫЙ СМЫСЛ И СИМВОЛИЗМ", "✦ AESTHETIC LORE & SYMBOLISM"));
            loreTitle.setTextSize(11.5f);
            loreTitle.setTypeface(AndroidUtilities.bold());
            loreTitle.setTextColor(Color.parseColor("#00E5FF"));
            loreCard.addView(loreTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

            TextView loreText = new TextView(context);
            loreText.setTextSize(13);
            loreText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            loreText.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            loreText.setText(getBadgeLore(selectedBadge));
            loreCard.addView(loreText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            root.addView(loreCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

            // Cute Close Button
            TextView okButton = new TextView(context);
            okButton.setText(MiogramLocale.get("Зрозуміло ໒꒱", "Понятно ໒꒱", "Got it ໒꒱"));
            okButton.setTextSize(15);
            okButton.setTypeface(AndroidUtilities.bold());
            okButton.setTextColor(Color.WHITE);
            okButton.setGravity(Gravity.CENTER);
            GradientDrawable btnBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFFF70A6, 0xFF00E5FF}
            );
            btnBg.setCornerRadius(AndroidUtilities.dp(16));
            okButton.setBackground(btnBg);
            okButton.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
            okButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                dismiss();
            });
            root.addView(okButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        } else {
            // =============================================================
            // BRANCH B: CUSTOMIZATION MODE ("Інтерфейс вибору стрілочки")
            // =============================================================
            TextView selectHeader = new TextView(context);
            selectHeader.setText(MiogramLocale.get("🎨 ОБЕРІТЬ ВАШ СТИЛЬ (10 ВАРІАНТІВ):", "🎨 ВЫБЕРИТЕ ВАШ СТИЛЬ (10 ВАРИАНТОВ):", "🎨 CHOOSE YOUR BADGE STYLE (10 CHOICES):"));
            selectHeader.setTextSize(12);
            selectHeader.setTypeface(AndroidUtilities.bold());
            selectHeader.setTextColor(Color.parseColor("#FF70A6"));
            root.addView(selectHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            // Rich Card Carousel
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout carousel = new LinearLayout(context);
            carousel.setOrientation(LinearLayout.HORIZONTAL);
            carousel.setGravity(Gravity.CENTER_VERTICAL);
            carousel.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(8));

            carouselCards.clear();
            for (MiogramBadgeType type : MiogramBadgeType.values()) {
                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

                boolean active = (type == selectedBadge);
                card.setBackground(buildCardBackground(active, type));

                ImageView icon = new ImageView(context);
                icon.setImageDrawable(new MiogramArrowDrawable(38, type));
                card.addView(icon, LayoutHelper.createLinear(48, 38, Gravity.CENTER));

                TextView name = new TextView(context);
                name.setText(getBadgeShortName(type));
                name.setTextSize(12.5f);
                name.setTypeface(AndroidUtilities.bold());
                name.setTextColor(active ? getBadgeAccentColor(type) : Theme.getColor(Theme.key_dialogTextBlack));
                card.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 4, 0, 0));

                TextView tag = new TextView(context);
                tag.setText(getBadgeSubTag(type));
                tag.setTextSize(10.5f);
                tag.setTextColor(Color.parseColor("#8E8E93"));
                card.addView(tag, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 2, 0, 0));

                card.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    selectedBadge = type;

                    // Update Holographic Showcase
                    badgePreviewView.setImageDrawable(new MiogramArrowDrawable(88, selectedBadge));
                    badgeNameView.setText(getBadgeDisplayName(selectedBadge));
                    badgeTagView.setText(getBadgeSubTag(selectedBadge));
                    badgeTagView.setTextColor(getBadgeAccentColor(selectedBadge));
                    updateShowcaseBorder(selectedBadge);

                    // Update Dynamic Lore
                    if (dynamicLoreView != null) {
                        dynamicLoreView.setText(getBadgeLore(selectedBadge));
                    }

                    // Auto-sync in background
                    long curUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                    MiogramSupabaseBridge.setSelectedBadgeForAccount(finalContext, curUserId, selectedBadge);

                    // Re-draw selection states on all cards
                    for (int i = 0; i < carousel.getChildCount(); i++) {
                        View ch = carousel.getChildAt(i);
                        boolean isCur = (i == selectedBadge.ordinal());
                        ch.setBackground(buildCardBackground(isCur, MiogramBadgeType.values()[i]));
                        if (ch instanceof LinearLayout) {
                            LinearLayout l = (LinearLayout) ch;
                            if (l.getChildCount() >= 2 && l.getChildAt(1) instanceof TextView) {
                                ((TextView) l.getChildAt(1)).setTextColor(isCur ? getBadgeAccentColor(selectedBadge) : Theme.getColor(Theme.key_dialogTextBlack));
                            }
                        }
                    }
                });

                carouselCards.add(card);
                carousel.addView(card, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));
            }
            scroll.addView(carousel);
            root.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

            // Dynamic Live Lore Card for Selected Style
            LinearLayout dynamicLoreCard = new LinearLayout(context);
            dynamicLoreCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable dLoreBg = new GradientDrawable();
            dLoreBg.setShape(GradientDrawable.RECTANGLE);
            dLoreBg.setCornerRadius(AndroidUtilities.dp(16));
            dLoreBg.setColor(Color.argb(18, 255, 112, 166));
            dLoreBg.setStroke(AndroidUtilities.dp(1), Color.argb(60, 255, 112, 166));
            dynamicLoreCard.setBackground(dLoreBg);
            dynamicLoreCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

            TextView loreHead = new TextView(context);
            loreHead.setText(MiogramLocale.get("✦ ОПИС ТА ЕСТЕТИКА ОБРАНОГО СТИЛЮ ✦", "✦ ОПИСАНИЕ И ЭСТЕТИКА ВЫБРАННОГО СТИЛЯ ✦", "✦ SELECTED STYLE AESTHETICS & LORE ✦"));
            loreHead.setTextSize(11.5f);
            loreHead.setTypeface(AndroidUtilities.bold());
            loreHead.setTextColor(Color.parseColor("#FF70A6"));
            dynamicLoreCard.addView(loreHead, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

            dynamicLoreView = new TextView(context);
            dynamicLoreView.setTextSize(13);
            dynamicLoreView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            dynamicLoreView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            dynamicLoreView.setText(getBadgeLore(selectedBadge));
            dynamicLoreCard.addView(dynamicLoreView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            root.addView(dynamicLoreCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

            // Cloud Sync Indicator Card
            LinearLayout syncCard = new LinearLayout(context);
            syncCard.setOrientation(LinearLayout.HORIZONTAL);
            syncCard.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable syncBg = new GradientDrawable();
            syncBg.setShape(GradientDrawable.RECTANGLE);
            syncBg.setCornerRadius(AndroidUtilities.dp(14));
            syncBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            syncCard.setBackground(syncBg);
            syncCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

            TextView syncDot = new TextView(context);
            syncDot.setText("☁️ ");
            syncDot.setTextSize(13);
            syncCard.addView(syncDot);

            TextView syncText = new TextView(context);
            syncText.setText(MiogramLocale.get(
                    "Хмарна синхронізація Supabase: Активна (Стиль миттєво видно всім у чатах)",
                    "Облачная синхронизация Supabase: Активна (Стиль мгновенно виден всем в чатах)",
                    "Supabase Cloud Sync: Active (Instant badge visibility across chats)"));
            syncText.setTextSize(12.5f);
            syncText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            syncCard.addView(syncText);

            root.addView(syncCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

            // Save & Apply Button
            TextView saveButton = new TextView(context);
            saveButton.setText(MiogramLocale.get("✦ Застосувати стиль стрілочки ໒꒱", "✦ Применить стиль стрелочки ໒꒱", "✦ Apply & Save Badge Style ໒꒱"));
            saveButton.setTextSize(15.5f);
            saveButton.setTypeface(AndroidUtilities.bold());
            saveButton.setTextColor(Color.WHITE);
            saveButton.setGravity(Gravity.CENTER);

            GradientDrawable btnBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFFF70A6, 0xFF00E5FF}
            );
            btnBg.setCornerRadius(AndroidUtilities.dp(16));
            saveButton.setBackground(btnBg);
            saveButton.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));

            saveButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                long curUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                MiogramSupabaseBridge.setSyncEnabledForAccount(finalContext, curUserId, true);
                MiogramSupabaseBridge.setSelectedBadgeForAccount(finalContext, curUserId, selectedBadge);
                dismiss();
            });

            root.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        scrollView.addView(root);
        setCustomView(scrollView);
    }

    private View createDetailRow(Context context, String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(labelView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView valView = new TextView(context);
        valView.setText(value);
        valView.setTextSize(13);
        valView.setTypeface(AndroidUtilities.bold());
        valView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        row.addView(valView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        return row;
    }

    private void updateShowcaseBorder(MiogramBadgeType type) {
        if (previewBgDrawable == null) return;
        int accent = getBadgeAccentColor(type);
        previewBgDrawable.setColor(Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent)));
        previewBgDrawable.setStroke(AndroidUtilities.dp(2), Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent)));
    }

    private GradientDrawable buildCardBackground(boolean active, MiogramBadgeType type) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(AndroidUtilities.dp(16));
        int accent = getBadgeAccentColor(type);
        if (active) {
            d.setColor(Color.argb(45, Color.red(accent), Color.green(accent), Color.blue(accent)));
            d.setStroke(AndroidUtilities.dp(2f), accent);
        } else {
            d.setColor(Color.argb(14, 128, 128, 128));
            d.setStroke(AndroidUtilities.dp(1.2f), Color.argb(35, 128, 128, 128));
        }
        return d;
    }

    private int getBadgeAccentColor(MiogramBadgeType type) {
        switch (type) {
            case PINK:    return 0xFFFF2A93;
            case CYAN:    return 0xFF00E5FF;
            case DARK:    return 0xFF9D4EDD;
            case ANGEL:   return 0xFFC77DFF;
            case DEVIL:   return 0xFFFF0055;
            case RAINBOW: return 0xFFFFD166;
            case OUTLINE: return 0xFF00F0FF;
            case GLITCH:  return 0xFF00F0FF;
            case PREMIUM: return 0xFFFFD700;
            case ORIGINAL:
            default:      return 0xFF00F0FF;
        }
    }

    private String getBadgeDisplayName(MiogramBadgeType type) {
        switch (type) {
            case PINK:    return "02 • K-Angel Pink 💖";
            case CYAN:    return "03 • Cyber Cyan ⚡";
            case DARK:    return "04 • Dark Obsidian 🌌";
            case ANGEL:   return "05 • Seraphim Angel 👼";
            case DEVIL:   return "06 • Devil Rebel 😈";
            case RAINBOW: return "07 • Prismatic Rainbow 🌈";
            case OUTLINE: return "08 • Wireframe Cyber 🔲";
            case GLITCH:  return "09 • CRT Glitch 📺";
            case PREMIUM: return "10 • Royal Gold 👑";
            case ORIGINAL:
            default:      return "01 • Original Classic ໒꒱";
        }
    }

    private String getBadgeShortName(MiogramBadgeType type) {
        switch (type) {
            case PINK:    return "Pink";
            case CYAN:    return "Cyan";
            case DARK:    return "Dark";
            case ANGEL:   return "Angel";
            case DEVIL:   return "Devil";
            case RAINBOW: return "Rainbow";
            case OUTLINE: return "Outline";
            case GLITCH:  return "Glitch";
            case PREMIUM: return "Premium";
            case ORIGINAL:
            default:      return "Original";
        }
    }

    private String getBadgeSubTag(MiogramBadgeType type) {
        switch (type) {
            case PINK:    return MiogramLocale.get("💖 K-Angel Неон", "💖 K-Angel Неон", "💖 K-Angel Neon");
            case CYAN:    return MiogramLocale.get("⚡ Кібер Сяйво", "⚡ Кибер Сияние", "⚡ Cyber Glow");
            case DARK:    return MiogramLocale.get("🌌 Обсидіан", "🌌 Обсидиан", "🌌 Obsidian");
            case ANGEL:   return MiogramLocale.get("👼 Ширяючий Німб", "👼 Парящий Нимб", "👼 Halo & Feathers");
            case DEVIL:   return MiogramLocale.get("😈 Бунтарські Ріжки", "😈 Бунтарские Рожки", "😈 Rebel Horns");
            case RAINBOW: return MiogramLocale.get("🌈 Призматичний", "🌈 Призматический", "🌈 Prismatic");
            case OUTLINE: return MiogramLocale.get("🔲 Вайрфрейм 1px", "🔲 Вайрфрейм 1px", "🔲 Wireframe 1px");
            case GLITCH:  return MiogramLocale.get("📺 CRT Розщеплення", "📺 CRT Расщепление", "📺 CRT RGB Split");
            case PREMIUM: return MiogramLocale.get("👑 Золота Корона", "👑 Золотая Корона", "👑 Royal Crown");
            case ORIGINAL:
            default:      return MiogramLocale.get("໒꒱ Канонічний", "໒꒱ Канонический", "໒꒱ Canonical");
        }
    }

    private String getBadgeLore(MiogramBadgeType type) {
        switch (type) {
            case PINK:
                return MiogramLocale.get(
                        "Неоново-рожевий кібер-стиль із шевронами серця. Символ естетики Needy Streamer Overload та безмежної любові до Інтернет-Ангела.",
                        "Неоново-розовый кибер-стиль с шевронами сердца. Символ эстетики Needy Streamer Overload и бесконечной любви к Интернет-Ангелу.",
                        "Neon pink cyber aesthetic with chevron heart ribs. The quintessential symbol of Needy Streamer Overload devotion.");
            case CYAN:
                return MiogramLocale.get(
                        "Електричний блакитний стиль з білими акцентами та сяйвом. Символізує технологічність, холодний розум та надшвидку реакцію Miogram.",
                        "Электрический лазурный стиль с белыми акцентами и сиянием. Символизирует технологичность, холодный ум и сверхбыструю реакцию Miogram.",
                        "Electric sky-blue cyber wings with luminous starlight. Symbolizes Miogram speed, clarity, and next-gen technology.");
            case DARK:
                return MiogramLocale.get(
                        "Темний обсидіановий варіант з оксамитовим неоновим краєм для поціновувачів нічного режиму, таємничості та естетики глибокого космосу.",
                        "Темный обсидиановый вариант с бархатным неоновым краем для ценителей ночного режима, таинственности и эстетики глубокого космоса.",
                        "Midnight obsidian wings with velvet violet aura. Crafted for night owls, stealth lovers, and deep-space vibes.");
            case ANGEL:
                return MiogramLocale.get(
                        "Ангельські крила з ширяючим білим німбом та лавандовим серцем. Відзнака гармонії, чистих помислів та піднесення †昇天†.",
                        "Ангельские крылья с парящим белым нимбом и лавандовым сердцем. Знак гармонии, чистых помыслов и вознесения †昇天†.",
                        "Angelic wings with hovering white halo and lavender heart. The badge of purity, harmony, and transcendental ascension †昇天†.");
            case DEVIL:
                return MiogramLocale.get(
                        "Грайливі ріжки та крила кажана з гарячим рожевим неоном. Відзнака бунтарського духу, свободи від правил та зухвалого шарму.",
                        "Игривые рожки и крылья летучей мыши с горячим розовым неоном. Знак бунтарского духа, свободы от правил и дерзкого шарма.",
                        "Playful devil horns and scalloped bat wings with blazing neon. Distinctive emblem of rebellion, defiance, and chaos charm.");
            case RAINBOW:
                return MiogramLocale.get(
                        "Призматичний веселковий спектр із золотим контуром. Символ безмежного різноманіття, креативності та яскравих емоцій у спілкуванні.",
                        "Призматический радужный спектр с золотым контуром. Символ безграничного разнообразия, креативности и ярких эмоций в общении.",
                        "Prismatic rainbow spectrum with golden accents. Represents limitless diversity, creative energy, and joyful communication.");
            case OUTLINE:
                return MiogramLocale.get(
                        "Мінімалістичний 1-піксельний вайрфрейм-контур. Кіберпанк у чистому вигляді — жодної зайвої деталі, лише чиста геометрія та функціонал.",
                        "Минималистичный 1-пиксельный вайрфрейм-контур. Чистый киберпанк — ни единой лишней детали, только чистая геометрия и функционал.",
                        "Minimalist 1px wireframe cyber contour. Pure cyberpunk minimalism — clean geometry, sharp lines, zero excess.");
            case GLITCH:
                return MiogramLocale.get(
                        "Хроматична аберація RGB із розщепленням форми та сканлайнами. Для поціновувачів естетики VHS касет, CRT моніторів та кібер-збоїв.",
                        "Хроматическая аберрация RGB с расщеплением формы и сканлайнами. Для ценителей эстетики VHS кассет, CRT мониторов и кибер-сбоев.",
                        "Chromatic RGB displacement with dynamic scanlines. Made for connoisseurs of VHS tapes, CRT monitors, and cyber distortion.");
            case PREMIUM:
                return MiogramLocale.get(
                        "Королівська золота корона, янтарні крила та золоті ребра. Елітна відзнака визнання найвищих досягнень та статусу в екосистемі Miogram.",
                        "Королевская золотая корона, янтарные крылья и золотые ребра. Элитное отличие признания высших достижений и статуса в экосистеме Miogram.",
                        "Royal golden crown and amber wings with chest armor. Elite distinction honoring top contributors and paramount status.");
            case ORIGINAL:
            default:
                return MiogramLocale.get(
                        "Класична стрілочка Miogram з антеною та рожевими пір'їнами. Перша відзнака екосистеми, з якої розпочалася вся історія проекту ໒꒱.",
                        "Классическая стрелочка Miogram с антенной и розовыми перьями. Первое отличие экосистемы, с которого началась вся история проекта ໒꒱.",
                        "Canonical Miogram winged heart with antenna visor. The foundational badge of the ecosystem that started it all ໒꒱.");
        }
    }
}
