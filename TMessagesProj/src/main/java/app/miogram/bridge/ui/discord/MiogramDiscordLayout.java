package app.miogram.bridge.ui.discord;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
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
 * <p>Polished revision: fixes the "looks like trash" issues of the first iteration —
 * misaligned pills, flat hash glyph, fake-feeling footer, layout thrash in
 * {@code onMeasure}, int-truncation of 64-bit dialog ids and missing
 * accessibility labels.
 *
 * <p>Heights are intentionally kept at 48dp (header) / 52dp (footer) and rail
 * width at 72dp to stay compatible with the margins applied in
 * {@code DialogsActivity.createView}.
 *
 * <p>Semantics of rail selection ids handed to {@link OnServerSelectedListener}:
 *   {@link #RAIL_HOME} (-1)  -> All chats (default folder, id 0)
 *   positive                 -> folder id (a "server")
 *   negative                 -> hashed rail id of a group chat opened as a server
 *   (use {@link #resolveDialogId(int)} to get the real 64-bit dialog id back).
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

    // Discord palette (official desktop values)
    public static final int COLOR_RAIL_BG = 0xFF1E1F22;
    public static final int COLOR_CHANNELS_BG = 0xFF2B2D31;
    public static final int COLOR_CHAT_BG = 0xFF313338;
    public static final int COLOR_BLURPLE = 0xFF5865F2;
    public static final int COLOR_BLURPLE_DARK = 0xFF4752C4;
    public static final int COLOR_TEXT_PRIMARY = 0xFFDBDEE1;
    public static final int COLOR_TEXT_MUTED = 0xFF949BA4;
    public static final int COLOR_ONLINE_GREEN = 0xFF23A55A;
    public static final int COLOR_BADGE_RED = 0xFFF23F43;
    public static final int COLOR_HEADER_BG = 0xFF2B2D31;
    public static final int COLOR_FOOTER_BG = 0xFF232428;
    public static final int COLOR_CHANNEL_ACTIVE = 0xFF404249;
    public static final int COLOR_SEPARATOR = 0xFF35363C;
    public static final int COLOR_INPUT_BG = 0xFF383A40;
    public static final int COLOR_ADD_BG = 0xFF313338;
    public static final int COLOR_ADD_GLYPH = 0xFF23A55A;

    /** Layout contract shared with DialogsActivity — do not change without updating it. */
    public static final int RAIL_WIDTH_DP = 72;
    public static final int HEADER_HEIGHT_DP = 48;
    public static final int FOOTER_HEIGHT_DP = 52;
    public static final int RAIL_ICON_DP = 48;
    /** Idle icons are circles (24dp radius on a 48dp box), selected morph to squircle. */
    public static final int RADIUS_IDLE_DP = 24;
    public static final int RADIUS_ACTIVE_DP = 16;

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Hot-path cache: this flag is checked inside {@code Theme.getColor} (up to
     * 3x per color resolve) and in every {@code DialogCell} bind/draw. A disk
     * read + {@code Preset.valueOf} on each call caused measurable fling jank.
     * All writers ({@link #setDiscordUiEnabled}, {@link #setUiMode},
     * {@code MiogramDivineEngine.applyPreset} which routes through the former)
     * invalidate the cache, so it never goes stale.
     */
    private static volatile Boolean cachedDiscordEnabled = null;

    public static boolean isDiscordUiEnabled() {
        Boolean c = cachedDiscordEnabled;
        if (c != null) return c;
        boolean v = getPrefs().getInt(KEY_UI_MODE, UI_MODE_TELEGRAM) == UI_MODE_DISCORD
                || app.miogram.bridge.divine.MiogramDivineEngine.getCurrentPreset(null) == app.miogram.bridge.divine.MiogramDivineEngine.Preset.DISCORD_ULTRA;
        cachedDiscordEnabled = v;
        return v;
    }

    /** Called by every writer of the UI mode / divine preset. */
    public static void invalidateUiModeCache() {
        cachedDiscordEnabled = null;
    }

    public static void setDiscordUiEnabled(boolean enabled) {
        getPrefs().edit().putInt(KEY_UI_MODE, enabled ? UI_MODE_DISCORD : UI_MODE_TELEGRAM).apply();
        cachedDiscordEnabled = enabled
                || app.miogram.bridge.divine.MiogramDivineEngine.getCurrentPreset(null) == app.miogram.bridge.divine.MiogramDivineEngine.Preset.DISCORD_ULTRA;
    }

    public static int getUiMode() {
        return getPrefs().getInt(KEY_UI_MODE, UI_MODE_TELEGRAM);
    }

    public static void setUiMode(int mode) {
        getPrefs().edit().putInt(KEY_UI_MODE, mode).apply();
        invalidateUiModeCache();
    }

    public static int getSelectedRailId() {
        return getPrefs().getInt(KEY_SELECTED, RAIL_HOME);
    }

    public static void setSelectedRailId(int id) {
        getPrefs().edit().putInt(KEY_SELECTED, id).apply();
    }

    // ------------------------------------------------------------------
    // Helpers for DialogCell / DialogsActivity integration
    // ------------------------------------------------------------------

    /**
     * Maps a 64-bit dialog id to a stable 32-bit rail id that never collides
     * with {@link #RAIL_HOME}, 0 or small positive folder ids.
     */
    public static int railIdForDialog(long dialogId) {
        int h = (int) (dialogId ^ (dialogId >>> 32));
        if (h == 0 || h == RAIL_HOME) h = -424242;
        if (h > 0) h = -h; // rail ids for chats are always negative, folders positive
        return h;
    }

    /** Reverse lookup for rail ids created by {@link #railIdForDialog(long)}. */
    private static final HashMap<Integer, Long> railToDialog = new HashMap<>();

    public static long resolveDialogId(int railId) {
        Long v = railToDialog.get(railId);
        return v == null ? railId : v;
    }

    /** Discord channels are lowercase "# name"; DMs keep their plain name. */
    public static CharSequence formatChannelName(CharSequence name, long dialogId) {
        if (name == null) return "";
        String s = name.toString().replace('\n', ' ').trim();
        if (dialogId < 0 && !s.startsWith("#") && !s.isEmpty()) {
            return "# " + s;
        }
        return s;
    }

    public static boolean isChannelLike(long dialogId) {
        return dialogId < 0;
    }

    // ------------------------------------------------------------------
    // Channel "#" icon drawn inside DialogCell rows
    // ------------------------------------------------------------------

    private static final Paint channelIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint unreadPillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        channelIconPaint.setStyle(Paint.Style.STROKE);
        channelIconPaint.setStrokeCap(Paint.Cap.ROUND);
        unreadPillPaint.setStyle(Paint.Style.FILL);
    }

    public static void drawChannelIcon(Canvas canvas, float x, float cy, boolean unread) {
        drawChannelIcon(canvas, x, cy, unread, false);
    }

    /**
     * @param x  center X of the hash glyph
     * @param cy center Y of the row
     */
    public static void drawChannelIcon(Canvas canvas, float x, float cy, boolean unread, boolean selected) {
        channelIconPaint.setColor(unread || selected ? 0xFFFFFFFF : COLOR_TEXT_MUTED);
        channelIconPaint.setStrokeWidth(AndroidUtilities.dp(2f));
        float size = AndroidUtilities.dp(9);
        // Two slanted verticals + two horizontals = Discord "#" glyph.
        canvas.drawLine(x - size * 0.30f, cy - size, x - size * 0.50f, cy + size, channelIconPaint);
        canvas.drawLine(x + size * 0.40f, cy - size, x + size * 0.20f, cy + size, channelIconPaint);
        canvas.drawLine(x - size * 0.85f, cy - size * 0.32f, x + size * 0.85f, cy - size * 0.32f, channelIconPaint);
        canvas.drawLine(x - size * 0.85f, cy + size * 0.38f, x + size * 0.85f, cy + size * 0.38f, channelIconPaint);

        // Discord-style unread/selected pill pinned to the very left edge of the row.
        unreadPillPaint.setColor(0xFFFFFFFF);
        float pillW = AndroidUtilities.dp(4);
        float pillH = selected ? AndroidUtilities.dp(32) : AndroidUtilities.dp(8);
        if (unread || selected) {
            float left = AndroidUtilities.dp(2);
            float top = cy - pillH / 2f;
            canvas.drawRoundRect(left, top, left + pillW, top + pillH,
                    AndroidUtilities.dp(2), AndroidUtilities.dp(2), unreadPillPaint);
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
        root.setLayoutParams(new ViewGroup.LayoutParams(AndroidUtilities.dp(RAIL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT));
        // Keep the rail clear of the status bar instead of drawing icons under it.
        root.setPadding(0, Math.max(0, AndroidUtilities.statusBarHeight - AndroidUtilities.dp(4)), 0, AndroidUtilities.dp(8));

        int currentAccount = UserConfig.selectedAccount;
        int selectedId = getSelectedRailId();
        final RailItemView[] selectedRailItem = new RailItemView[1];
        railToDialog.clear();

        HashMap<Long, TLRPC.Dialog> dialogById = new HashMap<>();
        ArrayList<TLRPC.Dialog> allDialogs = null;
        try {
            allDialogs = MessagesController.getInstance(currentAccount).getDialogs(0);
        } catch (Throwable ignored) {}
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
        homeItem.setHasUnread(dmUnread > 0);
        homeItem.setSelectedVisual(selectedId == RAIL_HOME);
        homeItem.setContentDescription("Direct Messages" + (dmUnread > 0 ? ", " + dmUnread + " unread" : ""));
        if (selectedId == RAIL_HOME) selectedRailItem[0] = homeItem;
        homeItem.setOnClickListener(v -> {
            haptic(v);
            setSelectedRailId(RAIL_HOME);
            selectRailItem(selectedRailItem, homeItem);
            if (listener != null) listener.onServerSelected(RAIL_HOME);
        });
        root.addView(homeItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(60), 0, 4, 0, 2));

        View sep = new View(context);
        sep.setBackgroundColor(COLOR_SEPARATOR);
        root.addView(sep, LayoutHelper.createLinear(32, 2, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 6));

        // 2. Folders as "servers"
        ScrollView sv = new ScrollView(context);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout serverList = new LinearLayout(context);
        serverList.setOrientation(LinearLayout.VERTICAL);

        ArrayList<MessagesController.DialogFilter> filters = null;
        try {
            filters = MessagesController.getInstance(currentAccount).dialogFilters;
        } catch (Throwable ignored) {}
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                MessagesController.DialogFilter filter = filters.get(i);
                if (filter == null || filter.id == 0) continue; // 0 = All chats = home button
                final int filterId = filter.id;

                int folderUnread = 0;
                if (filter.alwaysShow != null) {
                    for (int k = 0; k < filter.alwaysShow.size() && folderUnread < 999; k++) {
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
                item.setHasUnread(folderUnread > 0);
                item.setSelectedVisual(selectedId == filterId);
                String label = filter.name != null ? filter.name : "Folder";
                item.setContentDescription(label + (folderUnread > 0 ? ", " + folderUnread + " unread" : ""));
                if (selectedId == filterId) selectedRailItem[0] = item;
                item.setOnClickListener(v -> {
                    haptic(v);
                    setSelectedRailId(filterId);
                    selectRailItem(selectedRailItem, item);
                    if (listener != null) listener.onServerSelected(filterId);
                });
                serverList.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56), 0, 0, 0, 4));
            }
        }

        // Group chats without a folder also appear as servers.
        if (allDialogs != null) {
            int added = 0;
            for (int i = 0; i < allDialogs.size() && added < 40; i++) {
                TLRPC.Dialog dialog = allDialogs.get(i);
                if (dialog == null || dialog.id >= 0) continue;
                TLRPC.Chat chat = null;
                try {
                    chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                } catch (Throwable ignored) {}
                if (chat == null) continue;
                final long dialogId = dialog.id;
                final int railId = railIdForDialog(dialogId);
                if (railToDialog.containsKey(railId)) continue;
                railToDialog.put(railId, dialogId);

                RailItemView item = new RailItemView(context, false);
                item.setAvatar(currentAccount, chat);
                item.setBadge(dialog.unread_count);
                item.setHasUnread(dialog.unread_count > 0);
                item.setSelectedVisual(selectedId == railId);
                String title = chat.title != null ? chat.title : "Group";
                item.setContentDescription(title + (dialog.unread_count > 0 ? ", " + dialog.unread_count + " unread" : ""));
                if (selectedId == railId) selectedRailItem[0] = item;
                item.setOnClickListener(v -> {
                    haptic(v);
                    setSelectedRailId(railId);
                    selectRailItem(selectedRailItem, item);
                    if (listener != null) listener.onServerSelected(railId);
                });
                serverList.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56), 0, 0, 0, 4));
                added++;
            }
        }

        sv.addView(serverList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));

        // 3. "+" — create a folder (Discord: "Add a Server")
        RailItemView addItem = new RailItemView(context, false);
        addItem.setAddAction();
        addItem.setContentDescription(MiogramLocale.get("Створити папку", "Создать папку", "Add a server"));
        addItem.setOnClickListener(v -> {
            haptic(v);
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new FiltersSetupActivity());
                }
            }
        });
        root.addView(addItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(56), 0, 4, 0, 2));

        // 4. Self avatar + status dot pinned to the bottom of the rail
        FrameLayout userBox = new FrameLayout(context);
        userBox.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(4));

        FrameLayout avatarBox = new FrameLayout(context);
        BackupImageView userAvatar = new BackupImageView(context);
        userAvatar.setRoundRadius(AndroidUtilities.dp(20));
        TLRPC.User self = null;
        try {
            self = UserConfig.getInstance(currentAccount).getCurrentUser();
        } catch (Throwable ignored) {}
        if (self != null) {
            userAvatar.setForUserOrChat(self, new AvatarDrawable(self));
        } else {
            userAvatar.setImageDrawable(new AvatarDrawable());
        }
        avatarBox.addView(userAvatar, LayoutHelper.createFrame(42, 42, Gravity.CENTER));

        View onlineDot = new View(context);
        onlineDot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(7), COLOR_ONLINE_GREEN));
        avatarBox.addView(onlineDot, LayoutHelper.createFrame(14, 14, Gravity.CENTER, 15, 15, 0, 0));

        avatarBox.setOnClickListener(v -> {
            haptic(v);
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new MiogramSettingsActivity());
                }
            }
        });
        avatarBox.setContentDescription(MiogramLocale.get("Налаштування", "Настройки", "User settings"));
        avatarBox.setBackground(Theme.createSelectorDrawable(0x1FFFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        userBox.addView(avatarBox, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(52), Gravity.CENTER));

        root.addView(userBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return root;
    }

    private static void haptic(View v) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignored) {}
    }

    /** Keeps exactly one guild visually selected as the active folder changes. */
    private static void selectRailItem(RailItemView[] selectedRailItem, RailItemView next) {
        if (selectedRailItem[0] == next) return;
        if (selectedRailItem[0] != null) selectedRailItem[0].animateSelection(false);
        selectedRailItem[0] = next;
        next.animateSelection(true);
    }

    // ------------------------------------------------------------------
    // User panel (bottom of the channel pane)
    // ------------------------------------------------------------------

    public static View createDiscordUserFooter(Context context) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(COLOR_FOOTER_BG);

        View topLine = new View(context);
        topLine.setBackgroundColor(0xFF1E1F22);
        wrapper.addView(topLine, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(COLOR_FOOTER_BG);
        bar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(8), AndroidUtilities.dp(7));
        wrapper.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, FOOTER_HEIGHT_DP - 1));

        int currentAccount = UserConfig.selectedAccount;
        TLRPC.User self = null;
        try {
            self = UserConfig.getInstance(currentAccount).getCurrentUser();
        } catch (Throwable ignored) {}

        FrameLayout avatarBox = new FrameLayout(context);
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(16));
        if (self != null) {
            avatar.setForUserOrChat(self, new AvatarDrawable(self));
        } else {
            avatar.setImageDrawable(new AvatarDrawable());
        }
        avatarBox.addView(avatar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));
        View dot = new View(context);
        boolean muted = getPrefs().getBoolean(KEY_MIC_MUTED, false);
        boolean deafened = getPrefs().getBoolean(KEY_DEAFENED, false);
        dot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6),
                deafened ? COLOR_BADGE_RED : COLOR_ONLINE_GREEN));
        avatarBox.addView(dot, LayoutHelper.createFrame(11, 11, Gravity.CENTER, 11, 11, 0, 0));
        bar.addView(avatarBox, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(context);
        String firstName = (self != null && self.first_name != null && !self.first_name.isEmpty())
                ? self.first_name : "User";
        nameView.setText(firstName);
        nameView.setTextSize(13);
        nameView.setTypeface(AndroidUtilities.bold());
        nameView.setTextColor(COLOR_TEXT_PRIMARY);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        textGroup.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView tagView = new TextView(context);
        tagView.setTextSize(11);
        tagView.setTextColor(COLOR_TEXT_MUTED);
        tagView.setSingleLine(true);
        tagView.setEllipsize(TextUtils.TruncateAt.END);
        textGroup.addView(tagView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final TLRPC.User selfFinal = self;
        Runnable refreshStatus = () -> {
            boolean m = getPrefs().getBoolean(KEY_MIC_MUTED, false);
            boolean d = getPrefs().getBoolean(KEY_DEAFENED, false);
            String status;
            TLRPC.User u = selfFinal;
            if (d) {
                status = MiogramLocale.get("Заглушено", "Заглушен", "Deafened");
            } else if (m) {
                status = MiogramLocale.get("Мут", "Мут", "Muted");
            } else if (u != null && u.username != null && !u.username.isEmpty()) {
                status = "@" + u.username;
            } else {
                status = MiogramLocale.get("Онлайн", "Онлайн", "Online");
            }
            tagView.setText(status);
            dot.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), d ? COLOR_BADGE_RED : COLOR_ONLINE_GREEN));
        };
        refreshStatus.run();

        bar.addView(textGroup, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));

        MicButton micBtn = new MicButton(context);
        micBtn.setMuted(muted);
        micBtn.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        micBtn.setContentDescription("Toggle mute");
        micBtn.setOnClickListener(v -> {
            haptic(v);
            boolean next = !getPrefs().getBoolean(KEY_MIC_MUTED, false);
            getPrefs().edit().putBoolean(KEY_MIC_MUTED, next).apply();
            micBtn.setMuted(next);
            refreshStatus.run();
        });
        bar.addView(micBtn, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 2, 0, 2, 0));

        DeafenButton deafenBtn = new DeafenButton(context);
        deafenBtn.setDeafened(deafened);
        deafenBtn.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        deafenBtn.setContentDescription("Toggle deafen");
        deafenBtn.setOnClickListener(v -> {
            haptic(v);
            boolean next = !getPrefs().getBoolean(KEY_DEAFENED, false);
            getPrefs().edit().putBoolean(KEY_DEAFENED, next).apply();
            deafenBtn.setDeafened(next);
            if (next) {
                getPrefs().edit().putBoolean(KEY_MIC_MUTED, true).apply();
                micBtn.setMuted(true);
            }
            refreshStatus.run();
        });
        bar.addView(deafenBtn, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 2, 0, 2, 0));

        View settingsBtn = new SettingsGearView(context);
        settingsBtn.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        settingsBtn.setContentDescription(MiogramLocale.get("Налаштування", "Настройки", "Settings"));
        settingsBtn.setOnClickListener(v -> {
            haptic(v);
            LaunchActivity act = LaunchActivity.instance;
            if (act != null && !act.isFinishing()) {
                BaseFragment frag = act.getSafeLastFragment();
                if (frag != null) {
                    frag.presentFragment(new MiogramSettingsActivity());
                }
            }
        });
        bar.addView(settingsBtn, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 2, 0, 0, 0));

        return wrapper;
    }

    // ------------------------------------------------------------------
    // Channel pane header
    // ------------------------------------------------------------------

    /** Resolves the active "server" title: selected folder name or DMs default. */
    public static String channelPaneTitle(Context context) {
        int selected = getSelectedRailId();
        if (selected != RAIL_HOME) {
            ArrayList<MessagesController.DialogFilter> filters = null;
            try {
                filters = MessagesController.getInstance(UserConfig.selectedAccount).dialogFilters;
            } catch (Throwable ignored) {}
            if (filters != null) {
                for (int i = 0; i < filters.size(); i++) {
                    MessagesController.DialogFilter f = filters.get(i);
                    if (f != null && f.id == selected && f.name != null && !f.name.isEmpty()) {
                        return f.name;
                    }
                }
            }
            Long did = railToDialog.get(selected);
            if (did != null) {
                try {
                    TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-did);
                    if (chat != null && chat.title != null && !chat.title.isEmpty()) return chat.title;
                } catch (Throwable ignored) {}
            }
        }
        return MiogramLocale.get("Повідомлення", "Сообщения", "Messages");
    }

    public static View createDiscordChannelHeader(Context context, String title, Runnable onSearchClick) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(COLOR_HEADER_BG);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_HEADER_BG);
        header.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        wrapper.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, HEADER_HEIGHT_DP - 1));

        HashGlyphView hash = new HashGlyphView(context);
        header.addView(hash, LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView titleView = new TextView(context);
        titleView.setText(title != null && !title.isEmpty() ? title : MiogramLocale.get("Повідомлення", "Сообщения", "Messages"));
        titleView.setTextColor(COLOR_TEXT_PRIMARY);
        titleView.setTextSize(15);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 4, 0));

        View search = new SearchGlyphView(context);
        search.setBackground(Theme.createSelectorDrawable(0x22FFFFFF, Theme.RIPPLE_MASK_CIRCLE_20DP));
        search.setContentDescription(MiogramLocale.get("Пошук", "Поиск", "Search"));
        search.setOnClickListener(v -> {
            haptic(v);
            if (onSearchClick != null) onSearchClick.run();
        });
        header.addView(search, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        View divider = new View(context);
        divider.setBackgroundColor(0xFF1E1F22);
        wrapper.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        return wrapper;
    }

    /** Discord channel hash glyph. */
    private static class HashGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HashGlyphView(Context context) {
            super(context);
            paint.setColor(COLOR_TEXT_MUTED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            canvas.drawLine(w * 0.32f, AndroidUtilities.dp(1), w * 0.20f, h - AndroidUtilities.dp(1), paint);
            canvas.drawLine(w * 0.72f, AndroidUtilities.dp(1), w * 0.60f, h - AndroidUtilities.dp(1), paint);
            canvas.drawLine(AndroidUtilities.dp(1), h * 0.38f, w - AndroidUtilities.dp(1), h * 0.38f, paint);
            canvas.drawLine(AndroidUtilities.dp(1), h * 0.66f, w - AndroidUtilities.dp(1), h * 0.66f, paint);
        }
    }

    // ==================================================================
    // Custom views
    // ==================================================================

    /**
     * One rail row: white pill indicator on the left edge, a squircle icon that
     * morphs its corner radius between circle (idle) and rounded square
     * (selected) and an optional red unread badge.
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

        // Cached backgrounds — creating a drawable per animation frame was the
        // main source of GC churn in the old version.
        private android.graphics.drawable.Drawable letterBgIdle;
        private android.graphics.drawable.Drawable letterBgActive;
        private android.graphics.drawable.Drawable homeBg;
        private android.graphics.drawable.Drawable addBg;

        private float selectionT = 0f; // 0 idle .. 1 selected
        private boolean hasUnread = false;
        private ValueAnimator animator;
        private int lastPillH = -1;

        public RailItemView(Context context, boolean home) {
            super(context);
            this.isHome = home;

            setClipChildren(false);
            setClipToPadding(false);

            pill = new View(context);
            pill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), 0xFFFFFFFF));
            addView(pill, LayoutHelper.createFrame(4, 8, Gravity.LEFT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            iconBox = new FrameLayout(context);
            addView(iconBox, LayoutHelper.createFrame(RAIL_ICON_DP, RAIL_ICON_DP, Gravity.CENTER));

            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(RADIUS_IDLE_DP));
            iconBox.addView(avatarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            letterBgIdle = Theme.createRoundRectDrawable(AndroidUtilities.dp(RADIUS_IDLE_DP), COLOR_CHANNELS_BG);
            letterBgActive = Theme.createRoundRectDrawable(AndroidUtilities.dp(RADIUS_ACTIVE_DP), COLOR_BLURPLE);
            homeBg = Theme.createRoundRectDrawable(AndroidUtilities.dp(RADIUS_IDLE_DP), COLOR_BLURPLE);
            addBg = Theme.createRoundRectDrawable(AndroidUtilities.dp(RADIUS_IDLE_DP), COLOR_ADD_BG);

            letterBadge = new TextView(context);
            letterBadge.setTextSize(15);
            letterBadge.setTypeface(AndroidUtilities.bold());
            letterBadge.setTextColor(COLOR_TEXT_PRIMARY);
            letterBadge.setGravity(Gravity.CENTER);
            letterBadge.setBackground(letterBgIdle);
            iconBox.addView(letterBadge, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            homeGlyph = new HomeGlyphView(context);
            homeGlyph.setBackground(homeBg);
            homeGlyph.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(13), AndroidUtilities.dp(13), AndroidUtilities.dp(13));
            iconBox.addView(homeGlyph, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            addGlyph = new AddGlyphView(context);
            addGlyph.setBackground(addBg);
            addGlyph.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            iconBox.addView(addGlyph, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            countBadge = new TextView(context);
            countBadge.setTextSize(11);
            countBadge.setTypeface(AndroidUtilities.bold());
            countBadge.setTextColor(0xFFFFFFFF);
            countBadge.setGravity(Gravity.CENTER);
            countBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(9), COLOR_BADGE_RED));
            countBadge.setMinWidth(AndroidUtilities.dp(18));
            countBadge.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(1), AndroidUtilities.dp(5), AndroidUtilities.dp(1));
            countBadge.setSingleLine(true);
            addView(countBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, AndroidUtilities.dp(18), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 24, 2, 0, 0));

            avatarView.setVisibility(GONE);
            letterBadge.setVisibility(GONE);
            homeGlyph.setVisibility(GONE);
            addGlyph.setVisibility(GONE);
            countBadge.setVisibility(GONE);

            setClickable(true);
            setFocusable(true);
            setBackground(Theme.createSelectorDrawable(0x1FFFFFFF, Theme.RIPPLE_MASK_ALL));
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
            try {
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(account, chat);
                avatarView.setForUserOrChat(chat, avatarDrawable);
            } catch (Throwable ignored) {}
        }

        public void setAvatar(int account, TLRPC.User user) {
            avatarView.setVisibility(VISIBLE);
            try {
                AvatarDrawable avatarDrawable = new AvatarDrawable();
                avatarDrawable.setInfo(account, user);
                avatarView.setForUserOrChat(user, avatarDrawable);
            } catch (Throwable ignored) {}
        }

        public void setLetters(String name) {
            letterBadge.setVisibility(VISIBLE);
            String n = name != null && !name.trim().isEmpty() ? name.trim() : "G";
            // First letters of first two words ("Dark Side" -> "DS"), like Discord.
            String[] parts = n.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].codePointAt(0)));
            }
            if (sb.length() == 0) sb.append("G");
            letterBadge.setText(sb.toString());
        }

        public void setBadge(int count) {
            if (count <= 0) {
                countBadge.setVisibility(GONE);
                return;
            }
            countBadge.setVisibility(VISIBLE);
            countBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        }

        public void setHasUnread(boolean unread) {
            this.hasUnread = unread;
            applySelection();
        }

        public void setSelectedVisual(boolean selected) {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            selectionT = selected ? 1f : 0f;
            applySelection();
        }

        /** Animates the pill grow + squircle morph towards the selected state. */
        public void animateSelection(boolean selected) {
            if (animator != null) animator.cancel();
            float from = selectionT;
            float to = selected ? 1f : 0f;
            if (from == to) {
                applySelection();
                return;
            }
            animator = ValueAnimator.ofFloat(from, to);
            animator.setDuration(220);
            animator.setInterpolator(new OvershootInterpolator(1.02f));
            animator.addUpdateListener(a -> {
                selectionT = (float) a.getAnimatedValue();
                applySelection();
            });
            animator.start();
        }

        private void applySelection() {
            float t = Math.max(0f, Math.min(1f, selectionT));
            boolean selected = t > 0.5f;

            // Pill: 40dp bar when selected, 8dp dot when unread, hidden otherwise.
            int pillH;
            if (t > 0.02f) {
                pillH = AndroidUtilities.dp(8 + 32 * t);
            } else if (hasUnread) {
                pillH = AndroidUtilities.dp(8);
            } else {
                pillH = 0;
            }
            if (pillH != lastPillH) {
                lastPillH = pillH;
                if (pillH <= 0) {
                    pill.setVisibility(INVISIBLE);
                } else {
                    pill.setVisibility(VISIBLE);
                    ViewGroup.LayoutParams lp = pill.getLayoutParams();
                    if (lp != null && lp.height != pillH) {
                        lp.height = pillH;
                        pill.setLayoutParams(lp);
                    }
                }
            }
            pill.setAlpha(t > 0.02f || hasUnread ? 1f : 0f);

            int radius = AndroidUtilities.dp(Math.round(RADIUS_IDLE_DP - (RADIUS_IDLE_DP - RADIUS_ACTIVE_DP) * t));
            if (avatarView.getVisibility() == VISIBLE) {
                avatarView.setRoundRadius(radius);
            }
            if (letterBadge.getVisibility() == VISIBLE) {
                letterBadge.setBackground(selected ? letterBgActive : letterBgIdle);
                letterBadge.setTextColor(selected ? 0xFFFFFFFF : COLOR_TEXT_PRIMARY);
            }
            if (homeGlyph.getVisibility() == VISIBLE) {
                // Recreate only when crossing the threshold, not per animation frame.
                if (selected && homeGlyph.getBackground() != homeBg) {
                    homeBg = Theme.createRoundRectDrawable(radius, COLOR_BLURPLE);
                    homeGlyph.setBackground(homeBg);
                } else if (!selected && lastPillH == AndroidUtilities.dp(8)) {
                    // keep steady home bg while idle
                }
            }
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
            float w = getWidth() - getPaddingLeft() - getPaddingRight();
            float h = getHeight() - getPaddingTop() - getPaddingBottom();
            float ox = getPaddingLeft();
            float oy = getPaddingTop();
            path.reset();
            path.moveTo(ox + w / 2f, oy + h * 0.12f);
            path.lineTo(ox + w * 0.88f, oy + h * 0.48f);
            path.lineTo(ox + w * 0.70f, oy + h * 0.48f);
            path.lineTo(ox + w * 0.70f, oy + h * 0.88f);
            path.lineTo(ox + w * 0.30f, oy + h * 0.88f);
            path.lineTo(ox + w * 0.30f, oy + h * 0.48f);
            path.lineTo(ox + w * 0.12f, oy + h * 0.48f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    /** Green "+" glyph for the add-server action. */
    private static class AddGlyphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public AddGlyphView(Context context) {
            super(context);
            paint.setColor(COLOR_ADD_GLYPH);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float arm = Math.min(getWidth(), getHeight()) / 5f;
            paint.setStrokeWidth(AndroidUtilities.dp(3));
            canvas.drawLine(cx - arm, cy, cx + arm, cy, paint);
            canvas.drawLine(cx, cy - arm, cx, cy + arm, paint);
        }
    }

    /** Mic button: capsule mic, red slash when muted (Discord voice UI). */
    private static class MicButton extends View {
        private boolean muted = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF capsule = new RectF();
        private final RectF arc = new RectF();

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
            float cy = getHeight() / 2f - AndroidUtilities.dp(1);
            int color = muted ? COLOR_BADGE_RED : COLOR_TEXT_MUTED;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);

            float micW = AndroidUtilities.dp(7);
            float micH = AndroidUtilities.dp(12);
            capsule.set(cx - micW / 2f, cy - micH / 2f, cx + micW / 2f, cy - micH / 6f);
            canvas.drawRoundRect(capsule, micW / 2f, micW / 2f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.6f));
            arc.set(cx - micW, cy - micH / 2f + AndroidUtilities.dp(1), cx + micW, cy + micH / 6f + AndroidUtilities.dp(1));
            canvas.drawArc(arc, 20, 140, false, paint);
            canvas.drawLine(cx, cy + micH / 6f + AndroidUtilities.dp(1), cx, cy + micH / 2f, paint);
            canvas.drawLine(cx - AndroidUtilities.dp(3), cy + micH / 2f, cx + AndroidUtilities.dp(3), cy + micH / 2f, paint);

            if (muted) {
                paint.setStrokeWidth(AndroidUtilities.dp(2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(cx - AndroidUtilities.dp(8), cy + AndroidUtilities.dp(8), cx + AndroidUtilities.dp(8), cy - AndroidUtilities.dp(8), paint);
            }
        }
    }

    /** Headset button: arc + pads, red slash when deafened. */
    private static class DeafenButton extends View {
        private boolean deafened = false;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();
        private final RectF leftPad = new RectF();
        private final RectF rightPad = new RectF();

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
            float cy = getHeight() / 2f - AndroidUtilities.dp(1);
            int color = deafened ? COLOR_BADGE_RED : COLOR_TEXT_MUTED;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);

            arc.set(cx - AndroidUtilities.dp(8), cy - AndroidUtilities.dp(8), cx + AndroidUtilities.dp(8), cy + AndroidUtilities.dp(7));
            canvas.drawArc(arc, 180, 180, false, paint);

            paint.setStyle(Paint.Style.FILL);
            leftPad.set(cx - AndroidUtilities.dp(9.5f), cy - AndroidUtilities.dp(2), cx - AndroidUtilities.dp(5), cy + AndroidUtilities.dp(7));
            rightPad.set(cx + AndroidUtilities.dp(5), cy - AndroidUtilities.dp(2), cx + AndroidUtilities.dp(9.5f), cy + AndroidUtilities.dp(7));
            canvas.drawRoundRect(leftPad, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
            canvas.drawRoundRect(rightPad, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);

            if (deafened) {
                paint.setStyle(Paint.Style.STROKE);
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
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = AndroidUtilities.dp(5);
            canvas.drawCircle(cx, cy, r, paint);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45 + 22.5);
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
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f - AndroidUtilities.dp(1.5f);
            float cy = getHeight() / 2f - AndroidUtilities.dp(1.5f);
            canvas.drawCircle(cx, cy, AndroidUtilities.dp(5.5f), paint);
            canvas.drawLine(cx + AndroidUtilities.dp(4), cy + AndroidUtilities.dp(4), cx + AndroidUtilities.dp(8.5f), cy + AndroidUtilities.dp(8.5f), paint);
        }
    }
}
