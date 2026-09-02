package app.miogram.bridge.ui.ios;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;

/**
 * Direct 1:1 port of Apple iOS Haptic Feedback Generators:
 * - UISelectionFeedbackGenerator (tab switches, picker wheels)
 * - UIImpactFeedbackGenerator (.light, .medium, .heavy, .rigid, .soft)
 * - UINotificationFeedbackGenerator (.success, .warning, .error)
 */
public final class MiogramIosHapticEngine {

    private MiogramIosHapticEngine() {}

    private static Vibrator getVibrator(Context context) {
        if (context == null) context = ApplicationLoader.applicationContext;
        if (context == null) return null;
        try {
            return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * UISelectionFeedbackGenerator: Selection changed (e.g. Tab switch).
     */
    public static void selectionChanged(View view) {
        if (view != null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            return;
        }
        Vibrator v = getVibrator(null);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(12, 60));
            } else {
                v.vibrate(12);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * UIImpactFeedbackGenerator(style: .light)
     */
    public static void impactLight(View view) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE);
            return;
        }
        selectionChanged(view);
    }

    /**
     * UIImpactFeedbackGenerator(style: .medium)
     */
    public static void impactMedium(View view) {
        if (view != null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }
        Vibrator v = getVibrator(null);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(22, 140));
            } else {
                v.vibrate(22);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * UIImpactFeedbackGenerator(style: .heavy)
     */
    public static void impactHeavy(View view) {
        if (view != null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            return;
        }
        Vibrator v = getVibrator(null);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(35, 255));
            } else {
                v.vibrate(35);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * UINotificationFeedbackGenerator(type: .success)
     */
    public static void notificationSuccess() {
        Vibrator v = getVibrator(null);
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] timings = new long[]{0, 15, 60, 20};
                int[] amplitudes = new int[]{0, 100, 0, 180};
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } else {
                v.vibrate(new long[]{0, 15, 60, 20}, -1);
            }
        } catch (Throwable ignored) {}
    }
}
