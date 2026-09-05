package app.miogram.bridge.feed;

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
        actionBar.setTitle("Розумна стрічка ໒꒱");
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
        emptyTitle.setText("Розумний ШІ-дайджест");
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        emptyTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        emptyTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(6));
        emptyView.addView(emptyTitle);

        TextView emptyDesc = new TextView(context);
        emptyDesc.setText("Оберіть канали, з яких хочете отримувати щотижневу вижимку. ШІ очистить потік від реклами та підготує змістовні картки з фотографіями.");
        emptyDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        emptyDesc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyDesc.setGravity(Gravity.CENTER);
        emptyDesc.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        emptyView.addView(emptyDesc);

        TextView setupBtn = new TextView(context);
        setupBtn.setText("Обрати канали ໒꒱");
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
        progressText.setText("ШІ готує щотижневу вижимку...");

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
                    Toast.makeText(getParentActivity(), "Оновлено! Додано " + newItems.size() + " важливих новин без спаму ໒꒱", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (progressContainer != null) {
                    progressContainer.setVisibility(View.GONE);
                }
                if (getParentActivity() != null) {
                    if (error != null && error.contains("API-ключ")) {
                        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                        b.setTitle("Miogram AI ໒꒱");
                        b.setMessage(error);
                        b.setPositiveButton("Налаштувати AI", (d, w) -> {
                            presentFragment(new app.miogram.bridge.ui.MiogramAiSettingsActivity());
                        });
                        b.setNegativeButton("Пізніше", null);
                        showDialog(b.create());
                    } else {
                        Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                    }
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
            Toast.makeText(context, "Не знайдено підписаних каналів.", Toast.LENGTH_SHORT).show();
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
                toKanbanBtn.setText("В Канбан 📌");
                toKanbanBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                toKanbanBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                toKanbanBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                toKanbanBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
                actionsRow.addView(toKanbanBtn);

                openChatBtn = new TextView(ctx);
                openChatBtn.setText("Читати в каналі →");
                openChatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                openChatBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                openChatBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                openChatBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
                actionsRow.addView(openChatBtn);

                cardView.addView(actionsRow, actLp);
            }

            void bind(MiogramSmartFeedService.FeedItem item) {
                channelTitle.setText(item.channelTitle != null ? item.channelTitle : "Канал");
                dateText.setText(dateFormat.format(new Date(item.timestamp > 0 ? item.timestamp : System.currentTimeMillis())));

                if (!TextUtils.isEmpty(item.category)) {
                    categoryBadge.setText(item.category);
                    categoryBadge.setVisibility(View.VISIBLE);
                } else {
                    categoryBadge.setVisibility(View.GONE);
                }

                postTitle.setText(item.title != null ? item.title : "");
                postSummary.setText(item.summary != null ? item.summary : "");

                if (item.hasPhoto && item.originalMessage != null && item.originalMessage.media instanceof TLRPC.TL_messageMediaPhoto) {
                    TLRPC.TL_messageMediaPhoto photoMedia = (TLRPC.TL_messageMediaPhoto) item.originalMessage.media;
                    if (photoMedia.photo != null) {
                        photoView.setVisibility(View.VISIBLE);
                        photoView.setImage(ImageLocation.getForPhoto(photoMedia.photo.sizes.get(photoMedia.photo.sizes.size() - 1), photoMedia.photo), "160_160", null, null, currentAccount);
                    } else {
                        photoView.setVisibility(View.GONE);
                    }
                } else {
                    photoView.setVisibility(View.GONE);
                }

                openChatBtn.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putLong("chat_id", -item.dialogId);
                    args.putInt("message_id", item.messageId);
                    presentFragment(new ChatActivity(args));
                });

                toKanbanBtn.setOnClickListener(v -> {
                    MiogramKanbanStorage.addItem(item.title, item.summary, 0, item.dialogId, item.messageId);
                    Toast.makeText(itemView.getContext(), "Додано в канбан дошку!", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
