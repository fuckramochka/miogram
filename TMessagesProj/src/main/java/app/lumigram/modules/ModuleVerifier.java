package app.lumigram.modules;

import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Перевірка цілісності .lumi-архіву.
 *
 * <p>Рівень 1 (зараз): SHA-256 з каталогу. Рівень 2 (перед першим релізом):
 * Ed25519-підпис авторським ключем — прапор VERIFY_SIGNATURE і заглушка
 * {@link #verifySignature} чесно повертає false, доки ключ не вшито.
 * Без підпису релізний білд модулі ставити відмовиться.
 */
public final class ModuleVerifier {

    /** Увімкнути перед релізом разом із вшитим AUTHOR_PUBLIC_KEY. */
    private static final boolean VERIFY_SIGNATURE = false;
    // TODO(release): вшити Ed25519 public key автора, видалити dev-допуск.

    private ModuleVerifier() {
    }

    public static String sha256(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int read;
            while ((read = in.read(buf)) > 0) {
                digest.update(buf, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            FileLog.e("LumiModules: sha256 failed", e);
            return null;
        }
    }

    public static boolean verifySha(File file, String expectedHex) {
        if (expectedHex == null || expectedHex.isEmpty()) {
            FileLog.e("LumiModules: no sha256 in catalog");
            return false;
        }
        String actual = sha256(file);
        return actual != null && actual.equalsIgnoreCase(expectedHex);
    }

    /**
     * Заглушка підпису: повертає false, доки не вшито ключ.
     * НЕ міняти на return true — це дірка під чужі модулі.
     */
    public static boolean verifySignature(File file, String signatureBase64) {
        if (!VERIFY_SIGNATURE) {
            FileLog.d("LumiModules: signature check skipped (dev)");
            return true;
        }
        if (signatureBase64 == null || signatureBase64.isEmpty()) {
            return false;
        }
        // TODO(release): Ed25519 verify(file bytes, signature, AUTHOR_PUBLIC_KEY).
        return false;
    }
}
