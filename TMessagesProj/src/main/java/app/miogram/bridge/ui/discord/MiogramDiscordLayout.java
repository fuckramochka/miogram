package app.miogram.bridge.ui.discord;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.FiltersSetupActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.HashMap;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.settings.MiogramSettingsActivity;

/**
 * Native Discord interface components for Miogram.
 *
 * The rail mirrors Discord's guild list behaviour: squircle icons that morph
 * between 24dp and 16dp corner radii on selection, a white pill indicator that
 * grows from a 8dp nub to a 40dp bar, red mention badges and a green "add"
 * button. Selection is persisted across restarts.
 *
 * Semantics of rail selection ids handed to {@link OnServerSelectedListener}:
 *   {@link #RAIL_HOME} (-1)  -> All chats (default folder, id 0)
 *   positive                 -> folder id (a "server")
 *   negative (other)         -> dialog id of a group chat opened as a server
 */
public class MiogramDiscordLayout {

    private static final String PREFS = "miogram_ui_prefs";
    private static final String KEY_UI_MODE = "interface_layout_mode";
    private static final String KEY_SELECTED = "discord_rail_selected";
    private static final String KEY_MIC_MUTED = "discord_mic_muted";
    private static final String KEY_DEAFENED = "discord_deafened";

    public static final int RAIL_HOME = -1;

    public static final int UI_MODE_TELEGRAM = 0;
    public static final int UI_MODE_DISCORD = 1;

    // Discord palette
    public static final int COLOR_RAIL_BG = 0xFF1E1F22;
    public static final int COLOR_CHANNELS_BG = 0xFF2B2D31;
    public static final int COLOR_CHAT_BG = 0xFF313338;
    public static final int COLOR_BLURPLE = 0xFF5865F2;
    public static final int COLOR_TEXT_PRIMARY = 0xFFDBDEE1;
    public static final int COLOR_TEXT_MUTED = 0xFF949BA4;
    public static final int COLOR_ONLINE_GREEN = 0xFF23A55A;
    public static final int COLOR_BADGE_RED = 0xFFF23F43;
    public static final int COLOR_HEADER_BG = 0xFF232428;
    public static final int COLOR_CHANNEL_ACTIVE = 0xFF404249;
    public static final int COLOR_SEPARATOR = 0xFF35363C;
    public static final int COLOR_INPUT_BG = 0xFF383A40;

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDiscordUiEnabled() {
        return getPrefs().getInt(KEY_UI_MODE, UI_MODE_TELEGRAM) == UI_MODE_DISCORD
                || app.miogram.bridge.divine.MiogramDivineEngine.getCurrentPreset(null) == app.miogram.bridge.divine.MiogramDivineEngine.Preset.DISCORD_ULTRA;
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

    public static int getSelectedRailId() {
        return getPrefs().getInt(KEY_SELECTED, RAIL_HOME);
    }

    public static void setSelectedRailId(int id) {
        getPrefs().edit().putInt(KEY_SELECTED, id).apply();
    }

    private static final Paint channelIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint unreadPillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    static {
        channelIconPaint.setStyle(Paint.Style.STROKE);
        channelIconPaint.setStrokeCap(Paint.Cap.ROUND);
        unreadPillPaint.setColor(0xFFFFFFFF);
        unreadPillPaint.setStyle(Paint.Style.FILL);
    }

    public static void drawChannelIcon(Canvas canvas, float x, float cy, boolean unread) {
        channelIconPaint.setColor(unread ? 0xFFFFFFFF : COLOR_TEXT_MUTED);
        channelIconPaint.setStrokeWidth(AndroidUtilities.dp(1.8f));
        float size = AndroidUtilities.dp(8);
        canvas.drawLine(x - size * 0.25f, cy - size, x - size * 0.45f, cy + size, channelIconPaint);
        canvas.drawLine(x + size * 0.35f, cy - size, x + size * 0.15f, cy + size, channelIconPaint);
        canvas.drawLine(x - size * 0.8f, cy - size * 0.35f, x + size * 0.8f, cy - size * 0.35f, channelIconPaint);
        canvas.drawLine(x - size * 0.8f, cy + size * 0.35f, x + size * 0.8f, cy + size * 0.35f, channelIconPaint);

        if (unread) {
            rectF.set(0, cy - AndroidUtilities.dp(4), AndroidUtilities.dp(4), cy + AndroidUtilities.dp(4));
            canvas.drawRoundRect(rectF, AndroidUtilities.dp(2), AndroidUtilities.dp(2), unreadPillPaint);
        }
    }

    public interface OnServerSelectedListener {
        void onServerSelected(int id);
    }

    // ------------------------------------------------------------------
    // Server / guild rail
    // ------------------------------------------------------------------

    public static View createDiscordServerRail(Context context, OnServerSelectedListener listener) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_RAIL_BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(AndroidUtilities.dp(72), ViewGroup.LayoutParams.MATCH_PARENT));

        int currentAccount = UserConfig.selectedAccount;
        int selectedId = getSelectedRailId();

        // Unread map for badges (built once per rail construction).
        HashMap<Long, TLRPC.Dialog> dialogById = new HashMap<>();
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getDialogs(0);
        int dmUnread = 0;
        if (allDialogs != null) {
            for (TLRPC.Dialog d : allDialogs) {
                if (d != null) {
                    dialogById.put(d.id, d);
                    if (d.id > 0) dmUnread += d.unread_count;
                }
            }
        }

        // 1. Home / Direct Messages
        RailItemView homeItem = new RailItemView(context, true);
        homeItem.setHome(dmUnread);
        homeItem.setSelectedVisual(selectedId == RAIL_HOME);
        homeItem.setOnClickListener(v -> {
            setSelectedRailId(RAIL_HOME);
            homeItem.animateSelection(true);
            if (listener != null) listener.onServerSelected(RAIL_HOME);
        });
        root.addView(homeItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(68), 0, 8, 0, 2));

        View sep = new View(context);
        sep.setBackgroundColor(COLOR_SEPARATOR);
        root.addView(sep, LayoutHelper.createLinear(32, 2, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 6));

        // 2. Folders as "servers"
        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout serverList = new LinearLayout(context);
        serverList.setOrientation(LinearLayout.VERTICAL);

        ArrayList<MessagesController.DialogFilter> filters = MessagesController.getInstance(currentAccount).dialogFilters;
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                MessagesController.DialogFilter filter = filters.get(i);
                if (filter == null || filter.id == 0) continue; // 0 = default All chats, already the home button
                final int filterId = filter.id;

                int folderUnread = 0;
                if (filter.alwaysShow != null) {
                    for (int k = 0; k < filter.alwaysShow.size() && folderUnread < 99; k++) {
                        TLRPC.Dialog d = dialogById.get(filter.alwaysShow.get(k));
                        if (d != null) folderUnread += d.unread_count;
                    }
                }

                RailItemView item = new RailItemView(context, false);
                TLRPC.Chat chat = null;
                TLRPC.User user = null;
                if (filter.alwaysShow != null && !filter.alwaysShow.isEmpty()) {
                    for (int k = 0; k < filter.alwaysShow.size(); k++) {
                        long did = filter.alwaysShow.get(k);
                        if (did < 0) {
                            chat = MessagesController.getInstance(currentAccount).getChat(-did);
                            if (chat != null) break;
                        } else if (did > 0) {
                            user = MessagesController.getInstance(currentAccount).getUser(did);
                            if (user != null) break;
                        }
                    }
                }
                if (chat != null) {
                    item.setAvatar(currentAccount, chat);
                } else if (user != null) {
                    item.setAvatar(currentAccount, user);
                } else {
                    item.setLetters(filter.name != null ? filter.name : "");
                }
                item.setBadge(folderUnread);
                item.setSelectedVisual(selectedId == filterId);
                item.setOnClickListener(v -> {
                    setSelectedRailId(filterId);
                    item.animateSelection(true);
                    if (listener != null) listener.onServerSelected(filterId);
                });
                serverList.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56)));
            }
        }

        // Group chats without a folder also appear as servers (parity with prior behaviour).
        if (allDialogs != null) {
            for (int i = 0; i < allDialogs.size() && i < 30; i++) {
                TLRPC.Dialog dialog = allDialogs.get(i);
                if (dialog == null || dialog.id >= 0) continue;
                final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                if (chat == null) continue;
                final long dialogId = dialog.id;
                // An id cast to int keeps folder ids and dialog ids in one int space; dialog ids are negative.
                final int railId = (int) dialogId;

                RailItemView item = new RailItemView(context, false);
                item.setAvatar(currentAccount, chat);
                item.setBadge(dialog.unread_count);
                item.setSelectedVisual(selectedId == railId);
                item.setOnClickListener(v -> {
                    setSelectedRailId(railId);
                    item.animateSelection(true);
                    if (listener != null) listener.onServerSelected(railId);
                });
                serverList.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56)));
            }
        }

        sv.addView(serverList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        // 3. Green "+" — create a folder (Discord: "Add a Server")
        RailItemView addItem = new RailItemView(context, false);
        addItem.setAddAction();
        addItem.setOnClickListener(v -> {
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new FiltersSetupActivity());
                }
            }
        });
        root.addView(addItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56)));

        // 4. Self avatar + status dot pinned to the bottom of the rail
        FrameLayout userBox = new FrameLayout(context);
        userBox.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(10));

        FrameLayout avatarBox = new FrameLayout(context);
        BackupImageView userAvatar = new BackupImageView(context);
        userAvatar.setRoundRadius(AndroidUtilities.dp(20));
        TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (self != null) {
            userAvatar.setForUserOrChat(self, new AvatarDrawable(self));
        }
        avatarBox.addView(userAvatar, LayoutHelper.createFrame(42, 42, Gravity.CENTER));

        View onlineDot = new View(context);
        onlineDot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), COLOR_ONLINE_GREEN));
        avatarBox.addView(onlineDot, LayoutHelper.createFrame(12, 12, Gravity.CENTER, 14, 14, 0, 0));

        avatarBox.setOnClickListener(v -> {
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new MiogramSettingsActivity());
                }
            }
        });
        userBox.addView(avatarBox, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(52), Gravity.CENTER));

        root.addView(userBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return root;
    }

    // ------------------------------------------------------------------
    // User panel (bottom of the channel pane)
    // ------------------------------------------------------------------

    public static View createDiscordUserFooter(Context context) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(COLOR_HEADER_BG);
        bar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));

        int currentAccount = UserConfig.selectedAccount;
        TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(16));
        if (self != null) {
            avatar.setForUserOrChat(self, new AvatarDrawable(self));
        }
        bar.addView(avatar, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(context);
        nameView.setText(self != null && self.first_name != null ? self.first_name : "User");
        nameView.setTextSize(12.5f);
        nameView.setTypeface(AndroidUtilities.bold());
        nameView.setTextColor(COLOR_TEXT_PRIMARY);
        nameView.setSingleLine(true);
        textGroup.addView(nameView);

        TextView tagView = new TextView(context);
        tagView.setText(self != null && self.username != null ? "@" + self.username : MiogramLocale.get("Онлайн", "Онлайн", "Online"));
        tagView.setTextSize(10.5f);
        tagView.setTextColor(COLOR_TEXT_MUTED);
        tagView.setSingleLine(true);
        textGroup.addView(tagView);

        bar.addView(textGroup, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        MicButton micBtn = new MicButton(context);
        micBtn.setMuted(getPrefs().getBoolean(KEY_MIC_MUTED, false));
        micBtn.setOnClickListener(v -> {
            boolean muted = !getPrefs().getBoolean(KEY_MIC_MUTED, false);
            getPrefs().edit().putBoolean(KEY_MIC_MUTED, muted).apply();
            micBtn.setMuted(muted);
        });
        bar.addView(micBtn, LayoutHelper.createLinear(30, 30, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));

        DeafenButton deafenBtn = new DeafenButton(context);
        deafenBtn.setDeafened(getPrefs().getBoolean(KEY_DEAFENED, false));
        deafenBtn.setOnClickListener(v -> {
            boolean deafened = !getPrefs().getBoolean(KEY_DEAFENED, false);
            getPrefs().edit().putBoolean(KEY_DEAFENED, deafened).apply();
            deafenBtn.setDeafened(deafened);
            if (deafened) {
                getPrefs().edit().putBoolean(KEY_MIC_MUTED, true).apply();
                micBtn.setMuted(true);
            }
        });
        bar.addView(deafenBtn, LayoutHelper.createLinear(30, 30, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));

        View settingsBtn = new SettingsGearView(context);
        settingsBtn.setOnClickListener(v -> {
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new MiogramSettingsActivity());
                }
            }
        });
        bar.addView(settingsBtn, LayoutHelper.createLinear(30, 30, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

        return bar;
    }

    // ------------------------------------------------------------------
    // Channel pane header
    // ------------------------------------------------------------------

    /** Resolves the active "server" title: selected folder name or DMs default. */
    public static String channelPaneTitle(Context context) {
        int selected = getSelectedRailId();
        if (selected != RAIL_HOME) {
            ArrayList<MessagesController.DialogFilter> filters =
                    MessagesController.getInstance(UserConfig.selectedAccount).dialogFilters;
            if (filters != null) {
                for (int i = 0; i < filters.size(); i++) {
                    MessagesController.DialogFilter f = filters.get(i);
                    if (f != null && f.id == selected && f.name != null && !f.name.isEmpty()) {
                        return f.name;
                    }
                }
            }
        }
        return MiogramLocale.get("Повідомлення", "Сообщения", "Messages");
    }

    public static View createDiscordChannelHeader(Context context, String title, Runnable onSearchClick) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_HEADER_BG);
        header.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        // Discord channel glyph: # hash before the title.
        HashGlyphView hash = new HashGlyphView(context);
        header.addView(hash, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView titleView = new TextView(context);
        titleView.setText(title != null && !title.isEmpty() ? title : MiogramLocale.get("Повідомлення", "Сообщения", "Messages"));
        titleView.setTextColor(COLOR_TEXT_PRIMARY);
        titleView.setTextSize(16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(true);

        header.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        View search = new SearchGlyphView(context);
        search.setOnClickListener(v -> {
            if (onSearchClick != null) onSearchClick.run();
        });
        header.addView(search, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        return header;
    }

    /** Discord channel hash glyph. */
    private static class HashGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HashGlyphView(Context context) {
            super(context);
            paint.setColor(COLOR_TEXT_MUTED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            canvas.drawLine(w * 0.30f, 0, w * 0.18f, h, paint);
            canvas.drawLine(w * 0.72f, 0, w * 0.60f, h, paint);
            canvas.drawLine(0, h * 0.38f, w, h * 0.38f, paint);
            canvas.drawLine(0, h * 0.66f, w, h * 0.66f, paint);
        }
    }

    // ==================================================================
    // Custom views
    // ==================================================================

    /**
     * One rail row: white pill indicator on the left edge, a squircle icon that
     * morphs its corner radius between 24dp (idle) and 16dp (selected, Discord's
     * squircle-to-rounded-square morph) and an optional red unread badge.
     */
    private static class RailItemView extends FrameLayout {
        private final boolean isHome;

        private final View pill;
        private final FrameLayout iconBox;
        private final BackupImageView avatarView;
        private final TextView letterBadge;
        private final HomeGlyphView homeGlyph;
        private final AddGlyphView addGlyph;
        private final TextView countBadge;

        private float selectionT = 0f; // 0 idle .. 1 selected
        private ValueAnimator animator;

        public RailItemView(Context context, boolean home) {
            super(context);
            this.isHome = home;

            pill = new View(context);
            pill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0xFFFFFFFF));
            addView(pill, LayoutHelper.createFrame(4, 8, Gravity.LEFT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            iconBox = new FrameLayout(context);
            addView(iconBox, LayoutHelper.createFrame(48, 48, Gravity.CENTER));

            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(24));
            iconBox.addView(avatarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            letterBadge = new TextView(context);
            letterBadge.setTextSize(14);
            letterBadge.setTypeface(AndroidUtilities.bold());
            letterBadge.setTextColor(COLOR_TEXT_PRIMARY);
            letterBadge.setGravity(Gravity.CENTER);
            letterBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), COLOR_CHANNELS_BG));
            iconBox.addView(letterBadge, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            homeGlyph = new HomeGlyphView(context);
            homeGlyph.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), COLOR_BLURPLE));
            homeGlyph.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(13), AndroidUtilities.dp(13), AndroidUtilities.dp(13));
            iconBox.addView(homeGlyph, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            addGlyph = new AddGlyphView(context);
            addGlyph.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), COLOR_ONLINE_GREEN));
            addGlyph.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            iconBox.addView(addGlyph, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            countBadge = new TextView(context);
            countBadge.setTextSize(11);
            countBadge.setTypeface(AndroidUtilities.bold());
            countBadge.setTextColor(0xFFFFFFFF);
            countBadge.setGravity(Gravity.CENTER);
            countBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(9), COLOR_BADGE_RED));
            countBadge.setMinWidth(AndroidUtilities.dp(18));
            countBadge.setPadding(AndroidUtilities.dp(5), 0, AndroidUtilities.dp(5), 0);
            addView(countBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, AndroidUtilities.dp(18), Gravity.CENTER_HORIZONTAL | Gravity.TOP, 26, 4, 0, 0));

            avatarView.setVisibility(GONE);
            letterBadge.setVisibility(GONE);
            homeGlyph.setVisibility(GONE);
            addGlyph.setVisibility(GONE);
            countBadge.setVisibility(GONE);

            setClickable(true);
        }

        public void setHome(int unread) {
            homeGlyph.setVisibility(VISIBLE);
            if (unread > 0) setBadge(unread);
        }

        public void setAddAction() {
            addGlyph.setVisibility(VISIBLE);
            setContentDescription(MiogramLocale.get("Створити папку", "Создать папку", "Create a folder"));
        }

        public void setAvatar(int account, TLRPC.Chat chat) {
            avatarView.setVisibility(VISIBLE);
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(account, chat);
            avatarView.setForUserOrChat(chat, avatarDrawable);
        }

        public void setAvatar(int account, TLRPC.User user) {
            avatarView.setVisibility(VISIBLE);
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(account, user);
            avatarView.setForUserOrChat(user, avatarDrawable);
        }

        public void setLetters(String name) {
            letterBadge.setVisibility(VISIBLE);
            String n = name != null && !name.isEmpty() ? name : "G";
            letterBadge.setText(n.substring(0, Math.min(2, n.length())).toUpperCase());
        }

        public void setBadge(int count) {
            if (count <= 0) {
                countBadge.setVisibility(GONE);
                return;
            }
            countBadge.setVisibility(VISIBLE);
            countBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        }

        public void setSelectedVisual(boolean selected) {
            selectionT = selected ? 1f : 0f;
            applySelection();
        }

        /** Animates the pill grow + squircle morph towards the selected state. */
        public void animateSelection(boolean selected) {
            if (animator != null) animator.cancel();
            float from = selectionT;
            float to = selected ? 1f : 0f;
            animator = ValueAnimator.ofFloat(from, to);
            animator.setDuration(260);
            animator.setInterpolator(new OvershootInterpolator(1.02f));
            animator.addUpdateListener(a -> {
                selectionT = (float) a.getAnimatedValue();
                applySelection();
            });
            animator.start();
        }

        private void applySelection() {
            float t = Math.max(0f, Math.min(1f, selectionT));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pill.getLayoutParams();
            lp.height = AndroidUtilities.dp(8 + 32 * t);
            pill.setLayoutParams(lp);
            pill.setAlpha(0f + t);

            int radius = AndroidUtilities.dp(24 - 8 * t);
            if (avatarView.getVisibility() == VISIBLE) {
                avatarView.setRoundRadius(radius);
            } else if (letterBadge.getVisibility() == VISIBLE) {
                letterBadge.setBackground(Theme.createRoundRectDrawable(radius, t > 0.5f ? COLOR_BLURPLE : COLOR_CHANNELS_BG));
                letterBadge.setTextColor(t > 0.5f ? 0xFFFFFFFF : COLOR_TEXT_PRIMARY);
            } else if (homeGlyph.getVisibility() == VISIBLE) {
                homeGlyph.setBackground(Theme.createRoundRectDrawable(radius, COLOR_BLURPLE));
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            applySelection();
        }
    }

    /** Discord home glyph (house silhouette). */
    private static class HomeGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public HomeGlyphView(Context context) {
            super(context);
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            path.reset();
            path.moveTo(w / 2f, h / 6f);
            path.lineTo(w * 5f / 6f, h / 2f);
            path.lineTo(w * 0.68f, h / 2f);
            path.lineTo(w * 0.68f, h * 5f / 6f);
            path.lineTo(w * 0.32f, h * 5f / 6f);
            path.lineTo(w * 0.32f, h / 2f);
            path.lineTo(w / 6f, h / 2f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    /** Green "+" glyph for the add-server action. */
    private static class AddGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public AddGlyphView(Context context) {
            super(context);
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float arm = getWidth() / 5f;
            paint.setStrokeWidth(AndroidUtilities.dp(3));
            canvas.drawLine(cx - arm, cy, cx + arm, cy, paint);
            canvas.drawLine(cx, cy - arm, cx, cy + arm, paint);
        }
    }

    /** Mic button: capsule mic, red slash when muted (Discord voice UI). */
    private static class MicButton extends View {
        private boolean muted = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public MicButton(Context context) {
            super(context);
            paint.setAntiAlias(true);
        }

        public void setMuted(boolean muted) {
            if (this.muted != muted) {
                this.muted = muted;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            int color = muted ? COLOR_BADGE_RED : COLOR_TEXT_MUTED;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);

            float micW = AndroidUtilities.dp(7);
            float micH = AndroidUtilities.dp(12);
            RectF capsule = new RectF(cx - micW / 2f, cy - micH / 2f, cx + micW / 2f, cy - micH / 6f);
            canvas.drawRoundRect(capsule, micW / 2f, micW / 2f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.5f));
            RectF arc = new RectF(cx - micW, cy - micH / 2f + AndroidUtilities.dp(1), cx + micW, cy + micH / 6f);
            canvas.drawArc(arc, 0, 180, false, paint);
            canvas.drawLine(cx, cy + micH / 6f, cx, cy + micH / 2f, paint);

            if (muted) {
                paint.setStrokeWidth(AndroidUtilities.dp(2));
                canvas.drawLine(cx - AndroidUtilities.dp(8), cy + AndroidUtilities.dp(8), cx + AndroidUtilities.dp(8), cy - AndroidUtilities.dp(8), paint);
            }
        }
    }

    /** Headset button: arc + pads, red slash when deafened. */
    private static class DeafenButton extends View {
        private boolean deafened = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public DeafenButton(Context context) {
            super(context);
            paint.setAntiAlias(true);
        }

        public void setDeafened(boolean deafened) {
            if (this.deafened != deafened) {
                this.deafened = deafened;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            int color = deafened ? COLOR_BADGE_RED : COLOR_TEXT_MUTED;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));

            RectF arc = new RectF(cx - AndroidUtilities.dp(8), cy - AndroidUtilities.dp(9), cx + AndroidUtilities.dp(8), cy + AndroidUtilities.dp(7));
            canvas.drawArc(arc, 180, 180, false, paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(cx - AndroidUtilities.dp(9), cy - AndroidUtilities.dp(2), cx - AndroidUtilities.dp(5), cy + AndroidUtilities.dp(7)), AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
            canvas.drawRoundRect(new RectF(cx + AndroidUtilities.dp(5), cy - AndroidUtilities.dp(2), cx + AndroidUtilities.dp(9), cy + AndroidUtilities.dp(7)), AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);

            if (deafened) {
                paint.setStrokeWidth(AndroidUtilities.dp(2));
                canvas.drawLine(cx - AndroidUtilities.dp(8), cy + AndroidUtilities.dp(8), cx + AndroidUtilities.dp(8), cy - AndroidUtilities.dp(8), paint);
            }
        }
    }

    /** Gear glyph for the settings button. */
    private static class SettingsGearView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SettingsGearView(Context context) {
            super(context);
            paint.setColor(COLOR_TEXT_MUTED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = AndroidUtilities.dp(5);
            canvas.drawCircle(cx, cy, r, paint);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                canvas.drawLine(
                        (float) (cx + Math.cos(a) * (r + AndroidUtilities.dp(1.5f))),
                        (float) (cy + Math.sin(a) * (r + AndroidUtilities.dp(1.5f))),
                        (float) (cx + Math.cos(a) * (r + AndroidUtilities.dp(4))),
                        (float) (cy + Math.sin(a) * (r + AndroidUtilities.dp(4))),
                        paint);
            }
        }
    }

    /** Magnifier glyph for the channel header. */
    private static class SearchGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SearchGlyphView(Context context) {
            super(context);
            paint.setColor(COLOR_TEXT_MUTED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(2));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f - AndroidUtilities.dp(2);
            float cy = getHeight() / 2f - AndroidUtilities.dp(2);
            canvas.drawCircle(cx, cy, AndroidUtilities.dp(5), paint);
            canvas.drawLine(cx + AndroidUtilities.dp(4), cy + AndroidUtilities.dp(4), cx + AndroidUtilities.dp(8), cy + AndroidUtilities.dp(8), paint);
        }
    }
}
