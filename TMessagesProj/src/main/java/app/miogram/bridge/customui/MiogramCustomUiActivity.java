package app.miogram.bridge.customui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;

/**
 * Miogram Custom UI Studio (Студія Кастом Юай):
 * Comprehensive design studio built with the exact visual language, depth, and modularity
 * of Custom Profile, providing complete mastery over the Telegram client interface:
 *
 * 1. Воршоп / Майстерня: Curated theme masterpieces with instant live previews & 1-tap activation.
 * 2. Бульбашки чату: Full gradient shaders, angle wheels, corner curves, custom typography & glow.
 * 3. Ефекти Імен: Shaders (🔥 Fire, ❄️ Ice, 🌈 Rainbow, ✨ Glare, ⚡ Cyber Neon), glow layers, custom fonts.
 * 4. Аватари та Рамки: Shapes (Circle, Squircle, Rounded Rect, Hexagon, Star, Diamond) & Glowing Neon Story Rings.
 * 5. Інтерфейс та Чат: Floating cards, glassmorphic headers, unread pills, ProMotion 120Hz, and rich haptics.
 */
public class MiogramCustomUiActivity extends BaseFragment {

    private int activeTab = 0; // 0=Workshop, 1=Bubbles, 2=Name FX, 3=Avatars, 4=Interface

    private LinearLayout tabRail;
    private FrameLayout container;
    private LivePreviewView livePreview;

    // Bubbles UI Elements
    private TextView angleValueText;
    private TextView radiusValueText;
    private SeekBar angleSeekBar;
    private SeekBar radiusSeekBar;
    private TextCheckCell gradientSwitch;

    // Name FX UI Elements
    private TextView glowRadiusValueText;
    private SeekBar glowRadiusSeekBar;
    private TextCheckCell nameGlowSwitch;
    private TextView fontSubtitle;

    // Avatar UI Elements
    private TextCheckCell avatarRingSwitch;
    private TextCheckCell avatarPulseSwitch;
    private TextView ringWidthValueText;
    private SeekBar ringWidthSeekBar;

    // Interface UI Elements
    private TextView dialogStyleSubtitle;
    private TextView unreadStyleSubtitle;
    private TextView headerStyleSubtitle;
    private TextView hapticSubtitle;
    private TextCheckCell proMotionSwitch;
    private TextCheckCell onlinePulseSwitch;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Кастом Юай Студія ໒꒱", "Кастом Юай Студия ໒꒱", "Custom UI Studio ໒꒱"));
        actionBar.setSubtitle(MiogramLocale.get("Дизайн-система Miogram", "Дизайн-система Miogram", "Miogram Design System"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showResetDialog();
                }
            }
        });
        actionBar.createMenu().addItem(1, R.drawable.msg_reset);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // 1. Navigation Rail Tabs (Воршоп, Бульбашки, Ефекти Імен, Аватари, Інтерфейс)
        mainLayout.addView(createNavigationRail(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 2. Interactive Live Preview Stage (Sticky at top)
        livePreview = new LivePreviewView(context);
        mainLayout.addView(livePreview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 210, 12, 8, 12, 8));

        // 3. Dynamic Studio Tab Container (Scrollable)
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        mainLayout.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        // 4. Bottom Sticky Action Bar
        mainLayout.addView(createBottomBar(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        root.addView(mainLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Switch to initial tab (0 = Workshop)
        switchTab(0);

        fragmentView = root;
        return fragmentView;
    }

    /* =========================================================================
     * NAVIGATION RAIL (TABS)
     * ========================================================================= */

    private View createNavigationRail(Context context) {
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));

        tabRail = new LinearLayout(context);
        tabRail.setOrientation(LinearLayout.HORIZONTAL);
        tabRail.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

        String[] tabs = {
                MiogramLocale.get("🎨 Воршоп", "🎨 Воркшоп", "🎨 Workshop"),
                MiogramLocale.get("💬 Бульбашки", "💬 Пузырьки", "💬 Bubbles"),
                MiogramLocale.get("✨ Ефекти Імен", "✨ Эффекты Имен", "✨ Name FX"),
                MiogramLocale.get("👤 Аватари", "👤 Аватары", "👤 Avatars"),
                MiogramLocale.get("📱 Інтерфейс", "📱 Интерфейс", "📱 Interface")
        };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView tabBtn = new TextView(context);
            tabBtn.setText(tabs[i]);
            tabBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tabBtn.setTypeface(AndroidUtilities.bold());
            tabBtn.setGravity(Gravity.CENTER);
            tabBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

            tabBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                switchTab(index);
            });

            tabRail.addView(tabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 0));
        }

        hScroll.addView(tabRail);
        return hScroll;
    }

    private void switchTab(int index) {
        activeTab = index;
        Context context = getParentActivity();
        if (context == null) return;

        // Update tab styling
        for (int i = 0; i < tabRail.getChildCount(); i++) {
            TextView tv = (TextView) tabRail.getChildAt(i);
            boolean selected = (i == activeTab);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(AndroidUtilities.dp(12));
            if (selected) {
                bg.setColor(0xFF7052FF);
                tv.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
            }
            tv.setBackground(bg);
        }

        container.removeAllViews();
        switch (activeTab) {
            case 0: container.addView(createWorkshopTab(context)); break;
            case 1: container.addView(createBubblesTab(context)); break;
            case 2: container.addView(createNameFxTab(context)); break;
            case 3: container.addView(createAvatarsTab(context)); break;
            case 4: container.addView(createInterfaceTab(context)); break;
        }
        livePreview.invalidate();
    }

    /* =========================================================================
     * TAB 0: WORKSHOP / МАЙСТЕРНЯ (CURATED MASTERPIECES)
     * ========================================================================= */

    private View createWorkshopTab(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createSectionHeader(context, MiogramLocale.get("Колекція готових стилів ✨", "Коллекция готовых стилей ✨", "Curated Masterpieces ✨")));

        String[] titles = {
                "🚀 Cyberpunk 2077", "💎 Liquid Glass iOS 18",
                "🌌 Deep Space AMOLED", "🌅 Sunset Miami Beach",
                "🌿 Emerald Cyber Frost", "💜 Electric Royalty"
        };
        String[] descs = {
                "Полум'яні імена, неонові сквіркли та рожево-блакитний градієнт",
                "Прозоре розмите скло, плавні кути 22dp та елегантний блік",
                "Глибокий чорний AMOLED, гексагони та крижані кристали",
                "Теплий градієнт заходу сонця, зіркові аватари та веселка",
                "Смарагдово-м'ятний градієнт, неоновий кіберпанк та сквіркли",
                "Королівський фіолетово-золотий градієнт, діаманти та сяйво"
        };
        int[][] colors = {
                {0xFFFF007F, 0xFF00F0FF}, {0xFF3A88E9, 0xFF7052FF},
                {0xFF161618, 0xFF00C7FF}, {0xFFFF5E3A, 0xFFFF2A68},
                {0xFF00B09B, 0xFF96C93D}, {0xFF8A2387, 0xFFFFD700}
        };

        for (int i = 0; i < titles.length; i++) {
            final int presetId = i;
            layout.addView(createWorkshopCard(context, titles[i], descs[i], colors[i], v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                MiogramCustomUiPrefs.applyPreset(presetId);
                livePreview.invalidate();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Стиль «" + titles[presetId] + "» активовано! ໒꒱").show();
            }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));
        }

        return layout;
    }

    private View createWorkshopCard(Context context, String title, String desc, int[] gradColors, View.OnClickListener onClick) {
        FrameLayout card = new FrameLayout(context);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        card.setBackground(bg);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Gradient Indicator Pill
        View pill = new View(context);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(AndroidUtilities.dp(12));
        pillBg.setColors(gradColors);
        pillBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        pill.setBackground(pillBg);
        row.addView(pill, LayoutHelper.createLinear(36, 36, 0, 0, 14, 0));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tView = new TextView(context);
        tView.setText(title);
        tView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tView.setTypeface(AndroidUtilities.bold());
        tView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        textCol.addView(tView);

        TextView dView = new TextView(context);
        dView.setText(desc);
        dView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        dView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        textCol.addView(dView);

        row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView applyBtn = new TextView(context);
        applyBtn.setText("Застосувати");
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        applyBtn.setTypeface(AndroidUtilities.bold());
        applyBtn.setTextColor(0xFFFFFFFF);
        applyBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(AndroidUtilities.dp(10));
        btnBg.setColor(0xFF7052FF);
        applyBtn.setBackground(btnBg);
        row.addView(applyBtn);

        card.addView(row);
        card.setOnClickListener(onClick);
        return card;
    }

    /* =========================================================================
     * TAB 1: CHAT BUBBLES STUDIO
     * ========================================================================= */

    private View createBubblesTab(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(layout);

        gradientSwitch = new TextCheckCell(context);
        gradientSwitch.setTextAndCheck(MiogramLocale.get("Градієнт вихідних бульбашок", "Градиент исходящих пузырьков", "Outgoing Bubble Gradient"),
                MiogramCustomUiPrefs.isBubbleGradientEnabled(), true);
        gradientSwitch.setOnClickListener(v -> {
            boolean newVal = !gradientSwitch.isChecked();
            gradientSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setBubbleGradientEnabled(newVal);
            livePreview.invalidate();
        });
        layout.addView(gradientSwitch);

        // Dual Color Pickers
        LinearLayout colorsRow = new LinearLayout(context);
        colorsRow.setOrientation(LinearLayout.HORIZONTAL);
        colorsRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        colorsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText("Кольори градієнта (C1 / C2):");
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        colorsRow.addView(label, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        View c1 = createColorChip(context, MiogramCustomUiPrefs.getBubbleColor1(), color -> {
            MiogramCustomUiPrefs.setBubbleColor1(color);
            livePreview.invalidate();
        });
        View c2 = createColorChip(context, MiogramCustomUiPrefs.getBubbleColor2(), color -> {
            MiogramCustomUiPrefs.setBubbleColor2(color);
            livePreview.invalidate();
        });
        colorsRow.addView(c1);
        colorsRow.addView(c2, LayoutHelper.createLinear(32, 32, 10, 0, 0, 0));
        layout.addView(colorsRow);

        // Angle Slider
        LinearLayout angleRow = new LinearLayout(context);
        angleRow.setOrientation(LinearLayout.VERTICAL);
        angleRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

        LinearLayout angleHeader = new LinearLayout(context);
        TextView aTitle = new TextView(context);
        aTitle.setText("Кут нахилу градієнта");
        aTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        angleValueText = new TextView(context);
        angleValueText.setText(MiogramCustomUiPrefs.getBubbleAngle() + "°");
        angleValueText.setTextColor(0xFF7052FF);
        angleHeader.addView(aTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        angleHeader.addView(angleValueText);
        angleRow.addView(angleHeader);

        angleSeekBar = new SeekBar(context);
        angleSeekBar.setMax(360);
        angleSeekBar.setProgress(MiogramCustomUiPrefs.getBubbleAngle());
        angleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MiogramCustomUiPrefs.setBubbleAngle(progress);
                    angleValueText.setText(progress + "°");
                    livePreview.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        angleRow.addView(angleSeekBar);
        layout.addView(angleRow);

        // Radius Slider
        LinearLayout radiusRow = new LinearLayout(context);
        radiusRow.setOrientation(LinearLayout.VERTICAL);
        radiusRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        LinearLayout radiusHeader = new LinearLayout(context);
        TextView rTitle = new TextView(context);
        rTitle.setText("Радіус кутів бульбашок");
        rTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        radiusValueText = new TextView(context);
        radiusValueText.setText(MiogramCustomUiPrefs.getBubbleRadius() + " dp");
        radiusValueText.setTextColor(0xFF7052FF);
        radiusHeader.addView(rTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        radiusHeader.addView(radiusValueText);
        radiusRow.addView(radiusHeader);

        radiusSeekBar = new SeekBar(context);
        radiusSeekBar.setMax(28); // 4..32 dp
        radiusSeekBar.setProgress(Math.max(0, MiogramCustomUiPrefs.getBubbleRadius() - 4));
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int r = progress + 4;
                    MiogramCustomUiPrefs.setBubbleRadius(r);
                    radiusValueText.setText(r + " dp");
                    livePreview.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        radiusRow.addView(radiusSeekBar);
        layout.addView(radiusRow);

        return layout;
    }

    /* =========================================================================
     * TAB 2: NAME & TEXT FX STUDIO
     * ========================================================================= */

    private View createNameFxTab(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(layout);

        layout.addView(createSectionHeader(context, "Шейдери та спецефекти імені"));

        String[] fxNames = {
                "⚪ Без ефектів", "🔥 Полум'я Вогню",
                "❄️ Крижаний Лід", "🌈 Анімована Веселка",
                "✨ Сяючий Блік", "⚡ Кіберпанк Неон",
                "🌌 Градієнт"
        };
        int[] fxIds = {
                MiogramUiEngine.FX_NONE, MiogramUiEngine.FX_FIRE,
                MiogramUiEngine.FX_ICE, MiogramUiEngine.FX_RAINBOW,
                MiogramUiEngine.FX_GLARE, MiogramUiEngine.FX_CYBER_NEON,
                MiogramUiEngine.FX_GRADIENT
        };

        for (int i = 0; i < fxNames.length; i++) {
            final int id = fxIds[i];
            boolean isCur = (MiogramCustomUiPrefs.getNameFx() == id);
            TextView item = new TextView(context);
            item.setText(fxNames[i] + (isCur ? "  ✓" : ""));
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            item.setTypeface(isCur ? AndroidUtilities.bold() : null);
            item.setTextColor(isCur ? 0xFF7052FF : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
            item.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            item.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                MiogramCustomUiPrefs.setNameFx(id);
                switchTab(2);
                livePreview.invalidate();
            });
            layout.addView(item);
        }

        // Name Glow Switch
        nameGlowSwitch = new TextCheckCell(context);
        nameGlowSwitch.setTextAndCheck("Неонове світіння навколо імені", MiogramCustomUiPrefs.isNameGlowEnabled(), true);
        nameGlowSwitch.setOnClickListener(v -> {
            boolean newVal = !nameGlowSwitch.isChecked();
            nameGlowSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setNameGlowEnabled(newVal);
            livePreview.invalidate();
        });
        layout.addView(nameGlowSwitch);

        // Glow Color & Radius
        LinearLayout glowColorRow = new LinearLayout(context);
        glowColorRow.setOrientation(LinearLayout.HORIZONTAL);
        glowColorRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        glowColorRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView gLabel = new TextView(context);
        gLabel.setText("Колір світіння:");
        gLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        glowColorRow.addView(gLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        View gChip = createColorChip(context, MiogramCustomUiPrefs.getNameGlowColor(), c -> {
            MiogramCustomUiPrefs.setNameGlowColor(c);
            livePreview.invalidate();
        });
        glowColorRow.addView(gChip);
        layout.addView(glowColorRow);

        // Typography Selector
        LinearLayout fontRow = new LinearLayout(context);
        fontRow.setOrientation(LinearLayout.HORIZONTAL);
        fontRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        fontRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout fontTexts = new LinearLayout(context);
        fontTexts.setOrientation(LinearLayout.VERTICAL);
        TextView fTitle = new TextView(context);
        fTitle.setText("Шрифт імені (Typography)");
        fTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        fontSubtitle = new TextView(context);
        fontSubtitle.setText(getFontName(MiogramCustomUiPrefs.getNameFont()));
        fontSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fontSubtitle.setTextColor(0xFF7052FF);
        fontTexts.addView(fTitle);
        fontTexts.addView(fontSubtitle);
        fontRow.addView(fontTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        fontRow.setOnClickListener(v -> showFontDialog());
        layout.addView(fontRow);

        return layout;
    }

    /* =========================================================================
     * TAB 3: AVATARS & RINGS STUDIO
     * ========================================================================= */

    private View createAvatarsTab(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(layout);

        layout.addView(createSectionHeader(context, "Геометрія форми аватара"));

        String[] shapes = {
                "⚪ Круглий (Classic Circle)", "⬛ Сквіркл (iOS Modern Squircle)",
                "🔲 Закруглений прямокутник", "⬡ Гексагон (Cyber Polygon)",
                "⭐ Зірка (8-point Star)", "💎 Діамант (Diamond Shape)"
        };
        int[] sIds = {
                MiogramUiEngine.SHAPE_CIRCLE, MiogramUiEngine.SHAPE_SQUIRCLE,
                MiogramUiEngine.SHAPE_ROUNDED_RECT, MiogramUiEngine.SHAPE_HEXAGON,
                MiogramUiEngine.SHAPE_STAR, MiogramUiEngine.SHAPE_DIAMOND
        };

        for (int i = 0; i < shapes.length; i++) {
            final int id = sIds[i];
            boolean isCur = (MiogramCustomUiPrefs.getAvatarShape() == id);
            TextView item = new TextView(context);
            item.setText(shapes[i] + (isCur ? "  ✓" : ""));
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            item.setTypeface(isCur ? AndroidUtilities.bold() : null);
            item.setTextColor(isCur ? 0xFF7052FF : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
            item.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            item.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                MiogramCustomUiPrefs.setAvatarShape(id);
                switchTab(3);
                livePreview.invalidate();
            });
            layout.addView(item);
        }

        // Glowing Ring Switch
        avatarRingSwitch = new TextCheckCell(context);
        avatarRingSwitch.setTextAndCheck("Сяюче неонове кільце аватара", MiogramCustomUiPrefs.isAvatarRingEnabled(), true);
        avatarRingSwitch.setOnClickListener(v -> {
            boolean newVal = !avatarRingSwitch.isChecked();
            avatarRingSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setAvatarRingEnabled(newVal);
            livePreview.invalidate();
        });
        layout.addView(avatarRingSwitch);

        // Pulse Animation
        avatarPulseSwitch = new TextCheckCell(context);
        avatarPulseSwitch.setTextAndCheck("Пульсуюча анімація сяйва", MiogramCustomUiPrefs.isAvatarRingPulse(), true);
        avatarPulseSwitch.setOnClickListener(v -> {
            boolean newVal = !avatarPulseSwitch.isChecked();
            avatarPulseSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setAvatarRingPulse(newVal);
            livePreview.invalidate();
        });
        layout.addView(avatarPulseSwitch);

        // Ring Color
        LinearLayout ringColorRow = new LinearLayout(context);
        ringColorRow.setOrientation(LinearLayout.HORIZONTAL);
        ringColorRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        ringColorRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView rcLabel = new TextView(context);
        rcLabel.setText("Колір кільця:");
        rcLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        ringColorRow.addView(rcLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        View rcChip = createColorChip(context, MiogramCustomUiPrefs.getAvatarRingColor(), c -> {
            MiogramCustomUiPrefs.setAvatarRingColor(c);
            livePreview.invalidate();
        });
        ringColorRow.addView(rcChip);
        layout.addView(ringColorRow);

        // Ring Width Slider
        LinearLayout rwRow = new LinearLayout(context);
        rwRow.setOrientation(LinearLayout.VERTICAL);
        rwRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        LinearLayout rwHeader = new LinearLayout(context);
        TextView rwTitle = new TextView(context);
        rwTitle.setText("Товщина кільця");
        rwTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        ringWidthValueText = new TextView(context);
        ringWidthValueText.setText(MiogramCustomUiPrefs.getAvatarRingWidth() + " dp");
        ringWidthValueText.setTextColor(0xFF7052FF);
        rwHeader.addView(rwTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        rwHeader.addView(ringWidthValueText);
        rwRow.addView(rwHeader);

        ringWidthSeekBar = new SeekBar(context);
        ringWidthSeekBar.setMax(6); // 1..7 dp
        ringWidthSeekBar.setProgress((int) Math.max(0, MiogramCustomUiPrefs.getAvatarRingWidth() - 1));
        ringWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float w = progress + 1f;
                    MiogramCustomUiPrefs.setAvatarRingWidth(w);
                    ringWidthValueText.setText(w + " dp");
                    livePreview.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        rwRow.addView(ringWidthSeekBar);
        layout.addView(rwRow);

        return layout;
    }

    /* =========================================================================
     * TAB 4: INTERFACE & PERFORMANCE STUDIO
     * ========================================================================= */

    private View createInterfaceTab(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(layout);

        // Dialog List Style
        layout.addView(createClickRow(context, "Стиль списку діалогів", getDialogStyleName(MiogramCustomUiPrefs.getDialogStyle()), v -> {
            String[] s = {"Класичний список ⚪", "Окремі плаваючі картки 🪟", "Скляний Glassmorphism 💎"};
            showChoiceDialog("Стиль списку діалогів", s, which -> {
                MiogramCustomUiPrefs.setDialogStyle(which);
                switchTab(4);
            });
        }));

        // Unread Badge Style
        layout.addView(createClickRow(context, "Стиль бейджиків непрочитаних", getUnreadStyleName(MiogramCustomUiPrefs.getUnreadStyle()), v -> {
            String[] s = {"Класичний круг ⚪", "Елегантна пігулка (Pill) 💊", "Неонове сяйво (Neon Glow) ✨", "Мінімалістична крапка (Dot) 🔘"};
            showChoiceDialog("Стиль бейджиків", s, which -> {
                MiogramCustomUiPrefs.setUnreadStyle(which);
                switchTab(4);
            });
        }));

        // Header Style
        layout.addView(createClickRow(context, "Стиль ActionBar (Шапки)", getHeaderStyleName(MiogramCustomUiPrefs.getHeaderStyle()), v -> {
            String[] s = {"Стандартний (Material 3)", "Прозоре скло з розмиттям (Glass)", "Акцентний градієнт (Aurora)"};
            showChoiceDialog("Стиль верхньої панелі", s, which -> {
                MiogramCustomUiPrefs.setHeaderStyle(which);
                switchTab(4);
            });
        }));

        // Haptic Feedback
        layout.addView(createClickRow(context, "Тактильний відгук (Haptics)", getHapticName(MiogramCustomUiPrefs.getHapticLevel()), v -> {
            String[] s = {"М'який відгук", "Чіткий клік (Crisp)", "Делікатний", "Вимкнено"};
            showChoiceDialog("Тактильний відгук", s, which -> {
                MiogramCustomUiPrefs.setHapticLevel(which);
                switchTab(4);
            });
        }));

        // ProMotion 120Hz Switch
        proMotionSwitch = new TextCheckCell(context);
        proMotionSwitch.setTextAndCheck("ProMotion 120Hz / 144Hz плавний скрол", MiogramCustomUiPrefs.isProMotionLock(), true);
        proMotionSwitch.setOnClickListener(v -> {
            boolean newVal = !proMotionSwitch.isChecked();
            proMotionSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setProMotionLock(newVal);
        });
        layout.addView(proMotionSwitch);

        // Online Pulse Switch
        onlinePulseSwitch = new TextCheckCell(context);
        onlinePulseSwitch.setTextAndCheck("Пульсуючий індикатор «В мережі»", MiogramCustomUiPrefs.isOnlinePulseEnabled(), true);
        onlinePulseSwitch.setOnClickListener(v -> {
            boolean newVal = !onlinePulseSwitch.isChecked();
            onlinePulseSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setOnlinePulseEnabled(newVal);
        });
        layout.addView(onlinePulseSwitch);

        return layout;
    }

    /* =========================================================================
     * BOTTOM ACTIONS BAR
     * ========================================================================= */

    private View createBottomBar(Context context) {
        FrameLayout bar = new FrameLayout(context);
        bar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        bar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

        TextView applyBtn = new TextView(context);
        applyBtn.setText("Застосувати у додатку ✨");
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        applyBtn.setTypeface(AndroidUtilities.bold());
        applyBtn.setTextColor(0xFFFFFFFF);
        applyBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(AndroidUtilities.dp(14));
        btnBg.setColor(0xFF7052FF);
        applyBtn.setBackground(btnBg);

        applyBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            Theme.reloadWallpaper();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Усі стилі Custom UI успішно активовано у чатах! ໒꒱").show();
        });

        bar.addView(applyBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return bar;
    }

    /* =========================================================================
     * LIVE INTERACTIVE CANVAS PREVIEW
     * ========================================================================= */

    private static class LivePreviewView extends View {
        private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        LivePreviewView(Context context) {
            super(context);
            textPaint.setTextSize(AndroidUtilities.dp(13.5f));
            namePaint.setTextSize(AndroidUtilities.dp(14f));
            namePaint.setTypeface(AndroidUtilities.bold());
            ringPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();

            // Background Container Card
            rect.set(0, 0, w, h);
            cardPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), cardPaint);

            // 1. Avatar Preview (Left)
            float avSize = AndroidUtilities.dp(44);
            float avLeft = AndroidUtilities.dp(16);
            float avTop = AndroidUtilities.dp(16);
            rect.set(avLeft, avTop, avLeft + avSize, avTop + avSize);

            int shape = MiogramCustomUiPrefs.getAvatarShape();
            Path path = MiogramUiEngine.getAvatarShapePath(rect, shape);
            canvas.save();
            canvas.clipPath(path);
            fillPaint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    0xFF7052FF, 0xFF00D2FF, Shader.TileMode.CLAMP));
            canvas.drawPaint(fillPaint);
            canvas.restore();

            // Glowing Neon Ring
            MiogramUiEngine.drawAvatarGlowRing(canvas, rect);

            // 2. Sender Name with Name FX Shaders & Glow
            float nameX = avLeft + avSize + AndroidUtilities.dp(12);
            float nameY = avTop + AndroidUtilities.dp(14);
            String nameStr = "Miogram Studio ໒꒱";
            float nameW = namePaint.measureText(nameStr);

            MiogramUiEngine.applyNameEffect(namePaint, (int) nameW, Theme.getColor(Theme.key_chat_messageLinkIn));
            canvas.drawText(nameStr, nameX, nameY, namePaint);
            MiogramUiEngine.restoreNameEffect(namePaint);

            // 3. Incoming Message Bubble
            float b1Top = nameY + AndroidUtilities.dp(10);
            String msg1 = "Як тобі новий дизайн чату? ໒꒱";
            float m1W = textPaint.measureText(msg1);
            rect.set(nameX, b1Top, nameX + m1W + AndroidUtilities.dp(24), b1Top + AndroidUtilities.dp(36));

            fillPaint.setShader(null);
            fillPaint.setColor(Theme.getColor(Theme.key_chat_inBubble));
            float radius = AndroidUtilities.dp(MiogramCustomUiPrefs.getBubbleRadius());
            canvas.drawRoundRect(rect, radius, radius, fillPaint);
            textPaint.setColor(Theme.getColor(Theme.key_chat_messageTextIn));
            canvas.drawText(msg1, rect.left + AndroidUtilities.dp(12), rect.top + AndroidUtilities.dp(23), textPaint);

            // 4. Outgoing Message Bubble with Gradient Shaders & Corner Radius
            float b2Top = rect.bottom + AndroidUtilities.dp(12);
            String msg2 = "Виглядає просто неймовірно! 🔥";
            float m2W = textPaint.measureText(msg2);
            float b2Right = w - AndroidUtilities.dp(16);
            rect.set(b2Right - m2W - AndroidUtilities.dp(26), b2Top, b2Right, b2Top + AndroidUtilities.dp(36));

            if (MiogramCustomUiPrefs.isBubbleGradientEnabled()) {
                Rect rInt = new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
                fillPaint.setShader(MiogramUiEngine.createGradient(rInt,
                        MiogramCustomUiPrefs.getBubbleColor1(), MiogramCustomUiPrefs.getBubbleColor2(),
                        MiogramCustomUiPrefs.getBubbleAngle()));
            } else {
                fillPaint.setShader(null);
                fillPaint.setColor(MiogramCustomUiPrefs.getBubbleColor1());
            }
            canvas.drawRoundRect(rect, radius, radius, fillPaint);
            textPaint.setColor(MiogramCustomUiPrefs.getBubbleTextColor());
            canvas.drawText(msg2, rect.left + AndroidUtilities.dp(13), rect.top + AndroidUtilities.dp(23), textPaint);

            // Loop animation for live shader waves
            postInvalidateDelayed(33);
        }
    }

    /* =========================================================================
     * HELPERS & DIALOGS
     * ========================================================================= */

    private TextView createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, getResourceProvider()));
        tv.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(6));
        return tv;
    }

    private View createClickRow(Context context, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView tView = new TextView(context);
        tView.setText(title);
        tView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        TextView sView = new TextView(context);
        sView.setText(subtitle);
        sView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sView.setTextColor(0xFF7052FF);
        texts.addView(tView);
        texts.addView(sView);

        row.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.msg_arrowright);
        arrow.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, getResourceProvider()));
        row.addView(arrow);

        row.setOnClickListener(onClick);
        return row;
    }

    private View createColorChip(Context context, int initialColor, org.telegram.messenger.Utilities.Callback<Integer> callback) {
        View chip = new View(context);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(initialColor);
        d.setStroke(AndroidUtilities.dp(2), 0xFFFFFFFF);
        chip.setBackground(d);
        chip.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(32), AndroidUtilities.dp(32)));

        int[] colors = {0xFFFF007F, 0xFF00F0FF, 0xFF7052FF, 0xFF00D2FF, 0xFFFF5E3A, 0xFFFF2A68, 0xFF00B09B, 0xFF96C93D, 0xFFFFD700, 0xFFFFFFFF, 0xFF161618};
        chip.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(context);
            b.setTitle("Оберіть колір");
            String[] names = {"Neon Pink", "Cyber Cyan", "Electric Violet", "Sky Blue", "Coral Orange", "Rose Gold", "Mint Frost", "Lime Green", "Gold Royalty", "Pure White", "AMOLED Black"};
            b.setItems(names, (dialog, which) -> {
                int c = colors[which];
                d.setColor(c);
                callback.run(c);
            });
            showDialog(b.create());
        });
        return chip;
    }

    private void applyCardBackground(View view) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        view.setBackground(bg);
    }

    private void showChoiceDialog(String title, String[] items, org.telegram.messenger.Utilities.Callback<Integer> callback) {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(title);
        b.setItems(items, (dialog, which) -> callback.run(which));
        showDialog(b.create());
    }

    private void showFontDialog() {
        String[] fonts = {"Roboto (Стандартний)", "Rounded Modern", "Cyber Monospace", "Elegant Serif", "Casual Script"};
        showChoiceDialog("Шрифт імені", fonts, which -> {
            MiogramCustomUiPrefs.setNameFont(which);
            switchTab(2);
            livePreview.invalidate();
        });
    }

    private void showResetDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Скидання налаштувань");
        b.setMessage("Скинути всі стилі Custom UI Studio до початкових параметрів?");
        b.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            MiogramCustomUiPrefs.resetDefaults();
            switchTab(activeTab);
            livePreview.invalidate();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Параметри успішно скинуто").show();
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private String getFontName(int font) {
        switch (font) {
            case 1: return "Rounded Modern";
            case 2: return "Cyber Monospace";
            case 3: return "Elegant Serif";
            case 4: return "Casual Script";
            default: return "Roboto (Стандартний)";
        }
    }

    private String getDialogStyleName(int style) {
        switch (style) {
            case 1: return "Окремі плаваючі картки 🪟";
            case 2: return "Скляний Glassmorphism 💎";
            default: return "Класичний список ⚪";
        }
    }

    private String getUnreadStyleName(int style) {
        switch (style) {
            case 1: return "Елегантна пігулка (Pill) 💊";
            case 2: return "Неонове сяйво (Neon Glow) ✨";
            case 3: return "Мінімалістична крапка (Dot) 🔘";
            default: return "Класичний круг ⚪";
        }
    }

    private String getHeaderStyleName(int style) {
        switch (style) {
            case 1: return "Прозоре скло з розмиттям (Glass)";
            case 2: return "Акцентний градієнт (Aurora)";
            default: return "Стандартний (Material 3)";
        }
    }

    private String getHapticName(int haptic) {
        switch (haptic) {
            case 1: return "Чіткий клік (Crisp)";
            case 2: return "Делікатний";
            case 3: return "Вимкнено";
            default: return "М'який відгук";
        }
    }
}
