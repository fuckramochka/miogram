package app.miogram.bridge.presence;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.utils.ViewOutlineProviderImpl;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.discord.MiogramDiscordManager;
import app.miogram.bridge.github.MiogramGitHubManager;
import app.miogram.bridge.spotify.MiogramSpotifyManager;
import app.miogram.bridge.steam.MiogramSteamManager;

/**
 * Unified Multi-Platform Digital Presence Card for Telegram Profiles.
 * Dynamically displays only connected services (Steam, GitHub, Discord, Spotify).
 * Navigation is controlled via reactive indicator dots that only exist for connected platforms.
 * Zero tacky emojis in buttons — pure Durov-grade minimalism.
 */
public class MiogramPresenceCard extends FrameLayout {

    public static final int SERVICE_STEAM = 0;
    public static final int SERVICE_GITHUB = 1;
    public static final int SERVICE_DISCORD = 2;
    public static final int SERVICE_SPOTIFY = 3;

    private final Theme.ResourcesProvider resourcesProvider;
    private final ViewPager viewPager;
    private final PresencePagerAdapter pagerAdapter;
    private final LinearLayout headerRow;
    private final TextView serviceTitleView;
    private final LinearLayout dotsRow;
    private final List<View> dotViews = new ArrayList<>();
    private final List<Integer> activeServices = new ArrayList<>();

    private MiogramSteamManager.SteamProfile steamProfile;
    private MiogramGitHubManager.GitHubUser githubUser;
    private MiogramDiscordManager.DiscordPresence discordPresence;

    private final MiogramSpotifyManager.SpotifyListener spotifyListener = new MiogramSpotifyManager.SpotifyListener() {
        @Override
        public void onSpotifyTrackChanged(String track, String artist, boolean isPlaying) {
            AndroidUtilities.runOnUIThread(() -> {
                refreshActiveServices();
            });
        }

        @Override
        public void onSpotifyPlaybackChanged(boolean isPlaying) {
            AndroidUtilities.runOnUIThread(() -> {
                refreshActiveServices();
            });
        }
    };

    public MiogramPresenceCard(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));

        FrameLayout cardLayout = new FrameLayout(context);
        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF162334, 0xFF0E1724, 0xFF0A101A}
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

        // 1. Header Bar: Service Title (Left) + Dots Indicator (Right)
        headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        container.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        serviceTitleView = new TextView(context);
        serviceTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        serviceTitleView.setTypeface(AndroidUtilities.bold());
        serviceTitleView.setLetterSpacing(0.04f);
        serviceTitleView.setTextColor(0xAA66C0F4);
        ScaleStateListAnimator.apply(serviceTitleView, 0.035f, 1.4f);
        serviceTitleView.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            openConnectedAppsHub(context);
        });
        headerRow.addView(serviceTitleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        dotsRow = new LinearLayout(context);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(dotsRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        // 2. ViewPager for smooth swiping
        viewPager = new ViewPager(context) {
            private float startX;
            private float startY;

            @Override
            public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
                switch (ev.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = ev.getX();
                        startY = ev.getY();
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(ev.getX() - startX);
                        float dy = Math.abs(ev.getY() - startY);
                        if (dx > dy && dx > AndroidUtilities.dp(4)) {
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        } else if (dy > dx && dy > AndroidUtilities.dp(4)) {
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                            }
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = 0;
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                    int h = child.getMeasuredHeight();
                    if (h > height) height = h;
                }
                if (height != 0) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        pagerAdapter = new PresencePagerAdapter(context);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateDotSelection(position);
            }
        });
        container.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        refreshActiveServices();
        loadLiveData();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MiogramSpotifyManager.getInstance().addListener(spotifyListener);
        refreshActiveServices();
        loadLiveData();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        MiogramSpotifyManager.getInstance().removeListener(spotifyListener);
    }

    public void openConnectedAppsHub(Context context) {
        MiogramConnectedAppsSheet sheet = new MiogramConnectedAppsSheet(context, resourcesProvider);
        sheet.setOnAppsChangedListener(this::refreshActiveServices);
        sheet.show();
    }

    public void refreshActiveServices() {
        activeServices.clear();
        if (MiogramSteamManager.getInstance().isLinked()) {
            activeServices.add(SERVICE_STEAM);
        }
        if (MiogramGitHubManager.getInstance().isLinked()) {
            activeServices.add(SERVICE_GITHUB);
        }
        if (MiogramDiscordManager.getInstance().isLinked()) {
            activeServices.add(SERVICE_DISCORD);
        }
        if (MiogramSpotifyManager.getInstance().isLinked()) {
            activeServices.add(SERVICE_SPOTIFY);
        }

        buildDots();
        pagerAdapter.notifyDataSetChanged();

        int count = activeServices.isEmpty() ? 1 : activeServices.size();
        int cur = viewPager.getCurrentItem();
        if (cur >= count) {
            viewPager.setCurrentItem(Math.max(0, count - 1), false);
        }
        updateDotSelection(viewPager.getCurrentItem());
    }

    private void buildDots() {
        dotsRow.removeAllViews();
        dotViews.clear();

        // If no service is connected, no dots exist at all ("крапки нема взагалі")
        if (activeServices.isEmpty()) {
            dotsRow.setVisibility(View.GONE);
            return;
        }

        dotsRow.setVisibility(View.VISIBLE);
        Context context = getContext();
        for (int i = 0; i < activeServices.size(); i++) {
            final int pageIndex = i;
            View dot = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(AndroidUtilities.dp(6), AndroidUtilities.dp(6));
            lp.leftMargin = AndroidUtilities.dp(3);
            lp.rightMargin = AndroidUtilities.dp(3);
            dot.setLayoutParams(lp);

            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(3));
            gd.setColor(0x44FFFFFF);
            dot.setBackground(gd);

            ScaleStateListAnimator.apply(dot, 0.1f, 1.4f);
            dot.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                viewPager.setCurrentItem(pageIndex, true);
            });
            dotViews.add(dot);
            dotsRow.addView(dot);
        }
    }

    private void updateDotSelection(int selectedIndex) {
        if (activeServices.isEmpty()) {
            serviceTitleView.setText(MiogramLocale.get("ЦИФРОВА ПРИСУТНІСТЬ ໒꒱", "ЦИФРОВОЕ ПРИСУТСТВИЕ ໒꒱", "DIGITAL PRESENCE ໒꒱"));
            serviceTitleView.setTextColor(0xAA66C0F4);
            return;
        }

        if (selectedIndex >= activeServices.size()) {
            selectedIndex = Math.max(0, activeServices.size() - 1);
        }

        int activeService = activeServices.get(selectedIndex);
        int accentColor;
        String title;

        if (activeService == SERVICE_STEAM) {
            accentColor = 0xFF66C0F4;
            title = "STEAM GAMING";
        } else if (activeService == SERVICE_GITHUB) {
            accentColor = 0xFFFFFFFF;
            title = "GITHUB PROFILE";
        } else if (activeService == SERVICE_DISCORD) {
            accentColor = 0xFF5865F2;
            title = "DISCORD PRESENCE";
        } else {
            accentColor = 0xFF1DB954;
            title = "SPOTIFY LIVE";
        }

        serviceTitleView.setText(title);
        serviceTitleView.setTextColor(accentColor);

        for (int i = 0; i < dotViews.size(); i++) {
            View dot = dotViews.get(i);
            boolean isSel = (i == selectedIndex);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (lp != null) {
                lp.width = AndroidUtilities.dp(isSel ? 18 : 6);
                lp.height = AndroidUtilities.dp(6);
                dot.setLayoutParams(lp);
            }
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(AndroidUtilities.dp(3));
            gd.setColor(isSel ? accentColor : 0x44FFFFFF);
            dot.setBackground(gd);
        }
    }

    public void setSteamProfile(MiogramSteamManager.SteamProfile profile) {
        this.steamProfile = profile;
        refreshActiveServices();
    }

    public void loadLiveData() {
        MiogramGitHubManager.getInstance().fetchUser(false, user -> {
            this.githubUser = user;
            pagerAdapter.notifyDataSetChanged();
        });

        MiogramDiscordManager.getInstance().fetchPresence(false, presence -> {
            this.discordPresence = presence;
            pagerAdapter.notifyDataSetChanged();
        });
    }

    private static TextView createButton(Context context, String text, int bgColor, int textColor) {
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

    private class PresencePagerAdapter extends PagerAdapter {
        private final Context context;

        public PresencePagerAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return activeServices.isEmpty() ? 1 : activeServices.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View page;
            if (activeServices.isEmpty()) {
                page = buildEmptyView(context);
            } else {
                int service = activeServices.get(position);
                if (service == SERVICE_STEAM) {
                    page = buildSteamView(context);
                } else if (service == SERVICE_GITHUB) {
                    page = buildGitHubView(context);
                } else if (service == SERVICE_DISCORD) {
                    page = buildDiscordView(context);
                } else {
                    page = buildSpotifyView(context);
                }
            }
            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }

    // EMPTY STATE (0 platforms linked)
    private View buildEmptyView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));

        TextView title = new TextView(context);
        title.setText(MiogramLocale.get("Немає підключених платформ", "Нет подключенных платформ", "No Platforms Connected"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView subtitle = new TextView(context);
        subtitle.setText(MiogramLocale.get(
                "Підключіть Steam, GitHub, Discord або Spotify у налаштуваннях",
                "Подключите Steam, GitHub, Discord или Spotify в настройках",
                "Link Steam, GitHub, Discord or Spotify in settings"
        ));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subtitle.setTextColor(0x88B0C4DE);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView btnConnect = createButton(context, MiogramLocale.get("Підключити сервіси", "Подключить сервисы", "Connect Services"), 0x3366C0F4, 0xFF66C0F4);
        btnConnect.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            openConnectedAppsHub(context);
        });
        root.addView(btnConnect, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));

        return root;
    }

    // SLIDE: Steam
    private View buildSteamView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramSteamManager.SteamProfile p = steamProfile;
        boolean hasGame = p != null && p.hasGame();

        LinearLayout contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(contentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        BackupImageView artwork = new BackupImageView(context);
        artwork.setClipToOutline(true);
        artwork.setRoundRadius(AndroidUtilities.dp(10));
        contentRow.addView(artwork, LayoutHelper.createLinear(76, 52, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        contentRow.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFFFFF);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitle.setTextColor(0xFF8FB0C6);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        if (p != null) {
            if (hasGame) {
                title.setText(Emoji.replaceEmoji(p.gameName, title.getPaint().getFontMetricsInt(), false));
                String hours = !TextUtils.isEmpty(p.gameHours2Weeks)
                        ? (p.gameHours2Weeks + " " + MiogramLocale.get("год за 2 тижні", "ч за 2 недели", "hrs past 2 wks"))
                        : MiogramLocale.get("Запущено на ПК", "Запущено на ПК", "Playing on PC");
                subtitle.setText(hours);

                String capsuleUrl = !TextUtils.isEmpty(p.gameId)
                        ? "https://cdn.cloudflare.steamstatic.com/steam/apps/" + p.gameId + "/capsule_184x69.jpg"
                        : p.gameIconUrl;
                if (!TextUtils.isEmpty(capsuleUrl)) {
                    artwork.setImage(ImageLocation.getForPath(capsuleUrl), "184_69", null, 0, null);
                } else {
                    artwork.setImageResource(R.drawable.baseline_videogame_asset_16);
                }
            } else {
                title.setText(p.personaName);
                subtitle.setText(!TextUtils.isEmpty(p.stateMessage) ? p.stateMessage : MiogramLocale.get("Зараз не у грі", "Сейчас не в игре", "Not in game"));
                if (!TextUtils.isEmpty(p.avatarUrl)) {
                    artwork.setImage(ImageLocation.getForPath(p.avatarUrl), "100_100", null, 0, null);
                } else {
                    artwork.setImageResource(R.drawable.baseline_videogame_asset_16);
                }
            }
        } else {
            title.setText("Steam Profile");
            subtitle.setText(MiogramLocale.get("Завантаження даних...", "Загрузка данных...", "Loading profile..."));
            artwork.setImageResource(R.drawable.baseline_videogame_asset_16);
        }

        // Action Buttons (Clean Durov typography)
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (p != null && hasGame && !TextUtils.isEmpty(p.gameId)) {
            TextView btnPlay = createButton(context, MiogramLocale.get("Зайти в гру", "Зайти в игру", "Launch Game"), 0xFF5C7E10, 0xFFFFFFFF);
            btnPlay.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramSteamManager.getInstance().openGame(context, p.gameId);
            });
            actions.addView(btnPlay, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));
        }

        if (p != null && !TextUtils.isEmpty(p.steamId)) {
            TextView btnFriend = createButton(context, MiogramLocale.get("Додати в друзі", "Добавить в друзья", "Add Friend"), 0x3366C0F4, 0xFF66C0F4);
            btnFriend.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramSteamManager.getInstance().addFriend(context, p.steamId);
            });
            actions.addView(btnFriend, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));
        }

        TextView btnProf = createButton(context, MiogramLocale.get("Профіль", "Профиль", "Profile"), 0x2AFFFFFF, 0xFFD2DBE3);
        btnProf.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            if (p != null) {
                MiogramSteamManager.getInstance().openProfile(context, p.profileUrl, p.steamId);
            } else {
                openConnectedAppsHub(context);
            }
        });
        actions.addView(btnProf, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));

        return root;
    }

    // SLIDE: GitHub (Account Profile)
    private View buildGitHubView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramGitHubManager gm = MiogramGitHubManager.getInstance();
        MiogramGitHubManager.GitHubUser u = githubUser;

        LinearLayout contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(contentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(24));
        if (u != null && !TextUtils.isEmpty(u.avatarUrl)) {
            avatar.setImage(ImageLocation.getForPath(u.avatarUrl), "100_100", null, 0, null);
        } else {
            avatar.setImageResource(R.drawable.msg_fave);
        }
        contentRow.addView(avatar, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        contentRow.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView name = new TextView(context);
        name.setText(u != null ? u.getDisplayName() : ("@" + gm.getLinkedUsername()));
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFFFFFFFF);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView stats = new TextView(context);
        String statsText;
        if (u != null) {
            String repoStr = u.publicRepos + " " + MiogramLocale.get("репозиторіїв", "репозиториев", "repos");
            String follStr = u.followers + " " + MiogramLocale.get("читачів", "читателей", "followers");
            statsText = "@" + u.username + " • " + repoStr + " • " + follStr;
        } else {
            statsText = "@" + gm.getLinkedUsername();
        }
        stats.setText(statsText);
        stats.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        stats.setTextColor(0xFFB0C4DE);
        stats.setSingleLine(true);
        stats.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(stats, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // Activity / Bio sub-line
        if (u != null && (!TextUtils.isEmpty(u.latestActivityRepo) || !TextUtils.isEmpty(u.bio))) {
            TextView activity = new TextView(context);
            if (!TextUtils.isEmpty(u.latestActivityRepo)) {
                activity.setText(MiogramLocale.get("Остання активність: ", "Последняя активность: ", "Latest activity: ") + u.latestActivityRepo);
            } else {
                activity.setText(u.bio);
            }
            activity.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            activity.setTextColor(0x88B0C4DE);
            activity.setSingleLine(true);
            activity.setEllipsize(TextUtils.TruncateAt.END);
            root.addView(activity, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        }

        // Action buttons
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnProfile = createButton(context, MiogramLocale.get("Профіль", "Профиль", "Profile"), 0x2AFFFFFF, 0xFFFFFFFF);
        btnProfile.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            gm.openProfile(context);
        });
        actions.addView(btnProfile, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));

        TextView btnRepos = createButton(context, MiogramLocale.get("Репозиторії", "Репозитории", "Repositories"), 0x1A66C0F4, 0xFF66C0F4);
        btnRepos.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            gm.openUserRepos(context);
        });
        actions.addView(btnRepos, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

        TextView btnRefresh = createButton(context, MiogramLocale.get("Оновити", "Обновить", "Refresh"), 0x12FFFFFF, 0xFFD2DBE3);
        btnRefresh.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            gm.fetchUser(true, user -> {
                this.githubUser = user;
                pagerAdapter.notifyDataSetChanged();
            });
        });
        actions.addView(btnRefresh, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));

        return root;
    }

    // SLIDE: Discord Presence
    private View buildDiscordView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramDiscordManager.DiscordPresence d = discordPresence;

        LinearLayout contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(contentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(24));
        if (d != null && !TextUtils.isEmpty(d.avatarUrl)) {
            avatar.setImage(ImageLocation.getForPath(d.avatarUrl), "100_100", null, 0, null);
        } else {
            avatar.setImageResource(R.drawable.msg_contacts);
        }
        contentRow.addView(avatar, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        contentRow.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView name = new TextView(context);
        name.setText(d != null ? d.getDisplayName() : "Discord");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(0xFFFFFFFF);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView status = new TextView(context);
        String statusText;
        int statusColor = 0xFF80848E;
        if (d != null) {
            statusColor = d.getStatusColor();
            if (!TextUtils.isEmpty(d.activityName)) {
                statusText = d.activityName + (!TextUtils.isEmpty(d.activityDetails) ? (" • " + d.activityDetails) : "");
            } else if (!TextUtils.isEmpty(d.customStatus)) {
                statusText = d.customStatus;
            } else {
                statusText = d.getStatusText();
            }
        } else {
            statusText = MiogramLocale.get("Синхронізація Lanyard...", "Синхронизация Lanyard...", "Lanyard presence...");
        }
        status.setText(statusText);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        status.setTextColor(statusColor);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // Action buttons
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnProfile = createButton(context, MiogramLocale.get("Профіль", "Профиль", "Profile"), 0x335865F2, 0xFFFFFFFF);
        btnProfile.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramDiscordManager.getInstance().openProfile(context);
        });
        actions.addView(btnProfile, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));

        TextView btnCopy = createButton(context, MiogramLocale.get("Скопіювати ID", "Скопировать ID", "Copy ID"), 0x2AFFFFFF, 0xFFD2DBE3);
        btnCopy.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramDiscordManager.getInstance().copyId(context);
        });
        actions.addView(btnCopy, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

        TextView btnRefresh = createButton(context, MiogramLocale.get("Оновити", "Обновить", "Refresh"), 0x12FFFFFF, 0xFFD2DBE3);
        btnRefresh.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramDiscordManager.getInstance().fetchPresence(true, presence -> {
                this.discordPresence = presence;
                pagerAdapter.notifyDataSetChanged();
            });
        });
        actions.addView(btnRefresh, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));

        return root;
    }

    // SLIDE: Spotify Live
    private View buildSpotifyView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramSpotifyManager sm = MiogramSpotifyManager.getInstance();
        boolean playing = sm.isPlaying();
        String track = playing ? sm.getCurrentTrack() : MiogramLocale.get("Spotify в режимі очікування", "Spotify в режиме ожидания", "Spotify in Standby");
        String artist = playing ? sm.getCurrentArtist() : MiogramLocale.get("Увімкніть трек у додатку", "Включите трек в приложении", "Play a track in Spotify");

        LinearLayout contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(contentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        BackupImageView artwork = new BackupImageView(context);
        artwork.setRoundRadius(AndroidUtilities.dp(10));
        artwork.setImageResource(R.drawable.msg_media);
        contentRow.addView(artwork, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        contentRow.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView trackView = new TextView(context);
        trackView.setText(track);
        trackView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        trackView.setTypeface(AndroidUtilities.bold());
        trackView.setTextColor(0xFFFFFFFF);
        trackView.setSingleLine(true);
        trackView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(trackView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView artistView = new TextView(context);
        artistView.setText(artist);
        artistView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        artistView.setTextColor(0xFF1DB954);
        artistView.setSingleLine(true);
        artistView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(artistView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // Synced lyrics line if available
        String lyrics = sm.getCurrentLyricsLine();
        if (playing && !TextUtils.isEmpty(lyrics)) {
            TextView lyricsView = new TextView(context);
            lyricsView.setText(lyrics);
            lyricsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            lyricsView.setTextColor(0xAAFFFFFF);
            lyricsView.setSingleLine(true);
            lyricsView.setEllipsize(TextUtils.TruncateAt.END);
            root.addView(lyricsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        }

        // Action buttons
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (playing) {
            TextView btnPlayTG = createButton(context, MiogramLocale.get("Грати в Telegram", "Играть в Telegram", "Play in Telegram"), 0xFF1DB954, 0xFFFFFFFF);
            btnPlayTG.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                sm.checkAutoTransfer(context, UserConfig.selectedAccount);
            });
            actions.addView(btnPlayTG, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));
        }

        TextView btnOpenSpot = createButton(context, MiogramLocale.get("Відкрити Spotify", "Открыть Spotify", "Open Spotify"), 0x2AFFFFFF, 0xFFD2DBE3);
        btnOpenSpot.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            sm.openSpotifyApp(context);
        });
        actions.addView(btnOpenSpot, LayoutHelper.createLinear(0, 36, 1f, 0, 0, playing ? 0 : 6, 0));

        if (!playing) {
            TextView btnGuide = createButton(context, MiogramLocale.get("Інструкція", "Инструкция", "Setup Guide"), 0x1A1DB954, 0xFF1DB954);
            btnGuide.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                new app.miogram.bridge.spotify.MiogramSpotifySheet(context, resourcesProvider).show();
            });
            actions.addView(btnGuide, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));
        }

        return root;
    }
}
