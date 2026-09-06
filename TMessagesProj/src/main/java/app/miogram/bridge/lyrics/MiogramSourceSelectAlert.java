package app.miogram.bridge.lyrics;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadioButton;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Bottom Sheet dialog for selecting lyrics provider/engine:
 * - Автоматично / Автоматически
 * - Зібране сервером / Собранное сервером
 * - LRCLib
 * - NetEase
 * - Яндекс Музика / Яндекс Музыка
 * - Genius
 * - YouTube (опис ролика)
 * - ШІ зі звуку / ИИ со слуха
 */
public class MiogramSourceSelectAlert extends BottomSheet {

    public interface OnSourceSelectedListener {
        void onSourceSelected(int sourceId, String sourceTitle);
    }

    private int selectedSourceId;
    private final OnSourceSelectedListener listener;
    private final RadioButton[] radioButtons;

    public MiogramSourceSelectAlert(Context context, int currentSourceId, Theme.ResourcesProvider resourcesProvider, OnSourceSelectedListener listener) {
        super(context, false, resourcesProvider);
        this.selectedSourceId = currentSourceId;
        this.listener = listener;

        final int[] sources = new int[]{
                MiogramLyricsEngine.SOURCE_AUTO,
                MiogramLyricsEngine.SOURCE_SERVER,
                MiogramLyricsEngine.SOURCE_LRCLIB,
                MiogramLyricsEngine.SOURCE_NETEASE,
                MiogramLyricsEngine.SOURCE_YANDEX,
                MiogramLyricsEngine.SOURCE_GENIUS,
                MiogramLyricsEngine.SOURCE_YOUTUBE,
                MiogramLyricsEngine.SOURCE_AI
        };

        final String[] titles = new String[]{
                MiogramLocale.get("Автоматично", "Автоматически", "Automatic"),
                MiogramLocale.get("Зібране сервером", "Собранное сервером", "Server Cached"),
                "LRCLib",
                "NetEase",
                MiogramLocale.get("Яндекс Музика", "Яндекс Музыка", "Yandex Music"),
                "Genius",
                MiogramLocale.get("YouTube (опис ролика)", "YouTube (описание ролика)", "YouTube (Description)"),
                MiogramLocale.get("ШІ зі звуку", "ИИ со слуха", "AI by ear")
        };

        radioButtons = new RadioButton[sources.length];

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        // Drag handle pill
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_sheet_scrollUp), 100));
        handleBg.setCornerRadius(AndroidUtilities.dp(2.5f));
        handle.setBackground(handleBg);
        root.addView(handle, LayoutHelper.createLinear(36, 5, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(MiogramLocale.get("Джерело тексту", "Источник текста", "Lyrics Source"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        // Subtitle: "Зараз: ..."
        String curName = getSourceName(currentSourceId);
        TextView subtitleView = new TextView(context);
        subtitleView.setText(MiogramLocale.get("Зараз: ", "Сейчас: ", "Current: ") + curName);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        root.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Scrollable container for options
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout itemsLayout = new LinearLayout(context);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);

        int accentColor = getThemedColor(Theme.key_dialogRadioBackgroundChecked);
        if (accentColor == 0) {
            accentColor = getThemedColor(Theme.key_featuredStickers_addButton);
        }

        for (int i = 0; i < sources.length; i++) {
            final int index = i;
            final int srcId = sources[i];
            final String srcTitle = titles[i];

            FrameLayout row = new FrameLayout(context);
            row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
            row.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12));

            TextView rowText = new TextView(context);
            rowText.setText(srcTitle);
            rowText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            rowText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            row.addView(rowText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 8, 0, 48, 0));

            RadioButton rb = new RadioButton(context);
            rb.setSize(AndroidUtilities.dp(20));
            rb.setColor(getThemedColor(Theme.key_dialogRadioBackground), accentColor);
            rb.setChecked(srcId == selectedSourceId, false);
            radioButtons[i] = rb;
            row.addView(rb, LayoutHelper.createFrame(22, 22, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

            row.setOnClickListener(v -> {
                MiogramHaptic.select(v);
                selectedSourceId = srcId;
                for (int j = 0; j < radioButtons.length; j++) {
                    radioButtons[j].setChecked(j == index, true);
                }
            });

            itemsLayout.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        scrollView.addView(itemsLayout);
        root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Buttons row
        LinearLayout buttonsRow = new LinearLayout(context);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.RIGHT);

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText(MiogramLocale.get("Скасувати", "Отмена", "Cancel"));
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        cancelBtn.setTypeface(AndroidUtilities.bold());
        cancelBtn.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
        cancelBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        cancelBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
        cancelBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            dismiss();
        });
        buttonsRow.addView(cancelBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView doneBtn = new TextView(context);
        doneBtn.setText(MiogramLocale.get("Готово", "Готово", "Done"));
        doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneBtn.setTypeface(AndroidUtilities.bold());
        doneBtn.setTextColor(0xFFFFFFFF);
        doneBtn.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setColor(accentColor);
        doneBg.setCornerRadius(AndroidUtilities.dp(8));
        doneBtn.setBackground(doneBg);
        doneBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            dismiss();
            if (listener != null) {
                listener.onSourceSelected(selectedSourceId, getSourceName(selectedSourceId));
            }
        });
        buttonsRow.addView(doneBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        root.addView(buttonsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(root);
    }

    public static String getSourceName(int sourceId) {
        switch (sourceId) {
            case MiogramLyricsEngine.SOURCE_SERVER:
                return MiogramLocale.get("Зібране сервером", "Собранное сервером", "Server Cached");
            case MiogramLyricsEngine.SOURCE_LRCLIB:
                return "LRCLib";
            case MiogramLyricsEngine.SOURCE_NETEASE:
                return "NetEase";
            case MiogramLyricsEngine.SOURCE_YANDEX:
                return MiogramLocale.get("Яндекс Музика", "Яндекс Музыка", "Yandex Music");
            case MiogramLyricsEngine.SOURCE_GENIUS:
                return "Genius";
            case MiogramLyricsEngine.SOURCE_YOUTUBE:
                return MiogramLocale.get("YouTube (опис)", "YouTube (описание)", "YouTube (Description)");
            case MiogramLyricsEngine.SOURCE_AI:
                return MiogramLocale.get("ШІ зі звуку", "ИИ со слуха", "AI by ear");
            case MiogramLyricsEngine.SOURCE_AUTO:
            default:
                return MiogramLocale.get("Автоматично", "Автоматически", "Automatic");
        }
    }
}
