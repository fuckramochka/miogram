package app.miogram.bridge.multichat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import app.miogram.bridge.MiogramLocale;

/**
 * Miogram Dual Split-Screen Multi-Chat Activity:
 * Allows opening two active chats simultaneously (Top/Bottom on portrait, Left/Right on landscape)
 * with an interactive draggable divider to resize panes dynamically.
 */
public class MiogramSplitChatActivity extends BaseFragment {

    private long primaryDialogId;
    private long secondaryDialogId;

    private LinearLayout splitContainer;
    private FrameLayout primaryPane;
    private FrameLayout secondaryPane;
    private View dividerHandle;

    private float splitRatio = 0.5f; // 50/50 default
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
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("Мультичат (Спліт-екран) ໒꒱", "Мультичат (Сплит-экран) ໒꒱", "Multi-Chat Split Screen ໒꒱"));
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

        // Top Right Menu: Change Secondary Chat or Floating Window
        actionBar.createMenu().addItem(1, R.drawable.msg_fave);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));

        splitContainer = new LinearLayout(context);
        splitContainer.setOrientation(LinearLayout.VERTICAL);

        // 1. Primary Chat Pane (Top)
        primaryPane = new FrameLayout(context);
        primaryPane.setId(View.generateViewId());
        setupPanePlaceholder(primaryPane, primaryDialogId, true);
        splitContainer.addView(primaryPane, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, splitRatio));

        // 2. Draggable Divider Handle
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
                // Draw pill handle in center
                float pillW = AndroidUtilities.dp(36);
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

        // 3. Secondary Chat Pane (Bottom)
        secondaryPane = new FrameLayout(context);
        secondaryPane.setId(View.generateViewId());
        setupPanePlaceholder(secondaryPane, secondaryDialogId, false);
        splitContainer.addView(secondaryPane, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f - splitRatio));

        root.addView(splitContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

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

    private void setupPanePlaceholder(FrameLayout pane, long dialogId, boolean isPrimary) {
        Context context = getParentActivity();
        if (context == null) context = ApplicationLoader.applicationContext;

        pane.removeAllViews();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        title.setGravity(Gravity.CENTER);

        String chatName = getDialogTitle(dialogId);
        title.setText(chatName != null ? chatName : (isPrimary ? "Чат 1 (Основний)" : "Чат 2 (Оберіть діалог)"));
        layout.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView desc = new TextView(context);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        desc.setGravity(Gravity.CENTER);
        desc.setText(dialogId != 0
                ? MiogramLocale.get("Спліт-вікно готове. Натисніть, щоб розгорнути повний чат або відкрити в плаваючому баблі ໒꒱", "Сплит-окно готово. Нажмите, чтобы развернуть полный чат или открыть в плавающем бабле ໒꒱", "Split pane ready. Tap to expand or open in floating bubble ໒꒱")
                : MiogramLocale.get("Натисніть для вибору другого чату зі списку", "Нажмите для выбора второго чата из списка", "Tap to select second chat from list"));
        layout.addView(desc, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        TextView actionBtn = new TextView(context);
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        actionBtn.setTypeface(AndroidUtilities.bold());
        actionBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, getResourceProvider()));
        actionBtn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()));
        actionBtn.setBackground(bg);
        actionBtn.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
        actionBtn.setText(dialogId != 0 ? MiogramLocale.get("Відкрити чат", "Открыть чат", "Open Chat") : MiogramLocale.get("Обрати діалог", "Выбрать диалог", "Choose Dialog"));

        actionBtn.setOnClickListener(v -> {
            if (dialogId != 0) {
                Bundle args = new Bundle();
                if (dialogId > 0) {
                    args.putLong("user_id", dialogId);
                } else {
                    args.putLong("chat_id", -dialogId);
                }
                presentFragment(new ChatActivity(args));
            } else {
                // Open DialogsActivity to pick secondary chat
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putBoolean("checkCanWrite", false);
                DialogsActivity fragment = new DialogsActivity(args);
                fragment.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                    if (dids != null && !dids.isEmpty()) {
                        org.telegram.messenger.MessagesStorage.TopicKey topicKey = dids.get(0);
                        if (topicKey != null && topicKey.dialogId != 0) {
                            secondaryDialogId = topicKey.dialogId;
                            setupPanePlaceholder(secondaryPane, secondaryDialogId, false);
                        }
                    }
                    f.finishFragment();
                    return true;
                });
                presentFragment(fragment);
            }
        });
        layout.addView(actionBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        pane.addView(layout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private String getDialogTitle(long dialogId) {
        if (dialogId == 0) return null;
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                return org.telegram.messenger.UserObject.getUserName(user);
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

        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Опції мультичату ໒꒱", "Опции мультичата ໒꒱", "Multi-Chat Options ໒꒱"));

        String[] options = {
                MiogramLocale.get("Згорнути в плаваюче вікно (PIP)", "Свернуть в плавающее окно (PIP)", "Minimize to Floating Window (PIP)"),
                MiogramLocale.get("Змінити Чат 1 (Верхній)", "Сменить Чат 1 (Верхний)", "Change Chat 1 (Top)"),
                MiogramLocale.get("Змінити Чат 2 (Нижній)", "Сменить Чат 2 (Нижний)", "Change Chat 2 (Bottom)"),
                MiogramLocale.get("Поміняти чати місцями (Swap)", "Поменять чаты местами (Swap)", "Swap Chats")
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                long targetId = primaryDialogId != 0 ? primaryDialogId : secondaryDialogId;
                if (targetId != 0) {
                    MiogramFloatingChatService.startFloatingChat(context, targetId);
                }
            } else if (which == 1) {
                pickChatForPane(true);
            } else if (which == 2) {
                pickChatForPane(false);
            } else if (which == 3) {
                long temp = primaryDialogId;
                primaryDialogId = secondaryDialogId;
                secondaryDialogId = temp;
                setupPanePlaceholder(primaryPane, primaryDialogId, true);
                setupPanePlaceholder(secondaryPane, secondaryDialogId, false);
            }
        });
        showDialog(builder.create());
    }

    private void pickChatForPane(boolean isPrimary) {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null && !dids.isEmpty()) {
                org.telegram.messenger.MessagesStorage.TopicKey topicKey = dids.get(0);
                long selectedId = topicKey != null ? topicKey.dialogId : 0;
                if (selectedId != 0) {
                    if (isPrimary) {
                        primaryDialogId = selectedId;
                        setupPanePlaceholder(primaryPane, primaryDialogId, true);
                    } else {
                        secondaryDialogId = selectedId;
                        setupPanePlaceholder(secondaryPane, secondaryDialogId, false);
                    }
                }
            }
            f.finishFragment();
            return true;
        });
        presentFragment(fragment);
    }
}
