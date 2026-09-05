package app.miogram.bridge.customui;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * High-fidelity native haptic engine for Miogram Custom UI.
 * 1-to-1 matching Custom Profile (cpb.Haptic.java & cpb.HapticPolicy.java).
 * Delivers crisp, rich, tactile vibration waveforms with physical amplitude control.
 */
public final class MiogramHaptic {

    public static final int MAX_AMPLITUDE = 255;
    public static final long SAME_GAP_MS = 60;
    public static final long TICK_GAP_MS = 28;

    private static long lastAny;
    private static long lastTick;
    private static volatile Vibrator vibrator;

    private static final long[] TAP = {14};
    private static final int[] TAP_A = {190};

    private static final long[] SELECT = {12};
    private static final int[] SELECT_A = {165};

    private static final long[] TICK = {10};
    private static final int[] TICK_A = {140};

    private static final long[] ON = {18};
    private static final int[] ON_A = {225};

    private static final long[] OFF_WORD = {14};
    private static final int[] OFF_A = {160};

    private static final long[] EDGE = {24};
    private static final int[] EDGE_A = {255};

    private static final long[] GRAB = {22};
    private static final int[] GRAB_A = {235};

    private static final long[] RELEASE = {12};
    private static final int[] RELEASE_A = {180};

    private static final long[] ZIP_IN = {8, 20, 8, 19, 9, 18, 9, 17, 10, 16, 12};
    private static final int[] ZIP_IN_A = {60, 0, 95, 0, 130, 0, 165, 0, 205, 0, 240};

    private static final long[] ZIP_OUT = {12, 16, 10, 17, 9, 18, 9, 19, 8, 20, 8};
    private static final int[] ZIP_OUT_A = {240, 0, 205, 0, 165, 0, 130, 0, 95, 0, 60};

    private static final long[] SUCCESS = {14, 55, 22};
    private static final int[] SUCCESS_A = {180, 0, 245};

    private static final long[] WARN = {18, 60, 18};
    private static final int[] WARN_A = {210, 0, 210};

    private static final long[] ERROR = {18, 55, 18, 55, 26};
    private static final int[] ERROR_A = {200, 0, 220, 0, 255};

    private static final Map<View, Boolean> ZIPPED = new WeakHashMap<>();

    private MiogramHaptic() {
    }

    public static boolean edgeReached(int val, int oldVal, int min, int max) {
        return (val == min || val == max) && (oldVal != min && oldVal != max);
    }

    public static boolean tickDue(long last, long now) {
        return last <= 0 || now - last >= TICK_GAP_MS || now < last;
    }

    public static boolean tooSoon(long last, long now) {
        return last > 0 && now >= last && now - last < SAME_GAP_MS;
    }

    public static int amplitude(int a) {
        return Math.max(1, Math.min(MAX_AMPLITUDE, a));
    }

    public static void tap(View view) {
        play(view, TAP, TAP_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void select(View view) {
        play(view, SELECT, SELECT_A, HapticFeedbackConstants.CLOCK_TICK, false);
    }

    public static void tick(View view) {
        long now = SystemClock.uptimeMillis();
        if (tickDue(lastTick, now)) {
            lastTick = now;
            play(view, TICK, TICK_A, HapticFeedbackConstants.CLOCK_TICK, true);
        }
    }

    public static void toggle(View view, boolean on) {
        play(view, on ? ON : OFF_WORD, on ? ON_A : OFF_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void edge(View view) {
        play(view, EDGE, EDGE_A, HapticFeedbackConstants.LONG_PRESS, false);
    }

    public static void grab(View view) {
        play(view, GRAB, GRAB_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void release(View view) {
        play(view, RELEASE, RELEASE_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void zipIn(View view) {
        play(view, ZIP_IN, ZIP_IN_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void zipOut(View view) {
        play(view, ZIP_OUT, ZIP_OUT_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void zipper(View view) {
        if (view == null) return;
        synchronized (ZIPPED) {
            if (ZIPPED.put(view, Boolean.TRUE) != null) {
                return;
            }
            if (view.isAttachedToWindow()) {
                zipIn(view);
            }
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    zipIn(v);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    zipOut(v);
                }
            });
        }
    }

    public static void success(View view) {
        play(view, SUCCESS, SUCCESS_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void warn(View view) {
        play(view, WARN, WARN_A, HapticFeedbackConstants.KEYBOARD_TAP, false);
    }

    public static void error(View view) {
        play(view, ERROR, ERROR_A, HapticFeedbackConstants.LONG_PRESS, false);
    }

    private static void play(View view, long[] timings, int[] amplitudes, int fallbackConstant, boolean isTick) {
        if (!MiogramCustomUiPrefs.isHapticEnabled()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (!isTick) {
            if (tooSoon(lastAny, now)) {
                return;
            }
            lastAny = now;
        }
        if (shake(timings, amplitudes)) {
            return;
        }
        if (view != null) {
            try {
                view.performHapticFeedback(fallbackConstant);
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean shake(long[] timings, int[] amplitudes) {
        Vibrator v = getVibrator();
        if (v != null && v.hasVibrator()) {
            try {
                int len = timings.length;
                long[] t = new long[len];
                int[] a = new int[len];
                for (int i = 0; i < len; i++) {
                    t[i] = timings[i];
                    if (i < amplitudes.length && amplitudes[i] == 0) {
                        a[i] = 0;
                    } else {
                        a[i] = amplitude(i < amplitudes.length ? amplitudes[i] : 180);
                    }
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    VibrationEffect effect;
                    if (len == 1) {
                        effect = VibrationEffect.createOneShot(t[0], a[0]);
                    } else {
                        effect = VibrationEffect.createWaveform(t, a, -1);
                    }
                    v.vibrate(effect);
                    return true;
                } else {
                    long total = 0;
                    for (int i = 0; i < len; i++) {
                        total += t[i];
                    }
                    v.vibrate(total);
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static Vibrator getVibrator() {
        if (vibrator == null) {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                try {
                    vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                } catch (Throwable ignored) {
                }
            }
        }
        return vibrator;
    }
}
