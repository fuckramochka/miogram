package app.miogram.bridge.multichat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

import app.miogram.bridge.MiogramLocale;

/**
 * Miogram Dual Split-Screen Multi-Chat Activity:
 * Real live dual-pane layout running two fully functional INavigationLayout containers.
 * Allows interacting with, typing, and reading both chats simultaneously.
 */
public class MiogramSplitChatActivity extends BaseFragment {

    private long primaryDialogId;
    private long secondaryDialogId;

    private LinearLayout splitContainer;
    private FrameLayout primaryPane;
    private FrameLayout secondaryPane;
    private View dividerHandle;

    private INavigationLayout primaryLayout;
    private INavigationLayout secondaryLayout;

    private float splitRatio = 0.5f;
    private boolean isDragging = false;
    private float initialTouchY;

    public MiogramSplitChatActivity(long primaryDialogId, long secondaryDialogId) {
        super();
        this.primaryDialogId = primaryDialogId;
        this.secondaryDialogId = secondaryDialogId;
    }

    public MiogramSplitChatActivity(Bundle args) {
        super(args);
        if (args != null) {
            this.primaryDialogId = args.getLong("primary_id", 0);
            this.secondaryDialogId = args.getLong("secondary_id", 0);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("Мультичат ໒꒱", "Мультичат ໒꒱", "Multi-Chat ໒꒱"));
        updateActionBarSubtitle();

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showSplitOptionsMenu();
                }
            }
        });

        actionBar.createMenu().addItem(1, R.drawable.ic_ab_other);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));

        splitContainer = new LinearLayout(context);
        splitContainer.setOrientation(LinearLayout.VERTICAL);

        // 1. Primary Chat Pane
        primaryPane = new FrameLayout(context);
        splitContainer.addView(primaryPane, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, splitRatio));

        // 2. Interactive Draggable Divider Handle
        dividerHandle = new View(context) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                paint.setColor(Theme.getColor(Theme.key_sheet_scrollUp, getResourceProvider()));
            }
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                float pillW = AndroidUtilities.dp(44);
                float pillH = AndroidUtilities.dp(4);
                float left = (w - pillW) / 2f;
                float top = (h - pillH) / 2f;
                canvas.drawRoundRect(left, top, left + pillW, top + pillH, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
            }
        };
        dividerHandle.setBackgroundColor(Theme.getColor(Theme.key_divider, getResourceProvider()));
        dividerHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    initialTouchY = event.getRawY();
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        int totalH = splitContainer.getHeight();
                        if (totalH > 0) {
                            float newRatio = event.getRawY() / (float) totalH;
                            newRatio = Math.max(0.2f, Math.min(0.8f, newRatio));
                            updateSplitRatio(newRatio);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    return true;
            }
            return false;
        });
        splitContainer.addView(dividerHandle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(16)));

        // 3. Secondary Chat Pane
        secondaryPane = new FrameLayout(context);
        splitContainer.addView(secondaryPane, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f - splitRatio));

        root.addView(splitContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Init live navigation layouts
        initNavLayouts(context);

        fragmentView = root;
        return fragmentView;
    }

    private void updateSplitRatio(float ratio) {
        splitRatio = ratio;
        if (primaryPane != null && secondaryPane != null) {
            LinearLayout.LayoutParams p1 = (LinearLayout.LayoutParams) primaryPane.getLayoutParams();
            LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) secondaryPane.getLayoutParams();
            p1.weight = splitRatio;
            p2.weight = 1.0f - splitRatio;
            splitContainer.requestLayout();
        }
    }

    private void initNavLayouts(Context context) {
        // Primary
        primaryLayout = INavigationLayout.newLayout(context, false);
        primaryLayout.setFragmentStack(new ArrayList<>());
        primaryPane.addView(primaryLayout.getView(), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        loadPrimaryChat();

        // Secondary
        secondaryLayout = INavigationLayout.newLayout(context, false);
        secondaryLayout.setFragmentStack(new ArrayList<>());
        secondaryPane.addView(secondaryLayout.getView(), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        loadSecondaryChat();
    }

    private void loadPrimaryChat() {
        if (primaryLayout == null) return;
        if (primaryDialogId != 0) {
            Bundle args = new Bundle();
            if (primaryDialogId > 0) {
                args.putLong("user_id", primaryDialogId);
            } else {
                args.putLong("chat_id", -primaryDialogId);
            }
            ChatActivity chat = new ChatActivity(args);
            primaryLayout.presentFragment(new INavigationLayout.NavigationParams(chat).setNoAnimation(true));
        } else {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putBoolean("checkCanWrite", false);
            DialogsActivity dialogs = new DialogsActivity(args);
            dialogs.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                if (dids != null && !dids.isEmpty()) {
                    org.telegram.messenger.MessagesStorage.TopicKey key = dids.get(0);
                    if (key != null && key.dialogId != 0) {
                        primaryDialogId = key.dialogId;
                        updateActionBarSubtitle();
                        loadPrimaryChat();
                    }
                }
                return true;
            });
            primaryLayout.presentFragment(new INavigationLayout.NavigationParams(dialogs).setNoAnimation(true));
        }
        updateActionBarSubtitle();
    }

    private void loadSecondaryChat() {
        if (secondaryLayout == null) return;
        if (secondaryDialogId != 0) {
            Bundle args = new Bundle();
            if (secondaryDialogId > 0) {
                args.putLong("user_id", secondaryDialogId);
            } else {
                args.putLong("chat_id", -secondaryDialogId);
            }
            ChatActivity chat = new ChatActivity(args);
            secondaryLayout.presentFragment(new INavigationLayout.NavigationParams(chat).setNoAnimation(true));
        } else {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putBoolean("checkCanWrite", false);
            DialogsActivity dialogs = new DialogsActivity(args);
            dialogs.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                if (dids != null && !dids.isEmpty()) {
                    org.telegram.messenger.MessagesStorage.TopicKey key = dids.get(0);
                    if (key != null && key.dialogId != 0) {
                        secondaryDialogId = key.dialogId;
                        updateActionBarSubtitle();
                        loadSecondaryChat();
                    }
                }
                return true;
            });
            secondaryLayout.presentFragment(new INavigationLayout.NavigationParams(dialogs).setNoAnimation(true));
        }
        updateActionBarSubtitle();
    }

    private void updateActionBarSubtitle() {
        if (actionBar == null) return;
        String t1 = getDialogTitle(primaryDialogId);
        String t2 = getDialogTitle(secondaryDialogId);
        if (t1 != null && t2 != null) {
            actionBar.setSubtitle(t1 + "  ⬌  " + t2);
        } else if (t1 != null) {
            actionBar.setSubtitle(t1 + "  •  " + MiogramLocale.get("Оберіть другий чат", "Выберите второй чат", "Select second chat"));
        } else {
            actionBar.setSubtitle(MiogramLocale.get("Оберіть діалоги для спліту", "Выберите диалоги для сплита", "Select dialogs for split"));
        }
    }

    private String getDialogTitle(long dialogId) {
        if (dialogId == 0) return null;
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                return UserObject.getUserName(user);
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                return chat.title;
            }
        }
        return "ID " + dialogId;
    }

    private void showSplitOptionsMenu() {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Мультичат ໒꒱", "Мультичат ໒꒱", "Multi-Chat ໒꒱"));

        String[] options = {
                MiogramLocale.get("Поміняти чати місцями (Swap) ⇅", "Поменять чаты местами (Swap) ⇅", "Swap Chats ⇅"),
                MiogramLocale.get("Змінити верхній чат", "Сменить верхний чат", "Change Top Chat"),
                MiogramLocale.get("Змінити нижній чат", "Сменить нижний чат", "Change Bottom Chat"),
                MiogramLocale.get("Скинути пропорції (50 / 50)", "Сбросить пропорции (50 / 50)", "Reset to 50 / 50"),
                MiogramLocale.get("Згорнути в плаваюче вікно (PIP)", "Свернуть в плавающее окно (PIP)", "Minimize to Floating Window (PIP)")
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                long temp = primaryDialogId;
                primaryDialogId = secondaryDialogId;
                secondaryDialogId = temp;
                loadPrimaryChat();
                loadSecondaryChat();
            } else if (which == 1) {
                primaryDialogId = 0;
                loadPrimaryChat();
            } else if (which == 2) {
                secondaryDialogId = 0;
                loadSecondaryChat();
            } else if (which == 3) {
                updateSplitRatio(0.5f);
            } else if (which == 4) {
                long targetId = secondaryDialogId != 0 ? secondaryDialogId : primaryDialogId;
                if (targetId != 0) {
                    MiogramFloatingChatService.startFloatingChat(context, targetId);
                }
            }
        });
        showDialog(builder.create());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (primaryLayout != null) primaryLayout.onPause();
        if (secondaryLayout != null) secondaryLayout.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (primaryLayout != null) primaryLayout.onResume();
        if (secondaryLayout != null) secondaryLayout.onResume();
    }

    @Override
    public boolean onBackPressed() {
        if (secondaryLayout != null && secondaryLayout.getFragmentStack().size() > 1) {
            secondaryLayout.onBackPressed();
            return false;
        }
        if (primaryLayout != null && primaryLayout.getFragmentStack().size() > 1) {
            primaryLayout.onBackPressed();
            return false;
        }
        return super.onBackPressed();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (primaryLayout != null) {
            primaryLayout.removeAllFragments();
        }
        if (secondaryLayout != null) {
            secondaryLayout.removeAllFragments();
        }
    }
}
