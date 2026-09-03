package tw.nekomimi.nekogram.helpers;

import app.exteraless.appearance.AppearanceConfig;

import org.telegram.ui.MainTabsActivity;

import xyz.nextalone.nagram.NaConfig;

public final class MainTabsHelper {
    public static final int MAIN_TABS_HEIGHT = 56;
    public static final int MAIN_TABS_MARGIN = 8;
    public static final int MAIN_TABS_MARGIN_COMPACT = 4;
    public static final int FILTER_TABS_HEIGHT = 36;
    public static final int TAB_WIDTH = 80;
    public static final int TAB_PADDING = 4;

    private MainTabsHelper() {
    }

    public static boolean isMainTabsHideTitleStyle() {
        return NaConfig.INSTANCE.getMainTabsHideTitles().Bool();
    }

    public static int getMainTabsHeight() {
        if (app.miogram.bridge.ui.ios.MiogramIosLayout.isIosPresetActive(null)) {
            return 50;
        }
        return isMainTabsHideTitleStyle() ? FILTER_TABS_HEIGHT : MAIN_TABS_HEIGHT;
    }

    public static int getMainTabsMargin() {
        if (app.miogram.bridge.ui.ios.MiogramIosLayout.isIosPresetActive(null)) {
            return 0;
        }
        return isMainTabsHideTitleStyle() ? MAIN_TABS_MARGIN_COMPACT : MAIN_TABS_MARGIN;
    }

    public static int getMainTabsHeightWithMargins() {
        return getMainTabsHeight() + getMainTabsMargin() * 2;
    }

    public static boolean isContactsTabHidden() {
        return NaConfig.INSTANCE.getMainTabsHideContacts().Bool();
    }

    public static boolean isFeedTabShown() {
        return AppearanceConfig.showFeedTab();
    }

    public static boolean hasContactsOrFeedTab() {
        return !isContactsTabHidden() || isFeedTabShown();
    }

    public static int getChatsPosition() {
        return 0;
    }

    public static int getContactsPosition() {
        return 1;
    }

    public static int getCallsOrSettingsPosition() {
        return hasContactsOrFeedTab() ? 2 : 1;
    }

    public static int getProfilePosition() {
        return hasContactsOrFeedTab() ? 3 : 2;
    }

    public static int getFragmentsCount() {
        return hasContactsOrFeedTab() ? MainTabsActivity.TABS_COUNT : MainTabsActivity.TABS_COUNT - 1;
    }

    public static int getTabsViewWidth() {
        return TAB_WIDTH * getFragmentsCount() + (getMainTabsMargin() + TAB_PADDING) * 2;
    }
}
