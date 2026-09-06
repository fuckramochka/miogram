package app.miogram.bridge.lyrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Modern High-Fidelity Karaoke Lyrics View matching the reference player:
 * - Top title & artist with translation toggle [A], source selector [≡-], and close [✕]
 * - Horizontal rhythm waveform dots visualizer
 * - Live synchronized scrolling karaoke with bold active line and subtle glow/underline
 * - Click-to-seek to any lyric timestamp
 * - Source selection dialog integration (LRCLib, NetEase, Yandex, Genius, YouTube, AI)
 * - Floating pill toast feedback on source change
 * - Graceful fallback when lyrics not found without covering player controls
 */
public class MiogramLyricsView extends FrameLayout {

    public static final int MODE_BILINGUAL = 0;
    public static final int MODE_ORIGINAL = 1;
    public static final int MODE_TRANSLATION = 2;

    private int displayMode = MODE_BILINGUAL;
    private int currentSourceId = MiogramLyricsEngine.SOURCE_AUTO;

    private final Theme.ResourcesProvider resourcesProvider;

    // Header Views
    private final LinearLayout headerLayout;
    private final LinearLayout titleBox;
    private final TextView titleView;
    private final TextView artistView;
    private final LinearLayout actionsBox;
    private final TextView modeButton;
    private final ImageView sourceButton;
    private final ImageView closeButton;
    private final WaveformDotsView waveformDotsView;

    // Content Views
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
    private Runnable onCloseClickListener;

    public interface OnActiveLineChangeListener {
        void onActiveLineChanged(String text, String translation, int index);
    }
    private OnActiveLineChangeListener onActiveLineChangeListener;

    public MiogramLyricsView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setBackgroundColor(0xEE0B1118); // Deep midnight backdrop

        // 1. Top Header Row
        headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(8));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Title and Artist column
        titleBox = new LinearLayout(context);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        titleView.setSelected(true);
        titleBox.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        artistView = new TextView(context);
        artistView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        artistView.setTextColor(0xAAFFFFFF);
        artistView.setSingleLine(true);
        artistView.setEllipsize(TextUtils.TruncateAt.END);
        titleBox.addView(artistView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        topRow.addView(titleBox, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // Actions Row: [A] [≡-] [✕]
        actionsBox = new LinearLayout(context);
        actionsBox.setOrientation(LinearLayout.HORIZONTAL);
        actionsBox.setGravity(Gravity.CENTER_VERTICAL);

        // Language / Mode button [A]
        modeButton = new TextView(context);
        modeButton.setText("A");
        modeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        modeButton.setTypeface(AndroidUtilities.bold());
        modeButton.setTextColor(0xFFFFFFFF);
        modeButton.setGravity(Gravity.CENTER);
        modeButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(10), AndroidUtilities.dp(5));
        GradientDrawable modeBg = new GradientDrawable();
        modeBg.setColor(0x28FFFFFF);
        modeBg.setCornerRadius(AndroidUtilities.dp(14));
        modeButton.setBackground(modeBg);
        modeButton.setContentDescription(MiogramLocale.get("Переклад тексту", "Перевод текста", "Translation"));
        modeButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            displayMode = (displayMode + 1) % 3;
            updateModeButtonText();
            adapter.notifyDataSetChanged();
            showToastPill(getModeToastText());
        });
        actionsBox.addView(modeButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        // Source button [≡-] (filter_setup)
        sourceButton = new ImageView(context);
        sourceButton.setImageResource(R.drawable.filter_setup);
        sourceButton.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        sourceButton.setBackground(Theme.createSelectorDrawable(0x2BFFFFFF, 1, AndroidUtilities.dp(16)));
        sourceButton.setScaleType(ImageView.ScaleType.CENTER);
        sourceButton.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        sourceButton.setContentDescription(MiogramLocale.get("Джерело тексту", "Источник текста", "Lyrics Source"));
        sourceButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showSourceSelectDialog();
        });
        actionsBox.addView(sourceButton, LayoutHelper.createLinear(32, 32, 0, 0, 8, 0));

        // Close button [✕]
        closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.msg_close);
        closeButton.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        closeButton.setBackground(Theme.createSelectorDrawable(0x2BFFFFFF, 1, AndroidUtilities.dp(16)));
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        closeButton.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        closeButton.setContentDescription(LocaleController.getString(R.string.Close));
        closeButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (onCloseClickListener != null) {
                onCloseClickListener.run();
            }
        });
        actionsBox.addView(closeButton, LayoutHelper.createLinear(32, 32));

        topRow.addView(actionsBox, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        headerLayout.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Waveform visualizer dots
        waveformDotsView = new WaveformDotsView(context);
        headerLayout.addView(waveformDotsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 12, 0, 8, 0, 0));

        addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // 2. RecyclerView with lyrics
        this.layoutManager = new LinearLayoutManager(context);
        this.adapter = new LyricsAdapter();

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, AndroidUtilities.dp(78), 0, AndroidUtilities.dp(160));
        recyclerView.setAdapter(adapter);

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
        addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 3. Compact Empty / Error Container
        emptyContainer = new LinearLayout(context);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(Gravity.CENTER);
        emptyContainer.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(80), AndroidUtilities.dp(28), AndroidUtilities.dp(140));
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
        aiActionButton.setText(MiogramLocale.get("✨ Розпізнати слова через ШІ зі звуку", "✨ Распознать слова через ИИ со звука", "✨ Transcribe lyrics with AI"));
        aiActionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        aiActionButton.setTypeface(AndroidUtilities.bold());
        aiActionButton.setTextColor(0xFFFFFFFF);
        aiActionButton.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        GradientDrawable aiBtnBg = new GradientDrawable();
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider);
        if (accent == 0) accent = 0xFF3390EC;
        aiBtnBg.setColor(accent);
        aiBtnBg.setCornerRadius(AndroidUtilities.dp(20));
        aiActionButton.setBackground(aiBtnBg);
        aiActionButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            triggerAiTranscription();
        });
        emptyButtonsRow.addView(aiActionButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        changeSourceButton = new TextView(context);
        changeSourceButton.setText(MiogramLocale.get("⚙️ Змінити сервіс пошуку", "⚙️ Сменить сервис поиска", "⚙️ Change search service"));
        changeSourceButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        changeSourceButton.setTextColor(0xCCFFFFFF);
        changeSourceButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable csBg = new GradientDrawable();
        csBg.setColor(0x22FFFFFF);
        csBg.setCornerRadius(AndroidUtilities.dp(16));
        changeSourceButton.setBackground(csBg);
        changeSourceButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            showSourceSelectDialog();
        });
        emptyButtonsRow.addView(changeSourceButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        emptyContainer.addView(emptyButtonsRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        addView(emptyContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        // 4. Floating Pill Toast [Имя сервиса ✓]
        toastPillView = new TextView(context);
        toastPillView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        toastPillView.setTypeface(AndroidUtilities.bold());
        toastPillView.setTextColor(0xFFFFFFFF);
        toastPillView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xDC1E2833);
        pillBg.setCornerRadius(AndroidUtilities.dp(20));
        pillBg.setStroke(AndroidUtilities.dp(1), 0x33FFFFFF);
        toastPillView.setBackground(pillBg);
        toastPillView.setVisibility(View.GONE);
        toastPillView.setAlpha(0f);
        addView(toastPillView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
    }

    public void setOnCloseClickListener(Runnable listener) {
        this.onCloseClickListener = listener;
    }

    public void setOnActiveLineChangeListener(OnActiveLineChangeListener listener) {
        this.onActiveLineChangeListener = listener;
    }

    public void setSong(MessageObject messageObject) {
        if (messageObject == null) return;
        this.currentMessageObject = messageObject;

        titleView.setText(messageObject.getMusicTitle());
        artistView.setText(messageObject.getMusicAuthor());

        loadLyrics(messageObject, currentSourceId);
    }

    public void updateTime(long currentMs) {
        if (currentSong == null || currentSong.lines.isEmpty()) return;

        waveformDotsView.setPlaying(!MediaController.getInstance().isMessagePaused());

        if (isUserScrolling && SystemClock.elapsedRealtime() - lastUserScrollTime > 3500) {
            isUserScrolling = false;
        }

        int newIndex = currentSong.findLineIndex(currentMs);
        if (newIndex != activePosition && newIndex >= 0) {
            int old = activePosition;
            activePosition = newIndex;

            if (old >= 0) adapter.notifyItemChanged(old);
            adapter.notifyItemChanged(activePosition);

            if (onActiveLineChangeListener != null && activePosition < currentSong.lines.size()) {
                MiogramLrcModel.LrcLine line = currentSong.lines.get(activePosition);
                onActiveLineChangeListener.onActiveLineChanged(line.text, line.translation, activePosition);
            }

            if (!isUserScrolling) {
                smoothCenterTo(activePosition);
            }
        }
    }

    private void smoothCenterTo(int position) {
        if (position < 0 || position >= adapter.getItemCount()) return;

        LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 160f / displayMetrics.densityDpi;
            }
        };
        scroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(scroller);
    }

    private void showSourceSelectDialog() {
        MiogramSourceSelectAlert alert = new MiogramSourceSelectAlert(getContext(), currentSourceId, resourcesProvider, (sourceId, sourceTitle) -> {
            this.currentSourceId = sourceId;
            showToastPill(sourceTitle + " ✓");
            if (currentMessageObject != null) {
                loadLyrics(currentMessageObject, sourceId);
            }
        });
        alert.show();
    }

    private void showToastPill(String text) {
        if (hideToastRunnable != null) {
            removeCallbacks(hideToastRunnable);
        }
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
        postDelayed(hideToastRunnable, 1400);
    }

    private void updateModeButtonText() {
        if (displayMode == MODE_BILINGUAL) {
            modeButton.setText("A / UA");
        } else if (displayMode == MODE_ORIGINAL) {
            modeButton.setText("A");
        } else {
            modeButton.setText("UA");
        }
    }

    private String getModeToastText() {
        if (displayMode == MODE_BILINGUAL) {
            return MiogramLocale.get("Двомовний режим ✓", "Двуязычный режим ✓", "Bilingual mode ✓");
        } else if (displayMode == MODE_ORIGINAL) {
            return MiogramLocale.get("Лише оригінал ✓", "Только оригинал ✓", "Original only ✓");
        } else {
            return MiogramLocale.get("Лише переклад ✓", "Только перевод ✓", "Translation only ✓");
        }
    }

    private void loadLyrics(MessageObject messageObject, int sourceId) {
        showLoading(true);
        activePosition = -1;

        MiogramLyricsEngine.getInstance().fetchLyrics(messageObject, sourceId, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                showLoading(false);
                currentSong = song;
                adapter.setLines(song.lines);
                emptyContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                long currentMs = messageObject.audioProgressMs > 0
                        ? messageObject.audioProgressMs
                        : (long) (messageObject.audioProgress * (messageObject.audioPlayerDuration > 0 ? messageObject.audioPlayerDuration * 1000L : messageObject.getDuration() * 1000L));
                updateTime(currentMs);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                currentSong = null;
                adapter.setLines(new ArrayList<>());
                recyclerView.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                emptyButtonsRow.setVisibility(View.VISIBLE);
                emptyTitle.setText(MiogramLocale.get("Слова пісні не знайдено", "Слова песни не найдены", "Lyrics not found"));
                emptySubtitle.setText(MiogramLocale.get("Ви можете обрати інше джерело або розпізнати слова через ШІ", "Вы можете выбрать другой источник или распознать слова через ИИ", "You can switch source or transcribe using AI"));

                if (onActiveLineChangeListener != null) {
                    onActiveLineChangeListener.onActiveLineChanged(null, null, -1);
                }
            }
        });
    }

    public void triggerAiTranscription() {
        if (currentMessageObject == null) return;
        showLoading(true);
        emptyTitle.setText(MiogramLocale.get("ШІ розпізнає трек...", "ИИ распознает трек...", "AI is transcribing..."));
        emptySubtitle.setText(MiogramLocale.get("Синхронізація слів з ритмом музики", "Синхронизация слов с ритмом музыки", "Synchronizing lyrics with music rhythm"));
        emptyButtonsRow.setVisibility(View.GONE);

        MiogramLyricsEngine.getInstance().transcribeAudioWithAi(currentMessageObject, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                showLoading(false);
                currentSong = song;
                adapter.setLines(song.lines);
                emptyContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                showToastPill(MiogramLocale.get("ШІ зі звуку ✓", "ИИ со слуха ✓", "AI by ear ✓"));

                long currentMs = currentMessageObject.audioProgressMs > 0
                        ? currentMessageObject.audioProgressMs
                        : (long) (currentMessageObject.audioProgress * (currentMessageObject.audioPlayerDuration > 0 ? currentMessageObject.audioPlayerDuration * 1000L : currentMessageObject.getDuration() * 1000L));
                updateTime(currentMs);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                emptyContainer.setVisibility(View.VISIBLE);
                emptyButtonsRow.setVisibility(View.VISIBLE);
                emptyTitle.setText(MiogramLocale.get("Помилка розпізнавання", "Ошибка распознавания", "Transcription error"));
                emptySubtitle.setText(message);
            }
        });
    }

    private void showLoading(boolean loading) {
        if (loading) {
            emptyContainer.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            emptyButtonsRow.setVisibility(View.GONE);
            emptyTitle.setText(MiogramLocale.get("Завантаження слів...", "Загрузка слов...", "Loading lyrics..."));
            emptySubtitle.setText(MiogramLocale.get("Пошук у джерелі: ", "Поиск в источнике: ", "Searching source: ") + MiogramSourceSelectAlert.getSourceName(currentSourceId));
            recyclerView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /* =========================================================================
     * WAVEFORM DOTS VISUALIZER
     * ========================================================================= */

    private static class WaveformDotsView extends View {
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean isPlaying = false;
        private float phase = 0f;

        public WaveformDotsView(Context context) {
            super(context);
            dotPaint.setColor(0x66FFFFFF);
        }

        public void setPlaying(boolean playing) {
            if (this.isPlaying != playing) {
                this.isPlaying = playing;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            int dotCount = 28;
            float step = (float) width / (dotCount + 1);
            float centerY = height / 2.0f;

            if (isPlaying) {
                phase += 0.08f;
            }

            for (int i = 0; i < dotCount; i++) {
                float x = step * (i + 1);
                float wave = (float) Math.sin(i * 0.35f + phase);
                float radius = AndroidUtilities.dp(isPlaying ? (2.0f + 1.2f * wave) : 2.0f);
                int alpha = isPlaying ? (int) (120 + 80 * wave) : 80;
                dotPaint.setColor(ColorUtils.setAlphaComponent(0xFFFFFFFF, Math.max(40, Math.min(255, alpha))));
                canvas.drawCircle(x, centerY, radius, dotPaint);
            }

            if (isPlaying) {
                postInvalidateOnAnimation();
            }
        }
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
            underline.setBackgroundColor(0xCCFFFFFF);
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

            if (displayMode == MODE_BILINGUAL) {
                holder.mainText.setText(line.text);
                if (line.hasTranslation()) {
                    holder.transText.setVisibility(View.VISIBLE);
                    holder.transText.setText(line.translation);
                } else {
                    holder.transText.setVisibility(View.GONE);
                }
            } else if (displayMode == MODE_ORIGINAL) {
                holder.mainText.setText(line.text);
                holder.transText.setVisibility(View.GONE);
            } else {
                holder.mainText.setText(line.hasTranslation() ? line.translation : line.text);
                holder.transText.setVisibility(View.GONE);
            }

            if (isActive) {
                holder.mainText.setTextColor(0xFFFFFFFF);
                holder.mainText.setAlpha(1.0f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                holder.underline.setVisibility(View.VISIBLE);
                holder.transText.setAlpha(0.9f);
                holder.itemView.setScaleX(1.02f);
                holder.itemView.setScaleY(1.02f);
            } else {
                holder.mainText.setTextColor(0xFFFFFFFF);
                holder.mainText.setAlpha(0.38f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                holder.underline.setVisibility(View.GONE);
                holder.transText.setAlpha(0.28f);
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (currentMessageObject != null) {
                    MiogramHaptic.select(v);
                    float dur = currentMessageObject.audioPlayerDuration > 0
                            ? (float) currentMessageObject.audioPlayerDuration
                            : (float) currentMessageObject.getDuration();
                    if (dur > 0) {
                        float progress = (float) line.timeMs / (dur * 1000f);
                        progress = Math.max(0.0f, Math.min(1.0f, progress));
                        MediaController.getInstance().seekToProgress(currentMessageObject, progress);
                    }
                    activePosition = holder.getAdapterPosition();
                    notifyDataSetChanged();
                    smoothCenterTo(activePosition);
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

        public LyricsViewHolder(@NonNull View itemView, TextView mainText, View underline, TextView transText) {
            super(itemView);
            this.mainText = mainText;
            this.underline = underline;
            this.transText = transText;
        }
    }
}
