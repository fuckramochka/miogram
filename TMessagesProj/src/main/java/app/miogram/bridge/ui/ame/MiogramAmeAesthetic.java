package app.miogram.bridge.ui.ame;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.ui.MiogramVisualsPrefs;

/**
 * Needy Streamer Overload / Ame-chan (KAngel) Cyber-Pastel Aesthetic Engine:
 * - Vaporwave / Y2K glitch-pastel color palette (Cyan #70D6FF & Neon Pink #FF70A6)
 * - Custom †昇天† (Ascension) status pill badges
 * - Floating pixel-heart decorations for chat and dialog headers
 */
public class MiogramAmeAesthetic {

    public static final int COLOR_AME_PINK = 0xFFFF70A6;
    public static final int COLOR_AME_CYAN = 0xFF70D6FF;
    public static final int COLOR_AME_LAVENDER = 0xFFE0AAFF;
    public static final int COLOR_AME_DARK = 0xFF141220;

    public static boolean isAmeEnabled(Context context) {
        Context ctx = context != null ? context : ApplicationLoader.applicationContext;
        return MiogramVisualsPrefs.loadBool(ctx, "ame_vibe_enabled", true);
    }

    public static View createAmeStatusPill(Context context) {
        if (!isAmeEnabled(context)) return null;

        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0x33FF70A6));
        pill.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));

        TextView heart = new TextView(context);
        heart.setText("💖 †昇天† ");
        heart.setTextSize(11);
        heart.setTextColor(COLOR_AME_PINK);
        heart.setTypeface(AndroidUtilities.bold());
        pill.addView(heart);

        TextView label = new TextView(context);
        label.setText("INTERNET YAMERO");
        label.setTextSize(10.5f);
        label.setTextColor(COLOR_AME_CYAN);
        label.setTypeface(AndroidUtilities.bold());
        pill.addView(label);

        return pill;
    }
}
