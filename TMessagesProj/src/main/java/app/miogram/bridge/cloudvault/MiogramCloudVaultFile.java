package app.miogram.bridge.cloudvault;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;

/**
 * Model representing a virtual file stored in the encrypted Miogram Cloud Vault.
 */
public class MiogramCloudVaultFile {

    public String fileId;
    public String name;
    public long totalSize;
    public String mimeType;
    public int chunksCount;
    public long chunkSize;
    public String sha256;
    public long topicId;
    public String topicName;
    public long date;
    public ArrayList<Integer> chunkMsgIds = new ArrayList<>();
    public ArrayList<Long> chunkDocIds = new ArrayList<>();
    public transient ArrayList<org.telegram.tgnet.TLRPC.Message> chunkMessages = new ArrayList<>();
    public transient ArrayList<org.telegram.tgnet.TLRPC.Document> chunkDocuments = new ArrayList<>();

    public boolean isDownloading;
    public float downloadProgress;
    public boolean isUploading;
    public float uploadProgress;
    public String localPath;

    public MiogramCloudVaultFile() {
    }

    public String getFormattedSize() {
        return AndroidUtilities.formatFileSize(totalSize);
    }

    public String getFormattedDate() {
        return LocaleController.formatDateChat(date);
    }

    public String getFileExtension() {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            return name.substring(idx + 1).toLowerCase();
        }
        return "";
    }

    public int getIconRes() {
        String ext = getFileExtension();
        switch (ext) {
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
            case "webm":
            case "flv":
                return R.drawable.msg_video;
            case "mp3":
            case "flac":
            case "wav":
            case "m4a":
            case "ogg":
            case "aac":
                return R.drawable.msg_media;
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
            case "iso":
                return R.drawable.msg_archive;
            case "jpg":
            case "jpeg":
            case "png":
            case "webp":
            case "gif":
                return R.drawable.msg_photos;
            case "pdf":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "txt":
                return R.drawable.msg_document;
            case "apk":
                return R.drawable.msg_android;
            default:
                return R.drawable.msg_file;
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("v", 1);
            obj.put("fileId", fileId);
            obj.put("name", name);
            obj.put("size", totalSize);
            obj.put("mime", mimeType != null ? mimeType : "");
            obj.put("chunks", chunksCount);
            obj.put("chunkSize", chunkSize);
            obj.put("sha256", sha256 != null ? sha256 : "");
            obj.put("topicId", topicId);
            obj.put("topicName", topicName != null ? topicName : "");
            obj.put("date", date);

            JSONArray msgArr = new JSONArray();
            for (Integer id : chunkMsgIds) {
                msgArr.put(id);
            }
            obj.put("chunkMsgIds", msgArr);

            JSONArray docArr = new JSONArray();
            for (Long id : chunkDocIds) {
                docArr.put(id);
            }
            obj.put("chunkDocIds", docArr);
        } catch (Exception ignore) {}
        return obj;
    }

    public static MiogramCloudVaultFile fromJson(JSONObject obj) {
        if (obj == null) return null;
        try {
            MiogramCloudVaultFile f = new MiogramCloudVaultFile();
            f.fileId = obj.optString("fileId");
            f.name = obj.optString("name");
            f.totalSize = obj.optLong("size", 0);
            f.mimeType = obj.optString("mime");
            f.chunksCount = obj.optInt("chunks", 1);
            f.chunkSize = obj.optLong("chunkSize", 1000000000L);
            f.sha256 = obj.optString("sha256");
            f.topicId = obj.optLong("topicId", 0);
            f.topicName = obj.optString("topicName", "");
            f.date = obj.optLong("date", System.currentTimeMillis() / 1000L);

            JSONArray msgArr = obj.optJSONArray("chunkMsgIds");
            if (msgArr != null) {
                for (int i = 0; i < msgArr.length(); i++) {
                    f.chunkMsgIds.add(msgArr.getInt(i));
                }
            }

            JSONArray docArr = obj.optJSONArray("chunkDocIds");
            if (docArr != null) {
                for (int i = 0; i < docArr.length(); i++) {
                    f.chunkDocIds.add(docArr.getLong(i));
                }
            }

            return f;
        } catch (Exception e) {
            return null;
        }
    }
}
