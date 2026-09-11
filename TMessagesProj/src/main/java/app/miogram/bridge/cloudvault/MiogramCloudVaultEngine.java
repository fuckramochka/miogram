package app.miogram.bridge.cloudvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_forum;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Core engine for Miogram Encrypted Cloud Vault:
 * - AES-256-GCM client-side encryption/decryption.
 * - Chunking of files > 2GB (default ~950MB chunks) to safely fit Telegram limits.
 * - Cloud-synced manifests hidden in `#MVLT:<Base64>` metadata.
 * - Dedicated Forum Supergroup management (topics as virtual drive folders).
 */
public class MiogramCloudVaultEngine {

    private static final String PREFS_NAME = "miogram_cloud_vault_prefs";
    private static final String KEY_VAULT_CHAT_ID = "vault_chat_id_";
    private static final String KEY_MASTER_KEY = "vault_master_key";
    private static final String KEY_CACHE_INDEX = "vault_index_cache_";

    public static final String MANIFEST_PREFIX = "#MVLT:";
    public static final String PART_PREFIX = "#MVLT_PART:";
    /**
     * 100 MB chunks: safely below every Telegram upload limit, keeps single
     * part captions under the 100-message sync window and bounds memory.
     * Old files carry their own chunkSize in the manifest, so this only
     * affects new uploads. (Was ~953 MB — large files silently failed.)
     */
    public static final long DEFAULT_CHUNK_SIZE = 100L * 1024L * 1024L;
    public static final byte[] MAGIC_HEADER = new byte[]{(byte) 0x4D, (byte) 0x56, (byte) 0x4C, (byte) 0x54}; // "MVLT"

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final ConcurrentHashMap<String, MiogramCloudVaultFile> memoryFiles = new ConcurrentHashMap<>();

    public interface ProgressCallback {
        void onProgress(float progress, String status);
    }

    public interface SyncCallback {
        void onSyncProgress(int count);
        void onSyncComplete(ArrayList<MiogramCloudVaultFile> files);
        void onSyncError(String message);
    }

    public interface VaultCreatedCallback {
        void onCreated(long chatId);
        void onError(String message);
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Master Key Management ---

    public static byte[] getMasterKey() {
        String hex = getPrefs().getString(KEY_MASTER_KEY, null);
        if (hex == null || hex.length() != 64) {
            byte[] newKey = new byte[32]; // 256 bits
            secureRandom.nextBytes(newKey);
            hex = bytesToHex(newKey);
            getPrefs().edit().putString(KEY_MASTER_KEY, hex).apply();
            return newKey;
        }
        return hexToBytes(hex);
    }

    public static String getMasterKeyHex() {
        return bytesToHex(getMasterKey());
    }

    public static void setMasterKeyHex(String hex) {
        if (hex != null && hex.length() == 64) {
            getPrefs().edit().putString(KEY_MASTER_KEY, hex).apply();
        }
    }

    // --- Vault Supergroup Association ---

    public static long getVaultChatId(int currentAccount) {
        return getPrefs().getLong(KEY_VAULT_CHAT_ID + currentAccount, 0);
    }

    public static void setVaultChatId(int currentAccount, long chatId) {
        getPrefs().edit().putLong(KEY_VAULT_CHAT_ID + currentAccount, chatId).apply();
    }

    public static boolean hasVault(int currentAccount) {
        return getVaultChatId(currentAccount) != 0;
    }

    // --- Cryptographic Primitives (AES-256-GCM) ---

    public static byte[] encryptData(byte[] plainText, byte[] key) throws Exception {
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plainText);

        byte[] result = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
        return result;
    }

    public static byte[] decryptData(byte[] cipherTextWithIv, byte[] key) throws Exception {
        if (cipherTextWithIv == null || cipherTextWithIv.length < 13) {
            throw new IllegalArgumentException("Invalid ciphertext length");
        }
        byte[] iv = new byte[12];
        System.arraycopy(cipherTextWithIv, 0, iv, 0, 12);

        int cipherLen = cipherTextWithIv.length - 12;
        byte[] cipherText = new byte[cipherLen];
        System.arraycopy(cipherTextWithIv, 12, cipherText, 0, cipherLen);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherText);
    }

    public static String encryptStringToBase64(String plainText, byte[] key) {
        try {
            byte[] enc = encryptData(plainText.getBytes(StandardCharsets.UTF_8), key);
            return Base64.encodeToString(enc, Base64.NO_WRAP);
        } catch (Exception e) {
            FileLog.e(e);
            return "";
        }
    }

    public static String decryptBase64ToString(String base64Cipher, byte[] key) {
        try {
            byte[] enc = Base64.decode(base64Cipher, Base64.NO_WRAP);
            byte[] dec = decryptData(enc, key);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Manifest Serialization ---

    public static String createManifestCaption(MiogramCloudVaultFile file) {
        byte[] key = getMasterKey();
        String json = file.toJson().toString();
        String encBase64 = encryptStringToBase64(json, key);
        return MANIFEST_PREFIX + encBase64;
    }

    public static MiogramCloudVaultFile parseManifestCaption(String text) {
        if (text == null || !text.startsWith(MANIFEST_PREFIX)) return null;
        try {
            String base64 = text.substring(MANIFEST_PREFIX.length()).trim();
            byte[] key = getMasterKey();
            String jsonStr = decryptBase64ToString(base64, key);
            if (jsonStr != null) {
                return MiogramCloudVaultFile.fromJson(new JSONObject(jsonStr));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String createPartCaption(String fileId, int partIndex, int totalParts) {
        return PART_PREFIX + fileId + ":" + partIndex + ":" + totalParts;
    }

    // --- Streaming File Chunking & Encryption ---

    public static ArrayList<File> splitAndEncryptFile(Context context, Uri fileUri, String originalName, long plainSize, String fileId, ProgressCallback callback) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(fileUri);
        if (in == null) {
            throw new IllegalArgumentException("Unable to open source file stream: " + fileUri);
        }
        return splitAndEncryptStream(context, in, originalName, plainSize, fileId, callback);
    }

    public static ArrayList<File> splitAndEncryptFile(Context context, File sourceFile, String originalName, long plainSize, String fileId, ProgressCallback callback) throws Exception {
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("Source file does not exist: " + sourceFile.getAbsolutePath());
        }
        return splitAndEncryptStream(context, new FileInputStream(sourceFile), originalName, plainSize, fileId, callback);
    }

    public static ArrayList<File> splitAndEncryptStream(Context context, InputStream in, String originalName, long plainSize, String fileId, ProgressCallback callback) throws Exception {
        ArrayList<File> chunkFiles = new ArrayList<>();
        byte[] masterKey = getMasterKey();

        long chunkSize = DEFAULT_CHUNK_SIZE;
        int totalChunks = (int) Math.max(1, Math.ceil((double) plainSize / chunkSize));

        File cacheDir = new File(context.getCacheDir(), "vault_temp");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        long totalBytesRead = 0;

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            String chunkFileName = "enc_" + fileId.substring(0, Math.min(8, fileId.length()))
                    + "_p" + String.format("%02d", chunkIndex + 1) + "_of_" + String.format("%02d", totalChunks) + ".bin";
            File chunkFile = new File(cacheDir, chunkFileName);
            if (chunkFile.exists()) {
                chunkFile.delete();
            }

            FileOutputStream chunkOut = new FileOutputStream(chunkFile);

            // Write 33-byte custom header:
            // 4 bytes: MAGIC ("MVLT")
            // 1 byte: Version (1)
            // 12 bytes: IV
            // 4 bytes: chunk index
            // 4 bytes: total chunks
            // 8 bytes: plain chunk length
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            long expectedChunkPlainSize = (chunkIndex == totalChunks - 1) ? (plainSize - (long) chunkIndex * chunkSize) : chunkSize;

            chunkOut.write(MAGIC_HEADER);
            chunkOut.write(1); // version 1
            chunkOut.write(iv);
            chunkOut.write(intToBytes(chunkIndex));
            chunkOut.write(intToBytes(totalChunks));
            chunkOut.write(longToBytes(expectedChunkPlainSize));

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, iv));

            long chunkBytesRead = 0;
            while (chunkBytesRead < expectedChunkPlainSize) {
                int bytesToRead = (int) Math.min((long) buffer.length, expectedChunkPlainSize - chunkBytesRead);
                int read = in.read(buffer, 0, bytesToRead);
                if (read <= 0) break;

                byte[] cipherChunk = cipher.update(buffer, 0, read);
                if (cipherChunk != null && cipherChunk.length > 0) {
                    chunkOut.write(cipherChunk);
                }

                chunkBytesRead += read;
                totalBytesRead += read;

                if (callback != null && plainSize > 0) {
                    float progress = (float) totalBytesRead / plainSize;
                    callback.onProgress(progress, "Шифрування частини " + (chunkIndex + 1) + " з " + totalChunks);
                }
            }

            byte[] finalBlock = cipher.doFinal();
            if (finalBlock != null && finalBlock.length > 0) {
                chunkOut.write(finalBlock);
            }

            chunkOut.flush();
            chunkOut.close();

            chunkFiles.add(chunkFile);
        }

        in.close();
        return chunkFiles;
    }

    public static void uploadFileToVault(int currentAccount, File file, String fileName, String mimeType, ProgressCallback callback, Utilities.Callback<MiogramCloudVaultFile> onComplete, Utilities.Callback<String> onError) {
        long vaultChatId = getVaultChatId(currentAccount);
        if (vaultChatId == 0) {
            if (onError != null) onError.run("Vault chat not linked");
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                String fileId = UUID.randomUUID().toString();
                long plainSize = file.length();
                String targetName = TextUtils.isEmpty(fileName) ? file.getName() : fileName;

                ArrayList<File> chunkFiles = splitAndEncryptFile(context, file, targetName, plainSize, fileId, callback);

                MiogramCloudVaultFile vaultFile = new MiogramCloudVaultFile();
                vaultFile.fileId = fileId;
                vaultFile.name = targetName;
                vaultFile.totalSize = plainSize;
                vaultFile.mimeType = mimeType != null ? mimeType : "application/octet-stream";
                vaultFile.chunksCount = chunkFiles.size();
                vaultFile.chunkSize = DEFAULT_CHUNK_SIZE;
                vaultFile.date = System.currentTimeMillis() / 1000L;
                vaultFile.localPath = file.getAbsolutePath();

                if (vaultFile.isMedia()) {
                    vaultFile.topicName = "🎬 Медіа";
                } else if (vaultFile.isAudio()) {
                    vaultFile.topicName = "🎵 Музика";
                } else if (vaultFile.isArchive()) {
                    vaultFile.topicName = "📦 Архіви";
                } else {
                    vaultFile.topicName = "📁 Документи";
                }

                long targetDialogId = -vaultChatId;
                for (int i = 0; i < chunkFiles.size(); i++) {
                    File chunk = chunkFiles.get(i);
                    String caption;
                    if (i == 0) {
                        caption = createManifestCaption(vaultFile);
                    } else {
                        caption = createPartCaption(vaultFile.fileId, i + 1, chunkFiles.size());
                    }

                    org.telegram.messenger.SendMessagesHelper.prepareSendingDocument(
                            org.telegram.messenger.AccountInstance.getInstance(currentAccount),
                            chunk.getAbsolutePath(),
                            chunk.getAbsolutePath(),
                            null,
                            caption,
                            "application/octet-stream",
                            targetDialogId,
                            null, null, null, null, null,
                            true, 0, null, null, false
                    );
                }

                registerFile(vaultFile);
                saveCache(currentAccount);

                if (onComplete != null) {
                    AndroidUtilities.runOnUIThread(() -> onComplete.run(vaultFile));
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (onError != null) {
                    AndroidUtilities.runOnUIThread(() -> onError.run(e.getMessage()));
                }
            }
        });
    }

    // --- Streaming File Decryption & Reassembly ---

    public static File decryptAndReassembleFile(Context context, MiogramCloudVaultFile vaultFile, ArrayList<File> chunkFiles, ProgressCallback callback) throws Exception {
        byte[] masterKey = getMasterKey();

        File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Miogram Vault");
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        String safeFileName = vaultFile.name != null ? vaultFile.name : ("file_" + vaultFile.fileId);
        File destFile = new File(downloadsDir, safeFileName);
        int counter = 1;
        while (destFile.exists()) {
            int dot = safeFileName.lastIndexOf('.');
            if (dot > 0) {
                destFile = new File(downloadsDir, safeFileName.substring(0, dot) + " (" + counter + ")" + safeFileName.substring(dot));
            } else {
                destFile = new File(downloadsDir, safeFileName + " (" + counter + ")");
            }
            counter++;
        }

        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        long totalDecrypted = 0;

        for (int i = 0; i < chunkFiles.size(); i++) {
            File chunkFile = chunkFiles.get(i);
            FileInputStream chunkIn = new FileInputStream(chunkFile);

            // Read header
            byte[] magic = new byte[4];
            chunkIn.read(magic);
            int version = chunkIn.read();
            byte[] iv = new byte[12];
            chunkIn.read(iv);
            byte[] idxBytes = new byte[4];
            chunkIn.read(idxBytes);
            byte[] totalBytes = new byte[4];
            chunkIn.read(totalBytes);
            byte[] plainLenBytes = new byte[8];
            chunkIn.read(plainLenBytes);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, iv));

            int read;
            while ((read = chunkIn.read(buffer)) > 0) {
                byte[] decrypted = cipher.update(buffer, 0, read);
                if (decrypted != null && decrypted.length > 0) {
                    out.write(decrypted);
                    totalDecrypted += decrypted.length;
                }
                if (callback != null && vaultFile.totalSize > 0) {
                    float progress = (float) totalDecrypted / vaultFile.totalSize;
                    callback.onProgress(progress, "Розшифрування частини " + (i + 1) + " з " + chunkFiles.size());
                }
            }

            byte[] finalBlock = cipher.doFinal();
            if (finalBlock != null && finalBlock.length > 0) {
                out.write(finalBlock);
                totalDecrypted += finalBlock.length;
            }

            chunkIn.close();
        }

        out.flush();
        out.close();

        MediaScannerConnection.scanFile(context, new String[]{destFile.getAbsolutePath()}, null, null);
        return destFile;
    }

    // --- Automatic Forum Supergroup Creation ---

    public static void createVaultSupergroup(BaseFragment fragment, int currentAccount, VaultCreatedCallback callback) {
        MessagesController mc = fragment.getMessagesController();
        if (mc == null) {
            if (callback != null) callback.onError("MessagesController unavailable");
            return;
        }

        TLRPC.TL_channels_createChannel req = new TLRPC.TL_channels_createChannel();
        req.title = "Miogram Cloud Vault ☁️";
        req.about = "Зашифроване персональне хмарне сховище Miogram.";
        req.megagroup = true;
        req.forum = true;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                if (callback != null) callback.onError(error.text != null ? error.text : "Error creating supergroup");
                return;
            }

            if (response instanceof TLRPC.Updates) {
                TLRPC.Updates updates = (TLRPC.Updates) response;
                mc.processUpdates(updates, false);
                if (updates.chats != null && !updates.chats.isEmpty()) {
                    long chatId = updates.chats.get(0).id;
                    setVaultChatId(currentAccount, chatId);

                    // Create starter forum topics
                    createTopic(currentAccount, chatId, "📁 Документи", 0x3390EC);
                    createTopic(currentAccount, chatId, "🎬 Медіа", 0xE53935);
                    createTopic(currentAccount, chatId, "📦 Архіви", 0xFB8C00);

                    if (callback != null) {
                        callback.onCreated(chatId);
                    }
                    return;
                }
            }
            if (callback != null) callback.onError("Invalid server response");
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    public static void createTopic(int currentAccount, long chatId, String title, int iconColor) {
        TL_forum.TL_messages_createForumTopic req = new TL_forum.TL_messages_createForumTopic();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(-chatId);
        req.title = title;
        req.random_id = Utilities.random.nextLong();
        req.icon_color = iconColor;
        req.flags |= 1;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.Updates) {
                MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) response, false);
            }
        });
    }

    // --- Memory Cache & File Registry ---

    public static void registerFile(MiogramCloudVaultFile file) {
        if (file != null && file.fileId != null) {
            memoryFiles.put(file.fileId, file);
            app.miogram.bridge.hooks.MioHook.dispatchVault("uploaded", file.fileId, file.name);
        }
    }

    public static ArrayList<MiogramCloudVaultFile> getFilesForTopic(long topicId) {
        ArrayList<MiogramCloudVaultFile> list = new ArrayList<>();
        for (MiogramCloudVaultFile f : memoryFiles.values()) {
            if (topicId == 0 || f.topicId == topicId) {
                list.add(f);
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(b.date, a.date));
        return list;
    }

    public static long getTotalVaultSize() {
        long sum = 0;
        for (MiogramCloudVaultFile f : memoryFiles.values()) {
            sum += f.totalSize;
        }
        return sum;
    }

    public static int getTotalVaultFilesCount() {
        return memoryFiles.size();
    }

    public static void saveCache(int currentAccount) {
        try {
            JSONArray arr = new JSONArray();
            for (MiogramCloudVaultFile f : memoryFiles.values()) {
                arr.put(f.toJson());
            }
            getPrefs().edit().putString(KEY_CACHE_INDEX + currentAccount, arr.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void loadCache(int currentAccount) {
        try {
            String json = getPrefs().getString(KEY_CACHE_INDEX + currentAccount, null);
            if (!TextUtils.isEmpty(json)) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    MiogramCloudVaultFile f = MiogramCloudVaultFile.fromJson(arr.getJSONObject(i));
                    if (f != null && f.fileId != null) {
                        memoryFiles.put(f.fileId, f);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void syncVaultFiles(int currentAccount, long vaultChatId, SyncCallback callback) {
        loadCache(currentAccount);
        if (vaultChatId == 0) {
            if (callback != null) callback.onSyncComplete(getFilesForTopic(0));
            return;
        }

        TLRPC.TL_messages_getHistory reqHistory = new TLRPC.TL_messages_getHistory();
        reqHistory.peer = MessagesController.getInstance(currentAccount).getInputPeer(-vaultChatId);
        reqHistory.limit = 100;

        ConnectionsManager.getInstance(currentAccount).sendRequest(reqHistory, (respHistory, errHistory) -> {
            if (respHistory instanceof TLRPC.messages_Messages) {
                processSyncMessages(currentAccount, ((TLRPC.messages_Messages) respHistory).messages);
            }
            AndroidUtilities.runOnUIThread(() -> {
                saveCache(currentAccount);
                if (callback != null) callback.onSyncComplete(getFilesForTopic(0));
            });
        });
    }

    private static void processSyncMessages(int currentAccount, ArrayList<TLRPC.Message> messages) {
        if (messages == null) return;
        java.util.HashSet<String> touched = new java.util.HashSet<>();
        // Pass 1: manifests first — parts that arrive before their manifest
        // (search returns newest-first) must not be dropped.
        for (TLRPC.Message msg : messages) {
            if (msg == null || msg.message == null) continue;
            if (!msg.message.startsWith(MANIFEST_PREFIX)) continue;
            MiogramCloudVaultFile parsed = parseManifestCaption(msg.message);
            if (parsed == null || parsed.fileId == null) continue;
            MiogramCloudVaultFile existing = memoryFiles.get(parsed.fileId);
            if (existing == null) {
                memoryFiles.put(parsed.fileId, parsed);
                existing = parsed;
            } else {
                if (existing.totalSize == 0 && parsed.totalSize > 0) existing.totalSize = parsed.totalSize;
                if (TextUtils.isEmpty(existing.name) && !TextUtils.isEmpty(parsed.name)) existing.name = parsed.name;
                if (existing.chunksCount <= 0 && parsed.chunksCount > 0) existing.chunksCount = parsed.chunksCount;
            }
            attachChunkDoc(existing, msg, 0);
            touched.add(parsed.fileId);
            flushPendingParts(existing);
        }
        // Pass 2: parts (orphans buffered until their manifest shows up).
        for (TLRPC.Message msg : messages) {
            if (msg == null || msg.message == null) continue;
            if (!msg.message.startsWith(PART_PREFIX)) continue;
            String fId = partFileIdOf(msg.message);
            if (fId == null) continue;
            MiogramCloudVaultFile existing = memoryFiles.get(fId);
            if (existing != null) {
                attachChunkDoc(existing, msg, partIndexOf(msg.message));
                touched.add(fId);
            } else {
                bufferPendingPart(fId, msg);
            }
        }
        // Pass 3: restore canonical chunk order (manifest + parts by index).
        for (String fId : touched) {
            MiogramCloudVaultFile f = memoryFiles.get(fId);
            if (f != null) reorderChunks(f);
        }
    }

    private static String partFileIdOf(String caption) {
        try {
            String[] tokens = caption.substring(PART_PREFIX.length()).split(":");
            if (tokens.length >= 2 && tokens[0] != null && !tokens[0].isEmpty()) return tokens[0];
        } catch (Throwable ignore) {}
        return null;
    }

    private static int partIndexOf(String caption) {
        if (caption == null) return Integer.MAX_VALUE;
        if (caption.startsWith(MANIFEST_PREFIX)) return 0;
        if (caption.startsWith(PART_PREFIX)) {
            try {
                String[] tokens = caption.substring(PART_PREFIX.length()).split(":");
                if (tokens.length >= 2) return Integer.parseInt(tokens[1]);
            } catch (Throwable ignore) {}
        }
        return Integer.MAX_VALUE;
    }

    /** Parts seen before their manifest — flushed once the manifest lands. */
    private static final ConcurrentHashMap<String, ArrayList<TLRPC.Message>> pendingParts = new ConcurrentHashMap<>();

    private static void bufferPendingPart(String fileId, TLRPC.Message msg) {
        ArrayList<TLRPC.Message> list = pendingParts.get(fileId);
        if (list == null) {
            list = new ArrayList<>();
            pendingParts.put(fileId, list);
        }
        synchronized (list) {
            for (TLRPC.Message m : list) {
                if (m.id == msg.id) return;
            }
            list.add(msg);
        }
    }

    private static void flushPendingParts(MiogramCloudVaultFile file) {
        ArrayList<TLRPC.Message> list = pendingParts.remove(file.fileId);
        if (list == null) return;
        synchronized (list) {
            for (TLRPC.Message m : list) {
                attachChunkDoc(file, m, partIndexOf(m.message));
            }
        }
        reorderChunks(file);
    }

    /** Adds a chunk message+document, de-duplicated by message id (TLRPC.Message has no equals()). */
    private static void attachChunkDoc(MiogramCloudVaultFile file, TLRPC.Message msg, int orderHint) {
        if (file == null || msg == null) return;
        if (file.chunkMsgIds.contains(msg.id)) return;
        file.chunkMsgIds.add(msg.id);
        if (msg.media != null && msg.media.document != null) {
            file.chunkMessages.add(msg);
            file.chunkDocuments.add(msg.media.document);
            if (file.chunkDocIds != null && !file.chunkDocIds.contains(msg.media.document.id)) {
                file.chunkDocIds.add(msg.media.document.id);
            }
        }
    }

    /** Sorts chunkMessages/chunkDocuments: manifest first, then parts by index. */
    private static void reorderChunks(MiogramCloudVaultFile file) {
        int n = file.chunkMessages.size();
        if (n <= 1) return;
        ArrayList<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        final ArrayList<TLRPC.Message> msgs = file.chunkMessages;
        java.util.Collections.sort(order, (a, b) -> {
            int ia = partIndexOf(msgs.get(a).message);
            int ib = partIndexOf(msgs.get(b).message);
            if (ia != ib) return Integer.compare(ia, ib);
            return Integer.compare(msgs.get(a).id, msgs.get(b).id);
        });
        ArrayList<TLRPC.Message> sortedMsgs = new ArrayList<>(n);
        ArrayList<TLRPC.Document> sortedDocs = new ArrayList<>(file.chunkDocuments.size());
        for (int idx : order) {
            TLRPC.Message m = msgs.get(idx);
            sortedMsgs.add(m);
            if (m.media != null && m.media.document != null) sortedDocs.add(m.media.document);
        }
        file.chunkMessages = sortedMsgs;
        file.chunkDocuments = sortedDocs;
    }

    /**
     * Resolves and populates chunkDocuments for a vault file if not yet loaded into memory.
     * Tries direct message lookup by chunkMsgIds via TL_channels_getMessages first,
     * and falls back to supergroup history sync if chunkMsgIds is empty or partial.
     */
    public static void resolveChunkDocuments(int currentAccount, long vaultChatId, MiogramCloudVaultFile file, Runnable onReady) {
        if (file == null) {
            if (onReady != null) AndroidUtilities.runOnUIThread(onReady);
            return;
        }
        if (!file.chunkDocuments.isEmpty() && file.chunkDocuments.size() >= file.chunksCount) {
            if (onReady != null) AndroidUtilities.runOnUIThread(onReady);
            return;
        }
        if (vaultChatId == 0) {
            if (onReady != null) AndroidUtilities.runOnUIThread(onReady);
            return;
        }

        if (file.chunkMsgIds != null && !file.chunkMsgIds.isEmpty()) {
            TLRPC.TL_channels_getMessages req = new TLRPC.TL_channels_getMessages();
            req.channel = MessagesController.getInstance(currentAccount).getInputChannel(vaultChatId);
            req.id = new ArrayList<>(file.chunkMsgIds);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (error == null && response instanceof TLRPC.messages_Messages) {
                    ArrayList<TLRPC.Message> msgs = ((TLRPC.messages_Messages) response).messages;
                    if (msgs != null) {
                        for (TLRPC.Message msg : msgs) {
                            if (msg != null && msg.media != null && msg.media.document != null) {
                                attachChunkDoc(file, msg, partIndexOf(msg.message));
                            }
                        }
                        reorderChunks(file);
                    }
                }
                if (!file.chunkDocuments.isEmpty()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onReady != null) onReady.run();
                    });
                } else {
                    syncVaultFiles(currentAccount, vaultChatId, new SyncCallback() {
                        @Override
                        public void onSyncProgress(int count) {}

                        @Override
                        public void onSyncComplete(ArrayList<MiogramCloudVaultFile> files) {
                            if (onReady != null) onReady.run();
                        }

                        @Override
                        public void onSyncError(String message) {
                            if (onReady != null) onReady.run();
                        }
                    });
                }
            });
        } else {
            syncVaultFiles(currentAccount, vaultChatId, new SyncCallback() {
                @Override
                public void onSyncProgress(int count) {}

                @Override
                public void onSyncComplete(ArrayList<MiogramCloudVaultFile> files) {
                    if (onReady != null) onReady.run();
                }

                @Override
                public void onSyncError(String message) {
                    if (onReady != null) onReady.run();
                }
            });
        }
    }

    public static void deleteVaultFile(int currentAccount, long vaultChatId, MiogramCloudVaultFile file, boolean deleteServerMessages) {
        if (file == null) return;
        memoryFiles.remove(file.fileId);
        saveCache(currentAccount);
        app.miogram.bridge.hooks.MioHook.dispatchVault("deleted", file.fileId, file.name);

        if (deleteServerMessages && vaultChatId != 0 && !file.chunkMsgIds.isEmpty()) {
            MessagesController.getInstance(currentAccount).deleteMessages(file.chunkMsgIds, null, null, -vaultChatId, 0, true, 0);
        }
    }

    // --- Helpers ---

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static byte[] intToBytes(int val) {
        return new byte[]{
                (byte) (val >> 24),
                (byte) (val >> 16),
                (byte) (val >> 8),
                (byte) val
        };
    }

    private static byte[] longToBytes(long val) {
        return new byte[]{
                (byte) (val >> 56),
                (byte) (val >> 48),
                (byte) (val >> 40),
                (byte) (val >> 32),
                (byte) (val >> 24),
                (byte) (val >> 16),
                (byte) (val >> 8),
                (byte) val
        };
    }
}
