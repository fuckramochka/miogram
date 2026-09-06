package app.miogram.bridge.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;
import app.miogram.bridge.lyrics.MiogramLyricsView;

/**
 * Modern High-Fidelity Audio Player Layout matching the reference design:
 * - Direct full-screen Karaoke Lyrics view (with Top Header: Title, Artist, [A], [≡-], [✕] + Waveform Dots)
 * - Seamless toggle to Album Cover / Playlist Queue
 * - Compact, beautifully balanced bottom controls:
 *   - Seekbar with current and total timestamps
 *   - 5 reliable playback buttons: Repeat, Prev, Play/Pause, Next, Queue
 *   - Profile "+ Додати в профіль" button
 */
public class MiogramModernPlayerLayout extends FrameLayout {

    private final AudioPlayerAlert alert;
    private final Theme.ResourcesProvider resourcesProvider;

    // Content Container
    private final FrameLayout contentContainer;
    private MiogramLyricsView lyricsView;
    private FrameLayout coverContainer;
    private View coverView;
    private FrameLayout queueContainer;
    private View queueListView;
    private enum PlayerMode { LYRICS, COVER, QUEUE }

    // A single source of truth prevents delayed fade callbacks from restoring an old page.
    private PlayerMode playerMode = PlayerMode.LYRICS;

    // Bottom Controls Section
    private final LinearLayout bottomSection;
    private final LinearLayout pageSwitcher;
    private final TextView lyricsModeButton;
    private final TextView coverModeButton;
    private final TextView queueModeButton;
    private final LinearLayout seekbarContainer;
    private View seekBarView;
    private final FrameLayout timersRow;
    private View timeView;
    private View durationView;

    private final LinearLayout mainControlsRow;
    private View repeatButton;
    private View prevButton;
    private FrameLayout heroPlayButton;
    private View playButton;
    private View nextButton;
    private ImageView queueButton;

    // Profile Button Container
    private final FrameLayout profileButtonContainer;
    private View saveToProfileButton;
    private View unsaveFromProfileButton;

    private boolean isPlaying = false;

    public MiogramModernPlayerLayout(Context context, AudioPlayerAlert alert, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.alert = alert;
        this.resourcesProvider = resourcesProvider;

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF101C2B, 0xFF0C131D, 0xFF080D14});
        setBackground(background);

        // Central Content Area (Lyrics / Cover / Queue)
        contentContainer = new FrameLayout(context);
        addView(contentContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 0, 0, 140));

        // Cover Container (Centered, for fallback or cover view)
        coverContainer = new FrameLayout(context);
        coverContainer.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= 21) {
            coverContainer.setElevation(AndroidUtilities.dp(10));
            coverContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(20));
                }
            });
            coverContainer.setClipToOutline(true);
        }
        contentContainer.addView(coverContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Queue Container (Full width/height inside contentContainer)
        queueContainer = new FrameLayout(context);
        queueContainer.setVisibility(View.GONE);
        GradientDrawable queueBackground = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF7132030, 0xFF0C131D});
        queueContainer.setBackground(queueBackground);
        contentContainer.addView(queueContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Bottom Controls Section
        bottomSection = new LinearLayout(context);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setGravity(Gravity.BOTTOM);
        bottomSection.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        GradientDrawable dockBackground = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00101C2B, 0xFA080D14});
        bottomSection.setBackground(dockBackground);

        // This control keeps every player scene discoverable instead of hiding lyrics behind one entry point.
        pageSwitcher = new LinearLayout(context);
        pageSwitcher.setOrientation(LinearLayout.HORIZONTAL);
        pageSwitcher.setGravity(Gravity.CENTER);
        pageSwitcher.setPadding(AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3));
        pageSwitcher.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), 0x26FFFFFF));

        lyricsModeButton = createModeButton(MiogramLocale.get("Текст", "Текст", "Lyrics"));
        coverModeButton = createModeButton(MiogramLocale.get("Обкладинка", "Обложка", "Cover"));
        queueModeButton = createModeButton(MiogramLocale.get("Черга", "Очередь", "Queue"));
        lyricsModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showLyrics(true);
        });
        coverModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showCover(true);
        });
        queueModeButton.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            showQueue(true, true);
        });
        pageSwitcher.addView(lyricsModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(32), 1f));
        pageSwitcher.addView(coverModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(32), 1f));
        pageSwitcher.addView(queueModeButton, new LinearLayout.LayoutParams(0, AndroidUtilities.dp(32), 1f));
        bottomSection.addView(pageSwitcher, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 0, 0, 6));

        // 1. Seekbar & Timestamps
        seekbarContainer = new LinearLayout(context);
        seekbarContainer.setOrientation(LinearLayout.VERTICAL);
        timersRow = new FrameLayout(context);
        seekbarContainer.addView(timersRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        bottomSection.addView(seekbarContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        // 2. Main Playback Controls Row (Repeat, Prev, Play/Pause, Next, Queue)
        mainControlsRow = new LinearLayout(context);
        mainControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        mainControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        mainControlsRow.setWeightSum(5);

        // Circular Hero Play button container (56x56 dp)
        heroPlayButton = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable();
        int accent = getThemedColor(Theme.key_featuredStickers_addButton);
        if (accent == 0) accent = 0xFF3390EC;
        heroBg.setColor(accent);
        heroBg.setShape(GradientDrawable.OVAL);
        heroPlayButton.setBackground(heroBg);
        if (Build.VERSION.SDK_INT >= 21) {
            heroPlayButton.setElevation(AndroidUtilities.dp(4));
        }

        bottomSection.addView(mainControlsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 0, 2, 0, 4));

        // 3. Profile Button Container ("+ Додати в профіль")
        profileButtonContainer = new FrameLayout(context);
        bottomSection.addView(profileButtonContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        addView(bottomSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
        updateModeButtons();
    }

    private TextView createModeButton(String text) {
        TextView button = new TextView(getContext());
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setContentDescription(text);
        return button;
    }

    public void setLyricsView(MiogramLyricsView view) {
        this.lyricsView = view;
        if (view != null) {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            view.setOnCloseClickListener(() -> {
                if (alert != null) {
                    alert.dismiss();
                }
            });
            contentContainer.addView(view, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            view.setVisibility(playerMode == PlayerMode.LYRICS ? View.VISIBLE : View.GONE);
            view.setAlpha(playerMode == PlayerMode.LYRICS ? 1f : 0f);
        }
    }

    public void setCoverView(View cover) {
        this.coverView = cover;
        if (coverContainer != null && cover != null) {
            if (cover.getParent() instanceof ViewGroup) {
                ((ViewGroup) cover.getParent()).removeView(cover);
            }
            coverContainer.removeAllViews();
            int size = Math.min(AndroidUtilities.dp(250), AndroidUtilities.displaySize.x - AndroidUtilities.dp(64));
            coverContainer.addView(cover, LayoutHelper.createFrame(size, size, Gravity.CENTER));
        }
    }

    public void setQueueListView(View listView) {
        this.queueListView = listView;
        if (queueContainer != null && listView != null) {
            if (listView.getParent() instanceof ViewGroup) {
                ((ViewGroup) listView.getParent()).removeView(listView);
            }
            queueContainer.removeAllViews();
            queueContainer.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }
    }

    public void setSeekBarViews(View seekBar, View time, View duration) {
        this.seekBarView = seekBar;
        this.timeView = time;
        this.durationView = duration;

        if (seekbarContainer != null && seekBar != null) {
            if (seekBar.getParent() instanceof ViewGroup) {
                ((ViewGroup) seekBar.getParent()).removeView(seekBar);
            }
            seekbarContainer.addView(seekBar, 0, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 30));
        }

        if (timersRow != null) {
            timersRow.removeAllViews();
            if (time != null) {
                if (time.getParent() instanceof ViewGroup) {
                    ((ViewGroup) time.getParent()).removeView(time);
                }
                timersRow.addView(time, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 4, 0, 0, 0));
            }
            if (duration != null) {
                if (duration.getParent() instanceof ViewGroup) {
                    ((ViewGroup) duration.getParent()).removeView(duration);
                }
                timersRow.addView(duration, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
            }
        }
    }

    public void setControlButtons(View repeat, View prev, View play, View next) {
        this.repeatButton = repeat;
        this.prevButton = prev;
        this.playButton = play;
        this.nextButton = next;

        if (mainControlsRow != null) {
            mainControlsRow.removeAllViews();

            // Slot 0: Repeat
            FrameLayout slot0 = new FrameLayout(getContext());
            if (repeat != null) {
                if (repeat.getParent() instanceof ViewGroup) ((ViewGroup) repeat.getParent()).removeView(repeat);
                repeat.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (alert != null) {
                        int mode = org.telegram.messenger.SharedConfig.repeatMode;
                        if (mode == 0) org.telegram.messenger.SharedConfig.setRepeatMode(1);
                        else if (mode == 1) org.telegram.messenger.SharedConfig.setRepeatMode(2);
                        else org.telegram.messenger.SharedConfig.setRepeatMode(0);
                        alert.updateRepeatButton();
                    }
                });
                slot0.addView(repeat, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            }
            mainControlsRow.addView(slot0, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 1: Previous (Explicit OnClickListener for 100% reliability)
            FrameLayout slot1 = new FrameLayout(getContext());
            if (prev != null) {
                if (prev.getParent() instanceof ViewGroup) ((ViewGroup) prev.getParent()).removeView(prev);
                prev.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playPreviousMessage();
                });
                slot1.addView(prev, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot1, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 2: Hero Play/Pause Button (56dp)
            FrameLayout slot2 = new FrameLayout(getContext());
            if (play != null) {
                if (play.getParent() instanceof ViewGroup) ((ViewGroup) play.getParent()).removeView(play);
                play.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    if (MediaController.getInstance().isDownloadingCurrentMessage()) return;
                    if (MediaController.getInstance().isMessagePaused()) {
                        MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
                    } else {
                        MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
                    }
                });
                heroPlayButton.removeAllViews();
                heroPlayButton.addView(play, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            }
            slot2.addView(heroPlayButton, LayoutHelper.createFrame(54, 54, Gravity.CENTER));
            mainControlsRow.addView(slot2, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 3: Next (Explicit OnClickListener for 100% reliability)
            FrameLayout slot3 = new FrameLayout(getContext());
            if (next != null) {
                if (next.getParent() instanceof ViewGroup) ((ViewGroup) next.getParent()).removeView(next);
                next.setOnClickListener(v -> {
                    MiogramHaptic.tap(v);
                    MediaController.getInstance().playNextMessage();
                });
                slot3.addView(next, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
            }
            mainControlsRow.addView(slot3, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));

            // Slot 4: Queue / Playlist Toggle Button
            FrameLayout slot4 = new FrameLayout(getContext());
            queueButton = new ImageView(getContext());
            queueButton.setImageResource(R.drawable.msg_list);
            queueButton.setScaleType(ImageView.ScaleType.CENTER);
            queueButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_player_button), PorterDuff.Mode.SRC_IN));
            queueButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
            queueButton.setContentDescription(MiogramLocale.get("Черга", "Очередь", "Queue"));
            queueButton.setOnClickListener(v -> {
                MiogramHaptic.tap(v);
                toggleQueue();
            });
            slot4.addView(queueButton, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
            mainControlsRow.addView(slot4, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1.0f));
        }
    }

    public void setProfileButtons(View saveBtn, View unsaveBtn) {
        this.saveToProfileButton = saveBtn;
        this.unsaveFromProfileButton = unsaveBtn;

        if (profileButtonContainer != null) {
            profileButtonContainer.removeAllViews();
            if (saveBtn != null) {
                if (saveBtn.getParent() instanceof ViewGroup) ((ViewGroup) saveBtn.getParent()).removeView(saveBtn);
                profileButtonContainer.addView(saveBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.CENTER));
            }
            if (unsaveBtn != null) {
                if (unsaveBtn.getParent() instanceof ViewGroup) ((ViewGroup) unsaveBtn.getParent()).removeView(unsaveBtn);
                profileButtonContainer.addView(unsaveBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.CENTER));
            }
        }
    }

    public void toggleQueue() {
        showQueue(!isQueueVisible(), true);
    }

    public boolean isQueueVisible() {
        return playerMode == PlayerMode.QUEUE;
    }

    public void showQueue(boolean show, boolean animated) {
        setPlayerMode(show ? PlayerMode.QUEUE : PlayerMode.LYRICS, animated);
    }

    public void showLyrics(boolean animated) {
        setPlayerMode(PlayerMode.LYRICS, animated);
    }

    public void showCover(boolean animated) {
        setPlayerMode(PlayerMode.COVER, animated);
    }

    private void setPlayerMode(PlayerMode mode, boolean animated) {
        if (playerMode == mode) return;
        PlayerMode previousMode = playerMode;
        playerMode = mode;

        View target = mode == PlayerMode.QUEUE ? queueContainer : mode == PlayerMode.COVER ? coverContainer : lyricsView;
        View previous = previousMode == PlayerMode.QUEUE ? queueContainer : previousMode == PlayerMode.COVER ? coverContainer : lyricsView;
        if (target == null) return;

        if (queueButton != null) {
            int color = mode == PlayerMode.QUEUE ? getThemedColor(Theme.key_player_buttonActive) : getThemedColor(Theme.key_player_button);
            queueButton.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        updateModeButtons();

        cancelPageAnimation(target);
        if (previous != null) cancelPageAnimation(previous);
        target.setVisibility(View.VISIBLE);

        if (!animated || previous == null || previous == target) {
            setPageVisible(target, true);
            if (previous != null && previous != target) setPageVisible(previous, false);
            return;
        }

        target.setAlpha(0f);
        target.setScaleX(0.985f);
        target.setScaleY(0.985f);
        target.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).start();
        final PlayerMode expectedMode = mode;
        previous.animate().alpha(0f).scaleX(0.985f).scaleY(0.985f).setDuration(180).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (playerMode == expectedMode) setPageVisible(previous, false);
            }
        }).start();
    }

    private void cancelPageAnimation(View page) {
        page.animate().setListener(null);
        page.animate().cancel();
    }

    private void setPageVisible(View page, boolean visible) {
        page.setVisibility(visible ? View.VISIBLE : View.GONE);
        page.setAlpha(visible ? 1f : 0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
    }

    private void updateModeButtons() {
        updateModeButton(lyricsModeButton, playerMode == PlayerMode.LYRICS);
        updateModeButton(coverModeButton, playerMode == PlayerMode.COVER);
        updateModeButton(queueModeButton, playerMode == PlayerMode.QUEUE);
    }

    private void updateModeButton(TextView button, boolean selected) {
        if (button == null) return;
        int accent = getThemedColor(Theme.key_featuredStickers_addButton);
        if (accent == 0) accent = 0xFF3390EC;
        button.setTextColor(selected ? 0xFFFFFFFF : 0xB3FFFFFF);
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), selected ? accent : 0x00000000));
        button.setScaleX(selected ? 1f : 0.96f);
        button.setScaleY(selected ? 1f : 0.96f);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (coverContainer != null) {
            coverContainer.animate().scaleX(playing ? 1.0f : 0.94f).scaleY(playing ? 1.0f : 0.94f).setDuration(220).start();
        }
    }

    public void setSong(MessageObject messageObject) {
        if (lyricsView != null && messageObject != null) {
            lyricsView.setSong(messageObject);
        }
    }

    public void setSpeedText(String speed) {
        // Handled in audio player state
    }

    public void setFavorite(boolean fav) {
        // Handled in audio player state
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}
