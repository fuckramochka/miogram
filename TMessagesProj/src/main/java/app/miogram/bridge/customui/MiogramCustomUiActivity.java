package app.miogram.bridge.customui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Miogram Custom UI Studio.
 * Crafted 1-to-1 matching the architecture, visual language, and granularity of Custom Profile.
 * Provides complete, uncompromising creative control over message bubbles, name shaders,
 * avatar geometries, story rings, and client UI.
 */
public class MiogramCustomUiActivity extends BaseFragment {

    public static final int TAB_BUBBLES = 0;
    public static final int TAB_NAMES = 1;
    public static final int TAB_AVATARS = 2;
    public static final int TAB_INTERFACE = 3;

    private int activeTab = TAB_BUBBLES;

    private LinearLayout tabRail;
    private FrameLayout previewContainer;
    private FrameLayout contentContainer;

    // Active previews
    private BubblePreview bubblePreview;
    private NamePreview namePreview;
    private AvatarPreview avatarPreview;
    private DialogPreview dialogPreview;

    // Groups for dynamic visibility
    private final Map<String, List<View>> groups = new HashMap<>();

    @Override
    public View createView(Context context) {
        // 1. Action Bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Додаткові функції");
        actionBar.setSubtitle("Кастомне оформлення");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // 2. Root layout
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        // 3. Navigation Rail (Tabs)
        HorizontalScrollView tabScroll = new HorizontalScrollView(context);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        tabRail = new LinearLayout(context);
        tabRail.setOrientation(LinearLayout.HORIZONTAL);
        tabRail.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        tabScroll.addView(tabRail, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buildTabRail(context);
        root.addView(tabScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Thin separator
        View div = new View(context);
        div.setBackgroundColor(Theme.getColor(Theme.key_divider));
        root.addView(div, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // 4. Scrollable Container
        ScrollView mainScroll = new ScrollView(context);
        mainScroll.setFillViewport(true);
        root.addView(mainScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(scrollContent, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 5. Live Preview Container
        previewContainer = new FrameLayout(context);
        previewContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        scrollContent.addView(previewContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 6. Settings Content Container
        contentContainer = new FrameLayout(context);
        scrollContent.addView(contentContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        switchTab(activeTab);

        return fragmentView;
    }

    private void buildTabRail(Context context) {
        tabRail.removeAllViews();
        String[] titles = {
                "💬 Пухирці",
                "✨ Ім'я та текст",
                "👤 Аватари",
                "📱 Інтерфейс"
        };

        for (int i = 0; i < titles.length; i++) {
            final int tabIndex = i;
            TextView tab = new TextView(context);
            tab.setText(titles[i]);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
            tab.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            tab.setGravity(Gravity.CENTER);
            tab.setSingleLine(true);

            boolean isCur = (i == activeTab);
            if (isCur) {
                tab.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
                tab.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                pill.setCornerRadius(AndroidUtilities.dp(16));
                tab.setBackground(pill);
            } else {
                tab.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.NORMAL);
                tab.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                tab.setBackground(null);
            }

            tab.setOnClickListener(v -> {
                performHaptic();
                switchTab(tabIndex);
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
            tabRail.addView(tab, lp);
        }
    }

    private void switchTab(int index) {
        activeTab = index;
        if (getContext() == null) return;
        buildTabRail(getContext());

        previewContainer.removeAllViews();
        contentContainer.removeAllViews();
        groups.clear();

        Context context = getContext();
        switch (activeTab) {
            case TAB_BUBBLES:
                bubblePreview = new BubblePreview(context);
                previewContainer.addView(bubblePreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 168));
                contentContainer.addView(buildBubblesSettings(context));
                break;
            case TAB_NAMES:
                namePreview = new NamePreview(context);
                previewContainer.addView(namePreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 96));
                contentContainer.addView(buildNamesSettings(context));
                break;
            case TAB_AVATARS:
                avatarPreview = new AvatarPreview(context);
                previewContainer.addView(avatarPreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120));
                contentContainer.addView(buildAvatarsSettings(context));
                break;
            case TAB_INTERFACE:
                dialogPreview = new DialogPreview(context);
                previewContainer.addView(dialogPreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 96));
                contentContainer.addView(buildInterfaceSettings(context));
                break;
        }
    }

    /* =========================================================================
     * 1. TAB: BUBBLES SETTINGS (Exact 1-to-1 with ExtraFeatures.java)
     * ========================================================================= */

    private View buildBubblesSettings(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);

        // Header
        box.addView(createHeader(context, "ПУХИРЕЦЬ СВОЇХ ПОВІДОМЛЕНЬ"));

        // Check: Custom color enabled
        TextCheckCell checkColor = createCheck(context, "Свій колір пухирця", MiogramCustomUiPrefs.isBubbleColorEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setBubbleColorEnabled(checked);
            updateBubbleVisibility();
            invalidateAll();
        });
        box.addView(checkColor);

        // Check: Gradient enabled
        TextCheckCell checkGrad = createCheck(context, "Градиент", MiogramCustomUiPrefs.isBubbleGradientEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setBubbleGradientEnabled(checked);
            updateBubbleVisibility();
            invalidateAll();
        });
        box.addView(checkGrad);

        // Group SOLID: Color
        final EditColorRow[] solidRow = new EditColorRow[1];
        solidRow[0] = new EditColorRow(context, "Колір пухирця", MiogramCustomUiPrefs.getBubbleColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір пухирця", MiogramCustomUiPrefs.getBubbleColor(), color -> {
                MiogramCustomUiPrefs.setBubbleColor(color);
                if (solidRow[0] != null) solidRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_SOLID", solidRow[0]);
        box.addView(solidRow[0]);

        // Group GRAD: Color 1, Color 2, Angle
        final EditColorRow[] c1Row = new EditColorRow[1];
        c1Row[0] = new EditColorRow(context, "Перший колір", MiogramCustomUiPrefs.getBubbleColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Перший колір", MiogramCustomUiPrefs.getBubbleColor(), color -> {
                MiogramCustomUiPrefs.setBubbleColor(color);
                if (c1Row[0] != null) c1Row[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_GRAD", c1Row[0]);
        box.addView(c1Row[0]);

        final EditColorRow[] c2Row = new EditColorRow[1];
        c2Row[0] = new EditColorRow(context, "Другий колір", MiogramCustomUiPrefs.getBubbleColor2(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Другий колір", MiogramCustomUiPrefs.getBubbleColor2(), color -> {
                MiogramCustomUiPrefs.setBubbleColor2(color);
                if (c2Row[0] != null) c2Row[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_GRAD", c2Row[0]);
        box.addView(c2Row[0]);

        View angleSlider = createSlider(context, "Напрямок", MiogramCustomUiPrefs.getBubbleGradAngle(), 0, 360, "°", angle -> {
            MiogramCustomUiPrefs.setBubbleGradAngle(angle);
            invalidateAll();
        });
        addToGroup("G_GRAD", angleSlider);
        box.addView(angleSlider);

        // Header: Geometry & Typography
        box.addView(createHeader(context, "ГЕОМЕТРІЯ ТА ТЕКСТ"));

        // Corner Radius
        View radiusSlider = createSlider(context, "Скруглення кутів", MiogramCustomUiPrefs.getBubbleRadius(), 4, 32, " dp", r -> {
            MiogramCustomUiPrefs.setBubbleRadius(r);
            invalidateAll();
        });
        box.addView(radiusSlider);

        // Text Color
        final EditColorRow[] textRow = new EditColorRow[1];
        textRow[0] = new EditColorRow(context, "Колір тексту", MiogramCustomUiPrefs.getBubbleTextColor(), false, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір тексту", MiogramCustomUiPrefs.getBubbleTextColor(), color -> {
                MiogramCustomUiPrefs.setBubbleTextColor(color);
                if (textRow[0] != null) textRow[0].setColor(color);
                invalidateAll();
            });
        });
        box.addView(textRow[0]);

        // Header: Glow
        box.addView(createHeader(context, "СВІТІННЯ"));

        TextCheckCell glowCheck = createCheck(context, "Світіння пухирця", MiogramCustomUiPrefs.isBubbleGlowEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setBubbleGlowEnabled(checked);
            setGroupVisible("G_BUBBLE_GLOW", checked);
            invalidateAll();
        });
        box.addView(glowCheck);

        final EditColorRow[] bGlowRow = new EditColorRow[1];
        bGlowRow[0] = new EditColorRow(context, "Колір світіння", MiogramCustomUiPrefs.getBubbleGlowColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір світіння", MiogramCustomUiPrefs.getBubbleGlowColor(), color -> {
                MiogramCustomUiPrefs.setBubbleGlowColor(color);
                if (bGlowRow[0] != null) bGlowRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_BUBBLE_GLOW", bGlowRow[0]);
        box.addView(bGlowRow[0]);

        View bGlowSlider = createSlider(context, "Радіус світіння", MiogramCustomUiPrefs.getBubbleGlowRadius(), 0, 30, "", r -> {
            MiogramCustomUiPrefs.setBubbleGlowRadius(r);
            invalidateAll();
        });
        addToGroup("G_BUBBLE_GLOW", bGlowSlider);
        box.addView(bGlowSlider);

        // Note
        box.addView(createNote(context, "Колір ваших повідомлень формується за допомогою прямого апаратного шейдера SRC_ATOP, який точно повторює геометрію векторних кутів та хвостиків Telegram."));

        updateBubbleVisibility();
        setGroupVisible("G_BUBBLE_GLOW", MiogramCustomUiPrefs.isBubbleGlowEnabled());

        return box;
    }

    private void updateBubbleVisibility() {
        boolean on = MiogramCustomUiPrefs.isBubbleColorEnabled();
        boolean grad = MiogramCustomUiPrefs.isBubbleGradientEnabled();
        setGroupVisible("G_SOLID", on && !grad);
        setGroupVisible("G_GRAD", on && grad);
    }

    /* =========================================================================
     * 2. TAB: NAME FX & TYPOGRAPHY (Exact 1-to-1 with EditNameSheet.java)
     * ========================================================================= */

    private View buildNamesSettings(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);

        // Header: Color
        box.addView(createHeader(context, "КОЛІР"));
        TextCheckCell checkColor = createCheck(context, "Свій колір імені", MiogramCustomUiPrefs.isNameColorEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setNameColorEnabled(checked);
            setGroupVisible("G_NAME_COLOR", checked);
            invalidateAll();
        });
        box.addView(checkColor);

        final EditColorRow[] nameColorRow = new EditColorRow[1];
        nameColorRow[0] = new EditColorRow(context, "Колір імені", MiogramCustomUiPrefs.getNameColor(), false, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір імені", MiogramCustomUiPrefs.getNameColor(), color -> {
                MiogramCustomUiPrefs.setNameColor(color);
                if (nameColorRow[0] != null) nameColorRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_NAME_COLOR", nameColorRow[0]);
        box.addView(nameColorRow[0]);

        // Header: Glow
        box.addView(createHeader(context, "СВІТІННЯ"));
        TextCheckCell checkGlow = createCheck(context, "Світіння імені", MiogramCustomUiPrefs.isNameGlowEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setNameGlowEnabled(checked);
            setGroupVisible("G_NAME_GLOW", checked);
            invalidateAll();
        });
        box.addView(checkGlow);

        final EditColorRow[] glowColorRow = new EditColorRow[1];
        glowColorRow[0] = new EditColorRow(context, "Колір світіння", MiogramCustomUiPrefs.getNameGlowColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір світіння", MiogramCustomUiPrefs.getNameGlowColor(), color -> {
                MiogramCustomUiPrefs.setNameGlowColor(color);
                if (glowColorRow[0] != null) glowColorRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_NAME_GLOW", glowColorRow[0]);
        box.addView(glowColorRow[0]);

        View glowRadiusSlider = createSlider(context, "Радіус", MiogramCustomUiPrefs.getNameGlowRadius(), 0, 40, "", r -> {
            MiogramCustomUiPrefs.setNameGlowRadius(r);
            invalidateAll();
        });
        addToGroup("G_NAME_GLOW", glowRadiusSlider);
        box.addView(glowRadiusSlider);

        View glowStrengthSlider = createSlider(context, "Сила", MiogramCustomUiPrefs.getNameGlowStrength(), 0, 100, "%", s -> {
            MiogramCustomUiPrefs.setNameGlowStrength(s);
            invalidateAll();
        });
        addToGroup("G_NAME_GLOW", glowStrengthSlider);
        box.addView(glowStrengthSlider);

        View glowNote = createNote(context, "Світіння створює м'який світловий ореол заданого радіуса без розмиття контуру літер.");
        addToGroup("G_NAME_GLOW", glowNote);
        box.addView(glowNote);

        // Header: Shadow
        box.addView(createHeader(context, "ТІНЬ"));
        TextCheckCell checkShadow = createCheck(context, "Тінь імені", MiogramCustomUiPrefs.isNameShadowEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setNameShadowEnabled(checked);
            setGroupVisible("G_NAME_SHADOW", checked);
            invalidateAll();
        });
        box.addView(checkShadow);

        final EditColorRow[] shadowColorRow = new EditColorRow[1];
        shadowColorRow[0] = new EditColorRow(context, "Колір тіні", MiogramCustomUiPrefs.getNameShadowColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір тіні", MiogramCustomUiPrefs.getNameShadowColor(), color -> {
                MiogramCustomUiPrefs.setNameShadowColor(color);
                if (shadowColorRow[0] != null) shadowColorRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_NAME_SHADOW", shadowColorRow[0]);
        box.addView(shadowColorRow[0]);

        View shadowBlurSlider = createSlider(context, "Розмиття", MiogramCustomUiPrefs.getNameShadowRadius(), 0, 40, "", b -> {
            MiogramCustomUiPrefs.setNameShadowRadius(b);
            invalidateAll();
        });
        addToGroup("G_NAME_SHADOW", shadowBlurSlider);
        box.addView(shadowBlurSlider);

        View shadowStrengthSlider = createSlider(context, "Сила", MiogramCustomUiPrefs.getNameShadowStrength(), 0, 100, "%", s -> {
            MiogramCustomUiPrefs.setNameShadowStrength(s);
            invalidateAll();
        });
        addToGroup("G_NAME_SHADOW", shadowStrengthSlider);
        box.addView(shadowStrengthSlider);

        View shadowDxSlider = createSlider(context, "Зсув убік", MiogramCustomUiPrefs.getNameShadowDx(), -20, 20, "", dx -> {
            MiogramCustomUiPrefs.setNameShadowDx(dx);
            invalidateAll();
        });
        addToGroup("G_NAME_SHADOW", shadowDxSlider);
        box.addView(shadowDxSlider);

        View shadowDySlider = createSlider(context, "Зсув вниз", MiogramCustomUiPrefs.getNameShadowDy(), -20, 20, "", dy -> {
            MiogramCustomUiPrefs.setNameShadowDy(dy);
            invalidateAll();
        });
        addToGroup("G_NAME_SHADOW", shadowDySlider);
        box.addView(shadowDySlider);

        View shadowNote = createNote(context, "Тінь і світіння ділять один пензель імені. Зсунута тінь створює ефект 3D-паріння напису.");
        addToGroup("G_NAME_SHADOW", shadowNote);
        box.addView(shadowNote);

        // Header: Effect
        box.addView(createHeader(context, "ЕФЕКТ"));
        String[] fxTitles = {"Ні", "Пульс", "Градиент", "Шиммер", "Райдуга", "Неон", "Вогонь", "Лід"};
        box.addView(createChooser(context, "Рух імені", MiogramCustomUiPrefs.getNameFx(), fxTitles, id -> {
            MiogramCustomUiPrefs.setNameFx(id);
            updateNameFxVisibility();
            invalidateAll();
        }));

        View speedSlider = createSlider(context, "Швидкість", MiogramCustomUiPrefs.getNameFxSpeed(), 10, 200, "%", s -> {
            MiogramCustomUiPrefs.setNameFxSpeed(s);
            invalidateAll();
        });
        addToGroup("G_NAME_SPEED", speedSlider);
        box.addView(speedSlider);

        final EditColorRow[] gradC1 = new EditColorRow[1];
        gradC1[0] = new EditColorRow(context, "Перший колір градієнта", MiogramCustomUiPrefs.getNameGradC1(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Перший колір градієнта", MiogramCustomUiPrefs.getNameGradC1(), color -> {
                MiogramCustomUiPrefs.setNameGradC1(color);
                if (gradC1[0] != null) gradC1[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_NAME_GRAD", gradC1[0]);
        box.addView(gradC1[0]);

        final EditColorRow[] gradC2 = new EditColorRow[1];
        gradC2[0] = new EditColorRow(context, "Другий колір градієнта", MiogramCustomUiPrefs.getNameGradC2(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Другий колір градієнта", MiogramCustomUiPrefs.getNameGradC2(), color -> {
                MiogramCustomUiPrefs.setNameGradC2(color);
                if (gradC2[0] != null) gradC2[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_NAME_GRAD", gradC2[0]);
        box.addView(gradC2[0]);

        View nameAngleSlider = createSlider(context, "Напрямок", MiogramCustomUiPrefs.getNameGradAngle(), 0, 360, "°", a -> {
            MiogramCustomUiPrefs.setNameGradAngle(a);
            invalidateAll();
        });
        addToGroup("G_NAME_ANGLE", nameAngleSlider);
        box.addView(nameAngleSlider);

        View angleNote = createNote(context, "Напрямок веде не лише градієнт: за ним же йдуть шиммер, райдуга, вогонь та лід.");
        addToGroup("G_NAME_ANGLE", angleNote);
        box.addView(angleNote);

        // Header: Typeface
        box.addView(createHeader(context, "НАКРЕСЛЕННЯ"));
        View sizeSlider = createSlider(context, "Розмір", MiogramCustomUiPrefs.getNameSize(), 50, 200, "%", s -> {
            MiogramCustomUiPrefs.setNameSize(s);
            invalidateAll();
        });
        box.addView(sizeSlider);

        String[] fonts = {"Стандарт", "Тонкий", "Засечки", "Моно", "Курсив", "Вузький"};
        box.addView(createChooser(context, "Шрифт", MiogramCustomUiPrefs.getNameFont(), fonts, f -> {
            MiogramCustomUiPrefs.setNameFont(f);
            invalidateAll();
        }));

        setGroupVisible("G_NAME_COLOR", MiogramCustomUiPrefs.isNameColorEnabled());
        setGroupVisible("G_NAME_GLOW", MiogramCustomUiPrefs.isNameGlowEnabled());
        setGroupVisible("G_NAME_SHADOW", MiogramCustomUiPrefs.isNameShadowEnabled());
        updateNameFxVisibility();

        return box;
    }

    private void updateNameFxVisibility() {
        int fx = MiogramCustomUiPrefs.getNameFx();
        setGroupVisible("G_NAME_SPEED", fx != 0);
        setGroupVisible("G_NAME_GRAD", fx == 2);
        setGroupVisible("G_NAME_ANGLE", fx >= 2 && fx != 5);
    }

    /* =========================================================================
     * 3. TAB: AVATAR & RINGS (Exact 1-to-1 with EditAvatarSheet.java)
     * ========================================================================= */

    private View buildAvatarsSettings(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);

        // Header: Shape
        box.addView(createHeader(context, "ФОРМА"));
        String[] shapes = {"Круг", "Скруглений", "Квадрат", "Шестикутник", "П'ятикутник", "Зірка", "Серце", "Квітка"};
        box.addView(createChooser(context, "Окреслення", MiogramCustomUiPrefs.getAvatarShape(), shapes, s -> {
            MiogramCustomUiPrefs.setAvatarShape(s);
            updateAvatarShapeVisibility();
            invalidateAll();
        }));

        View radSlider = createSlider(context, "Скруглення кутів", MiogramCustomUiPrefs.getAvatarRadius(), 0, 64, "", r -> {
            MiogramCustomUiPrefs.setAvatarRadius(r);
            invalidateAll();
        });
        addToGroup("G_AVATAR_RADIUS", radSlider);
        box.addView(radSlider);

        View roundSlider = createSlider(context, "Округлість", MiogramCustomUiPrefs.getAvatarRound(), 0, 100, "%", r -> {
            MiogramCustomUiPrefs.setAvatarRound(r);
            invalidateAll();
        });
        addToGroup("G_AVATAR_ROUND", roundSlider);
        box.addView(roundSlider);

        // Header: Ring
        box.addView(createHeader(context, "СЯЮЧЕ КІЛЬЦЕ"));
        TextCheckCell checkRing = createCheck(context, "Сяюче неонове кільце", MiogramCustomUiPrefs.isAvatarRingEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setAvatarRingEnabled(checked);
            setGroupVisible("G_AVATAR_RING", checked);
            invalidateAll();
        });
        box.addView(checkRing);

        final EditColorRow[] ringColorRow = new EditColorRow[1];
        ringColorRow[0] = new EditColorRow(context, "Колір кільця", MiogramCustomUiPrefs.getAvatarRingColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір кільця", MiogramCustomUiPrefs.getAvatarRingColor(), color -> {
                MiogramCustomUiPrefs.setAvatarRingColor(color);
                if (ringColorRow[0] != null) ringColorRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_AVATAR_RING", ringColorRow[0]);
        box.addView(ringColorRow[0]);

        View ringWidthSlider = createSlider(context, "Товщина обводки", MiogramCustomUiPrefs.getAvatarRingWidth(), 1, 10, " dp", w -> {
            MiogramCustomUiPrefs.setAvatarRingWidth(w);
            invalidateAll();
        });
        addToGroup("G_AVATAR_RING", ringWidthSlider);
        box.addView(ringWidthSlider);

        TextCheckCell pulseCheck = createCheck(context, "Пульсуюче дихання", MiogramCustomUiPrefs.isAvatarRingPulse(), false, checked -> {
            MiogramCustomUiPrefs.setAvatarRingPulse(checked);
            invalidateAll();
        });
        addToGroup("G_AVATAR_RING", pulseCheck);
        box.addView(pulseCheck);

        // Header: Appearance
        box.addView(createHeader(context, "ВИГЛЯД"));
        View alphaSlider = createSlider(context, "Прозорість", MiogramCustomUiPrefs.getAvatarAlpha(), 0, 100, "%", a -> {
            MiogramCustomUiPrefs.setAvatarAlpha(a);
            invalidateAll();
        });
        box.addView(alphaSlider);

        View dimSlider = createSlider(context, "Затемнення", MiogramCustomUiPrefs.getAvatarDim(), 0, 100, "%", d -> {
            MiogramCustomUiPrefs.setAvatarDim(d);
            invalidateAll();
        });
        box.addView(dimSlider);

        View fadeSlider = createSlider(context, "Розтушовування країв", MiogramCustomUiPrefs.getAvatarFade(), 0, 100, "%", f -> {
            MiogramCustomUiPrefs.setAvatarFade(f);
            invalidateAll();
        });
        box.addView(fadeSlider);

        View fadeRadSlider = createSlider(context, "Радіус розтушовування", MiogramCustomUiPrefs.getAvatarFadeRadius(), 0, 100, "%", r -> {
            MiogramCustomUiPrefs.setAvatarFadeRadius(r);
            invalidateAll();
        });
        box.addView(fadeRadSlider);

        box.addView(createNote(context, "Розтушовування розмиває край аватара за допомогою радіального маскування, надаючи фотографії ефекту розчинення у просторі."));

        updateAvatarShapeVisibility();
        setGroupVisible("G_AVATAR_RING", MiogramCustomUiPrefs.isAvatarRingEnabled());

        return box;
    }

    private void updateAvatarShapeVisibility() {
        int s = MiogramCustomUiPrefs.getAvatarShape();
        setGroupVisible("G_AVATAR_RADIUS", s == 1);
        setGroupVisible("G_AVATAR_ROUND", s != 0);
    }

    /* =========================================================================
     * 4. TAB: INTERFACE & DIALOGS
     * ========================================================================= */

    private View buildInterfaceSettings(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);

        // Header: Dialogs
        box.addView(createHeader(context, "КАРТКИ ДІАЛОГІВ"));
        TextCheckCell checkGlass = createCheck(context, "Скляний блюр панелей", MiogramCustomUiPrefs.isGlassBlurEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setGlassBlurEnabled(checked);
            invalidateAll();
        });
        box.addView(checkGlass);

        TextCheckCell checkCards = createCheck(context, "Відокремлені плаваючі картки", MiogramCustomUiPrefs.isDialogCardsEnabled(), false, checked -> {
            MiogramCustomUiPrefs.setDialogCardsEnabled(checked);
            invalidateAll();
        });
        box.addView(checkCards);

        // Header: Badges
        box.addView(createHeader(context, "НЕПРОЧИТАНІ БЕЙДЖІ"));
        TextCheckCell checkBadge = createCheck(context, "Свій стиль бейджів", MiogramCustomUiPrefs.isBadgeCustomEnabled(), true, checked -> {
            MiogramCustomUiPrefs.setBadgeCustomEnabled(checked);
            setGroupVisible("G_UI_BADGE", checked);
            invalidateAll();
        });
        box.addView(checkBadge);

        final EditColorRow[] badgeBgRow = new EditColorRow[1];
        badgeBgRow[0] = new EditColorRow(context, "Колір фону бейджа", MiogramCustomUiPrefs.getBadgeColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір фону бейджа", MiogramCustomUiPrefs.getBadgeColor(), color -> {
                MiogramCustomUiPrefs.setBadgeColor(color);
                if (badgeBgRow[0] != null) badgeBgRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_UI_BADGE", badgeBgRow[0]);
        box.addView(badgeBgRow[0]);

        final EditColorRow[] badgeTextRow = new EditColorRow[1];
        badgeTextRow[0] = new EditColorRow(context, "Колір цифр бейджа", MiogramCustomUiPrefs.getBadgeTextColor(), true, () -> {
            Context ctx = getParentActivity() != null ? getParentActivity() : context;
            EditColorPicker.show(ctx, "Колір цифр бейджа", MiogramCustomUiPrefs.getBadgeTextColor(), color -> {
                MiogramCustomUiPrefs.setBadgeTextColor(color);
                if (badgeTextRow[0] != null) badgeTextRow[0].setColor(color);
                invalidateAll();
            });
        });
        addToGroup("G_UI_BADGE", badgeTextRow[0]);
        box.addView(badgeTextRow[0]);

        TextCheckCell badgeGlowCheck = createCheck(context, "Неоновий ореол бейджа", MiogramCustomUiPrefs.isBadgeGlowEnabled(), false, checked -> {
            MiogramCustomUiPrefs.setBadgeGlowEnabled(checked);
            invalidateAll();
        });
        addToGroup("G_UI_BADGE", badgeGlowCheck);
        box.addView(badgeGlowCheck);

        // Header: Haptic
        box.addView(createHeader(context, "ТАКТИЛЬНІСТЬ"));
        TextCheckCell hapticCheck = createCheck(context, "Тактильний відгук (Haptic Feedback)", MiogramCustomUiPrefs.isHapticEnabled(), false, checked -> {
            MiogramCustomUiPrefs.setHapticEnabled(checked);
        });
        box.addView(hapticCheck);

        box.addView(createNote(context, "М'який приємний вібровідгук при натисканні кнопок, перемиканні тумблерів та русі повзунків налаштувань."));

        setGroupVisible("G_UI_BADGE", MiogramCustomUiPrefs.isBadgeCustomEnabled());

        return box;
    }

    /* =========================================================================
     * UI BUILDERS & HELPERS (1-to-1 with EditCells.java)
     * ========================================================================= */

    private HeaderCell createHeader(Context context, String text) {
        HeaderCell cell = new HeaderCell(context);
        cell.setText(text);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        return cell;
    }

    private TextCheckCell createCheck(Context context, String title, boolean checked, boolean divider, final ValueListener<Boolean> listener) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndCheck(title, checked, divider);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setOnClickListener(v -> {
            boolean newVal = !cell.isChecked();
            cell.setChecked(newVal);
            performHaptic();
            if (listener != null) {
                listener.onValue(newVal);
            }
        });
        return cell;
    }

    private TextInfoPrivacyCell createNote(Context context, String text) {
        TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
        cell.setText(text);
        return cell;
    }

    private View createSlider(Context context, final String title, int currentVal, final int min, final int max, final String unit, final ValueListener<Integer> listener) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        layout.setPadding(0, 0, 0, AndroidUtilities.dp(6));

        final TextView headerText = new TextView(context);
        headerText.setText(formatSliderLabel(title, currentVal, unit));
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        headerText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerText.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(12), AndroidUtilities.dp(21), AndroidUtilities.dp(4));
        layout.addView(headerText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        SeekBarView seekBar = new SeekBarView(context);
        seekBar.setReportChanges(true);
        float progress = (max <= min) ? 0f : (float) (currentVal - min) / (float) (max - min);
        seekBar.setProgress(progress);
        seekBar.setDelegate((stop, p) -> {
            int val = min + Math.round(p * (max - min));
            headerText.setText(formatSliderLabel(title, val, unit));
            if (listener != null) {
                listener.onValue(val);
            }
        });
        layout.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 12, 0, 12, 0));

        return layout;
    }

    private String formatSliderLabel(String title, int val, String unit) {
        return title + " — " + val + unit;
    }

    private View createChooser(Context context, String title, int curId, String[] items, final ValueListener<Integer> listener) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            final int id = i;
            TextView chip = new TextView(context);
            chip.setText(items[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);

            styleChip(chip, (i == curId));
            chips.add(chip);

            chip.setOnClickListener(v -> {
                performHaptic();
                for (int j = 0; j < chips.size(); j++) {
                    styleChip(chips.get(j), (j == id));
                }
                if (listener != null) {
                    listener.onValue(id);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            row.addView(chip, lp);
        }

        layout.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return layout;
    }

    private void styleChip(TextView chip, boolean active) {
        if (active) {
            chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
            chip.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            gd.setCornerRadius(AndroidUtilities.dp(16));
            chip.setBackground(gd);
        } else {
            chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.NORMAL);
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            gd.setCornerRadius(AndroidUtilities.dp(16));
            chip.setBackground(gd);
        }
    }

    private void addToGroup(String groupName, View view) {
        List<View> list = groups.get(groupName);
        if (list == null) {
            list = new ArrayList<>();
            groups.put(groupName, list);
        }
        list.add(view);
    }

    private void setGroupVisible(String groupName, boolean visible) {
        List<View> list = groups.get(groupName);
        if (list != null) {
            for (View v : list) {
                v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void performHaptic() {
        if (fragmentView != null && MiogramCustomUiPrefs.isHapticEnabled()) {
            fragmentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void invalidateAll() {
        if (bubblePreview != null) bubblePreview.invalidate();
        if (namePreview != null) namePreview.invalidate();
        if (avatarPreview != null) avatarPreview.invalidate();
        if (dialogPreview != null) dialogPreview.invalidate();

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    private interface ValueListener<T> {
        void onValue(T val);
    }

    /* =========================================================================
     * EDIT COLOR ROW (Exact 1-to-1 with EditColorRow.java from Custom Profile)
     * ========================================================================= */

    public static class EditColorRow extends FrameLayout {
        private final Band band;
        private final String title;
        private final TextCell textCell;

        public EditColorRow(Context context, String title, int color, boolean divider, final Runnable onClick) {
            super(context);
            this.title = title;
            setClickable(true);
            setFocusable(true);
            setBackground(Theme.getSelectorDrawable(false));

            textCell = new TextCell(context);
            textCell.setTextAndValue(title, "", divider);
            addView(textCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            band = new Band(context);
            band.setColor(color);
            band.setClickable(false);
            band.setFocusable(false);
            addView(band, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT));

            if (onClick != null) {
                setOnClickListener(v -> onClick.run());
                textCell.setOnClickListener(v -> onClick.run());
            }
        }

        public void setColor(int color) {
            band.setColor(color);
        }

        private static final class Band extends View {
            private int color;
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint fade = new Paint(Paint.ANTI_ALIAS_FLAG);
            private int shaderColor = 1;
            private int shaderWidth = -1;
            private int shaderHeight = -1;

            public Band(Context context) {
                super(context);
                fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            }

            public void setColor(int color) {
                if (this.color != color) {
                    this.color = color;
                    this.shaderWidth = -1;
                    invalidate();
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                int width = getWidth();
                int height = getHeight();
                if (width <= 0 || height <= 0) return;

                if (shaderWidth != width || shaderHeight != height || shaderColor != color) {
                    float f = width;
                    paint.setShader(new LinearGradient(0.3f * f, 0f, f, 0f, new int[]{color & 0x00FFFFFF, color}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
                    fade.setShader(new LinearGradient(0f, 0f, 0f, height, new int[]{0x00FFFFFF, -1, -1, 0x00FFFFFF}, new float[]{0f, 0.3f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                    shaderWidth = width;
                    shaderHeight = height;
                    shaderColor = color;
                }

                int save = canvas.saveLayer(0f, 0f, width, height, null);
                canvas.drawRect(0f, 0f, width, height, paint);
                canvas.drawRect(0f, 0f, width, height, fade);
                canvas.restoreToCount(save);
            }
        }
    }

    /* =========================================================================
     * EDIT COLOR PICKER (Exact 1-to-1 with EditColorPicker.java from Custom Profile)
     * ========================================================================= */

    public static class EditColorPicker {
        public interface ColorSink {
            void accept(int color);
        }

        static final class Alpha {
            private int rgb;
            private int value;

            Alpha(int a, int col) {
                this.value = Math.max(0, Math.min(255, a));
                this.rgb = 0x00FFFFFF & col;
            }

            int mix(int col) {
                this.rgb = 0x00FFFFFF & col;
                return this.rgb | (this.value << 24);
            }

            int with(int a) {
                this.value = Math.max(0, Math.min(255, a));
                return (this.value << 24) | this.rgb;
            }

            int percent() {
                return Math.round((this.value * 100.0f) / 255.0f);
            }
        }

        public static void show(Context context, String title, int currentColor, final ColorSink sink) {
            if (context == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(context, false);
            builder.setTitle(title, true);
            builder.setApplyBottomPadding(false);

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);

            final Alpha alpha = new Alpha(Color.alpha(currentColor), currentColor);

            ColorPicker picker = new ColorPicker(context, false, new ColorPicker.ColorPickerDelegate() {
                @Override
                public void setColor(int color, int num, boolean applyNow) {
                    if (sink != null) {
                        sink.accept(alpha.mix(color));
                    }
                }
            });
            picker.setType(-1, false, 1, 1, false, 0, false);
            picker.setColor(currentColor | 0xFF000000, 0);
            layout.addView(picker, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 340));

            // Opacity / Alpha Slider (Exact Custom Profile EditColorPicker.java)
            final TextView alphaHeader = new TextView(context);
            alphaHeader.setText("Прозорість — " + alpha.percent() + "%");
            alphaHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            alphaHeader.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            alphaHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            alphaHeader.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(8), AndroidUtilities.dp(21), AndroidUtilities.dp(4));
            layout.addView(alphaHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            SeekBarView seekBar = new SeekBarView(context);
            seekBar.setReportChanges(true);
            seekBar.setProgress(alpha.percent() / 100f);
            seekBar.setDelegate((stop, p) -> {
                int percent = Math.round(p * 100f);
                int newColor = alpha.with(Math.round(percent * 255f / 100f));
                alphaHeader.setText("Прозорість — " + percent + "%");
                if (sink != null) {
                    sink.accept(newColor);
                }
            });
            layout.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 12, 0, 12, 12));

            builder.setCustomView(layout);
            builder.show();
        }
    }

    /* =========================================================================
     * LIVE PREVIEWS (Exact 1-to-1 with BubblePreview.java from Custom Profile)
     * ========================================================================= */

    public static class BubblePreview extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        public BubblePreview(Context context) {
            super(context);
            text.setTextSize(AndroidUtilities.dpf2(14.5f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            boolean isDark = Theme.isCurrentThemeDark();
            fill.setShader(null);
            fill.setColor(isDark ? 0xFF171717 : 0xFFEBEBEB);
            rect.set(AndroidUtilities.dp(4), AndroidUtilities.dp(4), w - AndroidUtilities.dp(4), h - AndroidUtilities.dp(4));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), fill);

            // Incoming bubble
            float top = bubble(canvas, w, false, "Як тобі новий колір? 🎨", isDark ? 0xFF242424 : 0xFFFFFFFF, 0, 0, false, 0, isDark ? 0xFFFFFFFF : 0xFF000000, AndroidUtilities.dp(16));

            // Outgoing bubbles
            boolean on = MiogramCustomUiPrefs.isBubbleColorEnabled();
            boolean grad = MiogramCustomUiPrefs.isBubbleGradientEnabled();
            int c1 = MiogramCustomUiPrefs.getBubbleColor();
            int c2 = MiogramCustomUiPrefs.getBubbleColor2();
            int angle = MiogramCustomUiPrefs.getBubbleGradAngle();
            int textColor = MiogramCustomUiPrefs.getBubbleTextColor();

            int defBg = Theme.getColor(Theme.key_chat_outBubble);
            int baseC = on ? c1 : defBg;
            boolean isG = on && grad;

            float next = bubble(canvas, w, true, "Дивись, свій пухирець!", baseC, c1, c2, isG, angle, textColor, top + AndroidUtilities.dp(6));
            bubble(canvas, w, true, "І колір тексту теж 🔥", baseC, c1, c2, isG, angle, textColor, next + AndroidUtilities.dp(6));
        }

        private float bubble(Canvas canvas, int w, boolean isOut, String str, int bgColor, int c1, int c2, boolean isGrad, int angle, int textColor, float top) {
            float padH = AndroidUtilities.dp(12);
            float bubbleH = AndroidUtilities.dp(34);
            float strW = text.measureText(str) + padH * 2f;
            float left = isOut ? (w - AndroidUtilities.dp(18) - strW) : AndroidUtilities.dp(18);
            float right = left + strW;
            float bottom = top + bubbleH;

            rect.set(left, top, right, bottom);

            if (isGrad) {
                fill.setShader(MiogramUiEngine.createGradient(new Rect((int) left, (int) top, (int) right, (int) bottom), c1, c2, angle));
            } else {
                fill.setShader(null);
                fill.setColor(bgColor);
            }

            float rad = AndroidUtilities.dp(MiogramCustomUiPrefs.getBubbleRadius());
            canvas.drawRoundRect(rect, rad, rad, fill);
            fill.setShader(null);

            text.setColor(textColor);
            float baseline = top + bubbleH / 2f - (text.descent() + text.ascent()) / 2f;
            canvas.drawText(str, left + padH, baseline, text);

            return bottom;
        }
    }

    public static class NamePreview extends View {
        private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        public NamePreview(Context context) {
            super(context);
            paint.setTextSize(AndroidUtilities.dpf2(20));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            boolean isDark = Theme.isCurrentThemeDark();
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(isDark ? 0xFF171717 : 0xFFEBEBEB);
            RectF r = new RectF(AndroidUtilities.dp(4), AndroidUtilities.dp(4), w - AndroidUtilities.dp(4), h - AndroidUtilities.dp(4));
            canvas.drawRoundRect(r, AndroidUtilities.dp(16), AndroidUtilities.dp(16), bgPaint);

            String name = "✦ Alex [DEV] ✦";
            float tw = paint.measureText(name);
            float x = (w - tw) / 2f;
            float y = h / 2f - (paint.descent() + paint.ascent()) / 2f;

            MiogramUiEngine.applyNameEffect(paint, (int) tw, Theme.getColor(Theme.key_chat_name));
            canvas.drawText(name, x, y, paint);
            MiogramUiEngine.restoreNameEffect(paint);

            if (MiogramCustomUiPrefs.getNameFx() > 0) {
                postInvalidateOnAnimation();
            }
        }
    }

    public static class AvatarPreview extends View {
        private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF avatarRect = new RectF();

        public AvatarPreview(Context context) {
            super(context);
            avatarPaint.setColor(0xFF4A90E2);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            boolean isDark = Theme.isCurrentThemeDark();
            bgPaint.setColor(isDark ? 0xFF171717 : 0xFFEBEBEB);
            RectF r = new RectF(AndroidUtilities.dp(4), AndroidUtilities.dp(4), w - AndroidUtilities.dp(4), h - AndroidUtilities.dp(4));
            canvas.drawRoundRect(r, AndroidUtilities.dp(16), AndroidUtilities.dp(16), bgPaint);

            float cx = w / 2f;
            float cy = h / 2f;
            float size = AndroidUtilities.dp(64);
            avatarRect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);

            int shape = MiogramCustomUiPrefs.getAvatarShape();
            int radius = MiogramCustomUiPrefs.getAvatarRadius();
            int roundness = MiogramCustomUiPrefs.getAvatarRound();

            Path shapeP = MiogramUiEngine.getAvatarShapePath(avatarRect, shape, radius, roundness);
            int save = canvas.save();
            canvas.clipPath(shapeP);

            int alpha = (int) (255 * (MiogramCustomUiPrefs.getAvatarAlpha() / 100f));
            avatarPaint.setAlpha(alpha);
            canvas.drawRect(avatarRect, avatarPaint);
            canvas.restoreToCount(save);

            if (MiogramCustomUiPrefs.isAvatarRingEnabled()) {
                MiogramUiEngine.drawAvatarGlowRing(canvas, avatarRect);
                if (MiogramCustomUiPrefs.isAvatarRingPulse()) {
                    postInvalidateOnAnimation();
                }
            }
        }
    }

    public static class DialogPreview extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        public DialogPreview(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            boolean isDark = Theme.isCurrentThemeDark();
            bgPaint.setColor(isDark ? 0xFF171717 : 0xFFEBEBEB);
            RectF r = new RectF(AndroidUtilities.dp(4), AndroidUtilities.dp(4), w - AndroidUtilities.dp(4), h - AndroidUtilities.dp(4));
            canvas.drawRoundRect(r, AndroidUtilities.dp(16), AndroidUtilities.dp(16), bgPaint);

            // Dialog Card
            cardPaint.setColor(isDark ? 0xFF242424 : 0xFFFFFFFF);
            RectF card = new RectF(AndroidUtilities.dp(16), AndroidUtilities.dp(14), w - AndroidUtilities.dp(16), h - AndroidUtilities.dp(14));
            canvas.drawRoundRect(card, AndroidUtilities.dp(12), AndroidUtilities.dp(12), cardPaint);

            // Miniature Avatar
            RectF av = new RectF(card.left + AndroidUtilities.dp(12), card.top + AndroidUtilities.dp(12), card.left + AndroidUtilities.dp(52), card.top + AndroidUtilities.dp(52));
            int shape = MiogramCustomUiPrefs.getAvatarShape();
            Path avP = MiogramUiEngine.getAvatarShapePath(av, shape, MiogramCustomUiPrefs.getAvatarRadius(), MiogramCustomUiPrefs.getAvatarRound());
            int save = canvas.save();
            canvas.clipPath(avP);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFF7052FF);
            canvas.drawRect(av, p);
            canvas.restoreToCount(save);

            if (MiogramCustomUiPrefs.isAvatarRingEnabled()) {
                MiogramUiEngine.drawAvatarGlowRing(canvas, av);
            }

            // Name
            textPaint.setTextSize(AndroidUtilities.dpf2(15));
            textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textPaint.setColor(isDark ? 0xFFFFFFFF : 0xFF000000);
            String title = "Mio Channel";
            float tw = textPaint.measureText(title);
            MiogramUiEngine.applyNameEffect(textPaint, (int) tw, textPaint.getColor());
            canvas.drawText(title, av.right + AndroidUtilities.dp(12), card.top + AndroidUtilities.dp(28), textPaint);
            MiogramUiEngine.restoreNameEffect(textPaint);

            // Badge
            if (MiogramCustomUiPrefs.isBadgeCustomEnabled()) {
                Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
                bp.setColor(MiogramCustomUiPrefs.getBadgeColor());
                RectF bRect = new RectF(card.right - AndroidUtilities.dp(36), card.top + AndroidUtilities.dp(22), card.right - AndroidUtilities.dp(12), card.top + AndroidUtilities.dp(44));
                canvas.drawRoundRect(bRect, AndroidUtilities.dp(11), AndroidUtilities.dp(11), bp);

                textPaint.setColor(MiogramCustomUiPrefs.getBadgeTextColor());
                textPaint.setTextSize(AndroidUtilities.dpf2(12));
                canvas.drawText("5", bRect.centerX() - AndroidUtilities.dp(3), bRect.centerY() + AndroidUtilities.dp(4), textPaint);
            }
        }
    }
}
