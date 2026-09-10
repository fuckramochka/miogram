package app.miogram.bridge.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * In-chat AI assistant: writes message drafts and searches this chat.
 *
 * <p>Opened from the chat header menu (Miogram AI item). Two modes:
 * <ul>
 *   <li><b>Write</b> — a brief becomes a ready message (Copy / Insert into
 *       the input field via the host callback).</li>
 *   <li><b>Search</b> — server-side {@code messages.search} scoped to this
 *       peer; tapping a hit jumps straight to it.</li>
 * </ul>
 */
public class MiogramChatAiSheet extends BottomSheet {

    private final BaseFragment parent;
    private final long dialogId;
    private final int account;
    private final Utilities.Callback<String> onInsert;

    private TextView writeTab;
    private TextView searchTab;
    private EditTextBoldCursor input;
    private TextView actionBtn;
    private ProgressBar progress;
    private TextView statusView;
    private TextView resultView;
    private LinearLayout resultActions;
    private TextView copyBtn;
    private TextView insertBtn;
    private LinearLayout hitsList;

    private boolean writeMode = true;
    private String lastDraft;

    public MiogramChatAiSheet(BaseFragment parent, long dialogId, Utilities.Callback<String> onInsert) {
        super(parent.getParentActivity(), false, parent.getResourceProvider());
        this.parent = parent;
        this.dialogId = dialogId;
        this.account = parent.getCurrentAccount();
        this.onInsert = onInsert;
        setApplyBottomPadding(false);
        initUi(parent.getParentActivity() != null ? parent.getParentActivity() : ApplicationLoader.applicationContext);
    }

    private void initUi(Context ctx) {
        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(20));

        View handle = new View(ctx);
        handle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0x44888888));
        content.addView(handle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        TextView title = new TextView(ctx);
        title.setText(MiogramLocale.get("Miogram AI в чаті", "Miogram AI в чате", "Miogram AI in chat"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setGravity(Gravity.CENTER);
        content.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        LinearLayout tabs = new LinearLayout(ctx);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), 0x14000000));
        tabs.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        writeTab = makeTab(ctx, MiogramLocale.get("Написати", "Написать", "Write"));
        writeTab.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            setMode(true);
        });
        tabs.addView(writeTab, LayoutHelper.createLinear(0, 32, 1.0f));
        searchTab = makeTab(ctx, MiogramLocale.get("Пошук", "Поиск", "Search"));
        searchTab.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            setMode(false);
        });
        tabs.addView(searchTab, LayoutHelper.createLinear(0, 32, 1.0f));
        content.addView(tabs, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        refreshTabs();

        input = new EditTextBoldCursor(ctx);
        input.setHint(MiogramLocale.get("Коротко: про що написати…", "Коротко: о чем написать…", "Brief: what to write about…"));
        input.setMinLines(2);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        input.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        content.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        actionBtn = new TextView(ctx);
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        actionBtn.setTypeface(AndroidUtilities.bold());
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setTextColor(0xFFFFFFFF);
        actionBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        actionBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        actionBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onAction();
        });
        content.addView(actionBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        refreshAction();

        progress = new ProgressBar(ctx);
        progress.setVisibility(View.GONE);
        content.addView(progress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        statusView = new TextView(ctx);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        content.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout scrollInner = new LinearLayout(ctx);
        scrollInner.setOrientation(LinearLayout.VERTICAL);

        resultView = new TextView(ctx);
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        resultView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        resultView.setVisibility(View.GONE);
        scrollInner.addView(resultView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        resultActions = new LinearLayout(ctx);
        resultActions.setOrientation(LinearLayout.HORIZONTAL);
        resultActions.setVisibility(View.GONE);
        copyBtn = makeSmallButton(ctx, MiogramLocale.get("Копіювати", "Копировать", "Copy"));
        copyBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            copyDraft();
        });
        resultActions.addView(copyBtn, LayoutHelper.createLinear(0, 42, 1.0f, 0, 0, 6, 0));
        insertBtn = makeSmallButton(ctx, MiogramLocale.get("Вставити", "Вставить", "Insert"));
        insertBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            insertDraft();
        });
        resultActions.addView(insertBtn, LayoutHelper.createLinear(0, 42, 1.0f, 6, 0, 0, 0));
        scrollInner.addView(resultActions);

        hitsList = new LinearLayout(ctx);
        hitsList.setOrientation(LinearLayout.VERTICAL);
        scrollInner.addView(hitsList);

        scroll.addView(scrollInner);
        content.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(220)));

        root.addView(content);
        setCustomView(root);
    }

    private TextView makeTab(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTypeface(AndroidUtilities.bold());
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private TextView makeSmallButton(Context ctx, String text) {
        TextView b = new TextView(ctx);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(AndroidUtilities.bold());
        b.setGravity(Gravity.CENTER);
        b.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        b.setBackground(Theme.getSelectorDrawable(false));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void refreshTabs() {
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        writeTab.setTextColor(writeMode ? 0xFFFFFFFF : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        writeTab.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), writeMode ? accent : 0x00000000));
        searchTab.setTextColor(!writeMode ? 0xFFFFFFFF : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchTab.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), !writeMode ? accent : 0x00000000));
    }

    private void refreshAction() {
        actionBtn.setText(writeMode
                ? MiogramLocale.get("Згенерувати", "Сгенерировать", "Generate")
                : MiogramLocale.get("Знайти", "Найти", "Find"));
    }

    private void setMode(boolean write) {
        writeMode = write;
        refreshTabs();
        refreshAction();
        statusView.setText("");
        resultView.setVisibility(View.GONE);
        resultActions.setVisibility(View.GONE);
        hitsList.removeAllViews();
        input.setHint(write
                ? MiogramLocale.get("Коротко: про що написати…", "Коротко: о чем написать…", "Brief: what to write about…")
                : MiogramLocale.get("Слово або фраза для пошуку…", "Слово или фраза для поиска…", "Word or phrase to find…"));
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        actionBtn.setAlpha(busy ? 0.5f : 1f);
        actionBtn.setClickable(!busy);
    }

    private void onAction() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) return;
        if (writeMode) onWrite(q);
        else onSearch(q);
    }

    // ---- Write mode ----

    private void onWrite(String brief) {
        if (!MiogramAiService.hasApiKey()) {
            statusView.setText(MiogramLocale.get("Додайте API-ключ у Miogram AI.", "Добавьте API-ключ в Miogram AI.", "Add an API key in Miogram AI."));
            return;
        }
        setBusy(true);
        statusView.setText("");
        String prompt = "Write a short ready-to-send chat message from this brief. "
                + "Same language as the brief. No quotes, no preamble, message only:\n\n" + brief;
        MiogramAiService.generateText(prompt, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            setBusy(false);
            if (res != null && !res.trim().isEmpty()) {
                lastDraft = res.trim();
                resultView.setText(lastDraft);
                resultView.setVisibility(View.VISIBLE);
                resultActions.setVisibility(View.VISIBLE);
                insertBtn.setVisibility(onInsert != null ? View.VISIBLE : View.GONE);
                hitsList.removeAllViews();
            } else {
                statusView.setText(err != null ? err : MiogramLocale.get("Порожня відповідь", "Пустой ответ", "Empty response"));
            }
        }));
    }

    private void copyDraft() {
        if (lastDraft == null) return;
        try {
            ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("AI draft", lastDraft));
                Toast.makeText(getContext(), MiogramLocale.get("Скопійовано", "Скопировано", "Copied"), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    private void insertDraft() {
        if (lastDraft == null || onInsert == null) return;
        try {
            onInsert.run(lastDraft);
            dismiss();
        } catch (Throwable ignored) {}
    }

    // ---- Search mode ----

    private void onSearch(String query) {
        setBusy(true);
        statusView.setText("");
        resultView.setVisibility(View.GONE);
        resultActions.setVisibility(View.GONE);
        hitsList.removeAllViews();
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        try {
            req.peer = MessagesController.getInstance(account).getInputPeer(dialogId);
        } catch (Throwable t) {
            setBusy(false);
            statusView.setText(MiogramLocale.get("Немає доступу до чату", "Нет доступа к чату", "Chat unavailable"));
            return;
        }
        req.q = query;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.limit = 20;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            setBusy(false);
            if (!(response instanceof TLRPC.messages_Messages)) {
                statusView.setText(MiogramLocale.get("Нічого не знайдено", "Ничего не найдено", "Nothing found"));
                return;
            }
            List<TLRPC.Message> found = new ArrayList<>(((TLRPC.messages_Messages) response).messages);
            if (found.isEmpty()) {
                statusView.setText(MiogramLocale.get("Нічого не знайдено", "Ничего не найдено", "Nothing found"));
                return;
            }
            Context ctx = getContext();
            for (TLRPC.Message m : found) {
                if (m == null || m.message == null) continue;
                TextView row = new TextView(ctx);
                String snippet = m.message.length() > 90 ? m.message.substring(0, 90) + "…" : m.message;
                row.setText(snippet.replace("\n", " "));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                row.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                row.setBackground(Theme.getSelectorDrawable(false));
                row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
                final int mid = m.id;
                row.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    try {
                        Bundle args = new Bundle();
                        if (dialogId > 0) args.putLong("user_id", dialogId);
                        else args.putLong("chat_id", -dialogId);
                        args.putInt("message_id", mid);
                        parent.presentFragment(new ChatActivity(args));
                        dismiss();
                    } catch (Throwable ignored) {}
                });
                hitsList.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }
        }));
    }
}
