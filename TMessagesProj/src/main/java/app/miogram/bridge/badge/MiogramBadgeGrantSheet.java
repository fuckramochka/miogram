package app.miogram.bridge.badge;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Founder-only badge grants: pick a user, a style and a reason.
 *
 * <p>Visible exclusively when the current account IS the founder
 * ({@link MiogramBadgeManager#FOUNDER_USER_ID}). Writes the target row with
 * {@code grantor_id} so other clients render "Granted by Founder".
 * Honest limitation: with the anon key this is a trust signal, not proof —
 * proof is the staff-only {@code verified} flag.
 */
public class MiogramBadgeGrantSheet extends BottomSheet {

    public static boolean isFounderViewing() {
        try {
            return UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()
                    == MiogramBadgeManager.FOUNDER_USER_ID;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void show(Context context) {
        if (context == null) return;
        if (!isFounderViewing()) return;
        MiogramBadgeGrantSheet sheet = new MiogramBadgeGrantSheet(context);
        sheet.show();
    }

    private EditTextBoldCursor userInput;
    private EditTextBoldCursor reasonInput;
    private TextView styleBtn;
    private TextView grantBtn;
    private ProgressBar progress;
    private TextView statusView;
    private MiogramBadgeType picked = MiogramBadgeType.ORIGINAL;

    private MiogramBadgeGrantSheet(Context context) {
        super(context, false);
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
        title.setText(MiogramLocale.get("Видати стрілочку", "Выдать стрелочку", "Grant a badge"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setGravity(Gravity.CENTER);
        content.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        userInput = makeInput(ctx, MiogramLocale.get("ID користувача (цифри)", "ID пользователя (цифры)", "User ID (digits)"));
        userInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        content.addView(userInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        styleBtn = new TextView(ctx);
        styleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        styleBtn.setTypeface(AndroidUtilities.bold());
        styleBtn.setGravity(Gravity.CENTER);
        styleBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        styleBtn.setBackground(Theme.getSelectorDrawable(false));
        styleBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        styleBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showStylePicker();
        });
        content.addView(styleBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        refreshStyleBtn();

        reasonInput = makeInput(ctx, MiogramLocale.get("За що видано…", "За что выдано…", "Granted for…"));
        reasonInput.setMinLines(2);
        content.addView(reasonInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

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
        styleBtn.setText(MiogramLocale.get("Стиль: ", "Стиль: ", "Style: ") + picked.getTitle());
    }

    private void showStylePicker() {
        Context ctx = getContext();
        List<String> names = new ArrayList<>();
        MiogramBadgeType[] all = MiogramBadgeType.values();
        for (MiogramBadgeType t : all) names.add(t.getTitle());
        CharSequence[] items = names.toArray(new CharSequence[0]);
        int checked = picked.ordinal();
        org.telegram.ui.ActionBar.AlertDialog.Builder b = new org.telegram.ui.ActionBar.AlertDialog.Builder(ctx);
        b.setTitle(MiogramLocale.get("Яку стрілочку видати?", "Какую стрелочку выдать?", "Which badge to grant?"));
        b.setItems(items, (d, which) -> {
            if (which >= 0 && which < all.length) {
                picked = all[which];
                refreshStyleBtn();
            }
        });
        b.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        b.create().show();
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
