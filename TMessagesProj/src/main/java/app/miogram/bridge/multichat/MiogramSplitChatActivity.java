package app.miogram.bridge.multichat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
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
 * Native Miogram Dual Split-Screen Multi-Chat Activity:
 * Real live dual-pane layout running two fully functional INavigationLayout containers.
 * Eliminates redundant outer action bars so both chats look 100% native and integrated.
 */
public class MiogramSplitChatActivity extends BaseFragment {

    private long primaryDialogId;
    private long secondaryDialogId;

    private LinearLayout splitContainer;
    private FrameLayout primaryPane;
    private FrameLayout secondaryPane;
    private FrameLayout dividerBar;

    private INavigationLayout primaryLayout;
    private INavigationLayout secondaryLayout;

    private float splitRatio = 0.5f;
    private boolean isDragging = false;

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
        // Hide redundant outer action bar completely for seamless native OS multi-window look
        actionBar.setAddToContainer(false);
        actionBar.setVisibility(View.GONE);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));

        splitContainer = new LinearLayout(context);
        splitContainer.setOrientation(LinearLayout.VERTICAL);

        // 1. Primary Top Chat Pane
        primaryPane = new FrameLayout(context);
        splitContainer.addView(primaryPane, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, splitRatio));

        // 2. Sleek Draggable Divider Bar (24dp height)
        dividerBar = new FrameLayout(context);
        dividerBar.setBackgroundColor(Theme.getColor(Theme.key_chat_topPanelBackground, getResourceProvider()));

        View handleView = new View(context) {
            private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint borderPaint = new Paint();

            {
                pillPaint.setColor(Theme.getColor(Theme.key_sheet_scrollUp, getResourceProvider()));
                borderPaint.setColor(Theme.getColor(Theme.key_divider, getResourceProvider()));
                borderPaint.setStrokeWidth(AndroidUtilities.dp(1));
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();

                // Subtle top and bottom hairline borders
                canvas.drawLine(0, 0, w, 0, borderPaint);
                canvas.drawLine(0, h, w, h, borderPaint);

                // Centered modern drag pill
                float pillW = AndroidUtilities.dp(38);
                float pillH = AndroidUtilities.dp(4);
                float left = (w - pillW) / 2f;
                float top = (h - pillH) / 2f;
                canvas.drawRoundRect(left, top, left + pillW, top + pillH, AndroidUtilities.dp(2), AndroidUtilities.dp(2), pillPaint);
            }
        };
        dividerBar.addView(handleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Quick Swap / Menu Action Icon at the right
        ImageView swapButton = new ImageView(context);
        swapButton.setImageResource(R.drawable.baseline_swap_horiz_24);
        swapButton.setColorFilter(Theme.getColor(Theme.key_chat_topPanelClose, getResourceProvider()));
        swapButton.setScaleType(ImageView.ScaleType.CENTER);
        swapButton.setRotation(90); // Vertical swap
        swapButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            swapChats();
        });
        swapButton.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showSplitOptionsMenu();
            return true;
        });
        dividerBar.addView(swapButton, LayoutHelper.createFrame(36, 24, Gravity.END | Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                dividerBar.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                updateSplitRatio(0.5f);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                dividerBar.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                showSplitOptionsMenu();
            }
        });

        dividerBar.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
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
            return true;
        });

        splitContainer.addView(dividerBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(24)));

        // 3. Secondary Bottom Chat Pane
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
        // Primary Layout
        primaryLayout = INavigationLayout.newLayout(context, false);
        primaryLayout.setFragmentStack(new ArrayList<>());
        primaryLayout.setDelegate(new INavigationLayout.INavigationLayoutDelegate() {
            @Override
            public boolean needCloseLastFragment(INavigationLayout layout) {
                if (layout.getFragmentStack().size() <= 1) {
                    finishFragment();
                    return false;
                }
                return true;
            }
        });
        primaryPane.addView(primaryLayout.getView(), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        loadPrimaryChat();

        // Secondary Layout
        secondaryLayout = INavigationLayout.newLayout(context, false);
        secondaryLayout.setFragmentStack(new ArrayList<>());
        secondaryLayout.setDelegate(new INavigationLayout.INavigationLayoutDelegate() {
            @Override
            public boolean needCloseLastFragment(INavigationLayout layout) {
                if (layout.getFragmentStack().size() <= 1) {
                    finishFragment();
                    return false;
                }
                return true;
            }
        });
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
                        loadPrimaryChat();
                    }
                }
                return true;
            });
            primaryLayout.presentFragment(new INavigationLayout.NavigationParams(dialogs).setNoAnimation(true));
        }
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
                        loadSecondaryChat();
                    }
                }
                return true;
            });
            secondaryLayout.presentFragment(new INavigationLayout.NavigationParams(dialogs).setNoAnimation(true));
        }
    }

    private void swapChats() {
        long temp = primaryDialogId;
        primaryDialogId = secondaryDialogId;
        secondaryDialogId = temp;
        loadPrimaryChat();
        loadSecondaryChat();
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
                swapChats();
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
    public boolean onBackPressed(boolean invoked) {
        if (secondaryLayout != null && secondaryLayout.getFragmentStack().size() > 1) {
            secondaryLayout.onBackPressed();
            return false;
        }
        if (primaryLayout != null && primaryLayout.getFragmentStack().size() > 1) {
            primaryLayout.onBackPressed();
            return false;
        }
        return super.onBackPressed(invoked);
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
