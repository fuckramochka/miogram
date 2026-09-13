package app.miogram.bridge.steam;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.utils.ViewOutlineProviderImpl;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Native Steam Profile Card for Telegram ProfileActivity.
 * Designed with authentic Steam dark navy aesthetic and Durov-level Telegram micro-interactions.
 * Displays current game, session/two-week playtime, live pulsing status dot,
 * and native quick-actions ("Зайти в гру", "Додати в друзі", "Профіль").
 */
public class MiogramSteamProfileCard extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final FrameLayout cardLayout;
    private final TextView steamBadgeView;
    private final TextView personaNameView;
    private final StatusDotView statusDotView;
    private final TextView statusTextView;

    // Game/Avatar content views
    private final LinearLayout contentRow;
    private final BackupImageView gameArtworkView;
    private final LinearLayout textColumn;
    private final TextView primaryTitleView;
    private final TextView subtitleView;
    private final TextView detailView;

    // Action buttons
    private final LinearLayout actionsRow;
    private final TextView btnPlayGame;
    private final TextView btnAddFriend;
    private final TextView btnOpenProfile;

    private MiogramSteamManager.SteamProfile currentProfile;

    public MiogramSteamProfileCard(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));

        cardLayout = new FrameLayout(context);
        // Steam gradient card background: deep navy with subtle 1dp border
        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF172435, 0xFF0E1622, 0xFF0A0F17}
        );
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        cardBg.setStroke(AndroidUtilities.dp(1), 0x3366C0F4);
        cardLayout.setBackground(cardBg);
        cardLayout.setClipToOutline(true);
        cardLayout.setOutlineProvider(ViewOutlineProviderImpl.fromDrawable(cardBg));
        cardLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        addView(cardLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        cardLayout.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 1. Header Row (Steam badge, persona name, status dot + label)
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        container.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Steam pill badge
        steamBadgeView = new TextView(context);
        steamBadgeView.setText("STEAM");
        steamBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        steamBadgeView.setTypeface(AndroidUtilities.bold());
        steamBadgeView.setTextColor(0xFF66C0F4);
        steamBadgeView.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x3366C0F4);
        badgeBg.setCornerRadius(AndroidUtilities.dp(5));
        badgeBg.setStroke(AndroidUtilities.dp(0.75f), 0x5566C0F4);
        steamBadgeView.setBackground(badgeBg);
        headerRow.addView(steamBadgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        // Persona name
        personaNameView = new TextView(context);
        personaNameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        personaNameView.setTypeface(AndroidUtilities.bold());
        personaNameView.setTextColor(0xFFD2DBE3);
        personaNameView.setSingleLine(true);
        personaNameView.setEllipsize(TextUtils.TruncateAt.END);
        NotificationCenter.listenEmojiLoading(personaNameView);
        headerRow.addView(personaNameView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        // Live status dot
        statusDotView = new StatusDotView(context);
        headerRow.addView(statusDotView, LayoutHelper.createLinear(8, 8, Gravity.CENTER_VERTICAL, 6, 0, 5, 0));

        // Status text label
        statusTextView = new TextView(context);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        statusTextView.setTypeface(AndroidUtilities.bold());
        headerRow.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // 2. Main Game/Profile Content Row
        contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        container.addView(contentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Game artwork / Steam Avatar
        gameArtworkView = new BackupImageView(context);
        gameArtworkView.setClipToOutline(true);
        gameArtworkView.setRoundRadius(AndroidUtilities.dp(10));
        contentRow.addView(gameArtworkView, LayoutHelper.createLinear(76, 52, 0, 0, 12, 0));

        // Text details column
        textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        contentRow.addView(textColumn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        primaryTitleView = new TextView(context);
        primaryTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        primaryTitleView.setTypeface(AndroidUtilities.bold());
        primaryTitleView.setTextColor(0xFFFFFFFF);
        primaryTitleView.setSingleLine(true);
        primaryTitleView.setEllipsize(TextUtils.TruncateAt.END);
        NotificationCenter.listenEmojiLoading(primaryTitleView);
        textColumn.addView(primaryTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(0xFF8FB0C6);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        NotificationCenter.listenEmojiLoading(subtitleView);
        textColumn.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        detailView = new TextView(context);
        detailView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        detailView.setTextColor(0xAA8FB0C6);
        detailView.setSingleLine(true);
        detailView.setEllipsize(TextUtils.TruncateAt.END);
        detailView.setVisibility(GONE);
        textColumn.addView(detailView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // 3. Actions Row
        actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Button 1: "Зайти в гру" (Steam Play green)
        btnPlayGame = createActionButton(
                context,
                "🎮 " + MiogramLocale.get("Зайти в гру", "Зайти в игру", "Launch Game"),
                0xFF5C7E10,
                0xFFFFFFFF
        );
        btnPlayGame.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            if (currentProfile != null && !TextUtils.isEmpty(currentProfile.gameId)) {
                MiogramSteamManager.getInstance().openGame(getContext(), currentProfile.gameId);
            }
        });
        actionsRow.addView(btnPlayGame, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));

        // Button 2: "Додати в друзі" (Steam cyan tint)
        btnAddFriend = createActionButton(
                context,
                "➕ " + MiogramLocale.get("У друзі", "В друзья", "Add Friend"),
                0x3366C0F4,
                0xFF66C0F4
        );
        btnAddFriend.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            if (currentProfile != null && !TextUtils.isEmpty(currentProfile.steamId)) {
                MiogramSteamManager.getInstance().addFriend(getContext(), currentProfile.steamId);
            }
        });
        actionsRow.addView(btnAddFriend, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

        // Button 3: "Профіль" (Steam dark tint)
        btnOpenProfile = createActionButton(
                context,
                "🌐 " + MiogramLocale.get("Профіль", "Профиль", "Profile"),
                0x2AFFFFFF,
                0xFFD2DBE3
        );
        btnOpenProfile.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            if (currentProfile != null) {
                MiogramSteamManager.getInstance().openProfile(getContext(), currentProfile.profileUrl, currentProfile.steamId);
            }
        });
        actionsRow.addView(btnOpenProfile, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));
    }

    private TextView createActionButton(Context context, String text, int bgColor, int textColor) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(AndroidUtilities.dp(10));
        btn.setBackground(bg);

        ScaleStateListAnimator.apply(btn, 0.035f, 1.4f);
        return btn;
    }

    /**
     * Binds SteamProfile data to the card.
     */
    public void setProfile(MiogramSteamManager.SteamProfile profile) {
        this.currentProfile = profile;
        if (profile == null) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);

        // Persona name
        String name = !TextUtils.isEmpty(profile.personaName) ? profile.personaName : ("ID: " + profile.steamId);
        personaNameView.setText(Emoji.replaceEmoji(name, personaNameView.getPaint().getFontMetricsInt(), false));

        if (profile.hasGame()) {
            // In-Game state
            statusDotView.setColor(0xFFA4D007, true);
            statusTextView.setText(MiogramLocale.get("У грі", "В игре", "In-Game"));
            statusTextView.setTextColor(0xFFA4D007);

            // Primary title is the game name
            primaryTitleView.setText(Emoji.replaceEmoji(profile.gameName, primaryTitleView.getPaint().getFontMetricsInt(), false));

            // Subtitle: hours played or state message
            if (!TextUtils.isEmpty(profile.gameHours2Weeks)) {
                String hoursText = profile.gameHours2Weeks + " " + MiogramLocale.get("год за останні 2 тижні", "ч за 2 недели", "hrs past 2 wks");
                subtitleView.setText(hoursText);
            } else if (!TextUtils.isEmpty(profile.stateMessage)) {
                subtitleView.setText(profile.stateMessage);
            } else {
                subtitleView.setText(MiogramLocale.get("Запущено на ПК", "Запущено на ПК", "Playing now on PC"));
            }

            // Optional extra detail
            if (!TextUtils.isEmpty(profile.stateMessage) && !TextUtils.isEmpty(profile.gameHours2Weeks)) {
                detailView.setVisibility(VISIBLE);
                detailView.setText(profile.stateMessage);
            } else {
                detailView.setVisibility(GONE);
            }

            // Load Game Artwork
            gameArtworkView.setRoundRadius(AndroidUtilities.dp(10));
            String capsuleUrl = !TextUtils.isEmpty(profile.gameId)
                    ? "https://cdn.cloudflare.steamstatic.com/steam/apps/" + profile.gameId + "/capsule_184x69.jpg"
                    : profile.gameIconUrl;

            if (!TextUtils.isEmpty(capsuleUrl)) {
                gameArtworkView.setImage(ImageLocation.getForPath(capsuleUrl), "184_69", (android.graphics.drawable.Drawable) null, 0, null);
            } else if (!TextUtils.isEmpty(profile.avatarUrl)) {
                gameArtworkView.setImage(ImageLocation.getForPath(profile.avatarUrl), "100_100", (android.graphics.drawable.Drawable) null, 0, null);
            } else {
                gameArtworkView.setImageResource(R.drawable.baseline_videogame_asset_16);
            }

            // "Зайти в гру" is active
            btnPlayGame.setVisibility(VISIBLE);
        } else {
            // Online or Offline state
            boolean isOnline = profile.isInGame || (profile.stateMessage != null && profile.stateMessage.toLowerCase().contains("online"));
            int dotColor = isOnline ? 0xFF66C0F4 : 0xFF8A94A0;
            statusDotView.setColor(dotColor, false);

            String statusStr = isOnline
                    ? MiogramLocale.get("В мережі", "В сети", "Online")
                    : MiogramLocale.get("Не в мережі", "Не в сети", "Offline");
            statusTextView.setText(statusStr);
            statusTextView.setTextColor(dotColor);

            primaryTitleView.setText(Emoji.replaceEmoji(name, primaryTitleView.getPaint().getFontMetricsInt(), false));
            subtitleView.setText(!TextUtils.isEmpty(profile.stateMessage)
                    ? profile.stateMessage
                    : MiogramLocale.get("Зараз не у грі", "Сейчас не в игре", "Not in game"));
            detailView.setVisibility(GONE);

            // Load Steam Avatar
            gameArtworkView.setRoundRadius(AndroidUtilities.dp(26));
            if (!TextUtils.isEmpty(profile.avatarUrl)) {
                gameArtworkView.setImage(ImageLocation.getForPath(profile.avatarUrl), "100_100", (android.graphics.drawable.Drawable) null, 0, null);
            } else {
                gameArtworkView.setImageResource(R.drawable.baseline_videogame_asset_16);
            }

            // Hide "Зайти в гру" when not in game
            btnPlayGame.setVisibility(GONE);
        }
    }

    /**
     * Pulsing dot for live status.
     */
    private static class StatusDotView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ObjectAnimator pulseAnim;
        private float pulseAlpha = 0.4f;

        public StatusDotView(Context context) {
            super(context);
        }

        public void setPulseAlpha(float alpha) {
            this.pulseAlpha = alpha;
            invalidate();
        }

        public float getPulseAlpha() {
            return pulseAlpha;
        }

        public void setColor(int color, boolean pulse) {
            paint.setColor(color);
            glowPaint.setColor(color);
            glowPaint.setAlpha(70);

            if (pulse) {
                if (pulseAnim == null) {
                    pulseAnim = ObjectAnimator.ofFloat(this, "pulseAlpha", 0.15f, 0.85f);
                    pulseAnim.setDuration(1200);
                    pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
                    pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
                    pulseAnim.start();
                }
            } else {
                if (pulseAnim != null) {
                    pulseAnim.cancel();
                    pulseAnim = null;
                }
                pulseAlpha = 0.4f;
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy);

            // Outer pulse glow
            glowPaint.setAlpha((int) (pulseAlpha * 120));
            canvas.drawCircle(cx, cy, r, glowPaint);

            // Core solid dot
            canvas.drawCircle(cx, cy, r * 0.65f, paint);
        }
    }
}
