package app.miogram.bridge.performance;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

/**
 * Miogram 90 / 120 FPS Display & Refresh Rate Controller:
 * - Unlocks and locks high refresh rates (60Hz, 90Hz, 120Hz, Max)
 * - Directly manages Window display mode IDs and preferred refresh rate on Android 6+ / 11+
 * - Enhances UI smoothness and reduces render latency
 */
public class MiogramFpsController {

    private static final String PREF_NAME = "miogram_performance_prefs";
    private static final String KEY_REFRESH_RATE_MODE = "refresh_rate_mode";

    public static final int REFRESH_MODE_AUTO = 0;
    public static final int REFRESH_MODE_90HZ = 1;
    public static final int REFRESH_MODE_120HZ = 2;
    public static final int REFRESH_MODE_144HZ = 3;
    public static final int REFRESH_MODE_MAX = 4;

    private static SharedPreferences getPrefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getRefreshRateMode() {
        return getPrefs().getInt(KEY_REFRESH_RATE_MODE, REFRESH_MODE_MAX);
    }

    public static void setRefreshRateMode(int mode) {
        getPrefs().edit().putInt(KEY_REFRESH_RATE_MODE, mode).apply();
    }

    public static String getRefreshRateTitle(int mode) {
        switch (mode) {
            case REFRESH_MODE_90HZ:
                return "90 Hz";
            case REFRESH_MODE_120HZ:
                return "120 Hz (Ultra Smooth)";
            case REFRESH_MODE_144HZ:
                return "144 Hz (Pro Gaming)";
            case REFRESH_MODE_MAX:
                return app.miogram.bridge.MiogramLocale.get("Максимальна частота екрану", "Максимальная частота экрана", "Display Maximum (Auto Boost)");
            case REFRESH_MODE_AUTO:
            default:
                return app.miogram.bridge.MiogramLocale.get("Системна за замовчуванням (60-120Hz)", "Системная по умолчанию (60-120Hz)", "System Default");
        }
    }

    public static void applyToWindow(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        applyToWindow(activity.getWindow());
    }

    public static void applyToWindow(Window window) {
        if (window == null) return;
        try {
            Context ctx = window.getContext();
            android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isPowerSaveMode()) {
                return; // Respect Android battery saver mode
            }

            int mode = getRefreshRateMode();
            float maxSupported = Math.max(60f, AndroidUtilities.screenMaxRefreshRate);
            float targetFps = 0f;
            switch (mode) {
                case REFRESH_MODE_90HZ:
                    targetFps = Math.min(90f, maxSupported);
                    break;
                case REFRESH_MODE_120HZ:
                    targetFps = Math.min(120f, maxSupported);
                    break;
                case REFRESH_MODE_144HZ:
                    targetFps = Math.min(144f, maxSupported);
                    break;
                case REFRESH_MODE_MAX:
                case REFRESH_MODE_AUTO:
                default:
                    targetFps = maxSupported;
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.preferredRefreshRate = targetFps;

                // Match exact display mode if available on Android 6.0+
                Display display = window.getContext().getDisplay();
                if (display != null) {
                    Display.Mode[] supportedModes = display.getSupportedModes();
                    Display.Mode bestMode = null;
                    float closestDiff = Float.MAX_VALUE;
                    for (Display.Mode m : supportedModes) {
                        float diff = Math.abs(m.getRefreshRate() - targetFps);
                        if (diff < closestDiff && diff <= 1.0f) {
                            closestDiff = diff;
                            bestMode = m;
                        }
                    }
                    if (bestMode != null) {
                        params.preferredDisplayModeId = bestMode.getModeId();
                    }
                }
                window.setAttributes(params);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AndroidUtilities.setPreferredMaxRefreshRate(window, targetFps);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
