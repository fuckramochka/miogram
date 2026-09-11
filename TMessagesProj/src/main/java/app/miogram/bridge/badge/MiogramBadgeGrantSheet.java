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
    private TextView deleteBtn;
    private ProgressBar progress;
    private TextView statusView;
    private final java.util.LinkedHashSet<MiogramBadgeType> selectedBadges = new java.util.LinkedHashSet<>();

    private MiogramBadgeGrantSheet(Context context, long targetUserId) {
        super(context, true);
        this.targetUserId = targetUserId;
        if (targetUserId > 0) {
            MiogramSupabaseBridge.BadgeRecord existing = MiogramSupabaseBridge.getBadgeRecord(targetUserId);
            if (existing != null && existing.isActive && existing.badgeTypes != null && !existing.badgeTypes.isEmpty()) {
                selectedBadges.addAll(existing.badgeTypes);
            }
        }
        if (selectedBadges.isEmpty()) {
            selectedBadges.add(MiogramBadgeType.ORIGINAL);
        }
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
        title.setText(MiogramLocale.get("Керування стрілочками ໒꒱", "Управление стрелочками ໒꒱", "Badge Management ໒꒱"));
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
        styleIcon.setImageDrawable(new MiogramArrowDrawable(34, selectedBadges.iterator().next()));
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
        reasonInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        reasonInput.setMinLines(2);
        reasonInput.setMaxLines(4);
        if (targetUserId > 0) {
            MiogramSupabaseBridge.BadgeRecord existing = MiogramSupabaseBridge.getBadgeRecord(targetUserId);
            if (existing != null && existing.obtainedReason != null) {
                reasonInput.setText(existing.obtainedReason);
            }
        }
        content.addView(reasonInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        grantBtn = new TextView(ctx);
        grantBtn.setText(MiogramLocale.get("Зберегти / Видати", "Сохранить / Выдать", "Save / Grant"));
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

        deleteBtn = new TextView(ctx);
        deleteBtn.setText(MiogramLocale.get("Видалити бейдж", "Удалить бейдж", "Revoke badge"));
        deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        deleteBtn.setTypeface(AndroidUtilities.bold());
        deleteBtn.setGravity(Gravity.CENTER);
        deleteBtn.setTextColor(0xFFFF4B4B);
        deleteBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), 0x1AFF4B4B, 0x33FF4B4B));
        deleteBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        deleteBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onRevoke();
        });
        content.addView(deleteBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

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
        if (selectedBadges.isEmpty()) {
            selectedBadges.add(MiogramBadgeType.ORIGINAL);
        }
        MiogramBadgeType first = selectedBadges.iterator().next();
        if (styleIcon != null) {
            styleIcon.setImageDrawable(new MiogramArrowDrawable(34, first));
        }
        if (styleTitle != null) {
            if (selectedBadges.size() == 1) {
                styleTitle.setText(first.getCode());
            } else {
                styleTitle.setText(MiogramLocale.get("Обрано бейджів: ", "Выбрано бейджей: ", "Selected badges: ") + selectedBadges.size());
            }
        }
        if (styleSubtitle != null) {
            if (selectedBadges.size() == 1) {
                styleSubtitle.setText(first.getTitle());
            } else {
                StringBuilder sb = new StringBuilder();
                for (MiogramBadgeType b : selectedBadges) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(b.getCode());
                }
                styleSubtitle.setText(sb.toString());
            }
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
        pickerTitle.setText(MiogramLocale.get("Оберіть стилі стрілочок ໒꒱", "Выберите стили стрелочек ໒꒱", "Choose badge styles ໒꒱"));
        pickerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        pickerTitle.setTypeface(AndroidUtilities.bold());
        pickerTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        pickerTitle.setGravity(Gravity.CENTER);
        sheetContent.addView(pickerTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        ScrollView scroll = new ScrollView(ctx);
        sheetContent.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(340)));

        LinearLayout itemsList = new LinearLayout(ctx);
        itemsList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(itemsList, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        MiogramBadgeType[] allTypes = MiogramBadgeType.values();
        for (MiogramBadgeType type : allTypes) {
            boolean isSelected = selectedBadges.contains(type);

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10),
                    isSelected ? (Theme.getColor(Theme.key_featuredStickers_addButton) & 0x22FFFFFF) : 0));
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

            TextView check = new TextView(ctx);
            check.setText("✓");
            check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            check.setTypeface(AndroidUtilities.bold());
            check.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            row.addView(check, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            row.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                if (selectedBadges.contains(type)) {
                    if (selectedBadges.size() > 1) {
                        selectedBadges.remove(type);
                    } else {
                        Toast.makeText(ctx, MiogramLocale.get("Мінімум один бейдж має бути обраний", "Минимум один бейдж должен быть выбран", "At least one badge must be selected"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    selectedBadges.add(type);
                }
                boolean checkedNow = selectedBadges.contains(type);
                row.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10),
                        checkedNow ? (Theme.getColor(Theme.key_featuredStickers_addButton) & 0x22FFFFFF) : 0));
                check.setVisibility(checkedNow ? View.VISIBLE : View.GONE);
                refreshStyleBtn();
            });

            itemsList.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 2));
        }

        TextView doneBtn = new TextView(ctx);
        doneBtn.setText(MiogramLocale.get("Готово", "Готово", "Done"));
        doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        doneBtn.setTypeface(AndroidUtilities.bold());
        doneBtn.setGravity(Gravity.CENTER);
        doneBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        doneBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        doneBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        doneBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            refreshStyleBtn();
            pickerSheet.dismiss();
        });
        sheetContent.addView(doneBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        pickerSheet.setCustomView(root);
        pickerSheet.show();
    }

    private void onRevoke() {
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
        progress.setVisibility(View.VISIBLE);
        grantBtn.setAlpha(0.5f);
        grantBtn.setClickable(false);
        if (deleteBtn != null) {
            deleteBtn.setAlpha(0.5f);
            deleteBtn.setClickable(false);
        }
        statusView.setText("");
        MiogramSupabaseBridge.revokeBadge(target, () -> {
            progress.setVisibility(View.GONE);
            grantBtn.setAlpha(1f);
            grantBtn.setClickable(true);
            if (deleteBtn != null) {
                deleteBtn.setAlpha(1f);
                deleteBtn.setClickable(true);
            }
            statusView.setText(MiogramLocale.get("Видалено ✓", "Удалено ✓", "Revoked ✓"));
            MiogramHaptic.success(grantBtn);
            AndroidUtilities.runOnUIThread(this::dismiss, 800);
        });
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
        if (deleteBtn != null) {
            deleteBtn.setAlpha(0.5f);
            deleteBtn.setClickable(false);
        }
        statusView.setText("");
        final long t = target;
        final String r = reason;

        StringBuilder idsBuilder = new StringBuilder();
        StringBuilder titlesBuilder = new StringBuilder();
        for (MiogramBadgeType b : selectedBadges) {
            if (idsBuilder.length() > 0) idsBuilder.append(",");
            idsBuilder.append(b.getId());
            if (titlesBuilder.length() > 0) titlesBuilder.append(" & ");
            titlesBuilder.append(b.getTitle());
        }
        final String fBadgeIds = idsBuilder.toString();
        final String fTitle = selectedBadges.size() == 1 ? selectedBadges.iterator().next().getTitle() : "Miogram Community ໒꒱";

        MiogramSupabaseBridge.grantBadgeToUser(t, fBadgeIds, fTitle, r, () -> {
            progress.setVisibility(View.GONE);
            grantBtn.setAlpha(1f);
            grantBtn.setClickable(true);
            if (deleteBtn != null) {
                deleteBtn.setAlpha(1f);
                deleteBtn.setClickable(true);
            }
            statusView.setText(MiogramLocale.get("Збережено ✓", "Сохранено ✓", "Saved ✓"));
            MiogramHaptic.success(grantBtn);
            AndroidUtilities.runOnUIThread(this::dismiss, 800);
        });
    }
}
