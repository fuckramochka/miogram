package app.miogram.bridge.customui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextPaint;

import org.json.JSONObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.miogram.bridge.MiogramLocale;

/**
 * Miogram Custom UI Studio.
 * 1-to-1 matching Custom Profile architecture, visual design, and high-fidelity tactility.
 * Features acoustic zipper feedback, waveform haptic pulses, fluid touch physics,
 * zero-lag live color updates, and pixel-perfect DST_IN shaders.
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
        // Action Bar
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

        // Fragment View (Navigation Hub for all EditSheets)
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

        // Header
        HeaderCell headerModules = new HeaderCell(context);
        headerModules.setText(MiogramLocale.get("Розділи налаштувань Custom Profile", "Разделы настроек Custom Profile", "Custom Profile Settings"));
        headerModules.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        content.addView(headerModules);

        // 1. Bubbles (Extra Features)
        TextCell rowBubbles = new TextCell(context);
        rowBubbles.setTextAndValue(MiogramLocale.get("Додаткові функції (Пухирці)", "Дополнительные функции (Пузырьки)", "Extra features (Bubbles)"), MiogramLocale.get("Повідомлення", "Сообщения", "Messages"), true);
        rowBubbles.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBubbles.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            ExtraFeaturesSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowBubbles);

        // 2. Name
        TextCell rowNames = new TextCell(context);
        rowNames.setTextAndValue(MiogramLocale.get("Оформлення імені", "Оформление имени", "Name appearance"), MiogramLocale.get("Колір, тінь, ефекти", "Цвет, тень, эффекты", "Color, shadow, effects"), true);
        rowNames.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowNames.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditNameSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowNames);

        // 3. Avatar
        TextCell rowAvatars = new TextCell(context);
        rowAvatars.setTextAndValue(MiogramLocale.get("Оформлення аватара", "Оформление аватара", "Avatar appearance"), MiogramLocale.get("Форма, скруглення, вигляд", "Форма, скругление, вид", "Shape, rounding, look"), true);
        rowAvatars.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowAvatars.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditAvatarSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowAvatars);

        // 4. Frame
        TextCell rowFrame = new TextCell(context);
        rowFrame.setTextAndValue(MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"), MiogramLocale.get("Неонове кільце, товщина, пульс", "Неоновое кольцо, толщина, пульс", "Neon ring, width, pulse"), true);
        rowFrame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowFrame.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditFrameSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowFrame);

        // 5. Text colors (Photo 3)
        TextCell rowText = new TextCell(context);
        rowText.setTextAndValue(MiogramLocale.get("Текст профілю (Палітра)", "Текст профиля (Палитра)", "Profile text (Palette)"), MiogramLocale.get("Кольори елементів", "Цвета элементов", "Element colors"), true);
        rowText.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowText.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditTextColorsSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowText);

        // 6. Banner
        TextCell rowBanner = new TextCell(context);
        rowBanner.setTextAndValue(MiogramLocale.get("Шапка і банер", "Шапка и баннер", "Header and banner"), MiogramLocale.get("Фон, злиття, прозорість", "Фон, слияние, прозрачность", "Background, blend, alpha"), true);
        rowBanner.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBanner.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditBannerSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowBanner);

        // 7. Background
        TextCell rowBg = new TextCell(context);
        rowBg.setTextAndValue(MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"), MiogramLocale.get("Колір, медіа, сумісність", "Цвет, медиа, совместимость", "Color, media, compat"), true);
        rowBg.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBg.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditBackgroundSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowBg);

        // 8. Blocks
        TextCell rowBlocks = new TextCell(context);
        rowBlocks.setTextAndValue(MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), MiogramLocale.get("Колір, щільність, скруглення, блюр", "Цвет, плотность, скругление, блюр", "Color, density, rounding, blur"), true);
        rowBlocks.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowBlocks.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditBlocksSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowBlocks);

        // 9. Visibility
        TextCell rowVis = new TextCell(context);
        rowVis.setTextAndValue(MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"), MiogramLocale.get("Приховування блоків", "Скрытие блоков", "Hiding blocks"), true);
        rowVis.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowVis.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditVisibilitySheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowVis);

        // 10. Thought
        TextCell rowThought = new TextCell(context);
        rowThought.setTextAndValue(MiogramLocale.get("Думка у аватара", "Мысль у аватара", "Thought by avatar"), MiogramLocale.get("Облачко думок, текст, тінь", "Облачко мыслей, текст, тень", "Thought bubble, text, shadow"), true);
        rowThought.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowThought.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditThoughtSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowThought);

        // 11. UI & Badges
        TextCell rowUi = new TextCell(context);
        rowUi.setTextAndValue(MiogramLocale.get("Інтерфейс клієнта", "Интерфейс клиента", "Client interface"), MiogramLocale.get("Бейджі, скляний блюр, тактильність", "Бейджи, стеклянный блюр, тактильность", "Badges, glass blur, haptic"), false);
        rowUi.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rowUi.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            EditInterfaceSheet.show(getParentActivity() != null ? getParentActivity() : context);
        });
        content.addView(rowUi);

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context);
        infoCell.setText(MiogramLocale.get(
            "Усі розділи відкриваються через оригінальні діалогові вікна Custom Profile (EditSheet) та записують параметри напряму у сховище cpb_native_settings.",
            "Все разделы открываются через оригинальные диалоговые окна Custom Profile (EditSheet) и записывают параметры напрямую в хранилище cpb_native_settings.",
            "All sections open via original Custom Profile dialogs (EditSheet) and save settings directly to cpb_native_settings storage."
        ));
        content.addView(infoCell);

        return fragmentView;
    }

    /* =========================================================================
     * 1. NATIVE EDITSEGMENTS (cpb.EditSegments.java with fluid drag & haptics)
     * ========================================================================= */

    public static class EditSegments extends HorizontalScrollView {
        private final Strip strip;
        private boolean revealed;

        public EditSegments(Context context, int initialIndex, String[] options, final IntSink sink) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(OVER_SCROLL_NEVER);
            setClipToPadding(false);
            setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(8), AndroidUtilities.dp(21), AndroidUtilities.dp(8));
            strip = new Strip(context, initialIndex, options, sink);
            addView(strip, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(34)));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            strip.setAvailable((MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()) - getPaddingRight());
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (!revealed) {
                revealed = true;
                strip.revealSelected(this);
            }
        }

        public static class Strip extends View {
            private final String[] options;
            private final IntSink sink;
            private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF shape = new RectF();
            private final float[] edges;
            private int available;
            private int selected;
            private float position;
            private ValueAnimator moveAnim;
            private float downX, downY;
            private boolean pressing;
            private boolean dragging;
            private final int touchSlop;
            private final int accent;
            private final int textColor;
            private final int selectedTextColor;

            public Strip(Context context, int initialIndex, String[] options, IntSink sink) {
                super(context);
                this.options = options;
                this.sink = sink;
                this.edges = new float[options.length + 1];
                this.selected = Math.max(0, Math.min(initialIndex, options.length - 1));
                this.position = this.selected;
                this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

                accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
                textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
                selectedTextColor = Theme.getColor(Theme.key_featuredStickers_buttonText);

                trackPaint.setColor((accent & 0x00FFFFFF) | 0x1C000000);
                pillPaint.setColor(accent);

                labelPaint.setTextSize(AndroidUtilities.dp(14));
                labelPaint.setTextAlign(Paint.Align.CENTER);
            }

            public void setAvailable(int available) {
                this.available = available;
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                float totalNatural = 0;
                for (int i = 0; i < options.length; i++) {
                    totalNatural += naturalWidth(i);
                }
                boolean expand = (totalNatural <= available && available > 0);
                float total = expand ? available : totalNatural;
                float currentX = 0;
                for (int i = 0; i < options.length; i++) {
                    edges[i] = currentX;
                    currentX += expand ? (total / options.length) : naturalWidth(i);
                }
                edges[options.length] = currentX;
                setMeasuredDimension(Math.round(currentX), height);
            }

            private float naturalWidth(int index) {
                return Math.max(labelPaint.measureText(options[index]) + AndroidUtilities.dp(32), AndroidUtilities.dp(54));
            }

            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                float radius = h / 2.0f;
                shape.set(0, 0, w, h);
                canvas.drawRoundRect(shape, radius, radius, trackPaint);

                int floor = (int) Math.floor(position);
                float frac = position - floor;
                int next = Math.min(floor + 1, options.length - 1);
                float left = edges[floor] + ((edges[next] - edges[floor]) * frac);
                float right = edges[floor + 1] + ((edges[Math.min(next + 1, options.length)] - edges[floor + 1]) * frac);

                shape.set(left, 0, right, h);
                canvas.drawRoundRect(shape, radius, radius, pillPaint);

                Paint.FontMetrics fm = labelPaint.getFontMetrics();
                float textY = (h - (fm.ascent + fm.descent)) / 2.0f;

                for (int i = 0; i < options.length; i++) {
                    float centerX = (edges[i] + edges[i + 1]) / 2.0f;
                    float nearness = Math.max(0.0f, 1.0f - Math.abs(position - i));
                    labelPaint.setColor(mixColor(textColor, selectedTextColor, nearness));
                    canvas.drawText(options[i], centerX, textY, labelPaint);
                }
            }

            private int mixColor(int c1, int c2, float frac) {
                int a = (int) (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * frac);
                int r = (int) (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * frac);
                int g = (int) (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * frac);
                int b = (int) (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * frac);
                return Color.argb(a, r, g, b);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        pressing = true;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (pressing) {
                            if (!dragging && Math.abs(event.getX() - downX) > touchSlop) {
                                dragging = true;
                                if (getParent() != null) {
                                    getParent().requestDisallowInterceptTouchEvent(true);
                                }
                            }
                            if (dragging) {
                                int idx = indexAt(event.getX());
                                if (idx >= 0 && idx != selected) {
                                    selected = idx;
                                    MiogramHaptic.select(this);
                                    if (sink != null) sink.accept(idx);
                                    animateTo(idx);
                                }
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (pressing) {
                            pressing = false;
                            int clicked = indexAt(event.getX());
                            if (clicked >= 0) {
                                select(clicked, true);
                            }
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        pressing = false;
                        dragging = false;
                        break;
                }
                return super.onTouchEvent(event);
            }

            private int indexAt(float x) {
                for (int i = 0; i < options.length; i++) {
                    if (x >= edges[i] && x <= edges[i + 1]) return i;
                }
                return -1;
            }

            public void select(int index, boolean animate) {
                if (index < 0 || index >= options.length) return;
                MiogramHaptic.select(this);
                selected = index;
                if (sink != null) {
                    sink.accept(index);
                }
                if (animate) {
                    animateTo(index);
                } else {
                    if (moveAnim != null) moveAnim.cancel();
                    position = index;
                    invalidate();
                }
                if (getParent() instanceof HorizontalScrollView) {
                    revealSelected((HorizontalScrollView) getParent());
                }
            }

            private void animateTo(int index) {
                if (moveAnim != null) moveAnim.cancel();
                moveAnim = ValueAnimator.ofFloat(position, index);
                moveAnim.setDuration(200);
                moveAnim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                moveAnim.addUpdateListener(anim -> {
                    position = (float) anim.getAnimatedValue();
                    invalidate();
                });
                moveAnim.start();
            }

            public void revealSelected(HorizontalScrollView scroll) {
                if (selected < 0 || selected >= options.length) return;
                int targetX = Math.round((edges[selected] + edges[selected + 1]) / 2.0f) - (scroll.getWidth() / 2);
                scroll.smoothScrollTo(Math.max(0, targetX), 0);
            }
        }
    }

    /* =========================================================================
     * 2. NATIVE EDITSHEET (1-to-1 with cpb.EditSheet / BottomSheet & Tactile Physics)
     * ========================================================================= */

    public static class EditSheet {
        private final Context context;
        private final BottomSheet.Builder builder;
        private final LinearLayout content;
        private final HalfScreen halfScreen;
        private final Map<String, List<View>> groups = new HashMap<>();
        private String currentGroup = null;
        private BottomSheet bottomSheet;

        private final Runnable restoreRunnable = () -> {
            if (content != null) {
                content.animate().alpha(1.0f).setDuration(110).start();
            }
        };

        public EditSheet(Context context, String title) {
            this.context = context;
            builder = new BottomSheet.Builder(context);
            builder.setTitle(title, true);
            builder.setApplyBottomPadding(false);

            halfScreen = new HalfScreen(context);
            content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            halfScreen.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Acoustic zipper feedback: plays zipIn on attach, zipOut on detach
            MiogramHaptic.zipper(halfScreen);
            builder.setCustomView(halfScreen);
        }

        public void fade() {
            if (content == null) return;
            content.removeCallbacks(restoreRunnable);
            content.animate().cancel();
            content.setAlpha(0.65f);
            content.postDelayed(restoreRunnable, 200);
        }

        public EditSheet custom(View view) {
            if (view != null) add(view);
            return this;
        }

        public EditSheet header(String text) {
            HeaderCell cell = new HeaderCell(context);
            cell.setText(text);
            add(cell);
            return this;
        }

        public EditSheet note(String text) {
            TextInfoPrivacyCell cell = new TextInfoPrivacyCell(context);
            cell.setText(text);
            add(cell);
            return this;
        }

        public EditSheet check(String title, boolean checked, boolean divider, final IntSink sink) {
            TextCheckCell cell = new TextCheckCell(context);
            cell.setTextAndCheck(title, checked, divider);
            cell.setOnClickListener(v -> {
                boolean target = !cell.isChecked();
                cell.setChecked(target);
                MiogramHaptic.toggle(v, target);
                fade();
                if (sink != null) sink.accept(target ? 1 : 0);
            });
            add(cell);
            return this;
        }

        public EditSheet row(String title, String value, boolean divider, final Runnable onClick) {
            TextCell cell = new TextCell(context);
            cell.setTextAndValue(title, value, divider);
            if (onClick != null) {
                cell.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    onClick.run();
                });
            }
            add(cell);
            return this;
        }

        public EditSheet color(String title, int color, boolean divider, final ColorSink sink) {
            final EditColorRow row = new EditColorRow(context, title, color, divider, null);
            row.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                EditColorPicker.show(context, title, row.getColor(), picked -> {
                    row.setColor(picked);
                    fade();
                    if (sink != null) sink.accept(picked);
                });
            });
            add(row);
            return this;
        }

        public EditSheet slider(final String title, int cur, final int min, final int max, final String unit, final IntSink sink) {
            int clamped = Math.max(min, Math.min(max, cur));
            final HeaderCell header = new HeaderCell(context);
            header.setText(sliderLabel(title, clamped, unit));
            add(header);

            final SeekBarView bar = new SeekBarView(context);
            bar.setReportChanges(true);
            float progress = (max <= min) ? 0.0f : ((float) (clamped - min) / (max - min));
            bar.setProgress(progress);

            final int[] lastVal = new int[]{clamped};
            bar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float p) {
                    int val = min + Math.round(p * (max - min));
                    if (val != lastVal[0]) {
                        if (MiogramHaptic.edgeReached(val, lastVal[0], min, max)) {
                            MiogramHaptic.edge(bar);
                        } else {
                            MiogramHaptic.tick(bar);
                        }
                        lastVal[0] = val;
                    }
                    header.setText(sliderLabel(title, val, unit));
                    fade();
                    if (sink != null) sink.accept(val);
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {
                    if (pressed) {
                        MiogramHaptic.grab(bar);
                    } else {
                        MiogramHaptic.release(bar);
                    }
                }
            });
            add(bar);
            return this;
        }

        public EditSheet chooser(String title, int curId, String[] items, final IntSink sink) {
            header(title);
            EditSegments segments = new EditSegments(context, curId, items, val -> {
                fade();
                if (sink != null) sink.accept(val);
            });
            add(segments);
            return this;
        }

        public EditSheet chooser(String title, int curId, String[] items, String[] itemsRu, final IntSink sink) {
            String[] localized = (LocaleController.isRTL || !MiogramLocale.isUkrainian()) ? itemsRu : items;
            return chooser(title, curId, localized, sink);
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
            if (view != null) {
                content.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                if (currentGroup != null) {
                    groups.get(currentGroup).add(view);
                }
            }
            return this;
        }

        private String sliderLabel(String title, int val, String unit) {
            if (unit == null || unit.isEmpty()) {
                return title + ": " + val;
            }
            return title + ": " + val + " " + unit;
        }

        public void show() {
            bottomSheet = builder.create();
            bottomSheet.setDimBehindAlpha(60);
            bottomSheet.setFixNavigationBar(true);
            bottomSheet.show();
        }

        public void dismiss() {
            if (bottomSheet != null) {
                bottomSheet.dismiss();
                bottomSheet = null;
            }
        }

        public static class HalfScreen extends ScrollView {
            private float downY;

            public HalfScreen(Context context) {
                super(context);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int halfHeight = getResources().getDisplayMetrics().heightPixels / 2;
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(halfHeight, MeasureSpec.AT_MOST));
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                keepGesture(ev);
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                keepGesture(ev);
                return super.onTouchEvent(ev);
            }

            private void keepGesture(MotionEvent ev) {
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    downY = ev.getY();
                    boolean canScroll = canScrollVertically(1) || canScrollVertically(-1);
                    disallow(canScroll);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (ev.getX() < 0 || ev.getX() > getWidth() || ev.getY() < 0) {
                        disallow(true);
                    } else {
                        disallow(ev.getY() - downY < 0 ? canScrollVertically(1) : canScrollVertically(-1));
                    }
                }
            }

            private void disallow(boolean disallow) {
                ViewParent p = getParent();
                if (p != null) {
                    p.requestDisallowInterceptTouchEvent(disallow);
                }
            }
        }
    }

    /* =========================================================================
     * 3. NATIVE EDITCOLORROW WITH DST_IN BAND (1-to-1 with cpb.EditColorRow - PHOTO 3)
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
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint fade = new Paint(Paint.ANTI_ALIAS_FLAG);
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
                int save = canvas.saveLayer(0, 0, w, h, null);
                canvas.drawRect(0, 0, w, h, paint);
                canvas.drawRect(0, 0, w, h, fade);
                canvas.restoreToCount(save);
            }
        }
    }

    /* =========================================================================
     * 4. NATIVE EDITCOLORPICKER WITH LIVE ALPHA SLIDER (1-to-1 with cpb.EditColorPicker)
     * ========================================================================= */

    public static class EditColorPicker {
        public static void show(Context context, String title, int initialColor, final ColorSink sink) {
            if (context == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(context);
            builder.setTitle(title, true);
            builder.setApplyBottomPadding(false);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);

            // Acoustic zipper feedback for color picker
            MiogramHaptic.zipper(root);

            final int[] curRgb = new int[]{initialColor & 0x00FFFFFF};
            final int[] curAlpha = new int[]{Color.alpha(initialColor)};

            ColorPicker picker = new ColorPicker(context, false, (color, done) -> {
                curRgb[0] = color & 0x00FFFFFF;
                int res = (curAlpha[0] << 24) | curRgb[0];
                MiogramHaptic.tick(null);
                if (sink != null) sink.accept(res);
            });
            picker.setColor(initialColor | 0xFF000000);
            root.addView(picker, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 340));

            // Opacity Row (HeaderCell + SeekBarView)
            final HeaderCell alphaHeader = new HeaderCell(context);
            int percent = Math.round((curAlpha[0] * 100.0f) / 255.0f);
            alphaHeader.setText(MiogramLocale.get("Прозорість — ", "Прозрачность — ", "Opacity — ") + percent + "%");
            root.addView(alphaHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            SeekBarView alphaSeekBar = new SeekBarView(context);
            alphaSeekBar.setReportChanges(true);
            alphaSeekBar.setProgress(percent / 100.0f);
            final int[] lastAlphaVal = new int[]{percent};
            alphaSeekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    int p = Math.round(progress * 100.0f);
                    if (p != lastAlphaVal[0]) {
                        if (MiogramHaptic.edgeReached(p, lastAlphaVal[0], 0, 100)) {
                            MiogramHaptic.edge(alphaSeekBar);
                        } else {
                            MiogramHaptic.tick(alphaSeekBar);
                        }
                        lastAlphaVal[0] = p;
                    }
                    curAlpha[0] = Math.round((p * 255.0f) / 100.0f);
                    alphaHeader.setText(MiogramLocale.get("Прозорість — ", "Прозрачность — ", "Opacity — ") + p + "%");
                    int res = (curAlpha[0] << 24) | curRgb[0];
                    if (sink != null) sink.accept(res);
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {
                    if (pressed) {
                        MiogramHaptic.grab(alphaSeekBar);
                    } else {
                        MiogramHaptic.release(alphaSeekBar);
                    }
                }
            });
            root.addView(alphaSeekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 12, 0, 12, 12));

            builder.setCustomView(root);
            BottomSheet sheet = builder.create();
            sheet.setDimBehindAlpha(60);
            sheet.setFixNavigationBar(true);
            sheet.show();
        }
    }

    /* =========================================================================
     * 5. PROFILE EDIT MENU (1-to-1 with cpb.ProfileEditMenu - PHOTO 1 & PHOTO 2)
     * ========================================================================= */

    public static class ProfileEditMenu {
        public static void showForHeader(final BaseFragment fragment, final View anchorView) {
            if (fragment == null || anchorView == null) return;
            MiogramHaptic.select(anchorView);
            ItemOptions options = ItemOptions.makeOptions(fragment, anchorView);
            options.addText(MiogramLocale.get("Аватар профілю", "Аватар профиля", "Profile avatar"), 13);
            options.add(R.drawable.msg_edit, MiogramLocale.get("Оформлення аватара", "Оформление аватара", "Avatar appearance"), () -> {
                MiogramHaptic.tap(anchorView);
                EditAvatarSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_palette, MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"), () -> {
                MiogramHaptic.tap(anchorView);
                EditFrameSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_message, MiogramLocale.get("Думка у аватара", "Мысль у аватара", "Thought by the avatar"), () -> {
                MiogramHaptic.tap(anchorView);
                EditThoughtSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.show();
        }

        public static void showForRow(final BaseFragment fragment, final View rowView) {
            if (fragment == null || rowView == null) return;
            MiogramHaptic.select(rowView);
            ItemOptions options = ItemOptions.makeOptions(fragment, rowView);
            options.addText(MiogramLocale.get("Налаштування оформлення", "Настройки оформления", "Appearance settings"), 13);
            options.add(R.drawable.msg_palette, MiogramLocale.get("Колір тексту профілю", "Цвет текста профиля", "Profile text color"), () -> {
                MiogramHaptic.tap(rowView);
                EditTextColorsSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_views, MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"), () -> {
                MiogramHaptic.tap(rowView);
                EditVisibilitySheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_addcontact, MiogramLocale.get("Додати рядок", "Добавить строку", "Add row"), () -> {
                MiogramHaptic.tap(rowView);
                Toast.makeText(fragment.getContext(), MiogramLocale.get("Користувацькі блоки налаштовуються в меню блоків", "Пользовательские блоки настраиваются в меню блоков", "Custom blocks configured in blocks menu"), Toast.LENGTH_SHORT).show();
            });
            options.add(R.drawable.msg_colors, MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), () -> {
                MiogramHaptic.tap(rowView);
                EditBlocksSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_theme, MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"), () -> {
                MiogramHaptic.tap(rowView);
                EditBackgroundSheet.show(fragment.getParentActivity() != null ? fragment.getParentActivity() : fragment.getContext());
            });
            options.add(R.drawable.msg_clear, MiogramLocale.get("Приховати блок", "Скрыть блок", "Hide block"), true, () -> {
                MiogramHaptic.warn(rowView);
                rowView.setVisibility(View.GONE);
                Toast.makeText(fragment.getContext(), MiogramLocale.get("Блок приховано — повернути можна у «Видимості рядків»", "Блок скрыт — вернуть можно в «Видимости строк»", "Block hidden — bring it back in “Row visibility”"), Toast.LENGTH_LONG).show();
            });
            options.show();
        }
    }

    /* =========================================================================
     * 6. EXTRA FEATURES SHEET (cpb.ExtraFeatures)
     * ========================================================================= */

    public static class ExtraFeaturesSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Додаткові функції", "Дополнительные функции", "Extra features"));

            final BubblePreview preview = new BubblePreview(context);
            sheet.custom(preview);

            sheet.header(MiogramLocale.get("Пухирці повідомлень", "Пузырьки сообщений", "Message bubbles"));
            sheet.check(MiogramLocale.get("Кастомні пухирці", "Кастомные пузырьки", "Custom bubbles"), MiogramCustomUiPrefs.isBubbleColorEnabled(), true, val -> {
                MiogramCustomUiPrefs.setBubbleColorEnabled(val != 0);
                bubbleVis(sheet);
                preview.invalidate();
            });

            sheet.group("bubble_opts");
            sheet.check(MiogramLocale.get("Градієнтний пухирець", "Градиентный пузырёк", "Gradient bubble"), MiogramCustomUiPrefs.isBubbleGradient(), true, val -> {
                MiogramCustomUiPrefs.setBubbleGradient(val != 0);
                sheet.setGroupVisible("bubble_g2", val != 0);
                sheet.setGroupVisible("bubble_gangle", val != 0);
                preview.invalidate();
            });

            sheet.color(MiogramLocale.get("Основний колір", "Основной цвет", "Primary color"), MiogramCustomUiPrefs.getBubbleColor(), true, color -> {
                MiogramCustomUiPrefs.setBubbleColor(color);
                preview.invalidate();
            });

            sheet.group("bubble_g2");
            sheet.color(MiogramLocale.get("Другий колір градієнта", "Второй цвет градиента", "Secondary gradient color"), MiogramCustomUiPrefs.getBubbleColor2(), true, color -> {
                MiogramCustomUiPrefs.setBubbleColor2(color);
                preview.invalidate();
            });
            sheet.endGroup();

            sheet.group("bubble_gangle");
            sheet.slider(MiogramLocale.get("Кут нахилу", "Угол наклона", "Angle"), MiogramCustomUiPrefs.getBubbleGradAngle(), 0, 360, "°", val -> {
                MiogramCustomUiPrefs.setBubbleGradAngle(val);
                preview.invalidate();
            });
            sheet.endGroup();

            sheet.color(MiogramLocale.get("Колір тексту", "Цвет текста", "Text color"), MiogramCustomUiPrefs.getBubbleTextColor(), true, color -> {
                MiogramCustomUiPrefs.setBubbleTextColor(color);
                preview.invalidate();
            });

            sheet.slider(MiogramLocale.get("Скруглення кутів", "Скругление углов", "Corner radius"), MiogramCustomUiPrefs.getBubbleRadius(), 0, 30, "dp", val -> {
                MiogramCustomUiPrefs.setBubbleRadius(val);
                preview.invalidate();
            });

            sheet.check(MiogramLocale.get("Світіння пухирця", "Свечение пузырька", "Bubble glow"), MiogramCustomUiPrefs.isBubbleGlowEnabled(), true, val -> {
                MiogramCustomUiPrefs.setBubbleGlowEnabled(val != 0);
                sheet.setGroupVisible("bubble_glow_opts", val != 0);
                preview.invalidate();
            });

            sheet.group("bubble_glow_opts");
            sheet.color(MiogramLocale.get("Колір світіння", "Цвет свечения", "Glow color"), MiogramCustomUiPrefs.getBubbleGlowColor(), true, color -> {
                MiogramCustomUiPrefs.setBubbleGlowColor(color);
                preview.invalidate();
            });
            sheet.slider(MiogramLocale.get("Радіус світіння", "Радиус свечения", "Glow radius"), MiogramCustomUiPrefs.getBubbleGlowRadius(), 0, 40, "dp", val -> {
                MiogramCustomUiPrefs.setBubbleGlowRadius(val);
                preview.invalidate();
            });
            sheet.endGroup();

            sheet.endGroup(); // end bubble_opts

            bubbleVis(sheet);
            sheet.show();
        }

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
                boolean grad = MiogramCustomUiPrefs.isBubbleGradient();
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
                int radius = MiogramCustomUiPrefs.isBubbleColorEnabled() ? MiogramCustomUiPrefs.getBubbleRadius() : 17;
                float r = AndroidUtilities.dpf2(Math.max(4, radius));
                canvas.drawRoundRect(rect, r, r, fill);
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

        private static void bubbleVis(EditSheet sheet) {
            boolean enabled = MiogramCustomUiPrefs.isBubbleColorEnabled();
            sheet.setGroupVisible("bubble_opts", enabled);
            if (enabled) {
                boolean grad = MiogramCustomUiPrefs.isBubbleGradient();
                sheet.setGroupVisible("bubble_g2", grad);
                sheet.setGroupVisible("bubble_gangle", grad);
                sheet.setGroupVisible("bubble_glow_opts", MiogramCustomUiPrefs.isBubbleGlowEnabled());
            }
        }
    }

    /* =========================================================================
     * 7. EDITNAMESHEET (1-to-1 with cpb.EditNameSheet.java)
     * ========================================================================= */

    public static class EditNameSheet {
        private static final String[] EFFECTS = {"Немає", "Пульс", "Градієнт", "Шиммер", "Веселка", "Неон", "Вогонь", "Лід"};
        private static final String[] EFFECTS_RU = {"Нет", "Пульс", "Градиент", "Шиммер", "Радуга", "Неон", "Огонь", "Лёд"};
        private static final String[] FONTS = {"Стандарт", "Тонкий", "Із зарубками", "Моно", "Курсив", "Вузький", "Свій"};
        private static final String[] FONTS_RU = {"Стандарт", "Тонкий", "Засечки", "Моно", "Курсив", "Узкий", "Свой"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Ім'я", "Имя", "Name"));

            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.check(MiogramLocale.get("Свій колір імені", "Свой цвет имени", "Custom name color"), MiogramCustomUiPrefs.isNameColorEnabled(), true, val -> {
                MiogramCustomUiPrefs.setNameColorEnabled(val != 0);
                sheet.setGroupVisible("name_color", val != 0);
            });

            sheet.group("name_color");
            sheet.color(MiogramLocale.get("Колір імені", "Цвет имени", "Name color"), MiogramCustomUiPrefs.getNameColor(), false, color -> {
                MiogramCustomUiPrefs.setNameColor(color);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Світіння", "Свечение", "Glow"));
            sheet.check(MiogramLocale.get("Світіння імені", "Свечение имени", "Name glow"), MiogramCustomUiPrefs.isNameGlowEnabled(), true, val -> {
                MiogramCustomUiPrefs.setNameGlowEnabled(val != 0);
                sheet.setGroupVisible("name_glow", val != 0);
            });

            sheet.group("name_glow");
            sheet.slider(MiogramLocale.get("Радіус", "Радиус", "Radius"), MiogramCustomUiPrefs.getNameGlowRadius(), 0, 40, "", val -> {
                MiogramCustomUiPrefs.setNameGlowRadius(val);
            });
            sheet.slider(MiogramLocale.get("Сила", "Сила", "Strength"), MiogramCustomUiPrefs.getNameGlowStrength(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setNameGlowStrength(val);
            });
            sheet.note(MiogramLocale.get(
                "Світіння повторює колір самого імені: свого кольору у нього немає, інакше воно читалося б як другий напис позаду першого.",
                "Свечение повторяет цвет самого имени: своего цвета у него нет, иначе оно читалось бы как вторая надпись позади первой.",
                "The glow follows the name's own color: it has no color of its own, otherwise it would read as a second label behind the first."
            ));
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Тінь", "Тень", "Shadow"));
            sheet.check(MiogramLocale.get("Тінь імені", "Тень имени", "Name shadow"), MiogramCustomUiPrefs.isNameShadowEnabled(), true, val -> {
                MiogramCustomUiPrefs.setNameShadowEnabled(val != 0);
                sheet.setGroupVisible("name_shadow", val != 0);
            });

            sheet.group("name_shadow");
            sheet.color(MiogramLocale.get("Колір тіні", "Цвет тени", "Shadow color"), MiogramCustomUiPrefs.getNameShadowColor(), true, color -> {
                MiogramCustomUiPrefs.setNameShadowColor(color);
            });
            sheet.slider(MiogramLocale.get("Розмиття", "Размытие", "Blur"), MiogramCustomUiPrefs.getNameShadowRadius(), 0, 40, "", val -> {
                MiogramCustomUiPrefs.setNameShadowRadius(val);
            });
            sheet.slider(MiogramLocale.get("Сила", "Сила", "Strength"), MiogramCustomUiPrefs.getNameShadowStrength(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setNameShadowStrength(val);
            });
            sheet.slider(MiogramLocale.get("Зсув убік", "Сдвиг вбок", "Shift sideways"), MiogramCustomUiPrefs.getNameShadowDx(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setNameShadowDx(val);
            });
            sheet.slider(MiogramLocale.get("Зсув вниз", "Сдвиг вниз", "Shift down"), MiogramCustomUiPrefs.getNameShadowDy(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setNameShadowDy(val);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Ефект", "Эффект", "Effect"));
            sheet.chooser(MiogramLocale.get("Рух імені", "Движение имени", "Name motion"), MiogramCustomUiPrefs.getNameFx(), EFFECTS, EFFECTS_RU, val -> {
                MiogramCustomUiPrefs.setNameFx(val);
                applyEffect(sheet, val);
            });

            sheet.group("fx_speed");
            sheet.slider(MiogramLocale.get("Швидкість", "Скорость", "Speed"), MiogramCustomUiPrefs.getNameFxSpeed(), 10, 300, "%", val -> {
                MiogramCustomUiPrefs.setNameFxSpeed(val);
            });
            sheet.endGroup();

            sheet.group("fx_gradient");
            sheet.color(MiogramLocale.get("Перший колір градієнта", "Первый цвет градиента", "First gradient color"), MiogramCustomUiPrefs.getNameGradC1(), true, color -> {
                MiogramCustomUiPrefs.setNameGradC1(color);
            });
            sheet.color(MiogramLocale.get("Другий колір градієнта", "Второй цвет градиента", "Second gradient color"), MiogramCustomUiPrefs.getNameGradC2(), false, color -> {
                MiogramCustomUiPrefs.setNameGradC2(color);
            });
            sheet.endGroup();

            sheet.group("fx_angle");
            sheet.slider(MiogramLocale.get("Напрямок", "Направление", "Direction"), MiogramCustomUiPrefs.getNameGradAngle(), 0, 360, "°", val -> {
                MiogramCustomUiPrefs.setNameGradAngle(val);
            });
            sheet.note(MiogramLocale.get(
                "Напрямок веде не лише градієнт: за ним же йдуть шиммер, веселка, вогонь та лід.",
                "Направление ведёт не только градиент: по нему же идут шиммер, радуга, огонь и лёд.",
                "Direction guides not only the gradient: shimmer, rainbow, fire and ice follow it too."
            ));
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Накреслення", "Начертание", "Typeface"));
            sheet.slider(MiogramLocale.get("Розмір", "Размер", "Size"), MiogramCustomUiPrefs.getNameSize(), 50, 200, "%", val -> {
                MiogramCustomUiPrefs.setNameSize(val);
            });
            sheet.chooser(MiogramLocale.get("Шрифт", "Шрифт", "Font"), MiogramCustomUiPrefs.getNameFont(), FONTS, FONTS_RU, val -> {
                MiogramCustomUiPrefs.setNameFont(val);
            });

            sheet.setGroupVisible("name_color", MiogramCustomUiPrefs.isNameColorEnabled());
            sheet.setGroupVisible("name_glow", MiogramCustomUiPrefs.isNameGlowEnabled());
            sheet.setGroupVisible("name_shadow", MiogramCustomUiPrefs.isNameShadowEnabled());
            applyEffect(sheet, MiogramCustomUiPrefs.getNameFx());

            sheet.show();
        }

        private static void applyEffect(EditSheet sheet, int fx) {
            sheet.setGroupVisible("fx_speed", fx != 0);
            sheet.setGroupVisible("fx_gradient", fx == 2);
            sheet.setGroupVisible("fx_angle", fx >= 2 && fx != 5);
        }
    }

    /* =========================================================================
     * 8. EDITAVATARSHEET (1-to-1 with cpb.EditAvatarSheet.java)
     * ========================================================================= */

    public static class EditAvatarSheet {
        private static final String[] SHAPES = {"Коло", "Скруглений", "Квадрат", "Шестикутник", "П'ятикутник", "Зірка", "Серце", "Квітка", "Своя"};
        private static final String[] SHAPES_RU = {"Круг", "Скруглённый", "Квадрат", "Шестиугольник", "Пятиугольник", "Звезда", "Сердце", "Цветок", "Своя"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Аватар", "Аватар", "Avatar"));

            sheet.header(MiogramLocale.get("Форма", "Форма", "Shape"));
            sheet.chooser(MiogramLocale.get("Обрис", "Очертание", "Outline"), MiogramCustomUiPrefs.getAvatarShape(), SHAPES, SHAPES_RU, val -> {
                MiogramCustomUiPrefs.setAvatarShape(val);
                applyShape(sheet, val);
            });

            sheet.group("avatar_radius");
            sheet.slider(MiogramLocale.get("Скруглення кутів", "Скругление углов", "Corner rounding"), MiogramCustomUiPrefs.getAvatarRadius(), 0, 64, "", val -> {
                MiogramCustomUiPrefs.setAvatarRadius(val);
            });
            sheet.endGroup();

            sheet.group("avatar_round");
            sheet.slider(MiogramLocale.get("Округлість", "Округлость", "Roundness"), MiogramCustomUiPrefs.getAvatarRound(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarRound(val);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Appearance"));
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getAvatarAlpha(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarAlpha(val);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getAvatarDim(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarDim(val);
            });
            sheet.slider(MiogramLocale.get("Розтушовування країв", "Растушёвка краёв", "Edge feathering"), MiogramCustomUiPrefs.getAvatarFade(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarFade(val);
            });
            sheet.slider(MiogramLocale.get("Радіус розтушовування", "Радиус растушёвки", "Feather radius"), MiogramCustomUiPrefs.getAvatarFadeRadius(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setAvatarFadeRadius(val);
            });
            sheet.note(MiogramLocale.get(
                "Розтушовування розмиває край аватара, тому помітна лише поверх шапки з картинкою або кольором.",
                "Растушёвка размывает край аватара, поэтому заметна только поверх шапки с картинкой или цветом.",
                "Feathering blurs the avatar's edge, so it shows only over a header with an image or color."
            ));

            applyShape(sheet, MiogramCustomUiPrefs.getAvatarShape());
            sheet.show();
        }

        private static void applyShape(EditSheet sheet, int shape) {
            sheet.setGroupVisible("avatar_radius", shape == 1);
            sheet.setGroupVisible("avatar_round", shape != 0);
        }
    }

    /* =========================================================================
     * 9. EDITFRAMESHEET (1-to-1 with cpb.EditFrameSheet.java)
     * ========================================================================= */

    public static class EditFrameSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Рамка аватара", "Рамка аватара", "Avatar frame"));

            sheet.header(MiogramLocale.get("Неонова рамка", "Неоновая рамка", "Neon frame"));
            sheet.check(MiogramLocale.get("Увімкнути рамку", "Включить рамку", "Enable frame"), MiogramCustomUiPrefs.isAvatarRingEnabled(), true, val -> {
                MiogramCustomUiPrefs.setAvatarRingEnabled(val != 0);
                sheet.setGroupVisible("frame_opts", val != 0);
            });

            sheet.group("frame_opts");
            sheet.header(MiogramLocale.get("Розмір", "Размер", "Size"));
            sheet.slider(MiogramLocale.get("Товщина", "Толщина", "Thickness"), MiogramCustomUiPrefs.getAvatarRingWidth(), 2, 60, "dp", val -> {
                MiogramCustomUiPrefs.setAvatarRingWidth(val);
            });

            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Appearance"));
            sheet.color(MiogramLocale.get("Відтінок", "Оттенок", "Tint"), MiogramCustomUiPrefs.getAvatarRingColor(), true, color -> {
                MiogramCustomUiPrefs.setAvatarRingColor(color);
            });

            sheet.check(MiogramLocale.get("Пульсація", "Пульсация", "Pulse animation"), MiogramCustomUiPrefs.isAvatarRingPulse(), false, val -> {
                MiogramCustomUiPrefs.setAvatarRingPulse(val != 0);
            });

            sheet.note(MiogramLocale.get(
                "Рамка лягає на контур форми аватара, тому одна й та сама рамка сідає і на коло, і на зірку, і на намальовану від руки.",
                "Рамка ложится на контур формы аватара, поэтому одна и та же рамка садится и на круг, и на звезду, и на нарисованную от руки.",
                "The frame lies on the outline of the avatar's shape, so one and the same frame fits a circle, a star, and a hand-drawn shape alike."
            ));

            sheet.header(MiogramLocale.get("Зняти", "Снять", "Remove"));
            sheet.row(MiogramLocale.get("Прибрати рамку", "Убрать рамку", "Remove frame"), "", false, () -> {
                MiogramHaptic.warn(null);
                MiogramCustomUiPrefs.setAvatarRingEnabled(false);
                sheet.dismiss();
                Toast.makeText(context, MiogramLocale.get("Рамку знято", "Рамка снята", "Frame removed"), Toast.LENGTH_SHORT).show();
            });
            sheet.endGroup();

            sheet.setGroupVisible("frame_opts", MiogramCustomUiPrefs.isAvatarRingEnabled());
            sheet.show();
        }
    }

    /* =========================================================================
     * 10. EDITTEXTCOLORSSHEET (PHOTO 3: Segmented Pill + EditColorRow with Band)
     * ========================================================================= */

    public static class EditTextColorsSheet {

        private static class ColorItem {
            final String key;
            final String titleUa;
            final String titleRu;
            final String titleEn;
            final int segment; // 1=Name, 2=Bio, 3=Phone, 4=Username, 5=Palette
            final int defaultColor;

            ColorItem(String key, String titleUa, String titleRu, String titleEn, int segment, int defaultColor) {
                this.key = key;
                this.titleUa = titleUa;
                this.titleRu = titleRu;
                this.titleEn = titleEn;
                this.segment = segment;
                this.defaultColor = defaultColor;
            }
        }

        private static final ColorItem[] ALL_ITEMS = new ColorItem[]{
            // Segment 1: Name
            new ColorItem(Theme.key_profile_title, "Ім'я в профілі", "Имя в профиле", "Profile name", 1, 0xFFFFFFFF),
            new ColorItem(MiogramCustomUiPrefs.KEY_NAME_GRAD_C2, "Другий колір градієнта", "Второй цвет градиента", "Second gradient color", 1, 0xFF40C4FF),
            new ColorItem(MiogramCustomUiPrefs.KEY_NAME_GLOW_COLOR, "Світіння імені", "Свечение имени", "Name glow", 1, 0xFF29B6F6),
            new ColorItem(MiogramCustomUiPrefs.KEY_NAME_SHADOW_COLOR, "Тінь імені", "Тень имени", "Name shadow", 1, 0x88000000),

            // Segment 2: Bio
            new ColorItem("profile_bio_color", "Опис (Біо)", "Описание (Био)", "Bio", 2, 0xFFE0E0E0),
            new ColorItem(Theme.key_chat_messageLinkIn, "Посилання в біо", "Ссылки в био", "Links in bio", 2, 0xFF29B6F6),

            // Segment 3: Phone
            new ColorItem("profile_phone_color", "Номер телефону", "Номер телефона", "Phone number", 3, 0xFFFFFFFF),
            new ColorItem(Theme.key_windowBackgroundWhiteGrayText2, "Підпис під номером", "Подпись под номером", "Phone label", 3, 0xFF9E9E9E),

            // Segment 4: Username
            new ColorItem("profile_username_color", "Ім'я користувача", "Имя пользователя", "Username", 4, 0xFFFFFFFF),
            new ColorItem("profile_username_label_color", "Підпис під юзернеймом", "Подпись под юзернеймом", "Username label", 4, 0xFF9E9E9E),

            // Segment 5: Palette
            new ColorItem(Theme.key_windowBackgroundWhiteBlackText, "Основний текст", "Основной текст", "Main text", 5, 0xFFFFFFFF),
            new ColorItem(Theme.key_windowBackgroundWhiteGrayText, "Підписи", "Подписи", "Labels", 5, 0xFF9E9E9E),
            new ColorItem(Theme.key_windowBackgroundWhiteGrayText3, "Вторинний текст", "Второстепенный текст", "Secondary text", 5, 0xFF757575),
            new ColorItem(Theme.key_windowBackgroundWhiteGrayText4, "Пояснення під блоками", "Пояснения под блоками", "Notes under blocks", 5, 0xFF616161),
            new ColorItem(Theme.key_windowBackgroundWhiteValueText, "Значення", "Значения", "Values", 5, 0xFF40C4FF),
            new ColorItem(Theme.key_windowBackgroundWhiteBlueText, "Виділений текст", "Выделенный текст", "Highlighted text", 5, 0xFF29B6F6),
            new ColorItem(Theme.key_windowBackgroundWhiteBlueText4, "Кнопки дій", "Кнопки действий", "Action buttons", 5, 0xFF29B6F6),
            new ColorItem(Theme.key_windowBackgroundWhite, "Фон блоків", "Фон блоков", "Blocks background", 5, 0xFF1C242F),
            new ColorItem(Theme.key_windowBackgroundGray, "Фон сторінки", "Фон страницы", "Page background", 5, 0xFF0E1621),
            new ColorItem(Theme.key_divider, "Роздільники", "Разделители", "Dividers", 5, 0xFF242F3D),
            new ColorItem(Theme.key_actionBarDefault, "Панель зверху", "Панель сверху", "Top bar", 5, 0xFF1C242F),
            new ColorItem(Theme.key_profile_status, "Статус у профілі", "Статус в профиле", "Profile status", 5, 0xFF82B1FF)
        };

        private static final String[] SEGMENTS_UA = {"Усі", "Ім'я", "Біо", "Телефон", "Юзернейм", "Палітра"};
        private static final String[] SEGMENTS_RU = {"Все", "Имя", "Био", "Телефон", "Юзернейм", "Палитра"};

        public static void show(final Context context) {
            if (context == null) return;
            final BottomSheet.Builder builder = new BottomSheet.Builder(context);
            builder.setTitle(MiogramLocale.get("Кольори тексту профілю", "Цвета текста профиля", "Profile text colors"), true);
            builder.setApplyBottomPadding(false);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);

            // Zipper feedback
            MiogramHaptic.zipper(root);

            // Subtitle note from Photo 3
            TextInfoPrivacyCell noteCell = new TextInfoPrivacyCell(context);
            noteCell.setText(MiogramLocale.get(
                "Індивідуальні кольори для кожного елемента профілю. Колір лягає на відповідну частину або перевизначає палітру теми.",
                "Индивидуальные цвета для каждого элемента профиля. Цвет ложится на соответствующую часть или переопределяет палитру темы.",
                "Individual colors for each profile element. The color applies to its respective part or overrides the theme palette."
            ));
            root.addView(noteCell);

            // Animated segmented control (EditSegments)
            final LinearLayout rowsContainer = new LinearLayout(context);
            rowsContainer.setOrientation(LinearLayout.VERTICAL);

            final String[] segs = (LocaleController.isRTL || !MiogramLocale.isUkrainian()) ? SEGMENTS_RU : SEGMENTS_UA;
            final JSONObject paletteJson = getPaletteJson();

            EditSegments segments = new EditSegments(context, 0, segs, index -> {
                filterRows(context, rowsContainer, index, paletteJson);
            });
            root.addView(segments);

            // Container for EditColorRow items inside a ScrollView
            ScrollView scrollView = new ScrollView(context);
            scrollView.setFillViewport(true);
            scrollView.addView(rowsContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 320));

            // Restore theme colors button
            TextCell restoreCell = new TextCell(context);
            restoreCell.setTextAndValue(MiogramLocale.get("Повернути кольори теми", "Вернуть цвета темы", "Restore theme colors"), "", false);
            restoreCell.setOnClickListener(v -> {
                MiogramHaptic.warn(v);
                MiogramCustomUiPrefs.setProfilePalette("{}");
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
                Toast.makeText(context, MiogramLocale.get("Кольори скинуто до теми", "Цвета сброшены к теме", "Restored to theme colors"), Toast.LENGTH_SHORT).show();
                filterRows(context, rowsContainer, 0, new JSONObject());
            });
            root.addView(restoreCell);

            filterRows(context, rowsContainer, 0, paletteJson);

            builder.setCustomView(root);
            BottomSheet sheet = builder.create();
            sheet.setDimBehindAlpha(60);
            sheet.setFixNavigationBar(true);
            sheet.show();
        }

        private static void filterRows(final Context context, LinearLayout container, int segmentIndex, final JSONObject paletteJson) {
            container.removeAllViews();
            for (int i = 0; i < ALL_ITEMS.length; i++) {
                final ColorItem item = ALL_ITEMS[i];
                if (segmentIndex != 0 && item.segment != segmentIndex) {
                    continue;
                }
                final String title = MiogramLocale.get(item.titleUa, item.titleRu, item.titleEn);
                int color = item.defaultColor;
                if (paletteJson.has(item.key)) {
                    color = MiogramCustomUiPrefs.parseColor(paletteJson.optString(item.key), item.defaultColor);
                }

                final EditColorRow colorRow = new EditColorRow(context, title, color, true, null);
                colorRow.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    EditColorPicker.show(context, title, colorRow.getColor(), picked -> {
                        colorRow.setColor(picked);
                        try {
                            paletteJson.put(item.key, MiogramCustomUiPrefs.hex(picked));
                            MiogramCustomUiPrefs.setProfilePalette(paletteJson.toString());
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme);
                        } catch (Throwable ignore) {}
                    });
                });
                container.addView(colorRow);
            }
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
     * 11. EDITBANNERSHEET (1-to-1 with cpb.EditBannerSheet.java)
     * ========================================================================= */

    public static class EditBannerSheet {
        private static final String[] MODES = {"Медіа", "Колір", "Градієнт"};
        private static final String[] MODES_RU = {"Медиа", "Цвет", "Градиент"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Шапка профілю", "Шапка профиля", "Profile header"));

            sheet.check(MiogramLocale.get("Своя шапка", "Своя шапка", "Custom header"), MiogramCustomUiPrefs.isBannerEnabled(), true, val -> {
                MiogramCustomUiPrefs.setBannerEnabled(val != 0);
            });

            sheet.chooser(MiogramLocale.get("Чим закрита шапка", "Чем закрыта шапка", "Header fill"), MiogramCustomUiPrefs.getBannerMode(), MODES, MODES_RU, val -> {
                MiogramCustomUiPrefs.setBannerMode(val);
                applyMode(sheet, val);
            });

            sheet.group("banner_media");
            sheet.header(MiogramLocale.get("Медіа", "Медиа", "Media"));
            sheet.row(MiogramLocale.get("Картинка або відео", "Картинка или видео", "Image or video"), MiogramLocale.get("не обрано", "не выбран", "not selected"), true, () -> {
                Toast.makeText(context, MiogramLocale.get("Оберіть файл шапки у галереї", "Выберите файл шапки в галерее", "Select banner in gallery"), Toast.LENGTH_SHORT).show();
            });
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getBannerAlpha(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setBannerAlpha(val);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getBannerDim(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setBannerDim(val);
            });
            sheet.endGroup();

            sheet.group("banner_color");
            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.color(MiogramLocale.get("Колір шапки", "Цвет шапки", "Header color"), MiogramCustomUiPrefs.getBannerColor(), false, color -> {
                MiogramCustomUiPrefs.setBannerColor(color);
            });
            sheet.endGroup();

            sheet.check(MiogramLocale.get("З'єднати з фоном", "Соединить с фоном", "Blend into background"), MiogramCustomUiPrefs.isBannerBlend(), false, val -> {
                MiogramCustomUiPrefs.setBannerBlend(val != 0);
                sheet.setGroupVisible("banner_blend_opts", val != 0);
            });

            sheet.group("banner_blend_opts");
            sheet.slider(MiogramLocale.get("Радіус переходу", "Радиус перехода", "Blend radius"), MiogramCustomUiPrefs.getBannerBlendRadius(), 2, 60, "%", val -> {
                MiogramCustomUiPrefs.setBannerBlendRadius(val);
            });
            sheet.endGroup();

            sheet.note(MiogramLocale.get(
                "Нижній край шапки розчиняється та злегка розмивається, переходячи у фон профілю.",
                "Нижний край шапки растворяется и слегка размывается, переходя в фон профиля.",
                "The bottom edge of the header dissolves and blurs slightly into the profile background."
            ));

            sheet.check(MiogramLocale.get("Показувати емодзі", "Показывать эмодзи", "Show emoji"), MiogramCustomUiPrefs.isShowEmoji(), true, val -> {
                MiogramCustomUiPrefs.setShowEmoji(val != 0);
            });

            applyMode(sheet, MiogramCustomUiPrefs.getBannerMode());
            sheet.setGroupVisible("banner_blend_opts", MiogramCustomUiPrefs.isBannerBlend());

            sheet.show();
        }

        private static void applyMode(EditSheet sheet, int mode) {
            sheet.setGroupVisible("banner_media", mode == 0);
            sheet.setGroupVisible("banner_color", mode == 1);
        }
    }

    /* =========================================================================
     * 12. EDITBACKGROUNDSHEET (1-to-1 with cpb.EditBackgroundSheet.java)
     * ========================================================================= */

    public static class EditBackgroundSheet {
        private static final String[] MODES = {"Колір", "Медіа"};
        private static final String[] MODES_RU = {"Цвет", "Медиа"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Фон профілю", "Фон профиля", "Profile background"));

            sheet.check(MiogramLocale.get("Свій фон", "Свой фон", "Custom background"), MiogramCustomUiPrefs.isBgEnabled(), false, val -> {
                MiogramCustomUiPrefs.setBgEnabled(val != 0);
            });

            sheet.check(MiogramLocale.get("Сумісність", "Совместимость", "Compatibility"), MiogramCustomUiPrefs.isBgCompat(), false, val -> {
                MiogramCustomUiPrefs.setBgCompat(val != 0);
            });
            sheet.note(MiogramLocale.get(
                "Сумісність кладе фон під список іншим способом. Вона потрібна оболонкам, у яких свій фон профілю.",
                "Совместимость кладёт фон под список другим способом. Она нужна оболочкам, у которых свой фон профиля.",
                "Compatibility places the background under the list a different way."
            ));

            sheet.chooser(MiogramLocale.get("Чим закритий фон", "Чем закрыт фон", "Background fill"), MiogramCustomUiPrefs.getBgMode(), MODES, MODES_RU, val -> {
                MiogramCustomUiPrefs.setBgMode(val);
                applyMode(sheet, val);
            });

            sheet.group("bg_color");
            sheet.header(MiogramLocale.get("Колір", "Цвет", "Color"));
            sheet.color(MiogramLocale.get("Колір фону", "Цвет фона", "Background color"), MiogramCustomUiPrefs.getBgColor(), false, color -> {
                MiogramCustomUiPrefs.setBgColor(color);
            });
            sheet.endGroup();

            sheet.group("bg_media");
            sheet.header(MiogramLocale.get("Медіа", "Медиа", "Media"));
            sheet.row(MiogramLocale.get("Картинка або відео", "Картинка или видео", "Image or video"), MiogramLocale.get("не обрано", "не выбран", "not selected"), true, () -> {
                Toast.makeText(context, MiogramLocale.get("Оберіть файл фону у галереї", "Выберите файл фона в галерее", "Select background in gallery"), Toast.LENGTH_SHORT).show();
            });
            sheet.slider(MiogramLocale.get("Прозорість", "Прозрачность", "Opacity"), MiogramCustomUiPrefs.getBgAlpha(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setBgAlpha(val);
            });
            sheet.slider(MiogramLocale.get("Затемнення", "Затемнение", "Dimming"), MiogramCustomUiPrefs.getBgDim(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setBgDim(val);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Блоки", "Блоки", "Blocks"));
            sheet.row(MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"), "", false, () -> {
                sheet.dismiss();
                EditBlocksSheet.show(context);
            });

            applyMode(sheet, MiogramCustomUiPrefs.getBgMode());
            sheet.show();
        }

        private static void applyMode(EditSheet sheet, int mode) {
            sheet.setGroupVisible("bg_color", mode == 0);
            sheet.setGroupVisible("bg_media", mode == 1);
        }
    }

    /* =========================================================================
     * 13. EDITBLOCKSSHEET (1-to-1 with cpb.EditBlocksSheet.java)
     * ========================================================================= */

    public static class EditBlocksSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Налаштувати блоки", "Настроить блоки", "Configure blocks"));

            sheet.check(MiogramLocale.get("Перефарбувати блоки", "Перекрасить блоки", "Recolor blocks"), MiogramCustomUiPrefs.isBlocksColorEnabled(), true, val -> {
                MiogramCustomUiPrefs.setBlocksColorEnabled(val != 0);
                sheet.setGroupVisible("blocks_color_opts", val != 0);
                sheet.setGroupVisible("blocks_rad_opts", val != 0 && MiogramCustomUiPrefs.isBlocksRadiusEnabled());
            });

            sheet.group("blocks_color_opts");
            sheet.header(MiogramLocale.get("Вигляд", "Вид", "Look"));
            sheet.color(MiogramLocale.get("Колір блоків", "Цвет блоков", "Block color"), MiogramCustomUiPrefs.getBlocksColor(), false, color -> {
                MiogramCustomUiPrefs.setBlocksColor(color);
            });
            sheet.slider(MiogramLocale.get("Щільність", "Плотность", "Density"), MiogramCustomUiPrefs.getBlocksAlpha(), 10, 100, "%", val -> {
                MiogramCustomUiPrefs.setBlocksAlpha(val);
            });

            sheet.header(MiogramLocale.get("Форма", "Форма", "Shape"));
            sheet.check(MiogramLocale.get("З'єднати найближчі блоки", "Соединить ближайшие блоки", "Join nearby blocks"), MiogramCustomUiPrefs.isBlocksJoin(), false, val -> {
                MiogramCustomUiPrefs.setBlocksJoin(val != 0);
            });
            sheet.note(MiogramLocale.get(
                "Дрібні зазори між сусідніми блоками закриваються, і вони встають однією колонкою.",
                "Мелкие зазоры между соседними блоками закрываются, и они встают одной колонкой.",
                "Small gaps between neighbouring blocks are closed and they stand as one column."
            ));

            sheet.check(MiogramLocale.get("Своє скруглення", "Своё скругление", "Custom rounding"), MiogramCustomUiPrefs.isBlocksRadiusEnabled(), false, val -> {
                MiogramCustomUiPrefs.setBlocksRadiusEnabled(val != 0);
                sheet.setGroupVisible("blocks_rad_opts", val != 0);
            });

            sheet.group("blocks_rad_opts");
            sheet.slider(MiogramLocale.get("Скруглення блоків", "Скругление блоков", "Block rounding"), MiogramCustomUiPrefs.getBlocksRadius(), 0, 30, "dp", val -> {
                MiogramCustomUiPrefs.setBlocksRadius(val);
            });
            sheet.endGroup();

            sheet.endGroup(); // end blocks_color_opts

            sheet.header(MiogramLocale.get("Розмиття", "Размытие", "Blur"));
            sheet.slider(MiogramLocale.get("Розмиття за блоком", "Размытие за блоком", "Blur behind block"), MiogramCustomUiPrefs.getBlocksBlur(), 0, 50, "", val -> {
                MiogramCustomUiPrefs.setBlocksBlur(val);
            });
            sheet.slider(MiogramLocale.get("Глибина розмиття", "Глубина размытия", "Blur depth"), MiogramCustomUiPrefs.getBlocksDepth(), 1, 4, "", val -> {
                MiogramCustomUiPrefs.setBlocksDepth(val);
            });
            sheet.note(MiogramLocale.get(
                "Розмиття бере те, що лежить під блоком, тому помітне лише на своєму фоні.",
                "Размытие берёт то, что лежит под блоком, поэтому заметно только на своём фоне.",
                "Blur takes what lies under the block, so it shows only over a custom background."
            ));

            boolean colOn = MiogramCustomUiPrefs.isBlocksColorEnabled();
            sheet.setGroupVisible("blocks_color_opts", colOn);
            sheet.setGroupVisible("blocks_rad_opts", colOn && MiogramCustomUiPrefs.isBlocksRadiusEnabled());

            sheet.show();
        }
    }

    /* =========================================================================
     * 14. EDITTHOUGHTSHEET (1-to-1 with cpb.EditThoughtSheet.java)
     * ========================================================================= */

    public static class EditThoughtSheet {
        private static final String[] FONTS = {"Стандарт", "Тонкий", "Із зарубками", "Моно", "Курсив", "Вузький", "Свій"};
        private static final String[] FONTS_RU = {"Стандарт", "Тонкий", "Засечки", "Моно", "Курсив", "Узкий", "Свой"};

        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Думка", "Мысль", "Thought"));

            sheet.header(MiogramLocale.get("Текст думки", "Текст мысли", "Thought text"));
            sheet.row(MiogramLocale.get("Текст", "Текст", "Text"), MiogramCustomUiPrefs.getThoughtText(), false, () -> {
                promptText(context, MiogramLocale.get("Про що думає профіль", "О чём думает профиль", "What the profile is thinking"), MiogramCustomUiPrefs.getThoughtText(), newText -> {
                    MiogramCustomUiPrefs.setThoughtText(newText);
                    sheet.dismiss();
                    show(context);
                });
            });
            sheet.note(MiogramLocale.get(
                "Порожній текст прибирає хмаринку з профілю.",
                "Пустой текст убирает облачко с профиля.",
                "Empty text removes the bubble from the profile."
            ));

            sheet.header(MiogramLocale.get("Кольори", "Цвета", "Colors"));
            sheet.color(MiogramLocale.get("Колір тексту", "Цвет текста", "Text color"), MiogramCustomUiPrefs.getThoughtTextColor(), true, color -> {
                MiogramCustomUiPrefs.setThoughtTextColor(color);
            });
            sheet.color(MiogramLocale.get("Колір хмаринки", "Цвет облачка", "Bubble color"), MiogramCustomUiPrefs.getThoughtBgColor(), false, color -> {
                MiogramCustomUiPrefs.setThoughtBgColor(color);
            });

            sheet.header(MiogramLocale.get("Тінь", "Тень", "Shadow"));
            sheet.check(MiogramLocale.get("Тінь хмаринки", "Тень облачка", "Bubble shadow"), MiogramCustomUiPrefs.isThoughtShadowEnabled(), true, val -> {
                MiogramCustomUiPrefs.setThoughtShadowEnabled(val != 0);
                sheet.setGroupVisible("thought_shadow", val != 0);
            });

            sheet.group("thought_shadow");
            sheet.color(MiogramLocale.get("Колір тіні", "Цвет тени", "Shadow color"), MiogramCustomUiPrefs.getThoughtShadowColor(), true, color -> {
                MiogramCustomUiPrefs.setThoughtShadowColor(color);
            });
            sheet.slider(MiogramLocale.get("Розмиття", "Размытие", "Blur"), MiogramCustomUiPrefs.getThoughtShadowRadius(), 0, 40, "", val -> {
                MiogramCustomUiPrefs.setThoughtShadowRadius(val);
            });
            sheet.slider(MiogramLocale.get("Сила", "Сила", "Strength"), MiogramCustomUiPrefs.getThoughtShadowStrength(), 0, 100, "%", val -> {
                MiogramCustomUiPrefs.setThoughtShadowStrength(val);
            });
            sheet.slider(MiogramLocale.get("Зсув убік", "Сдвиг вбок", "Shift sideways"), MiogramCustomUiPrefs.getThoughtShadowDx(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setThoughtShadowDx(val);
            });
            sheet.slider(MiogramLocale.get("Зсув вниз", "Сдвиг вниз", "Shift down"), MiogramCustomUiPrefs.getThoughtShadowDy(), -20, 20, "", val -> {
                MiogramCustomUiPrefs.setThoughtShadowDy(val);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Накреслення", "Начертание", "Typeface"));
            sheet.check(MiogramLocale.get("Як у імені", "Как у имени", "Same as name"), MiogramCustomUiPrefs.isThoughtFontCopy(), false, val -> {
                MiogramCustomUiPrefs.setThoughtFontCopy(val != 0);
                sheet.setGroupVisible("thought_font_opts", val == 0);
            });

            sheet.group("thought_font_opts");
            sheet.chooser(MiogramLocale.get("Шрифт", "Шрифт", "Font"), MiogramCustomUiPrefs.getThoughtFont(), FONTS, FONTS_RU, val -> {
                MiogramCustomUiPrefs.setThoughtFont(val);
            });
            sheet.endGroup();

            sheet.setGroupVisible("thought_shadow", MiogramCustomUiPrefs.isThoughtShadowEnabled());
            sheet.setGroupVisible("thought_font_opts", !MiogramCustomUiPrefs.isThoughtFontCopy());

            sheet.show();
        }

        private static void promptText(Context context, String title, String initial, final IntSinkStr onDone) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title);
            final android.widget.EditText input = new android.widget.EditText(context);
            input.setText(initial);
            builder.setView(input);
            builder.setPositiveButton(MiogramLocale.get("Зберегти", "Сохранить", "Save"), (dialog, which) -> {
                if (onDone != null) onDone.accept(input.getText().toString().trim());
            });
            builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
            builder.show();
        }

        private interface IntSinkStr {
            void accept(String str);
        }
    }

    /* =========================================================================
     * 15. EDITVISIBILITYSHEET (1-to-1 with cpb.EditVisibilitySheet.java)
     * ========================================================================= */

    public static class EditVisibilitySheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Видимість рядків", "Видимость строк", "Row visibility"));

            sheet.header(MiogramLocale.get("Рядки профілю", "Строки профиля", "Profile rows"));
            sheet.check(MiogramLocale.get("Номер телефону", "Номер телефона", "Phone number"), !MiogramCustomUiPrefs.isHideRowPhone(), true, val -> {
                MiogramCustomUiPrefs.setHideRowPhone(val == 0);
            });
            sheet.check(MiogramLocale.get("Ім'я користувача", "Имя пользователя", "Username"), !MiogramCustomUiPrefs.isHideRowUsername(), true, val -> {
                MiogramCustomUiPrefs.setHideRowUsername(val == 0);
            });
            sheet.check(MiogramLocale.get("Опис (Біо)", "Описание (Био)", "Bio"), !MiogramCustomUiPrefs.isHideRowBio(), true, val -> {
                MiogramCustomUiPrefs.setHideRowBio(val == 0);
            });
            sheet.note(MiogramLocale.get(
                "Прихований рядок зникає з профілю цілком. Повернути його можна тут же.",
                "Скрытая строка исчезает с профиля целиком. Вернуть её можно здесь же.",
                "A hidden row disappears entirely. You can restore it right here."
            ));

            sheet.header(MiogramLocale.get("Нижній блок", "Нижний блок", "Bottom block"));
            sheet.check(MiogramLocale.get("Показувати блок із вкладками", "Показывать блок с вкладками", "Show tabs block"), !MiogramCustomUiPrefs.isHideMediaTabs(), false, val -> {
                MiogramCustomUiPrefs.setHideMediaTabs(val == 0);
            });
            sheet.note(MiogramLocale.get(
                "Смуга вкладок унизу профілю з усім її вмістом: публікаціями, подарунками, медіа, файлами.",
                "Полоса вкладок внизу профиля со всем её содержимым: публикациями, подарками, медиа, файлами.",
                "The tab strip at the bottom of the profile with all its content."
            ));

            sheet.show();
        }
    }

    /* =========================================================================
     * 16. EDITINTERFACESHEET (UI & Badges)
     * ========================================================================= */

    public static class EditInterfaceSheet {
        public static void show(final Context context) {
            if (context == null) return;
            final EditSheet sheet = new EditSheet(context, MiogramLocale.get("Інтерфейс клієнта", "Интерфейс клиента", "Client interface"));

            sheet.header(MiogramLocale.get("Скляний блюр", "Стеклянный блюр", "Glass blur"));
            sheet.check(MiogramLocale.get("Скляний блюр панелей", "Стеклянный блюр панелей", "Glass blur panels"), MiogramCustomUiPrefs.isUiGlassBlur(), true, val -> {
                MiogramCustomUiPrefs.setUiGlassBlur(val != 0);
            });
            sheet.check(MiogramLocale.get("Картки діалогів", "Карточки диалогов", "Dialog cards"), MiogramCustomUiPrefs.isUiDialogCards(), false, val -> {
                MiogramCustomUiPrefs.setUiDialogCards(val != 0);
            });

            sheet.header(MiogramLocale.get("Бейджі лічильників", "Бейджи счётчиков", "Counter badges"));
            sheet.check(MiogramLocale.get("Кастомні бейджі", "Кастомные бейджи", "Custom badges"), MiogramCustomUiPrefs.isUiBadgeCustom(), true, val -> {
                MiogramCustomUiPrefs.setUiBadgeCustom(val != 0);
                sheet.setGroupVisible("badge_opts", val != 0);
            });

            sheet.group("badge_opts");
            sheet.color(MiogramLocale.get("Колір бейджа", "Цвет бейджа", "Badge color"), MiogramCustomUiPrefs.getUiBadgeColor(), true, color -> {
                MiogramCustomUiPrefs.setUiBadgeColor(color);
            });
            sheet.color(MiogramLocale.get("Колір цифр", "Цвет цифр", "Text color"), MiogramCustomUiPrefs.getUiBadgeTextColor(), true, color -> {
                MiogramCustomUiPrefs.setUiBadgeTextColor(color);
            });
            sheet.check(MiogramLocale.get("Світіння бейджа", "Свечение бейджа", "Badge glow"), MiogramCustomUiPrefs.isUiBadgeGlow(), false, val -> {
                MiogramCustomUiPrefs.setUiBadgeGlow(val != 0);
            });
            sheet.endGroup();

            sheet.header(MiogramLocale.get("Тактильність", "Тактильность", "Haptic feedback"));
            sheet.check(MiogramLocale.get("Тактильний відгук (Haptic)", "Тактильный отклик (Haptic)", "Haptic feedback"), MiogramCustomUiPrefs.isUiHaptic(), false, val -> {
                MiogramCustomUiPrefs.setUiHaptic(val != 0);
            });

            sheet.setGroupVisible("badge_opts", MiogramCustomUiPrefs.isUiBadgeCustom());
            sheet.show();
        }
    }
}
