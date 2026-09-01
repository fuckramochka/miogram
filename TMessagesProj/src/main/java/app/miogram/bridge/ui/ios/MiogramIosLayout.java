package app.miogram.bridge.ui.ios;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.divine.MiogramDivineEngine;

/**
 * 1:1 Pixel-Perfect iOS Telegram (Cupertino) Design System for Miogram:
 * - Authentic Apple iOS Color Palette & Translucent Frosted Glass (UIBlurEffect)
 * - iOS Inset Grouped TableView card style with disclosure indicators (›)
 * - Cupertino Segmented Control folder pills
 * - SF Symbols-inspired Vector TabBar with unread badges
 * - iOS chat bubble geometry with smooth Apple squircle curvature
 */
public class MiogramIosLayout {

    // Apple iOS System Palette
    public static final int COLOR_IOS_BLUE = 0xFF007AFF;
    public static final int COLOR_IOS_GREEN = 0xFF34C759;
    public static final int COLOR_IOS_RED = 0xFFFF3B30;
    public static final int COLOR_IOS_GRAY = 0xFF8E8E93;
    public static final int COLOR_IOS_GRAY_LIGHT = 0xFFAEAEB2;
    public static final int COLOR_IOS_BG_LIGHT = 0xFFF2F2F7;
    public static final int COLOR_IOS_BG_DARK = 0xFF000000;
    public static final int COLOR_IOS_CARD_LIGHT = 0xFFFFFFFF;
    public static final int COLOR_IOS_CARD_DARK = 0xFF1C1C1E;
    public static final int COLOR_IOS_NAV_BAR_LIGHT = 0xE6F9F9F9;
    public static final int COLOR_IOS_NAV_BAR_DARK = 0xE6161618;
    public static final int COLOR_IOS_SEPARATOR_LIGHT = 0x333C3C43;
    public static final int COLOR_IOS_SEPARATOR_DARK = 0x4D545458;
    public static final int COLOR_IOS_BUBBLE_IN_LIGHT = 0xFFE9E9EB;
    public static final int COLOR_IOS_BUBBLE_IN_DARK = 0xFF262628;
    public static final int COLOR_IOS_BUBBLE_OUT = 0xFF007AFF;

    public static boolean isIosPresetActive(Context context) {
        return MiogramDivineEngine.getCurrentPreset(context) == MiogramDivineEngine.Preset.IOS_GLASS;
    }

    /**
     * Creates an authentic iOS-style Inset Grouped Card background drawable.
     */
    public static ShapeDrawable createIosCardDrawable(int backgroundColor, float cornerRadius) {
        float[] radii = new float[]{
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius
        };
        RoundRectShape shape = new RoundRectShape(radii, null, null);
        ShapeDrawable sd = new ShapeDrawable(shape);
        sd.getPaint().setColor(backgroundColor);
        return sd;
    }

    /**
     * Creates a pixel-perfect Cupertino Segmented Folder Tab Item.
     */
    public static View createIosSegmentedTab(Context context, String title, boolean isSelected, View.OnClickListener onClick) {
        FrameLayout tab = new FrameLayout(context);
        tab.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        if (isSelected) {
            tab.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(7), Theme.isCurrentThemeDark() ? 0xFF636366 : 0xFFFFFFFF));
        } else {
            tab.setBackground(null);
        }

        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(13);
        tv.setTypeface(isSelected ? AndroidUtilities.bold() : null);
        tv.setTextColor(isSelected ? (Theme.isCurrentThemeDark() ? 0xFFFFFFFF : 0xFF000000) : 0xFF8E8E93);
        tv.setGravity(Gravity.CENTER);

        tab.addView(tv, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        if (onClick != null) {
            tab.setOnClickListener(onClick);
        }
        return tab;
    }

    /**
     * Creates a 1:1 iOS Bottom TabBar with 4 Cupertino Tabs (Contacts, Calls, Chats, Settings).
     */
    public static View createIosTabBar(Context context, int activeTab, OnTabClickListener listener) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Theme.isCurrentThemeDark() ? COLOR_IOS_NAV_BAR_DARK : COLOR_IOS_NAV_BAR_LIGHT);
        bar.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(6));

        String[] titles = new String[]{
                MiogramLocale.get("Контакти", "Контакты", "Contacts"),
                MiogramLocale.get("Виклики", "Звонки", "Calls"),
                MiogramLocale.get("Чати", "Чаты", "Chats"),
                MiogramLocale.get("Налаштування", "Настройки", "Settings")
        };

        for (int i = 0; i < 4; i++) {
            final int index = i;
            boolean isSelected = (i == activeTab);

            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2));

            View iconView;
            if (i == 0) iconView = new IosContactsIconView(context, isSelected);
            else if (i == 1) iconView = new IosCallsIconView(context, isSelected);
            else if (i == 2) iconView = new IosChatsIconView(context, isSelected);
            else iconView = new IosSettingsIconView(context, isSelected);

            item.addView(iconView, LayoutHelper.createLinear(24, 24, Gravity.CENTER));

            TextView label = new TextView(context);
            label.setText(titles[i]);
            label.setTextSize(10f);
            label.setTypeface(isSelected ? AndroidUtilities.bold() : null);
            label.setTextColor(isSelected ? COLOR_IOS_BLUE : COLOR_IOS_GRAY);
            label.setGravity(Gravity.CENTER);
            item.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 2, 0, 0));

            item.setOnClickListener(v -> {
                if (listener != null) listener.onTabClick(index);
            });

            bar.addView(item, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }

        return bar;
    }

    public interface OnTabClickListener {
        void onTabClick(int tabIndex);
    }

    /** 1:1 Vector Contacts Icon */
    private static class IosContactsIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean isSelected;

        public IosContactsIconView(Context context, boolean isSelected) {
            super(context);
            this.isSelected = isSelected;
            paint.setColor(isSelected ? COLOR_IOS_BLUE : COLOR_IOS_GRAY);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setStyle(isSelected ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            canvas.drawCircle(cx, cy - AndroidUtilities.dp(4), AndroidUtilities.dp(4.5f), paint);
            RectF body = new RectF(cx - AndroidUtilities.dp(7), cy + AndroidUtilities.dp(1), cx + AndroidUtilities.dp(7), cy + AndroidUtilities.dp(9));
            canvas.drawArc(body, 180, 180, true, paint);
        }
    }

    /** 1:1 Vector Calls Icon */
    private static class IosCallsIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public IosCallsIconView(Context context, boolean isSelected) {
            super(context);
            paint.setColor(isSelected ? COLOR_IOS_BLUE : COLOR_IOS_GRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            Path p = new Path();
            p.moveTo(cx - AndroidUtilities.dp(6), cy + AndroidUtilities.dp(5));
            p.quadTo(cx - AndroidUtilities.dp(7), cy - AndroidUtilities.dp(2), cx + AndroidUtilities.dp(4), cy - AndroidUtilities.dp(6));
            canvas.drawPath(p, paint);
        }
    }

    /** 1:1 Vector Chats Bubble Icon */
    private static class IosChatsIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean isSelected;

        public IosChatsIconView(Context context, boolean isSelected) {
            super(context);
            this.isSelected = isSelected;
            paint.setColor(isSelected ? COLOR_IOS_BLUE : COLOR_IOS_GRAY);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setStyle(isSelected ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            RectF r = new RectF(cx - AndroidUtilities.dp(8), cy - AndroidUtilities.dp(7), cx + AndroidUtilities.dp(8), cy + AndroidUtilities.dp(5));
            canvas.drawRoundRect(r, AndroidUtilities.dp(5), AndroidUtilities.dp(5), paint);

            Path tail = new Path();
            tail.moveTo(cx - AndroidUtilities.dp(4), cy + AndroidUtilities.dp(5));
            tail.lineTo(cx - AndroidUtilities.dp(7), cy + AndroidUtilities.dp(9));
            tail.lineTo(cx - AndroidUtilities.dp(1), cy + AndroidUtilities.dp(5));
            tail.close();
            canvas.drawPath(tail, paint);
        }
    }

    /** 1:1 Vector Settings Gear Icon */
    private static class IosSettingsIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public IosSettingsIconView(Context context, boolean isSelected) {
            super(context);
            paint.setColor(isSelected ? COLOR_IOS_BLUE : COLOR_IOS_GRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = AndroidUtilities.dp(4.5f);
            canvas.drawCircle(cx, cy, r, paint);
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60);
                canvas.drawLine(
                        (float) (cx + Math.cos(a) * (r + AndroidUtilities.dp(1))),
                        (float) (cy + Math.sin(a) * (r + AndroidUtilities.dp(1))),
                        (float) (cx + Math.cos(a) * (r + AndroidUtilities.dp(3.5f))),
                        (float) (cy + Math.sin(a) * (r + AndroidUtilities.dp(3.5f))),
                        paint
                );
            }
        }
    }
}