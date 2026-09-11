package app.miogram.bridge.badge;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Miogram Badge Granting Bottom Sheet with animated badge style picker.
 * Allows authorized users/founder to grant prestigious community arrow badges.
 */
public class MiogramBadgeGrantSheet extends BottomSheet {

    public static boolean canGrantBadges() {
        try {
            long clientUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
            return clientUserId == MiogramBadgeManager.FOUNDER_USER_ID
                    || MiogramSupabaseBridge.hasCloudBadge(clientUserId)
                    || BuildVars.DEBUG_VERSION;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isFounderViewing() {
        return canGrantBadges();
    }

    public static void show(Context context) {
        show(context, 0);
    }

    public static void show(Context context, long targetUserId) {
        if (context == null) return;
        if (!canGrantBadges()) return;
        MiogramBadgeGrantSheet sheet = new MiogramBadgeGrantSheet(context, targetUserId);
        sheet.show();
    }

    private final long targetUserId;
    private EditTextBoldCursor userInput;
    private EditTextBoldCursor reasonInput;
    private ImageView styleIcon;
    private TextView styleTitle;
    private TextView styleSubtitle;
    private TextView grantBtn;
    private ProgressBar progress;
    private TextView statusView;
    private MiogramBadgeType picked = MiogramBadgeType.ORIGINAL;

    private MiogramBadgeGrantSheet(Context context, long targetUserId) {
        super(context, false);
        this.targetUserId = targetUserId;
        initUi(context);
    }

    private void initUi(Context ctx) {
        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ScrollView scroll = new ScrollView(ctx);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(22));
        scroll.addView(content);

        View handle = new View(ctx);
        handle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0x44888888));
        content.addView(handle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        TextView title = new TextView(ctx);
        title.setText(MiogramLocale.get("Видати стрілочку ໒꒱", "Выдать стрелочку ໒꒱", "Grant a badge ໒꒱"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setGravity(Gravity.CENTER);
        content.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        userInput = makeInput(ctx, MiogramLocale.get("ID користувача (цифри)", "ID пользователя (цифры)", "User ID (digits)"));
        userInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (targetUserId > 0) {
            userInput.setText(String.valueOf(targetUserId));
        }
        content.addView(userInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Style selector card with animated icon preview
        LinearLayout styleCard = new LinearLayout(ctx);
        styleCard.setOrientation(LinearLayout.HORIZONTAL);
        styleCard.setGravity(Gravity.CENTER_VERTICAL);
        styleCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        styleCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        styleCard.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showStylePicker();
        });

        styleIcon = new ImageView(ctx);
        styleIcon.setImageDrawable(new MiogramArrowDrawable(34, picked));
        styleCard.addView(styleIcon, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout styleTextLayout = new LinearLayout(ctx);
        styleTextLayout.setOrientation(LinearLayout.VERTICAL);

        styleTitle = new TextView(ctx);
        styleTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        styleTitle.setTypeface(AndroidUtilities.bold());
        styleTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        styleTextLayout.addView(styleTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        styleSubtitle = new TextView(ctx);
        styleSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        styleSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        styleTextLayout.addView(styleSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        styleCard.addView(styleTextLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView chevron = new TextView(ctx);
        chevron.setText("›");
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        chevron.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        styleCard.addView(chevron, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        content.addView(styleCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        refreshStyleBtn();

        reasonInput = makeInput(ctx, MiogramLocale.get("За що видано…", "За что выдано…", "Granted for…"));
        reasonInput.setMinLines(2);
        content.addView(reasonInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        grantBtn = new TextView(ctx);
        grantBtn.setText(MiogramLocale.get("Видати", "Выдать", "Grant"));
        grantBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        grantBtn.setTypeface(AndroidUtilities.bold());
        grantBtn.setGravity(Gravity.CENTER);
        grantBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        grantBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        grantBtn.setPadding(0, AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13));
        grantBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onGrant();
        });
        content.addView(grantBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        progress = new ProgressBar(ctx);
        progress.setVisibility(View.GONE);
        content.addView(progress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        statusView = new TextView(ctx);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setGravity(Gravity.CENTER);
        content.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
    }

    private EditTextBoldCursor makeInput(Context ctx, String hint) {
        EditTextBoldCursor e = new EditTextBoldCursor(ctx);
        e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        e.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        e.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        e.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        return e;
    }

    private void refreshStyleBtn() {
        if (styleIcon != null) {
            styleIcon.setImageDrawable(new MiogramArrowDrawable(34, picked));
        }
        if (styleTitle != null) {
            styleTitle.setText(picked.getCode());
        }
        if (styleSubtitle != null) {
            styleSubtitle.setText(picked.getTitle());
        }
    }

    private void showStylePicker() {
        Context ctx = getContext();
        BottomSheet pickerSheet = new BottomSheet(ctx, false);

        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout sheetContent = new LinearLayout(ctx);
        sheetContent.setOrientation(LinearLayout.VERTICAL);
        sheetContent.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(18));
        root.addView(sheetContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        View handle = new View(ctx);
        handle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0x44888888));
        sheetContent.addView(handle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        TextView pickerTitle = new TextView(ctx);
        pickerTitle.setText(MiogramLocale.get("Оберіть стиль стрілочки ໒꒱", "Выберите стиль стрелочки ໒꒱", "Choose badge style ໒꒱"));
        pickerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        pickerTitle.setTypeface(AndroidUtilities.bold());
        pickerTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        pickerTitle.setGravity(Gravity.CENTER);
        sheetContent.addView(pickerTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        ScrollView scroll = new ScrollView(ctx);
        sheetContent.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(360)));

        LinearLayout itemsList = new LinearLayout(ctx);
        itemsList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(itemsList, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        MiogramBadgeType[] allTypes = MiogramBadgeType.values();
        for (MiogramBadgeType type : allTypes) {
            boolean isSelected = (type == picked);

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10),
                    isSelected ? Theme.getColor(Theme.key_featuredStickers_addButton) & 0x22FFFFFF : 0));
            row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

            ImageView icon = new ImageView(ctx);
            icon.setImageDrawable(new MiogramArrowDrawable(38, type));
            row.addView(icon, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            LinearLayout textCol = new LinearLayout(ctx);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView codeView = new TextView(ctx);
            codeView.setText(type.getCode());
            codeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            codeView.setTypeface(AndroidUtilities.bold());
            codeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCol.addView(codeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            TextView descView = new TextView(ctx);
            descView.setText(type.getTitle());
            descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            textCol.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

            if (isSelected) {
                TextView check = new TextView(ctx);
                check.setText("✓");
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                check.setTypeface(AndroidUtilities.bold());
                check.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                row.addView(check, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
            }

            row.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                picked = type;
                refreshStyleBtn();
                pickerSheet.dismiss();
            });

            itemsList.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 2));
        }

        pickerSheet.setCustomView(root);
        pickerSheet.show();
    }

    private void onGrant() {
        long target;
        try {
            target = Long.parseLong(userInput.getText().toString().trim());
        } catch (Throwable t) {
            target = 0;
        }
        if (target <= 0) {
            Toast.makeText(getContext(), MiogramLocale.get("Введіть числовий ID", "Введите числовой ID", "Enter a numeric ID"), Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = reasonInput.getText().toString().trim();
        if (reason.isEmpty()) {
            reason = MiogramLocale.get("Відзнака засновника Miogram", "Отличие основателя Miogram", "Miogram founder award");
        }
        progress.setVisibility(View.VISIBLE);
        grantBtn.setAlpha(0.5f);
        grantBtn.setClickable(false);
        statusView.setText("");
        final long t = target;
        final String r = reason;
        final MiogramBadgeType type = picked;
        MiogramSupabaseBridge.grantBadgeToUser(t, type.getId(), type.getTitle(), r, () -> {
            progress.setVisibility(View.GONE);
            grantBtn.setAlpha(1f);
            grantBtn.setClickable(true);
            statusView.setText(MiogramLocale.get("Видано ✓", "Выдано ✓", "Granted ✓"));
            MiogramHaptic.success(grantBtn);
        });
    }
}
