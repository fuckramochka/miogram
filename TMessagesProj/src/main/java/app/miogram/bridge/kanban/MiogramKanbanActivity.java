package app.miogram.bridge.kanban;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;

/**
 * Native Telegram Kanban Board:
 * Organize chats, saved tasks, and messages into 4 structured columns:
 * [0: Inbox / Вхідні] [1: In Progress / В роботі] [2: Important / Важливе] [3: Done / Виконано]
 */
public class MiogramKanbanActivity extends BaseFragment {

    private LinearLayout boardContainer;
    private final List<LinearLayout> columnLayouts = new ArrayList<>();

    private static final String[] COLUMN_TITLES_UK = {"📥 Вхідні", "⏳ В роботі", "🔔 Важливе", "✅ Виконано"};
    private static final String[] COLUMN_TITLES_RU = {"📥 Входящие", "⏳ В работе", "🔔 Важное", "✅ Выполнено"};
    private static final String[] COLUMN_TITLES_EN = {"📥 Inbox", "⏳ In Progress", "🔔 Important", "✅ Done"};

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("Канбан-дошка ໒꒱", "Канбан-доска ໒꒱", "Kanban Board ໒꒱"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showCreateCardDialog();
                }
            }
        });

        // Add Card Action
        actionBar.createMenu().addItem(1, R.drawable.msg_add);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);

        boardContainer = new LinearLayout(context);
        boardContainer.setOrientation(LinearLayout.HORIZONTAL);
        boardContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(16));

        rebuildColumns(context);

        scroll.addView(boardContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = root;
        return fragmentView;
    }

    private void rebuildColumns(Context context) {
        boardContainer.removeAllViews();
        columnLayouts.clear();

        List<MiogramKanbanStorage.KanbanItem> allItems = MiogramKanbanStorage.loadItems();

        for (int col = 0; col < 4; col++) {
            final int colIndex = col;

            LinearLayout colWrapper = new LinearLayout(context);
            colWrapper.setOrientation(LinearLayout.VERTICAL);
            colWrapper.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);

            // Column Header
            LinearLayout colHeader = new LinearLayout(context);
            colHeader.setOrientation(LinearLayout.HORIZONTAL);
            colHeader.setGravity(Gravity.CENTER_VERTICAL);
            colHeader.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));

            TextView title = new TextView(context);
            title.setText(getColumnTitle(colIndex));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            title.setTypeface(AndroidUtilities.bold());
            title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
            colHeader.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            TextView countBadge = new TextView(context);
            countBadge.setText(String.valueOf(countInColPlaceholder(colIndex, allItems)));
            countBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            countBadge.setTypeface(AndroidUtilities.bold());
            countBadge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
            countBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8),
                    Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider())));
            countBadge.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(2), AndroidUtilities.dp(7), AndroidUtilities.dp(2));
            colHeader.addView(countBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

            TextView addBtn = new TextView(context);
            addBtn.setText("+");
            addBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            addBtn.setTypeface(AndroidUtilities.bold());
            addBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()));
            addBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, getResourceProvider()), Theme.RIPPLE_MASK_CIRCLE_20DP));
            addBtn.setContentDescription(MiogramLocale.get("Додати завдання", "Добавить задачу", "Add task"));
            addBtn.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
            addBtn.setOnClickListener(v -> {
                haptic(v);
                showCreateCardDialogForColumn(colIndex);
            });
            colHeader.addView(addBtn);

            colWrapper.addView(colHeader, LayoutHelper.createLinear(AndroidUtilities.dp(240), LayoutHelper.WRAP_CONTENT));

            // Column Items Container inside ScrollView
            ScrollView colScroll = new ScrollView(context);
            colScroll.setVerticalScrollBarEnabled(false);

            LinearLayout cardsContainer = new LinearLayout(context);
            cardsContainer.setOrientation(LinearLayout.VERTICAL);
            cardsContainer.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(12));

            // Populate Cards for this column
            int countInCol = 0;
            for (MiogramKanbanStorage.KanbanItem item : allItems) {
                if (item.column == colIndex) {
                    cardsContainer.addView(createCardView(context, item));
                    countInCol++;
                }
            }

            if (countInCol == 0) {
                TextView empty = new TextView(context);
                empty.setText(MiogramLocale.get("Немає завдань", "Нет задач", "No tasks"));
                empty.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4, getResourceProvider()));
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20));
                cardsContainer.addView(empty);
            }

            colScroll.addView(cardsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            colWrapper.addView(colScroll, LayoutHelper.createLinear(AndroidUtilities.dp(240), LayoutHelper.MATCH_PARENT));

            columnLayouts.add(cardsContainer);
            boardContainer.addView(colWrapper);
        }
    }

    private static int countInColPlaceholder(int colIndex, List<MiogramKanbanStorage.KanbanItem> allItems) {
        int n = 0;
        if (allItems != null) {
            for (MiogramKanbanStorage.KanbanItem it : allItems) {
                if (it != null && it.column == colIndex) n++;
            }
        }
        return n;
    }

    private static void haptic(View v) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignored) {}
    }

    private View createCardView(Context context, MiogramKanbanStorage.KanbanItem item) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setColor(Theme.getColor(Theme.key_dialogBackground, getResourceProvider()));
        bg.setStroke(AndroidUtilities.dp(1), Color.argb(25, 128, 128, 128));
        card.setBackground(bg);

        TextView title = new TextView(context);
        title.setText(item.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        if (!android.text.TextUtils.isEmpty(item.description)) {
            TextView desc = new TextView(context);
            desc.setText(item.description);
            desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
            desc.setMaxLines(3);
            card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        }

        // Action Buttons Row (Move, Open Chat, Delete)
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        if (item.dialogId != 0) {
            TextView openChat = new TextView(context);
            openChat.setText("💬 " + MiogramLocale.get("Чат", "Чат", "Chat"));
            openChat.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            openChat.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()));
            openChat.setBackground(Theme.getSelectorDrawable(false));
            openChat.setContentDescription(MiogramLocale.get("Відкрити чат", "Открыть чат", "Open chat"));
            openChat.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            openChat.setOnClickListener(v -> {
                haptic(v);
                Bundle args = new Bundle();
                if (item.dialogId > 0) {
                    args.putLong("user_id", item.dialogId);
                } else {
                    args.putLong("chat_id", -item.dialogId);
                }
                presentFragment(new ChatActivity(args));
            });
            actions.addView(openChat);
        }

        TextView moveBtn = new TextView(context);
        moveBtn.setText("➡️ " + MiogramLocale.get("Перенести", "Перенести", "Move"));
        moveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        moveBtn.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        moveBtn.setBackground(Theme.getSelectorDrawable(false));
        moveBtn.setContentDescription(MiogramLocale.get("Перенести в іншу колонку", "Перенести в другую колонку", "Move to another column"));
        moveBtn.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        moveBtn.setOnClickListener(v -> {
            haptic(v);
            showMoveDialog(item);
        });
        actions.addView(moveBtn);

        TextView delBtn = new TextView(context);
        delBtn.setText("✕");
        delBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        delBtn.setTextColor(Color.parseColor("#FF2A93"));
        delBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, getResourceProvider()), Theme.RIPPLE_MASK_CIRCLE_20DP));
        delBtn.setContentDescription(MiogramLocale.get("Видалити завдання", "Удалить задачу", "Delete task"));
        delBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        delBtn.setOnClickListener(v -> {
            haptic(v);
            confirmDeleteCard(context, item);
        });
        actions.addView(delBtn);

        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = AndroidUtilities.dp(8);
        card.setLayoutParams(params);

        return card;
    }

    private void confirmDeleteCard(Context context, MiogramKanbanStorage.KanbanItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Видалити завдання?", "Удалить задачу?", "Delete task?"));
        builder.setMessage(item.title != null ? item.title : "");
        builder.setPositiveButton(MiogramLocale.get("Видалити", "Удалить", "Delete"), (d, w) -> {
            MiogramKanbanStorage.deleteItem(item.id);
            rebuildColumns(context);
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        showDialog(builder.create());
    }

    private void showMoveDialog(MiogramKanbanStorage.KanbanItem item) {        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Перенести завдання в колонку:", "Перенести задачу в колонку:", "Move task to column:"));

        String[] options = {
                getColumnTitle(0),
                getColumnTitle(1),
                getColumnTitle(2),
                getColumnTitle(3)
        };

        builder.setItems(options, (dialog, which) -> {
            MiogramKanbanStorage.moveItem(item.id, which);
            rebuildColumns(context);
        });
        showDialog(builder.create());
    }

    private void showCreateCardDialog() {
        showCreateCardDialogForColumn(0);
    }

    private void showCreateCardDialogForColumn(int col) {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Створити завдання ໒꒱", "Создать задачу ໒꒱", "Create Task ໒꒱"));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));

        EditTextBoldCursor titleInput = new EditTextBoldCursor(context);
        titleInput.setHint(MiogramLocale.get("Назва завдання...", "Название задачи...", "Task title..."));
        layout.addView(titleInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        EditTextBoldCursor descInput = new EditTextBoldCursor(context);
        descInput.setHint(MiogramLocale.get("Деталі або опис...", "Детали или описание...", "Details or notes..."));
        layout.addView(descInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton(MiogramLocale.get("Додати", "Добавить", "Add"), (dialog, which) -> {
            String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
            String desc = descInput.getText() != null ? descInput.getText().toString().trim() : "";
            if (!android.text.TextUtils.isEmpty(title)) {
                MiogramKanbanStorage.addItem(title, desc, col, 0, 0);
                rebuildColumns(context);
            }
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        showDialog(builder.create());
    }

    private String getColumnTitle(int col) {
        return MiogramLocale.get(COLUMN_TITLES_UK[col], COLUMN_TITLES_RU[col], COLUMN_TITLES_EN[col]);
    }
}
