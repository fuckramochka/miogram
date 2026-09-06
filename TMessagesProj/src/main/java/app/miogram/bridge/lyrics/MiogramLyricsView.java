package app.miogram.bridge.lyrics;

import android.content.Context;
import android.graphics.Color;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
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
 * Modern Interactive Karaoke Lyrics View for Miogram Player.
 * Features:
 * - Real-time line-by-line sync with playback
 * - Tap-to-seek to any lyric timestamp
 * - Bilingual support: Original / Translation / Both
 * - Smooth physics auto-scroll with manual drag pause
 * - On-demand AI Audio Transcription integration
 */
public class MiogramLyricsView extends FrameLayout {

    public static final int MODE_BILINGUAL = 0;
    public static final int MODE_ORIGINAL = 1;
    public static final int MODE_TRANSLATION = 2;

    private int displayMode = MODE_BILINGUAL;

    private final Theme.ResourcesProvider resourcesProvider;
    private final TextView badgeView;
    private final TextView modeButton;
    private final ImageView aiButton;
    private final ImageView refreshButton;
    private final RecyclerView recyclerView;
    private final LinearLayout emptyContainer;
    private final TextView emptyTitle;
    private final TextView emptySubtitle;
    private final TextView aiActionButton;
    private final ProgressBar progressBar;

    private final LyricsAdapter adapter;
    private final LinearLayoutManager layoutManager;

    private MessageObject currentMessageObject;
    private MiogramLrcModel.LrcSong currentSong;
    private int activePosition = -1;
    private boolean isUserScrolling = false;
    private long lastUserScrollTime = 0L;

    public MiogramLyricsView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setBackgroundColor(0xEE0E1621); // Modern deep blurred midnight backdrop

        // Top Control Bar
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

        badgeView = new TextView(context);
        badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badgeView.setTypeface(AndroidUtilities.bold());
        badgeView.setTextColor(0xFFFFFFFF);
        badgeView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x33FFFFFF);
        badgeBg.setCornerRadius(AndroidUtilities.dp(12));
        badgeView.setBackground(badgeBg);
        topBar.addView(badgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        View spacer = new View(context);
        topBar.addView(spacer, LayoutHelper.createLinear(0, 1, 1.0f));

        // Mode Switcher Button
        modeButton = new TextView(context);
        modeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        modeButton.setTypeface(AndroidUtilities.bold());
        modeButton.setTextColor(0xFFFFFFFF);
        modeButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        GradientDrawable modeBg = new GradientDrawable();
        modeBg.setColor(0x2BFFFFFF);
        modeBg.setCornerRadius(AndroidUtilities.dp(12));
        modeButton.setBackground(modeBg);
        updateModeButtonText();
        modeButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            displayMode = (displayMode + 1) % 3;
            updateModeButtonText();
            adapter.notifyDataSetChanged();
        });
        topBar.addView(modeButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        // AI Transcription Button
        aiButton = new ImageView(context);
        aiButton.setImageResource(R.drawable.baseline_mic_24);
        aiButton.setColorFilter(0xFFFFFFFF);
        aiButton.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        aiButton.setBackground(Theme.createSelectorDrawable(0x2BFFFFFF, 1, AndroidUtilities.dp(16)));
        aiButton.setContentDescription(MiogramLocale.get("ШІ Розшифровка", "ИИ Расшифровка", "AI Transcription"));
        aiButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            triggerAiTranscription();
        });
        topBar.addView(aiButton, LayoutHelper.createLinear(32, 32, 0, 0, 4, 0));

        // Refresh Button
        refreshButton = new ImageView(context);
        refreshButton.setImageResource(R.drawable.outline_header_search);
        refreshButton.setColorFilter(0xFFFFFFFF);
        refreshButton.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        refreshButton.setBackground(Theme.createSelectorDrawable(0x2BFFFFFF, 1, AndroidUtilities.dp(16)));
        refreshButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (currentMessageObject != null) {
                loadLyrics(currentMessageObject, true);
            }
        });
        topBar.addView(refreshButton, LayoutHelper.createLinear(32, 32));

        addView(topBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // RecyclerView with lyrics
        recyclerView = new RecyclerView(context);
        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, AndroidUtilities.dp(64), 0, AndroidUtilities.dp(180));
        adapter = new LyricsAdapter();
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

        // Empty / Error Container
        emptyContainer = new LinearLayout(context);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(Gravity.CENTER);
        emptyContainer.setPadding(AndroidUtilities.dp(32), AndroidUtilities.dp(32), AndroidUtilities.dp(32), AndroidUtilities.dp(32));
        emptyContainer.setVisibility(View.GONE);

        progressBar = new ProgressBar(context);
        emptyContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        emptyTitle = new TextView(context);
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        emptyTitle.setTypeface(AndroidUtilities.bold());
        emptyTitle.setTextColor(0xFFFFFFFF);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyContainer.addView(emptyTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        emptySubtitle = new TextView(context);
        emptySubtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        emptySubtitle.setTextColor(0xAAFFFFFF);
        emptySubtitle.setGravity(Gravity.CENTER);
        emptyContainer.addView(emptySubtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        aiActionButton = new TextView(context);
        aiActionButton.setText(MiogramLocale.get("✨ ШІ-Розшифровка звуку", "✨ ИИ-Расшифровка звука", "✨ AI Audio Transcription"));
        aiActionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        aiActionButton.setTypeface(AndroidUtilities.bold());
        aiActionButton.setTextColor(0xFFFFFFFF);
        aiActionButton.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        GradientDrawable aiBtnBg = new GradientDrawable();
        aiBtnBg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider));
        aiBtnBg.setCornerRadius(AndroidUtilities.dp(20));
        aiActionButton.setBackground(aiBtnBg);
        aiActionButton.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            triggerAiTranscription();
        });
        emptyContainer.addView(aiActionButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        addView(emptyContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
    }

    private void updateModeButtonText() {
        if (displayMode == MODE_BILINGUAL) {
            modeButton.setText(MiogramLocale.get("Двомовний", "Двуязычный", "Bilingual"));
        } else if (displayMode == MODE_ORIGINAL) {
            modeButton.setText(MiogramLocale.get("Оригінал", "Оригинал", "Original"));
        } else {
            modeButton.setText(MiogramLocale.get("Переклад", "Перевод", "Translation"));
        }
    }

    public void setSong(MessageObject messageObject) {
        if (messageObject == null) return;
        this.currentMessageObject = messageObject;
        loadLyrics(messageObject, false);
    }

    public void updateTime(long currentMs) {
        if (currentSong == null || currentSong.lines.isEmpty()) return;

        // Resume auto-scroll after 3.5s of manual touch
        if (isUserScrolling && SystemClock.elapsedRealtime() - lastUserScrollTime > 3500) {
            isUserScrolling = false;
        }

        int newIndex = currentSong.findLineIndex(currentMs);
        if (newIndex != activePosition && newIndex >= 0) {
            int old = activePosition;
            activePosition = newIndex;

            if (old >= 0) adapter.notifyItemChanged(old);
            adapter.notifyItemChanged(activePosition);

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
                return 180f / displayMetrics.densityDpi;
            }
        };
        scroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(scroller);
    }

    private void loadLyrics(MessageObject messageObject, boolean forceRefresh) {
        showLoading(true);
        activePosition = -1;

        MiogramLyricsEngine.getInstance().fetchLyrics(messageObject, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                showLoading(false);
                currentSong = song;
                badgeView.setText(song.source.toUpperCase(java.util.Locale.ROOT));
                badgeView.setVisibility(View.VISIBLE);
                adapter.setLines(song.lines);
                emptyContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                if (currentMessageObject != null) {
                    updateTime(currentMessageObject.audioProgressMs);
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                currentSong = null;
                adapter.setLines(new ArrayList<>());
                recyclerView.setVisibility(View.GONE);
                badgeView.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                emptyTitle.setText(MiogramLocale.get("Слова пісні не знайдено", "Слова песни не найдены", "Lyrics not found"));
                emptySubtitle.setText(MiogramLocale.get("Ви можете розпізнати слова безпосередньо зі звукової доріжки", "Вы можете распознать слова прямо из аудиодорожки", "You can transcribe lyrics from the audio track using AI"));
                aiActionButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void triggerAiTranscription() {
        if (currentMessageObject == null) return;
        showLoading(true);
        emptyTitle.setText(MiogramLocale.get("ШІ розпізнає трек...", "ИИ распознает трек...", "AI is transcribing..."));
        emptySubtitle.setText(MiogramLocale.get("Синхронізація слів з ритмом музики", "Синхронизация слов с ритмом музыки", "Synchronizing lyrics with music rhythm"));
        aiActionButton.setVisibility(View.GONE);

        MiogramLyricsEngine.getInstance().transcribeAudioWithAi(currentMessageObject, new MiogramLyricsEngine.LyricsCallback() {
            @Override
            public void onLyricsLoaded(MiogramLrcModel.LrcSong song) {
                showLoading(false);
                currentSong = song;
                badgeView.setText(song.source.toUpperCase(java.util.Locale.ROOT));
                badgeView.setVisibility(View.VISIBLE);
                adapter.setLines(song.lines);
                emptyContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), MiogramLocale.get("Розшифровано ШІ", "Расшифровано ИИ", "Transcribed with AI"), Toast.LENGTH_SHORT).show();

                if (currentMessageObject != null) {
                    updateTime(currentMessageObject.audioProgressMs);
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        if (loading) {
            emptyContainer.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            emptyTitle.setText(MiogramLocale.get("Завантаження слів...", "Загрузка слов...", "Loading lyrics..."));
            emptySubtitle.setText(MiogramLocale.get("Пошук у базах LRCLib, NetEase та тегах", "Поиск в базах LRCLib, NetEase и тегах", "Searching LRCLib, NetEase and tags"));
            aiActionButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
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
            item.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(14), AndroidUtilities.dp(24), AndroidUtilities.dp(14));

            TextView main = new TextView(parent.getContext());
            main.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            main.setTypeface(AndroidUtilities.bold());
            main.setTextColor(0xFFFFFFFF);
            item.addView(main, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            TextView trans = new TextView(parent.getContext());
            trans.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            trans.setTextColor(0xCCFFFFFF);
            trans.setPadding(0, AndroidUtilities.dp(3), 0, 0);
            item.addView(trans, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            return new LyricsViewHolder(item, main, trans);
        }

        @Override
        public void onBindViewHolder(@NonNull LyricsViewHolder holder, int position) {
            MiogramLrcModel.LrcLine line = items.get(position);
            boolean isActive = (position == activePosition);

            // Display text based on displayMode
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
            } else { // MODE_TRANSLATION
                holder.mainText.setText(line.hasTranslation() ? line.translation : line.text);
                holder.transText.setVisibility(View.GONE);
            }

            // Visual Styling: Active vs Inactive
            if (isActive) {
                int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, resourcesProvider);
                holder.mainText.setTextColor(accent != 0 ? accent : 0xFF40C4FF);
                holder.mainText.setAlpha(1.0f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                holder.transText.setAlpha(0.9f);
                holder.itemView.setScaleX(1.02f);
                holder.itemView.setScaleY(1.02f);
            } else {
                holder.mainText.setTextColor(0xFFFFFFFF);
                holder.mainText.setAlpha(0.38f);
                holder.mainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                holder.transText.setAlpha(0.30f);
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
            }

            // Click to Seek
            holder.itemView.setOnClickListener(v -> {
                if (currentMessageObject != null) {
                    MiogramHaptic.select(v);
                    int durSec = (int) Math.round(currentMessageObject.getDuration());
                    if (durSec > 0) {
                        float progress = (float) line.timeMs / (float) (durSec * 1000L);
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
        final TextView transText;

        public LyricsViewHolder(@NonNull View itemView, TextView mainText, TextView transText) {
            super(itemView);
            this.mainText = mainText;
            this.transText = transText;
        }
    }
}