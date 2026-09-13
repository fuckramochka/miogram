package app.miogram.bridge.spotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Interactive Spotify Bridge Hub for Miogram.
 * Displays live Spotify playback status, instructions on enabling
 * Spotify Android Device Broadcast Status, and quick navigation.
 */
public class MiogramSpotifySheet extends BottomSheet {

    private static final int SPOTIFY_GREEN = 0xFF1DB954;

    private final Theme.ResourcesProvider resourcesProvider;
    private final MiogramSpotifyManager spotifyManager;

    private LinearLayout statusCard;
    private TextView statusIndicator;
    private TextView trackTitleView;
    private TextView trackArtistView;
    private TextView lyricsPreviewView;

    public MiogramSpotifySheet(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.resourcesProvider = resourcesProvider;
        this.spotifyManager = MiogramSpotifyManager.getInstance();

        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        int bgColor = getThemedColor(Theme.key_dialogBackground);
        if (bgColor == 0) bgColor = 0xFF181A22;
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
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(24));

        // Drag Handle
        View dragHandle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x44888888);
        handleBg.setCornerRadius(AndroidUtilities.dp(2.5f));
        dragHandle.setBackground(handleBg);
        root.addView(dragHandle, LayoutHelper.createLinear(38, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(MiogramLocale.get("Spotify Міст ໒꒱", "Spotify Мост ໒꒱", "Spotify Bridge ໒꒱"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(textColor);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // Subtitle
        TextView subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get("Синхронізація музики, живі слова в чаті та ШІ-інструменти",
                "Синхронизация музыки, живые слова в чате и ИИ-инструменты",
                "Music synchronization, live chat lyrics & AI companion tools"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(subTextColor);
        subtitleView.setGravity(Gravity.CENTER);
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // Live Status Card
        statusCard = new LinearLayout(context);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        updateStatusCardAppearance();

        root.addView(statusCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Explanation / Feature List Section
        addSectionHeader(root, MiogramLocale.get("ДЕ ПРАЦЮЄ SPOTIFY В MIOGRAM", "ГДЕ РАБОТАЕТ SPOTIFY В MIOGRAM", "WHERE SPOTIFY WORKS IN MIOGRAM"), textColor);

        // Feature 1: Chat input lyrics
        addFeatureRow(root, context, "💬",
                MiogramLocale.get("Живі слова у рядку вводу", "Живые слова в строке ввода", "Live lyrics in chat input"),
                MiogramLocale.get("Коли у вас грає Spotify, замість тексту «Напишіть повідомлення...» автоматично транслюються слова поточної пісні в реальному часі.",
                        "Когда у вас играет Spotify, вместо текста «Напишите сообщение...» автоматически транслируются слова текущей песни в реальном времени.",
                        "When Spotify is playing, the message input hint shows live synchronized lyrics of the current song."));

        // Feature 2: AI Companion
        addFeatureRow(root, context, "🤖",
                MiogramLocale.get("ШІ-Компаньйон (Ame / K-Angel)", "ИИ-Компаньон (Ame / K-Angel)", "AI Companion (Ame / K-Angel)"),
                MiogramLocale.get("Кнопка «🎵 Зараз грає» у меню ШІ або пряме запитання «що грає в Spotify» дозволяє ШІ побачити трек, оцінити його або поділитися лінком.",
                        "Кнопка «🎵 Сейчас играет» в меню ИИ или прямой вопрос «что играет в Spotify» позволяет ИИ увидеть трек, оценить его или поделиться ссылкой.",
                        "Tap the '🎵 Now playing' chip or ask AI 'what's playing on Spotify' to inspect or share the active song."));

        // Feature 3: Player menu
        addFeatureRow(root, context, "🎧",
                MiogramLocale.get("Пошук треків у Spotify з плеєра", "Поиск треков в Spotify из плеера", "Find Telegram tracks in Spotify"),
                MiogramLocale.get("У меню трьох крапок будь-якого аудіофайлу в плеєрі Telegram є опція «Знайти в Spotify».",
                        "В меню трех точек любого аудиофайла в плеере Telegram есть опция «Найти в Spotify».",
                        "Any audio message in the player has a 'Find in Spotify' action in its options menu."));

        // Setup instructions block
        addSectionHeader(root, MiogramLocale.get("ЯК УВІМКНУТИ СИНХРОНІЗАЦІЮ", "КАК ВКЛЮЧИТЬ СИНХРОНИЗАЦИЮ", "HOW TO ENABLE BROADCAST"), textColor);

        TextView instructions = new TextView(context);
        instructions.setText(MiogramLocale.get(
                "1. Відкрийте офіційний додаток Spotify на телефоні.\n" +
                "2. Натисніть «Налаштування» ⚙️ (профіль або шестерня вгорі).\n" +
                "3. Прокрутіть до розділу «Пристрої» або «Конфіденційність».\n" +
                "4. Увімкніть тумблер «Статус трансляції пристрою» (Device Broadcast Status).\n" +
                "5. Запустіть будь-яку пісню — Miogram одразу її підхопить!",
                "1. Откройте официальное приложение Spotify на телефоне.\n" +
                "2. Нажмите «Настройки» ⚙️ (профиль или шестеренка вверху).\n" +
                "3. Прокрутите к разделу «Устройства» или «Конфиденциальность».\n" +
                "4. Включите тумблер «Статус трансляции устройства» (Device Broadcast Status).\n" +
                "5. Запустите любую песню — Miogram сразу ее подхватит!",
                "1. Open the Spotify app on your device.\n" +
                "2. Go to Settings ⚙️.\n" +
                "3. Scroll to Devices or Privacy.\n" +
                "4. Enable 'Device Broadcast Status' (Allow other apps to see playback).\n" +
                "5. Play any song — Miogram will instantly pick it up!"
        ));
        instructions.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        instructions.setTextColor(subTextColor);
        instructions.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        instructions.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(16));
        root.addView(instructions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Open Spotify action button
        TextView openSpotifyBtn = new TextView(context);
        openSpotifyBtn.setText(MiogramLocale.get("Відкрити додаток Spotify ↗", "Открыть приложение Spotify ↗", "Open Spotify App ↗"));
        openSpotifyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        openSpotifyBtn.setTextColor(0xFFFFFFFF);
        openSpotifyBtn.setTypeface(AndroidUtilities.bold());
        openSpotifyBtn.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(SPOTIFY_GREEN);
        btnBg.setCornerRadius(AndroidUtilities.dp(16));
        openSpotifyBtn.setBackground(btnBg);
        openSpotifyBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        openSpotifyBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            spotifyManager.openInSpotify(context);
        });
        root.addView(openSpotifyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Done button
        TextView doneBtn = new TextView(context);
        doneBtn.setText(MiogramLocale.get("Зрозуміло", "Понятно", "Got it"));
        doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneBtn.setTextColor(subTextColor);
        doneBtn.setGravity(Gravity.CENTER);
        doneBtn.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(4));
        doneBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            dismiss();
        });
        root.addView(doneBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(root);
        setCustomView(scrollView);
    }

    private void updateStatusCardAppearance() {
        Context context = getContext();
        boolean isPlaying = spotifyManager.isPlaying();

        statusCard.removeAllViews();

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        if (isPlaying) {
            cardBg.setColor(ColorUtils.setAlphaComponent(SPOTIFY_GREEN, 30));
            cardBg.setStroke(AndroidUtilities.dp(1.5f), ColorUtils.setAlphaComponent(SPOTIFY_GREEN, 120));
        } else {
            cardBg.setColor(0x18FFFFFF);
            cardBg.setStroke(AndroidUtilities.dp(1), 0x28FFFFFF);
        }
        statusCard.setBackground(cardBg);

        int textColor = getThemedColor(Theme.key_dialogTextBlack);
        if (textColor == 0) textColor = 0xFFFFFFFF;
        int subTextColor = getThemedColor(Theme.key_dialogTextGray2);
        if (subTextColor == 0) subTextColor = 0xAAFFFFFF;

        statusIndicator = new TextView(context);
        statusIndicator.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        statusIndicator.setTypeface(AndroidUtilities.bold());
        if (isPlaying) {
            statusIndicator.setText(MiogramLocale.get("🟢 ЗАРАЗ ГРАЄ В SPOTIFY", "🟢 СЕЙЧАС ИГРАЕТ В SPOTIFY", "🟢 NOW PLAYING IN SPOTIFY"));
            statusIndicator.setTextColor(SPOTIFY_GREEN);
        } else {
            statusIndicator.setText(MiogramLocale.get("⚪ SPOTIFY НЕ ТРАНСЛЮЄ МУЗИКУ", "⚪ SPOTIFY НЕ ТРАНСЛИРУЕТ МУЗЫКУ", "⚪ NO ACTIVE SPOTIFY BROADCAST"));
            statusIndicator.setTextColor(subTextColor);
        }
        statusCard.addView(statusIndicator, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        if (isPlaying) {
            trackTitleView = new TextView(context);
            trackTitleView.setText(spotifyManager.getCurrentTrack());
            trackTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            trackTitleView.setTypeface(AndroidUtilities.bold());
            trackTitleView.setTextColor(textColor);
            statusCard.addView(trackTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

            trackArtistView = new TextView(context);
            String artistAndAlbum = spotifyManager.getCurrentArtist();
            if (!TextUtils.isEmpty(spotifyManager.getCurrentAlbum())) {
                artistAndAlbum += " — " + spotifyManager.getCurrentAlbum();
            }
            trackArtistView.setText(artistAndAlbum);
            trackArtistView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            trackArtistView.setTextColor(subTextColor);
            statusCard.addView(trackArtistView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            String lyricsLine = spotifyManager.getCurrentLyricsLine();
            if (!TextUtils.isEmpty(lyricsLine)) {
                lyricsPreviewView = new TextView(context);
                lyricsPreviewView.setText("“" + lyricsLine + "”");
                lyricsPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                lyricsPreviewView.setTextColor(SPOTIFY_GREEN);
                lyricsPreviewView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
                GradientDrawable lyricBg = new GradientDrawable();
                lyricBg.setColor(ColorUtils.setAlphaComponent(SPOTIFY_GREEN, 25));
                lyricBg.setCornerRadius(AndroidUtilities.dp(10));
                lyricsPreviewView.setBackground(lyricBg);
                statusCard.addView(lyricsPreviewView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            }

            LinearLayout buttonRow = new LinearLayout(context);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView copyLinkBtn = new TextView(context);
            copyLinkBtn.setText(MiogramLocale.get("Копіювати посилання", "Копировать ссылку", "Copy Link"));
            copyLinkBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            copyLinkBtn.setTextColor(textColor);
            copyLinkBtn.setGravity(Gravity.CENTER);
            GradientDrawable copyBg = new GradientDrawable();
            copyBg.setColor(0x22FFFFFF);
            copyBg.setCornerRadius(AndroidUtilities.dp(12));
            copyLinkBtn.setBackground(copyBg);
            copyLinkBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            copyLinkBtn.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                String url = spotifyManager.getTrackWebUrl();
                if (!TextUtils.isEmpty(url)) {
                    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Spotify URL", url));
                        Toast.makeText(context, MiogramLocale.get("Посилання скопійовано!", "Ссылка скопирована!", "Link copied!"), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            buttonRow.addView(copyLinkBtn, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

            View spacer = new View(context);
            buttonRow.addView(spacer, LayoutHelper.createLinear(8, 1));

            TextView openBtn = new TextView(context);
            openBtn.setText(MiogramLocale.get("Відкрити трек", "Открыть трек", "Open Track"));
            openBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            openBtn.setTextColor(0xFFFFFFFF);
            openBtn.setTypeface(AndroidUtilities.bold());
            openBtn.setGravity(Gravity.CENTER);
            GradientDrawable openBg = new GradientDrawable();
            openBg.setColor(SPOTIFY_GREEN);
            openBg.setCornerRadius(AndroidUtilities.dp(12));
            openBtn.setBackground(openBg);
            openBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            openBtn.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                spotifyManager.openInSpotify(context);
            });
            buttonRow.addView(openBtn, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

            statusCard.addView(buttonRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        } else {
            TextView hint = new TextView(context);
            hint.setText(MiogramLocale.get(
                    "Увімкніть «Статус трансляції пристрою» у налаштуваннях Spotify та запустіть трек, щоб Miogram транслював слова та інфо.",
                    "Включите «Статус трансляции устройства» в настройках Spotify и запустите трек, чтобы Miogram транслировал слова и инфо.",
                    "Enable 'Device Broadcast Status' in Spotify settings and start a song to sync track & lyrics."
            ));
            hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            hint.setTextColor(subTextColor);
            statusCard.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private void addSectionHeader(LinearLayout root, String title, int color) {
        TextView tv = new TextView(root.getContext());
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(ColorUtils.setAlphaComponent(color, 160));
        tv.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(6));
        root.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addFeatureRow(LinearLayout root, Context context, String icon, String title, String desc) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

        TextView iconView = new TextView(context);
        iconView.setText(icon);
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        iconView.setPadding(0, 0, AndroidUtilities.dp(12), 0);
        row.addView(iconView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        int textColor = getThemedColor(Theme.key_dialogTextBlack);
        if (textColor == 0) textColor = 0xFFFFFFFF;
        int subTextColor = getThemedColor(Theme.key_dialogTextGray2);
        if (subTextColor == 0) subTextColor = 0xAAFFFFFF;

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(textColor);
        textCol.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        TextView descView = new TextView(context);
        descView.setText(desc);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        descView.setTextColor(subTextColor);
        textCol.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        row.addView(textCol, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
    }
}
