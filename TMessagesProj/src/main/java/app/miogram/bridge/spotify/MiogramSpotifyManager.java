package app.miogram.bridge.spotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import app.miogram.bridge.ai.tools.MioTool;
import app.miogram.bridge.lyrics.MiogramLrcModel;
import app.miogram.bridge.lyrics.MiogramLyricsEngine;

/**
 * Spotify Integration Bridge for Miogram.
 * Intercepts Spotify Android broadcasts (metadata, playback state)
 * to provide real-time now-playing info, synced lyrics and MioTool actions.
 */
public class MiogramSpotifyManager {

    public static final String SPOTIFY_PACKAGE = "com.spotify.music";
    public static final String ACTION_METADATA_CHANGED = "com.spotify.music.metadatachanged";
    public static final String ACTION_PLAYBACK_STATE_CHANGED = "com.spotify.music.playbackstatechanged";
    public static final String ACTION_QUEUE_CHANGED = "com.spotify.music.queuechanged";

    private static volatile MiogramSpotifyManager instance;

    public static MiogramSpotifyManager getInstance() {
        if (instance == null) {
            synchronized (MiogramSpotifyManager.class) {
                if (instance == null) {
                    instance = new MiogramSpotifyManager();
                }
            }
        }
        return instance;
    }

    public interface SpotifyListener {
        void onSpotifyTrackChanged(String track, String artist, boolean isPlaying);
        void onSpotifyPlaybackChanged(boolean isPlaying);
    }

    private final List<SpotifyListener> listeners = new CopyOnWriteArrayList<>();

    private boolean registered = false;
    private boolean isPlaying = false;
    private String currentTrack = "";
    private String currentArtist = "";
    private String currentAlbum = "";
    private String currentTrackUri = "";
    private long durationMs = 0;
    private long lastPositionMs = 0;
    private long lastPositionTimestamp = 0;

    static {
        try {
            MioTool.register(new MioTool.Def(
                    "spotify_now_playing",
                    "Spotify Now Playing",
                    "Returns current song, artist, album, duration and active lyrics line from Spotify.",
                    false,
                    (account, params, cb) -> {
                        MiogramSpotifyManager sm = getInstance();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("playing", sm.isPlaying());
                            obj.put("track", sm.getCurrentTrack());
                            obj.put("artist", sm.getCurrentArtist());
                            obj.put("album", sm.getCurrentAlbum());
                            obj.put("url", sm.getTrackWebUrl());
                            obj.put("lyrics_line", sm.getCurrentLyricsLine());
                            if (cb != null) cb.run(obj.toString());
                        } catch (Throwable t) {
                            if (cb != null) cb.run("Spotify status error: " + t.getMessage());
                        }
                    }
            ));

            MioTool.register(new MioTool.Def(
                    "spotify_share_track",
                    "Spotify Share Track",
                    "Gets the Spotify web share link for currently playing song.",
                    false,
                    (account, params, cb) -> {
                        MiogramSpotifyManager sm = getInstance();
                        String url = sm.getTrackWebUrl();
                        if (TextUtils.isEmpty(url)) {
                            if (cb != null) cb.run("No track is currently playing in Spotify.");
                        } else {
                            if (cb != null) cb.run("Now playing: " + sm.getCurrentTrack() + " - " + sm.getCurrentArtist() + "\n" + url);
                        }
                    }
            ));
        } catch (Throwable ignore) {}
    }

    private final BroadcastReceiver spotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (ACTION_METADATA_CHANGED.equals(action)) {
                String id = intent.getStringExtra("id");
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");
                int length = intent.getIntExtra("length", 0);
                boolean playing = intent.getBooleanExtra("playing", false);
                int position = intent.getIntExtra("playbackPosition", 0);

                currentTrackUri = id != null ? id : "";
                currentArtist = artist != null ? artist : "";
                currentAlbum = album != null ? album : "";
                currentTrack = track != null ? track : "";
                durationMs = length;
                isPlaying = playing;
                lastPositionMs = position;
                lastPositionTimestamp = SystemClock.elapsedRealtime();

                // Prefetch lyrics via MiogramLyricsEngine
                if (!TextUtils.isEmpty(currentTrack)) {
                    MiogramLyricsEngine.getInstance().fetchLyricsByMeta(currentArtist, currentTrack, (int) (durationMs / 1000), song -> {
                        AndroidUtilities.runOnUIThread(this::notifyTrackChanged);
                    });
                }

                notifyTrackChanged();
            } else if (ACTION_PLAYBACK_STATE_CHANGED.equals(action)) {
                boolean playing = intent.getBooleanExtra("playing", false);
                int position = intent.getIntExtra("playbackPosition", (int) lastPositionMs);

                isPlaying = playing;
                lastPositionMs = position;
                lastPositionTimestamp = SystemClock.elapsedRealtime();

                notifyPlaybackChanged();
            }
        }
    };

    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                try {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.messagePlayingProgressDidChanged, 0);
                } catch (Throwable ignore) {}
                AndroidUtilities.runOnUIThread(this, 1000);
            }
        }
    };

    private void updateTicker() {
        AndroidUtilities.cancelRunOnUIThread(progressTicker);
        if (isPlaying) {
            AndroidUtilities.runOnUIThread(progressTicker, 1000);
        }
    }

    private MiogramSpotifyManager() {
        ensureRegistered(ApplicationLoader.applicationContext);
    }

    public synchronized void ensureRegistered(Context context) {
        if (registered || context == null) return;
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_METADATA_CHANGED);
            filter.addAction(ACTION_PLAYBACK_STATE_CHANGED);
            filter.addAction(ACTION_QUEUE_CHANGED);
            context.registerReceiver(spotifyReceiver, filter);
            registered = true;
        } catch (Throwable t) {
            FileLog.e("MiogramSpotifyManager: register receiver failed", t);
        }
    }

    public void addListener(SpotifyListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(SpotifyListener l) {
        listeners.remove(l);
    }

    private void notifyTrackChanged() {
        updateTicker();
        AndroidUtilities.runOnUIThread(() -> {
            for (SpotifyListener l : listeners) {
                try {
                    l.onSpotifyTrackChanged(currentTrack, currentArtist, isPlaying);
                } catch (Throwable ignore) {}
            }
            try {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.messagePlayingPlayStateChanged);
            } catch (Throwable ignore) {}
        });
    }

    private void notifyPlaybackChanged() {
        updateTicker();
        AndroidUtilities.runOnUIThread(() -> {
            for (SpotifyListener l : listeners) {
                try {
                    l.onSpotifyPlaybackChanged(isPlaying);
                } catch (Throwable ignore) {}
            }
            try {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.messagePlayingPlayStateChanged);
            } catch (Throwable ignore) {}
        });
    }

    public boolean isPlaying() {
        return isPlaying && !TextUtils.isEmpty(currentTrack);
    }

    public String getCurrentTrack() {
        return currentTrack;
    }

    public String getCurrentArtist() {
        return currentArtist;
    }

    public String getCurrentAlbum() {
        return currentAlbum;
    }

    public String getCurrentTrackUri() {
        return currentTrackUri;
    }

    public String getTrackWebUrl() {
        if (TextUtils.isEmpty(currentTrackUri)) return "";
        if (currentTrackUri.startsWith("spotify:track:")) {
            return "https://open.spotify.com/track/" + currentTrackUri.substring("spotify:track:".length());
        }
        return currentTrackUri;
    }

    public long getCurrentPositionMs() {
        if (!isPlaying) return lastPositionMs;
        long elapsed = SystemClock.elapsedRealtime() - lastPositionTimestamp;
        return Math.max(0, lastPositionMs + elapsed);
    }

    public String getCurrentLyricsLine() {
        if (!isPlaying() || TextUtils.isEmpty(currentTrack)) return "";
        MiogramLrcModel.LrcSong song = MiogramLyricsEngine.getInstance().getCachedSongByMeta(currentArtist, currentTrack);
        if (song == null || song.lines.isEmpty()) {
            return currentTrack + " — " + currentArtist;
        }
        long pos = getCurrentPositionMs();
        int idx = song.findLineIndex(pos);
        if (idx >= 0 && idx < song.lines.size()) {
            String line = song.lines.get(idx).text;
            if (!TextUtils.isEmpty(line)) {
                return line;
            }
        }
        return currentTrack + " — " + currentArtist;
    }

    public void openInSpotify(Context context) {
        if (context == null) return;
        try {
            if (!TextUtils.isEmpty(currentTrackUri)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentTrackUri));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
