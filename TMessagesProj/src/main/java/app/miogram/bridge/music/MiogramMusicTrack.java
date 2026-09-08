package app.miogram.bridge.music;

import java.io.File;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;

public class MiogramMusicTrack {

    public enum Source {
        TELEGRAM("Telegram Cloud", 0xFF0088CC),
        DEEZER("Deezer HQ", 0xFFFF0055),
        ITUNES("iTunes / Apple", 0xFFFA2D48),
        JAMENDO("Jamendo HQ", 0xFF9B59B6);

        public final String label;
        public final int badgeColor;

        Source(String label, int badgeColor) {
            this.label = label;
            this.badgeColor = badgeColor;
        }
    }

    public String id;
    public String title;
    public String artist;
    public String album;
    public int durationSeconds;
    public String coverUrl;
    public String streamUrl;
    public String downloadUrl;
    public long fileSize;
    public Source source;
    public MessageObject telegramMessage;
    public File localFile;

    // Download state tracking
    public boolean isDownloading = false;
    public float downloadProgress = 0f;
    public boolean isInstalled = false;

    public MiogramMusicTrack() {
    }

    public String getFormattedDuration() {
        if (durationSeconds <= 0) return "--:--";
        return AndroidUtilities.formatShortDuration(durationSeconds);
    }

    public String getFormattedFileSize() {
        if (fileSize <= 0) return "";
        return AndroidUtilities.formatFileSize(fileSize);
    }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        return "Unknown Title";
    }

    public String getDisplayArtist() {
        if (artist != null && !artist.trim().isEmpty()) {
            return artist.trim();
        }
        return "Unknown Artist";
    }
}
