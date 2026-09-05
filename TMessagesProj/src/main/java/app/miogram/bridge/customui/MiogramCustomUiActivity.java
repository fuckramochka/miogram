package app.miogram.bridge.customui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
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
 * Miogram Custom UI Studio (Кастом Юай):
 * Visual styling companion designed with the aesthetic architecture of Custom Profile.
 * Provides complete control over everything outside the user profile:
 * - Real-time chat bubbles styling with gradients, angles, corner radius, and custom text colors
 * - Avatar geometric shapes (Circle, Squircle, Rounded Rect, Hexagon) and glowing online/story rings
 * - Dialogs cards, unread pill/glow badges, and glassmorphism headers
 * - Instant live canvas preview and 1-tap preset themes
 */
public class MiogramCustomUiActivity extends BaseFragment {

    private LivePreviewView livePreview;
    private TextView angleValueText;
    private TextView radiusValueText;
    private SeekBar angleSeekBar;
    private SeekBar radiusSeekBar;
    private TextCheckCell gradientSwitch;
    private TextCheckCell avatarRingSwitch;
    private TextView avatarShapeSubtitle;
    private TextView unreadStyleSubtitle;
    private TextView headerStyleSubtitle;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Кастом Юай ໒꒱", "Кастом Юай ໒꒱", "Custom UI ໒꒱"));
        actionBar.setSubtitle(MiogramLocale.get("Дизайн-студія інтерфейсу", "Дизайн-студия интерфейса", "Interface Design Studio"));
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

        FrameLayout fragmentRoot = new FrameLayout(context);
        fragmentRoot.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(24));

        // 1. Top Banner Card
        contentLayout.addView(createBannerCard(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // 2. Interactive Live Canvas Preview
        livePreview = new LivePreviewView(context);
        contentLayout.addView(livePreview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200, 0, 0, 0, 14));

        // 3. One-Tap Preset Themes
        contentLayout.addView(createSectionHeader(context, MiogramLocale.get("Стилі в 1 дотик ✨", "Стили в 1 касание ✨", "1-Tap Presets ✨")));
        contentLayout.addView(createPresetsRow(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 14));

        // 4. Chat Bubbles Section
        contentLayout.addView(createSectionHeader(context, MiogramLocale.get("Бульбашки чату 💬", "Пузырьки чата 💬", "Chat Bubbles 💬")));
        contentLayout.addView(createBubblesSection(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 14));

        // 5. Avatars & Dialogs Section
        contentLayout.addView(createSectionHeader(context, MiogramLocale.get("Аватари та список чатів 📱", "Аватары и список чатов 📱", "Avatars & Chats 📱")));
        contentLayout.addView(createAvatarsSection(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 14));

        // 6. Header & Performance Section
        contentLayout.addView(createSectionHeader(context, MiogramLocale.get("Шапка чату та відгук ⚡", "Шапка чата и отклик ⚡", "Header & Performance ⚡")));
        contentLayout.addView(createHeaderAndPerformanceSection(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 14));

        // 7. Apply Button
        contentLayout.addView(createApplyButton(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50, 0, 8, 0, 10));

        scrollView.addView(contentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        fragmentRoot.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = fragmentRoot;
        return fragmentView;
    }

    private View createBannerCard(Context context) {
        FrameLayout card = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColors(new int[]{0xFF7052FF, 0xFF00D2FF});
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        card.setBackground(bg);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText("Miogram Custom UI ໒꒱");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFFFFF);
        inner.addView(title);

        TextView desc = new TextView(context);
        desc.setText(MiogramLocale.get(
                "Гнучка кастомізація всіх елементів Telegram: градієнтні бульбашки, форми аватарів, сяючі кільця та скло.",
                "Гибкая кастомизация всех элементов Telegram: градиентные пузырьки, формы аватаров, светящиеся кольца и стекло.",
                "Flexible customization of all Telegram elements: gradient bubbles, avatar shapes, glowing rings, and glass."));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        desc.setTextColor(0xDDFFFFFF);
        desc.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        inner.addView(desc);

        card.addView(inner);
        return card;
    }

    private TextView createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, getResourceProvider()));
        tv.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(4));
        return tv;
    }

    private View createPresetsRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);

        String[] presets = {"⚡ Neon", "💎 Glass", "🌌 AMOLED", "🌅 Sunset", "🌿 Mint"};
        int[] ids = {0, 1, 2, 3, 4};

        for (int i = 0; i < presets.length; i++) {
            final int id = ids[i];
            TextView chip = new TextView(context);
            chip.setText(presets[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(AndroidUtilities.dp(12));
            chipBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
            chip.setBackground(chipBg);

            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                MiogramCustomUiPrefs.applyPreset(id);
                syncUiFromPrefs();
                livePreview.invalidate();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Стиль застосовано! ✨").show();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) lp.leftMargin = AndroidUtilities.dp(6);
            row.addView(chip, lp);
        }
        return row;
    }

    private View createBubblesSection(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(section);

        gradientSwitch = new TextCheckCell(context);
        gradientSwitch.setTextAndCheck(MiogramLocale.get("Градієнтні вихідні бульбашки", "Градиентные исходящие пузырьки", "Gradient Outgoing Bubbles"),
                MiogramCustomUiPrefs.isBubbleGradientEnabled(), true);
        gradientSwitch.setOnClickListener(v -> {
            boolean newVal = !gradientSwitch.isChecked();
            gradientSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setBubbleGradientEnabled(newVal);
            livePreview.invalidate();
        });
        section.addView(gradientSwitch);

        // Color Chips Row
        LinearLayout colorsRow = new LinearLayout(context);
        colorsRow.setOrientation(LinearLayout.HORIZONTAL);
        colorsRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        colorsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView colorsLabel = new TextView(context);
        colorsLabel.setText(MiogramLocale.get("Палітра градієнта:", "Палитра градиента:", "Gradient Palette:"));
        colorsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        colorsLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        colorsRow.addView(colorsLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        View color1 = createColorChip(context, MiogramCustomUiPrefs.getBubbleColor1(), c -> {
            MiogramCustomUiPrefs.setBubbleColor1(c);
            livePreview.invalidate();
        });
        View color2 = createColorChip(context, MiogramCustomUiPrefs.getBubbleColor2(), c -> {
            MiogramCustomUiPrefs.setBubbleColor2(c);
            livePreview.invalidate();
        });
        colorsRow.addView(color1);
        colorsRow.addView(color2, LayoutHelper.createLinear(32, 32, 10, 0, 0, 0));
        section.addView(colorsRow);

        // Angle Slider
        LinearLayout angleRow = new LinearLayout(context);
        angleRow.setOrientation(LinearLayout.VERTICAL);
        angleRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        LinearLayout angleHeader = new LinearLayout(context);
        TextView angleTitle = new TextView(context);
        angleTitle.setText(MiogramLocale.get("Кут градієнта", "Угол градиента", "Gradient Angle"));
        angleTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        angleValueText = new TextView(context);
        angleValueText.setText(MiogramCustomUiPrefs.getBubbleAngle() + "°");
        angleValueText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, getResourceProvider()));
        angleHeader.addView(angleTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
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
        section.addView(angleRow);

        // Radius Slider
        LinearLayout radiusRow = new LinearLayout(context);
        radiusRow.setOrientation(LinearLayout.VERTICAL);
        radiusRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        LinearLayout radiusHeader = new LinearLayout(context);
        TextView radiusTitle = new TextView(context);
        radiusTitle.setText(MiogramLocale.get("Радіус кутів бульбашок", "Радиус углов пузырьков", "Bubble Corner Radius"));
        radiusTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        radiusValueText = new TextView(context);
        radiusValueText.setText(MiogramCustomUiPrefs.getBubbleRadius() + " dp");
        radiusValueText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, getResourceProvider()));
        radiusHeader.addView(radiusTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        radiusHeader.addView(radiusValueText);
        radiusRow.addView(radiusHeader);

        radiusSeekBar = new SeekBar(context);
        radiusSeekBar.setMax(24); // maps to 4..28 dp
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
        section.addView(radiusRow);

        return section;
    }

    private View createAvatarsSection(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(section);

        // Avatar Shape Selector
        LinearLayout shapeRow = new LinearLayout(context);
        shapeRow.setOrientation(LinearLayout.HORIZONTAL);
        shapeRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        shapeRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout shapeTexts = new LinearLayout(context);
        shapeTexts.setOrientation(LinearLayout.VERTICAL);
        TextView shapeTitle = new TextView(context);
        shapeTitle.setText(MiogramLocale.get("Геометрія аватарів", "Геометрия аватаров", "Avatar Geometry"));
        shapeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        shapeTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        avatarShapeSubtitle = new TextView(context);
        avatarShapeSubtitle.setText(getAvatarShapeName(MiogramCustomUiPrefs.getAvatarShape()));
        avatarShapeSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        avatarShapeSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        shapeTexts.addView(shapeTitle);
        shapeTexts.addView(avatarShapeSubtitle);
        shapeRow.addView(shapeTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.msg_arrowright);
        arrow.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, getResourceProvider()));
        shapeRow.addView(arrow);

        shapeRow.setOnClickListener(v -> showAvatarShapeDialog());
        section.addView(shapeRow);

        // Glowing Avatar Ring Switch
        avatarRingSwitch = new TextCheckCell(context);
        avatarRingSwitch.setTextAndCheck(MiogramLocale.get("Сяюче неонове кільце аватара", "Светящееся неоновое кольцо аватара", "Glowing Avatar Neon Ring"),
                MiogramCustomUiPrefs.isAvatarRingEnabled(), true);
        avatarRingSwitch.setOnClickListener(v -> {
            boolean newVal = !avatarRingSwitch.isChecked();
            avatarRingSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setAvatarRingEnabled(newVal);
            livePreview.invalidate();
        });
        section.addView(avatarRingSwitch);

        // Unread Badge Style
        LinearLayout unreadRow = new LinearLayout(context);
        unreadRow.setOrientation(LinearLayout.HORIZONTAL);
        unreadRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        unreadRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout unreadTexts = new LinearLayout(context);
        unreadTexts.setOrientation(LinearLayout.VERTICAL);
        TextView unreadTitle = new TextView(context);
        unreadTitle.setText(MiogramLocale.get("Бейджики непрочитаних", "Бейджики непрочитанных", "Unread Badges"));
        unreadTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        unreadTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        unreadStyleSubtitle = new TextView(context);
        unreadStyleSubtitle.setText(getUnreadStyleName(MiogramCustomUiPrefs.getUnreadStyle()));
        unreadStyleSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        unreadStyleSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        unreadTexts.addView(unreadTitle);
        unreadTexts.addView(unreadStyleSubtitle);
        unreadRow.addView(unreadTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        ImageView unreadArrow = new ImageView(context);
        unreadArrow.setImageResource(R.drawable.msg_arrowright);
        unreadArrow.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, getResourceProvider()));
        unreadRow.addView(unreadArrow);

        unreadRow.setOnClickListener(v -> showUnreadStyleDialog());
        section.addView(unreadRow);

        return section;
    }

    private View createHeaderAndPerformanceSection(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        applyCardBackground(section);

        // Header Style Row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headerTexts = new LinearLayout(context);
        headerTexts.setOrientation(LinearLayout.VERTICAL);
        TextView hTitle = new TextView(context);
        hTitle.setText(MiogramLocale.get("Стиль верхньої панелі (ActionBar)", "Стиль верхней панели (ActionBar)", "Top ActionBar Style"));
        hTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        headerStyleSubtitle = new TextView(context);
        headerStyleSubtitle.setText(getHeaderStyleName(MiogramCustomUiPrefs.getHeaderStyle()));
        headerStyleSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        headerStyleSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        headerTexts.addView(hTitle);
        headerTexts.addView(headerStyleSubtitle);
        headerRow.addView(headerTexts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        ImageView hArrow = new ImageView(context);
        hArrow.setImageResource(R.drawable.msg_arrowright);
        hArrow.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, getResourceProvider()));
        headerRow.addView(hArrow);

        headerRow.setOnClickListener(v -> showHeaderStyleDialog());
        section.addView(headerRow);

        // ProMotion 120Hz Switch
        TextCheckCell proMotionSwitch = new TextCheckCell(context);
        proMotionSwitch.setTextAndCheck(MiogramLocale.get("ProMotion 120Hz / 144Hz плавний скрол", "ProMotion 120Hz / 144Hz плавный скролл", "ProMotion 120Hz Smooth Scroll"),
                MiogramCustomUiPrefs.isProMotionLock(), true);
        proMotionSwitch.setOnClickListener(v -> {
            boolean newVal = !proMotionSwitch.isChecked();
            proMotionSwitch.setChecked(newVal);
            MiogramCustomUiPrefs.setProMotionLock(newVal);
        });
        section.addView(proMotionSwitch);

        return section;
    }

    private View createApplyButton(Context context) {
        TextView applyBtn = new TextView(context);
        applyBtn.setText(MiogramLocale.get("Застосувати зміни ✨", "Применить изменения ✨", "Apply Changes ✨"));
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        applyBtn.setTypeface(AndroidUtilities.bold());
        applyBtn.setTextColor(0xFFFFFFFF);
        applyBtn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()));
        applyBtn.setBackground(bg);

        applyBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            Theme.reloadWallpaper();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Усі зміни оформлення успішно активовано! ໒꒱").show();
        });
        return applyBtn;
    }

    private View createColorChip(Context context, int initialColor, org.telegram.messenger.Utilities.Callback<Integer> callback) {
        View chip = new View(context);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(initialColor);
        d.setStroke(AndroidUtilities.dp(2), 0xFFFFFFFF);
        chip.setBackground(d);
        chip.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(32), AndroidUtilities.dp(32)));

        int[] colors = {0xFFFF007F, 0xFF00F0FF, 0xFF7052FF, 0xFF00D2FF, 0xFFFF5E3A, 0xFFFF2A68, 0xFF00B09B, 0xFF96C93D, 0xFFFFFFFF, 0xFF18181A};
        chip.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(context);
            b.setTitle(MiogramLocale.get("Оберіть колір", "Выберите цвет", "Choose Color"));
            String[] names = {"Neon Pink", "Cyber Cyan", "Electric Violet", "Sky Blue", "Coral Orange", "Rose Gold", "Mint Frost", "Lime Green", "Pure White", "AMOLED Dark"};
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
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        view.setBackground(bg);
    }

    private void syncUiFromPrefs() {
        if (gradientSwitch != null) gradientSwitch.setChecked(MiogramCustomUiPrefs.isBubbleGradientEnabled());
        if (avatarRingSwitch != null) avatarRingSwitch.setChecked(MiogramCustomUiPrefs.isAvatarRingEnabled());
        if (angleSeekBar != null) angleSeekBar.setProgress(MiogramCustomUiPrefs.getBubbleAngle());
        if (radiusSeekBar != null) radiusSeekBar.setProgress(Math.max(0, MiogramCustomUiPrefs.getBubbleRadius() - 4));
        if (angleValueText != null) angleValueText.setText(MiogramCustomUiPrefs.getBubbleAngle() + "°");
        if (radiusValueText != null) radiusValueText.setText(MiogramCustomUiPrefs.getBubbleRadius() + " dp");
        if (avatarShapeSubtitle != null) avatarShapeSubtitle.setText(getAvatarShapeName(MiogramCustomUiPrefs.getAvatarShape()));
        if (unreadStyleSubtitle != null) unreadStyleSubtitle.setText(getUnreadStyleName(MiogramCustomUiPrefs.getUnreadStyle()));
        if (headerStyleSubtitle != null) headerStyleSubtitle.setText(getHeaderStyleName(MiogramCustomUiPrefs.getHeaderStyle()));
    }

    private void showAvatarShapeDialog() {
        String[] shapes = {"Круглий (Classic) ⚪", "Сквіркл (iOS Modern) ⬛", "Закруглений прямокутник 🔲", "Гексагон (Cyber) ⬡"};
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(MiogramLocale.get("Геометрія аватара", "Геометрия аватара", "Avatar Geometry"));
        b.setItems(shapes, (dialog, which) -> {
            MiogramCustomUiPrefs.setAvatarShape(which);
            avatarShapeSubtitle.setText(getAvatarShapeName(which));
            livePreview.invalidate();
        });
        showDialog(b.create());
    }

    private void showUnreadStyleDialog() {
        String[] styles = {"Класичний круг ⚪", "Елегантна пігулка (Pill) 💊", "Неонове сяйво (Neon Glow) ✨"};
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(MiogramLocale.get("Стиль бейджиків", "Стиль бейджиков", "Badge Style"));
        b.setItems(styles, (dialog, which) -> {
            MiogramCustomUiPrefs.setUnreadStyle(which);
            unreadStyleSubtitle.setText(getUnreadStyleName(which));
            livePreview.invalidate();
        });
        showDialog(b.create());
    }

    private void showHeaderStyleDialog() {
        String[] styles = {"Стандартний (Material 3)", "Прозоре скло (Glassmorphism Blur)", "Акцентний градієнт (Aurora Accent)"};
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(MiogramLocale.get("Стиль ActionBar", "Стиль ActionBar", "ActionBar Style"));
        b.setItems(styles, (dialog, which) -> {
            MiogramCustomUiPrefs.setHeaderStyle(which);
            headerStyleSubtitle.setText(getHeaderStyleName(which));
            livePreview.invalidate();
        });
        showDialog(b.create());
    }

    private void showResetDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(MiogramLocale.get("Скидання налаштувань", "Сброс настроек", "Reset Settings"));
        b.setMessage(MiogramLocale.get("Скинути всі стилі Custom UI до заводських значень?", "Сбросить все стили Custom UI к заводским?", "Reset all Custom UI styles to defaults?"));
        b.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            MiogramCustomUiPrefs.resetDefaults();
            syncUiFromPrefs();
            livePreview.invalidate();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Скинуто до стандартних значень").show();
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private String getAvatarShapeName(int shape) {
        switch (shape) {
            case 1: return "Сквіркл (iOS Modern) ⬛";
            case 2: return "Закруглений прямокутник 🔲";
            case 3: return "Гексагон (Cyber) ⬡";
            default: return "Круглий (Classic) ⚪";
        }
    }

    private String getUnreadStyleName(int style) {
        switch (style) {
            case 1: return "Елегантна пігулка (Pill) 💊";
            case 2: return "Неонове сяйво (Neon Glow) ✨";
            default: return "Класичний круг ⚪";
        }
    }

    private String getHeaderStyleName(int style) {
        switch (style) {
            case 1: return "Прозоре скло (Glassmorphism Blur)";
            case 2: return "Акцентний градієнт (Aurora Accent)";
            default: return "Стандартний (Material 3)";
        }
    }

    /**
     * Live Interactive Canvas Preview Card.
     * Accurately mimics Custom Profile's BubblePreview and FrameCardView architecture.
     */
    private static class LivePreviewView extends View {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();

        LivePreviewView(Context context) {
            super(context);
            textPaint.setTextSize(AndroidUtilities.dp(14));
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(AndroidUtilities.dp(2.5f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();

            // Background Card
            rect.set(0, 0, w, h);
            fillPaint.setShader(null);
            fillPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), fillPaint);

            // 1. Avatar Preview (Left side)
            int avatarSize = AndroidUtilities.dp(44);
            float avLeft = AndroidUtilities.dp(16);
            float avTop = AndroidUtilities.dp(20);
            rect.set(avLeft, avTop, avLeft + avatarSize, avTop + avatarSize);

            int shape = MiogramCustomUiPrefs.getAvatarShape();
            fillPaint.setColor(0xFF7052FF);

            if (shape == 1) { // Squircle
                canvas.drawRoundRect(rect, AndroidUtilities.dp(12), AndroidUtilities.dp(12), fillPaint);
            } else if (shape == 2) { // Rounded Rect
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), fillPaint);
            } else if (shape == 3) { // Hexagon
                path.reset();
                float cx = rect.centerX();
                float cy = rect.centerY();
                float r = avatarSize / 2f;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(60 * i);
                    float x = (float) (cx + r * Math.cos(angle));
                    float y = (float) (cy + r * Math.sin(angle));
                    if (i == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                path.close();
                canvas.drawPath(path, fillPaint);
            } else { // Circle
                canvas.drawCircle(rect.centerX(), rect.centerY(), avatarSize / 2f, fillPaint);
            }

            // Glowing ring if enabled
            if (MiogramCustomUiPrefs.isAvatarRingEnabled()) {
                ringPaint.setColor(MiogramCustomUiPrefs.getAvatarRingColor());
                float ringInset = AndroidUtilities.dp(3);
                RectF ringRect = new RectF(rect.left - ringInset, rect.top - ringInset, rect.right + ringInset, rect.bottom + ringInset);
                canvas.drawRoundRect(ringRect, AndroidUtilities.dp(shape == 0 ? 25 : 15), AndroidUtilities.dp(shape == 0 ? 25 : 15), ringPaint);
            }

            // 2. Incoming Bubble ("Привіт! Як тобі новий стиль? ໒꒱")
            float b1Left = avLeft + avatarSize + AndroidUtilities.dp(12);
            float b1Top = avTop;
            String msg1 = "Привіт! Як тобі оформлення? ໒꒱";
            float text1W = textPaint.measureText(msg1);
            rect.set(b1Left, b1Top, b1Left + text1W + AndroidUtilities.dp(24), b1Top + AndroidUtilities.dp(38));

            fillPaint.setShader(null);
            fillPaint.setColor(Theme.getColor(Theme.key_chat_inBubble));
            float radius = AndroidUtilities.dp(MiogramCustomUiPrefs.getBubbleRadius());
            canvas.drawRoundRect(rect, radius, radius, fillPaint);

            textPaint.setColor(Theme.getColor(Theme.key_chat_messageTextIn));
            canvas.drawText(msg1, b1Left + AndroidUtilities.dp(12), b1Top + AndroidUtilities.dp(24), textPaint);

            // 3. Outgoing Bubble ("Виглядає космічно! 🔥")
            String msg2 = "Виглядає космічно! 🔥";
            float text2W = textPaint.measureText(msg2);
            float b2Right = w - AndroidUtilities.dp(16);
            float b2Top = b1Top + AndroidUtilities.dp(52);
            rect.set(b2Right - text2W - AndroidUtilities.dp(28), b2Top, b2Right, b2Top + AndroidUtilities.dp(38));

            if (MiogramCustomUiPrefs.isBubbleGradientEnabled()) {
                double rad = Math.toRadians(MiogramCustomUiPrefs.getBubbleAngle());
                float cx = rect.centerX();
                float cy = rect.centerY();
                float maxD = Math.max(rect.width(), rect.height()) / 2f;
                float xCos = (float) Math.cos(rad) * maxD;
                float ySin = (float) Math.sin(rad) * maxD;
                fillPaint.setShader(new LinearGradient(cx - xCos, cy - ySin, cx + xCos, cy + ySin,
                        MiogramCustomUiPrefs.getBubbleColor1(), MiogramCustomUiPrefs.getBubbleColor2(), Shader.TileMode.CLAMP));
            } else {
                fillPaint.setShader(null);
                fillPaint.setColor(MiogramCustomUiPrefs.getBubbleColor1());
            }
            canvas.drawRoundRect(rect, radius, radius, fillPaint);

            textPaint.setColor(MiogramCustomUiPrefs.getBubbleTextColor());
            canvas.drawText(msg2, rect.left + AndroidUtilities.dp(14), b2Top + AndroidUtilities.dp(24), textPaint);

            // 4. Outgoing Message 2 ("Кастом Юай працює нативно ໒꒱")
            String msg3 = "Кастом Юай працює нативно ໒꒱";
            float text3W = textPaint.measureText(msg3);
            float b3Top = b2Top + AndroidUtilities.dp(46);
            rect.set(b2Right - text3W - AndroidUtilities.dp(28), b3Top, b2Right, b3Top + AndroidUtilities.dp(38));
            canvas.drawRoundRect(rect, radius, radius, fillPaint);
            canvas.drawText(msg3, rect.left + AndroidUtilities.dp(14), b3Top + AndroidUtilities.dp(24), textPaint);
        }
    }
}
