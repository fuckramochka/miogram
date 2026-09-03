package app.miogram.bridge.vault;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Clean, rock-solid manager for Double Bottom (Подвійне сховище).
 * Provides:
 *  - Real Passcode (full access)
 *  - Emergency / Duress Passcode (decoy mode)
 *  - Designated Emergency Account
 *  - Whitelisted / Allowed Chats per account in Emergency mode
 *  - Panic Logout option
 */
public class MiogramDoubleBottomManager {

    private static final String PREFS_NAME = "miogram_double_bottom";
    private static final String KEY_REAL_PIN = "real_pin";
    private static final String KEY_DURESS_PIN = "duress_pin";
    private static final String KEY_DECOY_ACCOUNT = "decoy_account";
    private static final String KEY_PANIC_LOGOUT = "panic_logout";
    private static final String PREF_ALLOWED_PREFIX = "allowed_chats_";

    public static final int VERDICT_NONE = 0;
    public static final int VERDICT_REAL = 1;
    public static final int VERDICT_DURESS = 2;

    public static volatile boolean isDuressActive = false;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isConfigured() {
        return getRealPasscode().length() > 0 && getDuressPasscode().length() > 0;
    }

    public static boolean isDuressActive() {
        return isDuressActive;
    }

    public static void setDuressActive(boolean active) {
        isDuressActive = active;
    }

    public static String getRealPasscode() {
        return getPrefs().getString(KEY_REAL_PIN, "");
    }

    public static void setRealPasscode(String pin) {
        getPrefs().edit().putString(KEY_REAL_PIN, pin != null ? pin.trim() : "").apply();
        if (pin != null && pin.trim().length() > 0) {
            syncToTelegramPasscode(pin.trim());
        }
    }

    public static String getDuressPasscode() {
        return getPrefs().getString(KEY_DURESS_PIN, "");
    }

    public static void setDuressPasscode(String pin) {
        getPrefs().edit().putString(KEY_DURESS_PIN, pin != null ? pin.trim() : "").apply();
    }

    public static int getDecoyAccount() {
        return getPrefs().getInt(KEY_DECOY_ACCOUNT, -1);
    }

    public static void setDecoyAccount(int account) {
        getPrefs().edit().putInt(KEY_DECOY_ACCOUNT, account).apply();
    }

    public static boolean isPanicLogoutEnabled() {
        return getPrefs().getBoolean(KEY_PANIC_LOGOUT, false);
    }

    public static void setPanicLogoutEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_PANIC_LOGOUT, enabled).apply();
    }

    public static Set<Long> getAllowedDialogIds(int account) {
        Set<String> stringSet = getPrefs().getStringSet(PREF_ALLOWED_PREFIX + account, null);
        Set<Long> result = new HashSet<>();
        if (stringSet != null) {
            for (String s : stringSet) {
                try {
                    result.add(Long.parseLong(s));
                } catch (Exception ignore) {}
            }
        }
        return result;
    }

    public static void setAllowedDialogIds(int account, Collection<Long> ids) {
        Set<String> stringSet = new HashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    stringSet.add(String.valueOf(id));
                }
            }
        }
        getPrefs().edit().putStringSet(PREF_ALLOWED_PREFIX + account, stringSet).apply();
    }

    public static boolean hasAllowedDialogs(int account) {
        Set<String> set = getPrefs().getStringSet(PREF_ALLOWED_PREFIX + account, null);
        return set != null && !set.isEmpty();
    }

    public static boolean isChatAllowed(int account, long dialogId) {
        if (!isDuressActive) {
            return true;
        }
        if (!hasAllowedDialogs(account)) {
            return true;
        }
        return getAllowedDialogIds(account).contains(dialogId);
    }

    public static int checkPasscode(String pin) {
        if (pin == null || pin.isEmpty()) {
            return VERDICT_NONE;
        }
        String real = getRealPasscode();
        String duress = getDuressPasscode();

        if (real.length() > 0 && real.equals(pin)) {
            return VERDICT_REAL;
        }
        if (duress.length() > 0 && duress.equals(pin)) {
            return VERDICT_DURESS;
        }
        return VERDICT_NONE;
    }

    public static void clearAll() {
        getPrefs().edit().clear().apply();
        isDuressActive = false;
        SharedConfig.passcodeHash = "";
        SharedConfig.appLocked = false;
        SharedConfig.saveConfig();
    }

    private static void syncToTelegramPasscode(String pin) {
        try {
            SharedConfig.passcodeSalt = new byte[16];
            Utilities.random.nextBytes(SharedConfig.passcodeSalt);
            byte[] passcodeBytes = pin.getBytes(StandardCharsets.UTF_8);
            byte[] bytes = new byte[32 + passcodeBytes.length];
            System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, 0, 16);
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
            System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
            SharedConfig.passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
            SharedConfig.passcodeType = SharedConfig.PASSCODE_TYPE_PIN;
            SharedConfig.appLocked = false;
            SharedConfig.saveConfig();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
