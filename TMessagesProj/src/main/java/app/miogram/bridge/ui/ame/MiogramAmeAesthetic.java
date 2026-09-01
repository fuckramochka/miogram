package app.miogram.bridge.ui.ame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import app.miogram.bridge.ui.MiogramVisualsPrefs;

/**
 * Needy Streamer Overload / Ame-chan (KAngel) Omnipresent Visual Engine:
 * - Vaporwave / Y2K cyber-pastel aesthetic everywhere across Miogram
 * - Neon Pink #FF70A6, Cyan #70D6FF, Lavender #E0AAFF, Dark Velvet #120F1D
 * - Angelic halo rings, pixel hearts, †昇天† badges and gradient bubble shaders
 */
public class MiogramAmeAesthetic {

    public static final int COLOR_AME_PINK = 0xFFFF70A6;
    public static final int COLOR_AME_CYAN = 0xFF70D6FF;
    public static final int COLOR_AME_LAVENDER = 0xFFE0AAFF;
    public static final int COLOR_AME_PURPLE = 0xFF9D4EDD;
    public static final int COLOR_AME_DARK = 0xFF120F1D;
    public static final int COLOR_AME_CARD = 0x26FF70A6;
    public static final int COLOR_AME_GOLD = 0xFFFFD166;

    private static final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    static {
        haloPaint.setStyle(Paint.Style.STROKE);
        haloPaint.setStrokeWidth(AndroidUtilities.dp(2));

        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setColor(COLOR_AME_PINK);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(AndroidUtilities.dp(11));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(AndroidUtilities.bold());
    }

    public static boolean isAmeEnabled(Context context) {
        Context ctx = context != null ? context : ApplicationLoader.applicationContext;
        return MiogramVisualsPrefs.loadBool(ctx, "ame_vibe_enabled", true);
    }

    public static View createAmeStatusPill(Context context) {
        if (!isAmeEnabled(context)) return null;

        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), COLOR_AME_CARD));
        pill.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));

        TextView heart = new TextView(context);
        heart.setText("💖 †昇天† ");
        heart.setTextSize(11.5f);
        heart.setTextColor(COLOR_AME_PINK);
        heart.setTypeface(AndroidUtilities.bold());
        pill.addView(heart);

        TextView label = new TextView(context);
        label.setText("INTERNET YAMERO ໒꒱");
        label.setTextSize(11f);
        label.setTextColor(COLOR_AME_CYAN);
        label.setTypeface(AndroidUtilities.bold());
        pill.addView(label);

        return pill;
    }

    public static View createAmeHeaderBadge(Context context, String title) {
        if (!isAmeEnabled(context)) return null;

        LinearLayout badge = new LinearLayout(context);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), 0x3370D6FF));
        badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));

        TextView txt = new TextView(context);
        txt.setText("✨ " + (title != null ? title : "Ame") + " ໒꒱");
        txt.setTextSize(11f);
        txt.setTextColor(COLOR_AME_LAVENDER);
        txt.setTypeface(AndroidUtilities.bold());
        badge.addView(txt);

        return badge;
    }

    public static void drawAmeAvatarHalo(Canvas canvas, float cx, float cy, float radius) {
        if (canvas == null || radius <= 0) return;
        LinearGradient gradient = new LinearGradient(
                cx - radius, cy - radius, cx + radius, cy + radius,
                new int[]{COLOR_AME_PINK, COLOR_AME_CYAN, COLOR_AME_LAVENDER, COLOR_AME_PINK},
                null,
                Shader.TileMode.CLAMP
        );
        haloPaint.setShader(gradient);
        canvas.drawCircle(cx, cy, radius + AndroidUtilities.dp(2), haloPaint);
    }

    public static void drawAmeUnreadBadge(Canvas canvas, float left, float top, float right, float bottom, int count) {
        if (canvas == null) return;
        rectF.set(left, top, right, bottom);
        float radius = rectF.height() / 2f;
        badgePaint.setColor(COLOR_AME_PINK);
        canvas.drawRoundRect(rectF, radius, radius, badgePaint);

        String text = count > 99 ? "99+" : String.valueOf(count);
        float textY = rectF.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(text, rectF.centerX(), textY, textPaint);
    }

    public static GradientDrawable getAmeBubbleOutGradient() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF241635, 0xFF3D1635, 0xFF1B1635}
        );
        gd.setCornerRadius(AndroidUtilities.dp(16));
        gd.setStroke(AndroidUtilities.dp(1.2f), COLOR_AME_PINK);
        return gd;
    }

    public static GradientDrawable getAmeBubbleInGradient() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF12142B, 0xFF14223A, 0xFF121B2B}
        );
        gd.setCornerRadius(AndroidUtilities.dp(16));
        gd.setStroke(AndroidUtilities.dp(1.2f), COLOR_AME_CYAN);
        return gd;
    }
}
