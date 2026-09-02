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
     * Unlocks maximum available display refresh rate (e.g. 120Hz / 90Hz / 144Hz)
     * matching Apple iOS ProMotion fluidity.
     */
    public static void applyProMotionRefreshRate(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowManager.LayoutParams params = window.getAttributes();
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
