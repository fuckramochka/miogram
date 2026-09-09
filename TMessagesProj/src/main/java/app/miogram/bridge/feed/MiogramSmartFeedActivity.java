package app.miogram.bridge.feed;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.miogram.bridge.kanban.MiogramKanbanStorage;

/**
 * Розумна стрічка новин (Smart Feed) з аналітикою від Gemini AI:
 * - Автоматичне видалення реклами та скаму.
 * - Щотижнева стисла вижимка зі збереженням контексту та фотографій.
 * - Швидкий перехід до поста та інтеграція з Канбан-дошкою.
 */
public class MiogramSmartFeedActivity extends BaseFragment {

    private static final int MENU_REFRESH = 1;
    private static final int MENU_SETTINGS = 2;

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private LinearLayout emptyView;
    private LinearLayout progressContainer;
    private TextView progressText;
    private ProgressBar progressBar;

    private List<MiogramSmartFeedService.FeedItem> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Розумна стрічка ໒꒱", "Умная лента ໒꒱", "Smart Feed ໒꒱"));
        actionBar.setSubtitle("AI Digest • Без спаму");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_REFRESH) {
                    refreshFeed();
                } else if (id == MENU_SETTINGS) {
                    showChannelPicker();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_REFRESH, R.drawable.msg_retry); // Refresh icon
        menu.addItem(MENU_SETTINGS, R.drawable.msg_settings_old);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // 1. Progress banner
        progressContainer = new LinearLayout(context);
        progressContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressContainer.setGravity(Gravity.CENTER_VERTICAL);
        progressContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        progressContainer.setBackgroundColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        progressContainer.setVisibility(View.GONE);

        progressBar = new ProgressBar(context);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24)));
        progressContainer.addView(progressBar);

        progressText = new TextView(context);
        progressText.setTextColor(Color.WHITE);
        progressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        progressText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        progressText.setPadding(AndroidUtilities.dp(12), 0, 0, 0);
        progressContainer.addView(progressText);

        root.addView(progressContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // 2. RecyclerView for digest cards
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(20));
        recyclerView.setClipToPadding(false);
        adapter = new FeedAdapter();
        recyclerView.setAdapter(adapter);
        root.addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 3. Empty state
        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);

        TextView emptyIcon = new TextView(context);
        emptyIcon.setText("໒꒱✨");
        emptyIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        emptyIcon.setGravity(Gravity.CENTER);
        emptyView.addView(emptyIcon);

        TextView emptyTitle = new TextView(context);
        emptyTitle.setText(MiogramLocale.get("Розумний ШІ-дайджест", "Умный ИИ-дайджест", "Smart AI Digest"));
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        emptyTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        emptyTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(6));
        emptyView.addView(emptyTitle);

        TextView emptyDesc = new TextView(context);
        emptyDesc.setText(MiogramLocale.get("Оберіть канали, з яких хочете отримувати щотижневу вижимку. ШІ очистить потік від реклами та підготує змістовні картки з фотографіями.", "Выберите каналы для еженедельной выжимки. ИИ очистит ленту от рекламы и подготовит содержательные карточки с фотографиями.", "Pick channels for a weekly digest. AI will strip ads and build rich cards with photos."));
        emptyDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        emptyDesc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyDesc.setGravity(Gravity.CENTER);
        emptyDesc.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        emptyView.addView(emptyDesc);

        TextView setupBtn = new TextView(context);
        setupBtn.setText(MiogramLocale.get("Обрати канали ໒꒱", "Выбрать каналы ໒꒱", "Choose channels ໒꒱"));
        setupBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        setupBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setupBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        setupBtn.setGravity(Gravity.CENTER);
        setupBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        setupBtn.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        setupBtn.setOnClickListener(v -> showChannelPicker());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = AndroidUtilities.dp(20);
        emptyView.addView(setupBtn, btnLp);

        root.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        fragmentView = root;

        loadCachedData();
        return fragmentView;
    }

    private void loadCachedData() {
        items = MiogramSmartFeedService.getCachedFeed();
        updateVisibility();
        adapter.notifyDataSetChanged();

        if (items.isEmpty() && !MiogramSmartFeedService.getTrackedChannels().isEmpty()) {
            refreshFeed();
        }
    }

    private void updateVisibility() {
        if (items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshFeed() {
        progressContainer.setVisibility(View.VISIBLE);
        progressText.setText(MiogramLocale.get("ШІ готує щотижневу вижимку...", "ИИ готовит еженедельную выжимку...", "AI is preparing the weekly digest..."));

        MiogramSmartFeedService.generateWeeklyDigest(currentAccount, new MiogramSmartFeedService.FeedCallback() {
            @Override
            public void onProgress(String status) {
                if (progressText != null) {
                    progressText.setText(status);
                }
            }

            @Override
            public void onComplete(List<MiogramSmartFeedService.FeedItem> newItems) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                items = newItems;
                updateVisibility();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                if (getParentActivity() != null) {
                    Toast.makeText(getParentActivity(), MiogramLocale.get("Оновлено! Додано " + newItems.size() + " важливих новин без спаму ໒꒱", "Обновлено! Добавлено " + newItems.size() + " важных новостей без спама ໒꒱", "Updated! Added " + newItems.size() + " important stories, no spam ໒꒱"), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                if (getParentActivity() == null) return;
                if (MiogramSmartFeedService.ERR_NO_API_KEY.equals(error) || (error != null && error.contains("API-ключ"))) {
                    AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                    b.setTitle(MiogramLocale.get("Miogram AI ໒꒱", "Miogram AI ໒꒱", "Miogram AI ໒꒱"));
                    b.setMessage(MiogramLocale.get("Вкажіть API-ключ Gemini у Miogram AI, щоб генерувати розумну стрічку ໒꒱", "Укажите API-ключ Gemini в Miogram AI для генерации умной ленты ໒꒱", "Add your Gemini API key in Miogram AI to generate the smart feed ໒꒱"));
                    b.setPositiveButton(MiogramLocale.get("Налаштувати AI", "Настроить AI", "Configure AI"), (d, w) -> {
                        presentFragment(new app.miogram.bridge.ui.MiogramAiSettingsActivity());
                    });
                    b.setNegativeButton(MiogramLocale.get("Пізніше", "Позже", "Later"), null);
                    showDialog(b.create());
                } else if (MiogramSmartFeedService.ERR_NO_CHANNELS.equals(error)) {
                    Toast.makeText(getParentActivity(), MiogramLocale.get("Оберіть канали для стрічки, щоб почати.", "Выберите каналы для ленты, чтобы начать.", "Pick channels for the feed to get started."), Toast.LENGTH_LONG).show();
                    showChannelPicker();
                } else {
                    Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showChannelPicker() {
        Context context = getParentActivity();
        if (context == null) return;

        ArrayList<TLRPC.Chat> availableChats = new ArrayList<>();
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        for (TLRPC.Dialog d : allDialogs) {
            if (d.id < 0) {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-d.id);
                if (chat != null && ChatObject.isChannel(chat) && !ChatObject.isMegagroup(chat)) {
                    availableChats.add(chat);
                }
            }
        }

        if (availableChats.isEmpty()) {
            Toast.makeText(context, MiogramLocale.get("Не знайдено підписаних каналів.", "Не найдено подписанных каналов.", "No subscribed channels found."), Toast.LENGTH_SHORT).show();
            return;
        }

        Set<Long> currentTracked = new HashSet<>(MiogramSmartFeedService.getTrackedChannels());

        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Оберіть канали для Smart Feed", "Выберите каналы для Smart Feed", "Select channels for Smart Feed"));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        List<CheckBoxCell> cells = new ArrayList<>();
        for (TLRPC.Chat chat : availableChats) {
            CheckBoxCell cell = new CheckBoxCell(context, 1, 21, null);
            cell.setBackground(Theme.getSelectorDrawable(false));
            long dialogId = -chat.id;
            boolean isChecked = currentTracked.contains(dialogId);
            cell.setText(chat.title, "", isChecked, false);
            cell.setOnClickListener(v -> {
                boolean checked = !cell.isChecked();
                cell.setChecked(checked, true);
                if (checked) {
                    currentTracked.add(dialogId);
                } else {
                    currentTracked.remove(dialogId);
                }
            });
            container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            cells.add(cell);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("Зберегти", (dialog, which) -> {
            MiogramSmartFeedService.setTrackedChannels(currentTracked);
            refreshFeed();
        });
        builder.setNegativeButton("Скасувати", null);

        showDialog(builder.create());
    }

    private class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault());

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

            // Squircle card background
            card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(12);
            card.setLayoutParams(lp);

            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MiogramSmartFeedService.FeedItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout cardView;
            TextView channelTitle;
            TextView categoryBadge;
            TextView dateText;
            TextView postTitle;
            TextView postSummary;
            BackupImageView photoView;
            LinearLayout actionsRow;
            TextView openChatBtn;
            TextView toKanbanBtn;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = (LinearLayout) itemView;
                Context ctx = itemView.getContext();

                // 1. Header row
                LinearLayout header = new LinearLayout(ctx);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);

                channelTitle = new TextView(ctx);
                channelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                channelTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                channelTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                channelTitle.setEllipsize(TextUtils.TruncateAt.END);
                channelTitle.setMaxLines(1);
                header.addView(channelTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                categoryBadge = new TextView(ctx);
                categoryBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                categoryBadge.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                categoryBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                categoryBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton) & 0x22FFFFFF));
                categoryBadge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
                header.addView(categoryBadge);

                dateText = new TextView(ctx);
                dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                dateText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                dateText.setPadding(AndroidUtilities.dp(8), 0, 0, 0);
                header.addView(dateText);

                cardView.addView(header);

                // 2. Photo thumbnail
                photoView = new BackupImageView(ctx);
                photoView.setRoundRadius(AndroidUtilities.dp(10));
                photoView.setVisibility(View.GONE);
                LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(160));
                photoLp.topMargin = AndroidUtilities.dp(10);
                cardView.addView(photoView, photoLp);

                // 3. Post Title
                postTitle = new TextView(ctx);
                postTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                postTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                postTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                titleLp.topMargin = AndroidUtilities.dp(10);
                cardView.addView(postTitle, titleLp);

                // 4. AI Squeeze / Digest
                postSummary = new TextView(ctx);
                postSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                postSummary.setLineSpacing(AndroidUtilities.dp(2), 1.1f);
                postSummary.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                LinearLayout.LayoutParams sumLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                sumLp.topMargin = AndroidUtilities.dp(6);
                cardView.addView(postSummary, sumLp);

                // 5. Actions row
                actionsRow = new LinearLayout(ctx);
                actionsRow.setOrientation(LinearLayout.HORIZONTAL);
                actionsRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                actLp.topMargin = AndroidUtilities.dp(14);

                toKanbanBtn = new TextView(ctx);
                toKanbanBtn.setText(MiogramLocale.get("В Канбан 📌", "В Канбан 📌", "To Kanban 📌"));
                toKanbanBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                toKanbanBtn.setTextColor(0xFFFFFFFF);
                toKanbanBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                toKanbanBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
                toKanbanBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
                toKanbanBtn.setContentDescription(MiogramLocale.get("Додати в Канбан", "Добавить в Канбан", "Add to Kanban"));
                actionsRow.addView(toKanbanBtn);

                openChatBtn = new TextView(ctx);
                openChatBtn.setText(MiogramLocale.get("Читати в каналі →", "Читать в канале →", "Read in channel →"));
                openChatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                openChatBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                openChatBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                openChatBtn.setBackground(Theme.getSelectorDrawable(false));
                openChatBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
                openChatBtn.setContentDescription(MiogramLocale.get("Відкрити пост у каналі", "Открыть пост в канале", "Open post in channel"));
                actionsRow.addView(openChatBtn);

                cardView.addView(actionsRow, actLp);
            }

            void bind(MiogramSmartFeedService.FeedItem item) {
                channelTitle.setText(item.channelTitle != null ? item.channelTitle : MiogramLocale.get("Канал", "Канал", "Channel"));
                dateText.setText(dateFormat.format(new Date(item.timestamp > 0 ? item.timestamp : System.currentTimeMillis())));

                if (!TextUtils.isEmpty(item.category)) {
                    categoryBadge.setText(item.category);
                    int catColor = categoryColor(item.category);
                    categoryBadge.setTextColor(catColor);
                    categoryBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), catColor & 0x22FFFFFF));
                    categoryBadge.setVisibility(View.VISIBLE);
                } else {
                    categoryBadge.setVisibility(View.GONE);
                }

                postTitle.setText(item.title != null ? item.title : "");
                postSummary.setText(item.summary != null ? item.summary : "");

                if (item.hasPhoto && item.originalMessage != null && item.originalMessage.media instanceof TLRPC.TL_messageMediaPhoto) {
                    TLRPC.TL_messageMediaPhoto photoMedia = (TLRPC.TL_messageMediaPhoto) item.originalMessage.media;
                    if (photoMedia.photo != null && photoMedia.photo.sizes != null && !photoMedia.photo.sizes.isEmpty()) {
                        photoView.setVisibility(View.VISIBLE);
                        photoView.setImage(ImageLocation.getForPhoto(photoMedia.photo.sizes.get(photoMedia.photo.sizes.size() - 1), photoMedia.photo), "160_160", null, null, currentAccount);
                    } else {
                        photoView.setVisibility(View.GONE);
                    }
                } else {
                    photoView.setVisibility(View.GONE);
                }

                openChatBtn.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    Bundle args = new Bundle();
                    args.putLong("chat_id", -item.dialogId);
                    args.putInt("message_id", item.messageId);
                    presentFragment(new ChatActivity(args));
                });

                toKanbanBtn.setOnClickListener(v -> {
                    MiogramHaptic.success(v);
                    MiogramKanbanStorage.addItem(item.title, item.summary, 0, item.dialogId, item.messageId);
                    Toast.makeText(itemView.getContext(), MiogramLocale.get("Додано в канбан дошку!", "Добавлено на канбан-доску!", "Added to the Kanban board!"), Toast.LENGTH_SHORT).show();
                });
            }

            /** Stable tint per category so the feed scans faster. */
            private int categoryColor(String category) {
                if (category == null) return Theme.getColor(Theme.key_featuredStickers_addButton);
                String c = category.trim().toLowerCase(Locale.US);
                if (c.contains("крипто") || c.contains("crypto")) return 0xFFF0B90B;
                if (c.contains("техно") || c.contains("tech") || c.contains("техно")) return 0xFF00A8FC;
                if (c.contains("спорт") || c.contains("sport")) return 0xFF23A55A;
                if (c.contains("культур") || c.contains("cultur") || c.contains("культур")) return 0xFF9D4EDD;
                if (c.contains("наук") || c.contains("scien")) return 0xFF00C2A8;
                return Theme.getColor(Theme.key_featuredStickers_addButton);
            }
        }
    }
}
