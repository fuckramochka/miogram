package app.miogram.bridge.presence;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.discord.MiogramDiscordManager;
import app.miogram.bridge.github.MiogramGitHubManager;
import app.miogram.bridge.spotify.MiogramSpotifyManager;
import app.miogram.bridge.spotify.MiogramSpotifySheet;
import app.miogram.bridge.steam.MiogramSteamManager;
import app.miogram.bridge.steam.MiogramSteamSheet;

/**
 * Unified Hub for Connected Platforms (Steam, GitHub, Discord, Spotify).
 * Zero tacky emojis in buttons — pure Durov-grade typography & micro-interactions.
 */
public class MiogramConnectedAppsSheet extends BottomSheet {

    public interface OnAppsChangedListener {
        void onAppsChanged();
    }

    private final Theme.ResourcesProvider resourcesProvider;
    private final LinearLayout cardsContainer;
    private OnAppsChangedListener listener;

    public MiogramConnectedAppsSheet(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.resourcesProvider = resourcesProvider;

        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        int bgColor = getThemedColor(Theme.key_dialogBackground);
        if (bgColor == 0) bgColor = 0xFF101822;
        fixNavigationBar(bgColor);

        int textColor = getThemedColor(Theme.key_dialogTextBlack);
        if (textColor == 0) textColor = 0xFFFFFFFF;
        int subTextColor = getThemedColor(Theme.key_dialogTextGray2);
        if (subTextColor == 0) subTextColor = 0xAAFFFFFF;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(28));

        // Drag Handle
        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x44888888);
        handleBg.setCornerRadius(AndroidUtilities.dp(2.5f));
        dragHandle.setBackground(handleBg);
        root.addView(dragHandle, LayoutHelper.createLinear(38, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(MiogramLocale.get("Прив'язані додатки ໒꒱", "Привязанные приложения ໒꒱", "Connected Apps ໒꒱"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(textColor);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // Subtitle
        TextView subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get(
                "Керування платформами та синхронізація вашої присутності у профілі",
                "Управление платформами и синхронизация вашего присутствия в профиле",
                "Manage linked platforms and live digital presence in your profile"
        ));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(subTextColor);
        subtitleView.setGravity(Gravity.CENTER);
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        cardsContainer = new LinearLayout(context);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(cardsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(root);
        setCustomView(scrollView);

        buildCards();
    }

    public void setOnAppsChangedListener(OnAppsChangedListener listener) {
        this.listener = listener;
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onAppsChanged();
        }
    }

    private void buildCards() {
        cardsContainer.removeAllViews();
        Context context = getContext();

        // 1. Steam Card
        buildSteamCard(context);

        // 2. GitHub Card
        buildGitHubCard(context);

        // 3. Discord Card
        buildDiscordCard(context);

        // 4. Spotify Card
        buildSpotifyCard(context);
    }

    private LinearLayout createCardContainer(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setStroke(AndroidUtilities.dp(1), 0x22FFFFFF);
        card.setBackground(bg);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        return card;
    }

    // STEAM
    private void buildSteamCard(Context context) {
        LinearLayout card = createCardContainer(context);
        cardsContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        MiogramSteamManager sm = MiogramSteamManager.getInstance();
        boolean linked = sm.isLinked();

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView name = new TextView(context);
        name.setText("Steam");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFF66C0F4);
        headerRow.addView(name, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView badge = createStatusBadge(context, linked);
        headerRow.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(0xAAFFFFFF);
        desc.setSingleLine(true);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        if (linked) {
            String steamId = sm.getLinkedSteamId();
            desc.setText(MiogramLocale.get("Прив'язаний ID: ", "Привязанный ID: ", "Linked ID: ") + steamId);
        } else {
            desc.setText(MiogramLocale.get("Трансляція ігор, статистика та статус", "Трансляция игр, статистика и статус", "Live game broadcasts, stats & status"));
        }
        card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnManage = createButton(context, linked
                ? MiogramLocale.get("Налаштувати", "Настроить", "Configure")
                : MiogramLocale.get("Підключити", "Подключить", "Connect"), 0x3366C0F4, 0xFF66C0F4);
        btnManage.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            new MiogramSteamSheet(context, resourcesProvider).show();
            dismiss();
        });
        actions.addView(btnManage, LayoutHelper.createLinear(0, 34, 1f, 0, 0, linked ? 6 : 0, 0));

        if (linked) {
            TextView btnUnlink = createButton(context, MiogramLocale.get("Відв'язати", "Отвязать", "Unlink"), 0x22FF4B4B, 0xFFFF6B6B);
            btnUnlink.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                sm.setLinkedSteamId("");
                buildCards();
                notifyChanged();
            });
            actions.addView(btnUnlink, LayoutHelper.createLinear(0, 34, 1f, 0, 0, 0, 0));
        }
    }

    // GITHUB
    private void buildGitHubCard(Context context) {
        LinearLayout card = createCardContainer(context);
        cardsContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        MiogramGitHubManager gm = MiogramGitHubManager.getInstance();
        boolean linked = gm.isLinked();

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView name = new TextView(context);
        name.setText("GitHub");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFFFFFFFF);
        headerRow.addView(name, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView badge = createStatusBadge(context, linked);
        headerRow.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(0xAAFFFFFF);
        desc.setSingleLine(true);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        if (linked) {
            desc.setText("@" + gm.getLinkedUsername() + " — " + MiogramLocale.get("Акаунт верифіковано", "Аккаунт верифицирован", "Account verified"));
        } else {
            desc.setText(MiogramLocale.get("Прив'язка персонального акаунта та активності", "Привязка личного аккаунта и активности", "Link personal profile & push activity"));
        }
        card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnManage = createButton(context, linked
                ? MiogramLocale.get("Змінити акаунт", "Изменить аккаунт", "Change Account")
                : MiogramLocale.get("Підключити", "Подключить", "Connect"), 0x2AFFFFFF, 0xFFFFFFFF);
        btnManage.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            gm.showLinkUserDialog(context, user -> {
                buildCards();
                notifyChanged();
            });
        });
        actions.addView(btnManage, LayoutHelper.createLinear(0, 34, 1f, 0, 0, linked ? 6 : 0, 0));

        if (linked) {
            TextView btnUnlink = createButton(context, MiogramLocale.get("Відв'язати", "Отвязать", "Unlink"), 0x22FF4B4B, 0xFFFF6B6B);
            btnUnlink.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                gm.setLinkedUsername("");
                buildCards();
                notifyChanged();
            });
            actions.addView(btnUnlink, LayoutHelper.createLinear(0, 34, 1f, 0, 0, 0, 0));
        }
    }

    // DISCORD
    private void buildDiscordCard(Context context) {
        LinearLayout card = createCardContainer(context);
        cardsContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        MiogramDiscordManager dm = MiogramDiscordManager.getInstance();
        boolean linked = dm.isLinked();

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView name = new TextView(context);
        name.setText("Discord");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFF5865F2);
        headerRow.addView(name, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView badge = createStatusBadge(context, linked);
        headerRow.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(0xAAFFFFFF);
        desc.setSingleLine(true);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        if (linked) {
            desc.setText("User ID: " + dm.getLinkedUserId());
        } else {
            desc.setText(MiogramLocale.get("Трансляція статусу та активності Lanyard", "Трансляция статуса и активности Lanyard", "Live Lanyard presence & activity"));
        }
        card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnManage = createButton(context, linked
                ? MiogramLocale.get("Змінити ID", "Изменить ID", "Change ID")
                : MiogramLocale.get("Підключити", "Подключить", "Connect"), 0x335865F2, 0xFFFFFFFF);
        btnManage.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            dm.showConfigDialog(context, presence -> {
                buildCards();
                notifyChanged();
            });
        });
        actions.addView(btnManage, LayoutHelper.createLinear(0, 34, 1f, 0, 0, linked ? 6 : 0, 0));

        if (linked) {
            TextView btnUnlink = createButton(context, MiogramLocale.get("Відв'язати", "Отвязать", "Unlink"), 0x22FF4B4B, 0xFFFF6B6B);
            btnUnlink.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                dm.setLinkedUserId("");
                buildCards();
                notifyChanged();
            });
            actions.addView(btnUnlink, LayoutHelper.createLinear(0, 34, 1f, 0, 0, 0, 0));
        }
    }

    // SPOTIFY
    private void buildSpotifyCard(Context context) {
        LinearLayout card = createCardContainer(context);
        cardsContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        MiogramSpotifyManager spm = MiogramSpotifyManager.getInstance();
        boolean linked = spm.isLinked();

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView name = new TextView(context);
        name.setText("Spotify");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFF1DB954);
        headerRow.addView(name, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView badge = createStatusBadge(context, linked);
        headerRow.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(0xAAFFFFFF);
        desc.setSingleLine(true);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        if (spm.isPlaying()) {
            desc.setText("🟢 " + spm.getCurrentTrack() + " — " + spm.getCurrentArtist());
        } else if (linked) {
            desc.setText(MiogramLocale.get("Міст активний (очікування відтворення)", "Мост активен (ожидание трека)", "Bridge active (awaiting playback)"));
        } else {
            desc.setText(MiogramLocale.get("Працює на Free та Premium через трансляцію", "Работает на Free и Premium через трансляцию", "Works on Free & Premium via Broadcast"));
        }
        card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnToggle = createButton(context, linked
                ? MiogramLocale.get("Вимкнути міст", "Отключить мост", "Disable Bridge")
                : MiogramLocale.get("Увімкнути міст", "Включить мост", "Enable Bridge"),
                linked ? 0x22FF4B4B : 0x331DB954,
                linked ? 0xFFFF6B6B : 0xFF1DB954);
        btnToggle.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            spm.setBridgeEnabled(!linked);
            buildCards();
            notifyChanged();
        });
        actions.addView(btnToggle, LayoutHelper.createLinear(0, 34, 1f, 0, 0, 6, 0));

        TextView btnGuide = createButton(context, MiogramLocale.get("Інструкція", "Инструкция", "Setup Guide"), 0x2AFFFFFF, 0xFFD2DBE3);
        btnGuide.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            new MiogramSpotifySheet(context, resourcesProvider).show();
            dismiss();
        });
        actions.addView(btnGuide, LayoutHelper.createLinear(0, 34, 1f, 0, 0, 0, 0));
    }

    private TextView createStatusBadge(Context context, boolean active) {
        TextView badge = new TextView(context);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(6));
        if (active) {
            badge.setText(MiogramLocale.get("ПІДКЛЮЧЕНО", "ПОДКЛЮЧЕНО", "CONNECTED"));
            badge.setTextColor(0xFFFFFFFF);
            bg.setColor(0xFF238636);
        } else {
            badge.setText(MiogramLocale.get("НЕ ПІДКЛЮЧЕНО", "НЕ ПОДКЛЮЧЕНО", "NOT LINKED"));
            badge.setTextColor(0x88FFFFFF);
            bg.setColor(0x22FFFFFF);
        }
        badge.setBackground(bg);
        return badge;
    }

    private static TextView createButton(Context context, String text, int bgColor, int textColor) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(AndroidUtilities.dp(8));
        btn.setBackground(bg);

        ScaleStateListAnimator.apply(btn, 0.035f, 1.4f);
        return btn;
    }
}
