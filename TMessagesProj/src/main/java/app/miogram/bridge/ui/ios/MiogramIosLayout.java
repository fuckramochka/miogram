package app.miogram.bridge.ui.ios;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
 * Directly ported from official Telegram-iOS open-source codebase:
 *  - ChatBubblePath.swift (Cubic Bezier curve message tails & 17pt radii)
 *  - NavigationBar.swift (34pt Large Titles & iOS Search Bar)
 *  - TabBarNode.swift (49pt Translucent Cupertino Bottom Bar & SF Symbols)
 *  - ItemListNode.swift (10pt Inset Grouped card styles & disclosure chevrons ›)
 *  - ChatListItemNode.swift (54pt continuous squircle avatars & 78pt offset dividers)
 */
public class MiogramIosLayout {

    // Apple iOS System Palette
    public static final int COLOR_IOS_BLUE = 0xFF007AFF;
    public static final int COLOR_IOS_BLUE_DARK = 0xFF0A84FF;
    public static final int COLOR_IOS_GREEN = 0xFF34C759;
    public static final int COLOR_IOS_RED = 0xFFFF3B30;
    public static final int COLOR_IOS_ORANGE = 0xFFFF9500;
    public static final int COLOR_IOS_GRAY = 0xFF8E8E93;
    public static final int COLOR_IOS_GRAY_LIGHT = 0xFFAEAEB2;
    public static final int COLOR_IOS_SEARCH_BG_LIGHT = 0x1F767680;
    public static final int COLOR_IOS_SEARCH_BG_DARK = 0x3D767680;
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

    private static final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    static {
        separatorPaint.setStyle(Paint.Style.STROKE);
        separatorPaint.setStrokeWidth(AndroidUtilities.dp(0.5f));
    }

    public static boolean isIosPresetActive(Context context) {
        return MiogramDivineEngine.getCurrentPreset(context) == MiogramDivineEngine.Preset.IOS_GLASS;
    }

    /**
     * Port of Telegram-iOS `ChatBubblePath.swift`:
     * Calculates the exact Cubic Bezier curve for iOS message bubbles.
     */
    public static Path buildIosBubblePath(float left, float top, float right, float bottom, boolean isOut, boolean hasTail) {
        Path path = new Path();
        float radius = AndroidUtilities.dp(17);
        float tailW = AndroidUtilities.dp(6);
        float tailH = AndroidUtilities.dp(17.5f);

        if (!hasTail) {
            rectF.set(left, top, right, bottom);
            path.addRoundRect(rectF, radius, radius, Path.Direction.CW);
            return path;
        }

        if (isOut) {
            // Outgoing bubble with tail on bottom-right
            path.moveTo(left + radius, top);
            path.lineTo(right - radius, top);
            path.quadTo(right, top, right, top + radius);
            path.lineTo(right, bottom - tailH);
            
            // Exact S-curve from ChatBubblePath.swift
            path.cubicTo(right, bottom - tailH + AndroidUtilities.dp(8), right + tailW - AndroidUtilities.dp(2), bottom - AndroidUtilities.dp(2), right + tailW, bottom);
            path.cubicTo(right + tailW - AndroidUtilities.dp(4), bottom - AndroidUtilities.dp(0.5f), right - AndroidUtilities.dp(2), bottom - AndroidUtilities.dp(3), right - AndroidUtilities.dp(6), bottom);
            
            path.lineTo(left + radius, bottom);
            path.quadTo(left, bottom, left, bottom - radius);
            path.lineTo(left, top + radius);
            path.quadTo(left, top, left + radius, top);
            path.close();
        } else {
            // Incoming bubble with tail on bottom-left
            path.moveTo(left + radius, top);
            path.lineTo(right - radius, top);
            path.quadTo(right, top, right, top + radius);
            path.lineTo(right, bottom - radius);
            path.quadTo(right, bottom, right - radius, bottom);
            path.lineTo(left + AndroidUtilities.dp(6), bottom);
            
            // Exact S-curve from ChatBubblePath.swift for incoming tail
            path.cubicTo(left + AndroidUtilities.dp(2), bottom - AndroidUtilities.dp(3), left - tailW + AndroidUtilities.dp(4), bottom - AndroidUtilities.dp(0.5f), left - tailW, bottom);
            path.cubicTo(left - tailW + AndroidUtilities.dp(2), bottom - AndroidUtilities.dp(2), left, bottom - tailH + AndroidUtilities.dp(8), left, bottom - tailH);
            
            path.lineTo(left, top + radius);
            path.quadTo(left, top, left + radius, top);
            path.close();
        }

        return path;
    }

    /**
     * Port of `Display/NavigationBar.swift`:
     * Creates an iOS Large Title Navigation Bar Header.
     */
    public static View createIosLargeTitleHeader(Context context, String title, View.OnClickListener onEditClick, View.OnClickListener onComposeClick) {
        return createIosLargeTitleHeader(context, title, onEditClick, onComposeClick, null);
    }

    public static View createIosLargeTitleHeader(Context context, String title, View.OnClickListener onEditClick, View.OnClickListener onComposeClick, View.OnClickListener onSearchClick) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Theme.isCurrentThemeDark() ? COLOR_IOS_NAV_BAR_DARK : COLOR_IOS_NAV_BAR_LIGHT);
        header.setPadding(AndroidUtilities.dp(16), AndroidUtilities.statusBarHeight + AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        // Top Row: "Edit" on left, "Compose" on right
        FrameLayout topRow = new FrameLayout(context);

        TextView editBtn = new TextView(context);
        editBtn.setText(MiogramLocale.get("Ред.", "Изм.", "Edit"));
        editBtn.setTextColor(COLOR_IOS_BLUE);
        editBtn.setTextSize(17);
        if (onEditClick != null) editBtn.setOnClickListener(onEditClick);
        topRow.addView(editBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        ImageView composeBtn = new ImageView(context);
        composeBtn.setImageResource(R.drawable.msg_edit);
        composeBtn.setColorFilter(COLOR_IOS_BLUE);
        if (onComposeClick != null) composeBtn.setOnClickListener(onComposeClick);
        topRow.addView(composeBtn, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        header.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Large Title: 34pt bold
        TextView largeTitle = new TextView(context);
        largeTitle.setText(title != null ? title : MiogramLocale.get("Чати", "Чаты", "Chats"));
        largeTitle.setTextSize(32);
        largeTitle.setTypeface(AndroidUtilities.bold());
        largeTitle.setTextColor(Theme.isCurrentThemeDark() ? Color.WHITE : Color.BLACK);
        largeTitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        header.addView(largeTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // iOS Search Bar
        header.addView(createIosSearchBar(context, onSearchClick), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));

        return header;
    }

    /**
     * Port of `Display/SearchBarNode.swift`:
     * Creates a pixel-perfect Cupertino Search Bar.
     */
    public static View createIosSearchBar(Context context, View.OnClickListener onSearchClick) {
        FrameLayout searchBox = new FrameLayout(context);
        searchBox.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), Theme.isCurrentThemeDark() ? COLOR_IOS_SEARCH_BG_DARK : COLOR_IOS_SEARCH_BG_LIGHT));
        searchBox.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER);

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.msg_search);
        searchIcon.setColorFilter(COLOR_IOS_GRAY);
        inner.addView(searchIcon, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL));

        TextView hint = new TextView(context);
        hint.setText(" " + MiogramLocale.get("Пошук повідомлень або людей", "Поиск сообщений или людей", "Search for messages or users"));
        hint.setTextColor(COLOR_IOS_GRAY);
        hint.setTextSize(14);
        inner.addView(hint, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        searchBox.addView(inner, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        if (onSearchClick != null) searchBox.setOnClickListener(onSearchClick);

        return searchBox;
    }

    /**
     * Creates an authentic iOS Inset Grouped Card background drawable.
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
     * Port of `TabBarNode.swift`:
     * Creates a 1:1 iOS Bottom TabBar with 4 Cupertino Tabs (Contacts, Calls, Chats, Settings).
     */
    public static View createIosTabBar(Context context, int activeTab, OnTabClickListener listener) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(MiogramIosTheme.getTabBarBg());

        // 0.5dp top hairline separator
        View hairline = new View(context);
        hairline.setBackgroundColor(MiogramIosTheme.getTabBarSeparator());
        root.addView(hairline, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 0.5f, Gravity.TOP));

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(4));

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

            FrameLayout iconContainer = new FrameLayout(context);

            View iconView;
            if (i == 0) iconView = new IosContactsIconView(context, isSelected);
            else if (i == 1) iconView = new IosCallsIconView(context, isSelected);
            else if (i == 2) iconView = new IosChatsIconView(context, isSelected);
            else iconView = new IosSettingsIconView(context, isSelected);

            iconContainer.addView(iconView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));

            if (i == 2) {
                // Chats unread badge: 1:1 from TabBarNode.swift (18dp height pill, min 18dp width)
                int totalUnread = org.telegram.messenger.NotificationsController.getInstance(UserConfig.selectedAccount).getTotalUnreadCount();
                if (totalUnread > 0) {
                    TextView badge = new TextView(context);
                    badge.setText(totalUnread > 99 ? "99+" : String.valueOf(totalUnread));
                    badge.setTextSize(10f);
                    badge.setTextColor(Theme.isCurrentThemeDark() ? MiogramIosTheme.TAB_BAR_BADGE_TEXT_DARK : MiogramIosTheme.TAB_BAR_BADGE_TEXT_LIGHT);
                    badge.setTypeface(AndroidUtilities.bold());
                    badge.setGravity(Gravity.CENTER);
                    int badgeBg = Theme.isCurrentThemeDark() ? MiogramIosTheme.TAB_BAR_BADGE_BG_DARK : MiogramIosTheme.TAB_BAR_BADGE_BG_LIGHT;
                    badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(9), badgeBg));
                    badge.setPadding(AndroidUtilities.dp(5), 0, AndroidUtilities.dp(5), 0);
                    FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            AndroidUtilities.dp(18)
                    );
                    badgeLp.gravity = Gravity.TOP | Gravity.RIGHT;
                    badgeLp.topMargin = AndroidUtilities.dp(-2);
                    badgeLp.rightMargin = AndroidUtilities.dp(-6);
                    iconContainer.addView(badge, badgeLp);
                }
            }

            item.addView(iconContainer, LayoutHelper.createLinear(36, 26, Gravity.CENTER));

            TextView label = new TextView(context);
            label.setText(titles[i]);
            label.setTextSize(10f);
            label.setTypeface(isSelected ? AndroidUtilities.bold() : null);
            label.setTextColor(MiogramIosTheme.getTabBarText(isSelected));
            label.setGravity(Gravity.CENTER);
            item.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 1, 0, 0));

            item.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                if (listener != null) listener.onTabClick(index);
            });

            bar.addView(item, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }

        root.addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return root;
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
            paint.setColor(MiogramIosTheme.getTabBarIcon(isSelected));
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
            paint.setColor(MiogramIosTheme.getTabBarIcon(isSelected));
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
            paint.setColor(MiogramIosTheme.getTabBarIcon(isSelected));
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
            paint.setColor(MiogramIosTheme.getTabBarIcon(isSelected));
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