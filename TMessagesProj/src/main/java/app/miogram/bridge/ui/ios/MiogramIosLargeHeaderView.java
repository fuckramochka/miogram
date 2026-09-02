package app.miogram.bridge.ui.ios;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import app.miogram.bridge.MiogramLocale;

/**
 * Direct 1:1 implementation of Telegram-iOS Large Title Navigation Bar Header
 * based on `NavigationBar.swift` and `ChatListControllerNode.swift`.
 * Supports dynamic scroll collapse of 34sp Large Title into 17sp inline title.
 */
public class MiogramIosLargeHeaderView extends FrameLayout {

    private final FrameLayout topBar;
    private final TextView editButton;
    private final TextView inlineTitleView;
    private final ImageView composeButton;
    private final View topBarDivider;

    private final LinearLayout collapsingContainer;
    private final TextView largeTitleView;
    private final FrameLayout searchBox;

    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public interface OnHeaderActionListener {
        void onEditClick();
        void onComposeClick();
        void onSearchClick();
    }

    public MiogramIosLargeHeaderView(Context context, String title, OnHeaderActionListener listener) {
        super(context);
        setBackgroundColor(MiogramIosTheme.getNavBarBg());

        int statusBarHeight = AndroidUtilities.statusBarHeight;
        int topBarHeight = AndroidUtilities.dp(44);

        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(AndroidUtilities.dp(0.5f));
        dividerPaint.setColor(MiogramIosTheme.getNavBarSeparator());

        // 1. Fixed Top Bar (44dp + status bar)
        topBar = new FrameLayout(context);
        topBar.setPadding(AndroidUtilities.dp(16), statusBarHeight, AndroidUtilities.dp(16), 0);
        addView(topBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, topBarHeight + statusBarHeight, Gravity.TOP));

        // Left: "Ред." (Edit) in iOS Blue
        editButton = new TextView(context);
        editButton.setText(MiogramLocale.get("Ред.", "Изм.", "Edit"));
        editButton.setTextColor(MiogramIosTheme.getAccent());
        editButton.setTextSize(17);
        if (listener != null) {
            editButton.setOnClickListener(v -> listener.onEditClick());
        }
        topBar.addView(editButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        // Center: Inline title (17sp semibold, initially hidden)
        inlineTitleView = new TextView(context);
        inlineTitleView.setText(title != null ? title : MiogramLocale.get("Чати", "Чаты", "Chats"));
        inlineTitleView.setTextColor(MiogramIosTheme.getChatListTitle());
        inlineTitleView.setTextSize(17);
        inlineTitleView.setTypeface(AndroidUtilities.bold());
        inlineTitleView.setAlpha(0f);
        topBar.addView(inlineTitleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Right: Compose icon in iOS Blue
        composeButton = new ImageView(context);
        composeButton.setImageResource(R.drawable.msg_edit);
        composeButton.setColorFilter(MiogramIosTheme.getAccent());
        if (listener != null) {
            composeButton.setOnClickListener(v -> listener.onComposeClick());
        }
        topBar.addView(composeButton, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        // Top Bar bottom hairline divider (0.5dp, initially hidden)
        topBarDivider = new View(context);
        topBarDivider.setBackgroundColor(MiogramIosTheme.getNavBarSeparator());
        topBarDivider.setAlpha(0f);
        topBar.addView(topBarDivider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM));

        // 2. Collapsing Container (Large Title + Search Bar)
        collapsingContainer = new LinearLayout(context);
        collapsingContainer.setOrientation(LinearLayout.VERTICAL);
        collapsingContainer.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        FrameLayout.LayoutParams ccLp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        ccLp.topMargin = statusBarHeight + topBarHeight;
        addView(collapsingContainer, ccLp);

        // Large Title: 34sp bold
        largeTitleView = new TextView(context);
        largeTitleView.setText(title != null ? title : MiogramLocale.get("Чати", "Чаты", "Chats"));
        largeTitleView.setTextSize(34);
        largeTitleView.setTypeface(AndroidUtilities.bold());
        largeTitleView.setTextColor(MiogramIosTheme.getChatListTitle());
        largeTitleView.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(6));
        collapsingContainer.addView(largeTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Cupertino Search Bar (36dp, radius 10dp)
        searchBox = new FrameLayout(context);
        searchBox.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), MiogramIosTheme.getSearchInputFill()));
        searchBox.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);

        LinearLayout searchInner = new LinearLayout(context);
        searchInner.setOrientation(LinearLayout.HORIZONTAL);
        searchInner.setGravity(Gravity.CENTER);

        ImageView searchIcon = new ImageView(context);
        searchIcon.setImageResource(R.drawable.msg_search);
        searchIcon.setColorFilter(MiogramIosTheme.getSearchPlaceholder());
        searchInner.addView(searchIcon, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL));

        TextView hint = new TextView(context);
        hint.setText(" " + MiogramLocale.get("Пошук", "Поиск", "Search"));
        hint.setTextColor(MiogramIosTheme.getSearchPlaceholder());
        hint.setTextSize(15);
        searchInner.addView(hint, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        searchBox.addView(searchInner, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        if (listener != null) {
            searchBox.setOnClickListener(v -> listener.onSearchClick());
        }
        collapsingContainer.addView(searchBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
    }

    /**
     * Binds scroll position of the chat list to coordinate 1:1 iOS collapsing behavior.
     */
    public void onScrollOffsetChanged(int scrollY) {
        float maxScroll = AndroidUtilities.dp(48);
        float progress = Math.min(1.0f, Math.max(0.0f, scrollY / maxScroll));

        // Fade out large title
        largeTitleView.setAlpha(1.0f - progress);
        largeTitleView.setScaleX(1.0f - progress * 0.1f);
        largeTitleView.setScaleY(1.0f - progress * 0.1f);

        // Fade in inline title & top bar hairline
        inlineTitleView.setAlpha(progress);
        topBarDivider.setAlpha(progress);
    }

    public void setTitle(String title) {
        if (inlineTitleView != null) inlineTitleView.setText(title);
        if (largeTitleView != null) largeTitleView.setText(title);
    }

    public int getHeaderTotalHeight() {
        return AndroidUtilities.statusBarHeight + AndroidUtilities.dp(44 + 48 + 36 + 10);
    }
}
