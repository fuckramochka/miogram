package app.miogram.bridge.perf;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;

/**
 * High-performance engine for Miogram:
 * - 120Hz ProMotion / High Refresh Rate unlocking on Android 11+
 * - Aggressive memory trimming on ComponentCallbacks2 trim events
 * - Background cache recycler
 */
public final class MiogramPerformanceOptimizer {

    private static boolean initialized = false;

    private MiogramPerformanceOptimizer() {}

    public static synchronized void init(android.app.Application app) {
        if (initialized || app == null) return;
        initialized = true;

        app.registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                if (level >= TRIM_MEMORY_MODERATE) {
                    try {
                        ImageLoader.getInstance().clearMemory();
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                }
            }

            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {}

            @Override
            public void onLowMemory() {
                try {
                    ImageLoader.getInstance().clearMemory();
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            }
        });
    }

    /**
     * Checks if Android Battery Saver is active or battery level is <= 20% (when discharging).
     */
    public static boolean isPowerSaveOrLowBattery(android.content.Context context) {
        if (context == null) context = org.telegram.messenger.ApplicationLoader.applicationContext;
        if (context == null) return false;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
            if (pm != null && pm.isPowerSaveMode()) {
                return true;
            }
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                        || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                if (!isCharging && scale > 0) {
                    float batteryPct = (level / (float) scale) * 100f;
                    if (batteryPct <= 20f) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Unlocks maximum available display refresh rate (e.g. 120Hz / 90Hz / 144Hz)
     * matching Apple iOS ProMotion fluidity. Capped to 60Hz if power saver or low battery is active.
     */
    public static void applyProMotionRefreshRate(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowManager.LayoutParams params = window.getAttributes();
                if (isPowerSaveOrLowBattery(activity)) {
                    params.preferredDisplayModeId = 0;
                    params.preferredRefreshRate = 60.0f;
                    window.setAttributes(params);
                    return;
                }
                android.view.Display display = activity.getDisplay();
                if (display != null) {
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    android.view.Display.Mode maxMode = null;
                    float maxRate = 60.0f;
                    for (android.view.Display.Mode mode : modes) {
                        if (mode.getRefreshRate() > maxRate) {
                            maxRate = mode.getRefreshRate();
                            maxMode = mode;
                        }
                    }
                    if (maxMode != null) {
                        params.preferredDisplayModeId = maxMode.getModeId();
                        window.setAttributes(params);
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }
}
