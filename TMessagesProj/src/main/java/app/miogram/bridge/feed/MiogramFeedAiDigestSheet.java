package app.miogram.bridge.feed;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;

public class MiogramFeedAiDigestSheet extends BottomSheet {

    private final List<String> posts;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView copyButton;

    public MiogramFeedAiDigestSheet(Activity activity, List<String> posts) {
        super(activity, false);
        this.posts = posts;
        setApplyBottomPadding(false);
        setApplyTopPadding(false);
        fixNavigationBar(Theme.getColor(Theme.key_dialogBackground));

        Context context = activity;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        root.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(20));

        // 1. Drag Handle
        ImageView dragHandle = new ImageView(context);
        GradientDrawable handle = new GradientDrawable();
        handle.setShape(GradientDrawable.RECTANGLE);
        handle.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handle.setCornerRadius(AndroidUtilities.dp(3));
        dragHandle.setImageDrawable(handle);
        root.addView(dragHandle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 2. Header
        TextView title = new TextView(context);
        title.setText(MiogramLocale.get("ШІ-дайджест стрічки ໒꒱", "ИИ-дайджест ленты ໒꒱", "AI Feed Digest ໒꒱"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        statusTextView = new TextView(context);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setText(MiogramLocale.get("Аналіз останніх публікацій...", "Анализ последних публикаций...", "Analyzing recent posts..."));
        root.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 3. Card Container
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        card.setBackground(cardBg);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        progressBar = new ProgressBar(context);
        card.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 20, 0, 20));

        resultTextView = new TextView(context);
        resultTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        resultTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        resultTextView.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        resultTextView.setTextIsSelectable(true);
        resultTextView.setVisibility(View.GONE);
        card.addView(resultTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 4. Buttons Row
        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        copyButton = new TextView(context);
        copyButton.setText(MiogramLocale.get("Копіювати", "Копировать", "Copy"));
        copyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        copyButton.setTypeface(AndroidUtilities.bold());
        copyButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        copyButton.setGravity(Gravity.CENTER);
        copyButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        copyButton.setVisibility(View.GONE);
        copyButton.setOnClickListener(v -> {
            CharSequence txt = resultTextView.getText();
            if (!TextUtils.isEmpty(txt)) {
                AndroidUtilities.addToClipboard(txt);
                Toast.makeText(context, MiogramLocale.get("Скопійовано 📋", "Скопировано 📋", "Copied 📋"), Toast.LENGTH_SHORT).show();
            }
        });
        buttons.addView(copyButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 8, 0));

        TextView closeButton = new TextView(context);
        closeButton.setText(MiogramLocale.get("Закрити", "Закрыть", "Close"));
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        closeButton.setTypeface(AndroidUtilities.bold());
        closeButton.setTextColor(Color.WHITE);
        closeButton.setGravity(Gravity.CENTER);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(AndroidUtilities.dp(10));
        closeBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        closeButton.setBackground(closeBg);
        closeButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        closeButton.setOnClickListener(v -> dismiss());
        buttons.addView(closeButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        root.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(root);
        setCustomView(scrollView);

        generateDigest();
    }

    private void generateDigest() {
        if (posts == null || posts.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(MiogramLocale.get("Немає завантажених постів", "Нет загруженных постов", "No posts loaded"));
            resultTextView.setText(MiogramLocale.get("У стрічці наразі немає публікацій для складання ШІ-дайджесту.",
                    "В ленте сейчас нет публикаций для составления ИИ-дайджеста.",
                    "No feed posts available to generate AI digest."));
            resultTextView.setVisibility(View.VISIBLE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Ти — розумний ШІ-редактор новинного дайджесту в додатку Miogram.\n");
        sb.append("Склади стислий, структурований і інформативний дайджест українською мовою з наведених останніх постів із каналів стрічки.\n");
        sb.append("Вимоги:\n");
        sb.append("- Згрупуй головні новини тезами з емодзі.\n");
        sb.append("- Пропусти рекламу, спам, промо-посилання та повтори.\n");
        sb.append("- В кінці додай 1-2 речення короткого резюме.\n\n");
        sb.append("Останні пости:\n");

        int limit = Math.min(25, posts.size());
        for (int i = 0; i < limit; i++) {
            sb.append(i + 1).append(". ").append(posts.get(i)).append("\n---\n");
        }

        MiogramAiService.generateText(sb.toString(), (result, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (result != null) {
                    statusTextView.setText(MiogramLocale.get("Готово ✓", "Готово ✓", "Complete ✓"));
                    resultTextView.setText(result);
                    resultTextView.setVisibility(View.VISIBLE);
                    copyButton.setVisibility(View.VISIBLE);
                } else {
                    statusTextView.setText(MiogramLocale.get("Помилка генерації", "Ошибка генерации", "Generation Error"));
                    resultTextView.setText(error != null ? error : "Не вдалося отримати відповідь від ШІ. Перевірте підключення до Інтернету або ключ API.");
                    resultTextView.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
