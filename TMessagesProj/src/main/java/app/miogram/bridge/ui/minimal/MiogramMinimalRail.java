package app.miogram.bridge.ui.minimal;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProfileActivity;

import android.os.Bundle;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.divine.MiogramDivineEngine;
import app.miogram.bridge.settings.MiogramSettingsActivity;

/**
 * Slim left navigation rail for the MINIMALIST preset.
 *
 * <p>The preset hides the bottom navigation, which used to leave users with
 * no way to reach Saved/Profile except the drawer. This rail restores
 * one-tap navigation on the left edge: Chats, Saved, Profile, Settings.
 * Theme-adaptive (unlike the fixed-dark Discord rail) and zero-cost when
 * another preset is active (the host only builds it then).
 */
public final class MiogramMinimalRail {

    public static final int RAIL_WIDTH_DP = 60;

    private MiogramMinimalRail() {}

    public static boolean isActive(Context context) {
        if (app.miogram.bridge.ui.discord.MiogramDiscordLayout.isDiscordUiEnabled()) return false;
        if (app.miogram.bridge.ui.ios.MiogramIosLayout.isIosPresetActive(context)) return false;
        return MiogramDivineEngine.isMinimalistActive(context);
    }

    public static View createMinimalRail(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setLayoutParams(new ViewGroup.LayoutParams(AndroidUtilities.dp(RAIL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(0, Math.max(0, AndroidUtilities.statusBarHeight - AndroidUtilities.dp(4)), 0, AndroidUtilities.dp(12));

        int accent = Theme.getColor(Theme.key_chats_actionBackground);
        int inactive = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        int ripple = Theme.getColor(Theme.key_listSelector);

        // Chats (we are here — always the active one in the dialog list).
        root.addView(makeButton(context, R.drawable.msg_list_solar,
                MiogramLocale.get("Чати", "Чаты", "Chats"), accent, ripple, v -> {
                    haptic(v);
                }), LayoutHelper.createLinear(52, 52, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

        // Saved Messages.
        root.addView(makeButton(context, R.drawable.baseline_bookmark_24,
                MiogramLocale.get("Збережені", "Избранное", "Saved"), inactive, ripple, v -> {
                    haptic(v);
                    openSelfChat(context, true);
                }), LayoutHelper.createLinear(52, 52, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

        // Profile (own avatar doubles as the button).
        root.addView(makeAvatarButton(context, ripple), LayoutHelper.createLinear(52, 52, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

        // Settings.
        root.addView(makeButton(context, R.drawable.baseline_settings_24,
                MiogramLocale.get("Налаштування", "Настройки", "Settings"), inactive, ripple, v -> {
                    haptic(v);
                    openFragment(new MiogramSettingsActivity());
                }), LayoutHelper.createLinear(52, 52, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 4));

        return root;
    }

    private static View makeButton(Context context, int iconRes, String desc, int tint, int ripple, View.OnClickListener l) {
        FrameLayout box = new FrameLayout(context);
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tint);
        icon.setBackground(Theme.createSelectorDrawable(ripple, Theme.RIPPLE_MASK_CIRCLE_20DP));
        icon.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        icon.setContentDescription(desc);
        icon.setOnClickListener(l);
        box.addView(icon, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
        box.setContentDescription(desc);
        return box;
    }

    private static View makeAvatarButton(Context context, int ripple) {
        FrameLayout box = new FrameLayout(context);
        int account = UserConfig.selectedAccount;
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(20));
        try {
            TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
            if (self != null) {
                avatar.setForUserOrChat(self, new AvatarDrawable(self));
            } else {
                avatar.setImageDrawable(new AvatarDrawable());
            }
        } catch (Throwable ignored) {
            avatar.setImageDrawable(new AvatarDrawable());
        }
        avatar.setBackground(Theme.createSelectorDrawable(ripple, Theme.RIPPLE_MASK_CIRCLE_20DP));
        avatar.setContentDescription(MiogramLocale.get("Профіль", "Профиль", "Profile"));
        avatar.setOnClickListener(v -> {
            haptic(v);
            try {
                TLRPC.User self = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
                if (self == null) return;
                Bundle args = new Bundle();
                args.putLong("user_id", self.id);
                openFragment(new ProfileActivity(args));
            } catch (Throwable ignored) {}
        });
        box.addView(avatar, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
        return box;
    }

    private static void openSelfChat(Context context, boolean saved) {
        try {
            long selfId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
            if (selfId == 0) return;
            Bundle args = new Bundle();
            args.putLong("user_id", selfId);
            openFragment(new ChatActivity(args));
        } catch (Throwable ignored) {}
    }

    private static void openFragment(BaseFragment fragment) {
        try {
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment last = act.getSafeLastFragment();
                if (last != null) last.presentFragment(fragment);
            }
        } catch (Throwable ignored) {}
    }

    private static void haptic(View v) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignored) {}
    }
}
