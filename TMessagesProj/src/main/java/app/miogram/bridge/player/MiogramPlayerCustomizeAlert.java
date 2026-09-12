package app.miogram.bridge.player;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

public class MiogramPlayerCustomizeAlert extends BottomSheet {

    public interface OnDismissListener {
        void onDismiss();
    }

    private final MiogramModernPlayerLayout playerLayout;
    private final OnDismissListener dismissListener;

    public MiogramPlayerCustomizeAlert(Context context, Theme.ResourcesProvider resourcesProvider, MiogramModernPlayerLayout playerLayout, OnDismissListener dismissListener) {
        super(context, false, resourcesProvider);
        this.playerLayout = playerLayout;
        this.dismissListener = dismissListener;

        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        int bgColor = getThemedColor(Theme.key_dialogBackground);
        if (bgColor == 0) bgColor = 0xFF181A22;
        fixNavigationBar(bgColor);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(24));

        int accentColor = getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader);
        if (accentColor == 0) accentColor = 0xFF5B8DEF;
        int textColor = getThemedColor(Theme.key_dialogTextBlack);
        if (textColor == 0) textColor = 0xFFFFFFFF;
        int subTextColor = getThemedColor(Theme.key_dialogTextGray2);
        if (subTextColor == 0) subTextColor = 0xAAFFFFFF;

        // 1. Drag Handle
        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x44888888);
        handleBg.setCornerRadius(AndroidUtilities.dp(2.5f));
        dragHandle.setBackground(handleBg);
        root.addView(dragHandle, LayoutHelper.createLinear(38, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 2. Title & Subtitle
        TextView titleView = new TextView(context);
        titleView.setText(MiogramLocale.get("Кастомізація плеєра ✏️", "Кастомизация плеера ✏️", "Player Customization ✏️"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(textColor);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get("Змінюй фон, розмиття, яскравість, кнопки та текст пісні", "Меняй фон, размытие, яркость, кнопки и текст песни", "Customize background, blur, buttons and lyrics live"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(subTextColor);
        subtitleView.setGravity(Gravity.CENTER);
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // --- SECTION 1: ФОН ПЛЕЄРА (BACKGROUND) ---
        addSectionHeader(root, MiogramLocale.get("ФОН ПЛЕЄРА", "ФОН ПЛЕЕРА", "PLAYER BACKGROUND"), accentColor);

        // Mode Selector: [ Обкладинка | Градієнт | Суцільний | Прозоре скло ]
        LinearLayout modesRow = new LinearLayout(context);
        modesRow.setOrientation(LinearLayout.HORIZONTAL);
        modesRow.setGravity(Gravity.CENTER);
        modesRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(12));

        String[] modeLabels = new String[]{
                MiogramLocale.get("Обкладинка", "Обложка", "Cover"),
                MiogramLocale.get("Градієнт", "Градиент", "Gradient"),
                MiogramLocale.get("Суцільний", "Сплошной", "Solid"),
                MiogramLocale.get("Скло", "Стекло", "Glass"),
                MiogramLocale.get("Фото", "Фото", "Photo"),
                MiogramLocale.get("Відео", "Видео", "Video")
        };
        int[] modeValues = new int[]{
                MiogramPlayerPrefs.BG_MODE_COVER_BLUR,
                MiogramPlayerPrefs.BG_MODE_GRADIENT,
                MiogramPlayerPrefs.BG_MODE_SOLID,
                MiogramPlayerPrefs.BG_MODE_TRANSPARENT,
                MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO,
                MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO
        };

        TextView[] modeButtons = new TextView[modeValues.length];
        java.util.List<LinearLayout> modeRows = new java.util.ArrayList<>();
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            modeRows.add(row);
        }
        for (int i = 0; i < modeValues.length; i++) {
            final int modeVal = modeValues[i];
            TextView btn = new TextView(context);
            btn.setText(modeLabels[i]);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            btn.setTypeface(AndroidUtilities.bold());
            btn.setGravity(Gravity.CENTER);
            btn.setSingleLine(true);
            btn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            modeButtons[i] = btn;

            btn.setOnClickListener(v -> {
                MiogramHaptic.select(v);
                // Photo/Video without file -> open picker flow first.
                if (modeVal == MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO
                        && !MiogramPlayerPrefs.isCustomMediaValid(MiogramPlayerPrefs.getCustomPhotoPath())) {
                    openBackdropPicker(false);
                    return;
                }
                if (modeVal == MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO
                        && !MiogramPlayerPrefs.isCustomMediaValid(MiogramPlayerPrefs.getCustomVideoPath())) {
                    openBackdropPicker(true);
                    return;
                }
                MiogramPlayerPrefs.setBackgroundMode(modeVal);
                updateModeButtonStyles(modeButtons, modeValues, accentColor);
                if (playerLayout != null) playerLayout.applyCustomization();
            });
            modeRows.get(i / 3).addView(btn, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        }
        updateModeButtonStyles(modeButtons, modeValues, accentColor);
        for (LinearLayout row : modeRows) {
            row.setPadding(0, 0, 0, AndroidUtilities.dp(6));
            root.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        // Custom media: pick / status rows with live preview note.
        addMediaPickRow(root, context, false, accentColor, textColor, subTextColor);
        addMediaPickRow(root, context, true, accentColor, textColor, subTextColor);

        // Gradient direction: vertical / diagonal / horizontal.
        addSectionSubtitle(root, MiogramLocale.get("Напрям градієнта:", "Направление градиента:", "Gradient direction:"), subTextColor);
        LinearLayout orientRow = new LinearLayout(context);
        orientRow.setOrientation(LinearLayout.HORIZONTAL);
        orientRow.setGravity(Gravity.CENTER);
        String[] orientLabels = new String[]{
                MiogramLocale.get("Вертикаль", "Вертикаль", "Vertical"),
                MiogramLocale.get("Діагональ", "Диагональ", "Diagonal"),
                MiogramLocale.get("Горизонталь", "Горизонталь", "Horizontal")
        };
        TextView[] orientBtns = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int oi = i;
            TextView b = new TextView(context);
            b.setText(orientLabels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            b.setTypeface(AndroidUtilities.bold());
            b.setGravity(Gravity.CENTER);
            b.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(8), AndroidUtilities.dp(7));
            orientBtns[i] = b;
            b.setOnClickListener(v -> {
                MiogramHaptic.select(v);
                MiogramPlayerPrefs.setGradientOrientation(oi);
                MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_GRADIENT);
                updateModeButtonStyles(modeButtons, modeValues, accentColor);
                updateOrientStyles(orientBtns, accentColor);
                if (playerLayout != null) playerLayout.applyCustomization();
            });
            orientRow.addView(b, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        }
        updateOrientStyles(orientBtns, accentColor);
        root.addView(orientRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Presets chips for Gradient / Solid Colors
        addSectionSubtitle(root, MiogramLocale.get("Колірні пресети:", "Цветовые пресеты:", "Color presets:"), subTextColor);
        LinearLayout presetsRow = new LinearLayout(context);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setGravity(Gravity.CENTER);
        presetsRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(12));

        int[][] presets = new int[][]{
                {0xFF2D1B4E, 0xFF0D0B18, 0xFF171226}, // Cyber Violet
                {0xFF0E2F3A, 0xFF06141B, 0xFF0A1F26}, // Deep Ocean
                {0xFF3D1616, 0xFF140707, 0xFF220C0C}, // Crimson Glow
                {0xFF103322, 0xFF07170E, 0xFF0B2114}, // Emerald Matrix
                {0xFF2E2E3A, 0xFF14141A, 0xFF1E1E26}, // Midnight Slate
                {0xFF101010, 0xFF050505, 0xFF080808}  // OLED Pure Black
        };

        for (int[] p : presets) {
            FrameLayout chip = new FrameLayout(context);
            GradientDrawable chipBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{p[0], p[1]});
            chipBg.setShape(GradientDrawable.OVAL);
            chipBg.setStroke(AndroidUtilities.dp(2), 0x44FFFFFF);
            chip.setBackground(chipBg);
            chip.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                MiogramPlayerPrefs.setGradientColor1(p[0]);
                MiogramPlayerPrefs.setGradientColor2(p[1]);
                MiogramPlayerPrefs.setSolidColor(p[2]);
                // Preset implies gradient — switch so user instantly sees result.
                MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_GRADIENT);
                updateModeButtonStyles(modeButtons, modeValues, accentColor);
                if (playerLayout != null) playerLayout.applyCustomization();
            });
            presetsRow.addView(chip, LayoutHelper.createLinear(36, 36, Gravity.CENTER, 6, 0, 6, 0));
        }
        root.addView(presetsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Slider: Прозорість фону (Opacity)
        addSliderRow(root, context, MiogramLocale.get("Прозорість фону", "Прозрачность фона", "Background opacity"),
                (int) (MiogramPlayerPrefs.getBgOpacity() * 100), 10, 100, "%", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setBgOpacity(val / 100.0f);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Slider: Розмиття (Blur radius)
        addSliderRow(root, context, MiogramLocale.get("Сила розмиття (Blur)", "Сила размытия (Blur)", "Blur strength"),
                MiogramPlayerPrefs.getBgBlur(), 0, 30, " px", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setBgBlur(val);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Slider: Яскравість фону (Brightness)
        addSliderRow(root, context, MiogramLocale.get("Яскравість фону", "Яркость фона", "Background brightness"),
                (int) (MiogramPlayerPrefs.getBgBrightness() * 100), 20, 160, "%", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setBgBrightness(val / 100.0f);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // --- SECTION 2: КНОПКИ ТА ЕЛЕМЕНТИ ---
        addSectionHeader(root, MiogramLocale.get("КНОПКИ ТА КЕРУВАННЯ", "КНОПКИ И УПРАВЛЕНИЕ", "BUTTONS & CONTROLS"), accentColor);
        addSectionSubtitle(root, MiogramLocale.get("Центр (⏮ ▶ ⏭) завжди по центру. Стрілки ← → рухають зайві кнопки ліворуч/праворуч, око — ховає.", "Центр (⏮ ▶ ⏭) всегда по центру. Стрелки ← → двигают лишние кнопки влево/вправо, глаз — прячет.", "Center (⏮ ▶ ⏭) stays centered. Arrows move extra buttons left/right, eye hides."), subTextColor);
        LinearLayout orderBox = new LinearLayout(context);
        orderBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(orderBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
        rebuildOrderBox(orderBox, context, accentColor, textColor, subTextColor);

        // Slider: Прозорість кнопок
        addSliderRow(root, context, MiogramLocale.get("Прозорість кнопок", "Прозрачность кнопок", "Button opacity"),
                (int) (MiogramPlayerPrefs.getButtonOpacity() * 100), 40, 100, "%", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setButtonOpacity(val / 100.0f);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Toggle: Неонове світіння / тінь кнопок
        addToggleRow(root, context, MiogramLocale.get("Неонове сяйво кнопки Play", "Неоновое сияние кнопки Play", "Play button neon glow"),
                MiogramPlayerPrefs.isButtonGlowEnabled(), accentColor, textColor, enabled -> {
                    MiogramPlayerPrefs.setButtonGlowEnabled(enabled);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Toggle: Візуалайзер (стрибаючі полоски) — за замовчуванням ВИМКНЕНО
        addToggleRow(root, context, MiogramLocale.get("Візуалайзер (полоски)", "Визуалайзер (полоски)", "Bass visualizer bars"),
                MiogramPlayerPrefs.isVisualizerEnabled(), accentColor, textColor, enabled -> {
                    MiogramPlayerPrefs.setVisualizerEnabled(enabled);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Toggle: Кнопка "Додати в профіль" (можна прибрати / повернути, компактна по центру)
        addToggleRow(root, context, MiogramLocale.get("Кнопка «В профіль»", "Кнопка «В профиль»", "Add-to-profile button"),
                MiogramPlayerPrefs.isProfileButtonEnabled(), accentColor, textColor, enabled -> {
                    MiogramPlayerPrefs.setProfileButtonEnabled(enabled);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // --- SECTION 3: ТЕКСТ ПІСНІ (LYRICS) ---
        addSectionHeader(root, MiogramLocale.get("ТЕКСТ ПІСНІ (LYRICS)", "ТЕКСТ ПЕСНИ (LYRICS)", "LYRICS STYLING"), accentColor);

        // Slider: Розмір шрифту тексту
        addSliderRow(root, context, MiogramLocale.get("Розмір шрифту тексту", "Размер шрифта текста", "Lyrics font size"),
                MiogramPlayerPrefs.getLyricsFontSize(), 14, 28, " sp", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setLyricsFontSize(val);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Toggle: Сяйво активного рядка
        addToggleRow(root, context, MiogramLocale.get("Сяйво активного рядка", "Сияние активной строки", "Active line glow effect"),
                MiogramPlayerPrefs.isLyricsActiveGlow(), accentColor, textColor, enabled -> {
                    MiogramPlayerPrefs.setLyricsActiveGlow(enabled);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // Slider: Прозорість неактивного тексту
        addSliderRow(root, context, MiogramLocale.get("Прозорість фонового тексту", "Прозрачность фонового текста", "Inactive lines opacity"),
                (int) (MiogramPlayerPrefs.getLyricsInactiveOpacity() * 100), 20, 90, "%", accentColor, textColor, val -> {
                    MiogramPlayerPrefs.setLyricsInactiveOpacity(val / 100.0f);
                    if (playerLayout != null) playerLayout.applyCustomization();
                });

        // --- SECTION 4: ДІЇ (ACTIONS) ---
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        // Reset Button
        TextView resetBtn = new TextView(context);
        resetBtn.setText(MiogramLocale.get("Скинути", "Сбросить", "Reset"));
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        resetBtn.setTextColor(0xFFE55757);
        resetBtn.setTypeface(AndroidUtilities.bold());
        resetBtn.setGravity(Gravity.CENTER);
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(0x1AE55757);
        resetBg.setCornerRadius(AndroidUtilities.dp(16));
        resetBtn.setBackground(resetBg);
        resetBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            MiogramPlayerPrefs.resetToDefaults();
            if (playerLayout != null) playerLayout.applyCustomization();
            dismiss();
        });
        actionsRow.addView(resetBtn, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(44), 1.0f));

        View spacer = new View(context);
        actionsRow.addView(spacer, LayoutHelper.createLinear(12, LayoutHelper.MATCH_PARENT));

        // Done Button
        TextView doneBtn = new TextView(context);
        doneBtn.setText(MiogramLocale.get("Готово ✨", "Готово ✨", "Done ✨"));
        doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        doneBtn.setTextColor(0xFFFFFFFF);
        doneBtn.setTypeface(AndroidUtilities.bold());
        doneBtn.setGravity(Gravity.CENTER);
        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setColor(accentColor);
        doneBg.setCornerRadius(AndroidUtilities.dp(16));
        doneBtn.setBackground(doneBg);
        doneBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            dismiss();
        });
        actionsRow.addView(doneBtn, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(44), 1.4f));

        root.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(root);
        setCustomView(scrollView);
    }

    private void addSectionHeader(LinearLayout parent, String title, int accentColor) {
        TextView tv = new TextView(parent.getContext());
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(accentColor);
        tv.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(14), 0, AndroidUtilities.dp(6));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addSectionSubtitle(LinearLayout parent, String text, int color) {
        TextView tv = new TextView(parent.getContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        tv.setTextColor(color);
        tv.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), 0, AndroidUtilities.dp(4));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void updateModeButtonStyles(TextView[] buttons, int[] values, int accentColor) {
        int currentMode = MiogramPlayerPrefs.getBackgroundMode();
        for (int i = 0; i < buttons.length; i++) {
            boolean active = values[i] == currentMode;
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(14));
            if (active) {
                gd.setColor(accentColor);
                buttons[i].setTextColor(0xFFFFFFFF);
            } else {
                gd.setColor(0x18FFFFFF);
                buttons[i].setTextColor(0xAAFFFFFF);
            }
            buttons[i].setBackground(gd);
        }
    }

    public interface OnValueChangedListener {
        void onValueChanged(int value);
    }

    private void addSliderRow(LinearLayout parent, Context context, String label, int currentVal, int minVal, int maxVal, String suffix, int accentColor, int textColor, OnValueChangedListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(6));

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        labelView.setTextColor(textColor);
        top.addView(labelView, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView valueView = new TextView(context);
        valueView.setText(currentVal + suffix);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setTextColor(accentColor);
        top.addView(valueView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        row.addView(top, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(maxVal - minVal);
        seekBar.setProgress(Math.max(0, currentVal - minVal));
        if (Build.VERSION.SDK_INT >= 21) {
            seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
            seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                int actualVal = minVal + p;
                valueView.setText(actualVal + suffix);
                if (fromUser && listener != null) {
                    listener.onValueChanged(actualVal);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        row.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 2));

        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public interface OnToggleListener {
        void onToggle(boolean enabled);
    }

    private void addToggleRow(LinearLayout parent, Context context, String label, boolean currentVal, int accentColor, int textColor, OnToggleListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(8));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        labelView.setTextColor(textColor);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView toggleBtn = new TextView(context);
        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        toggleBtn.setTypeface(AndroidUtilities.bold());
        toggleBtn.setGravity(Gravity.CENTER);
        toggleBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        final boolean[] state = new boolean[]{currentVal};
        updateToggleStyle(toggleBtn, state[0], accentColor);

        toggleBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            state[0] = !state[0];
            updateToggleStyle(toggleBtn, state[0], accentColor);
            if (listener != null) listener.onToggle(state[0]);
        });

        row.addView(toggleBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void updateToggleStyle(TextView tv, boolean active, int accentColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(14));
        if (active) {
            gd.setColor(accentColor);
            tv.setTextColor(0xFFFFFFFF);
            tv.setText(MiogramLocale.get("Увімкнено", "Включено", "Enabled"));
        } else {
            gd.setColor(0x22888888);
            tv.setTextColor(0xAAFFFFFF);
            tv.setText(MiogramLocale.get("Вимкнено", "Выключено", "Disabled"));
        }
        tv.setBackground(gd);
    }

    private void updateOrientStyles(TextView[] btns, int accentColor) {
        int cur = MiogramPlayerPrefs.getGradientOrientation();
        for (int i = 0; i < btns.length; i++) {
            boolean active = i == cur;
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(12));
            if (active) {
                gd.setColor(accentColor);
                btns[i].setTextColor(0xFFFFFFFF);
            } else {
                gd.setColor(0x18FFFFFF);
                btns[i].setTextColor(0xAAFFFFFF);
            }
            btns[i].setBackground(gd);
        }
    }

    private void addMediaPickRow(LinearLayout parent, Context context, boolean isVideo, int accentColor, int textColor, int subTextColor) {
        String path = isVideo ? MiogramPlayerPrefs.getCustomVideoPath() : MiogramPlayerPrefs.getCustomPhotoPath();
        boolean valid = MiogramPlayerPrefs.isCustomMediaValid(path);
        String label = isVideo
                ? MiogramLocale.get("Відео-фон", "Видео-фон", "Video background")
                : MiogramLocale.get("Фото-фон", "Фото-фон", "Photo background");
        String state = valid
                ? MiogramLocale.get("✓ обрано — тап щоб змінити", "✓ выбрано — тап чтобы сменить", "✓ set — tap to change")
                : MiogramLocale.get("не обрано", "не выбрано", "not set");

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));

        TextView info = new TextView(context);
        info.setText(label + " · " + state);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        info.setTextColor(valid ? textColor : subTextColor);
        row.addView(info, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView pick = new TextView(context);
        pick.setText(MiogramLocale.get("Обрати…", "Выбрать…", "Pick…"));
        pick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        pick.setTypeface(AndroidUtilities.bold());
        pick.setGravity(Gravity.CENTER);
        pick.setTextColor(0xFFFFFFFF);
        GradientDrawable pgd = new GradientDrawable();
        pgd.setCornerRadius(AndroidUtilities.dp(12));
        pgd.setColor(accentColor);
        pick.setBackground(pgd);
        pick.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        pick.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            openBackdropPicker(isVideo);
        });
        row.addView(pick, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        if (valid) {
            TextView clear = new TextView(context);
            clear.setText("✕");
            clear.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            clear.setTypeface(AndroidUtilities.bold());
            clear.setGravity(Gravity.CENTER);
            clear.setTextColor(0xFFE55757);
            GradientDrawable cgd = new GradientDrawable();
            cgd.setCornerRadius(AndroidUtilities.dp(12));
            cgd.setColor(0x1AE55757);
            clear.setBackground(cgd);
            clear.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
            clear.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (isVideo) MiogramPlayerPrefs.setCustomVideoPath("");
                else MiogramPlayerPrefs.setCustomPhotoPath("");
                if (playerLayout != null) playerLayout.applyCustomization();
                dismiss();
            });
            row.addView(clear, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
        }
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void openBackdropPicker(boolean isVideo) {
        try {
            org.telegram.ui.ActionBar.BaseFragment frag = org.telegram.ui.LaunchActivity.getLastFragment();
            if (frag != null) {
                android.os.Bundle args = new android.os.Bundle();
                args.putString(MiogramPlayerBackdropPicker.ARG_MODE, isVideo ? "video" : "photo");
                MiogramPlayerBackdropPicker picker = new MiogramPlayerBackdropPicker();
                picker.setArguments(args);
                // Close sheet so user returns straight to player with live result (no stale buttons).
                try {
                    dismiss();
                } catch (Throwable ignore) {}
                frag.presentFragment(picker);
            }
        } catch (Throwable ignore) {}
    }

    private String controlName(String id) {
        if (id.equals("shuffle")) return MiogramLocale.get("Shuffle", "Shuffle", "Shuffle");
        if (id.equals("repeat")) return MiogramLocale.get("Repeat", "Repeat", "Repeat");
        if (id.equals("prev")) return "⏮ Prev";
        if (id.equals("play")) return "▶ Play";
        if (id.equals("next")) return "Next ⏭";
        if (id.equals("queue")) return MiogramLocale.get("Queue", "Queue", "Queue");
        return id;
    }

    private void rebuildOrderBox(LinearLayout box, Context context, int accentColor, int textColor, int subTextColor) {
        box.removeAllViews();
        java.util.List<String> order = MiogramPlayerPrefs.getControlsOrderList();
        for (int i = 0; i < order.size(); i++) {
            final String id = order.get(i);
            boolean locked = id.equals("prev") || id.equals("play") || id.equals("next");
            boolean hidden = !locked && MiogramPlayerPrefs.isControlHidden(id);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(3), AndroidUtilities.dp(4), AndroidUtilities.dp(3));

            TextView name = new TextView(context);
            name.setText((locked ? "🔒 " : (hidden ? "👁‍🗨 " : "• ")) + controlName(id));
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            name.setTextColor(hidden ? subTextColor : textColor);
            row.addView(name, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            if (!locked) {
                TextView left = new TextView(context);
                left.setText("←");
                left.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                left.setTypeface(AndroidUtilities.bold());
                left.setGravity(Gravity.CENTER);
                left.setTextColor(textColor);
                left.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
                left.setAlpha(i == 0 ? 0.3f : 1f);
                left.setOnClickListener(v -> {
                    MiogramHaptic.select(v);
                    MiogramPlayerPrefs.moveControl(id, -1);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox(box, context, accentColor, textColor, subTextColor);
                });
                row.addView(left, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

                TextView right = new TextView(context);
                right.setText("→");
                right.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                right.setTypeface(AndroidUtilities.bold());
                right.setGravity(Gravity.CENTER);
                right.setTextColor(textColor);
                right.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
                right.setAlpha(i == order.size() - 1 ? 0.3f : 1f);
                right.setOnClickListener(v -> {
                    MiogramHaptic.select(v);
                    MiogramPlayerPrefs.moveControl(id, 1);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox(box, context, accentColor, textColor, subTextColor);
                });
                row.addView(right, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

                TextView eye = new TextView(context);
                eye.setText(hidden ? "🚫" : "👁");
                eye.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                eye.setGravity(Gravity.CENTER);
                eye.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
                eye.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MiogramPlayerPrefs.setControlHidden(id, !hidden);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox(box, context, accentColor, textColor, subTextColor);
                });
                row.addView(eye, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            } else {
                TextView lock = new TextView(context);
                lock.setText(MiogramLocale.get("центр", "центр", "center"));
                lock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
                lock.setTextColor(subTextColor);
                lock.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(4), 0);
                row.addView(lock, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }
            box.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        TextView reset = new TextView(context);
        reset.setText(MiogramLocale.get("↺ Повернути стандартний порядок", "↺ Вернуть стандартный порядок", "↺ Reset default order"));
        reset.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        reset.setTextColor(accentColor);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        reset.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            MiogramPlayerPrefs.resetControlsLayout();
            if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
            rebuildOrderBox(box, context, accentColor, textColor, subTextColor);
        });
        box.addView(reset, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }
}
