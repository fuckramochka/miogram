package app.miogram.bridge.player;

import android.content.Context;
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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Per-element settings panel for player edit (jiggle) mode.
 * Opens ONLY the tapped element's section — never the full sheet.
 * Clean names, no emoji, same visual quality as the player itself.
 *
 * Sections: background, controls, lyrics, visualizer, profile, effects.
 */
public class MiogramPlayerSectionSheet extends BottomSheet {

    private static final int[] PALETTE = new int[]{
            0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFFF9500,
            0xFFFFCC00, 0xFF34C759, 0xFF00C7BE, 0xFF32ADE6,
            0xFF007AFF, 0xFF5856D6, 0xFFAF52DE, 0xFFFF2D55
    };

    private final MiogramModernPlayerLayout playerLayout;
    private final String section;
    private final int accentColor;
    private final int textColor;
    private final int subTextColor;

    private LinearLayout orderBox;
    private final List<View> grad1Swatches = new ArrayList<>();
    private final List<View> grad2Swatches = new ArrayList<>();
    private final List<View> glowSwatches = new ArrayList<>();
    private final List<View> lyricGlowSwatches = new ArrayList<>();
    private TextView mediaStatus;

    public MiogramPlayerSectionSheet(Context context, Theme.ResourcesProvider resourcesProvider,
                                     MiogramModernPlayerLayout playerLayout, String section) {
        super(context, false, resourcesProvider);
        this.playerLayout = playerLayout;
        this.section = section != null ? section : "controls";

        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        int bgColor = getThemedColor(Theme.key_dialogBackground);
        if (bgColor == 0) bgColor = 0xFF181A22;
        fixNavigationBar(bgColor);

        int accent = getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader);
        if (accent == 0) accent = 0xFF5B8DEF;
        accentColor = accent;
        int tc = getThemedColor(Theme.key_dialogTextBlack);
        textColor = tc == 0 ? 0xFFFFFFFF : tc;
        int stc = getThemedColor(Theme.key_dialogTextGray2);
        subTextColor = stc == 0 ? 0xAAFFFFFF : stc;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(24));

        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x44888888);
        handleBg.setCornerRadius(AndroidUtilities.dp(2.5f));
        dragHandle.setBackground(handleBg);
        root.addView(dragHandle, LayoutHelper.createLinear(38, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        TextView titleView = new TextView(context);
        titleView.setText(sectionTitle(this.section));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(textColor);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView subtitleView = new TextView(context);
        subtitleView.setText(sectionSubtitle(this.section));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(subTextColor);
        subtitleView.setGravity(Gravity.CENTER);
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        buildSection(root, context, this.section);

        // Footer: done only — no general settings, each panel is standalone.
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        TextView doneBtn = new TextView(context);
        doneBtn.setText(MiogramLocale.get("Готово", "Готово", "Done"));
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
        actionsRow.addView(doneBtn, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(44), 1.0f));

        root.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(root);
        setCustomView(scrollView);
    }

    private String sectionTitle(String s) {
        if ("background".equals(s)) return MiogramLocale.get("Фон", "Фон", "Background");
        if ("lyrics".equals(s)) return MiogramLocale.get("Текст пісні", "Текст песни", "Lyrics");
        if ("visualizer".equals(s)) return MiogramLocale.get("Візуалізатор", "Визуалайзер", "Visualizer");
        if ("profile".equals(s)) return MiogramLocale.get("Кнопка профілю", "Кнопка профиля", "Profile button");
        if ("effects".equals(s)) return MiogramLocale.get("Ефекти", "Эффекты", "Effects");
        return MiogramLocale.get("Кнопки", "Кнопки", "Controls");
    }

    private String sectionSubtitle(String s) {
        if ("background".equals(s)) return MiogramLocale.get("Фон, градієнт, фото або відео", "Фон, градиент, фото или видео", "Backdrop, gradient, photo or video");
        if ("lyrics".equals(s)) return MiogramLocale.get("Шрифт, сяйво та прозорість рядків", "Шрифт, сияние и прозрачность строк", "Font, glow and line opacity");
        if ("visualizer".equals(s)) return MiogramLocale.get("Смуги підскакують під бас", "Полосы прыгают под бас", "Bars bounce with bass");
        if ("profile".equals(s)) return MiogramLocale.get("Видимість та положення пігулки", "Видимость и положение пилюли", "Pill visibility and position");
        if ("effects".equals(s)) return MiogramLocale.get("Колір неонового сяйва", "Цвет неонового сияния", "Neon glow color");
        return MiogramLocale.get("Прозорість, порядок та видимість", "Прозрачность, порядок и видимость", "Opacity, order and visibility");
    }

    private void buildSection(LinearLayout root, Context context, String s) {
        if ("background".equals(s)) {
            buildBackground(root, context);
        } else if ("lyrics".equals(s)) {
            buildLyrics(root, context);
        } else if ("visualizer".equals(s)) {
            addToggleRow(root, context, MiogramLocale.get("Увімкнути візуалізатор", "Включить визуалайзер", "Enable visualizer"),
                    MiogramPlayerPrefs.isVisualizerEnabled(), enabled -> {
                        MiogramPlayerPrefs.setVisualizerEnabled(enabled);
                        apply();
                    });
            addNote(root, context, MiogramLocale.get("За замовчуванням вимкнено, щоб нічого не стрибало без потреби.",
                    "По умолчанию выключен, чтобы ничего не прыгало без нужды.",
                    "Off by default so nothing jumps without need."));
        } else if ("profile".equals(s)) {
            addToggleRow(root, context, MiogramLocale.get("Показувати кнопку", "Показывать кнопку", "Show button"),
                    MiogramPlayerPrefs.isProfileButtonEnabled(), enabled -> {
                        MiogramPlayerPrefs.setProfileButtonEnabled(enabled);
                        apply();
                    });
            addSegmentRow(root, context, MiogramLocale.get("Положення", "Положение", "Position"),
                    new String[]{
                            MiogramLocale.get("Ліво", "Лево", "Left"),
                            MiogramLocale.get("Центр", "Центр", "Center"),
                            MiogramLocale.get("Право", "Право", "Right")
                    }, MiogramPlayerPrefs.getProfileAlign(), index -> {
                        MiogramPlayerPrefs.setProfileAlign(index);
                        if (playerLayout != null) playerLayout.applyProfileAlign();
                    });
            addNote(root, context, MiogramLocale.get("Пігулку також можна тягнути пальцем прямо в плеєрі.",
                    "Пилюлю тоже можно тянуть пальцем прямо в плеере.",
                    "You can also drag the pill right in the player."));
        } else if ("effects".equals(s)) {
            addToggleRow(root, context, MiogramLocale.get("Неонове сяйво кнопки Play", "Неоновое сияние кнопки Play", "Play button neon glow"),
                    MiogramPlayerPrefs.isButtonGlowEnabled(), enabled -> {
                        MiogramPlayerPrefs.setButtonGlowEnabled(enabled);
                        apply();
                    });
            addLabel(root, context, MiogramLocale.get("Колір сяйва", "Цвет сияния", "Glow color"));
            addPaletteRow(root, context, 0, glowSwatches, color -> {
                MiogramPlayerPrefs.setButtonGlowColor(color);
                apply();
            });
            refreshPalette(glowSwatches, MiogramPlayerPrefs.getButtonGlowColor());
        } else {
            buildControls(root, context);
        }
    }

    // ---------------- Background ----------------

    private TextView[] bgModeButtons;
    private int[] bgModeValues;

    private void buildBackground(LinearLayout root, Context context) {
        String[] labels = new String[]{
                MiogramLocale.get("Обкладинка", "Обложка", "Cover"),
                MiogramLocale.get("Градієнт", "Градиент", "Gradient"),
                MiogramLocale.get("Суцільний", "Сплошной", "Solid"),
                MiogramLocale.get("Скло", "Стекло", "Glass"),
                MiogramLocale.get("Медіа", "Медиа", "Media")
        };
        bgModeValues = new int[]{
                MiogramPlayerPrefs.BG_MODE_COVER_BLUR,
                MiogramPlayerPrefs.BG_MODE_GRADIENT,
                MiogramPlayerPrefs.BG_MODE_SOLID,
                MiogramPlayerPrefs.BG_MODE_TRANSPARENT,
                -1
        };
        bgModeButtons = new TextView[labels.length];
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        for (int i = 0; i < labels.length; i++) {
            final int modeVal = bgModeValues[i];
            TextView btn = new TextView(context);
            btn.setText(labels[i]);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            btn.setTypeface(AndroidUtilities.bold());
            btn.setGravity(Gravity.CENTER);
            btn.setSingleLine(true);
            btn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            bgModeButtons[i] = btn;
            btn.setOnClickListener(v -> {
                MiogramHaptic.select(v);
                if (modeVal == -1) {
                    openMediaPicker();
                    return;
                }
                MiogramPlayerPrefs.setBackgroundMode(modeVal);
                refreshModeButtons();
                apply();
            });
            (i < 3 ? row1 : row2).addView(btn, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        }
        refreshModeButtons();
        root.addView(row1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
        root.addView(row2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        addLabel(root, context, MiogramLocale.get("Перший колір градієнта", "Первый цвет градиента", "Gradient first color"));
        addPaletteRow(root, context, MiogramPlayerPrefs.getGradientColor1(), grad1Swatches, color -> {
            MiogramPlayerPrefs.setGradientColor1(color);
            MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_GRADIENT);
            refreshModeButtons();
            refreshPalette(grad1Swatches, color);
            apply();
        });
        addLabel(root, context, MiogramLocale.get("Другий колір градієнта", "Второй цвет градиента", "Gradient second color"));
        addPaletteRow(root, context, MiogramPlayerPrefs.getGradientColor2(), grad2Swatches, color -> {
            MiogramPlayerPrefs.setGradientColor2(color);
            MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_GRADIENT);
            refreshModeButtons();
            refreshPalette(grad2Swatches, color);
            apply();
        });
        refreshPalette(grad1Swatches, MiogramPlayerPrefs.getGradientColor1());
        refreshPalette(grad2Swatches, MiogramPlayerPrefs.getGradientColor2());

        addSegmentRow(root, context, MiogramLocale.get("Напрям градієнта", "Направление градиента", "Gradient direction"),
                new String[]{
                        MiogramLocale.get("Вертикаль", "Вертикаль", "Vertical"),
                        MiogramLocale.get("Діагональ", "Диагональ", "Diagonal"),
                        MiogramLocale.get("Горизонталь", "Горизонталь", "Horizontal")
                }, MiogramPlayerPrefs.getGradientOrientation(), index -> {
                    MiogramPlayerPrefs.setGradientOrientation(index);
                    apply();
                });

        // Single photo/video button.
        LinearLayout mediaRow = new LinearLayout(context);
        mediaRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaRow.setGravity(Gravity.CENTER_VERTICAL);
        mediaRow.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(4));

        TextView info = new TextView(context);
        info.setText(MiogramLocale.get("Фото або відео на фон", "Фото или видео на фон", "Photo or video backdrop"));
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        info.setTextColor(textColor);
        mediaRow.addView(info, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView pick = new TextView(context);
        pick.setText(MiogramLocale.get("Обрати", "Выбрать", "Pick"));
        pick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        pick.setTypeface(AndroidUtilities.bold());
        pick.setGravity(Gravity.CENTER);
        pick.setTextColor(0xFFFFFFFF);
        GradientDrawable pickBg = new GradientDrawable();
        pickBg.setCornerRadius(AndroidUtilities.dp(12));
        pickBg.setColor(accentColor);
        pick.setBackground(pickBg);
        pick.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(7), AndroidUtilities.dp(16), AndroidUtilities.dp(7));
        pick.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            openMediaPicker();
        });
        mediaRow.addView(pick, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        root.addView(mediaRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        mediaStatus = new TextView(context);
        mediaStatus.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        mediaStatus.setTextColor(subTextColor);
        root.addView(mediaStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
        refreshMediaStatus();

        addSliderRow(root, context, MiogramLocale.get("Прозорість фону", "Прозрачность фона", "Background opacity"),
                (int) (MiogramPlayerPrefs.getBgOpacity() * 100), 10, 100, "%", val -> {
                    MiogramPlayerPrefs.setBgOpacity(val / 100.0f);
                    apply();
                });
        addSliderRow(root, context, MiogramLocale.get("Розмиття", "Размытие", "Blur"),
                MiogramPlayerPrefs.getBgBlur(), 0, 30, "", val -> {
                    MiogramPlayerPrefs.setBgBlur(val);
                    apply();
                });
        addSliderRow(root, context, MiogramLocale.get("Яскравість", "Яркость", "Brightness"),
                (int) (MiogramPlayerPrefs.getBgBrightness() * 100), 20, 160, "%", val -> {
                    MiogramPlayerPrefs.setBgBrightness(val / 100.0f);
                    apply();
                });
    }

    private void refreshModeButtons() {
        if (bgModeButtons == null) return;
        int currentMode = MiogramPlayerPrefs.getBackgroundMode();
        for (int i = 0; i < bgModeButtons.length; i++) {
            boolean active = bgModeValues[i] == currentMode
                    || (bgModeValues[i] == -1 && (currentMode == MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO
                    || currentMode == MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO));
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(12));
            if (active) {
                gd.setColor(accentColor);
                bgModeButtons[i].setTextColor(0xFFFFFFFF);
            } else {
                gd.setColor(0x18FFFFFF);
                bgModeButtons[i].setTextColor(0xAAFFFFFF);
            }
            bgModeButtons[i].setBackground(gd);
        }
    }

    private void refreshMediaStatus() {
        if (mediaStatus == null) return;
        boolean photoOk = MiogramPlayerPrefs.isCustomMediaValid(MiogramPlayerPrefs.getCustomPhotoPath());
        boolean videoOk = MiogramPlayerPrefs.isCustomMediaValid(MiogramPlayerPrefs.getCustomVideoPath());
        if (photoOk && videoOk) {
            mediaStatus.setText(MiogramLocale.get("Обрано фото і відео", "Выбраны фото и видео", "Photo and video set"));
        } else if (photoOk) {
            mediaStatus.setText(MiogramLocale.get("Обрано фото", "Выбрано фото", "Photo set"));
        } else if (videoOk) {
            mediaStatus.setText(MiogramLocale.get("Обрано відео без звуку", "Выбрано видео без звука", "Muted video set"));
        } else {
            mediaStatus.setText(MiogramLocale.get("Не обрано", "Не выбрано", "Not set"));
        }
    }

    private void openMediaPicker() {
        try {
            org.telegram.ui.ActionBar.BaseFragment frag = org.telegram.ui.LaunchActivity.getLastFragment();
            if (frag != null) {
                android.os.Bundle args = new android.os.Bundle();
                args.putString(MiogramPlayerBackdropPicker.ARG_MODE, "auto");
                MiogramPlayerBackdropPicker picker = new MiogramPlayerBackdropPicker(args);
                try {
                    dismiss();
                } catch (Throwable ignore) {}
                frag.presentFragment(picker);
            }
        } catch (Throwable ignore) {}
    }

    // ---------------- Controls ----------------

    private void buildControls(LinearLayout root, Context context) {
        addSliderRow(root, context, MiogramLocale.get("Прозорість кнопок", "Прозрачность кнопок", "Button opacity"),
                (int) (MiogramPlayerPrefs.getButtonOpacity() * 100), 40, 100, "%", val -> {
                    MiogramPlayerPrefs.setButtonOpacity(val / 100.0f);
                    apply();
                });
        addToggleRow(root, context, MiogramLocale.get("Неонове сяйво кнопки Play", "Неоновое сияние кнопки Play", "Play button neon glow"),
                MiogramPlayerPrefs.isButtonGlowEnabled(), enabled -> {
                    MiogramPlayerPrefs.setButtonGlowEnabled(enabled);
                    apply();
                });
        addLabel(root, context, MiogramLocale.get("Порядок кнопок", "Порядок кнопок", "Button order"));
        addNote(root, context, MiogramLocale.get("Центр завжди по центру. Кнопки також можна тягнути в плеєрі.",
                "Центр всегда по центру. Кнопки тоже можно тянуть в плеере.",
                "Center stays centered. Buttons can also be dragged in the player."));
        orderBox = new LinearLayout(context);
        orderBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(orderBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
        rebuildOrderBox();

        TextView reset = new TextView(context);
        reset.setText(MiogramLocale.get("Скинути порядок", "Сбросить порядок", "Reset order"));
        reset.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        reset.setTypeface(AndroidUtilities.bold());
        reset.setTextColor(accentColor);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        reset.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            MiogramPlayerPrefs.resetControlsLayout();
            if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
            rebuildOrderBox();
        });
        orderBox.addView(reset, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private String controlName(String id) {
        if ("shuffle".equals(id)) return MiogramLocale.get("Перемішати", "Перемешать", "Shuffle");
        if ("repeat".equals(id)) return MiogramLocale.get("Повтор", "Повтор", "Repeat");
        if ("prev".equals(id)) return MiogramLocale.get("Назад", "Назад", "Previous");
        if ("play".equals(id)) return MiogramLocale.get("Пауза", "Пауза", "Play");
        if ("next".equals(id)) return MiogramLocale.get("Далі", "Далее", "Next");
        if ("speed".equals(id)) return MiogramLocale.get("Швидкість", "Скорость", "Speed");
        if ("queue".equals(id)) return MiogramLocale.get("Черга", "Очередь", "Queue");
        return id;
    }

    private boolean isTrio(String id) {
        return "prev".equals(id) || "play".equals(id) || "next".equals(id);
    }

    private void rebuildOrderBox() {
        if (orderBox == null) return;
        Context context = orderBox.getContext();
        // Keep the reset row (last child) out of the way: rebuild all, re-add reset below by caller order.
        // Simpler: remove everything except we re-add reset at the end of buildControls only once.
        // Here we rebuild rows before the reset row.
        int resetIndex = orderBox.getChildCount() - 1;
        View resetRow = resetIndex >= 0 ? orderBox.getChildAt(resetIndex) : null;
        if (resetRow != null) orderBox.removeViewAt(resetIndex);
        // Remove old rows.
        orderBox.removeAllViews();

        java.util.List<String> order = MiogramPlayerPrefs.getControlsOrderList();
        for (int i = 0; i < order.size(); i++) {
            final String id = order.get(i);
            final boolean locked = isTrio(id);
            final boolean hidden = !locked && MiogramPlayerPrefs.isControlHidden(id);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

            TextView name = new TextView(context);
            name.setText((locked ? MiogramLocale.get("Центр", "Центр", "Center") + " · " : "") + controlName(id));
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            name.setTextColor(hidden ? subTextColor : textColor);
            row.addView(name, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            if (!locked) {
                TextView left = makeSmallBtn(context, "←");
                left.setAlpha(i == 0 ? 0.3f : 1f);
                left.setOnClickListener(v -> {
                    MiogramHaptic.select(v);
                    MiogramPlayerPrefs.moveControl(id, -1);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox();
                });
                row.addView(left, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

                TextView right = makeSmallBtn(context, "→");
                right.setText("→");
                right.setAlpha(i == order.size() - 1 ? 0.3f : 1f);
                right.setOnClickListener(v -> {
                    MiogramHaptic.select(v);
                    MiogramPlayerPrefs.moveControl(id, 1);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox();
                });
                row.addView(right, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

                TextView eye = makeSmallBtn(context, hidden
                        ? MiogramLocale.get("Показати", "Показать", "Show")
                        : MiogramLocale.get("Сховати", "Скрыть", "Hide"));
                eye.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MiogramPlayerPrefs.setControlHidden(id, !hidden);
                    if (playerLayout != null) playerLayout.rebuildControlsForPrefs();
                    rebuildOrderBox();
                });
                row.addView(eye, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }
            orderBox.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        if (resetRow != null) orderBox.addView(resetRow);
    }

    private TextView makeSmallBtn(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(textColor);
        tv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(5), AndroidUtilities.dp(12), AndroidUtilities.dp(5));
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(12));
        gd.setColor(0x18FFFFFF);
        tv.setBackground(gd);
        return tv;
    }

    // ---------------- Lyrics ----------------

    private void buildLyrics(LinearLayout root, Context context) {
        addSliderRow(root, context, MiogramLocale.get("Розмір шрифту", "Размер шрифта", "Font size"),
                MiogramPlayerPrefs.getLyricsFontSize(), 14, 28, "", val -> {
                    MiogramPlayerPrefs.setLyricsFontSize(val);
                    apply();
                });
        addToggleRow(root, context, MiogramLocale.get("Сяйво активного рядка", "Сияние активной строки", "Active line glow"),
                MiogramPlayerPrefs.isLyricsActiveGlow(), enabled -> {
                    MiogramPlayerPrefs.setLyricsActiveGlow(enabled);
                    apply();
                });
        addLabel(root, context, MiogramLocale.get("Колір сяйва", "Цвет сияния", "Glow color"));
        addPaletteRow(root, context, MiogramPlayerPrefs.getLyricsActiveColor(), lyricGlowSwatches, color -> {
            MiogramPlayerPrefs.setLyricsActiveColor(color);
            apply();
        });
        refreshPalette(lyricGlowSwatches, MiogramPlayerPrefs.getLyricsActiveColor());
        addSliderRow(root, context, MiogramLocale.get("Прозорість інших рядків", "Прозрачность остальных строк", "Other lines opacity"),
                (int) (MiogramPlayerPrefs.getLyricsInactiveOpacity() * 100), 20, 90, "%", val -> {
                    MiogramPlayerPrefs.setLyricsInactiveOpacity(val / 100.0f);
                    apply();
                });
    }

    // ---------------- Shared widgets ----------------

    private void apply() {
        try {
            if (playerLayout != null) playerLayout.applyCustomization();
        } catch (Throwable ignore) {}
    }

    private void addLabel(LinearLayout parent, Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTextColor(textColor);
        tv.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(10), 0, AndroidUtilities.dp(6));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addNote(LinearLayout parent, Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTextColor(subTextColor);
        tv.setPadding(AndroidUtilities.dp(4), 0, 0, AndroidUtilities.dp(4));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public interface OnValueChangedListener {
        void onValueChanged(int value);
    }

    private void addSliderRow(LinearLayout parent, Context context, String label, int currentVal, int minVal, int maxVal, String suffix, OnValueChangedListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(6));

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
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
        seekBar.setMax(Math.max(1, maxVal - minVal));
        seekBar.setProgress(Math.max(0, Math.min(maxVal - minVal, currentVal - minVal)));
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
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        row.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 2));

        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public interface OnToggleListener {
        void onToggle(boolean enabled);
    }

    private void addToggleRow(LinearLayout parent, Context context, String label, boolean currentVal, OnToggleListener listener) {
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
        updateToggleStyle(toggleBtn, state[0]);

        toggleBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            state[0] = !state[0];
            updateToggleStyle(toggleBtn, state[0]);
            if (listener != null) listener.onToggle(state[0]);
        });

        row.addView(toggleBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void updateToggleStyle(TextView tv, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(14));
        if (active) {
            gd.setColor(accentColor);
            tv.setTextColor(0xFFFFFFFF);
            tv.setText(MiogramLocale.get("Увімкнено", "Включено", "On"));
        } else {
            gd.setColor(0x22888888);
            tv.setTextColor(0xAAFFFFFF);
            tv.setText(MiogramLocale.get("Вимкнено", "Выключено", "Off"));
        }
        tv.setBackground(gd);
    }

    public interface OnSegmentListener {
        void onSelect(int index);
    }

    private void addSegmentRow(LinearLayout parent, Context context, String label, String[] options, int selected, OnSegmentListener listener) {
        addLabel(parent, context, label);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        TextView[] btns = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView b = new TextView(context);
            b.setText(options[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            b.setTypeface(AndroidUtilities.bold());
            b.setGravity(Gravity.CENTER);
            b.setSingleLine(true);
            b.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(8), AndroidUtilities.dp(7));
            btns[i] = b;
            b.setOnClickListener(v -> {
                MiogramHaptic.select(v);
                updateSegmentStyles(btns, index);
                if (listener != null) listener.onSelect(index);
            });
            row.addView(b, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        }
        updateSegmentStyles(btns, selected);
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
    }

    private void updateSegmentStyles(TextView[] btns, int selected) {
        for (int i = 0; i < btns.length; i++) {
            boolean active = i == selected;
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

    public interface OnColorListener {
        void onPick(int color);
    }

    private void addPaletteRow(LinearLayout parent, Context context, int selected, List<View> store, OnColorListener listener) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(6));
        LinearLayout row = null;
        for (int i = 0; i < PALETTE.length; i++) {
            if (i % 6 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                grid.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            }
            final int color = PALETTE[i];
            FrameLayout chip = new FrameLayout(context);
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.OVAL);
            chipBg.setColor(color);
            chipBg.setStroke(AndroidUtilities.dp(2), color == selected ? accentColor : 0x44FFFFFF);
            chip.setBackground(chipBg);
            chip.setTag(color);
            chip.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                refreshPalette(store, color);
                if (listener != null) listener.onPick(color);
            });
            row.addView(chip, LayoutHelper.createLinear(38, 38, Gravity.CENTER, 6, 0, 6, 0));
            store.add(chip);
        }
        parent.addView(grid, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void refreshPalette(List<View> store, int selected) {
        for (View chip : store) {
            try {
                Object tag = chip.getTag();
                int color = tag instanceof Integer ? (Integer) tag : 0;
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(color);
                gd.setStroke(AndroidUtilities.dp(2), color == selected ? accentColor : 0x44FFFFFF);
                chip.setBackground(gd);
            } catch (Throwable ignore) {}
        }
    }
}
