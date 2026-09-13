package app.exteraless.plugins.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import app.exteraless.plugins.Plugin;
import app.exteraless.plugins.PluginInstallHelper;
import app.exteraless.plugins.PluginsController;
import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * In-app catalog bottom sheet for browsing and 1-tap installing plugins
 * directly from GitHub (fuckramochka/mioplugin catalog.json).
 */
public class MiogramPluginCatalogAlert extends BottomSheet {

    public static final String CATALOG_URL = "https://raw.githubusercontent.com/fuckramochka/mioplugin/main/catalog.json";
    public static final String GITHUB_REPO_URL = "https://github.com/fuckramochka/mioplugin";

    public static class CatalogPlugin {
        public String id;
        public String name;
        public String version;
        public String author;
        public String description;
        public String category;
        public String downloadUrl;
        public String bundleUrl;

        public String getEffectiveDownloadUrl() {
            if (!TextUtils.isEmpty(bundleUrl)) {
                return bundleUrl;
            }
            return downloadUrl;
        }

        public String getFileName() {
            String url = getEffectiveDownloadUrl();
            if (url != null && url.contains("/")) {
                String last = url.substring(url.lastIndexOf("/") + 1);
                try {
                    last = java.net.URLDecoder.decode(last, "UTF-8");
                } catch (Throwable ignore) {}
                if (!TextUtils.isEmpty(last)) return last;
            }
            return (id != null ? id : "plugin") + ".plugin";
        }
    }

    private final Activity activity;
    private final List<CatalogPlugin> allPlugins = new ArrayList<>();
    private String selectedCategory = "ALL";

    private LinearLayout itemsContainer;
    private ProgressBar loadingBar;
    private LinearLayout errorContainer;
    private TextView errorText;
    private HorizontalScrollView categoriesScroll;
    private LinearLayout categoriesLayout;

    private static String cachedCatalogJson = null;
    private static long lastCatalogFetchTime = 0;

    public MiogramPluginCatalogAlert(Activity activity) {
        super(activity, false);
        this.activity = activity;
        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        Context context = activity;

        FrameLayout root = new FrameLayout(context);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(24));

        // --- Header ---
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), AndroidUtilities.dp(8));

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(MiogramLocale.get("Каталог плагінів MioGram ໒꒱", "Каталог плагинов MioGram ໒꒱", "MioGram Plugin Catalog ໒꒱"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titles.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(MiogramLocale.get("Офіційні плагіни та модулі з GitHub (@fuckramochka)", "Официальные плагины и модули с GitHub (@fuckramochka)", "Official plugins & modules from GitHub (@fuckramochka)"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        titles.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        header.addView(titles, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // GitHub Button
        TextView ghBtn = new TextView(context);
        ghBtn.setText("GitHub");
        ghBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        ghBtn.setTypeface(AndroidUtilities.bold());
        ghBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        ghBtn.setGravity(Gravity.CENTER);
        ghBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        GradientDrawable ghBg = new GradientDrawable();
        ghBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        ghBg.setCornerRadius(AndroidUtilities.dp(12));
        ghBtn.setBackground(ghBg);
        ghBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            Browser.openUrl(activity, GITHUB_REPO_URL);
        });
        header.addView(ghBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        content.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // --- Category filter tabs ---
        categoriesScroll = new HorizontalScrollView(context);
        categoriesScroll.setHorizontalScrollBarEnabled(false);
        categoriesScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        categoriesScroll.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        categoriesLayout = new LinearLayout(context);
        categoriesLayout.setOrientation(LinearLayout.HORIZONTAL);
        categoriesScroll.addView(categoriesLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        content.addView(categoriesScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // --- Loading Spinner ---
        loadingBar = new ProgressBar(context);
        content.addView(loadingBar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 36, 0, 36));

        // --- Error state ---
        errorContainer = new LinearLayout(context);
        errorContainer.setOrientation(LinearLayout.VERTICAL);
        errorContainer.setGravity(Gravity.CENTER);
        errorContainer.setVisibility(View.GONE);
        errorContainer.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        errorText = new TextView(context);
        errorText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        errorText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        errorText.setGravity(Gravity.CENTER);
        errorContainer.addView(errorText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        TextView retryBtn = new TextView(context);
        retryBtn.setText(MiogramLocale.get("Спробувати знову", "Попробовать снова", "Try again"));
        retryBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        retryBtn.setTypeface(AndroidUtilities.bold());
        retryBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        retryBtn.setGravity(Gravity.CENTER);
        retryBtn.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(8), AndroidUtilities.dp(18), AndroidUtilities.dp(8));
        GradientDrawable retryBg = new GradientDrawable();
        retryBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        retryBg.setCornerRadius(AndroidUtilities.dp(14));
        retryBtn.setBackground(retryBg);
        retryBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            fetchCatalog(true);
        });
        errorContainer.addView(retryBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        content.addView(errorContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // --- Items Container ---
        itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        itemsContainer.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        content.addView(itemsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);

        // Fetch catalog
        fetchCatalog(false);
    }

    private void fetchCatalog(boolean forceReload) {
        if (!forceReload && cachedCatalogJson != null && (System.currentTimeMillis() - lastCatalogFetchTime < 10 * 60 * 1000)) {
            parseAndDisplayCatalog(cachedCatalogJson);
            return;
        }

        loadingBar.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        itemsContainer.setVisibility(View.GONE);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CATALOG_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "Miogram/1.0");

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();
                    final String json = sb.toString();
                    cachedCatalogJson = json;
                    lastCatalogFetchTime = System.currentTimeMillis();

                    AndroidUtilities.runOnUIThread(() -> parseAndDisplayCatalog(json));
                } else {
                    final String err = "HTTP " + code + " " + conn.getResponseMessage();
                    AndroidUtilities.runOnUIThread(() -> showError(err));
                }
            } catch (Throwable t) {
                FileLog.e(t);
                final String err = t.getMessage() != null ? t.getMessage() : "Network error";
                AndroidUtilities.runOnUIThread(() -> showError(err));
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignore) {}
                }
            }
        }).start();
    }

    private void showError(String err) {
        loadingBar.setVisibility(View.GONE);
        itemsContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(MiogramLocale.get(
                "Не вдалося завантажити каталог з GitHub.\nПомилка: ",
                "Не удалось загрузить каталог с GitHub.\nОшибка: ",
                "Failed to load catalog from GitHub.\nError: "
        ) + err);
    }

    private void parseAndDisplayCatalog(String json) {
        loadingBar.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        itemsContainer.setVisibility(View.VISIBLE);
        allPlugins.clear();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("plugins");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    CatalogPlugin cp = new CatalogPlugin();
                    cp.id = obj.optString("id", "");
                    cp.name = obj.optString("name", cp.id);
                    cp.version = obj.optString("version", "1.0.0");
                    cp.author = obj.optString("author", "@fuckramochka");
                    cp.description = obj.optString("description", "");
                    cp.category = obj.optString("category", "General");
                    cp.downloadUrl = obj.optString("download_url", "");
                    cp.bundleUrl = obj.optString("bundle_url", "");
                    if (!TextUtils.isEmpty(cp.id)) {
                        allPlugins.add(cp);
                    }
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
            showError(t.getMessage());
            return;
        }

        buildCategoryTabs();
        filterAndRender();
    }

    private void buildCategoryTabs() {
        Context context = activity;
        categoriesLayout.removeAllViews();

        List<String> categories = new ArrayList<>();
        categories.add("ALL");
        for (CatalogPlugin p : allPlugins) {
            if (!categories.contains(p.category)) {
                categories.add(p.category);
            }
        }

        for (String cat : categories) {
            final String c = cat;
            boolean selected = selectedCategory.equalsIgnoreCase(c);

            TextView tab = new TextView(context);
            if ("ALL".equalsIgnoreCase(c)) {
                tab.setText(MiogramLocale.get("Всі", "Все", "All"));
            } else {
                tab.setText(c);
            }
            tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            tab.setTypeface(AndroidUtilities.bold());
            tab.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(5), AndroidUtilities.dp(12), AndroidUtilities.dp(5));
            tab.setGravity(Gravity.CENTER);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(14));
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            if (selected) {
                bg.setColor(accent);
                tab.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            } else {
                bg.setColor(Theme.getColor(Theme.key_dialogBackground));
                bg.setStroke(AndroidUtilities.dp(1), Color.argb(30, 128, 128, 128));
                tab.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            }
            tab.setBackground(bg);

            tab.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                selectedCategory = c;
                buildCategoryTabs();
                filterAndRender();
            });

            categoriesLayout.addView(tab, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        }
    }

    private void filterAndRender() {
        Context context = activity;
        itemsContainer.removeAllViews();

        List<CatalogPlugin> filtered = new ArrayList<>();
        for (CatalogPlugin p : allPlugins) {
            if ("ALL".equalsIgnoreCase(selectedCategory) || selectedCategory.equalsIgnoreCase(p.category)) {
                filtered.add(p);
            }
        }

        if (filtered.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(MiogramLocale.get("Немає плагінів у цій категорії", "Нет плагинов в этой категории", "No plugins in this category"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            empty.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, AndroidUtilities.dp(30), 0, AndroidUtilities.dp(30));
            itemsContainer.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        PluginsController controller = PluginsController.getInstance();

        for (CatalogPlugin item : filtered) {
            Plugin installed = controller.getPlugin(item.id);
            boolean isInstalled = installed != null;
            boolean hasUpdate = isInstalled && isVersionNewer(item.version, installed.version);

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_dialogBackground));
            cardBg.setCornerRadius(AndroidUtilities.dp(16));
            cardBg.setStroke(AndroidUtilities.dp(1), Color.argb(25, 128, 128, 128));
            card.setBackground(cardBg);
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

            // Top row: Title + Version + Category Badge + Action Button
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titleBox = new LinearLayout(context);
            titleBox.setOrientation(LinearLayout.VERTICAL);

            LinearLayout nameRow = new LinearLayout(context);
            nameRow.setOrientation(LinearLayout.HORIZONTAL);
            nameRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameView = new TextView(context);
            nameView.setText(item.name);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
            nameView.setTypeface(AndroidUtilities.bold());
            nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            nameRow.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView verView = new TextView(context);
            verView.setText("v" + item.version);
            verView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            verView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            verView.setPadding(AndroidUtilities.dp(6), 0, 0, 0);
            nameRow.addView(verView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            titleBox.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            LinearLayout subRow = new LinearLayout(context);
            subRow.setOrientation(LinearLayout.HORIZONTAL);
            subRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView authorView = new TextView(context);
            authorView.setText(item.author);
            authorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            authorView.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            subRow.addView(authorView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView catBadge = new TextView(context);
            catBadge.setText(item.category);
            catBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
            catBadge.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            catBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(1), AndroidUtilities.dp(6), AndroidUtilities.dp(1));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.argb(20, 128, 128, 128));
            badgeBg.setCornerRadius(AndroidUtilities.dp(8));
            catBadge.setBackground(badgeBg);
            subRow.addView(catBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

            titleBox.addView(subRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            topRow.addView(titleBox, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

            // Action Button (Install / Update / Installed)
            FrameLayout actionWrapper = new FrameLayout(context);
            TextView actionBtn = new TextView(context);
            actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            actionBtn.setTypeface(AndroidUtilities.bold());
            actionBtn.setGravity(Gravity.CENTER);
            actionBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

            GradientDrawable actBg = new GradientDrawable();
            actBg.setCornerRadius(AndroidUtilities.dp(14));

            ProgressBar downloadSpin = new ProgressBar(context);
            downloadSpin.setVisibility(View.GONE);
            downloadSpin.setLayoutParams(new FrameLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24), Gravity.CENTER));

            if (hasUpdate) {
                actionBtn.setText(MiogramLocale.get("Оновити", "Обновить", "Update"));
                actBg.setColor(0xFFF2994A);
                actionBtn.setTextColor(Color.WHITE);
                actionBtn.setOnClickListener(v -> downloadAndInstall(item, actionBtn, downloadSpin));
            } else if (isInstalled) {
                actionBtn.setText(MiogramLocale.get("Встановлено ✓", "Установлен ✓", "Installed ✓"));
                actBg.setColor(Color.argb(30, 46, 204, 113));
                actionBtn.setTextColor(0xFF2ECC71);
                actionBtn.setOnClickListener(v -> downloadAndInstall(item, actionBtn, downloadSpin));
            } else {
                actionBtn.setText(MiogramLocale.get("Встановити", "Установить", "Install"));
                actBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                actionBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
                actionBtn.setOnClickListener(v -> downloadAndInstall(item, actionBtn, downloadSpin));
            }
            actionBtn.setBackground(actBg);

            actionWrapper.addView(actionBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            actionWrapper.addView(downloadSpin);

            topRow.addView(actionWrapper, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

            card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Description
            if (!TextUtils.isEmpty(item.description)) {
                TextView desc = new TextView(context);
                desc.setText(item.description);
                desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                desc.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                desc.setPadding(0, AndroidUtilities.dp(8), 0, 0);
                desc.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
                card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            itemsContainer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        }
    }

    private void downloadAndInstall(CatalogPlugin item, TextView button, ProgressBar spin) {
        if (activity.isFinishing()) return;
        MiogramHaptic.tap(button);

        button.setVisibility(View.INVISIBLE);
        spin.setVisibility(View.VISIBLE);

        new Thread(() -> {
            File downloadedFile = null;
            HttpURLConnection conn = null;
            try {
                String downloadUrl = item.getEffectiveDownloadUrl();
                URL url = new URL(downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Miogram/1.0");

                int code = conn.getResponseCode();
                if (code == 200) {
                    File cacheDir = activity.getCacheDir();
                    File pluginsDir = new File(cacheDir, "catalog_downloads");
                    if (!pluginsDir.exists()) pluginsDir.mkdirs();

                    String fileName = item.getFileName();
                    downloadedFile = new File(pluginsDir, fileName);

                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    FileOutputStream out = new FileOutputStream(downloadedFile);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                    out.close();
                    in.close();

                    final File target = downloadedFile;
                    AndroidUtilities.runOnUIThread(() -> {
                        spin.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        if (activity.isFinishing()) return;

                        // Dismiss catalog and launch official consent sheet
                        dismiss();
                        PluginInstallHelper.confirmAndInstall(activity, target);
                    });
                } else {
                    final String err = "HTTP " + code;
                    AndroidUtilities.runOnUIThread(() -> {
                        spin.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        showDownloadError(err);
                    });
                }
            } catch (Throwable t) {
                FileLog.e(t);
                final String err = t.getMessage() != null ? t.getMessage() : "Download error";
                AndroidUtilities.runOnUIThread(() -> {
                    spin.setVisibility(View.GONE);
                    button.setVisibility(View.VISIBLE);
                    showDownloadError(err);
                });
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignore) {}
                }
            }
        }).start();
    }

    private void showDownloadError(String err) {
        if (activity.isFinishing()) return;
        new org.telegram.ui.ActionBar.AlertDialog.Builder(activity)
                .setTitle(MiogramLocale.get("Помилка завантаження", "Ошибка загрузки", "Download error"))
                .setMessage(MiogramLocale.get(
                        "Не вдалося завантажити файл плагіна з GitHub: ",
                        "Не удалось загрузить файл плагина с GitHub: ",
                        "Could not download plugin file from GitHub: "
                ) + err)
                .setPositiveButton(LocaleController.getString(R.string.OK), null)
                .show();
    }

    private boolean isVersionNewer(String vCatalog, String vInstalled) {
        if (vCatalog == null || vInstalled == null) return false;
        try {
            String[] catParts = vCatalog.split("\\.");
            String[] instParts = vInstalled.split("\\.");
            int max = Math.max(catParts.length, instParts.length);
            for (int i = 0; i < max; i++) {
                int c = i < catParts.length ? Integer.parseInt(catParts[i].replaceAll("[^0-9]", "")) : 0;
                int in = i < instParts.length ? Integer.parseInt(instParts[i].replaceAll("[^0-9]", "")) : 0;
                if (c > in) return true;
                if (c < in) return false;
            }
        } catch (Throwable ignore) {}
        return false;
    }
}
