package app.miogram.bridge.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

import org.telegram.messenger.FileLog;

import app.miogram.bridge.MiogramLocale;

/**
 * Handles callback events from PackageInstaller session (Android 12+ unattended background update).
 */
public class MiogramInstallReceiver extends BroadcastReceiver {

    public static final String ACTION_INSTALL_STATUS = ".INSTALL_STATUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        FileLog.d("MiogramInstallReceiver: onReceive with status=" + status);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Android OS requires user interaction/confirmation dialog.
            Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmationIntent != null) {
                try {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmationIntent);
                } catch (Exception e) {
                    FileLog.e("MiogramInstallReceiver: failed to start confirmation activity", e);
                }
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            FileLog.d("MiogramInstallReceiver: installation succeeded unattended!");
            try {
                Toast.makeText(context, MiogramLocale.get("Miogram успішно оновлено!", "Miogram успешно обновлен!", "Miogram successfully updated!"), Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {}
        } else {
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            FileLog.e("MiogramInstallReceiver: install failed: " + status + " (" + message + ")");
        }
    }
}
