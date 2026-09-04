package app.miogram.bridge.badge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;

/**
 * High-End Cyber Miogram Badge Modal:
 * - Mode A (Inspection): Displays badge lore, authentic obtain reason, and date from Supabase.
 * - Mode B (Customization): High-tech interactive card carousel for selecting from 10 canonical styles,
 *   with live preview, haptics, and instant Supabase cloud synchronization.
 */
public class MiogramBadgeBottomSheet extends BottomSheet {

    private final long targetUserId;
    private final boolean isSelf;
    private MiogramBadgeType selectedBadge;
    private ImageView badgePreviewView;
    private TextView subtitleView;

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
        fixNavigationBar(Theme.getColor(Theme.key_windowBackgroundWhite));

        final Context finalContext = context;
        final MiogramSupabaseBridge.BadgeRecord record = MiogramBadgeManager.getBadgeRecord(targetUserId);
        final boolean isFounder = (targetUserId == MiogramBadgeManager.FOUNDER_USER_ID);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(24));

        // 1. Drag Handle
        ImageView dragHandle = new ImageView(context);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setShape(GradientDrawable.RECTANGLE);
        handleDrawable.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handleDrawable.setCornerRadius(AndroidUtilities.dp(3));
        dragHandle.setImageDrawable(handleDrawable);
        root.addView(dragHandle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 2. Large Radiant Badge Preview Box
        LinearLayout previewCard = new LinearLayout(context);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setGravity(Gravity.CENTER);
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setShape(GradientDrawable.RECTANGLE);
        previewBg.setCornerRadius(AndroidUtilities.dp(22));
        previewBg.setColor(Color.argb(30, 0, 229, 255));
        previewBg.setStroke(AndroidUtilities.dp(1.5f), Color.argb(120, 0, 229, 255));
        previewCard.setBackground(previewBg);
        previewCard.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        badgePreviewView = new ImageView(context);
        badgePreviewView.setImageDrawable(new MiogramArrowDrawable(80, selectedBadge));
        previewCard.addView(badgePreviewView, LayoutHelper.createLinear(100, 80, Gravity.CENTER));

        root.addView(previewCard, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // 3. Title
        TextView titleView = new TextView(context);
        titleView.setTextSize(20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);

        if (isSelf) {
            titleView.setText(MiogramLocale.get("Налаштування бейджа ໒꒱", "Настройка бейджика ໒꒱", "Badge Customization ໒꒱"));
        } else if (isFounder) {
            titleView.setText(MiogramLocale.get("Засновник Miogram ໒꒱", "Создатель Miogram ໒꒱", "Miogram Founder ໒꒱"));
        } else {
            String title = (record != null && record.title != null) ? record.title : "Відзнака спільноти Miogram";
            titleView.setText(title);
        }
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // 4. Subtitle / Style tag
        subtitleView = new TextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(AndroidUtilities.bold());
        subtitleView.setTextColor(Color.parseColor("#00E5FF"));
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setText(selectedBadge.getCode() + " • " + selectedBadge.getTitle());
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // -------------------------------------------------------------
        // BRANCH: INSPECTION MODE (Viewing other user)
        // -------------------------------------------------------------
        if (!isSelf) {
            // Card: Історія отримання (Obtain History & Reason)
            LinearLayout historyCard = new LinearLayout(context);
            historyCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable histBg = new GradientDrawable();
            histBg.setShape(GradientDrawable.RECTANGLE);
            histBg.setCornerRadius(AndroidUtilities.dp(16));
            histBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            historyCard.setBackground(histBg);
            historyCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

            TextView historyHeader = new TextView(context);
            historyHeader.setText(MiogramLocale.get("✦ Історія отримання", "✦ История получения", "✦ Obtain History"));
            historyHeader.setTextSize(12.5f);
            historyHeader.setTypeface(AndroidUtilities.bold());
            historyHeader.setTextColor(Color.parseColor("#00E5FF"));
            historyCard.addView(historyHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

            TextView reasonView = new TextView(context);
            reasonView.setTextSize(13.5f);
            reasonView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            reasonView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);

            String reason = (record != null && record.obtainedReason != null) ? record.obtainedReason :
                    (isFounder ? "Створено автором Miogram як найпершу відзнаку екосистеми з моменту заснування проекту (01.09.2026)." : "Верифікований учасник хмарної екосистеми Miogram.");
            String date = (record != null && record.obtainedAt != null) ? record.obtainedAt : "2026";
            reasonView.setText(reason + "\n\n" + MiogramLocale.get("Дата надання: ", "Дата выдачи: ", "Granted: ") + date);
            historyCard.addView(reasonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            root.addView(historyCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

            // Card: Що це за бейдж (Badge Lore)
            LinearLayout loreCard = new LinearLayout(context);
            loreCard.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable loreBg = new GradientDrawable();
            loreBg.setShape(GradientDrawable.RECTANGLE);
            loreBg.setCornerRadius(AndroidUtilities.dp(16));
            loreBg.setColor(Color.argb(20, 112, 214, 255));
            loreBg.setStroke(AndroidUtilities.dp(1), Color.argb(60, 112, 214, 255));
            loreCard.setBackground(loreBg);
            loreCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

            TextView loreText = new TextView(context);
            loreText.setTextSize(13f);
            loreText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            loreText.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            loreText.setText(getBadgeLore(selectedBadge));
            loreCard.addView(loreText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            root.addView(loreCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

            // Close button
            TextView okButton = new TextView(context);
            okButton.setText(MiogramLocale.get("Зрозуміло ໒꒱", "Понятно ໒꒱", "Got it ໒꒱"));
            okButton.setTextSize(15);
            okButton.setTypeface(AndroidUtilities.bold());
            okButton.setTextColor(Color.WHITE);
            okButton.setGravity(Gravity.CENTER);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(AndroidUtilities.dp(14));
            btnBg.setColor(0xFF00B4D8);
            okButton.setBackground(btnBg);
            okButton.setPadding(0, AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13));
            okButton.setOnClickListener(v -> dismiss());
            root.addView(okButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        } else {
            // -------------------------------------------------------------
            // BRANCH: CUSTOMIZATION MODE (Own badge selector)
            // -------------------------------------------------------------
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout carousel = new LinearLayout(context);
            carousel.setOrientation(LinearLayout.HORIZONTAL);
            carousel.setGravity(Gravity.CENTER_VERTICAL);
            carousel.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(10));

            for (MiogramBadgeType type : MiogramBadgeType.values()) {
                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

                GradientDrawable cBg = new GradientDrawable();
                cBg.setShape(GradientDrawable.RECTANGLE);
                cBg.setCornerRadius(AndroidUtilities.dp(16));
                boolean active = (type == selectedBadge);
                cBg.setColor(active ? Color.argb(55, 0, 229, 255) : Color.argb(16, 128, 128, 128));
                cBg.setStroke(AndroidUtilities.dp(1.5f), active ? Color.parseColor("#00E5FF") : Color.argb(40, 128, 128, 128));
                card.setBackground(cBg);

                ImageView icon = new ImageView(context);
                icon.setImageDrawable(new MiogramArrowDrawable(34, type));
                card.addView(icon, LayoutHelper.createLinear(44, 34, Gravity.CENTER));

                TextView num = new TextView(context);
                num.setText(type.getCode().substring(0, 2));
                num.setTextSize(12);
                num.setTypeface(AndroidUtilities.bold());
                num.setTextColor(active ? Color.parseColor("#00E5FF") : Theme.getColor(Theme.key_dialogTextBlack));
                card.addView(num, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 4, 0, 0));

                card.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    selectedBadge = type;
                    badgePreviewView.setImageDrawable(new MiogramArrowDrawable(80, selectedBadge));
                    subtitleView.setText(selectedBadge.getCode() + " • " + selectedBadge.getTitle());

                    long curUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                    MiogramSupabaseBridge.setSelectedBadgeForAccount(finalContext, curUserId, selectedBadge);

                    // Re-render selection borders
                    for (int i = 0; i < carousel.getChildCount(); i++) {
                        View ch = carousel.getChildAt(i);
                        GradientDrawable b = (GradientDrawable) ch.getBackground();
                        boolean isCur = (i == selectedBadge.ordinal());
                        b.setColor(isCur ? Color.argb(55, 0, 229, 255) : Color.argb(16, 128, 128, 128));
                        b.setStroke(AndroidUtilities.dp(1.5f), isCur ? Color.parseColor("#00E5FF") : Color.argb(40, 128, 128, 128));
                    }
                });

                carousel.addView(card, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));
            }
            scroll.addView(carousel);
            root.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

            // Sync Status Card
            LinearLayout statusCard = new LinearLayout(context);
            statusCard.setOrientation(LinearLayout.HORIZONTAL);
            statusCard.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setShape(GradientDrawable.RECTANGLE);
            statusBg.setCornerRadius(AndroidUtilities.dp(14));
            statusBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            statusCard.setBackground(statusBg);
            statusCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

            TextView statusDot = new TextView(context);
            statusDot.setText("🟢 ");
            statusDot.setTextSize(12);
            statusCard.addView(statusDot);

            TextView statusText = new TextView(context);
            statusText.setText(MiogramLocale.get("Хмарна синхронізація Supabase активна", "Облачная синхронизация Supabase активна", "Supabase Cloud Sync Active"));
            statusText.setTextSize(13);
            statusText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            statusCard.addView(statusText);

            root.addView(statusCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

            // Save & Cloud Sync Button
            TextView saveButton = new TextView(context);
            saveButton.setText(MiogramLocale.get("Зберегти та Синхронізувати ໒꒱", "Сохранить и Синхронизировать ໒꒱", "Save & Sync to Cloud ໒꒱"));
            saveButton.setTextSize(15);
            saveButton.setTypeface(AndroidUtilities.bold());
            saveButton.setTextColor(Color.WHITE);
            saveButton.setGravity(Gravity.CENTER);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(AndroidUtilities.dp(14));
            btnBg.setColor(0xFF00B4D8);
            saveButton.setBackground(btnBg);
            saveButton.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));

            saveButton.setOnClickListener(v -> {
                long curUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                MiogramSupabaseBridge.setSyncEnabledForAccount(finalContext, curUserId, true);
                MiogramSupabaseBridge.setSelectedBadgeForAccount(finalContext, curUserId, selectedBadge);
                dismiss();
            });

            root.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        setCustomView(root);
    }

    private String getBadgeLore(MiogramBadgeType type) {
        switch (type) {
            case PINK:
                return MiogramLocale.get("Неоново-рожевий кібер-стиль із шевронами серця. Символ естетики Needy Streamer Overload.", "Неоново-розовый кибер-стиль с шевронами сердца. Символ эстетики Needy Streamer Overload.", "Neon pink cyber aesthetic with chevron heart ribs.");
            case CYAN:
                return MiogramLocale.get("Електричний блакитний стиль з білими акцентами та сяйвом. Символізує технологічність Miogram.", "Электрический лазурный стиль с белыми акцентами и сиянием. Символизирует технологичность Miogram.", "Electric sky-blue cyber wings with luminous starlight.");
            case DARK:
                return MiogramLocale.get("Темний обсидіановий варіант з оксамитовим неоновим краєм для поціновувачів нічного режиму.", "Темный обсидиановый вариант с бархатным неоновым краем для ценителей ночного режима.", "Midnight obsidian wings with velvet violet aura.");
            case ANGEL:
                return MiogramLocale.get("Ангельські крила з ширяючим німбом та лавандовим серцем. Відзнака гармонії та спокою.", "Ангельские крылья с парящим нимбом и лавандовым сердцем. Знак гармонии и покоя.", "Angelic wings with hovering white halo and lavender heart.");
            case DEVIL:
                return MiogramLocale.get("Грайливі ріжки та крила кажана з гарячим рожевим неоном. Відзнака бунтарського духу.", "Игривые рожки и крылья летучей мыши с горячим розовым неоном. Знак бунтарского духа.", "Playful devil horns and scalloped bat wings.");
            case RAINBOW:
                return MiogramLocale.get("Призматичний веселковий спектр із золотим контуром. Символ безмежного різноманіття.", "Призматический радужный спектр с золотым контуром. Символ безграничного разнообразия.", "Prismatic rainbow spectrum with golden accents.");
            case OUTLINE:
                return MiogramLocale.get("Мінімалістичний 1-піксельний вайрфрейм-контур. Кіберпанк у чистому вигляді.", "Минималистичный 1-пиксельный вайрфрейм-контур. Чистый киберпанк.", "Minimalist 1px wireframe cyber contour.");
            case GLITCH:
                return MiogramLocale.get("Хроматична аберація RGB із розщепленням форми та сканлайнами. Для поціновувачів CRT-глітчу.", "Хроматическая аберрация RGB с расщеплением формы и сканлайнами. Для ценителей CRT-глитча.", "Chromatic RGB displacement with dynamic scanlines.");
            case PREMIUM:
                return MiogramLocale.get("Королівська золота корона, янтарні крила та золоті ребра. Преміальна відзнака Miogram.", "Королевская золотая корона, янтарные крылья и золотые ребра. Премиальное отличие Miogram.", "Royal golden crown and amber wings with chest armor.");
            case ORIGINAL:
            default:
                return MiogramLocale.get("Класична стрілочка Miogram з антеною та рожевими пір'їнами. Перша відзнака екосистеми.", "Классическая стрелочка Miogram с антенной и розовыми перьями. Первое отличие экосистемы.", "Canonical Miogram winged heart with antenna visor.");
        }
    }
}
