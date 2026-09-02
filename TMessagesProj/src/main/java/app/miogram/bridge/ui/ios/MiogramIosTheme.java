package app.miogram.bridge.ui.ios;

import org.telegram.ui.ActionBar.Theme;

/**
 * Direct 1:1 port of Telegram-iOS Presentation Themes:
 * - DefaultDayPresentationTheme.swift
 * - DefaultDarkPresentationTheme.swift
 * Extracted from official TelegramMessenger/Telegram-iOS repository.
 */
public final class MiogramIosTheme {

    private MiogramIosTheme() {}

    // MARK: - Apple / Telegram-iOS System Accents
    public static final int ACCENT_BLUE = 0xFF007AFF;         // defaultDayAccentColor
    public static final int ACCENT_BLUE_DARK = 0xFF0A84FF;    // Dark mode iOS accent
    public static final int ACCENT_GREEN = 0xFF34C759;        // Switch / Secret chat
    public static final int ACCENT_RED = 0xFFFF3B30;          // Badges / Destructive actions
    public static final int ACCENT_ORANGE = 0xFFFF9500;
    public static final int ACCENT_PINK = 0xFFFF2D55;         // Reactions badge

    // MARK: - Navigation Bar (PresentationThemeRootNavigationBar)
    public static final int NAV_BAR_BG_LIGHT = 0xE6F2F2F2;    // UIColor(rgb: 0xf2f2f2, alpha: 0.9)
    public static final int NAV_BAR_BG_DARK = 0xE61D1D1D;     // UIColor(rgb: 0x1d1d1d, alpha: 0.9)
    public static final int NAV_BAR_OPAQUE_LIGHT = 0xFFF7F7F7;
    public static final int NAV_BAR_OPAQUE_DARK = 0xFF1D1D1D;
    public static final int NAV_BAR_SEPARATOR_LIGHT = 0xFFC8C7CC; // hairline 0.5dp
    public static final int NAV_BAR_SEPARATOR_DARK = 0x8C545458;  // UIColor(rgb: 0x545458, alpha: 0.55)

    // MARK: - Tab Bar (PresentationThemeRootTabBar)
    public static final int TAB_BAR_BG_LIGHT = 0xE6F2F2F2;
    public static final int TAB_BAR_BG_DARK = 0xE61D1D1D;
    public static final int TAB_BAR_SEPARATOR_LIGHT = 0xFFB2B2B2;
    public static final int TAB_BAR_SEPARATOR_DARK = 0x8C545458;
    public static final int TAB_BAR_ICON_LIGHT = 0xFF959595;
    public static final int TAB_BAR_ICON_DARK = 0x80FFFFFF;
    public static final int TAB_BAR_ICON_SELECTED_LIGHT = 0xFF007AFF;
    public static final int TAB_BAR_ICON_SELECTED_DARK = 0xFFFFFFFF;
    public static final int TAB_BAR_TEXT_LIGHT = 0xCC000000;
    public static final int TAB_BAR_TEXT_DARK = 0xFFFFFFFF;
    public static final int TAB_BAR_BADGE_BG_LIGHT = 0xFFFF3B30;
    public static final int TAB_BAR_BADGE_BG_DARK = 0xFFFFFFFF;
    public static final int TAB_BAR_BADGE_TEXT_LIGHT = 0xFFFFFFFF;
    public static final int TAB_BAR_BADGE_TEXT_DARK = 0xFF000000;

    // MARK: - Navigation Search Bar (PresentationThemeNavigationSearchBar)
    public static final int SEARCH_BG_LIGHT = 0xFFFFFFFF;
    public static final int SEARCH_BG_DARK = 0xFF1C1C1D;
    public static final int SEARCH_INPUT_FILL_LIGHT = 0x0F000000; // UIColor(rgb: 0x000000, alpha: 0.06)
    public static final int SEARCH_INPUT_FILL_DARK = 0x1AFFFFFF;  // UIColor(white: 1.0, alpha: 0.10)
    public static final int SEARCH_TEXT_LIGHT = 0xFF000000;
    public static final int SEARCH_TEXT_DARK = 0xFFFFFFFF;
    public static final int SEARCH_PLACEHOLDER_LIGHT = 0xFF8E8E93;
    public static final int SEARCH_PLACEHOLDER_DARK = 0xFF8F8F8F;
    public static final int SEARCH_ICON_LIGHT = 0xFF8E8E93;
    public static final int SEARCH_ICON_DARK = 0xFF8F8F8F;

    // MARK: - Chat List (PresentationThemeChatList)
    public static final int CHAT_LIST_BG_LIGHT = 0xFFFFFFFF;
    public static final int CHAT_LIST_BG_DARK = 0xFF000000;       // Pure OLED black in iOS
    public static final int CHAT_LIST_PINNED_BG_LIGHT = 0xFFF7F7F7;
    public static final int CHAT_LIST_PINNED_BG_DARK = 0xFF1C1C1D;
    public static final int CHAT_LIST_HIGHLIGHT_LIGHT = 0xFFE5E5EA;
    public static final int CHAT_LIST_HIGHLIGHT_DARK = 0xFF121212;
    public static final int CHAT_LIST_SEPARATOR_LIGHT = 0xFFC8C7CC;
    public static final int CHAT_LIST_SEPARATOR_DARK = 0x8C545458;
    public static final int CHAT_LIST_TITLE_LIGHT = 0xFF000000;
    public static final int CHAT_LIST_TITLE_DARK = 0xFFFFFFFF;
    public static final int CHAT_LIST_DATE_LIGHT = 0xFF8E8E93;
    public static final int CHAT_LIST_DATE_DARK = 0xFF8D8E93;
    public static final int CHAT_LIST_MESSAGE_LIGHT = 0xFF8E8E93;
    public static final int CHAT_LIST_MESSAGE_DARK = 0xFF8D8E93;
    public static final int CHAT_LIST_BADGE_ACTIVE_LIGHT = 0xFF007AFF;
    public static final int CHAT_LIST_BADGE_ACTIVE_DARK = 0xFFFFFFFF;
    public static final int CHAT_LIST_BADGE_MUTED_LIGHT = 0xFFB6B6BB;
    public static final int CHAT_LIST_BADGE_MUTED_DARK = 0xFF666666;

    // MARK: - Dynamic Resolvers
    public static int getNavBarBg() {
        return Theme.isCurrentThemeDark() ? NAV_BAR_BG_DARK : NAV_BAR_BG_LIGHT;
    }

    public static int getNavBarSeparator() {
        return Theme.isCurrentThemeDark() ? NAV_BAR_SEPARATOR_DARK : NAV_BAR_SEPARATOR_LIGHT;
    }

    public static int getAccent() {
        return Theme.isCurrentThemeDark() ? ACCENT_BLUE_DARK : ACCENT_BLUE;
    }

    public static int getTabBarBg() {
        return Theme.isCurrentThemeDark() ? TAB_BAR_BG_DARK : TAB_BAR_BG_LIGHT;
    }

    public static int getTabBarSeparator() {
        return Theme.isCurrentThemeDark() ? TAB_BAR_SEPARATOR_DARK : TAB_BAR_SEPARATOR_LIGHT;
    }

    public static int getTabBarIcon(boolean selected) {
        if (Theme.isCurrentThemeDark()) {
            return selected ? TAB_BAR_ICON_SELECTED_DARK : TAB_BAR_ICON_DARK;
        } else {
            return selected ? TAB_BAR_ICON_SELECTED_LIGHT : TAB_BAR_ICON_LIGHT;
        }
    }

    public static int getTabBarText(boolean selected) {
        if (Theme.isCurrentThemeDark()) {
            return selected ? TAB_BAR_ICON_SELECTED_DARK : TAB_BAR_ICON_DARK;
        } else {
            return selected ? TAB_BAR_ICON_SELECTED_LIGHT : TAB_BAR_TEXT_LIGHT;
        }
    }

    public static int getSearchInputFill() {
        return Theme.isCurrentThemeDark() ? SEARCH_INPUT_FILL_DARK : SEARCH_INPUT_FILL_LIGHT;
    }

    public static int getSearchText() {
        return Theme.isCurrentThemeDark() ? SEARCH_TEXT_DARK : SEARCH_TEXT_LIGHT;
    }

    public static int getSearchPlaceholder() {
        return Theme.isCurrentThemeDark() ? SEARCH_PLACEHOLDER_DARK : SEARCH_PLACEHOLDER_LIGHT;
    }

    public static int getChatListBg() {
        return Theme.isCurrentThemeDark() ? CHAT_LIST_BG_DARK : CHAT_LIST_BG_LIGHT;
    }

    public static int getChatListSeparator() {
        return Theme.isCurrentThemeDark() ? CHAT_LIST_SEPARATOR_DARK : CHAT_LIST_SEPARATOR_LIGHT;
    }

    public static int getChatListTitle() {
        return Theme.isCurrentThemeDark() ? CHAT_LIST_TITLE_DARK : CHAT_LIST_TITLE_LIGHT;
    }

    public static int getChatListMessage() {
        return Theme.isCurrentThemeDark() ? CHAT_LIST_MESSAGE_DARK : CHAT_LIST_MESSAGE_LIGHT;
    }
}
