package app.miogram.bridge.userbot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import app.miogram.bridge.MiogramLocale;

/**
 * Interactive Setup Dialog & Assistant for Heroku Userbot Bot API Token.
 * Allows entering an existing bot token or using step-by-step guidance
 * with @BotFather (/newbot, /setinline) to generate a helper bot.
 */
public class MiogramHerokuBotSetupDialog extends BottomSheet {

    public interface OnSetupCompletedListener {
        void onCompleted(boolean success, String username);
    }

    public static void show(Context context, OnSetupCompletedListener listener) {
        if (context == null) return;
        new MiogramHerokuBotSetupDialog(context, listener).show();
    }

    private final OnSetupCompletedListener listener;
    private EditText tokenInput;
    private TextView statusText;
    private TextView verifyBtnText;
    private FrameLayout verifyBtn;
    private boolean isVerifying = false;

    public MiogramHerokuBotSetupDialog(Context context, OnSetupCompletedListener listener) {
        super(context, true);
        this.listener = listener;

        setApplyTopPadding(false);
        setApplyBottomPadding(true);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
        scroll.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Header Title
        TextView titleView = new TextView(context);
        titleView.setText("🪐 " + MiogramLocale.get("Налаштування Heroku Bot API", "Настройка Heroku Bot API", "Heroku Bot API Setup"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Subtitle explanation
        TextView subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get(
                "Для роботи інтерактивних галерей, inline-кнопок та модулів Heroku потрібен допоміжний Bot API токен від @BotFather.",
                "Для работы интерактивных галерей, inline-кнопок и модулей Heroku требуется вспомогательный Bot API токен от @BotFather.",
                "Heroku userbot modules and interactive inline galleries require an auxiliary Bot API token from @BotFather."
        ));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitleView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        content.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Input card for Token
        LinearLayout inputCard = new LinearLayout(context);
        inputCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(14));
        cardBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        inputCard.setBackground(cardBg);
        inputCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        content.addView(inputCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView tokenLabel = new TextView(context);
        tokenLabel.setText("🔑 " + MiogramLocale.get("Bot API Токен:", "Bot API Токен:", "Bot API Token:"));
        tokenLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tokenLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tokenLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        inputCard.addView(tokenLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        tokenInput = new EditText(context);
        tokenInput.setHint("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ...");
        tokenInput.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        tokenInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tokenInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        tokenInput.setBackground(null);
        String currentToken = MiogramHerokuManager.getInstance().getBotToken();
        if (!TextUtils.isEmpty(currentToken)) {
            tokenInput.setText(currentToken);
        }
        inputCard.addView(tokenInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        statusText = new TextView(context);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setVisibility(View.GONE);
        content.addView(statusText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Verify and Save Button
        verifyBtn = new FrameLayout(context);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        btnBg.setCornerRadius(AndroidUtilities.dp(12));
        verifyBtn.setBackground(btnBg);

        verifyBtnText = new TextView(context);
        verifyBtnText.setText(MiogramLocale.get("Перевірити та зберегти токен", "Проверить и сохранить токен", "Verify & Save Token"));
        verifyBtnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        verifyBtnText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        verifyBtnText.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        verifyBtnText.setGravity(Gravity.CENTER);
        verifyBtn.addView(verifyBtnText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 12, 0, 12));

        verifyBtn.setOnClickListener(v -> handleVerifyAndSave());
        content.addView(verifyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        content.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 16));

        // Section: BotFather Step-by-Step Assistant
        TextView guideHeader = new TextView(context);
        guideHeader.setText("🤖 " + MiogramLocale.get("Немає бота? Створіть за 1 хвилину через @BotFather:", "Нет бота? Создайте за 1 минуту через @BotFather:", "No bot? Create in 1 min via @BotFather:"));
        guideHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        guideHeader.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        guideHeader.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        content.addView(guideHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Step 1: Open BotFather button
        FrameLayout openBfBtn = createStepButton(context,
                "1. " + MiogramLocale.get("Відкрити чат @BotFather", "Открыть чат @BotFather", "Open @BotFather Chat"),
                () -> openBotFatherChat(context));
        content.addView(openBfBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Step 2: Copy /newbot
        FrameLayout copyNewBotBtn = createStepButton(context,
                "2. " + MiogramLocale.get("Скопіювати команду /newbot", "Скопировать команду /newbot", "Copy command /newbot"),
                () -> copyCommand(context, "/newbot", "/newbot"));
        content.addView(copyNewBotBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Step 3: Copy suggested username
        long myId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        String suggestedName = "miogram_helper_" + (myId > 0 ? myId : "user") + "_bot";
        FrameLayout copyUserBtn = createStepButton(context,
                "3. " + MiogramLocale.get("Скопіювати ім'я бота (@" + suggestedName + ")", "Скопировать имя бота (@" + suggestedName + ")", "Copy bot username (@" + suggestedName + ")"),
                () -> copyCommand(context, suggestedName, suggestedName));
        content.addView(copyUserBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Step 4: Copy /setinline command
        FrameLayout copyInlineBtn = createStepButton(context,
                "4. " + MiogramLocale.get("Скопіювати /setinline (для кнопок)", "Скопировать /setinline (для кнопок)", "Copy /setinline (for inline buttons)"),
                () -> copyCommand(context, "/setinline", "/setinline"));
        content.addView(copyInlineBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Step 5 hint
        TextView step5Hint = new TextView(context);
        step5Hint.setText(MiogramLocale.get(
                "5. Скопіюйте надісланий @BotFather токен у поле вище та натисніть «Перевірити та зберегти»!",
                "5. Скопируйте отправленный @BotFather токен в поле выше и нажмите «Проверить и сохранить»!",
                "5. Copy the token sent by @BotFather into the field above and click «Verify & Save»!"
        ));
        step5Hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        step5Hint.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        content.addView(step5Hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(scroll);
    }

    private FrameLayout createStepButton(Context context, String text, Runnable action) {
        FrameLayout btn = new FrameLayout(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        gd.setCornerRadius(AndroidUtilities.dp(10));
        gd.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        btn.setBackground(gd);

        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
        tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        btn.addView(tv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        btn.setOnClickListener(v -> action.run());
        return btn;
    }

    private void copyCommand(Context context, String copyText, String label) {
        AndroidUtilities.addToClipboard(copyText);
        Toast.makeText(context, "📋 " + MiogramLocale.get("Скопійовано: ", "Скопировано: ", "Copied: ") + label, Toast.LENGTH_SHORT).show();
    }

    private void openBotFatherChat(Context context) {
        try {
            int account = UserConfig.selectedAccount;
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.User botFather = mc.getUserOrChat("botfather") instanceof TLRPC.User ? (TLRPC.User) mc.getUserOrChat("botfather") : null;
            if (botFather != null && LaunchActivity.instance != null) {
                Bundle args = new Bundle();
                args.putLong("user_id", botFather.id);
                LaunchActivity.instance.presentFragment(new ChatActivity(args));
                dismiss();
            } else if (LaunchActivity.instance != null) {
                // Search by username
                Bundle args = new Bundle();
                args.putString("username", "botfather");
                LaunchActivity.instance.presentFragment(new ChatActivity(args));
                dismiss();
            }
        } catch (Throwable t) {
            Toast.makeText(context, "Напишіть @BotFather у пошуку", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVerifyAndSave() {
        if (isVerifying) return;
        String token = tokenInput.getText().toString().trim();
        if (TextUtils.isEmpty(token)) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setTextColor(0xFFE53935);
            statusText.setText("⚠️ " + MiogramLocale.get("Введіть токен бота!", "Введите токен бота!", "Enter bot token!"));
            return;
        }

        isVerifying = true;
        verifyBtnText.setText(MiogramLocale.get("Перевірка...", "Проверка...", "Verifying..."));
        statusText.setVisibility(View.VISIBLE);
        statusText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        statusText.setText("⏳ " + MiogramLocale.get("З'єднання з Telegram Bot API...", "Соединение с Telegram Bot API...", "Connecting to Telegram Bot API..."));

        MiogramHerokuManager.getInstance().verifyBotToken(token, (success, username, name, id, canJoinGroups, supportsInline, error) -> {
            isVerifying = false;
            verifyBtnText.setText(MiogramLocale.get("Перевірити та зберегти токен", "Проверить и сохранить токен", "Verify & Save Token"));

            if (success) {
                statusText.setTextColor(0xFF43A047);
                String msg = "✅ " + MiogramLocale.get("Успішно підключено!", "Успешно подключено!", "Connected successfully!") +
                        " @" + username + " (" + name + ")" +
                        (supportsInline ? " [Inline: OK]" : " [Inline: Не активовано]");
                statusText.setText(msg);

                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

                if (listener != null) {
                    listener.onCompleted(true, username);
                }

                AndroidUtilities.runOnUIThread(this::dismiss, 1200);
            } else {
                statusText.setTextColor(0xFFE53935);
                statusText.setText("❌ " + (error != null ? error : "Невірний токен"));
            }
        });
    }
}
