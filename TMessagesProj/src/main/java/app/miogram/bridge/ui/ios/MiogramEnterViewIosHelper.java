package app.miogram.bridge.ui.ios;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatActivityEnterView;

/**
 * 1:1 Native Implementation of iOS Cupertino Input Panel for ChatActivityEnterView:
 * - Floating rounded capsule edit text field (18dp corner radius with subtle glass contrast fill).
 * - iOS Circular action button with upward send arrow.
 * - Symmetrical margins and smooth Cupertino transitions.
 */
public class MiogramEnterViewIosHelper {

    private static final String PREFS_NAME = "miogram_chats_prefs";
    private static final String KEY_IOS_INPUT_PANEL = "ios_input_panel";

    public static boolean isIosInputPanelEnabled() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IOS_INPUT_PANEL, true);
    }

    public static void setIosInputPanelEnabled(boolean enabled) {
        ApplicationLoader.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IOS_INPUT_PANEL, enabled)
                .apply();
    }

    /**
     * Resolves the authentic iOS Cupertino text input capsule background.
     */
    public static int getCapsuleBackgroundColor() {
        boolean dark = Theme.isCurrentThemeDark();
        return dark ? 0x2D767680 : 0x18767680;
    }

    /**
     * Resolves the authentic iOS send button accent blue.
     */
    public static int getIosSendButtonColor(Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.key_chat_messagePanelSend, resourcesProvider);
    }

    /**
     * Applies native iOS Cupertino styling to ChatActivityEnterView.
     */
    public static void apply(ChatActivityEnterView enterView) {
        if (enterView == null || !isIosInputPanelEnabled()) {
            return;
        }

        try {
            // 1. Style messageEditTextContainer as a rounded Cupertino capsule
            if (enterView.messageEditTextContainer != null) {
                GradientDrawable capsule = new GradientDrawable();
                capsule.setShape(GradientDrawable.RECTANGLE);
                capsule.setCornerRadius(AndroidUtilities.dp(18));
                capsule.setColor(getCapsuleBackgroundColor());

                if (Theme.isCurrentThemeDark()) {
                    capsule.setStroke(AndroidUtilities.dp(0.5f), 0x22FFFFFF);
                }

                enterView.messageEditTextContainer.setBackground(capsule);

                ViewGroup.LayoutParams vlp = enterView.messageEditTextContainer.getLayoutParams();
                if (vlp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) vlp;
                    mlp.leftMargin = AndroidUtilities.dp(8);
                    mlp.rightMargin = AndroidUtilities.dp(48);
                    mlp.topMargin = AndroidUtilities.dp(4);
                    mlp.bottomMargin = AndroidUtilities.dp(4);
                    enterView.messageEditTextContainer.setLayoutParams(mlp);
                }
            }

            // 2. Style sendButton icon to upward arrow when not in scheduling mode
            if (enterView.sendButton != null && !enterView.isInScheduleMode()) {
                enterView.sendButton.setResourceId(R.drawable.baseline_arrow_upward_24);
            }

            // 3. Align send button container to match circular 36dp Cupertino footprint
            if (enterView.sendButtonContainer != null) {
                ViewGroup.LayoutParams svlp = enterView.sendButtonContainer.getLayoutParams();
                if (svlp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams smlp = (ViewGroup.MarginLayoutParams) svlp;
                    smlp.rightMargin = AndroidUtilities.dp(4);
                    smlp.bottomMargin = AndroidUtilities.dp(3);
                    enterView.sendButtonContainer.setLayoutParams(smlp);
                }
            }

        } catch (Throwable t) {
            org.telegram.messenger.FileLog.e("MiogramEnterViewIosHelper apply error: " + t.getMessage());
        }
    }
}
