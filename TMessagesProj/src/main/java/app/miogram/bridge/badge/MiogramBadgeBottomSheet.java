package app.miogram.bridge.badge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;

/**
 * Interactive Miogram Badge Bottom Sheet:
 * - Real-time 10-badge interactive selector & visualizer
 * - Shows badge origin, status, and cloud synchronization state
 * - Community presence toggle
 */
public class MiogramBadgeBottomSheet extends BottomSheet {

    private final long userId;
    private MiogramBadgeType currentBadge;
    private ImageView badgePreviewImageView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView descTextView;

    public MiogramBadgeBottomSheet(BaseFragment fragment, long userId) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        this.userId = userId;
        this.currentBadge = MiogramBadgeManager.getBadgeType(userId);
        init(fragment.getParentActivity());
    }

    public MiogramBadgeBottomSheet(Context context, long userId) {
        super(context, false);
        this.userId = userId;
        this.currentBadge = MiogramBadgeManager.getBadgeType(userId);
        init(context);
    }

    private void init(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }

        setApplyBottomPadding(false);
        setApplyTopPadding(false);
        fixNavigationBar(Theme.getColor(Theme.key_windowBackgroundWhite));

        long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        boolean isSelf = (userId == currentUserId || userId == 0);
        boolean isFounder = (userId == MiogramBadgeManager.FOUNDER_USER_ID);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(24));

        // 1. Drag handle
        ImageView dragHandle = new ImageView(context);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setShape(GradientDrawable.RECTANGLE);
        handleDrawable.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handleDrawable.setCornerRadius(AndroidUtilities.dp(3));
        dragHandle.setImageDrawable(handleDrawable);
        root.addView(dragHandle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 2. Large Badge Preview Container
        LinearLayout badgeContainer = new LinearLayout(context);
        badgeContainer.setOrientation(LinearLayout.VERTICAL);
        badgeContainer.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(AndroidUtilities.dp(20));
        badgeBg.setColor(Color.argb(26, 112, 214, 255));
        badgeBg.setStroke(AndroidUtilities.dp(1.2f), Color.argb(100, 112, 214, 255));
        badgeContainer.setBackground(badgeBg);
        badgeContainer.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));

        badgePreviewImageView = new ImageView(context);
        badgePreviewImageView.setImageDrawable(new MiogramArrowDrawable(72, currentBadge));
        badgeContainer.addView(badgePreviewImageView, LayoutHelper.createLinear(90, 72, Gravity.CENTER));

        root.addView(badgeContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // 3. Title
        titleView = new TextView(context);
        titleView.setTextSize(19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);
        titleView.setText(isFounder ? MiogramLocale.get("Засновник Miogram", "Создатель Miogram", "Miogram Founder") : MiogramLocale.get("Спільнота Miogram ໒꒱", "Сообщество Miogram ໒꒱", "Miogram Community ໒꒱"));
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // 4. Subtitle / Badge Style Name
        subtitleView = new TextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(AndroidUtilities.bold());
        subtitleView.setTextColor(Color.parseColor("#00E5FF"));
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setText(currentBadge.getCode() + " • " + currentBadge.getTitle());
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // 5. Horizontal 10-Badge Selector (if self or founder)
        if (isSelf || isFounder) {
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout selectorRow = new LinearLayout(context);
            selectorRow.setOrientation(LinearLayout.HORIZONTAL);
            selectorRow.setGravity(Gravity.CENTER_VERTICAL);
            selectorRow.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(8));

            for (MiogramBadgeType type : MiogramBadgeType.values()) {
                LinearLayout item = new LinearLayout(context);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));

                GradientDrawable itemBg = new GradientDrawable();
                itemBg.setShape(GradientDrawable.RECTANGLE);
                itemBg.setCornerRadius(AndroidUtilities.dp(12));
                boolean selected = (type == currentBadge);
                itemBg.setColor(selected ? Color.argb(45, 0, 229, 255) : Color.argb(15, 128, 128, 128));
                itemBg.setStroke(AndroidUtilities.dp(1), selected ? Color.parseColor("#00E5FF") : Color.TRANSPARENT);
                item.setBackground(itemBg);

                ImageView icon = new ImageView(context);
                icon.setImageDrawable(new MiogramArrowDrawable(28, type));
                item.addView(icon, LayoutHelper.createLinear(36, 28, Gravity.CENTER));

                TextView label = new TextView(context);
                label.setText(type.getCode().substring(0, 2));
                label.setTextSize(11);
                label.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                item.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 2, 0, 0));

                final Context finalContext = context;
                item.setOnClickListener(v -> {
                    currentBadge = type;
                    badgePreviewImageView.setImageDrawable(new MiogramArrowDrawable(72, currentBadge));
                    subtitleView.setText(currentBadge.getCode() + " • " + currentBadge.getTitle());
                    MiogramSupabaseBridge.setSelectedBadge(finalContext, currentBadge);
                    // Refresh styles
                    for (int i = 0; i < selectorRow.getChildCount(); i++) {
                        View child = selectorRow.getChildAt(i);
                        GradientDrawable bg = (GradientDrawable) child.getBackground();
                        boolean isCur = (i == currentBadge.ordinal());
                        bg.setColor(isCur ? Color.argb(45, 0, 229, 255) : Color.argb(15, 128, 128, 128));
                        bg.setStroke(AndroidUtilities.dp(1), isCur ? Color.parseColor("#00E5FF") : Color.TRANSPARENT);
                    }
                });

                selectorRow.addView(item, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
            }

            scroll.addView(selectorRow);
            root.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));
        }

        // 6. Description Card
        LinearLayout descCard = new LinearLayout(context);
        descCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(AndroidUtilities.dp(14));
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        descCard.setBackground(cardBg);
        descCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        descTextView = new TextView(context);
        descTextView.setTextSize(13.5f);
        descTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        descTextView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        descTextView.setGravity(Gravity.CENTER);

        if (isFounder) {
            descTextView.setText(MiogramLocale.get(
                    "Ексклюзивний піксельний бейдж творця та розробника Miogram. Підтримує всі 10 стилів дизайну та хмарну синхронізацію спільноти.",
                    "Эксклюзивный пиксельный бейдж создателя и разработчика Miogram. Поддерживает все 10 стилей дизайна и облачную синхронизацию сообщества.",
                    "Exclusive pixel badge of the Miogram creator. Supports all 10 canonical styles and community cloud synchronization."
            ));
        } else {
            descTextView.setText(MiogramLocale.get(
                    "Стрілочка — це знак приналежності до Miogram. 10 різних варіантів дозволяють виразити індивідуальність, зберігаючи єдину ідентичність.",
                    "Стрелочка — это знак принадлежности к Miogram. 10 разных вариантов позволяют выразить индивидуальность, сохраняя единую идентичность.",
                    "The arrow badge represents Miogram community identity. 10 styles allow individual expression with a unified spirit."
            ));
        }
        descCard.addView(descTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(descCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 7. Action Button
        TextView okButton = new TextView(context);
        okButton.setText(MiogramLocale.get("Зберегти ໒꒱", "Сохранить ໒꒱", "Done ໒꒱"));
        okButton.setTextSize(15);
        okButton.setTypeface(AndroidUtilities.bold());
        okButton.setTextColor(Color.WHITE);
        okButton.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(AndroidUtilities.dp(14));
        btnBg.setColor(0xFF00B4D8);
        okButton.setBackground(btnBg);
        okButton.setPadding(0, AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13));
        okButton.setOnClickListener(v -> dismiss());

        root.addView(okButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
    }
}
