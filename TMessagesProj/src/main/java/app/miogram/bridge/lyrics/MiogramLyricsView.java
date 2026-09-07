package app.miogram.bridge.lyrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Modern High-Fidelity Synced Lyrics View with:
 * - Transparent background inheriting player theme
 * - Integrated, clean sub-bar with native ItemOptions source picker and translation toggles
 * - Themed accent colors matching custom Telegram themes (red, blue, purple, etc.)
 * - Word-by-word karaoke & line highlights
 * - On-demand translation with immediate feedback
 */
public class MiogramLyricsView extends FrameLayout {

    public static final int MODE_ORIGINAL = 0;
    public static final int MODE_BILINGUAL = 1;
    public static final int MODE_TRANSLATION = 2;

    private static int savedSourceId = MiogramLyricsEngine.SOURCE_AUTO;
    private int displayMode = MODE_ORIGINAL;
    private int currentSourceId = savedSourceId;

    private final Theme.ResourcesProvider resourcesProvider;

    // Sub-Bar Toolbar (Source selector + Translation toggles)
    private final LinearLayout subBar;
    private final TextView sourcePillButton;
    private final TextView translationPillButton;

    // Lyrics Recycler
    private final RecyclerView recyclerView;
    private final LyricsAdapter adapter;
    private final LinearLayoutManager layoutManager;

    // Empty / Error Container
    private final LinearLayout emptyContainer;
    private final ProgressBar progressBar;
    private final TextView emptyTitle;
    private final TextView emptySubtitle;
    private final LinearLayout emptyButtonsRow;
    private final TextView aiActionButton;
    private final TextView changeSourceButton;

    // Floating Pill Toast
    private final TextView toastPillView;
    private Runnable hideToastRunnable;

    // State
    private MessageObject currentMessageObject;
    private MiogramLrcModel.LrcSong currentSong;
    private int activePosition = -1;
    private boolean isUserScrolling = false;
    private long lastUserScrollTime = 0L;
    private long lyricsRequestGeneration = 0L;
    private Runnable onCloseClickListener;

    public interface OnActiveLineChangeListener {
        void onActiveLineChanged(String text, String translation, int index);
    }
    private OnActiveLineChangeListener onActiveLineChangeListener;

    public MiogramLyricsView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.layoutManager = new LinearLayoutManager(context);
        this.adapter = new LyricsAdapter();

        setBackgroundColor(0x00000000); // Fully transparent, inherits player's themed background

        int accent = getThemedAccent();

        // 1. Sub-Bar (Source selector pill on left + Translation mode pill on right)
        subBar = new LinearLayout(context);
        subBar.setOrientation(LinearLayout.HORIZONTAL);
        subBar.setGravity(Gravity.CENTER_VERTICAL);
        subBar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        sourcePillButton = new TextView(context);
        sourcePillButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        sourcePillButton.setTypeface(AndroidUtilities.bold());
        sourcePillButton.setTextColor(0xFFFFFFFF);
        sourcePillButton.setGravity(Gravity.CENTER);
        sourcePillButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        sourcePillButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), ColorUtils.setAlphaComponent(accent, 45)));
        sourcePillButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showSourceOptions(v);
        });
        updateSourcePillText();
        subBar.addView(sourcePillButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        View spacer = new View(context);
        subBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        translationPillButton = new TextView(context);
        translationPillButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        translationPillButton.setTypeface(AndroidUtilities.bold());
        translationPillButton.setTextColor(0xFFFFFFFF);
        translationPillButton.setGravity(Gravity.CENTER);
        translationPillButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        translationPillButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), ColorUtils.setAlphaComponent(accent, 45)));
        translationPillButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showTranslationOptions(v);
        });
        updateTranslationButton();
        subBar.addView(translationPillButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        addView(subBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.TOP, 0, 4, 0, 0));
        subBar.bringToFront();

        // 2. RecyclerView with lyrics - positioned strictly below subBar so it never overlaps buttons
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setClipToPadding(true);
        recyclerView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(24));
        recyclerView.setAdapter(adapter);
        recyclerView.setVerticalScrollBarEnabled(false);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true;
                    lastUserScrollTime = SystemClock.elapsedRealtime();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    lastUserScrollTime = SystemClock.elapsedRealtime();
                }
            }
        });
        addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 46, 0, 0));

        // 3. Compact Empty / Error Container - also positioned below subBar
        emptyContainer = new LinearLayout(context);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(Gravity.CENTER);
        emptyContainer.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(20), AndroidUtilities.dp(28), AndroidUtilities.dp(40));
        emptyContainer.setVisibility(View.GONE);

        progressBar = new ProgressBar(context);
        emptyContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        emptyTitle = new TextView(context);
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        emptyTitle.setTypeface(AndroidUtilities.bold());
        emptyTitle.setTextColor(0xFFFFFFFF);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyContainer.addView(emptyTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        emptySubtitle = new TextView(context);
        emptySubtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        emptySubtitle.setTextColor(0xAAFFFFFF);
        emptySubtitle.setGravity(Gravity.CENTER);
        emptyContainer.addView(emptySubtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        emptyButtonsRow = new LinearLayout(context);
        emptyButtonsRow.setOrientation(LinearLayout.VERTICAL);
        emptyButtonsRow.setGravity(Gravity.CENTER);

        aiActionButton = new TextView(context);
        aiActionButton.setText(MiogramLocale.get("Розпізнати текст через ШІ Gemini", "Распознать текст через ИИ Gemini", "Transcribe lyrics with Gemini AI"));
        aiActionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        aiActionButton.setTypeface(AndroidUtilities.bold());
        aiActionButton.setTextColor(0xFFFFFFFF);
        aiActionButton.setGravity(Gravity.CENTER);
        aiActionButton.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(11), AndroidUtilities.dp(20), AndroidUtilities.dp(11));
        aiActionButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(20), accent));
        aiActionButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (currentMessageObject != null) {
                transcribeWithAi(currentMessageObject);
            }
        });
        emptyButtonsRow.addView(aiActionButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        changeSourceButton = new TextView(context);
        changeSourceButton.setText(MiogramLocale.get("Змінити джерело пошуку", "Изменить источник поиска", "Change search source"));
        changeSourceButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        changeSourceButton.setTypeface(AndroidUtilities.bold());
        changeSourceButton.setTextColor(0xCCFFFFFF);
        changeSourceButton.setGravity(Gravity.CENTER);
        changeSourceButton.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(8), AndroidUtilities.dp(18), AndroidUtilities.dp(8));
        changeSourceButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0x25FFFFFF));
        changeSourceButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showSourceOptions(v);
        });
        emptyButtonsRow.addView(changeSourceButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        emptyContainer.addView(emptyButtonsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addView(emptyContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 46, 0, 0));

        // 4. Floating Toast Notification Pill
        toastPillView = new TextView(context);
        toastPillView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        toastPillView.setTypeface(AndroidUtilities.bold());
        toastPillView.setTextColor(0xFFFFFFFF);
        toastPillView.setGravity(Gravity.CENTER);
        toastPillView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable toastBg = new GradientDrawable();
        toastBg.setColor(0xE610151E);
        toastBg.setCornerRadius(AndroidUtilities.dp(18));
        toastBg.setStroke(AndroidUtilities.dp(1), ColorUtils.setAlphaComponent(accent, 120));
        toastPillView.setBackground(toastBg);
        toastPillView.setVisibility(View.GONE);
        toastPillView.setAlpha(0.0f);
        if (Build.VERSION.SDK_INT >= 21) {
            toastPillView.setElevation(AndroidUtilities.dp(8));
        }
        addView(toastPillView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 52, 0, 0));
    }

    private void updateSourcePillText() {
        sourcePillButton.setText(MiogramSourceSelectAlert.getSourceName(currentSourceId));
    }

    private void updateTranslationButton() {
        String modeText;
        if (displayMode == MODE_ORIGINAL) {
            modeText = MiogramLocale.get("Оригінал", "Оригинал", "Original");
        } else if (displayMode == MODE_BILINGUAL) {
            modeText = MiogramLocale.get("Двомовний", "Двуязычный", "Bilingual");
        } else {
            modeText = MiogramLocale.get("Переклад", "Перевод", "Translation");
        }
        translationPillButton.setText(modeText);
    }

    private void showSourceOptions(View anchor) {
        ViewGroup root = (ViewGroup) getRootView();
        if (root == null) root = this;
        ItemOptions options = ItemOptions.makeOptions(root, resourcesProvider, anchor);
        options.setRoundRadius(AndroidUtilities.dp(14));

        options.add(R.drawable.player_new_order, MiogramLocale.get("Автоматично (Auto)", "Автоматически (Auto)", "Automatic (Auto)"), () -> selectSource(MiogramLyricsEngine.SOURCE_AUTO));
        options.add(R.drawable.player_new_order, "LRCLib", () -> selectSource(MiogramLyricsEngine.SOURCE_LRCLIB));
        options.add(R.drawable.player_new_order, "NetEase", () -> selectSource(MiogramLyricsEngine.SOURCE_NETEASE));
        options.add(R.drawable.player_new_order, MiogramLocale.get("Яндекс Музика", "Яндекс Музыка", "Yandex Music"), () -> selectSource(MiogramLyricsEngine.SOURCE_YANDEX));
        options.add(R.drawable.player_new_order, "Genius", () -> selectSource(MiogramLyricsEngine.SOURCE_GENIUS));
        options.add(R.drawable.player_new_order, MiogramLocale.get("YouTube (Опис)", "YouTube (Описание)", "YouTube (Description)"), () -> selectSource(MiogramLyricsEngine.SOURCE_YOUTUBE));
        options.add(R.drawable.msg_bot, MiogramLocale.get("ШІ зі звуку (Gemini)", "ИИ со слуха (Gemini)", "AI by ear (Gemini)"), () -> selectSource(MiogramLyricsEngine.SOURCE_AI));

        options.show();
    }

    private void selectSource(int sourceId) {
        this.currentSourceId = sourceId;
        savedSourceId = sourceId;
        updateSourcePillText();
        showToastPill(MiogramSourceSelectAlert.getSourceName(sourceId));
        if (currentMessageObject != null) {
            if (sourceId == MiogramLyricsEngine.SOURCE_AI) {
                transcribeWithAi(currentMessageObject);
            } else {
                loadLyrics(currentMessageObject, sourceId);
            }
        }
    }

    private void showTranslationOptions(View anchor) {
        ViewGroup root = (ViewGroup) getRootView();
        if (root == null) root = this;
        ItemOptions options = ItemOptions.makeOptions(root, resourcesProvider, anchor);
        options.setRoundRadius(AndroidUtilities.dp(14));

        options.add(0, MiogramLocale.get("Оригінальний текст", "Оригинальный текст", "Original Lyrics"), () -> {
            displayMode = MODE_ORIGINAL;
            updateTranslationButton();
            adapter.notifyDataSetChanged();
            showToastPill(MiogramLocale.get("Тільки оригінал", "Только оригинал", "Original only"));
        });

        options.add(0, MiogramLocale.get("Двомовний режим", "Двуязычный режим", "Bilingual mode"), () -> {
            displayMode = MODE_BILINGUAL;
            updateTranslationButton();
            if (currentSong != null && !currentSong.hasAnyTranslation()) {
                requestTranslation();
            } else {
                adapter.notifyDataSetChanged();
                showToastPill(MiogramLocale.get("Двомовний режим увімкнено", "Двуязычный режим включен", "Bilingual mode on"));
            }
        });

        options.add(0, MiogramLocale.get("Тільки переклад", "Только перевод", "Translation only"), () -> {
            displayMode = MODE_TRANSLATION;
            updateTranslationButton();
            if (currentSong != null && !currentSong.hasAnyTranslation()) {
                requestTranslation();
            } else {
                adapter.notifyDataSetChanged();
                showToastPill(MiogramLocale.get("Тільки переклад", "Только перевод", "Translation only"));
            }
        });

        options.show();
    }

    private void requestTranslation() {
        if (currentSong == null) return;
        showToastPill(MiogramLocale.get("Перекладаємо текст...", "Переводим текст...", "Translating lyrics..."));
        MiogramLyricsEngine.getInstance().translateSongLines(currentSong, () -> {
            adapter.notifyDataSetChanged();
            showToastPill(MiogramLocale.get("Переклад готовий", "Перевод готов", "Translation ready"));
        });
    }

    private void showToastPill(String text) {
        if (hideToastRunnable != null) {
            removeCallbacks(hideToastRunnable);
        }
        toastPillView.animate().setListener(null);
        toastPillView.animate().cancel();
        toastPillView.setText(text);
        toastPillView.setVisibility(View.VISIBLE);
        toastPillView.animate().alpha(1.0f).setDuration(180).start();

        hideToastRunnable = () -> {
            toastPillView.animate().alpha(0.0f).setDuration(220).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    toastPillView.setVisibility(View.GONE);
                }
            }).start();
        };
        postDelayed(hideToastRunnable, 1500);
    }

    public void setOnCloseClickListener(Runnable listener) {
        this.onCloseClickListener = listener;
    }

    public void setOnActiveLineChangeListener(OnActiveLineChangeListener listener) {
        this.onActiveLineChangeListener = listener;
    }

    public void setSong(MessageObject messageObject) {
        this.currentMessageObject = messageObject;
        activePosition = -1;
        adapter.setLines(null);
        if (messageObject != null) {
            MiogramLrcModel.LrcSong cached = MiogramLyricsEngine.getInstance().getCachedSong(messageObject);
            if (cached != null && !cached.isEmpty()) {
                currentSong = cached;
                if ("✨ Gemini AI".equals(cached.source)) {
                    currentSourceId = MiogramLyricsEngine.SOURCE_AI;
                    savedSourceId = currentSourceId;
                }
                updateSourcePillText();
                showLoading(false);
                adapter.setLines(cached.lines);
                updateTranslationButton();
                updateTime(MediaController.getInstance().getPlayingMessageObject() != null
                        ? MediaController.getInstance().getPlayingMessageObject().audioProgressMs
                        : 0L);
                return;
            }
            loadLyrics(messageObject, currentSourceId);
        } else {
            showEmptyState(false, "");
        }
    }

    private void loadLyrics(final MessageObject messageObject, final int preferredSource) {
        final long reqGen = ++lyricsRequestGeneration;
        showLoading(true);

        MiogramLyricsEngine.getInstance().fetchLyrics(messageObject, preferredSource, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                if (reqGen != lyricsRequestGeneration) return;
                currentSong = song;
                showLoading(false);
                adapter.setLines(song.lines);
                updateTranslationButton();
                updateTime(MediaController.getInstance().getPlayingMessageObject() != null
                        ? MediaController.getInstance().getPlayingMessageObject().audioProgressMs
                        : 0L);
            }

            @Override
            public void onError(String message) {
                if (reqGen != lyricsRequestGeneration) return;
                showEmptyState(true, message);
            }
        });
    }

    private void transcribeWithAi(final MessageObject messageObject) {
        final long reqGen = ++lyricsRequestGeneration;
        showLoading(true);
        emptyTitle.setText(MiogramLocale.get("ШІ розпізнає текст пісні...", "ИИ распознает текст песни...", "AI is transcribing lyrics..."));
        emptySubtitle.setText(MiogramLocale.get("Це може зайняти до 10-15 секунд", "Это может занять до 10-15 секунд", "This may take 10-15 seconds"));

        MiogramLyricsEngine.getInstance().transcribeAudioWithAi(messageObject, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                if (reqGen != lyricsRequestGeneration) return;
                currentSong = song;
                currentSourceId = MiogramLyricsEngine.SOURCE_AI;
                updateSourcePillText();
                showLoading(false);
                adapter.setLines(song.lines);
                updateTranslationButton();
                showToastPill(MiogramLocale.get("ШІ-розпізнавання завершено! ✓", "ИИ-распознавание завершено! ✓", "AI transcription complete! ✓"));
            }

            @Override
            public void onError(String message) {
                if (reqGen != lyricsRequestGeneration) return;
                showEmptyState(true, message);
            }
        });
    }

    public void updateTime(long currentPositionMs) {
        if (currentSong == null || currentSong.lines.isEmpty()) {
            if (onActiveLineChangeListener != null) {
                onActiveLineChangeListener.onActiveLineChanged(null, null, -1);
            }
            return;
        }

        int newActive = currentSong.findLineIndex(currentPositionMs);
        if (newActive != activePosition) {
            int oldActive = activePosition;
            activePosition = newActive;

            if (oldActive != -1) adapter.notifyItemChanged(oldActive);
            if (activePosition != -1) adapter.notifyItemChanged(activePosition);

            if (activePosition != -1 && onActiveLineChangeListener != null) {
                MiogramLrcModel.LrcLine line = currentSong.lines.get(activePosition);
                onActiveLineChangeListener.onActiveLineChanged(line.text, line.translation, activePosition);
            }

            if (activePosition != -1 && !isUserScrolling && SystemClock.elapsedRealtime() - lastUserScrollTime > 3000L) {
                scrollToCenter(activePosition);
            }
        }

        if (activePosition != -1 && currentSong != null && activePosition < currentSong.lines.size()) {
            LyricsViewHolder holder = (LyricsViewHolder) recyclerView.findViewHolderForAdapterPosition(activePosition);
            if (holder != null) {
                MiogramLrcModel.LrcLine line = currentSong.lines.get(activePosition);
                long nextTime = (activePosition + 1 < currentSong.lines.size())
                        ? currentSong.lines.get(activePosition + 1).timeMs
                        : (line.timeMs + 4000L);
                long lineDuration = Math.max(800L, Math.min(10000L, nextTime - line.timeMs));
                float fraction = Math.max(0f, Math.min(1f, (float)(currentPositionMs - line.timeMs) / (float)lineDuration));
                holder.updateKaraokeProgress(fraction);
            }
        }
    }

    private void scrollToCenter(int position) {
        int height = recyclerView.getHeight();
        if (height <= 0) return;
        try {
            LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
                @Override
                protected int getVerticalSnapPreference() {
                    return SNAP_TO_START;
                }

                @Override
                public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                    return (boxStart + (boxEnd - boxStart) / 3) - viewStart;
                }

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return 140f / displayMetrics.densityDpi;
                }
            };
            scroller.setTargetPosition(position);
            layoutManager.startSmoothScroll(scroller);
        } catch (Throwable ignore) {
            int targetOffset = height / 3;
            layoutManager.scrollToPositionWithOffset(position, targetOffset);
        }
    }

    private void showLoading(boolean show) {
        emptyContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyButtonsRow.setVisibility(View.GONE);

        if (show) {
            emptyTitle.setText(MiogramLocale.get("Пошук тексту пісні...", "Поиск текста песни...", "Searching lyrics..."));
            emptySubtitle.setText(MiogramLocale.get("Джерело: ", "Источник: ", "Source: ") + MiogramSourceSelectAlert.getSourceName(currentSourceId));
        }
    }

    private void showEmptyState(boolean error, String message) {
        emptyContainer.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyButtonsRow.setVisibility(View.VISIBLE);

        emptyTitle.setText(MiogramLocale.get("Текст не знайдено", "Текст не найден", "Lyrics not found"));
        emptySubtitle.setText(TextUtils.isEmpty(message)
                ? MiogramLocale.get("Спробуйте розпізнати через ШІ або змінити джерело", "Попробуйте распознать через ИИ или сменить источник", "Try AI transcription or change source")
                : message);
    }

    private int getThemedAccent() {
        int color = Theme.getColor(Theme.key_player_progress, resourcesProvider);
        if (color == 0 || color == 0xFF3390EC) {
            int active = Theme.getColor(Theme.key_player_buttonActive, resourcesProvider);
            if (active != 0 && active != 0xFF3390EC) return active;
            int chats = Theme.getColor(Theme.key_chats_actionBackground, resourcesProvider);
            if (chats != 0 && chats != 0xFF3390EC) return chats;
            if (active != 0) return active;
        }
        return color != 0 ? color : 0xFF3390EC;
    }

    /* =========================================================================
     * ADAPTER
     * ========================================================================= */

    private class LyricsAdapter extends RecyclerView.Adapter<LyricsViewHolder> {
        private final List<MiogramLrcModel.LrcLine> items = new ArrayList<>();

        public void setLines(List<MiogramLrcModel.LrcLine> newLines) {
            items.clear();
            if (newLines != null) {
                items.addAll(newLines);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LyricsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(parent.getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

            TextView main = new TextView(parent.getContext());
            main.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            main.setTypeface(AndroidUtilities.bold());
            main.setTextColor(0xFFFFFFFF);
            item.addView(main, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            View underline = new View(parent.getContext());
            underline.setVisibility(View.GONE);
            item.addView(underline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 2, 0, 4, 0, 2));

            TextView trans = new TextView(parent.getContext());
            trans.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            trans.setTextColor(0xCCFFFFFF);
            trans.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            item.addView(trans, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            return new LyricsViewHolder(item, main, underline, trans);
        }

        @Override
        public void onBindViewHolder(@NonNull LyricsViewHolder holder, int position) {
            MiogramLrcModel.LrcLine line = items.get(position);
            boolean isActive = (position == activePosition);
            int accent = getThemedAccent();

            String displayText;
            if (displayMode == MODE_BILINGUAL) {
                displayText = line.text;
                if (line.hasTranslation()) {
                    holder.transText.setVisibility(View.VISIBLE);
                    holder.transText.setText(line.translation);
                } else {
                    holder.transText.setVisibility(View.GONE);
                }
            } else if (displayMode == MODE_ORIGINAL) {
                displayText = line.text;
                holder.transText.setVisibility(View.GONE);
            } else {
                displayText = line.hasTranslation() ? line.translation : line.text;
                holder.transText.setVisibility(View.GONE);
            }

            holder.underline.setBackgroundColor(accent);

            if (isActive) {
                holder.mainText.setTextColor(0xFFFFFFFF);
                holder.mainText.setAlpha(1.0f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                holder.underline.setVisibility(View.VISIBLE);
                holder.transText.setAlpha(0.9f);
                holder.itemView.setScaleX(1.02f);
                holder.itemView.setScaleY(1.02f);
                holder.bindText(displayText);
                holder.updateKaraokeProgress(0f);
            } else {
                holder.mainText.setTextColor(0x60FFFFFF);
                holder.mainText.setAlpha(1.0f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                holder.mainText.setText(displayText);
                holder.underline.setVisibility(View.GONE);
                holder.transText.setAlpha(0.28f);
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
                holder.bindText("");
            }

            holder.itemView.setOnClickListener(v -> {
                if (currentMessageObject != null) {
                    MiogramHaptic.select(v);
                    float dur = currentMessageObject.audioPlayerDuration > 0
                            ? (float) currentMessageObject.audioPlayerDuration
                            : (float) currentMessageObject.getDuration();
                    if (dur > 0) {
                        float progress = (float) line.timeMs / (dur * 1000f);
                        MediaController.getInstance().seekToProgress(currentMessageObject, Math.max(0f, Math.min(1f, progress)));
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class LyricsViewHolder extends RecyclerView.ViewHolder {
        final TextView mainText;
        final View underline;
        final TextView transText;
        private String rawText = "";
        private int lastSungChars = -1;

        public LyricsViewHolder(View itemView, TextView main, View underline, TextView trans) {
            super(itemView);
            this.mainText = main;
            this.underline = underline;
            this.transText = trans;
        }

        public void bindText(String text) {
            this.rawText = text != null ? text : "";
            this.lastSungChars = -1;
        }

        public void updateKaraokeProgress(float fraction) {
            if (TextUtils.isEmpty(rawText)) return;
            int len = rawText.length();
            int sungChars = Math.round(len * fraction);
            sungChars = Math.max(0, Math.min(len, sungChars));
            if (sungChars == lastSungChars) return;
            lastSungChars = sungChars;

            android.text.SpannableString span = new android.text.SpannableString(rawText);
            if (sungChars > 0) {
                span.setSpan(new android.text.style.ForegroundColorSpan(0xFFFFFFFF), 0, sungChars, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (sungChars < len) {
                span.setSpan(new android.text.style.ForegroundColorSpan(0x60FFFFFF), sungChars, len, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mainText.setText(span, TextView.BufferType.SPANNABLE);
        }
    }
}
