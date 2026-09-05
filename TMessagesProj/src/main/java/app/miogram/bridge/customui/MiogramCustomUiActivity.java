package app.miogram.bridge.customui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.Toast;

import org.json.JSONObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.miogram.bridge.MiogramLocale;

/**
 * Miogram Custom UI Studio.
 * Crafted 1-to-1 matching the architecture, visual language, and granularity of Custom Profile.
 * Uses native Telegram BottomSheets (EditSheet), exact EditCells, and direct synchronization
 * with cpb_native_settings so that the user sees the identical Custom Profile interface.
 */
public class MiogramCustomUiActivity extends BaseFragment {

    public interface IntSink {
        void accept(int val);
    }

    public interface ColorSink {
        void accept(int color);
    }

    @Override
    public View createView(Context context) {
        // 1. Action Bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("Кастомне оформлення", "Кастомное оформление", "Custom Appearance"));
        actionBar.setSubtitle("Custom Profile Studio");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        // 2. Fragment View (Clean list hub for all Custom Profile EditSheets)
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Header: Modules
        HeaderCell headerModules = new HeaderCell(context);
        headerModules.setText(MiogramLocale.get("Розділи налаштувань Custom Profile", "Разделы настроек Custom Profile", "Custom Profile Settings"));
        headerModules.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        content.addView(headerModules);

        // 1. Bubbles (Extra Features)
        TextCell rowBubbles = new TextCell(context);
        rowBubbles.setTextAndValue(MiogramLocale.get("Додаткові функції (Пухирці)", "Дополнительные функции (Пузырьки)", "Extra features (Bubbles)"), MiogramLocale.get("Повідомлення", "Сообщения", "Messages"), true);
        rowBubbles.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBubbles.setOnClickListener(v -> ExtraFeaturesSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowBubbles);

        // 2. Names
        TextCell rowNames = new TextCell(context);
        rowNames.setTextAndValue(MiogramLocale.get("Оформлення імені", "Оформление имени", "Name appearance"), MiogramLocale.get("Колір, тінь, ефекти", "Цвет, тень, эффекты", "Color, shadow, effects"), true);
        rowNames.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowNames.setOnClickListener(v -> EditNameSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowNames);

        // 3. Avatars
        TextCell rowAvatars = new TextCell(context);
        rowAvatars.setTextAndValue(MiogramLocale.get("Оформлення аватара", "Оформление аватара", "Avatar appearance"), MiogramLocale.get("Форма, скруглення, вигляд", "Форма, скругление, вид", "Shape, rounding, look"), true);
        rowAvatars.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowAvatars.setOnClickListener(v -> EditAvatarSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowAvatars);

        // 4. Frame
        TextCell rowFrame = new TextCell(context);
        rowFrame.setTextAndValue(MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"), MiogramLocale.get("Неонове кільце, товщина, пульс", "Неоновое кольцо, толщина, пульс", "Neon ring, width, pulse"), true);
        rowFrame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowFrame.setOnClickListener(v -> EditFrameSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowFrame);

        // 5. Text colors (Photo 3)
        TextCell rowText = new TextCell(context);
        rowText.setTextAndValue(MiogramLocale.get("Текст профілю (Палітра)", "Текст профиля (Палитра)", "Profile text (Palette)"), MiogramLocale.get("Кольори елементів", "Цвета элементов", "Element colors"), true);
        rowText.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowText.setOnClickListener(v -> EditTextColorsSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowText);

        // 6. Banner (Photo 1)
        TextCell rowBanner = new TextCell(context);
        rowBanner.setTextAndValue(MiogramLocale.get("Шапка і банер", "Шапка и баннер", "Header and banner"), MiogramLocale.get("Фон, злиття, прозорість", "Фон, слияние, прозрачность", "Background, blend, alpha"), true);
        rowBanner.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBanner.setOnClickListener(v -> EditBannerSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowBanner);

        // 7. Background (Photo 2)
        TextCell rowBg = new TextCell(context);
        rowBg.setTextAndValue(MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"), MiogramLocale.get("Колір, медіа, сумісність", "Цвет, медиа, совместимость", "Color, media, compat"), true);
        rowBg.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBg.setOnClickListener(v -> EditBackgroundSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowBg);

        // 8. Blocks (Photo 2)
        TextCell rowBlocks = new TextCell(context);
        rowBlocks.setTextAndValue(MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), MiogramLocale.get("Колір, щільність, скруглення, блюр", "Цвет, плотность, скругление, блюр", "Color, density, rounding, blur"), true);
        rowBlocks.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBlocks.setOnClickListener(v -> EditBlocksSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowBlocks);

        // 9. Visibility (Photo 2)
        TextCell rowVis = new TextCell(context);
        rowVis.setTextAndValue(MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"), MiogramLocale.get("Приховування блоків", "Скрытие блоков", "Hiding blocks"), true);
        rowVis.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowVis.setOnClickListener(v -> EditVisibilitySheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowVis);

        // 10. UI & Badges
        TextCell rowUi = new TextCell(context);
        rowUi.setTextAndValue(MiogramLocale.get("Інтерфейс клієнта", "Интерфейс клиента", "Client interface"), MiogramLocale.get("Бейджі, скляний блюр, тактильність", "Бейджи, стеклянный блюр, тактильность", "Badges, glass blur, haptic"), false);
        rowUi.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowUi.setOnClickListener(v -> EditInterfaceSheet.show(getParentActivity() != null ? getParentActivity() : context));
        content.addView(rowUi);

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context);
        infoCell.setText(MiogramLocale.get(
            "Усі розділи відкриваються через оригінальні діалогові вікна Custom Profile (EditSheet) та записують параметри напряму у сховище cpb_native_settings.",
            "Все разделы открываются через оригинальные диалоговые окна Custom Profile (EditSheet) и записывают параметры напрямую в хранилище cpb_native_settings.",
            "All sections open via original Custom Profile dialogs (EditSheet) and save settings directly to cpb_native_settings storage."
        ));
        content.addView(infoCell);

        // Immediately show the ExtraFeaturesSheet on entrance so user directly gets the native sheet!
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() != null && !getParentActivity().isFinishing()) {
                ExtraFeaturesSheet.show(getParentActivity());
            }
        }, 100);

        return fragmentView;
    }

    /* =========================================================================
     * 1. NATIVE EDITSHEET (1-to-1 with cpb.EditSheet / BottomSheet)
     * ========================================================================= */

    public static class EditSheet {
        private final Context context;
        private final String title;
        private final LinearLayout content;
        private BottomSheet sheet;
        private final Map<String, List<View>> groups = new HashMap<>();
        private String currentGroup;

        public EditSheet(Context context, String title) {
            this.context = context;
            this.title = title;
            this.content = new LinearLayout(context);
            this.content.setOrientation(LinearLayout.VERTICAL);
            this.content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        }

        public Context getContext() {
            return context;
        }

        public EditSheet custom(View view) {
            if (view != null) add(view);
            return this;
        }

        public EditSheet header(String text) {
            HeaderCell cell = new HeaderCell(context);
            cell.setText(text);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return add(cell);
        }

        public EditSheet note(String text) {
            TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
            cell.setText(text);
            return add(cell);
        }

        public EditSheet check(String title, boolean checked, boolean divider, final IntSink sink) {
            final TextCheckCell cell = new TextCheckCell(context);
            cell.setTextAndCheck(title, checked, divider);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cell.setOnClickListener(v -> {
                boolean newVal = !cell.isChecked();
                cell.setChecked(newVal);
                hapticToggle(cell);
                if (sink != null) sink.accept(newVal ? 1 : 0);
            });
            return add(cell);
        }

        public EditSheet row(String title, String value, boolean divider, final Runnable onClick) {
            TextCell cell = new TextCell(context);
            cell.setTextAndValue(title, value != null ? value : "", divider);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            if (onClick != null) {
                cell.setOnClickListener(v -> {
                    hapticTap(cell);
                    onClick.run();
                });
            }
            return add(cell);
        }

        public EditSheet color(String title, int color, boolean divider, final ColorSink sink) {
            final EditColorRow row = new EditColorRow(context, title, color, divider, null);
            row.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            row.setOnClickListener(v -> {
                hapticTap(row);
                EditColorPicker.show(context, title, row.getColor(), c -> {
                    row.setColor(c);
                    if (sink != null) sink.accept(c);
                });
            });
            return add(row);
        }

        public EditSheet slider(final String title, int cur, final int min, final int max, final String unit, final IntSink sink) {
            LinearLayout box = new LinearLayout(context);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            box.setPadding(0, 0, 0, AndroidUtilities.dp(6));

            final TextView label = new TextView(context);
            label.setText(title + (unit.isEmpty() ? " — " + cur : " — " + cur + unit));
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            label.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            label.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(12), AndroidUtilities.dp(21), AndroidUtilities.dp(4));
            box.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            SeekBarView seekBar = new SeekBarView(context);
            seekBar.setReportChanges(true);
            float p = (max <= min) ? 0f : (float) (cur - min) / (float) (max - min);
            seekBar.setProgress(p);
            seekBar.setDelegate((stop, progress) -> {
                int val = min + Math.round(progress * (max - min));
                label.setText(title + (unit.isEmpty() ? " — " + val : " — " + val + unit));
                if (sink != null) sink.accept(val);
            });
            box.addView(seekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 12, 0, 12, 0));

            return add(box);
        }

        public EditSheet chooser(String title, int curId, String[] items, final IntSink sink) {
            LinearLayout box = new LinearLayout(context);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

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
                    hapticTap(chip);
                    for (int j = 0; j < chips.size(); j++) {
                        styleChip(chips.get(j), (j == id));
                    }
                    if (sink != null) sink.accept(id);
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                row.addView(chip, lp);
            }
            box.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return add(box);
        }

        private void styleChip(TextView chip, boolean active) {
            if (active) {
                chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
                chip.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
                GradientDrawable d = new GradientDrawable();
                d.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                d.setCornerRadius(AndroidUtilities.dp(16));
                chip.setBackground(d);
            } else {
                chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.NORMAL);
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                GradientDrawable d = new GradientDrawable();
                d.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
                d.setCornerRadius(AndroidUtilities.dp(16));
                chip.setBackground(d);
            }
        }

        public EditSheet group(String name) {
            currentGroup = name;
            if (!groups.containsKey(name)) {
                groups.put(name, new ArrayList<>());
            }
            return this;
        }

        public EditSheet endGroup() {
            currentGroup = null;
            return this;
        }

        public void setGroupVisible(String name, boolean visible) {
            List<View> list = groups.get(name);
            if (list == null) return;
            for (View v : list) {
                v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }

        private EditSheet add(View view) {
            content.addView(view);
            if (currentGroup != null) {
                List<View> list = groups.get(currentGroup);
                if (list != null) list.add(view);
            }
            return this;
        }

        public void show() {
            if (context == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(context);
            builder.setTitle(title, true);
            builder.setApplyBottomPadding(false);

            ScrollView scroll = new ScrollView(context);
            scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            builder.setCustomView(scroll);
            sheet = builder.create();
            sheet.setDimBehindAlpha(60);
            sheet.setFixNavigationBar(true);
            sheet.show();
        }

        public void dismiss() {
            if (sheet != null) {
                sheet.dismiss();
                sheet = null;
            }
        }
    }

    /* =========================================================================
     * 2. NATIVE EDITCOLORROW WITH DST_IN BAND (1-to-1 with cpb.EditColorRow - PHOTO 3)
     * ========================================================================= */

    public static class EditColorRow extends FrameLayout {
        private final TextCell textCell;
        private final Band band;
        private int color;

        public EditColorRow(Context context, String title, int color, boolean divider, final Runnable onClick) {
            super(context);
            this.color = color;

            textCell = new TextCell(context);
            textCell.setTextAndValue(title, "", divider);
            addView(textCell, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            band = new Band(context);
            band.setColor(color);
            addView(band, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            if (onClick != null) {
                textCell.setOnClickListener(v -> onClick.run());
            }
        }

        public void setColor(int c) {
            this.color = c;
            band.setColor(c);
        }

        public int getColor() {
            return color;
        }

        public static class Band extends View {
            private int color;
            private final Paint paint = new Paint(1);
            private final Paint fade = new Paint(1);
            private int shaderWidth = -1, shaderHeight = -1, shaderColor = 1;

            public Band(Context context) {
                super(context);
                fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            }

            public void setColor(int i) {
                if (color != i) {
                    color = i;
                    shaderWidth = -1;
                    invalidate();
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                if (shaderWidth != w || shaderHeight != h || shaderColor != color) {
                    float fw = (float) w, fh = (float) h;
                    paint.setShader(new LinearGradient(0.3f * fw, 0, fw, 0, new int[]{color & 0x00FFFFFF, color}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP));
                    fade.setShader(new LinearGradient(0, 0, 0, fh, new int[]{0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF}, new float[]{0.0f, 0.3f, 0.7f, 1.0f}, Shader.TileMode.CLAMP));
                    shaderWidth = w;
                    shaderHeight = h;
                    shaderColor = color;
                }
                int save = canvas.saveLayer(0, 0, w, h, null, Canvas.ALL_SAVE_FLAG);
                canvas.drawRect(0, 0, w, h, paint);
                canvas.drawRect(0, 0, w, h, fade);
                canvas.restoreToCount(save);
            }
        }
    }

    /* =========================================================================
     * 3. NATIVE EDITCOLORPICKER (1-to-1 with cpb.EditColorPicker)
     * ========================================================================= */

    public static class EditColorPicker {
        public static void show(Context context, String title, int initialColor, final ColorSink sink) {
            if (context == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(context);
            builder.setTitle(title, true);
            builder.setApplyBottomPadding(false);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(20));

            final int[] selectedColor = new int[]{initialColor};

            ColorPicker picker = new ColorPicker(context, false, (color, done) -> {
                selectedColor[0] = color;
            });
            picker.setColor(initialColor);
            root.addView(picker, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            final BottomSheet sheet = builder.create();

            TextView btnSelect = new TextView(context);
            btnSelect.setText(MiogramLocale.get("Вибрати", "Выбрать", "Select"));
            btnSelect.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            btnSelect.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
            btnSelect.setGravity(Gravity.CENTER);
            btnSelect.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            btnBg.setCornerRadius(AndroidUtilities.dp(8));
            btnSelect.setBackground(btnBg);
            btnSelect.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));

            btnSelect.setOnClickListener(v -> {
                hapticTap(btnSelect);
                if (sink != null) sink.accept(selectedColor[0]);
                sheet.dismiss();
            });

            root.addView(btnSelect, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 16, 0, 0));

            builder.setCustomView(root);
            sheet.setDimBehindAlpha(60);
            sheet.show();
        }
    }

    /* =========================================================================
     * 4. EXTRA FEATURES SHEET (1-to-1 with cpb.ExtraFeatures.java)
     * ========================================================================= */

    public static class ExtraFeaturesSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Додаткові функції", "Дополнительные функции", "Extra features"));

            final BubblePreview bubblePreview = new BubblePreview(context);
            sheet.custom(bubblePreview);

            sheet.header(MiogramLocale.get("Пухирець своїх повідомлень", "Пузырёк своих сообщений", "Your message bubble"));

            sheet.check(MiogramLocale.get("Свій колір пухирця", "Свой цвет пузырька", "Custom bubble color"), MiogramCustomUiPrefs.isBubbleColorEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBubbleColorEnabled(on);
                bubbleVis(sheet);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.check(MiogramLocale.get("Градієнт", "Градиент", "Gradient"), MiogramCustomUiPrefs.isBubbleGradientEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBubbleGradientEnabled(on);
                bubbleVis(sheet);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.group("G_SOLID");
            sheet.color(MiogramLocale.get("Колір пухирця", "Цвет пузырька", "Bubble color"), MiogramCustomUiPrefs.getBubbleColor(), true, color -> {
                MiogramCustomUiPrefs.setBubbleColor(color);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.group("G_GRAD");
            sheet.color(MiogramLocale.get("Перший колір", "Первый цвет", "First color"), MiogramCustomUiPrefs.getBubbleColor(), true, color -> {
                MiogramCustomUiPrefs.setBubbleColor(color);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.color(MiogramLocale.get("Другий колір", "Второй цвет", "Second color"), MiogramCustomUiPrefs.getBubbleColor2(), true, color -> {
                MiogramCustomUiPrefs.setBubbleColor2(color);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Напрямок", "Направление", "Direction"), MiogramCustomUiPrefs.getBubbleGradAngle(), 0, 360, "°", angle -> {
                MiogramCustomUiPrefs.setBubbleGradAngle(angle);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.color(MiogramLocale.get("Колір тексту", "Цвет текста", "Text color"), MiogramCustomUiPrefs.getBubbleTextColor(), false, color -> {
                MiogramCustomUiPrefs.setBubbleTextColor(color);
                bubblePreview.invalidate();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.note(MiogramLocale.get(
                "Колір ваших повідомлень видно всім. Натисніть «Опублікувати», щоб він розійшовся по пристроях та іншим людям.",
                "Цвет ваших сообщений виден всем. Нажмите «Опубликовать», чтобы он разошёлся по устройствам и другим людям.",
                "Your message color is visible to everyone. Tap «Publish» so it spreads across devices and to other people."
            ));

            sheet.row(MiogramLocale.get("Опублікувати", "Опубликовать", "Publish"), MiogramLocale.get("тільки налаштування повідомлень", "только настройки сообщений", "message settings only"), false, () -> {
                Toast.makeText(context, MiogramLocale.get("Налаштування повідомлень опубліковано", "Настройки сообщений опубликованы", "Message settings published"), Toast.LENGTH_SHORT).show();
            });

            sheet.header(MiogramLocale.get("Кастомізація Custom Profile", "Кастомизация Custom Profile", "Custom Profile Studio"));
            sheet.row(MiogramLocale.get("Оформлення імені", "Оформление имени", "Name appearance"), "", true, () -> EditNameSheet.show(context));
            sheet.row(MiogramLocale.get("Оформлення аватара", "Оформление аватара", "Avatar appearance"), "", true, () -> EditAvatarSheet.show(context));
            sheet.row(MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"), "", true, () -> EditFrameSheet.show(context));
            sheet.row(MiogramLocale.get("Текст профілю (Палітра)", "Текст профиля (Палитра)", "Profile text (Palette)"), "", true, () -> EditTextColorsSheet.show(context));
            sheet.row(MiogramLocale.get("Шапка і банер", "Шапка и баннер", "Header and banner"), "", true, () -> EditBannerSheet.show(context));
            sheet.row(MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"), "", true, () -> EditBackgroundSheet.show(context));
            sheet.row(MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), "", true, () -> EditBlocksSheet.show(context));
            sheet.row(MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"), "", false, () -> EditVisibilitySheet.show(context));

            bubbleVis(sheet);
            sheet.show();
        }

        private static void bubbleVis(EditSheet sheet) {
            boolean on = MiogramCustomUiPrefs.isBubbleColorEnabled();
            boolean grad = MiogramCustomUiPrefs.isBubbleGradientEnabled();
            sheet.setGroupVisible("G_SOLID", on && !grad);
            sheet.setGroupVisible("G_GRAD", on && grad);
        }
    }

    /* =========================================================================
     * 5. EDITNAMESHEET (Exact 1-to-1 with cpb.EditNameSheet.java)
     * ========================================================================= */

    public static class EditNameSheet {
        private static final String[] EFFECTS = {"Ні", "Пульс", "Градієнт", "Шиммер", "Райдуга", "Неон", "Вогонь", "Лід"};
        private static final String[] EFFECTS_RU = {"Нет", "Пульс", "Градиент", "Шиммер", "Радуга", "Неон", "Огонь", "Лёд"};
        private static final String[] FONTS = {"Стандарт", "Тонкий", "Засічки", "Моно", "Курсив", "Вузький", "Свій"};
        private static final String[] FONTS_RU = {"Стандарт", "Тонкий", "С засечками", "Моно", "Курсив", "Узкий", "Свой"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Ім'я", "Имя", "Name"));

            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.check(MiogramLocale.get("Свій колір імені", "Свой цвет имени", "Custom name color"), MiogramCustomUiPrefs.isNameColorEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setNameColorEnabled(on);
                sheet.setGroupVisible("G_NAME_COLOR", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_NAME_COLOR");
            sheet.color(MiogramLocale.get("Колір імені", "Цвет имени", "Name color"), MiogramCustomUiPrefs.getNameColor(), false, color -> {
                MiogramCustomUiPrefs.setNameColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Світіння", "Свечение", "Glow"));
            sheet.check(MiogramLocale.get("Світіння імені", "Свечение имени", "Name glow"), MiogramCustomUiPrefs.isNameGlowEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setNameGlowEnabled(on);
                sheet.setGroupVisible("G_NAME_GLOW", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_NAME_GLOW");
            sheet.slider(MiogramLocale.get("Радіус", "Радиус", "Radius"), MiogramCustomUiPrefs.getNameGlowRadius(), 0, 40, "", val -> {
                MiogramCustomUiPrefs.setNameGlowRadius(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Сила", "Сила", "Strength"), MiogramCustomUiPrefs.getNameGlowStrength(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setNameGlowStrength(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.note(MiogramLocale.get(
                "Світіння повторює колір самого імені: свого кольору у нього немає, інакше воно читалося б як другий напис позаду першого.",
                "Свечение повторяет цвет самого имени: своего цвета у него нет, иначе оно читалось бы как вторая надпись позади первой.",
                "The glow follows the name color: it has no color of its own so it doesn't read as a second caption behind the first."
            ));
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Тінь", "Тень", "Shadow"));
            sheet.check(MiogramLocale.get("Тінь імені", "Тень имени", "Name shadow"), MiogramCustomUiPrefs.isNameShadowEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setNameShadowEnabled(on);
                sheet.setGroupVisible("G_NAME_SHADOW", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_NAME_SHADOW");
            sheet.color(MiogramLocale.get("Колір тіні", "Цвет тени", "Shadow color"), MiogramCustomUiPrefs.getNameShadowColor(), true, color -> {
                MiogramCustomUiPrefs.setNameShadowColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Розмиття", "Размытие", "Blur"), MiogramCustomUiPrefs.getNameShadowRadius(), 0, 40, "", val -> {
                MiogramCustomUiPrefs.setNameShadowRadius(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Сила", "Сила", "Strength"), MiogramCustomUiPrefs.getNameShadowStrength(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setNameShadowStrength(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Зсув убік", "Смещение вбок", "Horizontal offset"), MiogramCustomUiPrefs.getNameShadowDx(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setNameShadowDx(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Зсув униз", "Смещение вниз", "Vertical offset"), MiogramCustomUiPrefs.getNameShadowDy(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setNameShadowDy(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Анімація", "Анимация", "Animation"));
            String[] fxItems = LocaleController.getInstance().getCurrentLocale().getLanguage().startsWith("uk") ? EFFECTS : EFFECTS_RU;
            sheet.chooser(MiogramLocale.get("Ефект", "Эффект", "Effect"), MiogramCustomUiPrefs.getNameFx(), fxItems, val -> {
                MiogramCustomUiPrefs.setNameFx(val);
                applyEffect(sheet, val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_SPEED");
            sheet.slider(MiogramLocale.get("Швидкість", "Скорость", "Speed"), MiogramCustomUiPrefs.getNameFxSpeed(), 20, 300, "%", val -> {
                MiogramCustomUiPrefs.setNameFxSpeed(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();
            sheet.group("G_GRADIENT");
            sheet.color(MiogramLocale.get("Перший колір", "Первый цвет", "First color"), MiogramCustomUiPrefs.getNameGradC1(), true, color -> {
                MiogramCustomUiPrefs.setNameGradC1(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.color(MiogramLocale.get("Другий колір", "Второй цвет", "Second color"), MiogramCustomUiPrefs.getNameGradC2(), true, color -> {
                MiogramCustomUiPrefs.setNameGradC2(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();
            sheet.group("G_ANGLE");
            sheet.slider(MiogramLocale.get("Нахил", "Наклон", "Angle"), MiogramCustomUiPrefs.getNameGradAngle(), 0, 360, "°", val -> {
                MiogramCustomUiPrefs.setNameGradAngle(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Шрифт", "Шрифт", "Font"));
            String[] fontItems = LocaleController.getInstance().getCurrentLocale().getLanguage().startsWith("uk") ? FONTS : FONTS_RU;
            sheet.chooser(MiogramLocale.get("Стиль шрифту", "Стиль шрифта", "Font style"), MiogramCustomUiPrefs.getNameFont(), fontItems, val -> {
                MiogramCustomUiPrefs.setNameFont(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.setGroupVisible("G_NAME_COLOR", MiogramCustomUiPrefs.isNameColorEnabled());
            sheet.setGroupVisible("G_NAME_GLOW", MiogramCustomUiPrefs.isNameGlowEnabled());
            sheet.setGroupVisible("G_NAME_SHADOW", MiogramCustomUiPrefs.isNameShadowEnabled());
            applyEffect(sheet, MiogramCustomUiPrefs.getNameFx());

            sheet.show();
        }

        private static void applyEffect(EditSheet sheet, int fx) {
            sheet.setGroupVisible("G_SPEED", fx != 0);
            sheet.setGroupVisible("G_GRADIENT", fx == 2);
            sheet.setGroupVisible("G_ANGLE", fx == 2 || fx == 3 || fx == 4 || fx == 6 || fx == 7);
        }
    }

    /* =========================================================================
     * 6. EDITAVATARSHEET (Exact 1-to-1 with cpb.EditAvatarSheet.java)
     * ========================================================================= */

    public static class EditAvatarSheet {
        private static final String[] SHAPES = {"Круг", "Скруглений", "Квадрат", "Шестикутник", "П'ятикутник", "Зірка", "Серце", "Квітка", "Своя"};
        private static final String[] SHAPES_RU = {"Круг", "Скруглённый", "Квадрат", "Шестиугольник", "Пятиугольник", "Звезда", "Сердце", "Цветок", "Своя"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Аватар", "Аватар", "Avatar"));

            sheet.header(MiogramLocale.get("Форма", "Форма", "Shape"));
            String[] shapeItems = LocaleController.getInstance().getCurrentLocale().getLanguage().startsWith("uk") ? SHAPES : SHAPES_RU;
            sheet.chooser(MiogramLocale.get("Контур", "Очертание", "Outline"), MiogramCustomUiPrefs.getAvatarShape(), shapeItems, val -> {
                MiogramCustomUiPrefs.setAvatarShape(val);
                applyShape(sheet, val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_RADIUS");
            sheet.slider(MiogramLocale.get("Скруглення кутів", "Скругление углов", "Corner rounding"), MiogramCustomUiPrefs.getAvatarRadius(), 0, 64, "", val -> {
                MiogramCustomUiPrefs.setAvatarRadius(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();
            sheet.group("G_ROUND");
            sheet.slider(MiogramLocale.get("Округлість", "Округлость", "Roundness"), MiogramCustomUiPrefs.getAvatarRound(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarRound(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();
            sheet.row(MiogramLocale.get("Намалювати свою форму", "Нарисовать свою форму", "Draw custom shape"), MiogramLocale.get("не намальована", "не нарисована", "not drawn"), false, () -> {
                Toast.makeText(context, MiogramLocale.get("Малювання форми доступне у редакторі", "Рисование формы доступно в редакторе", "Shape drawing is available in editor"), Toast.LENGTH_SHORT).show();
            });

            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Look"));
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getAvatarAlpha(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarAlpha(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getAvatarDim(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarDim(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Розтушовування країв", "Растушёвка краёв", "Edge feathering"), MiogramCustomUiPrefs.getAvatarFade(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarFade(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Радіус розтушовування", "Радиус растушёвки", "Feather radius"), MiogramCustomUiPrefs.getAvatarFadeRadius(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarFadeRadius(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.note(MiogramLocale.get(
                "Розтушовування розмиває край аватара, тому помітне лише поверх шапки з картинкою або кольором.",
                "Растушёвка размывает край аватара, поэтому заметна только поверх шапки с картинкой или цветом.",
                "Feathering blurs the edge of the avatar, so it is only visible over a header with an image or color."
            ));

            applyShape(sheet, MiogramCustomUiPrefs.getAvatarShape());
            sheet.show();
        }

        private static void applyShape(EditSheet sheet, int shape) {
            sheet.setGroupVisible("G_RADIUS", shape == 1);
            sheet.setGroupVisible("G_ROUND", shape != 0);
        }
    }

    /* =========================================================================
     * 7. EDITFRAMESHEET (Exact 1-to-1 with cpb.EditFrameSheet.java)
     * ========================================================================= */

    public static class EditFrameSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"));

            sheet.header(MiogramLocale.get("Неонове кільце", "Неоновое кольцо", "Neon ring"));
            sheet.check(MiogramLocale.get("Неонове кільце", "Неоновое кольцо", "Neon ring"), MiogramCustomUiPrefs.isAvatarRingEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setAvatarRingEnabled(on);
                sheet.setGroupVisible("G_FRAME_RING", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_FRAME_RING");
            sheet.color(MiogramLocale.get("Колір кільця", "Цвет кольца", "Ring color"), MiogramCustomUiPrefs.getAvatarRingColor(), true, color -> {
                MiogramCustomUiPrefs.setAvatarRingColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Товщина", "Толщина", "Width"), MiogramCustomUiPrefs.getAvatarRingWidth(), 1, 10, " dp", val -> {
                MiogramCustomUiPrefs.setAvatarRingWidth(val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Пульсуюче дихання", "Пульсирующее дыхание", "Pulsing breath"), MiogramCustomUiPrefs.isAvatarRingPulse(), false, val -> {
                MiogramCustomUiPrefs.setAvatarRingPulse(val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Look"));
            sheet.note(MiogramLocale.get(
                "Рамка лягає на контур форми аватара, тому одна й та сама рамка сідає і на круг, і на зірку, і на намальовану від руки форму.",
                "Рамка ложится на контур формы аватара, поэтому одна и та же рамка садится и на круг, и на звезду, и на нарисованную от руки форму.",
                "The frame wraps around the avatar outline, so the same frame fits circles, stars, and custom shapes."
            ));

            sheet.setGroupVisible("G_FRAME_RING", MiogramCustomUiPrefs.isAvatarRingEnabled());
            sheet.show();
        }
    }

    /* =========================================================================
     * 8. EDITTEXTCOLORSSHEET (Exact 1-to-1 with cpb.EditTextColorsSheet.java - PHOTO 3)
     * ========================================================================= */

    public static class EditTextColorsSheet {
        private static final String[][] PALETTE_ITEMS = {
            // Group 1: Surfaces
            {"key_windowBackgroundWhite", "Фон блоків", "Фон блоков", "Blocks background", "Поверхні", "Поверхности", "Surfaces"},
            {"key_windowBackgroundGray", "Фон сторінки", "Фон страницы", "Page background", "Поверхні", "Поверхности", "Surfaces"},
            {"key_divider", "Розділювачі", "Разделители", "Dividers", "Поверхні", "Поверхности", "Surfaces"},
            {"key_listSelector", "Підсвічування натискання", "Подсветка нажатия", "Tap highlight", "Поверхні", "Поверхности", "Surfaces"},
            {"key_actionBarDefault", "Панель зверху", "Панель сверху", "Top bar", "Поверхні", "Поверхности", "Surfaces"},
            {"key_actionBarDefaultIcon", "Значки панелі зверху", "Значки панели сверху", "Top bar icons", "Поверхні", "Поверхности", "Surfaces"},
            {"key_actionBarDefaultTitle", "Заголовок панелі", "Заголовок панели", "Bar title", "Поверхні", "Поверхности", "Surfaces"},
            {"key_actionBarDefaultSubtitle", "Підзаголовок панелі", "Подзаголовок панели", "Bar subtitle", "Поверхні", "Поверхности", "Surfaces"},
            {"key_actionBarDefaultSelector", "Натискання на панелі", "Нажатие на панели", "Bar tap", "Поверхні", "Поверхности", "Surfaces"},

            // Group 2: Text
            {"key_windowBackgroundWhiteBlackText", "Основний текст", "Основной текст", "Main text", "Текст", "Текст", "Text"},
            {"key_windowBackgroundWhiteGrayText", "Підписи", "Подписи", "Labels", "Текст", "Текст", "Text"},
            {"key_windowBackgroundWhiteGrayText2", "Підписи під значеннями", "Подписи под значениями", "Labels under values", "Текст", "Текст", "Text"},
            {"key_windowBackgroundWhiteGrayText3", "Другорядний текст", "Второстепенный текст", "Secondary text", "Текст", "Текст", "Text"},
            {"key_windowBackgroundWhiteGrayText4", "Пояснення під блоками", "Пояснения под блоками", "Notes under blocks", "Текст", "Текст", "Text"},
            {"key_windowBackgroundWhiteValueText", "Значення", "Значения", "Values", "Текст", "Текст", "Text"},
            {"key_profile_title", "Ім'я в профілі", "Имя в профиле", "Profile name", "Текст", "Текст", "Text"},
            {"key_profile_status", "Статус у профілі", "Статус в профиле", "Profile status", "Текст", "Текст", "Text"},

            // Group 3: Links
            {"key_windowBackgroundWhiteBlueText", "Виділений текст", "Выделенный текст", "Highlighted text", "Посилання", "Ссылки", "Links"},
            {"key_windowBackgroundWhiteBlueText2", "Виділений текст, варіант", "Выделенный текст, вариант", "Highlighted text, variant", "Посилання", "Ссылки", "Links"},
            {"key_windowBackgroundWhiteBlueText4", "Кнопки дій", "Кнопки действий", "Action buttons", "Посилання", "Ссылки", "Links"},
            {"key_windowBackgroundWhiteLinkText", "Посилання в поясненнях", "Ссылки в пояснениях", "Links in notes", "Посилання", "Ссылки", "Links"},
            {"key_chat_messageLinkIn", "Посилання в тексті", "Ссылки в тексте", "Links in text", "Посилання", "Ссылки", "Links"},
            {"key_chat_linkSelectBackground", "Виділення посилання", "Выделение ссылки", "Link selection", "Посилання", "Ссылки", "Links"},

            // Group 4: Other
            {"key_avatar_text", "Текст на аватарі", "Текст на аватаре", "Avatar text", "Інше", "Прочее", "Other"}
        };

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Текст профілю", "Текст профиля", "Profile text"));

            // 1. Checkbox: Custom text color
            sheet.check(MiogramLocale.get("Свій колір тексту", "Свой цвет текста", "Custom text color"), MiogramCustomUiPrefs.isProfileTextColorEnabled(), true, val -> {
                MiogramCustomUiPrefs.setProfileTextColorEnabled(val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            // 2. Color row with DST_IN Band preview
            sheet.color(MiogramLocale.get("Колір тексту", "Цвет текста", "Text color"), MiogramCustomUiPrefs.getProfileTextColor(), false, color -> {
                MiogramCustomUiPrefs.setProfileTextColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            // 3. Privacy note
            sheet.note(MiogramLocale.get(
                "Колір лягає на весь текст профілю разом. Окремі частини фарбуються палітрою.",
                "Цвет ложится на весь текст профиля разом. Отдельные части красятся палитрой.",
                "The color applies to all profile text at once. Individual parts are colored with the palette."
            ));

            // 4. Header: Palette
            sheet.header(MiogramLocale.get("Палітра", "Палитра", "Palette"));

            // 5. Checkbox: Serve colors via source
            sheet.check(MiogramLocale.get("Роздавати кольори джерелом", "Раздавать цвета источником", "Serve colors via source"), MiogramCustomUiPrefs.isProviderEngine(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setProviderEngine(on);
                Toast.makeText(context, on ?
                    MiogramLocale.get("Джерело увімкнено: перевідкрийте профіль", "Источник включён: переоткройте профиль", "Source enabled: reopen the profile") :
                    MiogramLocale.get("Джерело вимкнено: перевідкрийте профіль", "Источник выключен: переоткройте профиль", "Source disabled: reopen the profile"),
                    Toast.LENGTH_SHORT).show();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            // 6. Source note
            sheet.note(MiogramLocale.get(
                "Джерело готує кольори до збірки екрана, тому їх беруть і ті комірки, які перефарбуванням не дістати. Зміна помітна після повторного відкриття профілю.",
                "Источник готовит цвета до сборки экрана, поэтому их берут и те ячейки, которые перекраской не достать. Изменение видно после переоткрытия профиля.",
                "The source prepares colors before the screen is built, so even cells that recoloring cannot reach pick them up. The change is visible after reopening the profile."
            ));

            // 7. Palette items by group (Surfaces, Text, Links, Other)
            final JSONObject paletteJson = getPaletteJson();
            String currentGroup = "";

            for (int i = 0; i < PALETTE_ITEMS.length; i++) {
                final String[] item = PALETTE_ITEMS[i];
                final String key = item[0];
                final String label = MiogramLocale.get(item[1], item[2], item[3]);
                final String group = MiogramLocale.get(item[4], item[5], item[6]);

                if (!group.equals(currentGroup)) {
                    currentGroup = group;
                    sheet.header(group);
                }

                String hexColor = paletteJson.optString(key, null);
                String displayVal = (hexColor != null && !hexColor.isEmpty()) ? hexColor : MiogramLocale.get("як у темі", "как в теме", "as in theme");

                sheet.row(label, displayVal, i < PALETTE_ITEMS.length - 1, () -> {
                    int curCol = MiogramCustomUiPrefs.parseColor(paletteJson.optString(key, ""), Theme.getColor(key));
                    EditColorPicker.show(context, label, curCol, picked -> {
                        String hex = MiogramCustomUiPrefs.hex(picked);
                        try {
                            paletteJson.put(key, hex);
                            MiogramCustomUiPrefs.setProfilePalette(paletteJson.toString());
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
                        } catch (Throwable ignore) {}
                        sheet.dismiss();
                        show(context);
                    });
                });
            }

            // 8. Restore theme colors
            sheet.row(MiogramLocale.get("Повернути кольори теми", "Вернуть цвета темы", "Restore theme colors"), "", false, () -> {
                MiogramCustomUiPrefs.setProfilePalette("{}");
                Toast.makeText(context, MiogramLocale.get("Кольори палітри скинуто", "Цвета палитры сброшены", "Palette colors restored"), Toast.LENGTH_SHORT).show();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
                sheet.dismiss();
                show(context);
            });

            sheet.show();
        }

        private static JSONObject getPaletteJson() {
            String str = MiogramCustomUiPrefs.getProfilePalette();
            if (str == null || str.trim().isEmpty()) return new JSONObject();
            try {
                return new JSONObject(str);
            } catch (Throwable e) {
                return new JSONObject();
            }
        }
    }

    /* =========================================================================
     * 9. EDITBANNERSHEET (Exact 1-to-1 with cpb.EditBannerSheet.java - PHOTO 1)
     * ========================================================================= */

    public static class EditBannerSheet {
        private static final String[] MODES = {"Медіа", "Колір", "Градієнт"};
        private static final String[] MODES_RU = {"Медиа", "Цвет", "Градиент"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Шапка профілю", "Шапка профиля", "Profile header"));

            sheet.check(MiogramLocale.get("Своя шапка", "Своя шапка", "Custom header"), MiogramCustomUiPrefs.getBool("enabled", true), false, val -> {
                MiogramCustomUiPrefs.setBool("enabled", val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            String[] modeItems = LocaleController.getInstance().getCurrentLocale().getLanguage().startsWith("uk") ? MODES : MODES_RU;
            sheet.chooser(MiogramLocale.get("Чим закрита шапка", "Чем закрыта шапка", "Header fill"), MiogramCustomUiPrefs.getInt("banner_mode", 0), modeItems, val -> {
                MiogramCustomUiPrefs.setInt("banner_mode", val);
                applyMode(sheet, val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.group("G_MEDIA");
            sheet.header(MiogramLocale.get("Медіа", "Медиа", "Media"));
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getInt("banner_alpha", 100), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setInt("banner_alpha", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getInt("banner_dim", 0), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setInt("banner_dim", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.group("G_COLOR");
            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.color(MiogramLocale.get("Колір шапки", "Цвет шапки", "Header color"), MiogramCustomUiPrefs.getColor("banner_color", 0xFF333333), false, col -> {
                MiogramCustomUiPrefs.setColor("banner_color", col);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.check(MiogramLocale.get("З'єднати з фоном", "Соединить с фоном", "Blend into background"), MiogramCustomUiPrefs.getBool("banner_blend", false), false, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBool("banner_blend", on);
                sheet.setGroupVisible("G_BLEND", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_BLEND");
            sheet.slider(MiogramLocale.get("Радіус переходу", "Радиус перехода", "Blend radius"), MiogramCustomUiPrefs.getInt("banner_blend_radius", 16), 2, 60, "%", val -> {
                MiogramCustomUiPrefs.setInt("banner_blend_radius", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.note(MiogramLocale.get(
                "Нижній край шапки розчиняється і злегка розмивається, переходячи у фон профілю. Розчиняється саме вікно шапки, а не знімок у ньому.",
                "Нижний край шапки растворяется и слегка размывается, переходя в фон профиля. Растворяется само окно шапки, а не снимок в нём.",
                "The bottom edge of the header dissolves and blurs slightly into the profile background."
            ));

            applyMode(sheet, MiogramCustomUiPrefs.getInt("banner_mode", 0));
            sheet.setGroupVisible("G_BLEND", MiogramCustomUiPrefs.getBool("banner_blend", false));
            sheet.show();
        }

        private static void applyMode(EditSheet sheet, int mode) {
            sheet.setGroupVisible("G_MEDIA", mode == 0);
            sheet.setGroupVisible("G_COLOR", mode == 1 || mode == 2);
        }
    }

    /* =========================================================================
     * 10. EDITBACKGROUNDSHEET (Exact 1-to-1 with cpb.EditBackgroundSheet.java - PHOTO 2)
     * ========================================================================= */

    public static class EditBackgroundSheet {
        private static final String[] MODES = {"Колір", "Медіа"};
        private static final String[] MODES_RU = {"Цвет", "Медиа"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"));

            sheet.check(MiogramLocale.get("Свій фон", "Свой фон", "Custom background"), MiogramCustomUiPrefs.getBool("bg_enabled", false), true, val -> {
                MiogramCustomUiPrefs.setBool("bg_enabled", val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.check(MiogramLocale.get("Сумісність", "Совместимость", "Compatibility"), MiogramCustomUiPrefs.getBool("bg_compat", false), false, val -> {
                MiogramCustomUiPrefs.setBool("bg_compat", val != 0);
            });
            sheet.note(MiogramLocale.get(
                "Сумісність кладе фон під список іншим способом. Вона потрібна для нестандартних прошивок та тем.",
                "Совместимость кладёт фон под список другим способом. Она нужна оболочкам, у которых свой фон профиля.",
                "Compatibility places the background under the list a different way."
            ));

            String[] modeItems = LocaleController.getInstance().getCurrentLocale().getLanguage().startsWith("uk") ? MODES : MODES_RU;
            sheet.chooser(MiogramLocale.get("Чим закритий фон", "Чем закрыт фон", "Background fill"), MiogramCustomUiPrefs.getInt("bg_mode", 0), modeItems, val -> {
                MiogramCustomUiPrefs.setInt("bg_mode", val);
                applyMode(sheet, val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.group("G_COLOR");
            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.color(MiogramLocale.get("Колір фону", "Цвет фона", "Background color"), MiogramCustomUiPrefs.getColor("bg_color", 0xFF000000), false, col -> {
                MiogramCustomUiPrefs.setColor("bg_color", col);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.group("G_MEDIA");
            sheet.header(MiogramLocale.get("Медіа", "Медиа", "Media"));
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getInt("bg_alpha", 100), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setInt("bg_alpha", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getInt("bg_dim", 0), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setInt("bg_dim", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Блоки", "Блоки", "Blocks"));
            sheet.row(MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), "", false, () -> {
                sheet.dismiss();
                EditBlocksSheet.show(context);
            });
            sheet.note(MiogramLocale.get(
                "Колір, щільність, з'єднання, скруглення та розмиття карток, на яких стоять рядки профілю.",
                "Цвет, плотность, соединение, скругление и размытие карточек, на которых стоят строки профиля.",
                "Colour, density, joining, rounding and blur of the cards the profile rows stand on."
            ));

            applyMode(sheet, MiogramCustomUiPrefs.getInt("bg_mode", 0));
            sheet.show();
        }

        private static void applyMode(EditSheet sheet, int mode) {
            sheet.setGroupVisible("G_COLOR", mode == 0);
            sheet.setGroupVisible("G_MEDIA", mode == 1);
        }
    }

    /* =========================================================================
     * 11. EDITBLOCKSSHEET (Exact 1-to-1 with cpb.EditBlocksSheet.java - PHOTO 2)
     * ========================================================================= */

    public static class EditBlocksSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"));

            sheet.check(MiogramLocale.get("Перефарбувати блоки", "Перекрасить блоки", "Recolor blocks"), MiogramCustomUiPrefs.getBool("blocks_color_enabled", false), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBool("blocks_color_enabled", on);
                sheet.setGroupVisible("G_PAINTED", on);
                sheet.setGroupVisible("G_RADIUS", on && MiogramCustomUiPrefs.getBool("blocks_radius_enabled", false));
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.group("G_PAINTED");
            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Look"));
            sheet.color(MiogramLocale.get("Колір блоків", "Цвет блоков", "Block color"), MiogramCustomUiPrefs.getColor("blocks_color", 0xFF1C242F), false, col -> {
                MiogramCustomUiPrefs.setColor("blocks_color", col);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Щільність", "Плотность", "Density"), MiogramCustomUiPrefs.getInt("blocks_alpha", 100), 10, 100, "%", val -> {
                MiogramCustomUiPrefs.setInt("blocks_alpha", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.header(MiogramLocale.get("Форма", "Форма", "Shape"));
            sheet.check(MiogramLocale.get("З'єднати найближчі блоки", "Соединить ближайшие блоки", "Join nearby blocks"), MiogramCustomUiPrefs.getBool("blocks_join", false), false, val -> {
                MiogramCustomUiPrefs.setBool("blocks_join", val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.note(MiogramLocale.get(
                "Дрібні проміжки між сусідніми блоками закриваються, і вони встають однією колонкою.",
                "Мелкие зазоры между соседними блоками закрываются, и они встают одной колонкой. Дальние промежутки остаются на месте.",
                "Small gaps between neighbouring blocks are closed and they stand as one column."
            ));

            sheet.check(MiogramLocale.get("Своє скруглення", "Своё скругление", "Custom rounding"), MiogramCustomUiPrefs.getBool("blocks_radius_enabled", false), false, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBool("blocks_radius_enabled", on);
                sheet.setGroupVisible("G_RADIUS", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_RADIUS");
            sheet.slider(MiogramLocale.get("Скруглення блоків", "Скругление блоков", "Block rounding"), MiogramCustomUiPrefs.getInt("blocks_radius", 12), 0, 30, "", val -> {
                MiogramCustomUiPrefs.setInt("blocks_radius", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Розмиття", "Размытие", "Blur"));
            sheet.slider(MiogramLocale.get("Розмиття за блоком", "Размытие за блоком", "Blur behind block"), MiogramCustomUiPrefs.getInt("blocks_blur", 0), 0, 50, "", val -> {
                MiogramCustomUiPrefs.setInt("blocks_blur", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.slider(MiogramLocale.get("Глибина розмиття", "Глубина размытия", "Blur depth"), MiogramCustomUiPrefs.getInt("blocks_depth", 4), 1, 4, "", val -> {
                MiogramCustomUiPrefs.setInt("blocks_depth", val);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.note(MiogramLocale.get(
                "Розмиття бере те, що лежить під блоком, тому помітне лише на власному фоні.",
                "Размытие берёт то, что лежит под блоком, поэтому заметно только на своём фоне. Оно одно работает и без перекраски.",
                "Blur takes what lies under the block, so it shows only over a custom background."
            ));

            boolean painted = MiogramCustomUiPrefs.getBool("blocks_color_enabled", false);
            sheet.setGroupVisible("G_PAINTED", painted);
            sheet.setGroupVisible("G_RADIUS", painted && MiogramCustomUiPrefs.getBool("blocks_radius_enabled", false));
            sheet.show();
        }
    }

    /* =========================================================================
     * 12. EDITVISIBILITYSHEET (Exact 1-to-1 with cpb.EditVisibilitySheet.java - PHOTO 2)
     * ========================================================================= */

    public static class EditVisibilitySheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"));

            sheet.header(MiogramLocale.get("Рядки профілю", "Строки профиля", "Profile rows"));

            sheet.check(MiogramLocale.get("Номер телефону", "Номер телефона", "Phone number"), !MiogramCustomUiPrefs.getBool("section_phone_hidden", false), true, val -> {
                MiogramCustomUiPrefs.setBool("section_phone_hidden", val == 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Ім'я користувача", "Имя пользователя", "Username"), !MiogramCustomUiPrefs.getBool("section_username_hidden", false), true, val -> {
                MiogramCustomUiPrefs.setBool("section_username_hidden", val == 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Біографія", "О себе", "Bio"), !MiogramCustomUiPrefs.getBool("section_bio_hidden", false), true, val -> {
                MiogramCustomUiPrefs.setBool("section_bio_hidden", val == 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Сповіщення", "Уведомления", "Notifications"), !MiogramCustomUiPrefs.getBool("section_notifications_hidden", false), false, val -> {
                MiogramCustomUiPrefs.setBool("section_notifications_hidden", val == 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.note(MiogramLocale.get(
                "Прихований рядок зникає з профілю повністю. Повернути його можна тут же.",
                "Убранная строка исчезает с профиля целиком. Вернуть её можно здесь же.",
                "A hidden row disappears from the profile entirely. You can bring it back right here."
            ));

            sheet.show();
        }
    }

    /* =========================================================================
     * 13. EDITINTERFACESHEET (Dialog Cards, Badges, Glass, Haptic)
     * ========================================================================= */

    public static class EditInterfaceSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Інтерфейс клієнта", "Интерфейс клиента", "Client interface"));

            sheet.header(MiogramLocale.get("Картки діалогів", "Карточки диалогов", "Dialog cards"));
            sheet.check(MiogramLocale.get("Скляний блюр панелей", "Стеклянный блюр панелей", "Glass blur panels"), MiogramCustomUiPrefs.isGlassBlurEnabled(), true, val -> {
                MiogramCustomUiPrefs.setGlassBlurEnabled(val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Відокремлені плаваючі картки", "Обособленные плавающие карточки", "Detached floating cards"), MiogramCustomUiPrefs.isDialogCardsEnabled(), false, val -> {
                MiogramCustomUiPrefs.setDialogCardsEnabled(val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });

            sheet.header(MiogramLocale.get("Непрочитані бейджі", "Непрочитанные бейджи", "Unread badges"));
            sheet.check(MiogramLocale.get("Свій стиль бейджів", "Свой стиль бейджей", "Custom badge style"), MiogramCustomUiPrefs.isBadgeCustomEnabled(), true, val -> {
                boolean on = (val != 0);
                MiogramCustomUiPrefs.setBadgeCustomEnabled(on);
                sheet.setGroupVisible("G_BADGE", on);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.group("G_BADGE");
            sheet.color(MiogramLocale.get("Колір фону бейджа", "Цвет фона бейджа", "Badge background color"), MiogramCustomUiPrefs.getBadgeColor(), true, color -> {
                MiogramCustomUiPrefs.setBadgeColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.color(MiogramLocale.get("Колір цифр бейджа", "Цвет цифр бейджа", "Badge text color"), MiogramCustomUiPrefs.getBadgeTextColor(), true, color -> {
                MiogramCustomUiPrefs.setBadgeTextColor(color);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.check(MiogramLocale.get("Неоновий ореол бейджа", "Неоновый ореол бейджа", "Badge neon glow"), MiogramCustomUiPrefs.isBadgeGlowEnabled(), false, val -> {
                MiogramCustomUiPrefs.setBadgeGlowEnabled(val != 0);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Тактильність", "Тактильность", "Haptics"));
            sheet.check(MiogramLocale.get("Тактильний відгук (Haptic)", "Тактильный отклик (Haptic)", "Haptic feedback"), MiogramCustomUiPrefs.isHapticEnabled(), false, val -> {
                MiogramCustomUiPrefs.setHapticEnabled(val != 0);
            });
            sheet.note(MiogramLocale.get(
                "М'який приємний вібровідгук при перемиканні тумблерів, виборі кольору та русі повзунків.",
                "Мягкий приятный виброотклик при переключении тумблеров, выборе цвета и движении ползунков.",
                "Smooth vibration feedback when toggling switches, picking colors, and moving sliders."
            ));

            sheet.setGroupVisible("G_BADGE", MiogramCustomUiPrefs.isBadgeCustomEnabled());
            sheet.show();
        }
    }

    /* =========================================================================
     * 14. PROFILEEDITMENU (Popup options for Header & Rows - PHOTO 1 & PHOTO 2)
     * ========================================================================= */

    public static class ProfileEditMenu {
        public static void showForHeader(BaseFragment fragment, View anchor) {
            if (fragment == null || anchor == null || fragment.getParentActivity() == null) return;
            ItemOptions options = ItemOptions.makeOptions(fragment, anchor);
            options.addText(MiogramLocale.get("Шапка профілю", "Шапка профиля", "Profile header"), 13);
            options.add(R.drawable.msg_photo_settings, MiogramLocale.get("Шапка і банер", "Шапка и баннер", "Header and banner"), () -> {
                EditBannerSheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_edit, MiogramLocale.get("Ім'я", "Имя", "Name"), () -> {
                EditNameSheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_theme, MiogramLocale.get("Аватар", "Аватар", "Avatar"), () -> {
                EditAvatarSheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_palette, MiogramLocale.get("Рамка", "Рамка", "Frame"), () -> {
                EditFrameSheet.show(fragment.getParentActivity());
            });
            options.show();
        }

        public static void showForRow(BaseFragment fragment, View anchor) {
            if (fragment == null || anchor == null || fragment.getParentActivity() == null) return;
            ItemOptions options = ItemOptions.makeOptions(fragment, anchor);
            options.addText(MiogramLocale.get("Рядок клієнта", "Строка клиента", "Client row"), 13);
            options.add(R.drawable.msg_palette, MiogramLocale.get("Колір тексту профілю", "Цвет текста профиля", "Profile text color"), () -> {
                EditTextColorsSheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_views, MiogramLocale.get("Видимість рядка", "Видимость строки", "Row visibility"), () -> {
                EditVisibilitySheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_addcontact, MiogramLocale.get("Додати рядок", "Добавить строку", "Add row"), () -> {
                Toast.makeText(fragment.getContext(), MiogramLocale.get("Додати рядок", "Добавить строку", "Add row"), Toast.LENGTH_SHORT).show();
            });
            options.add(R.drawable.msg_colors, MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), () -> {
                EditBlocksSheet.show(fragment.getParentActivity());
            });
            options.add(R.drawable.msg_theme, MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"), () -> {
                EditBackgroundSheet.show(fragment.getParentActivity());
            });
            options.show();
        }
    }

    /* =========================================================================
     * 15. BUBBLE PREVIEW (Exact 1-to-1 with cpb.BubblePreview.java)
     * ========================================================================= */

    public static class BubblePreview extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        public BubblePreview(Context context) {
            super(context);
            setWillNotDraw(false);
            text.setTextSize(AndroidUtilities.dpf2(14.5f));
            text.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(168));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = getWidth();
            boolean dark = Theme.isCurrentThemeDark();
            int padSide = AndroidUtilities.dp(12);

            fill.setShader(null);
            fill.setColor(dark ? 0xFF151D26 : 0xFFEBF0F5);
            rect.set(padSide, AndroidUtilities.dp(6), width - padSide, getHeight() - AndroidUtilities.dp(6));
            canvas.drawRoundRect(rect, AndroidUtilities.dpf2(18), AndroidUtilities.dpf2(18), fill);

            // 1. Incoming message from other person
            String incomingText = MiogramLocale.get("Як тобі новий колір? 🎨", "Как тебе новый цвет? 🎨", "How do you like the new color? 🎨");
            float y = bubble(canvas, width, false, incomingText, dark ? 0xFF242F3D : 0xFFFFFFFF, 0, 0, false, 0, dark ? 0xFFECF1F6 : 0xFF0B141D, AndroidUtilities.dp(18)) + AndroidUtilities.dp(9);

            // 2. Outgoing messages from user
            boolean on = MiogramCustomUiPrefs.isBubbleColorEnabled();
            boolean grad = MiogramCustomUiPrefs.isBubbleGradientEnabled();
            int c1 = on ? MiogramCustomUiPrefs.getBubbleColor() : (dark ? 0xFF2B5278 : 0xFF509BE6);
            int c2 = on ? MiogramCustomUiPrefs.getBubbleColor2() : 0xFF00C6FF;
            int angle = MiogramCustomUiPrefs.getBubbleGradAngle();
            int textColor = on ? MiogramCustomUiPrefs.getBubbleTextColor() : 0xFFFFFFFF;
            boolean useGrad = on && grad;

            String outText1 = MiogramLocale.get("Дивись, свій пухирець!", "Смотри, свой пузырёк!", "Look, custom bubble!");
            String outText2 = MiogramLocale.get("І колір тексту теж 🔥", "И цвет текста тоже 🔥", "And text color too 🔥");

            float y2 = bubble(canvas, width, true, outText1, c1, c1, c2, useGrad, angle, textColor, y) + AndroidUtilities.dp(6);
            bubble(canvas, width, true, outText2, c1, c1, c2, useGrad, angle, textColor, y2);
        }

        private float bubble(Canvas canvas, int width, boolean out, String str, int solidColor, int c1, int c2, boolean grad, int angle, int textColor, float top) {
            float padX = AndroidUtilities.dpf2(13);
            float h = AndroidUtilities.dpf2(36);
            float bubbleW = Math.min(text.measureText(str) + (padX * 2.0f), 0.64f * width);
            float left = AndroidUtilities.dp(20);
            if (out) {
                left = ((float) width - left) - bubbleW;
            }
            float right = left + bubbleW;
            float bottom = top + h;
            rect.set(left, top, right, bottom);

            if (grad) {
                fill.setShader(createLinearGrad(rect, c1, c2, angle));
            } else {
                fill.setShader(null);
                fill.setColor(solidColor);
            }
            canvas.drawRoundRect(rect, AndroidUtilities.dpf2(17), AndroidUtilities.dpf2(17), fill);
            fill.setShader(null);

            text.setColor(textColor);
            float baseline = ((h / 2.0f) + top) - ((text.descent() + text.ascent()) / 2.0f);
            int save = canvas.save();
            canvas.clipRect(left, top, right, bottom);
            canvas.drawText(str, left + padX, baseline, text);
            canvas.restoreToCount(save);

            return bottom;
        }

        private LinearGradient createLinearGrad(RectF r, int c1, int c2, int angle) {
            double rad = Math.toRadians(angle);
            float cx = r.centerX(), cy = r.centerY();
            float maxR = Math.max(r.width(), r.height()) / 2.0f;
            float cos = (float) Math.cos(rad) * maxR;
            float sin = (float) Math.sin(rad) * maxR;
            return new LinearGradient(cx - cos, cy - sin, cx + cos, cy + sin, c1, c2, Shader.TileMode.CLAMP);
        }
    }

    /* =========================================================================
     * 16. HAPTIC FEEDBACK HELPERS (1-to-1 with cpb.Haptic)
     * ========================================================================= */

    private static void hapticTap(View v) {
        if (!MiogramCustomUiPrefs.isHapticEnabled() || v == null) return;
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignore) {}
    }

    private static void hapticToggle(View v) {
        if (!MiogramCustomUiPrefs.isHapticEnabled() || v == null) return;
        try {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        } catch (Throwable ignore) {}
    }
}
