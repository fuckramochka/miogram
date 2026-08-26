package app.miogram.bridge.passcode;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages Duress (Decoy / Emergency) mode configuration, PINs and allowed visible chats.
 */
public class MiogramDuressConfig {

    private static final String PREFS_NAME = "miogram_duress_prefs";
    private static final String KEY_DECOY_DIALOG_IDS = "decoy_dialog_ids";
    private static final String KEY_DURESS_ACTIVE = "duress_active";
    private static final String KEY_REAL_PIN_HASH = "real_pin_hash";
    private static final String KEY_DURESS_PIN_HASH = "duress_pin_hash";

    private static volatile boolean isDuressActive = false;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) return "";
        byte[] bytes = pin.getBytes(StandardCharsets.UTF_8);
        return Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
    }

    public static void setRealPin(String pin) {
        prefs().edit().putString(KEY_REAL_PIN_HASH, hashPin(pin)).apply();
    }

    public static boolean hasRealPin() {
        return prefs().contains(KEY_REAL_PIN_HASH) && !prefs().getString(KEY_REAL_PIN_HASH, "").isEmpty();
    }

    public static boolean checkRealPin(String pin) {
        String saved = prefs().getString(KEY_REAL_PIN_HASH, "");
        if (saved.isEmpty()) return false;
        return saved.equals(hashPin(pin));
    }

    public static void setDuressPin(String pin) {
        prefs().edit().putString(KEY_DURESS_PIN_HASH, hashPin(pin)).apply();
    }

    public static boolean hasDuressPin() {
        return prefs().contains(KEY_DURESS_PIN_HASH) && !prefs().getString(KEY_DURESS_PIN_HASH, "").isEmpty();
    }

    public static boolean checkDuressPin(String pin) {
        String saved = prefs().getString(KEY_DURESS_PIN_HASH, "");
        if (saved.isEmpty()) return false;
        return saved.equals(hashPin(pin));
    }

    public static boolean isDuressActive() {
        return isDuressActive;
    }

    public static void setDuressActive(boolean active) {
        isDuressActive = active;
        prefs().edit().putBoolean(KEY_DURESS_ACTIVE, active).apply();
    }

    public static Set<Long> getDecoyDialogIds() {
        Set<String> set = prefs().getStringSet(KEY_DECOY_DIALOG_IDS, null);
        Set<Long> result = new HashSet<>();
        if (set != null) {
            for (String s : set) {
                try {
                    result.add(Long.parseLong(s));
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    public static void setDecoyDialogIds(Set<Long> ids) {
        Set<String> set = new HashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                set.add(String.valueOf(id));
            }
        }
        prefs().edit().putStringSet(KEY_DECOY_DIALOG_IDS, set).apply();
    }

    public static void addDecoyDialogId(long dialogId) {
        Set<Long> ids = getDecoyDialogIds();
        ids.add(dialogId);
        setDecoyDialogIds(ids);
    }

    public static void removeDecoyDialogId(long dialogId) {
        Set<Long> ids = getDecoyDialogIds();
        ids.remove(dialogId);
        setDecoyDialogIds(ids);
    }

    public static boolean isDialogAllowedInDuress(long dialogId) {
        if (!isDuressActive) {
            return true;
        }
        Set<Long> ids = getDecoyDialogIds();
        if (ids.isEmpty()) {
            return true;
        }
        return ids.contains(dialogId);
    }
}
