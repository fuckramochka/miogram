package app.miogram.bridge.multichat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import app.miogram.bridge.MiogramLocale;

/**
 * Floating Picture-in-Picture Mini-Chat Service:
 * Renders a draggable compact floating chat window on top of any other app.
 */
public class MiogramFloatingChatService extends Service {

    public static final String EXTRA_DIALOG_ID = "dialog_id";

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private long dialogId;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    public static void startFloatingChat(Context context, long dialogId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Toast.makeText(context,
                    MiogramLocale.get("Надайте дозвіл на відображення поверх інших додатків", "Предоставьте разрешение на отображение поверх других приложений", "Please grant overlay permission for floating chat"),
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }

        Intent intent = new Intent(context, MiogramFloatingChatService.class);
        intent.putExtra(EXTRA_DIALOG_ID, dialogId);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            dialogId = intent.getLongExtra(EXTRA_DIALOG_ID, 0);
        }
        createFloatingWindow();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createFloatingWindow() {
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception ignored) {}
            floatingView = null;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int width = AndroidUtilities.dp(280);
        int height = AndroidUtilities.dp(200);

        params = new WindowManager.LayoutParams(
                width,
                height,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 80;
        params.y = 200;

        // Construct Floating View Container
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setShape(GradientDrawable.RECTANGLE);
        rootBg.setCornerRadius(AndroidUtilities.dp(16));
        rootBg.setColor(0xFF1E1630);
        rootBg.setStroke(AndroidUtilities.dp(1.5f), 0xFFFF70A6);
        root.setBackground(rootBg);
        root.setElevation(AndroidUtilities.dp(12));

        // 1. Header Bar (Drag zone)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        header.setBackgroundColor(0xFF281C44);

        TextView title = new TextView(this);
        title.setText(getDialogTitle(dialogId));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Color.WHITE);
        title.setSingleLine(true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // Maximize button
        TextView expandBtn = new TextView(this);
        expandBtn.setText("□");
        expandBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        expandBtn.setTextColor(Color.parseColor("#00F0FF"));
        expandBtn.setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6), 0);
        expandBtn.setOnClickListener(v -> {
            Intent appIntent = new Intent(this, LaunchActivity.class);
            appIntent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
            if (dialogId > 0) {
                appIntent.putExtra("userId", dialogId);
            } else if (dialogId < 0) {
                appIntent.putExtra("chatId", -dialogId);
            }
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(appIntent);
            stopSelf();
        });
        header.addView(expandBtn);

        // Close button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        closeBtn.setTextColor(Color.parseColor("#FF2A93"));
        closeBtn.setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(4), 0);
        closeBtn.setOnClickListener(v -> stopSelf());
        header.addView(closeBtn);

        // Dragging gesture on header
        header.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    try {
                        windowManager.updateViewLayout(floatingView, params);
                    } catch (Exception ignored) {}
                    return true;
            }
            return false;
        });
        root.addView(header);

        // 2. Mini Content Area
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView msgPreview = new TextView(this);
        msgPreview.setText(MiogramLocale.get("Діалог активний у фоні ໒꒱", "Диалог активен в фоне ໒꒱", "Chat active in background ໒꒱"));
        msgPreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        msgPreview.setTextColor(Color.parseColor("#DCD4F5"));
        msgPreview.setGravity(Gravity.CENTER);
        body.addView(msgPreview, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView quickOpen = new TextView(this);
        quickOpen.setText(MiogramLocale.get("Відкрити в додатку", "Открыть в приложении", "Open in App"));
        quickOpen.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        quickOpen.setTypeface(AndroidUtilities.bold());
        quickOpen.setTextColor(Color.WHITE);
        quickOpen.setGravity(Gravity.CENTER);
        GradientDrawable qBg = new GradientDrawable();
        qBg.setCornerRadius(AndroidUtilities.dp(10));
        qBg.setColor(Color.parseColor("#FF70A6"));
        quickOpen.setBackground(qBg);
        quickOpen.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        quickOpen.setOnClickListener(v -> {
            Intent appIntent = new Intent(this, LaunchActivity.class);
            appIntent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
            if (dialogId > 0) {
                appIntent.putExtra("userId", dialogId);
            } else if (dialogId < 0) {
                appIntent.putExtra("chatId", -dialogId);
            }
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(appIntent);
            stopSelf();
        });
        body.addView(quickOpen);

        root.addView(body, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        floatingView = root;
        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDialogTitle(long dialogId) {
        if (dialogId == 0) return "Miogram Chat ໒꒱";
        int currentAccount = UserConfig.selectedAccount;
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                return org.telegram.messenger.UserObject.getUserName(user);
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                return chat.title;
            }
        }
        return "Chat ໒꒱";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception ignored) {}
            floatingView = null;
        }
    }
}
