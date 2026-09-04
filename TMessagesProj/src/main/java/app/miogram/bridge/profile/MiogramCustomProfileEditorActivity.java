package app.miogram.bridge.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;

/**
 * Visual Interactive Custom Profile Editor for Miogram.
 * Features a real-time live preview of banners, avatar shapes, neon glows, and thought bubbles.
 */
public class MiogramCustomProfileEditorActivity extends BaseFragment {

    private int bannerMode;
    private int gradC1;
    private int gradC2;
    private int gradC3;
    private int avatarShape;
    private boolean avatarGlow;
    private int avatarGlowColor;
    private boolean thoughtEnabled;
    private String thoughtText;

    private View livePreviewBanner;
    private View livePreviewAvatar;

    @Override
    public boolean onFragmentCreate() {
        bannerMode = MiogramCustomProfileManager.getBannerMode();
        gradC1 = MiogramCustomProfileManager.getGradC1();
        gradC2 = MiogramCustomProfileManager.getGradC2();
        gradC3 = MiogramCustomProfileManager.getGradC3();
        avatarShape = MiogramCustomProfileManager.getAvatarShape();
        avatarGlow = MiogramCustomProfileManager.isAvatarGlowEnabled();
        avatarGlowColor = MiogramCustomProfileManager.getAvatarGlowColor();
        thoughtEnabled = MiogramCustomProfileManager.isThoughtEnabled();
        thoughtText = MiogramCustomProfileManager.getThoughtText();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Редактор профілю", "Редактор профиля", "Profile Editor"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, AndroidUtilities.dp(32));
        scrollView.addView(content, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // 1. Live Profile Preview (160dp banner height)
        FrameLayout previewContainer = new FrameLayout(context);
        previewContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(160)));

        livePreviewBanner = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                MiogramCustomProfileManager.drawBanner(canvas, getWidth(), getHeight(), 0L);
            }
        };
        previewContainer.addView(livePreviewBanner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Live Avatar View
        livePreviewAvatar = new View(context) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                android.graphics.Path path = MiogramCustomProfileManager.getAvatarShapePath(getWidth(), getHeight(), avatarShape);
                if (path != null) {
                    canvas.save();
                    canvas.clipPath(path);
                    p.setColor(Color.parseColor("#4A3B69"));
                    canvas.drawRect(0, 0, getWidth(), getHeight(), p);
                    // Mock avatar face
                    p.setColor(Color.WHITE);
                    p.setTextSize(AndroidUtilities.dp(22));
                    p.setTypeface(AndroidUtilities.bold());
                    p.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("M", getWidth() / 2f, getHeight() / 2f + AndroidUtilities.dp(8), p);
                    canvas.restore();

                    if (avatarGlow) {
                        MiogramCustomProfileManager.drawAvatarGlowAndBorder(canvas, getWidth(), getHeight(), avatarShape);
                    }
                }
            }
        };
        previewContainer.addView(livePreviewAvatar, LayoutHelper.createFrame(80, 80, Gravity.CENTER_VERTICAL | Gravity.LEFT, 24, 16, 0, 0));

        content.addView(previewContainer);

        // Section: Banner Styles
        content.addView(createSectionHeader(context, "СТИЛЬ БАНЕРА"));
        LinearLayout bannerOptions = createCardContainer(context);

        String[] bannerModes = {
                "Кібер-сітка (Ame Mesh)",
                "Лінійний градієнт",
                "Радіальний градієнт",
                "Суцільний колір",
                "За замовчуванням"
        };
        int[] modeValues = {
                MiogramCustomProfileManager.BANNER_MODE_MESH,
                MiogramCustomProfileManager.BANNER_MODE_LINEAR,
                MiogramCustomProfileManager.BANNER_MODE_RADIAL,
                MiogramCustomProfileManager.BANNER_MODE_SOLID,
                MiogramCustomProfileManager.BANNER_MODE_DEFAULT
        };

        for (int i = 0; i < bannerModes.length; i++) {
            final int mVal = modeValues[i];
            TextView modeBtn = createOptionRow(context, bannerModes[i], bannerMode == mVal);
            modeBtn.setOnClickListener(v -> {
                bannerMode = mVal;
                MiogramCustomProfileManager.setBannerMode(mVal);
                refreshSelectionInContainer(bannerOptions, v);
                livePreviewBanner.invalidate();
            });
            bannerOptions.addView(modeBtn);
        }
        content.addView(bannerOptions);

        // Section: Color Presets
        content.addView(createSectionHeader(context, "КОЛЬОРОВІ ПРЕСЕТИ"));
        LinearLayout colorsCard = createCardContainer(context);
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        LinearLayout colorPills = new LinearLayout(context);
        colorPills.setOrientation(LinearLayout.HORIZONTAL);
        colorPills.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        int[][] presets = {
                {Color.parseColor("#FF55A3"), Color.parseColor("#7B2CBF"), Color.parseColor("#00F5D4")}, // Ame Cyber
                {Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#45B7D1")}, // Pastel Wave
                {Color.parseColor("#845EC2"), Color.parseColor("#D65DB1"), Color.parseColor("#FF6F91")}, // Neon Velvet
                {Color.parseColor("#00C9FF"), Color.parseColor("#92FE9D"), Color.parseColor("#00F5D4")}, // Cyber Glow
                {Color.parseColor("#2C3E50"), Color.parseColor("#3498DB"), Color.parseColor("#2980B9")}  // Midnight
        };

        for (int[] preset : presets) {
            View pill = new View(context);
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, preset);
            gd.setCornerRadius(AndroidUtilities.dp(14));
            pill.setBackground(gd);
            pill.setOnClickListener(v -> {
                gradC1 = preset[0];
                gradC2 = preset[1];
                gradC3 = preset[2];
                MiogramCustomProfileManager.setGradC1(gradC1);
                MiogramCustomProfileManager.setGradC2(gradC2);
                MiogramCustomProfileManager.setGradC3(gradC3);
                livePreviewBanner.invalidate();
            });
            colorPills.addView(pill, LayoutHelper.createLinear(54, 36, 0, 0, 10, 0));
        }
        hScroll.addView(colorPills);
        colorsCard.addView(hScroll);
        content.addView(colorsCard);

        // Section: Avatar Shape
        content.addView(createSectionHeader(context, "ФОРМА АВАТАРКИ"));
        LinearLayout shapeOptions = createCardContainer(context);

        String[] shapes = {"▢ Сквіркл", "♥ Сердечко", "★ Зірка", "⬡ Гексагон", "◇ Ромб", "◯ Коло"};
        int[] shapeValues = {
                MiogramCustomProfileManager.SHAPE_SQUIRCLE,
                MiogramCustomProfileManager.SHAPE_HEART,
                MiogramCustomProfileManager.SHAPE_STAR,
                MiogramCustomProfileManager.SHAPE_HEXAGON,
                MiogramCustomProfileManager.SHAPE_DIAMOND,
                MiogramCustomProfileManager.SHAPE_CIRCLE
        };

        for (int i = 0; i < shapes.length; i++) {
            final int sVal = shapeValues[i];
            TextView shapeBtn = createOptionRow(context, shapes[i], avatarShape == sVal);
            shapeBtn.setOnClickListener(v -> {
                avatarShape = sVal;
                MiogramCustomProfileManager.setAvatarShape(sVal);
                refreshSelectionInContainer(shapeOptions, v);
                livePreviewAvatar.invalidate();
            });
            shapeOptions.addView(shapeBtn);
        }
        content.addView(shapeOptions);

        // Section: Neon Glow
        content.addView(createSectionHeader(context, "НЕОНОВЕ СЯЙВО АВАТАРКИ"));
        LinearLayout glowCard = createCardContainer(context);
        TextView glowToggle = createOptionRow(context, "Увімкнути неонове сяйво", avatarGlow);
        glowToggle.setOnClickListener(v -> {
            avatarGlow = !avatarGlow;
            MiogramCustomProfileManager.setAvatarGlowEnabled(avatarGlow);
            updateOptionRowStyle(glowToggle, avatarGlow);
            livePreviewAvatar.invalidate();
        });
        glowCard.addView(glowToggle);
        content.addView(glowCard);

        // Section: Thought Bubble
        content.addView(createSectionHeader(context, "ХМАРИНКА ДУМОК"));
        LinearLayout thoughtCard = createCardContainer(context);
        thoughtCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        EditText thoughtInput = new EditText(context);
        thoughtInput.setHint("Текст хмаринки думок (напр. ✦ Miogram ✦)");
        thoughtInput.setText(thoughtText);
        thoughtInput.setTextSize(14);
        thoughtInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        thoughtInput.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        thoughtInput.setBackground(null);
        thoughtInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                thoughtText = s != null ? s.toString() : "";
                MiogramCustomProfileManager.setThoughtText(thoughtText);
                livePreviewBanner.invalidate();
            }
        });
        thoughtCard.addView(thoughtInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(thoughtCard);

        // Save / Apply Button
        TextView saveBtn = new TextView(context);
        saveBtn.setText("Зберегти оформлення");
        saveBtn.setTextSize(16);
        saveBtn.setTypeface(AndroidUtilities.bold());
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setGravity(Gravity.CENTER);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(AndroidUtilities.dp(14));
        saveBg.setColor(Color.parseColor("#FF55A3"));
        saveBtn.setBackground(saveBg);
        saveBtn.setPadding(0, AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15));
        saveBtn.setOnClickListener(v -> {
            Toast.makeText(context, "Оформлення профілю збережено!", Toast.LENGTH_SHORT).show();
            finishFragment();
        });

        content.addView(saveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 20, 16, 0));

        fragmentView = root;
        return fragmentView;
    }

    private TextView createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(13);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        return tv;
    }

    private LinearLayout createCardContainer(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(14));
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        card.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createOptionRow(Context context, String text, boolean selected) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
        updateOptionRowStyle(tv, selected);
        return tv;
    }

    private void updateOptionRowStyle(TextView tv, boolean selected) {
        if (selected) {
            tv.setTextColor(Color.parseColor("#FF55A3"));
            tv.setTypeface(AndroidUtilities.bold());
        } else {
            tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            tv.setTypeface(Typeface.DEFAULT);
        }
    }

    private void refreshSelectionInContainer(LinearLayout container, View active) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                updateOptionRowStyle((TextView) child, child == active);
            }
        }
    }
}
