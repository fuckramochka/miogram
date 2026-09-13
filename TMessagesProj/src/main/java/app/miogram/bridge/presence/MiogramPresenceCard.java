package app.miogram.bridge.presence;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

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
 * Combines Steam Gaming, GitHub Actions CI, Discord Activity, and Spotify into
 * a single sleek horizontal swipeable container with native Durov-level micro-interactions.
 * Zero tacky emojis in buttons — pure, elegant Telegram typography.
 */
public class MiogramPresenceCard extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final ViewPager viewPager;
    private final PresencePagerAdapter pagerAdapter;
    private final LinearLayout tabRow;
    private final List<TextView> tabViews = new ArrayList<>();

    private MiogramSteamManager.SteamProfile steamProfile;
    private MiogramGitHubManager.WorkflowRun githubRun;
    private MiogramDiscordManager.DiscordPresence discordPresence;

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

        // 1. Header Tabs Switcher
        tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        container.addView(tabRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        setupTab("STEAM", 0);
        setupTab("GITHUB", 1);
        setupTab("DISCORD", 2);
        setupTab("SPOTIFY", 3);

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
                updateTabSelection(position);
            }
        });
        container.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateTabSelection(0);

        // Preload live data
        loadLiveData();
    }

    private void setupTab(String title, int index) {
        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
        tab.setTypeface(AndroidUtilities.bold());
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        ScaleStateListAnimator.apply(tab, 0.035f, 1.4f);
        tab.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            viewPager.setCurrentItem(index, true);
        });
        tabViews.add(tab);
        tabRow.addView(tab, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
    }

    private void updateTabSelection(int selectedIndex) {
        for (int i = 0; i < tabViews.size(); i++) {
            TextView t = tabViews.get(i);
            boolean isSel = (i == selectedIndex);
            t.setTextColor(isSel ? 0xFFFFFFFF : 0x8866C0F4);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(isSel ? 0x4466C0F4 : 0x1266C0F4);
            bg.setCornerRadius(AndroidUtilities.dp(6));
            if (isSel) {
                bg.setStroke(AndroidUtilities.dp(1), 0x8866C0F4);
            }
            t.setBackground(bg);
        }
    }

    public void setSteamProfile(MiogramSteamManager.SteamProfile profile) {
        this.steamProfile = profile;
        pagerAdapter.notifyDataSetChanged();
    }

    public void loadLiveData() {
        MiogramGitHubManager.getInstance().fetchLatestWorkflow(false, run -> {
            this.githubRun = run;
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
            return 4; // Steam, GitHub, Discord, Spotify
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View page;
            if (position == 0) {
                page = buildSteamView(context);
            } else if (position == 1) {
                page = buildGitHubView(context);
            } else if (position == 2) {
                page = buildDiscordView(context);
            } else {
                page = buildSpotifyView(context);
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

    // SLIDE 0: Steam
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
            subtitle.setText(MiogramLocale.get("Підключіть профіль у налаштуваннях", "Подключите профиль в настройках", "Link profile in settings"));
            artwork.setImageResource(R.drawable.baseline_videogame_asset_16);
        }

        // Action Buttons (Clean, 0 emojis)
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (p != null) {
            if (hasGame && !TextUtils.isEmpty(p.gameId)) {
                TextView btnPlay = createButton(context, MiogramLocale.get("Зайти в гру", "Зайти в игру", "Launch Game"), 0xFF5C7E10, 0xFFFFFFFF);
                btnPlay.setOnClickListener(v -> {
                    MiogramHaptic.click(v);
                    MiogramSteamManager.getInstance().openGame(context, p.gameId);
                });
                actions.addView(btnPlay, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));
            }

            TextView btnFriend = createButton(context, MiogramLocale.get("Додати в друзі", "Добавить в друзья", "Add Friend"), 0x3366C0F4, 0xFF66C0F4);
            btnFriend.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                if (!TextUtils.isEmpty(p.steamId)) {
                    MiogramSteamManager.getInstance().addFriend(context, p.steamId);
                }
            });
            actions.addView(btnFriend, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

            TextView btnProf = createButton(context, MiogramLocale.get("Профіль", "Профиль", "Profile"), 0x2AFFFFFF, 0xFFD2DBE3);
            btnProf.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramSteamManager.getInstance().openProfile(context, p.profileUrl, p.steamId);
            });
            actions.addView(btnProf, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));
        } else {
            TextView btnSetup = createButton(context, MiogramLocale.get("Підключити Steam", "Подключить Steam", "Link Steam"), 0x3366C0F4, 0xFF66C0F4);
            btnSetup.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                new app.miogram.bridge.steam.MiogramSteamSheet(context, resourcesProvider).show();
            });
            actions.addView(btnSetup, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        }

        return root;
    }

    // SLIDE 1: GitHub Actions CI
    private View buildGitHubView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramGitHubManager.WorkflowRun r = githubRun;

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView repoName = new TextView(context);
        repoName.setText(r != null ? r.repo : MiogramGitHubManager.getInstance().getTrackedRepo());
        repoName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        repoName.setTypeface(AndroidUtilities.bold());
        repoName.setTextColor(0xFFFFFFFF);
        repoName.setSingleLine(true);
        repoName.setEllipsize(TextUtils.TruncateAt.END);
        headerRow.addView(repoName, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView statusBadge = new TextView(context);
        statusBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        statusBadge.setTypeface(AndroidUtilities.bold());
        statusBadge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(AndroidUtilities.dp(6));

        if (r != null && r.isSuccess()) {
            statusBadge.setText("SUCCESS");
            statusBadge.setTextColor(0xFFFFFFFF);
            badgeBg.setColor(0xFF238636);
        } else if (r != null && r.isRunning()) {
            statusBadge.setText("BUILDING");
            statusBadge.setTextColor(0xFF000000);
            badgeBg.setColor(0xFFD29922);
        } else if (r != null && r.isFailed()) {
            statusBadge.setText("FAILED");
            statusBadge.setTextColor(0xFFFFFFFF);
            badgeBg.setColor(0xFFDA3633);
        } else {
            statusBadge.setText("WORKFLOW");
            statusBadge.setTextColor(0xFFFFFFFF);
            badgeBg.setColor(0x4466C0F4);
        }
        statusBadge.setBackground(badgeBg);
        headerRow.addView(statusBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView commitMsg = new TextView(context);
        commitMsg.setText(r != null && !TextUtils.isEmpty(r.commitMessage) ? r.commitMessage : "Latest GitHub Actions Release");
        commitMsg.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        commitMsg.setTextColor(0xFFB0C4DE);
        commitMsg.setSingleLine(true);
        commitMsg.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(commitMsg, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView branchText = new TextView(context);
        String branchInfo = (r != null && !TextUtils.isEmpty(r.branch))
                ? ("branch: " + r.branch + (TextUtils.isEmpty(r.commitSha) ? "" : " • " + r.commitSha))
                : "Continuous Integration & Builds";
        branchText.setText(branchInfo);
        branchText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        branchText.setTextColor(0x88B0C4DE);
        root.addView(branchText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Action buttons
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnSelect = createButton(context, MiogramLocale.get("Вибрати репо", "Выбрать репо", "Select Repo"), 0x2AFFFFFF, 0xFFFFFFFF);
        btnSelect.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramGitHubManager.getInstance().showSelectRepoDialog(context, run -> {
                this.githubRun = run;
                pagerAdapter.notifyDataSetChanged();
            });
        });
        actions.addView(btnSelect, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));

        TextView btnRefresh = createButton(context, MiogramLocale.get("Оновити", "Обновить", "Refresh"), 0x3366C0F4, 0xFF66C0F4);
        btnRefresh.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramGitHubManager.getInstance().fetchLatestWorkflow(true, run -> {
                this.githubRun = run;
                pagerAdapter.notifyDataSetChanged();
            });
        });
        actions.addView(btnRefresh, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

        TextView btnRuns = createButton(context, MiogramLocale.get("Всі запуски", "Все запуски", "Workflow Runs"), 0x1A66C0F4, 0xFFD2DBE3);
        btnRuns.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            MiogramGitHubManager.getInstance().openWorkflowRuns(context);
        });
        actions.addView(btnRuns, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));

        return root;
    }

    // SLIDE 2: Discord Presence
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
            avatar.setImageResource(R.drawable.msg_filled_menu_users_solar);
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
            statusText = MiogramLocale.get("Прив'яжіть Discord у налаштуваннях", "Привяжите Discord в настройках", "Link Discord in settings");
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

        if (d != null) {
            TextView btnProfile = createButton(context, MiogramLocale.get("Профіль", "Профиль", "Profile"), 0x335865F2, 0xFFFFFFFF);
            btnProfile.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramDiscordManager.getInstance().openProfile(context);
            });
            actions.addView(btnProfile, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 6, 0));

            TextView btnCopy = createButton(context, MiogramLocale.get("Скопіювати ID", "Скопировать ID", "Copy ID"), 0x2AFFFFFF, 0xFFD2DBE3);
            btnCopy.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramDiscordManager.getInstance().copyId(context);
            });
            actions.addView(btnCopy, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));
        } else {
            TextView btnSetup = createButton(context, MiogramLocale.get("Підключити Discord", "Подключить Discord", "Link Discord"), 0x335865F2, 0xFFFFFFFF);
            btnSetup.setOnClickListener(v -> {
                MiogramHaptic.click(v);
                MiogramDiscordManager.getInstance().showConfigDialog(context, updatedPresence -> {
                    this.discordPresence = updatedPresence;
                    pagerAdapter.notifyDataSetChanged();
                });
            });
            actions.addView(btnSetup, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        }

        return root;
    }

    // SLIDE 3: Spotify Live
    private View buildSpotifyView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        MiogramSpotifyManager sm = MiogramSpotifyManager.getInstance();
        boolean playing = sm.isPlaying();
        String track = playing ? sm.getCurrentTrack() : MiogramLocale.get("Не грає", "Не играет", "Not playing");
        String artist = playing ? sm.getCurrentArtist() : MiogramLocale.get("Синхронізація Spotify", "Синхронизация Spotify", "Spotify Sync");

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

        // Action buttons
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView btnPlayTG = createButton(context, MiogramLocale.get("Грати в Telegram", "Играть в Telegram", "Play in Telegram"), 0xFF1DB954, 0xFFFFFFFF);
        btnPlayTG.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            sm.checkAutoTransfer(context, org.telegram.messenger.UserConfig.selectedAccount);
        });
        actions.addView(btnPlayTG, LayoutHelper.createLinear(0, 36, 1.2f, 0, 0, 6, 0));

        TextView btnOpenSpot = createButton(context, MiogramLocale.get("Відкрити Spotify", "Открыть Spotify", "Open Spotify"), 0x2AFFFFFF, 0xFFD2DBE3);
        btnOpenSpot.setOnClickListener(v -> {
            MiogramHaptic.click(v);
            sm.openSpotifyApp(context);
        });
        actions.addView(btnOpenSpot, LayoutHelper.createLinear(0, 36, 1f, 0, 0, 0, 0));

        return root;
    }
}
