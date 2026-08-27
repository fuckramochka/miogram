package app.miogram.bridge.ui.discord;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

import app.miogram.bridge.MiogramLocale;

/**
 * Discord-Style Alternative Layout Controller for Miogram:
 * - Left vertical server/folder rail with white active pill indicators
 * - Discord hashtag channel prefix and categories
 * - Bottom Discord status bar with avatar, status badge and quick controls
 */
public class MiogramDiscordLayout {

    private static final String PREFS = "miogram_ui_prefs";
    private static final String KEY_UI_MODE = "interface_layout_mode";

    public static final int UI_MODE_TELEGRAM = 0;
    public static final int UI_MODE_DISCORD = 1;

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDiscordUiEnabled() {
        return getPrefs().getInt(KEY_UI_MODE, UI_MODE_TELEGRAM) == UI_MODE_DISCORD;
    }

    public static void setDiscordUiEnabled(boolean enabled) {
        getPrefs().edit().putInt(KEY_UI_MODE, enabled ? UI_MODE_DISCORD : UI_MODE_TELEGRAM).apply();
    }

    public static int getUiMode() {
        return getPrefs().getInt(KEY_UI_MODE, UI_MODE_TELEGRAM);
    }

    public static void setUiMode(int mode) {
        getPrefs().edit().putInt(KEY_UI_MODE, mode).apply();
    }

    /**
     * Creates Discord-Style Left Guild / Folder Rail View
     */
    public static View createDiscordServerRail(Context context, OnServerSelectedListener listener) {
        LinearLayout rail = new LinearLayout(context);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setBackgroundColor(0xFF1E1F22); // Discord dark rail bg
        rail.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12));
        rail.setLayoutParams(new ViewGroup.LayoutParams(AndroidUtilities.dp(68), ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Home / Direct Messages Icon
        ImageView homeIcon = new ImageView(context);
        homeIcon.setImageResource(R.drawable.msg_bot);
        homeIcon.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), 0xFF5865F2)); // Discord Blurple
        homeIcon.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        rail.addView(homeIcon, LayoutHelper.createLinear(48, 48, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));
        homeIcon.setOnClickListener(v -> {
            if (listener != null) listener.onServerSelected(-1);
        });

        // Separator
        View sep = new View(context);
        sep.setBackgroundColor(0xFF35363C);
        rail.addView(sep, LayoutHelper.createLinear(32, 2, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        // 2. Folder / Server list
        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout folderContainer = new LinearLayout(context);
        folderContainer.setOrientation(LinearLayout.VERTICAL);

        int currentAccount = UserConfig.selectedAccount;
        ArrayList<MessagesController.DialogFilter> filters = MessagesController.getInstance(currentAccount).dialogFilters;
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                MessagesController.DialogFilter filter = filters.get(i);
                final int filterId = filter.id;

                FrameLayout item = new FrameLayout(context);

                TextView letterBadge = new TextView(context);
                String name = filter.name != null && !filter.name.isEmpty() ? filter.name : "G";
                letterBadge.setText(name.substring(0, Math.min(2, name.length())).toUpperCase());
                letterBadge.setTextColor(0xFFDBDEE1);
                letterBadge.setTextSize(14);
                letterBadge.setTypeface(AndroidUtilities.bold());
                letterBadge.setGravity(Gravity.CENTER);
                letterBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0xFF2B2D31));

                item.addView(letterBadge, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
                item.setOnClickListener(v -> {
                    if (listener != null) listener.onServerSelected(filterId);
                });

                folderContainer.addView(item, LayoutHelper.createLinear(48, 48, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));
            }
        }

        sv.addView(folderContainer);
        rail.addView(sv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        // 3. User Avatar & Profile Bar at Bottom
        BackupImageView userAvatar = new BackupImageView(context);
        userAvatar.setRoundRadius(AndroidUtilities.dp(20));
        TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (self != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(self);
            userAvatar.setForUserOrChat(self, avatarDrawable);
        }
        rail.addView(userAvatar, LayoutHelper.createLinear(40, 40, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));

        return rail;
    }

    public interface OnServerSelectedListener {
        void onServerSelected(int folderId);
    }
}
