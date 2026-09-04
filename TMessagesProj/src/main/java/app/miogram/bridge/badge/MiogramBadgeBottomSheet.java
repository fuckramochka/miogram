package app.miogram.bridge.badge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Premium/Founder Pixel Badge information BottomSheet.
 * Displays interactive info when the Ame-chan pixel badge is tapped.
 */
public class MiogramBadgeBottomSheet extends BottomSheet {

    private final long userId;

    public MiogramBadgeBottomSheet(BaseFragment fragment, long userId) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        this.userId = userId;
        init(fragment.getParentActivity());
    }

    public MiogramBadgeBottomSheet(Context context, long userId) {
        super(context, false);
        this.userId = userId;
        init(context);
    }

    private void init(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }

        setApplyBottomPadding(false);
        setApplyTopPadding(false);
        fixNavigationBar(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(28));

        // 1. Drag Handle
        ImageView dragHandle = new ImageView(context);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setShape(GradientDrawable.RECTANGLE);
        handleDrawable.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handleDrawable.setCornerRadius(AndroidUtilities.dp(3));
        dragHandle.setImageDrawable(handleDrawable);
        root.addView(dragHandle, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 18));

        // 2. Large Pixel Badge Preview Container
        LinearLayout badgeContainer = new LinearLayout(context);
        badgeContainer.setOrientation(LinearLayout.VERTICAL);
        badgeContainer.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(AndroidUtilities.dp(20));
        // Deep cyber pastel background
        badgeBg.setColor(Color.argb(32, 255, 110, 199));
        badgeBg.setStroke(AndroidUtilities.dp(1.5f), Color.argb(120, 255, 110, 199));
        badgeContainer.setBackground(badgeBg);
        badgeContainer.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        ImageView badgeImageView = new ImageView(context);
        MiogramArrowDrawable largeBadgeDrawable = new MiogramArrowDrawable(64);
        badgeImageView.setImageDrawable(largeBadgeDrawable);
        badgeContainer.addView(badgeImageView, LayoutHelper.createLinear(80, 64, Gravity.CENTER));

        root.addView(badgeContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // 3. Title
        TextView titleView = new TextView(context);
        titleView.setTextSize(20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);
        boolean isFounder = (userId == MiogramBadgeManager.FOUNDER_USER_ID);
        titleView.setText(isFounder ? "Засновник Miogram" : "Особливий статус");
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        // 4. Subtitle / Tagline
        TextView subtitleView = new TextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(AndroidUtilities.bold());
        subtitleView.setTextColor(Color.parseColor("#FF55A3"));
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setText(isFounder ? "✦ AME-CHAN PC-98 FOUNDER ✦" : "✦ MIOGRAM SPECIAL ✦");
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 5. Description Card
        LinearLayout descCard = new LinearLayout(context);
        descCard.setOrientation(LinearLayout.VERTICAL);
        descCard.setBackground(app.miogram.bridge.ui.ame.MiogramAmeAesthetic.createAmeCardDrawable(16));
        descCard.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16));

        TextView descText = new TextView(context);
        descText.setTextSize(14);
        descText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        descText.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        descText.setGravity(Gravity.CENTER);

        if (isFounder) {
            descText.setText(app.miogram.bridge.MiogramLocale.get(
                    "Цей ексклюзивний піксельний бейдж належить творцю та розробнику Miogram. Він символізує оригінальну кібер-естетику Needy Girl Overdose, безмежну кастомізацію та унікальність нашого месенджера. †昇天†",
                    "Этот эксклюзивный пиксельный бейдж принадлежит создателю и разработчику Miogram. Он символизирует оригинальную кибер-эстетику Needy Girl Overdose, безграничную кастомизацию и уникальность нашего мессенджера. †昇天†",
                    "This exclusive pixel badge belongs to the creator & developer of Miogram. It symbolizes the cyber aesthetic of Needy Girl Overdose, boundless customization, and uniqueness. †BLESS†"
            ));
        } else {
            descText.setText(app.miogram.bridge.MiogramLocale.get(
                    "Цей користувач має унікальну відзнаку в системі Miogram.",
                    "Этот пользователь имеет уникальное отличие в системе Miogram.",
                    "This user holds a unique distinction in the Miogram system."
            ));
        }
        descCard.addView(descText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(descCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 6. Action Button: Close
        TextView okButton = new TextView(context);
        okButton.setText(app.miogram.bridge.MiogramLocale.get("Зрозуміло ໒꒱", "Понятно ໒꒱", "Got it ໒꒱"));
        okButton.setTextSize(15.5f);
        okButton.setTypeface(AndroidUtilities.bold());
        okButton.setTextColor(Color.WHITE);
        okButton.setGravity(Gravity.CENTER);
        okButton.setBackground(app.miogram.bridge.ui.ame.MiogramAmeAesthetic.createAmeButtonDrawable(16));
        okButton.setPadding(0, AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15));
        okButton.setOnClickListener(v -> dismiss());

        root.addView(okButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
    }
}
