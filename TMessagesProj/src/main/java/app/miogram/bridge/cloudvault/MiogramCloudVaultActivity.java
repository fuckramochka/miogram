package app.miogram.bridge.cloudvault;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.TopicsController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.DialogsActivity;
import org.telegram.messenger.MessagesStorage;
import androidx.core.content.FileProvider;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Modern Cloud Drive UI for the Miogram Encrypted Cloud Vault:
 * - Google Drive / Nextcloud-style virtual file directory.
 * - Forum Supergroup topics serve as cloud folders.
 * - Files > 2GB are transparently sliced into AES-256 encrypted chunks.
 * - Offline cached indexing + instant cloud sync.
 */
public class MiogramCloudVaultActivity extends BaseFragment {

    private static final int MENU_SEARCH = 1;
    private static final int MENU_OTHER = 2;
    private static final int MENU_VIEW_MODE = 3;
    private static final int SUBMENU_CHAT = 101;
    private static final int SUBMENU_SYNC = 102;
    private static final int SUBMENU_KEY = 103;
    private static final int SUBMENU_UNLINK = 104;

    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_GRID = 1;

    public static final int CATEGORY_ALL = 0;
    public static final int CATEGORY_MEDIA = 1;
    public static final int CATEGORY_DOCS = 2;
    public static final int CATEGORY_AUDIO = 3;
    public static final int CATEGORY_ARCHIVES = 4;

    private static final int REQUEST_PICK_FILE = 2101;

    private FrameLayout rootLayout;
    private LinearLayout onboardingLayout;
    private LinearLayout mainContentLayout;

    private TextView storageTitleText;
    private TextView storageSubtitleText;
    private ProgressBar storageProgressBar;

    private LinearLayout topicsContainer;
    private HorizontalScrollView topicsScrollView;
    private RecyclerView filesRecyclerView;
    private FilesAdapter filesAdapter;
    private LinearLayout emptyView;
    private TextView emptyText;
    private TextView emptyHint;
    private FrameLayout fabButton;

    private int currentCategoryFilter = CATEGORY_ALL;
    private int currentViewMode = VIEW_TYPE_GRID; // Default to Gallery!
    private ActionBarMenuItem viewModeItem;

    private long currentSelectedTopicId = 0; // 0 = All files
    private String currentSelectedTopicName = "";
    private String currentSearchQuery = "";

    private final ArrayList<MiogramCloudVaultFile> displayedFiles = new ArrayList<>();
    private final ArrayList<TLRPC.TL_forumTopic> cachedTopics = new ArrayList<>();

    @Override
    public View createView(Context context) {
        currentViewMode = ApplicationLoader.applicationContext
                .getSharedPreferences("miogram_cloud_vault_prefs", Context.MODE_PRIVATE)
                .getInt("vault_view_mode", VIEW_TYPE_GRID);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get("Хмарне сховище ☁️", "Облачное хранилище ☁️", "Cloud Vault ☁️"));
        updateSubtitle();

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(MENU_SEARCH, R.drawable.outline_header_search);
        searchItem.setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
            }

            @Override
            public void onSearchCollapse() {
                currentSearchQuery = "";
                filterAndReloadFiles();
            }

            @Override
            public void onTextChanged(EditText editText) {
                currentSearchQuery = editText.getText().toString().trim();
                filterAndReloadFiles();
            }
        });
        searchItem.setSearchFieldHint(MiogramLocale.get("Пошук файлів...", "Поиск файлов...", "Search files..."));

        viewModeItem = menu.addItem(MENU_VIEW_MODE, currentViewMode == VIEW_TYPE_GRID ? R.drawable.ic_filter_list : R.drawable.files_gallery);

        ActionBarMenuItem otherItem = menu.addItem(MENU_OTHER, R.drawable.ic_ab_other);
        otherItem.addSubItem(SUBMENU_CHAT, R.drawable.msg_channel, MiogramLocale.get("Відкрити форум у чаті", "Открыть форум в чате", "Open Forum in Chat"));
        otherItem.addSubItem(SUBMENU_SYNC, R.drawable.msg_retry, MiogramLocale.get("Синхронізувати з хмарою", "Синхронизировать с облаком", "Sync with Cloud"));
        otherItem.addSubItem(SUBMENU_KEY, R.drawable.msg_secret, MiogramLocale.get("Ключ шифрування (AES-256)", "Ключ шифрования (AES-256)", "Encryption Key (AES-256)"));
        otherItem.addSubItem(SUBMENU_UNLINK, R.drawable.msg_delete, MiogramLocale.get("Відв'язати супергрупу", "Отвязать супергруппу", "Unlink Vault Chat"));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_VIEW_MODE) {
                    toggleViewMode();
                } else if (id == SUBMENU_CHAT) {
                    openVaultChat();
                } else if (id == SUBMENU_SYNC) {
                    syncFromCloud();
                } else if (id == SUBMENU_KEY) {
                    showMasterKeyDialog();
                } else if (id == SUBMENU_UNLINK) {
                    showUnlinkDialog();
                }
            }
        });

        rootLayout = new FrameLayout(context);
        rootLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        buildOnboardingView(context);
        buildMainContentView(context);
        buildFab(context);

        updateVaultVisibility();
        fragmentView = rootLayout;
        return fragmentView;
    }

    private void toggleViewMode() {
        currentViewMode = (currentViewMode == VIEW_TYPE_GRID) ? VIEW_TYPE_LIST : VIEW_TYPE_GRID;
        ApplicationLoader.applicationContext
                .getSharedPreferences("miogram_cloud_vault_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("vault_view_mode", currentViewMode)
                .apply();
        if (viewModeItem != null) {
            viewModeItem.setIcon(currentViewMode == VIEW_TYPE_GRID ? R.drawable.ic_filter_list : R.drawable.files_gallery);
        }
        updateLayoutManager();
        if (filesAdapter != null) {
            filesAdapter.notifyDataSetChanged();
        }
    }

    private void updateLayoutManager() {
        if (filesRecyclerView == null) return;
        Context context = getContext();
        if (context == null) context = getParentActivity();
        if (context == null) return;
        if (currentViewMode == VIEW_TYPE_GRID) {
            filesRecyclerView.setLayoutManager(new GridLayoutManager(context, 3));
        } else {
            filesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateVaultVisibility();
        if (MiogramCloudVaultEngine.hasVault(currentAccount)) {
            loadTopicsFromTelegram();
            syncFromCloud();
        }
    }

    private void updateSubtitle() {
        if (actionBar == null) return;
        long totalSize = MiogramCloudVaultEngine.getTotalVaultSize();
        int count = MiogramCloudVaultEngine.getTotalVaultFilesCount();
        String sub = MiogramLocale.get("AES-256-GCM • ", "AES-256-GCM • ", "AES-256-GCM • ")
                + count + " " + MiogramLocale.get("файлів", "файлов", "files")
                + " (" + AndroidUtilities.formatFileSize(totalSize) + ")";
        actionBar.setSubtitle(sub);
    }

    private void updateVaultVisibility() {
        boolean hasVault = MiogramCloudVaultEngine.hasVault(currentAccount);
        if (onboardingLayout != null) onboardingLayout.setVisibility(hasVault ? View.GONE : View.VISIBLE);
        if (mainContentLayout != null) mainContentLayout.setVisibility(hasVault ? View.VISIBLE : View.GONE);
        if (fabButton != null) fabButton.setVisibility(hasVault ? View.VISIBLE : View.GONE);
    }

    // --- Onboarding / Welcome Hero View ---

    private void buildOnboardingView(Context context) {
        onboardingLayout = new LinearLayout(context);
        onboardingLayout.setOrientation(LinearLayout.VERTICAL);
        onboardingLayout.setGravity(Gravity.CENTER);
        onboardingLayout.setPadding(AndroidUtilities.dp(32), AndroidUtilities.dp(32), AndroidUtilities.dp(32), AndroidUtilities.dp(32));

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(R.drawable.cloud);
        iconView.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
        onboardingLayout.addView(iconView, LayoutHelper.createLinear(96, 96, Gravity.CENTER, 0, 0, 0, 20));

        TextView title = new TextView(context);
        title.setText(MiogramLocale.get("Зашифрований Miogram Vault", "Зашифрованный Miogram Vault", "Encrypted Miogram Vault"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setGravity(Gravity.CENTER);
        onboardingLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 12));

        TextView desc = new TextView(context);
        desc.setText(MiogramLocale.get(
                "Безлімітне персональне хмарне сховище на базі форум-супергрупи Telegram.\n\n"
                        + "• Файли будь-якого розміру (>2 ГБ автоматично ріжуться на чанки).\n"
                        + "• Наскрізне шифрування AES-256-GCM прямо на пристрої.\n"
                        + "• Для Telegram та інших користувачів це набір незрозумілих файлів.",
                "Безлимитное персональное облачное хранилище на базе форум-супергруппы Telegram.\n\n"
                        + "• Файлы любого размера (>2 ГБ автоматически нарезаются на чанки).\n"
                        + "• Сквозное шифрование AES-256-GCM прямо на устройстве.\n"
                        + "• Для Telegram и других пользователей это набор непонятных файлов.",
                "Unlimited personal cloud vault powered by Telegram forum supergroup.\n\n"
                        + "• Files of any size (>2 GB transparently sliced into chunks).\n"
                        + "• Client-side end-to-end AES-256-GCM encryption.\n"
                        + "• Regular clients only see raw opaque binary chunks."
        ));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(AndroidUtilities.dp(2), 1.1f);
        onboardingLayout.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 32));

        // Create Vault Button
        TextView createBtn = new TextView(context);
        createBtn.setText(MiogramLocale.get("Створити сховище в 1 клік", "Создать хранилище в 1 клик", "Create Vault in 1 Tap"));
        createBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        createBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        createBtn.setTextColor(Color.WHITE);
        createBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        btnBg.setCornerRadius(AndroidUtilities.dp(12));
        createBtn.setBackground(btnBg);
        createBtn.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));

        createBtn.setOnClickListener(v -> createVaultAutomatically());
        onboardingLayout.addView(createBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 16, 0, 16, 12));

        // Link existing chat button
        TextView linkBtn = new TextView(context);
        linkBtn.setText(MiogramLocale.get("Прив'язати існуючу супергрупу", "Привязать существующую супергруппу", "Link Existing Supergroup"));
        linkBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        linkBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        linkBtn.setGravity(Gravity.CENTER);
        linkBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        linkBtn.setOnClickListener(v -> showLinkExistingDialog());
        onboardingLayout.addView(linkBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        rootLayout.addView(onboardingLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    // --- Main Content View (Drive Interface) ---

    private void buildMainContentView(Context context) {
        mainContentLayout = new LinearLayout(context);
        mainContentLayout.setOrientation(LinearLayout.VERTICAL);

        // 1. Storage Banner Card
        FrameLayout bannerCard = new FrameLayout(context);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(14));
        bannerCard.setBackground(cardBg);
        bannerCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout bannerInner = new LinearLayout(context);
        bannerInner.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView cloudIco = new ImageView(context);
        cloudIco.setImageResource(R.drawable.cloud);
        cloudIco.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
        titleRow.addView(cloudIco, LayoutHelper.createLinear(26, 26, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        storageTitleText = new TextView(context);
        storageTitleText.setText(MiogramLocale.get("Сховище Miogram Vault", "Хранилище Miogram Vault", "Miogram Cloud Vault"));
        storageTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        storageTitleText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        storageTitleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleRow.addView(storageTitleText, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView secBadge = new TextView(context);
        secBadge.setText("● AES-256");
        secBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        secBadge.setTextColor(0xFF4CAF50);
        secBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleRow.addView(secBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        bannerInner.addView(titleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        storageSubtitleText = new TextView(context);
        storageSubtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        storageSubtitleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        bannerInner.addView(storageSubtitleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        storageProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        storageProgressBar.setIndeterminate(false);
        storageProgressBar.setMax(100);
        storageProgressBar.setProgress(15);
        bannerInner.addView(storageProgressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 6));

        bannerCard.addView(bannerInner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        mainContentLayout.addView(bannerCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 12, 12, 8));

        // 2. Horizontal Topic Folder Tabs
        topicsScrollView = new HorizontalScrollView(context);
        topicsScrollView.setHorizontalScrollBarEnabled(false);

        topicsContainer = new LinearLayout(context);
        topicsContainer.setOrientation(LinearLayout.HORIZONTAL);
        topicsContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        topicsScrollView.addView(topicsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        mainContentLayout.addView(topicsScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3. RecyclerView for Files
        FrameLayout listContainer = new FrameLayout(context);

        filesRecyclerView = new RecyclerView(context);
        updateLayoutManager();
        filesRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                if (currentViewMode == VIEW_TYPE_GRID) {
                    int s = AndroidUtilities.dp(3);
                    outRect.set(s, s, s, s);
                } else {
                    outRect.set(0, 0, 0, 0);
                }
            }
        });
        filesAdapter = new FilesAdapter();
        filesRecyclerView.setAdapter(filesAdapter);
        listContainer.addView(filesRecyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Empty view
        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);

        ImageView emptyIco = new ImageView(context);
        emptyIco.setImageResource(R.drawable.baseline_cloud_download_24);
        emptyIco.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        emptyView.addView(emptyIco, LayoutHelper.createLinear(64, 64, Gravity.CENTER, 0, 0, 0, 12));

        emptyText = new TextView(context);
        emptyText.setText(MiogramLocale.get("У цій папці поки немає файлів", "В этой папке пока нет файлов", "No files in this folder yet"));
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        emptyText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        emptyText.setGravity(Gravity.CENTER);
        emptyView.addView(emptyText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 6));

        emptyHint = new TextView(context);
        emptyHint.setText(MiogramLocale.get("Натисніть (+), щоб завантажити файл будь-якого розміру", "Нажмите (+), чтобы загрузить файл любого размера", "Tap (+) to upload files of any size"));
        emptyHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        emptyHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        emptyHint.setGravity(Gravity.CENTER);
        emptyView.addView(emptyHint, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        listContainer.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        mainContentLayout.addView(listContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        rootLayout.addView(mainContentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    // --- Floating Action Button (Upload) ---

    private void buildFab(Context context) {
        fabButton = new FrameLayout(context);
        int fabSize = AndroidUtilities.dp(56);

        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        fabButton.setBackground(fabBg);
        fabButton.setElevation(AndroidUtilities.dp(6));

        ImageView addIcon = new ImageView(context);
        addIcon.setImageResource(R.drawable.baseline_add_24);
        addIcon.setColorFilter(Color.WHITE);
        fabButton.addView(addIcon, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

        fabButton.setOnClickListener(v -> openFilePicker());

        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(56, 56, Gravity.BOTTOM | Gravity.END, 0, 0, 20, 20);
        rootLayout.addView(fabButton, lp);
    }

    // --- Topics / Folder Pills Management ---

    private void refreshTopicPills() {
        if (topicsContainer == null) return;
        topicsContainer.removeAllViews();
        Context context = getContext();
        if (context == null) return;

        // 1. Media Type Filter Pills
        addCategoryPill(context, CATEGORY_ALL, MiogramLocale.get("📁 Всі", "📁 Все", "📁 All"), currentCategoryFilter == CATEGORY_ALL);
        addCategoryPill(context, CATEGORY_MEDIA, MiogramLocale.get("🖼️ Галерея", "🖼️ Галерея", "🖼️ Gallery"), currentCategoryFilter == CATEGORY_MEDIA);
        addCategoryPill(context, CATEGORY_DOCS, MiogramLocale.get("📄 Документи", "📄 Документы", "📄 Docs"), currentCategoryFilter == CATEGORY_DOCS);
        addCategoryPill(context, CATEGORY_AUDIO, MiogramLocale.get("🎵 Музика", "🎵 Музыка", "🎵 Music"), currentCategoryFilter == CATEGORY_AUDIO);
        addCategoryPill(context, CATEGORY_ARCHIVES, MiogramLocale.get("📦 Архіви", "📦 Архивы", "📦 Archives"), currentCategoryFilter == CATEGORY_ARCHIVES);

        // Separator between categories and folder topics
        if (!cachedTopics.isEmpty()) {
            View sep = new View(context);
            sep.setBackgroundColor(0x33888888);
            topicsContainer.addView(sep, LayoutHelper.createLinear(1, 20, Gravity.CENTER_VERTICAL, 4, 0, 8, 0));
        }

        // 2. Topics from supergroup
        for (TLRPC.TL_forumTopic t : cachedTopics) {
            boolean active = (currentSelectedTopicId == t.id);
            addTopicPill(context, t.id, t.title != null ? t.title : "Папка #" + t.id, active);
        }

        // "+ Папка" pill
        TextView addPill = new TextView(context);
        addPill.setText("+ " + MiogramLocale.get("Папка", "Папка", "Folder"));
        addPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addPill.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addPill.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        addPill.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(Color.TRANSPARENT);
        addBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_featuredStickers_addButton));
        addBg.setCornerRadius(AndroidUtilities.dp(16));
        addPill.setBackground(addBg);

        addPill.setOnClickListener(v -> promptCreateFolder());
        topicsContainer.addView(addPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private void addCategoryPill(Context context, int category, String title, boolean active) {
        TextView pill = new TextView(context);
        pill.setText(title);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        pill.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        pill.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(6), AndroidUtilities.dp(13), AndroidUtilities.dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(16));
        if (active) {
            bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            pill.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            pill.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        pill.setBackground(bg);

        pill.setOnClickListener(v -> {
            currentCategoryFilter = category;
            if (category == CATEGORY_MEDIA && currentViewMode != VIEW_TYPE_GRID) {
                toggleViewMode();
            }
            refreshTopicPills();
            filterAndReloadFiles();
        });

        topicsContainer.addView(pill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private void addTopicPill(Context context, long topicId, String title, boolean active) {
        TextView pill = new TextView(context);
        pill.setText(title);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        pill.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        pill.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(16));
        if (active) {
            bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            pill.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            pill.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        pill.setBackground(bg);

        pill.setOnClickListener(v -> {
            currentSelectedTopicId = topicId;
            currentSelectedTopicName = topicId == 0 ? "" : title;
            refreshTopicPills();
            filterAndReloadFiles();
        });

        topicsContainer.addView(pill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private void promptCreateFolder() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Створити папку (Тему форуму)", "Создать папку (Тему форума)", "Create Folder (Forum Topic)"));

        final EditText input = new EditText(getParentActivity());
        input.setHint(MiogramLocale.get("Назва папки (напр. 📸 Фотографії)", "Название папки (напр. 📸 Фотографии)", "Folder name (e.g. 📸 Photos)"));
        input.setSingleLine(true);
        FrameLayout container = new FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.Create), (d, w) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
                MiogramCloudVaultEngine.createTopic(currentAccount, vaultChatId, name, 0x3390EC);
                Toast.makeText(getParentActivity(), MiogramLocale.get("Папку створено!", "Папка создана!", "Folder created!"), Toast.LENGTH_SHORT).show();
                AndroidUtilities.runOnUIThread(this::loadTopicsFromTelegram, 1000);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void loadTopicsFromTelegram() {
        long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
        if (vaultChatId == 0) return;

        TopicsController tc = getMessagesController().getTopicsController();
        tc.loadTopics(vaultChatId);
        ArrayList<TLRPC.TL_forumTopic> topics = tc.getTopics(vaultChatId);
        cachedTopics.clear();
        if (topics != null) {
            cachedTopics.addAll(topics);
        }
        refreshTopicPills();
    }

    // --- Files Filtering & Display ---

    private void filterAndReloadFiles() {
        ArrayList<MiogramCloudVaultFile> rawFiles = MiogramCloudVaultEngine.getFilesForTopic(currentSelectedTopicId);
        displayedFiles.clear();

        for (MiogramCloudVaultFile f : rawFiles) {
            if (currentCategoryFilter == CATEGORY_MEDIA && !f.isMedia()) continue;
            if (currentCategoryFilter == CATEGORY_DOCS && !f.isDocument()) continue;
            if (currentCategoryFilter == CATEGORY_AUDIO && !f.isAudio()) continue;
            if (currentCategoryFilter == CATEGORY_ARCHIVES && !f.isArchive()) continue;

            if (!TextUtils.isEmpty(currentSearchQuery)) {
                if (f.name == null || !f.name.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                    continue;
                }
            }
            displayedFiles.add(f);
        }

        if (filesAdapter != null) {
            filesAdapter.notifyDataSetChanged();
        }

        if (emptyView != null) {
            emptyView.setVisibility(displayedFiles.isEmpty() ? View.VISIBLE : View.GONE);
            if (emptyText != null && emptyHint != null) {
                if (currentCategoryFilter == CATEGORY_MEDIA) {
                    emptyText.setText(MiogramLocale.get("У галереї ще немає медіафайлів", "В галерее еще нет медиафайлов", "No media in gallery yet"));
                    emptyHint.setText(MiogramLocale.get("Збережіть фото чи відео з будь-якого чату!", "Сохраните фото или видео из любого чата!", "Save photos or videos from any chat!"));
                } else {
                    emptyText.setText(MiogramLocale.get("У цій папці поки немає файлів", "В этой папке пока нет файлов", "No files in this folder yet"));
                    emptyHint.setText(MiogramLocale.get("Натисніть (+), щоб завантажити файл будь-якого розміру", "Нажмите (+), чтобы загрузить файл любого размера", "Tap (+) to upload files of any size"));
                }
            }
        }

        updateStatsBanner();
        updateSubtitle();
    }

    private void updateStatsBanner() {
        if (storageSubtitleText == null || storageProgressBar == null) return;
        long totalSize = MiogramCloudVaultEngine.getTotalVaultSize();
        int totalFiles = MiogramCloudVaultEngine.getTotalVaultFilesCount();

        storageSubtitleText.setText(totalFiles + " " + MiogramLocale.get("файлів", "файлов", "files")
                + " • " + AndroidUtilities.formatFileSize(totalSize) + " " + MiogramLocale.get("у хмарі", "в облаке", "in cloud"));

        // Approximate percentage relative to 100GB visual bar
        float percent = Math.min(100f, (float) totalSize / (100L * 1024 * 1024 * 1024) * 100f);
        storageProgressBar.setProgress(Math.max(5, (int) percent));
    }

    private void syncFromCloud() {
        long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
        if (vaultChatId == 0) return;

        MiogramCloudVaultEngine.syncVaultFiles(currentAccount, vaultChatId, new MiogramCloudVaultEngine.SyncCallback() {
            @Override
            public void onSyncProgress(int count) {
            }

            @Override
            public void onSyncComplete(ArrayList<MiogramCloudVaultFile> files) {
                filterAndReloadFiles();
            }

            @Override
            public void onSyncError(String message) {
            }
        });
    }

    // --- File Upload Flow ---

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_PICK_FILE);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), MiogramLocale.get("Не вдалося відкрити вибір файлів", "Не удалось открыть выбор файлов", "Failed to open file picker"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                processAndUploadFile(uri);
            }
        }
    }

    private void processAndUploadFile(Uri uri) {
        Context context = getParentActivity() != null ? getParentActivity() : getContext();
        if (context == null) return;

        String fileName = "file_" + System.currentTimeMillis();
        long fileSize = 0;
        String mimeType = context.getContentResolver().getType(uri);

        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex);
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        final String finalFileName = fileName;
        final long finalFileSize = fileSize;
        final String finalMimeType = mimeType != null ? mimeType : "application/octet-stream";
        final String fileId = UUID.randomUUID().toString();

        final AlertDialog progressDialog = new AlertDialog(context, 3);
        progressDialog.setMessage(MiogramLocale.get("Шифрування файлу (AES-256-GCM)...", "Шифрование файла (AES-256-GCM)...", "Encrypting file (AES-256-GCM)..."));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Utilities.globalQueue.postRunnable(() -> {
            try {
                ArrayList<File> chunkFiles = MiogramCloudVaultEngine.splitAndEncryptFile(
                        context, uri, finalFileName, finalFileSize, fileId,
                        (progress, status) -> AndroidUtilities.runOnUIThread(() -> progressDialog.setMessage(status))
                );

                MiogramCloudVaultFile vaultFile = new MiogramCloudVaultFile();
                vaultFile.fileId = fileId;
                vaultFile.name = finalFileName;
                vaultFile.totalSize = finalFileSize;
                vaultFile.mimeType = finalMimeType;
                vaultFile.chunksCount = chunkFiles.size();
                vaultFile.chunkSize = MiogramCloudVaultEngine.DEFAULT_CHUNK_SIZE;
                vaultFile.topicId = currentSelectedTopicId;
                vaultFile.topicName = currentSelectedTopicName;
                vaultFile.date = System.currentTimeMillis() / 1000L;

                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.setMessage(MiogramLocale.get("Відправка в Telegram Cloud...", "Отправка в Telegram Cloud...", "Uploading to Telegram Cloud..."));

                    long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
                    if (vaultChatId == 0) {
                        progressDialog.dismiss();
                        showVaultRequiredDialog();
                        return;
                    }
                    long targetDialogId = -vaultChatId;

                    MessageObject replyToTopMsg = null;
                    if (vaultFile.topicId > 0) {
                        TLRPC.TL_message dummyMsg = new TLRPC.TL_message();
                        dummyMsg.id = (int) vaultFile.topicId;
                        dummyMsg.dialog_id = targetDialogId;
                        replyToTopMsg = new MessageObject(currentAccount, dummyMsg, false, false);
                        replyToTopMsg.isTopicMainMessage = true;
                    }

                    for (int i = 0; i < chunkFiles.size(); i++) {
                        File chunk = chunkFiles.get(i);
                        String caption;
                        if (i == 0) {
                            caption = MiogramCloudVaultEngine.createManifestCaption(vaultFile);
                        } else {
                            caption = MiogramCloudVaultEngine.createPartCaption(vaultFile.fileId, i + 1, chunkFiles.size());
                        }

                        SendMessagesHelper.prepareSendingDocument(
                                getAccountInstance(),
                                chunk.getAbsolutePath(),
                                chunk.getAbsolutePath(),
                                null,
                                caption,
                                "application/octet-stream",
                                targetDialogId,
                                replyToTopMsg, replyToTopMsg, null, null, null,
                                true, 0, null, null, false
                        );
                    }

                    MiogramCloudVaultEngine.registerFile(vaultFile);
                    MiogramCloudVaultEngine.saveCache(currentAccount);

                    progressDialog.dismiss();
                    Toast.makeText(context, MiogramLocale.get("Файл зашифровано та завантажено!", "Файл зашифрован и загружен!", "File encrypted & uploaded!"), Toast.LENGTH_SHORT).show();
                    filterAndReloadFiles();
                    // Pick up the freshly sent chunk messages so the file is
                    // viewable immediately instead of after the next manual sync.
                    AndroidUtilities.runOnUIThread(() -> syncFromCloud(), 2500);
                    filterAndReloadFiles();
                });

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, MiogramLocale.get("Помилка", "Ошибка", "Error") + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // --- File Download & Decryption Flow ---

    private void ensureFileDownloaded(MiogramCloudVaultFile file, Runnable onReady) {
        if (!TextUtils.isEmpty(file.localPath) && new File(file.localPath).exists()) {
            if (onReady != null) onReady.run();
            return;
        }

        Context context = getParentActivity() != null ? getParentActivity() : getContext();
        if (context == null) return;

        if (file.chunkDocuments.isEmpty()) {
            Toast.makeText(context, MiogramLocale.get("Очікування синхронізації чанків...", "Ожидание синхронизации чанков...", "Awaiting chunks sync..."), Toast.LENGTH_SHORT).show();
            syncFromCloud();
            return;
        }

        final AlertDialog progressDialog = new AlertDialog(context, 3);
        progressDialog.setMessage(MiogramLocale.get("Завантаження чанків з Telegram...", "Загрузка чанков из Telegram...", "Downloading chunks from Telegram..."));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        file.isDownloading = true;
        if (filesAdapter != null) filesAdapter.notifyDataSetChanged();

        Utilities.globalQueue.postRunnable(() -> {
            try {
                ArrayList<File> downloadedChunks = new ArrayList<>();
                for (int i = 0; i < file.chunkDocuments.size(); i++) {
                    TLRPC.Document doc = file.chunkDocuments.get(i);
                    File attachFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, true);
                    if (attachFile == null || !attachFile.exists()) {
                        FileLoader.getInstance(currentAccount).loadFile(doc, null, 0, 0);
                        // Bounded wait: 60s per chunk. The shared globalQueue must
                        // never be parked for minutes by one slow download.
                        int waitedMs = 0;
                        while ((attachFile == null || !attachFile.exists()) && waitedMs < 60000) {
                            Thread.sleep(500);
                            waitedMs += 500;
                            attachFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, true);
                        }
                    }
                    if (attachFile != null && attachFile.exists()) {
                        downloadedChunks.add(attachFile);
                    }
                }

                if (downloadedChunks.size() < file.chunksCount) {
                    throw new IllegalStateException("Not all chunks could be downloaded (" + downloadedChunks.size() + "/" + file.chunksCount + ")");
                }

                AndroidUtilities.runOnUIThread(() -> progressDialog.setMessage(MiogramLocale.get("Розшифрування та збирання файлу...", "Дешифрование и сборка файла...", "Decrypting and reassembling file...")));

                File assembled = MiogramCloudVaultEngine.decryptAndReassembleFile(
                        context, file, downloadedChunks,
                        (progress, status) -> AndroidUtilities.runOnUIThread(() -> progressDialog.setMessage(status))
                );

                AndroidUtilities.runOnUIThread(() -> {
                    file.isDownloading = false;
                    file.localPath = assembled.getAbsolutePath();
                    progressDialog.dismiss();
                    if (filesAdapter != null) filesAdapter.notifyDataSetChanged();
                    if (onReady != null) onReady.run();
                });

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    file.isDownloading = false;
                    progressDialog.dismiss();
                    if (filesAdapter != null) filesAdapter.notifyDataSetChanged();
                    Toast.makeText(context, MiogramLocale.get("Не вдалося завантажити", "Не удалось скачать", "Download failed") + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void downloadOrOpenFile(MiogramCloudVaultFile file) {
        ensureFileDownloaded(file, () -> {
            Context context = getParentActivity() != null ? getParentActivity() : getContext();
            if (context != null && !TextUtils.isEmpty(file.localPath)) {
                Toast.makeText(context, MiogramLocale.get("Збережено в Downloads/Miogram Vault/", "Сохранено в Downloads/Miogram Vault/", "Saved to Downloads/Miogram Vault/"), Toast.LENGTH_LONG).show();
                AndroidUtilities.openForView(new File(file.localPath), file.name, file.mimeType, getParentActivity(), null, false);
            }
        });
    }

    private void shareToTelegramChat(MiogramCloudVaultFile file) {
        ensureFileDownloaded(file, () -> {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putBoolean("canSelectTopics", true);
            DialogsActivity dialogs = new DialogsActivity(args);
            dialogs.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                if (dids != null && !dids.isEmpty()) {
                    for (int i = 0; i < dids.size(); i++) {
                        long did = dids.get(i).dialogId;
                        SendMessagesHelper.prepareSendingDocument(
                                getAccountInstance(),
                                file.localPath,
                                file.localPath,
                                null,
                                file.name,
                                file.mimeType != null ? file.mimeType : "application/octet-stream",
                                did,
                                null, null, null, null, null,
                                true, 0, null, null, false
                        );
                    }
                    fragment1.finishFragment();
                    Toast.makeText(getParentActivity(), MiogramLocale.get("Файл надіслано в чат!", "Файл отправлен в чат!", "File sent to chat!"), Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            presentFragment(dialogs);
        });
    }

    private void shareFileExternal(MiogramCloudVaultFile file) {
        if (getParentActivity() == null) return;
        ensureFileDownloaded(file, () -> {
            try {
                File f = new File(file.localPath);
                Uri uri = FileProvider.getUriForFile(
                        ApplicationLoader.applicationContext,
                        ApplicationLoader.getApplicationId() + ".provider",
                        f
                );
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(file.mimeType != null ? file.mimeType : "application/octet-stream");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getParentActivity().startActivity(Intent.createChooser(intent, MiogramLocale.get("Поділитися файлом", "Поделиться файлом", "Share File")));
            } catch (Exception e) {
                FileLog.e(e);
                Toast.makeText(getParentActivity(), MiogramLocale.get("Помилка", "Ошибка", "Error") + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMoveFileToTopicDialog(MiogramCloudVaultFile file) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Перемістити в папку", "Переместить в папку", "Move to folder"));

        ArrayList<String> topicNames = new ArrayList<>();
        ArrayList<Long> topicIds = new ArrayList<>();

        topicNames.add(MiogramLocale.get("Головна папка (Усі файли)", "Корень (Все файлы)", "Root (All files)"));
        topicIds.add(0L);

        for (TLRPC.TL_forumTopic topic : cachedTopics) {
            topicNames.add(topic.title);
            topicIds.add((long) topic.id);
        }

        CharSequence[] items = topicNames.toArray(new CharSequence[0]);
        builder.setItems(items, (d, which) -> {
            long newTopicId = topicIds.get(which);
            String newTopicName = which == 0 ? "" : topicNames.get(which);
            file.topicId = newTopicId;
            file.topicName = newTopicName;
            MiogramCloudVaultEngine.saveCache(currentAccount);
            filterAndReloadFiles();
            Toast.makeText(getParentActivity(), MiogramLocale.get("Файл переміщено!", "Файл перемещен!", "File moved!"), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    // --- Vault Management & Dialogs ---

    /** Shown when the user tries to upload before creating/linking a vault chat. */
    private void showVaultRequiredDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Сховище не підключено", "Хранилище не подключено", "Vault not linked"));
        builder.setMessage(MiogramLocale.get("Створіть нове сховище або прив'яжіть існуючу супергрупу, інакше файлу нікуди завантажуватись.", "Создайте новое хранилище или привяжите существующую супергруппу, иначе файлу некуда загружаться.", "Create a new vault or link an existing supergroup first — otherwise there is nowhere to upload."));
        builder.setPositiveButton(MiogramLocale.get("Створити сховище", "Создать хранилище", "Create vault"), (d, w) -> createVaultAutomatically());
        builder.setNeutralButton(MiogramLocale.get("Прив'язати", "Привязать", "Link existing"), (d, w) -> showLinkExistingDialog());
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void createVaultAutomatically() {
        if (getParentActivity() == null) return;
        final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
        progressDialog.setMessage(MiogramLocale.get("Створення форум-супергрупи...", "Создание форум-супергруппы...", "Creating forum supergroup..."));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        MiogramCloudVaultEngine.createVaultSupergroup(this, currentAccount, new MiogramCloudVaultEngine.VaultCreatedCallback() {
            @Override
            public void onCreated(long chatId) {
                progressDialog.dismiss();
                Toast.makeText(getParentActivity(), MiogramLocale.get("Сховище успішно створено!", "Хранилище успешно создано!", "Vault created successfully!"), Toast.LENGTH_SHORT).show();
                updateVaultVisibility();
                loadTopicsFromTelegram();
                filterAndReloadFiles();
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(getParentActivity(), MiogramLocale.get("Помилка", "Ошибка", "Error") + (message != null ? ": " + message : ""), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLinkExistingDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Прив'язати супергрупу", "Привязать супергруппу", "Link Supergroup"));
        builder.setMessage(MiogramLocale.get("Введіть ID супергрупи (без знаку мінус):", "Введите ID супергруппы (без знака минус):", "Enter Supergroup ID (without minus sign):"));

        final EditText input = new EditText(getParentActivity());
        input.setSingleLine(true);
        FrameLayout container = new FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            String str = input.getText().toString().trim().replace("-", "");
            try {
                long id = Long.parseLong(str);
                MiogramCloudVaultEngine.setVaultChatId(currentAccount, id);
                updateVaultVisibility();
                loadTopicsFromTelegram();
                syncFromCloud();
            } catch (Exception e) {
                Toast.makeText(getParentActivity(), MiogramLocale.get("Невірний ID", "Неверный ID", "Invalid ID"), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openVaultChat() {
        long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
        if (vaultChatId == 0) return;
        Bundle args = new Bundle();
        args.putLong("chat_id", vaultChatId);
        presentFragment(new ChatActivity(args));
    }

    private void showMasterKeyDialog() {
        if (getParentActivity() == null) return;
        String hex = MiogramCloudVaultEngine.getMasterKeyHex();

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Майстер-ключ AES-256", "Мастер-ключ AES-256", "AES-256 Master Key"));
        builder.setMessage(MiogramLocale.get(
                "Цей 256-бітний ключ використовується для клієнтського шифрування та дешифрування всіх файлів і маніфестів.\n\n"
                        + "Збережіть його в надійному місці, щоб мати доступ до файлів з інших пристроїв:\n\n" + hex,
                "Этот 256-битный ключ используется для клиентского шифрования и дешифрования всех файлов и манифестов.\n\n"
                        + "Сохраните его в надежном месте для доступа к файлам с других устройств:\n\n" + hex,
                "This 256-bit key is used for client-side encryption and decryption of all files and manifests.\n\n"
                        + "Back it up securely to access your files from other devices:\n\n" + hex
        ));

        builder.setPositiveButton(MiogramLocale.get("Скопіювати ключ", "Скопировать ключ", "Copy Key"), (d, w) -> {
            ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Vault Key", hex));
                Toast.makeText(getParentActivity(), MiogramLocale.get("Ключ скопійовано!", "Ключ скопирован!", "Key copied!"), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNeutralButton(MiogramLocale.get("Ввести інший", "Ввести другой", "Enter Custom"), (d, w) -> showEnterCustomKeyDialog());
        builder.setNegativeButton(LocaleController.getString(R.string.Close), null);
        showDialog(builder.create());
    }

    private void showEnterCustomKeyDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Введіть ключ шифрування", "Введите ключ шифрования", "Enter Encryption Key"));
        builder.setMessage(MiogramLocale.get("Введіть 64-символьний Hex-ключ (256 біт):", "Введите 64-символьный Hex-ключ (256 бит):", "Enter a 64-char hex key (256-bit):"));

        final EditText input = new EditText(getParentActivity());
        input.setSingleLine(true);
        FrameLayout container = new FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.Save), (d, w) -> {
            String hex = input.getText().toString().trim();
            if (hex.matches("(?i)[0-9a-f]{64}")) {
                MiogramCloudVaultEngine.setMasterKeyHex(hex);
                Toast.makeText(getParentActivity(), MiogramLocale.get("Ключ оновлено!", "Ключ обновлен!", "Key updated!"), Toast.LENGTH_SHORT).show();
                syncFromCloud();
            } else {
                Toast.makeText(getParentActivity(), MiogramLocale.get("Ключ — рівно 64 hex-символи (0-9, a-f)!", "Ключ — ровно 64 hex-символа (0-9, a-f)!", "Key must be exactly 64 hex chars (0-9, a-f)!"), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showUnlinkDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Відв'язати супергрупу?", "Отвязать супергруппу?", "Unlink Supergroup?"));
        builder.setMessage(MiogramLocale.get(
                "Супергрупа в Telegram залишиться недоторканою, але цей клієнт перейде в стан налаштування.",
                "Супергруппа в Telegram останется нетронутой, но этот клиент перейдет в состояние настройки.",
                "The Telegram supergroup will remain untouched, but this client will return to setup state."
        ));
        builder.setPositiveButton(MiogramLocale.get("Відв'язати", "Отвязать", "Unlink"), (d, w) -> {
            MiogramCloudVaultEngine.setVaultChatId(currentAccount, 0);
            updateVaultVisibility();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showFileOptions(MiogramCloudVaultFile file) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(file.name);

        CharSequence[] items = new CharSequence[]{
                MiogramLocale.get("Переглянути / Відкрити", "Просмотреть / Открыть", "Preview / Open"),
                MiogramLocale.get("Поділитися в чат Telegram", "Поделиться в чат Telegram", "Share to Telegram Chat"),
                MiogramLocale.get("Поділитися через інші додатки", "Поделиться через другие приложения", "Share via Other Apps"),
                MiogramLocale.get("Перемістити в папку (топік)", "Переместить в папку (топик)", "Move to Folder (Topic)"),
                MiogramLocale.get("Копіювати назву", "Копировать название", "Copy Name"),
                MiogramLocale.get("Видалити зі сховища", "Удалить из хранилища", "Delete from Vault")
        };

        builder.setItems(items, (d, which) -> {
            if (which == 0) {
                downloadOrOpenFile(file);
            } else if (which == 1) {
                shareToTelegramChat(file);
            } else if (which == 2) {
                shareFileExternal(file);
            } else if (which == 3) {
                showMoveFileToTopicDialog(file);
            } else if (which == 4) {
                ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Filename", file.name));
                    Toast.makeText(getParentActivity(), MiogramLocale.get("Назву скопійовано", "Название скопировано", "Name copied"), Toast.LENGTH_SHORT).show();
                }
            } else if (which == 5) {
                long vaultChatId = MiogramCloudVaultEngine.getVaultChatId(currentAccount);
                MiogramCloudVaultEngine.deleteVaultFile(currentAccount, vaultChatId, file, true);
                filterAndReloadFiles();
                Toast.makeText(getParentActivity(), MiogramLocale.get("Файл видалено", "Файл удален", "File deleted"), Toast.LENGTH_SHORT).show();
            }
        });
        showDialog(builder.create());
    }

    // --- RecyclerView Adapter ---

    private class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return currentViewMode == VIEW_TYPE_GRID ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_GRID) {
                return new GridViewHolder(new VaultGridCell(parent.getContext()));
            } else {
                return new ListViewHolder(new VaultFileCell(parent.getContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MiogramCloudVaultFile file = displayedFiles.get(position);
            if (holder instanceof GridViewHolder) {
                ((GridViewHolder) holder).cell.bind(file);
            } else if (holder instanceof ListViewHolder) {
                ((ListViewHolder) holder).cell.bind(file);
            }
        }

        @Override
        public int getItemCount() {
            return displayedFiles.size();
        }
    }

    private static class ListViewHolder extends RecyclerView.ViewHolder {
        VaultFileCell cell;
        public ListViewHolder(VaultFileCell cell) {
            super(cell);
            this.cell = cell;
        }
    }

    private static class GridViewHolder extends RecyclerView.ViewHolder {
        VaultGridCell cell;
        public GridViewHolder(VaultGridCell cell) {
            super(cell);
            this.cell = cell;
        }
    }

    // --- Modern Vault Gallery Grid Cell (1:1 Square) ---

    private class VaultGridCell extends FrameLayout {

        private final BackupImageView imageView;
        private final FrameLayout iconBox;
        private final ImageView iconView;
        private final TextView extBadge;
        private final TextView nameView;
        private final TextView sizeView;
        private final View gradientScrim;
        private final LinearLayout videoBadge;
        private final ImageView statusIcon;
        private final ProgressBar progressBar;
        private MiogramCloudVaultFile currentFile;

        public VaultGridCell(Context context) {
            super(context);

            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(12));
                }
            });
            setClipToOutline(true);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            // 1. Thumbnail Image (Covering whole card)
            imageView = new BackupImageView(context);
            imageView.setAspectFit(false);
            addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            // 2. Centered Icon Box (For non-images or files without thumbs)
            iconBox = new FrameLayout(context);
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            iconBox.setBackground(circleBg);

            iconView = new ImageView(context);
            iconView.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
            iconBox.addView(iconView, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
            addView(iconBox, LayoutHelper.createFrame(44, 44, Gravity.CENTER));

            // 3. File extension badge (top-left for non-media)
            extBadge = new TextView(context);
            extBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            extBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            extBadge.setTextColor(Color.WHITE);
            extBadge.setGravity(Gravity.CENTER);
            GradientDrawable extBg = new GradientDrawable();
            extBg.setColor(0xBB000000);
            extBg.setCornerRadius(AndroidUtilities.dp(4));
            extBadge.setBackground(extBg);
            extBadge.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(2), AndroidUtilities.dp(5), AndroidUtilities.dp(2));
            addView(extBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 8, 8, 0, 0));

            // 4. Video badge (top-left for videos)
            videoBadge = new LinearLayout(context);
            videoBadge.setOrientation(LinearLayout.HORIZONTAL);
            videoBadge.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable vidBg = new GradientDrawable();
            vidBg.setColor(0x99000000);
            vidBg.setCornerRadius(AndroidUtilities.dp(4));
            videoBadge.setBackground(vidBg);
            videoBadge.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));

            ImageView playIco = new ImageView(context);
            playIco.setImageResource(R.drawable.msg_video);
            playIco.setColorFilter(Color.WHITE);
            videoBadge.addView(playIco, LayoutHelper.createLinear(12, 12, Gravity.CENTER_VERTICAL, 0, 0, 3, 0));

            TextView vidLabel = new TextView(context);
            vidLabel.setText("VIDEO");
            vidLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            vidLabel.setTextColor(Color.WHITE);
            vidLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            videoBadge.addView(vidLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
            addView(videoBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 8, 8, 0, 0));

            // 5. Dark gradient scrim at bottom
            gradientScrim = new View(context);
            GradientDrawable scrimDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{0xDD000000, 0x66000000, 0x00000000}
            );
            gradientScrim.setBackground(scrimDrawable);
            addView(gradientScrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.BOTTOM));

            // 6. Text container at bottom
            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(6));

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameView.setTextColor(Color.WHITE);
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            textLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 1));

            sizeView = new TextView(context);
            sizeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            sizeView.setTextColor(0xCCFFFFFF);
            sizeView.setSingleLine(true);
            textLayout.addView(sizeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

            // 7. Top-right status icon
            FrameLayout statusContainer = new FrameLayout(context);
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setShape(GradientDrawable.OVAL);
            statusBg.setColor(0x88000000);
            statusContainer.setBackground(statusBg);

            statusIcon = new ImageView(context);
            statusIcon.setColorFilter(Color.WHITE);
            statusContainer.addView(statusIcon, LayoutHelper.createFrame(14, 14, Gravity.CENTER));
            addView(statusContainer, LayoutHelper.createFrame(24, 24, Gravity.TOP | Gravity.END, 0, 8, 8, 0));

            progressBar = new ProgressBar(context);
            progressBar.setVisibility(View.GONE);
            addView(progressBar, LayoutHelper.createFrame(24, 24, Gravity.TOP | Gravity.END, 0, 8, 8, 0));

            setOnClickListener(v -> {
                if (currentFile != null) downloadOrOpenFile(currentFile);
            });

            setOnLongClickListener(v -> {
                if (currentFile != null) {
                    showFileOptions(currentFile);
                    return true;
                }
                return false;
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec); // Square 1:1 Aspect Ratio
        }

        public void bind(MiogramCloudVaultFile file) {
            this.currentFile = file;
            nameView.setText(file.name);
            sizeView.setText(file.getFormattedSize());

            boolean hasLocal = !TextUtils.isEmpty(file.localPath) && new File(file.localPath).exists();
            boolean isMedia = file.isMedia();

            videoBadge.setVisibility(file.isVideo() ? View.VISIBLE : View.GONE);
            extBadge.setVisibility((!isMedia && !TextUtils.isEmpty(file.getFileExtension())) ? View.VISIBLE : View.GONE);
            if (extBadge.getVisibility() == View.VISIBLE) {
                extBadge.setText(file.getFileExtension().toUpperCase());
            }

            boolean hasThumb = false;
            if (isMedia) {
                if (hasLocal) {
                    imageView.setImage(ImageLocation.getForPath(file.localPath), "200_200", null, null, null);
                    hasThumb = true;
                } else if (!file.chunkDocuments.isEmpty()) {
                    TLRPC.Document doc = file.chunkDocuments.get(0);
                    if (doc.thumbs != null && !doc.thumbs.isEmpty()) {
                        TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 320);
                        imageView.setImage(ImageLocation.getForDocument(thumb, doc), "200_200", null, null, null);
                        hasThumb = true;
                    }
                }
            }

            if (hasThumb) {
                imageView.setVisibility(View.VISIBLE);
                iconBox.setVisibility(View.GONE);
                gradientScrim.setVisibility(View.VISIBLE);
                nameView.setTextColor(Color.WHITE);
                sizeView.setTextColor(0xCCFFFFFF);
            } else {
                imageView.setVisibility(View.GONE);
                iconBox.setVisibility(View.VISIBLE);
                iconView.setImageResource(file.getIconRes());
                gradientScrim.setVisibility(View.GONE);
                nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                sizeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }

            if (file.isDownloading) {
                progressBar.setVisibility(View.VISIBLE);
                statusIcon.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                statusIcon.setVisibility(View.VISIBLE);
                if (hasLocal) {
                    statusIcon.setImageResource(R.drawable.baseline_check_24);
                } else {
                    statusIcon.setImageResource(R.drawable.baseline_cloud_download_24);
                }
            }
        }
    }

    // --- Vault File Item View ---

    private class VaultFileCell extends FrameLayout {

        private final ImageView iconView;
        private final TextView nameView;
        private final TextView infoView;
        private final ImageView actionButton;
        private final ProgressBar progressBar;
        private MiogramCloudVaultFile currentFile;

        public VaultFileCell(Context context) {
            super(context);
            setBackground(Theme.getSelectorDrawable(false));
            setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(72)));

            // Left icon container
            FrameLayout iconBox = new FrameLayout(context);
            GradientDrawable boxBg = new GradientDrawable();
            boxBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            boxBg.setCornerRadius(AndroidUtilities.dp(12));
            iconBox.setBackground(boxBg);

            iconView = new ImageView(context);
            iconView.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
            iconBox.addView(iconView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));

            addView(iconBox, LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL | Gravity.START, 14, 0, 0, 0));

            // Texts
            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            textLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 3));

            infoView = new TextView(context);
            infoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            infoView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            infoView.setSingleLine(true);
            infoView.setEllipsize(TextUtils.TruncateAt.END);
            textLayout.addView(infoView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 74, 0, 56, 0));

            // Right action / download button
            actionButton = new ImageView(context);
            actionButton.setScaleType(ImageView.ScaleType.CENTER);
            actionButton.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
            actionButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
            actionButton.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            addView(actionButton, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 10, 0));

            progressBar = new ProgressBar(context);
            progressBar.setVisibility(View.GONE);
            addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 14, 0));

            setOnClickListener(v -> {
                if (currentFile != null) {
                    MiogramHaptic.tap(v);
                    downloadOrOpenFile(currentFile);
                }
            });

            setOnLongClickListener(v -> {
                if (currentFile != null) {
                    MiogramHaptic.tap(v);
                    showFileOptions(currentFile);
                    return true;
                }
                return false;
            });

            actionButton.setOnClickListener(v -> {
                if (currentFile != null) {
                    MiogramHaptic.tap(v);
                    showFileOptions(currentFile);
                }
            });
        }

        public void bind(MiogramCloudVaultFile file) {
            this.currentFile = file;
            nameView.setText(file.name);
            setContentDescription(file.name);

            String chunksInfo = file.chunksCount > 1 ? (" • " + file.chunksCount + " " + MiogramLocale.get("частин", "частей", "parts")) : "";
            String topicInfo = !TextUtils.isEmpty(file.topicName) ? (" • " + file.topicName) : "";
            String cloudState;
            if (!TextUtils.isEmpty(file.localPath) && new File(file.localPath).exists()) {
                cloudState = " • " + MiogramLocale.get("на пристрої", "на устройстве", "on device");
            } else if (!file.chunkDocuments.isEmpty()) {
                cloudState = " • " + MiogramLocale.get("в хмарі", "в облаке", "in cloud");
            } else {
                cloudState = " • " + MiogramLocale.get("очікує синхронізації", "ожидает синхронизации", "pending sync");
            }
            infoView.setText(file.getFormattedSize() + chunksInfo + topicInfo + cloudState + " • " + file.getFormattedDate());

            iconView.setImageResource(file.getIconRes());

            if (file.isDownloading) {
                progressBar.setVisibility(View.VISIBLE);
                actionButton.setVisibility(View.GONE);
                actionButton.setContentDescription(null);
            } else {
                progressBar.setVisibility(View.GONE);
                actionButton.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(file.localPath) && new File(file.localPath).exists()) {
                    actionButton.setImageResource(R.drawable.baseline_check_24);
                    actionButton.setContentDescription(MiogramLocale.get("Файл збережено, відкрити опції", "Файл сохранён, открыть опции", "File saved, open options"));
                } else {
                    actionButton.setImageResource(R.drawable.baseline_cloud_download_24);
                    actionButton.setContentDescription(MiogramLocale.get("Завантажити з хмари", "Скачать из облака", "Download from cloud"));
                }
            }
        }
    }
}
