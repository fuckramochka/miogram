package app.miogram.bridge.badge;

import android.app.Activity;

import org.telegram.ui.ActionBar.AlertDialog;

import app.miogram.bridge.MiogramLocale;

/**
 * Privacy-friendly community presence opt-in dialog.
 * Transparently requests community badge synchronization without intrusive telemetry.
 */
public class MiogramSyncDialog {

    public static void checkAndShow(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (MiogramSupabaseBridge.isOptInCompleted(activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(MiogramLocale.get("Синхронізація Miogram ໒꒱", "Синхронизация Miogram ໒꒱", "Miogram Community Sync ໒꒱"));
        builder.setMessage(MiogramLocale.get(
                "Бажаєте увімкнути відображення вашого бейджика для інших користувачів Miogram? Ваш ID буде додано до хмарного каталогу, щоб спільнота бачила ваш статус та унікальні відзнаки.",
                "Желаете включить отображение вашего бейджика для других пользователей Miogram? Ваш ID будет добавлен в облачный каталог, чтобы сообщество видело ваш статус и уникальные знаки отличия.",
                "Would you like to enable your badge visibility for other Miogram users? Your ID will be synced with the cloud so the community can see your active badge and style."
        ));

        builder.setPositiveButton(MiogramLocale.get("Увімкнути (Рекомендовано)", "Включить (Рекомендуется)", "Enable (Recommended)"), (dialog, which) -> {
            MiogramSupabaseBridge.setSyncEnabled(activity, true);
        });

        builder.setNegativeButton(MiogramLocale.get("Пізніше", "Позже", "Later"), (dialog, which) -> {
            MiogramSupabaseBridge.setSyncEnabled(activity, false);
        });

        builder.show();
    }
}
