package app.miogram.bridge.folders;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;

/**
 * Modern, sleek horizontal subfolder bar for DialogsActivity.
 * Features frosted squircle pill chips for parent/child subfolders,
 * smart type categories (Personal, Groups, Channels, Bots, Unread),
 * animated badges, and 1-tap subfolder creation.
 */
public class MiogramSubfolderBar extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final DialogsActivity dialogsActivity;
    private final HorizontalScrollView scrollView;
    private final LinearLayout pillsContainer;

    private int currentParentTabId = 0;
    private final ArrayList<PillView> pillViews = new ArrayList<>();
    private final android.graphics.Paint dividerPaint = new android.graphics.Paint();

    public MiogramSubfolderBar(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, DialogsActivity dialogsActivity) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.dialogsActivity = dialogsActivity;

        setBackgroundColor(getColor(Theme.key_windowBackgroundWhite));

        scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(3), AndroidUtilities.dp(12), AndroidUtilities.dp(3));
        addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        pillsContainer = new LinearLayout(context);
        pillsContainer.setOrientation(LinearLayout.HORIZONTAL);
        pillsContainer.setGravity(Gravity.CENTER_VERTICAL);
        scrollView.addView(pillsContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        refreshPills();
    }

    @Override
    protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        dividerPaint.setColor(getColor(Theme.key_divider));
        dividerPaint.setStrokeWidth(AndroidUtilities.dp(1));
        canvas.drawLine(0, getHeight() - AndroidUtilities.dp(1), getWidth(), getHeight() - AndroidUtilities.dp(1), dividerPaint);
    }

    private int getColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    public void onParentTabChanged(int parentTabId) {
        currentParentTabId = parentTabId;
        int currentAccount = dialogsActivity != null ? dialogsActivity.getCurrentAccount() : 0;
        MiogramSubfolderEngine.setActiveParentTabId(currentAccount, parentTabId);
        MiogramSubfolderEngine.resetActiveSubfolder(currentAccount);
        refreshPills();
        scrollView.scrollTo(0, 0);
    }

    public void onTabsUpdated() {
        refreshPills();
    }

    public void updateCounters() {
        if (dialogsActivity == null) return;
        int currentAccount = dialogsActivity.getCurrentAccount();
        MessagesController mc = dialogsActivity.getMessagesController();
        if (mc == null) return;

        ArrayList<TLRPC.Dialog> baseList = dialogsActivity.getDialogsArray(currentAccount, 0, 0, false);

        for (int i = 0; i < pillViews.size(); i++) {
            PillView pv = pillViews.get(i);
            int count = 0;
            if (pv.childFilterId > 0) {
                MessagesController.DialogFilter cf = mc.dialogFiltersById.get(pv.childFilterId);
                if (cf != null) {
                    count = cf.unreadCount;
                }
            } else if (pv.subfolderType == MiogramSubfolderEngine.TYPE_ALL) {
                count = MiogramSubfolderEngine.calculateUnreadCount(currentAccount, baseList, MiogramSubfolderEngine.TYPE_ALL);
            } else {
                count = MiogramSubfolderEngine.calculateUnreadCount(currentAccount, baseList, pv.subfolderType);
            }
            pv.setBadgeCount(count);
        }
    }

    public void refreshPills() {
        pillsContainer.removeAllViews();
        pillViews.clear();

        if (dialogsActivity == null) return;
        int currentAccount = dialogsActivity.getCurrentAccount();
        MessagesController mc = dialogsActivity.getMessagesController();
        if (mc == null) return;

        ArrayList<MessagesController.DialogFilter> filters = mc.getDialogFilters();
        MessagesController.DialogFilter parentFilter = null;
        if (currentParentTabId >= 0 && currentParentTabId < filters.size()) {
            parentFilter = filters.get(currentParentTabId);
        }

        ArrayList<MessagesController.DialogFilter> childFilters = null;
        if (parentFilter != null) {
            childFilters = MiogramSubfolderEngine.getChildFiltersForParent(currentAccount, parentFilter);
        }
        boolean hasChildFilters = childFilters != null && !childFilters.isEmpty();
        boolean showSmart = MiogramSubfolderEngine.isSmartFiltersEnabled();

        if (!hasChildFilters && !showSmart) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        int activeChildId = MiogramSubfolderEngine.getActiveChildFilterId(currentAccount);
        int activeType = MiogramSubfolderEngine.getActiveSubfolderType(currentAccount);

        // 1. "Усі" pill
        PillView allPill = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_ALL,
                LocaleController.getString(R.string.FilterAllChats), 0);
        allPill.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_ALL);
        pillViews.add(allPill);
        pillsContainer.addView(allPill);

        // 2. Child filters for this parent
        if (hasChildFilters) {
            for (int i = 0; i < childFilters.size(); i++) {
                MessagesController.DialogFilter cf = childFilters.get(i);
                String childTitle = MiogramSubfolderEngine.getChildName(cf.name);
                PillView cp = new PillView(getContext(), cf.id, MiogramSubfolderEngine.TYPE_ALL, childTitle, R.drawable.msg_folders);
                cp.setSelectedState(activeChildId == cf.id);
                cp.setDialogFilter(cf);
                pillViews.add(cp);
                pillsContainer.addView(cp);
            }
        }

        // 3. Smart categories (if enabled)
        if (showSmart) {
            PillView pPersonal = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_PERSONAL, "Особисті", R.drawable.msg_contact);
            pPersonal.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_PERSONAL);
            pillViews.add(pPersonal);
            pillsContainer.addView(pPersonal);

            PillView pGroups = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_GROUPS, "Групи", R.drawable.msg_groups);
            pGroups.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_GROUPS);
            pillViews.add(pGroups);
            pillsContainer.addView(pGroups);

            PillView pChannels = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_CHANNELS, "Канали", R.drawable.msg_channel);
            pChannels.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_CHANNELS);
            pillViews.add(pChannels);
            pillsContainer.addView(pChannels);

            PillView pBots = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_BOTS, "Боти", R.drawable.msg_bot);
            pBots.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_BOTS);
            pillViews.add(pBots);
            pillsContainer.addView(pBots);

            PillView pUnread = new PillView(getContext(), 0, MiogramSubfolderEngine.TYPE_UNREAD, "Непрочитані", R.drawable.msg_markunread);
            pUnread.setSelectedState(activeChildId == 0 && activeType == MiogramSubfolderEngine.TYPE_UNREAD);
            pillViews.add(pUnread);
            pillsContainer.addView(pUnread);
        }

        // 4. "+" Pill to create a new subfolder
        AddPillView addPill = new AddPillView(getContext(), () -> {
            int curAcc = dialogsActivity.getCurrentAccount();
            MessagesController m = dialogsActivity.getMessagesController();
            MessagesController.DialogFilter pf = null;
            if (m != null && currentParentTabId >= 0 && currentParentTabId < m.getDialogFilters().size()) {
                pf = m.getDialogFilters().get(currentParentTabId);
            }
            MiogramSubfolderEngine.showCreateSubfolderDialog(dialogsActivity, curAcc, pf, this::refreshPills);
        });
        pillsContainer.addView(addPill);

        updateCounters();
    }

    private void onPillClicked(PillView pill) {
        if (dialogsActivity == null) return;
        int currentAccount = dialogsActivity.getCurrentAccount();

        boolean alreadyActive = (pill.childFilterId == MiogramSubfolderEngine.getActiveChildFilterId(currentAccount)
                && pill.subfolderType == MiogramSubfolderEngine.getActiveSubfolderType(currentAccount));

        if (alreadyActive) {
            dialogsActivity.onSubfolderChanged();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
            }
        } catch (Exception ignore) {}

        MiogramSubfolderEngine.setActiveChildFilterId(currentAccount, pill.childFilterId);
        MiogramSubfolderEngine.setActiveSubfolderType(currentAccount, pill.subfolderType);

        for (int i = 0; i < pillViews.size(); i++) {
            PillView pv = pillViews.get(i);
            pv.setSelectedState(pv == pill);
        }

        dialogsActivity.onSubfolderChanged();
    }

    private void onPillLongClicked(PillView pill) {
        if (pill.dialogFilter == null || dialogsActivity == null) return;
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(MiogramSubfolderEngine.getChildName(pill.dialogFilter.name));

        CharSequence[] items = new CharSequence[]{"Редагувати підпапку", "Видалити підпапку"};
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                dialogsActivity.presentFragment(new FilterCreateActivity(pill.dialogFilter));
            } else if (which == 1) {
                AlertDialog.Builder delBuilder = new AlertDialog.Builder(context);
                delBuilder.setTitle("Видалити підпапку?");
                delBuilder.setMessage("Ця дія видалить підпапку '" + MiogramSubfolderEngine.getChildName(pill.dialogFilter.name) + "' з хмари Telegram. Чати залишаться недоторканими.");
                delBuilder.setPositiveButton("Видалити", (d, w) -> {
                    int currentAccount = dialogsActivity.getCurrentAccount();
                    MiogramSubfolderEngine.deleteSubfolder(dialogsActivity, currentAccount, pill.dialogFilter, () -> {
                        if (MiogramSubfolderEngine.getActiveChildFilterId(currentAccount) == pill.dialogFilter.id) {
                            MiogramSubfolderEngine.resetActiveSubfolder(currentAccount);
                            dialogsActivity.onSubfolderChanged();
                        }
                        refreshPills();
                    });
                });
                delBuilder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                AlertDialog alert = delBuilder.create();
                alert.show();
                TextView btn = (TextView) alert.getButton(AlertDialog.BUTTON_POSITIVE);
                if (btn != null) {
                    btn.setTextColor(getColor(Theme.key_text_RedBold));
                }
            }
        });
        builder.show();
    }

    /**
     * Sleek pill view for subfolder/category.
     */
    private class PillView extends LinearLayout {

        final int childFilterId;
        final int subfolderType;
        MessagesController.DialogFilter dialogFilter;

        private final ImageView iconView;
        private final TextView titleView;
        private final TextView badgeView;
        private boolean isSelected;

        public PillView(Context context, int childFilterId, int subfolderType, String title, int iconRes) {
            super(context);
            this.childFilterId = childFilterId;
            this.subfolderType = subfolderType;

            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(30));
            lp.rightMargin = AndroidUtilities.dp(6);
            setLayoutParams(lp);

            if (iconRes != 0) {
                iconView = new ImageView(context);
                iconView.setImageResource(iconRes);
                iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(14), AndroidUtilities.dp(14));
                iconLp.rightMargin = AndroidUtilities.dp(5);
                addView(iconView, iconLp);
            } else {
                iconView = null;
            }

            titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextSize(12.5f);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            badgeView = new TextView(context);
            badgeView.setTextSize(10);
            badgeView.setTypeface(AndroidUtilities.bold());
            badgeView.setGravity(Gravity.CENTER);
            badgeView.setVisibility(GONE);
            badgeView.setPadding(AndroidUtilities.dp(4.5f), 0, AndroidUtilities.dp(4.5f), 0);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(16));
            badgeLp.leftMargin = AndroidUtilities.dp(4);
            addView(badgeView, badgeLp);

            setOnClickListener(v -> onPillClicked(this));

            setOnLongClickListener(v -> {
                if (dialogFilter != null) {
                    onPillLongClicked(this);
                    return true;
                }
                return false;
            });
        }

        public void setDialogFilter(MessagesController.DialogFilter filter) {
            this.dialogFilter = filter;
        }

        public void setSelectedState(boolean selected) {
            this.isSelected = selected;
            int accentColor = getColor(Theme.key_chats_actionBackground);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(15));

            if (selected) {
                bg.setColor(accentColor);
                bg.setStroke(0, 0);
                setBackground(bg);
                titleView.setTextColor(Color.WHITE);
                if (iconView != null) {
                    iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                }
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(AndroidUtilities.dp(8));
                badgeBg.setColor(0x40FFFFFF);
                badgeView.setBackground(badgeBg);
                badgeView.setTextColor(Color.WHITE);
            } else {
                int inactiveBgColor = getColor(Theme.key_windowBackgroundGray);
                int textColor = getColor(Theme.key_windowBackgroundWhiteBlackText);
                int iconColor = getColor(Theme.key_windowBackgroundWhiteGrayIcon);

                bg.setColor(inactiveBgColor);
                bg.setStroke(0, 0);
                setBackground(bg);

                titleView.setTextColor(textColor);
                if (iconView != null) {
                    iconView.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
                }
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(AndroidUtilities.dp(8));
                badgeBg.setColor(accentColor);
                badgeView.setBackground(badgeBg);
                badgeView.setTextColor(Color.WHITE);
            }
        }

        public void setBadgeCount(int count) {
            if (count > 0 && MiogramSubfolderEngine.isShowCountersEnabled()) {
                badgeView.setText(count > 999 ? "999+" : String.valueOf(count));
                badgeView.setVisibility(VISIBLE);
            } else {
                badgeView.setVisibility(GONE);
            }
        }
    }

    /**
     * Compact rounded "+" button pill at the end of the list.
     */
    private class AddPillView extends FrameLayout {

        public AddPillView(Context context, Runnable onClick) {
            super(context);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(AndroidUtilities.dp(30), AndroidUtilities.dp(30));
            lp.rightMargin = AndroidUtilities.dp(8);
            setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(15));
            bg.setColor(getColor(Theme.key_windowBackgroundGray));
            bg.setStroke(0, 0);
            setBackground(bg);

            ImageView addIcon = new ImageView(context);
            addIcon.setImageResource(R.drawable.msg_add);
            addIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            addIcon.setColorFilter(new PorterDuffColorFilter(getColor(Theme.key_chats_actionBackground), PorterDuff.Mode.SRC_IN));
            addView(addIcon, LayoutHelper.createFrame(14, 14, Gravity.CENTER));

            setOnClickListener(v -> {
                try {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                } catch (Exception ignore) {}
                if (onClick != null) {
                    onClick.run();
                }
            });
        }
    }
}
