package app.miogram.bridge.localizer;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

public class MiogramLocalizerActivity extends BaseFragment {

    private final List<MiogramLocalizerEngine.StringEntry> allEntries = new ArrayList<>();
    private final List<MiogramLocalizerEngine.StringEntry> displayEntries = new ArrayList<>();

    private RecyclerListView listView;
    private LocalizerAdapter adapter;
    private EmptyTextProgressView emptyView;
    private EditText searchEditText;
    private ProgressBar loadingBar;
    private TextView countTextView;

    private boolean showOnlyOverridden = false;
    private String currentQuery = "";
    private Runnable filterRunnable;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Локалізатор", "Локализатор", "Localizer Studio"));

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_other);
        item.addSubItem(1, R.drawable.msg_share, MiogramLocale.get("Експорт налаштувань (.json)", "Экспорт настроек (.json)", "Export Overrides (.json)"));
        item.addSubItem(2, R.drawable.msg_download, MiogramLocale.get("Імпорт налаштувань (.json)", "Импорт настроек (.json)", "Import Overrides (.json)"));
        item.addSubItem(3, R.drawable.msg_delete, MiogramLocale.get("Скинути всі зміни", "Сбросить все изменения", "Reset All Overrides"));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    exportOverrides();
                } else if (id == 2) {
                    showImportDialog();
                } else if (id == 3) {
                    showResetAllConfirm();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = contentView;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        contentView.addView(root, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 1. Search Bar Card
        FrameLayout searchContainer = new FrameLayout(context);
        searchContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(4));

        FrameLayout searchInner = new FrameLayout(context);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Theme.getColor(Theme.key_chat_messagePanelBackground));
        searchBg.setCornerRadius(AndroidUtilities.dp(12));
        searchInner.setBackground(searchBg);

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.outline_header_search);
        searchIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchInner.addView(searchIcon, LayoutHelper.createFrame(22, 22, Gravity.CENTER_VERTICAL | Gravity.LEFT, 12, 0, 0, 0));

        searchEditText = new EditText(context);
        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        searchEditText.setHint(MiogramLocale.get("Пошук за ключем чи текстом...", "Поиск по ключу или тексту...", "Search by key or text..."));
        searchEditText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        searchEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        searchEditText.setBackground(null);
        searchEditText.setSingleLine(true);
        searchEditText.setPadding(AndroidUtilities.dp(44), 0, AndroidUtilities.dp(36), 0);
        searchInner.addView(searchEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 46, Gravity.CENTER_VERTICAL));

        ImageView clearBtn = new ImageView(context);
        clearBtn.setImageResource(R.drawable.ic_close_white);
        clearBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        clearBtn.setVisibility(View.GONE);
        clearBtn.setOnClickListener(v -> searchEditText.setText(""));
        searchInner.addView(clearBtn, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 10, 0));

        searchContainer.addView(searchInner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(searchContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 2. Info / Filter row
        LinearLayout filterRow = new LinearLayout(context);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        filterRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(6));

        countTextView = new TextView(context);
        countTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        countTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        filterRow.addView(countTextView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView filterChip = new TextView(context);
        filterChip.setText(MiogramLocale.get("Лише змінені", "Только измененные", "Only Overrides"));
        filterChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        filterChip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        updateChipStyle(filterChip, showOnlyOverridden);
        filterChip.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showOnlyOverridden = !showOnlyOverridden;
            updateChipStyle(filterChip, showOnlyOverridden);
            filterEntries();
        });
        filterRow.addView(filterChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        root.addView(filterRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3. Recycler List
        FrameLayout listContainer = new FrameLayout(context);

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new LocalizerAdapter();
        listView.setAdapter(adapter);
        listContainer.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new EmptyTextProgressView(context);
        emptyView.setShowAtCenter(true);
        emptyView.setText(MiogramLocale.get("Рядків не знайдено", "Строки не найдены", "No strings found"));
        listContainer.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setEmptyView(emptyView);

        loadingBar = new ProgressBar(context);
        loadingBar.setVisibility(View.VISIBLE);
        listContainer.addView(loadingBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        root.addView(listContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBtn.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                currentQuery = s.toString().trim().toLowerCase();
                if (filterRunnable != null) AndroidUtilities.cancelRunOnUIThread(filterRunnable);
                filterRunnable = () -> filterEntries();
                AndroidUtilities.runOnUIThread(filterRunnable, 200);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadAllData();
        return fragmentView;
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(12));
        if (selected) {
            gd.setColor(Theme.getColor(Theme.key_chats_actionBackground));
            chip.setTextColor(0xFFFFFFFF);
        } else {
            gd.setColor(Theme.getColor(Theme.key_chat_messagePanelBackground));
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        chip.setBackground(gd);
    }

    private void loadAllData() {
        Utilities.globalQueue.postRunnable(() -> {
            List<MiogramLocalizerEngine.StringEntry> list = MiogramLocalizerEngine.loadAllStrings(getParentActivity() != null ? getParentActivity() : getContext());
            AndroidUtilities.runOnUIThread(() -> {
                allEntries.clear();
                allEntries.addAll(list);
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                filterEntries();
            });
        });
    }

    private void filterEntries() {
        displayEntries.clear();
        for (MiogramLocalizerEngine.StringEntry entry : allEntries) {
            if (showOnlyOverridden && !entry.isOverridden()) {
                continue;
            }
            if (!currentQuery.isEmpty()) {
                boolean matchKey = entry.key.toLowerCase().contains(currentQuery);
                boolean matchOrig = entry.originalValue != null && entry.originalValue.toLowerCase().contains(currentQuery);
                boolean matchCustom = entry.customValue != null && entry.customValue.toLowerCase().contains(currentQuery);
                if (!matchKey && !matchOrig && !matchCustom) {
                    continue;
                }
            }
            displayEntries.add(entry);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        if (countTextView != null) {
            int overridesCount = MiogramLocalizerEngine.getOverridesCount();
            countTextView.setText(MiogramLocale.get(
                    "Знайдено: " + displayEntries.size() + " (Змінено: " + overridesCount + ")",
                    "Найдено: " + displayEntries.size() + " (Изменено: " + overridesCount + ")",
                    "Found: " + displayEntries.size() + " (Modified: " + overridesCount + ")"
            ));
        }
    }

    private void openEditDialog(MiogramLocalizerEngine.StringEntry entry) {
        Context ctx = getParentActivity() != null ? getParentActivity() : getContext();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(entry.key);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(8));

        TextView origLabel = new TextView(ctx);
        origLabel.setText(MiogramLocale.get("Оригінальний текст:", "Оригинальный текст:", "Original String:"));
        origLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        origLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        layout.addView(origLabel);

        TextView origText = new TextView(ctx);
        origText.setText(entry.originalValue != null ? entry.originalValue : "-");
        origText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        origText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        origText.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(10));
        layout.addView(origText);

        TextView customLabel = new TextView(ctx);
        customLabel.setText(MiogramLocale.get("Ваш варіант перекладу:", "Ваш вариант перевода:", "Custom Translation:"));
        customLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        customLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        layout.addView(customLabel);

        EditText input = new EditText(ctx);
        input.setText(entry.getDisplayValue());
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setSelection(input.getText().length());
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton(MiogramLocale.get("Зберегти", "Сохранить", "Save"), (dialog, which) -> {
            String newVal = input.getText().toString().trim();
            if (newVal.equals(entry.originalValue)) {
                MiogramLocalizerEngine.removeOverride(entry.key);
                entry.customValue = null;
            } else {
                MiogramLocalizerEngine.setOverride(entry.key, newVal);
                entry.customValue = newVal;
            }
            filterEntries();
            Toast.makeText(ctx, MiogramLocale.get("Рядок збережено!", "Строка сохранена!", "String updated!"), Toast.LENGTH_SHORT).show();
        });

        if (entry.isOverridden()) {
            builder.setNeutralButton(MiogramLocale.get("Скинути", "Сбросить", "Reset"), (dialog, which) -> {
                MiogramLocalizerEngine.removeOverride(entry.key);
                entry.customValue = null;
                filterEntries();
                Toast.makeText(ctx, MiogramLocale.get("Повернуто до оригіналу", "Сброшено к оригиналу", "Reset to original"), Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void exportOverrides() {
        Context ctx = getParentActivity() != null ? getParentActivity() : getContext();
        String json = MiogramLocalizerEngine.exportToJson();
        android.content.ClipboardManager cm = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Miogram Localizer", json));
            Toast.makeText(ctx, MiogramLocale.get("JSON скопійовано в буфер обміну!", "JSON скопирован в буфер обмена!", "JSON copied to clipboard!"), Toast.LENGTH_LONG).show();
        }
    }

    private void showImportDialog() {
        Context ctx = getParentActivity() != null ? getParentActivity() : getContext();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Імпорт локалізації", "Импорт локализации", "Import Localization"));

        EditText input = new EditText(ctx);
        input.setHint("{\"key\": \"translation\", ...}");
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        input.setLines(5);
        input.setGravity(Gravity.TOP);
        builder.setView(input);

        builder.setPositiveButton(MiogramLocale.get("Імпортувати", "Импортировать", "Import"), (dialog, which) -> {
            String text = input.getText().toString().trim();
            int count = MiogramLocalizerEngine.importFromJson(text);
            loadAllData();
            Toast.makeText(ctx, MiogramLocale.get("Імпортовано рядків: " + count, "Импортировано строк: " + count, "Imported strings: " + count), Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showResetAllConfirm() {
        Context ctx = getParentActivity() != null ? getParentActivity() : getContext();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Скинути всі зміни?", "Сбросить все изменения?", "Reset all overrides?"));
        builder.setMessage(MiogramLocale.get("Усі кастомні переклади будуть видалені і повернені до стандарту.", "Все кастомные переводы будут удалены и возвращены к стандарту.", "All custom strings will be reset to defaults."));
        builder.setPositiveButton(MiogramLocale.get("Скинути", "Сбросить", "Reset"), (dialog, which) -> {
            MiogramLocalizerEngine.clearAll();
            loadAllData();
            Toast.makeText(ctx, MiogramLocale.get("Усі рядки скинуто до оригіналу", "Все строки сброшены к оригиналу", "All strings reset"), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private class LocalizerAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return displayEntries.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new StringCell(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof StringCell) {
                ((StringCell) holder.itemView).setEntry(displayEntries.get(position));
            }
        }
    }

    private class StringCell extends FrameLayout {

        private final TextView keyView;
        private final TextView valueView;
        private final TextView origView;
        private final TextView badgeView;
        private MiogramLocalizerEngine.StringEntry currentEntry;

        public StringCell(Context context) {
            super(context);
            setBackground(Theme.getSelectorDrawable(false));
            setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

            LinearLayout col = new LinearLayout(context);
            col.setOrientation(LinearLayout.VERTICAL);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            keyView = new TextView(context);
            keyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            keyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            keyView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
            topRow.addView(keyView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            badgeView = new TextView(context);
            badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            badgeView.setTextColor(0xFFFFFFFF);
            badgeView.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(1), AndroidUtilities.dp(5), AndroidUtilities.dp(1));
            topRow.addView(badgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            col.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            valueView = new TextView(context);
            valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            valueView.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(1));
            col.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            origView = new TextView(context);
            origView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            origView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            col.addView(origView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(col, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (currentEntry != null) {
                    openEditDialog(currentEntry);
                }
            });
        }

        public void setEntry(MiogramLocalizerEngine.StringEntry entry) {
            this.currentEntry = entry;
            keyView.setText(entry.key);
            valueView.setText(entry.getDisplayValue());

            if (entry.isOverridden()) {
                GradientDrawable gd = new GradientDrawable();
                gd.setCornerRadius(AndroidUtilities.dp(4));
                gd.setColor(0xFF34C759);
                badgeView.setBackground(gd);
                badgeView.setText(MiogramLocale.get("Змінено", "Изменено", "Custom"));
                badgeView.setVisibility(View.VISIBLE);

                origView.setText(MiogramLocale.get("Оригінал: ", "Оригинал: ", "Original: ") + (entry.originalValue != null ? entry.originalValue : "-"));
                origView.setVisibility(View.VISIBLE);
            } else {
                badgeView.setVisibility(View.GONE);
                origView.setVisibility(View.GONE);
            }
        }
    }
}
