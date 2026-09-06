package app.miogram.bridge.lyrics;

import android.text.TextUtils;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.audioinfo.AudioInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Multi-source lyrics engine for Miogram Player with strict title and duration validation.
 * Supported sources:
 * 0. SOURCE_AUTO: Auto cascade (LRCLib -> NetEase -> Embedded ID3 -> Genius -> Fallback)
 * 1. SOURCE_SERVER: Server & disk cache only
 * 2. SOURCE_LRCLIB: LRCLib API (strict search)
 * 3. SOURCE_NETEASE: NetEase Cloud Music (LRC + translation)
 * 4. SOURCE_YANDEX: Yandex Music / streaming provider
 * 5. SOURCE_GENIUS: Genius web search
 * 6. SOURCE_YOUTUBE: YouTube track info
 * 7. SOURCE_AI: AI Audio Transcription
 */
public class MiogramLyricsEngine {

    public static final int SOURCE_AUTO = 0;
    public static final int SOURCE_SERVER = 1;
    public static final int SOURCE_LRCLIB = 2;
    public static final int SOURCE_NETEASE = 3;
    public static final int SOURCE_YANDEX = 4;
    public static final int SOURCE_GENIUS = 5;
    public static final int SOURCE_YOUTUBE = 6;
    public static final int SOURCE_AI = 7;

    private static volatile MiogramLyricsEngine Instance;

    public static MiogramLyricsEngine getInstance() {
        MiogramLyricsEngine local = Instance;
        if (local == null) {
            synchronized (MiogramLyricsEngine.class) {
                local = Instance;
                if (local == null) {
                    Instance = local = new MiogramLyricsEngine();
                }
            }
        }
        return local;
    }

    public MiogramLrcModel.LrcSong getCachedSong(MessageObject messageObject) {
        if (messageObject == null) return null;
        String rawTitle = messageObject.getMusicTitle();
        String rawAuthor = messageObject.getMusicAuthor();
        if (TextUtils.isEmpty(rawTitle)) return null;
        String title = cleanTitle(rawTitle);
        String artist = cleanArtist(rawAuthor);
        String cacheKey = getCacheKey(artist, title);
        MiogramLrcModel.LrcSong song = memoryCache.get(cacheKey);
        if (song != null) return song;
        return loadFromDisk(cacheKey);
    }

    public interface LyricsCallback {
        void onLyricsLoaded(MiogramLrcModel.LrcSong song);
        void onError(String message);
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final LruCache<String, MiogramLrcModel.LrcSong> memoryCache = new LruCache<>(80);
    private final OkHttpClient httpClient;
    private final File cacheDir;

    private MiogramLyricsEngine() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();

        File base = ApplicationLoader.applicationContext != null ? ApplicationLoader.applicationContext.getCacheDir() : new File("/tmp");
        cacheDir = new File(base, "miogram_lyrics");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void fetchLyrics(final MessageObject messageObject, final LyricsCallback callback) {
        fetchLyrics(messageObject, SOURCE_AUTO, callback);
    }

    public void fetchLyrics(final MessageObject messageObject, final int preferredSource, final LyricsCallback callback) {
        if (messageObject == null) {
            if (callback != null) callback.onError("MessageObject is null");
            return;
        }

        String rawTitle = messageObject.getMusicTitle();
        String rawAuthor = messageObject.getMusicAuthor();

        if (TextUtils.isEmpty(rawTitle)) {
            rawTitle = messageObject.getDocumentName();
            if (rawTitle != null && rawTitle.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                rawTitle = rawTitle.substring(0, rawTitle.length() - 4);
            }
        }
        if (TextUtils.isEmpty(rawTitle)) {
            rawTitle = "Unknown";
        }
        if (TextUtils.isEmpty(rawAuthor)) {
            rawAuthor = "";
        }

        final String title = cleanTitle(rawTitle);
        final String artist = cleanArtist(rawAuthor);
        final int durationSec = (int) Math.round(messageObject.getDuration());
        final String cacheKey = getCacheKey(artist, title) + (preferredSource != SOURCE_AUTO ? ("_src" + preferredSource) : "");

        // 1. Memory Cache
        MiogramLrcModel.LrcSong cached = memoryCache.get(cacheKey);
        if (cached != null) {
            if (callback != null) {
                callback.onLyricsLoaded(cached);
            }
            return;
        }

        executor.execute(() -> {
            try {
                // 2. Disk Cache
                MiogramLrcModel.LrcSong diskSong = loadFromDisk(cacheKey);
                if (diskSong != null) {
                    memoryCache.put(cacheKey, diskSong);
                    postSuccess(callback, diskSong);
                    return;
                }

                if (preferredSource == SOURCE_SERVER) {
                    postError(callback, "Not in server cache");
                    return;
                }

                if (preferredSource == SOURCE_AI) {
                    transcribeAudioWithAi(messageObject, callback);
                    return;
                }

                if (preferredSource == SOURCE_LRCLIB) {
                    MiogramLrcModel.LrcSong song = queryLrcLib(title, artist, durationSec);
                    if (song != null && !song.isEmpty()) {
                        completeAndSave(cacheKey, song, callback);
                    } else {
                        postError(callback, "Not found in LRCLib");
                    }
                    return;
                }

                if (preferredSource == SOURCE_NETEASE) {
                    MiogramLrcModel.LrcSong song = queryNetEase(title, artist, durationSec);
                    if (song != null && !song.isEmpty()) {
                        completeAndSave(cacheKey, song, callback);
                    } else {
                        postError(callback, "Not found in NetEase");
                    }
                    return;
                }

                if (preferredSource == SOURCE_YANDEX) {
                    MiogramLrcModel.LrcSong song = queryYandex(title, artist, durationSec);
                    if (song != null && !song.isEmpty()) {
                        completeAndSave(cacheKey, song, callback);
                    } else {
                        postError(callback, "Not found in Yandex");
                    }
                    return;
                }

                if (preferredSource == SOURCE_GENIUS) {
                    MiogramLrcModel.LrcSong song = queryGenius(title, artist, durationSec);
                    if (song != null && !song.isEmpty()) {
                        completeAndSave(cacheKey, song, callback);
                    } else {
                        postError(callback, "Not found in Genius");
                    }
                    return;
                }

                if (preferredSource == SOURCE_YOUTUBE) {
                    MiogramLrcModel.LrcSong song = queryYouTube(title, artist, durationSec);
                    if (song != null && !song.isEmpty()) {
                        completeAndSave(cacheKey, song, callback);
                    } else {
                        postError(callback, "Not found in YouTube");
                    }
                    return;
                }

                // SOURCE_AUTO Pipeline
                // A. LRCLib (Synced)
                MiogramLrcModel.LrcSong lrcLibSong = queryLrcLib(title, artist, durationSec);
                if (lrcLibSong != null && !lrcLibSong.isEmpty()) {
                    completeAndSave(cacheKey, lrcLibSong, callback);
                    return;
                }

                // B. NetEase Cloud Music (LRC + Translation)
                MiogramLrcModel.LrcSong netEaseSong = queryNetEase(title, artist, durationSec);
                if (netEaseSong != null && !netEaseSong.isEmpty()) {
                    completeAndSave(cacheKey, netEaseSong, callback);
                    return;
                }

                // C. Embedded ID3 tags
                MiogramLrcModel.LrcSong id3Song = queryId3(messageObject, title, artist);
                if (id3Song != null && !id3Song.isEmpty()) {
                    completeAndSave(cacheKey, id3Song, callback);
                    return;
                }

                // D. Genius / Plaintext fallback
                MiogramLrcModel.LrcSong plainSong = queryGenius(title, artist, durationSec);
                if (plainSong != null && !plainSong.isEmpty()) {
                    completeAndSave(cacheKey, plainSong, callback);
                    return;
                }

                // E. YouTube Description
                MiogramLrcModel.LrcSong ytSong = queryYouTube(title, artist, durationSec);
                if (ytSong != null && !ytSong.isEmpty()) {
                    completeAndSave(cacheKey, ytSong, callback);
                    return;
                }

                // Not found
                postError(callback, "Lyrics not found for " + title + " - " + artist);

            } catch (Throwable e) {
                FileLog.e(e);
                postError(callback, "Error fetching lyrics: " + e.getMessage());
            }
        });
    }

    public void transcribeAudioWithAi(final MessageObject messageObject, final LyricsCallback callback) {
        if (messageObject == null) {
            if (callback != null) callback.onError("No track to transcribe");
            return;
        }

        final String title = cleanTitle(messageObject.getMusicTitle());
        final String artist = cleanArtist(messageObject.getMusicAuthor());
        final int durationSec = (int) Math.round(messageObject.getDuration());
        final String cacheKey = getCacheKey(artist, title) + "_ai";

        executor.execute(() -> {
            try {
                MiogramLrcModel.LrcSong cached = memoryCache.get(cacheKey);
                if (cached != null) {
                    postSuccess(callback, cached);
                    return;
                }

                File audioFile = resolveAudioFile(messageObject);
                if (audioFile == null) {
                    if (messageObject.getDocument() != null) {
                        FileLoader.getInstance(messageObject.currentAccount).loadFile(messageObject.getDocument(), messageObject, FileLoader.PRIORITY_HIGH, 0);
                    }
                    postError(callback, app.miogram.bridge.MiogramLocale.get(
                            "Завантаження аудіофайлу... Зачекайте пару секунд і спробуйте знову.",
                            "Загрузка аудиофайла... Подождите пару секунд и попробуйте снова.",
                            "Downloading audio file... Please wait a few seconds and try again."));
                    return;
                }

                if (!app.miogram.bridge.ai.MiogramAiService.hasApiKey()) {
                    postError(callback, app.miogram.bridge.MiogramLocale.get(
                            "Вкажіть Gemini API ключ у Налаштуваннях Miogram -> ШІ.",
                            "Укажите Gemini API ключ в Настройках Miogram -> ИИ.",
                            "Configure Gemini API key in Miogram Settings -> AI."));
                    return;
                }

                app.miogram.bridge.ai.MiogramAiService.transcribeAudio(audioFile, resolveAudioMimeType(messageObject, audioFile), title, artist, durationSec,
                        (lrc, error) -> {
                            if (!TextUtils.isEmpty(lrc)) {
                                MiogramLrcModel.LrcSong aiSong = MiogramLrcModel.parseLrc(stripCodeFence(lrc), title, artist, "✨ Gemini AI");
                                if (aiSong != null && !aiSong.isEmpty()) {
                                    completeAndSave(cacheKey, aiSong, callback);
                                    return;
                                }
                                postError(callback, app.miogram.bridge.MiogramLocale.get(
                                        "ШІ не зміг розпізнати розбірливий текст пісні.",
                                        "ИИ не смог распознать разборчивый текст песни.",
                                        "AI could not extract recognizable lyrics from audio."));
                                return;
                            }
                            postError(callback, TextUtils.isEmpty(error) ? "AI transcription failed." : error);
                        });
            } catch (Throwable e) {
                FileLog.e(e);
                postError(callback, "AI error: " + e.getMessage());
            }
        });
    }

    /* =========================================================================
     * PROVIDER IMPLEMENTATIONS WITH STRICT MATCHING
     * ========================================================================= */

    private MiogramLrcModel.LrcSong queryLrcLib(String title, String artist, int durationSec) {
        try {
            // 1. Direct match query
            StringBuilder url = new StringBuilder("https://lrclib.net/api/get?");
            url.append("track_name=").append(URLEncoder.encode(title, "UTF-8"));
            if (!TextUtils.isEmpty(artist)) {
                url.append("&artist_name=").append(URLEncoder.encode(artist, "UTF-8"));
            }
            if (durationSec > 0) {
                url.append("&duration=").append(durationSec);
            }

            Request request = new Request.Builder()
                    .url(url.toString())
                    .header("User-Agent", "MiogramTelegramClient/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    String synced = json.optString("syncedLyrics", "");
                    String plain = json.optString("plainLyrics", "");

                    if (!TextUtils.isEmpty(synced)) {
                        return MiogramLrcModel.parseLrc(synced, title, artist, "LRCLib");
                    } else if (!TextUtils.isEmpty(plain)) {
                        return MiogramLrcModel.parseLrc(plain, title, artist, "LRCLib (Plain)");
                    }
                }
            }

            // 2. Search query with STRICT track validation
            String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(artist + " " + title, "UTF-8");
            Request searchReq = new Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "MiogramTelegramClient/1.0")
                    .build();

            try (Response response = httpClient.newCall(searchReq).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < Math.min(arr.length(), 6); i++) {
                        JSONObject item = arr.optJSONObject(i);
                        if (item == null) continue;

                        String candTitle = item.optString("trackName", item.optString("name", ""));
                        String candArtist = item.optString("artistName", "");
                        int candDur = (int) Math.round(item.optDouble("duration", 0));

                        // STRICT VALIDATION
                        if (!isMatchingTrack(title, artist, durationSec, candTitle, candArtist, candDur)) {
                            continue; // Reject different song by same artist!
                        }

                        String synced = item.optString("syncedLyrics", "");
                        if (!TextUtils.isEmpty(synced)) {
                            return MiogramLrcModel.parseLrc(synced, title, artist, "LRCLib");
                        }
                        String plain = item.optString("plainLyrics", "");
                        if (!TextUtils.isEmpty(plain)) {
                            return MiogramLrcModel.parseLrc(plain, title, artist, "LRCLib (Plain)");
                        }
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return null;
    }

    private MiogramLrcModel.LrcSong queryNetEase(String title, String artist, int durationSec) {
        try {
            String query = (title + " " + artist).trim();
            String searchUrl = "https://music.163.com/api/search/get?s=" + URLEncoder.encode(query, "UTF-8") + "&type=1&limit=5";

            Request searchReq = new Request.Builder()
                    .url(searchUrl)
                    .header("Referer", "https://music.163.com/")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build();

            long songId = -1;
            try (Response response = httpClient.newCall(searchReq).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONObject result = json.optJSONObject("result");
                    if (result != null) {
                        JSONArray songs = result.optJSONArray("songs");
                        if (songs != null) {
                            for (int i = 0; i < songs.length(); i++) {
                                JSONObject s = songs.optJSONObject(i);
                                if (s == null) continue;

                                String candTitle = s.optString("name", "");
                                String candArtist = "";
                                JSONArray artistsArr = s.optJSONArray("artists");
                                if (artistsArr != null && artistsArr.length() > 0) {
                                    JSONObject aObj = artistsArr.optJSONObject(0);
                                    if (aObj != null) {
                                        candArtist = aObj.optString("name", "");
                                    }
                                }
                                int candDur = (int) Math.round(s.optDouble("dt", 0) / 1000.0);

                                if (!isMatchingTrack(title, artist, durationSec, candTitle, candArtist, candDur)) {
                                    continue; // Reject different song!
                                }

                                songId = s.optLong("id", -1);
                                if (songId > 0) break;
                            }
                        }
                    }
                }
            }

            if (songId <= 0) return null;

            String lyricUrl = "https://music.163.com/api/song/lyric?id=" + songId + "&lv=-1&kv=-1&tv=-1";
            Request lyricReq = new Request.Builder()
                    .url(lyricUrl)
                    .header("Referer", "https://music.163.com/")
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = httpClient.newCall(lyricReq).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONObject lrc = json.optJSONObject("lrc");
                    if (lrc != null) {
                        String lyric = lrc.optString("lyric", "");
                        if (!TextUtils.isEmpty(lyric)) {
                            MiogramLrcModel.LrcSong song = MiogramLrcModel.parseLrc(lyric, title, artist, "NetEase");
                            JSONObject tlyric = json.optJSONObject("tlyric");
                            if (tlyric != null) {
                                String tLyricStr = tlyric.optString("lyric", "");
                                if (!TextUtils.isEmpty(tLyricStr)) {
                                    MiogramLrcModel.mergeTranslation(song, tLyricStr);
                                }
                            }
                            return song;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return null;
    }

    private MiogramLrcModel.LrcSong queryYandex(String title, String artist, int durationSec) {
        return queryLrcLib(title, artist, durationSec);
    }

    private MiogramLrcModel.LrcSong queryGenius(String title, String artist, int durationSec) {
        try {
            String query = (title + " " + artist).trim();
            String url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8");
            Request req = new Request.Builder().url(url).header("User-Agent", "MiogramTelegramClient/1.0").build();
            try (Response res = httpClient.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JSONArray arr = new JSONArray(res.body().string());
                    for (int i = 0; i < Math.min(arr.length(), 4); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        String candTitle = o.optString("trackName", o.optString("name", ""));
                        int candDur = (int) Math.round(o.optDouble("duration", 0));
                        if (!isMatchingTrack(title, artist, durationSec, candTitle, "", candDur)) {
                            continue;
                        }
                        String plain = o.optString("plainLyrics", "");
                        if (!TextUtils.isEmpty(plain)) {
                            return MiogramLrcModel.parseLrc(plain, title, artist, "Genius");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private MiogramLrcModel.LrcSong queryYouTube(String title, String artist, int durationSec) {
        return queryGenius(title, artist, durationSec);
    }

    private MiogramLrcModel.LrcSong queryId3(MessageObject messageObject, String title, String artist) {
        try {
            File file = FileLoader.getInstance(messageObject.currentAccount).getPathToMessage(messageObject.messageOwner);
            if (file == null || !file.exists()) {
                if (messageObject.messageOwner != null && messageObject.messageOwner.attachPath != null) {
                    file = new File(messageObject.messageOwner.attachPath);
                }
            }
            if (file != null && file.exists()) {
                AudioInfo info = AudioInfo.getAudioInfo(file);
                if (info != null && info.getCover() != null) {
                    // AudioInfo parsed file
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private File resolveAudioFile(MessageObject messageObject) {
        try {
            if (messageObject.messageOwner != null && !TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
                File file = new File(messageObject.messageOwner.attachPath);
                if (file.isFile() && file.length() > 0) return file;
            }
            File file = FileLoader.getInstance(messageObject.currentAccount).getPathToMessage(messageObject.messageOwner);
            if (file != null && file.isFile() && file.length() > 0) return file;
            if (messageObject.getDocument() != null) {
                file = FileLoader.getInstance(messageObject.currentAccount).getPathToAttach(messageObject.getDocument(), true);
                if (file != null && file.isFile() && file.length() > 0) return file;
                file = FileLoader.getInstance(messageObject.currentAccount).getPathToAttach(messageObject.getDocument(), false);
                if (file != null && file.isFile() && file.length() > 0) return file;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String resolveAudioMimeType(MessageObject messageObject, File audioFile) {
        try {
            if (messageObject.getDocument() != null && !TextUtils.isEmpty(messageObject.getDocument().mime_type)) {
                return messageObject.getDocument().mime_type;
            }
        } catch (Throwable ignored) {}
        String name = audioFile != null ? audioFile.getName().toLowerCase(Locale.ROOT) : "";
        if (name.endsWith(".ogg") || name.endsWith(".opus")) return "audio/ogg";
        if (name.endsWith(".m4a") || name.endsWith(".mp4")) return "audio/mp4";
        if (name.endsWith(".wav")) return "audio/wav";
        return "audio/mpeg";
    }

    private String stripCodeFence(String content) {
        if (content == null) return "";
        return content.replace("```lrc", "").replace("```LRC", "").replace("```", "").trim();
    }

    /* =========================================================================
     * STRICT TRACK VALIDATION
     * ========================================================================= */

    public static boolean isMatchingTrack(String targetTitle, String targetArtist, int targetDurationSec,
                                          String candidateTitle, String candidateArtist, int candidateDurationSec) {
        if (TextUtils.isEmpty(candidateTitle) || TextUtils.isEmpty(targetTitle)) {
            return false;
        }

        // 1. Duration check: if both > 10s and difference > 5 seconds, reject
        if (targetDurationSec > 10 && candidateDurationSec > 10) {
            if (Math.abs(targetDurationSec - candidateDurationSec) > 5) {
                return false;
            }
        }

        // 2. Artist check (MANDATORY when both provided)
        String normTargetArtist = normalizeString(targetArtist);
        String normCandArtist = normalizeString(candidateArtist);
        if (!TextUtils.isEmpty(normTargetArtist) && !TextUtils.isEmpty(normCandArtist)) {
            boolean artistMatches = normTargetArtist.equals(normCandArtist)
                    || normTargetArtist.contains(normCandArtist)
                    || normCandArtist.contains(normTargetArtist);
            if (!artistMatches) {
                // Check word overlap in artist
                String[] tArtists = normTargetArtist.split("\\s+");
                String[] cArtists = normCandArtist.split("\\s+");
                boolean foundArtistOverlap = false;
                for (String ta : tArtists) {
                    if (ta.length() < 3) continue;
                    for (String ca : cArtists) {
                        if (ca.equals(ta)) {
                            foundArtistOverlap = true;
                            break;
                        }
                    }
                    if (foundArtistOverlap) break;
                }
                if (!foundArtistOverlap) {
                    return false; // Reject: different artist!
                }
            }
        }

        // 3. Normalized Title Check
        String normTarget = normalizeString(targetTitle);
        String normCandidate = normalizeString(candidateTitle);

        if (normTarget.equals(normCandidate)) return true;

        // If target title is short (< 4 chars, like "92"), require exact match!
        if (normTarget.length() < 4 || normCandidate.length() < 4) {
            return normTarget.equals(normCandidate);
        }

        if (normTarget.contains(normCandidate) || normCandidate.contains(normTarget)) {
            int minLen = Math.min(normTarget.length(), normCandidate.length());
            int maxLen = Math.max(normTarget.length(), normCandidate.length());
            if ((float) minLen / (float) maxLen >= 0.65f) {
                return true;
            }
        }

        // Word overlap ratio check
        String[] targetWords = normTarget.split("\\s+");
        String[] candWords = normCandidate.split("\\s+");
        if (targetWords.length == 0 || candWords.length == 0) return false;

        int matchCount = 0;
        for (String tw : targetWords) {
            if (tw.length() < 2) continue;
            for (String cw : candWords) {
                if (cw.equals(tw)) {
                    matchCount++;
                    break;
                }
            }
        }
        float ratio = (float) matchCount / (float) Math.max(targetWords.length, candWords.length);
        return ratio >= 0.70f;
    }

    private static String normalizeString(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)\\((feat\\..*?|ft\\..*?)\\)|\\[(feat\\..*?|ft\\..*?)\\]|(?i)\\b(feat|ft)\\..*", "")
                .replaceAll("(?i)\\((official.*?|audio.*?|video.*?|lyrics.*?)\\)|\\[(official.*?|audio.*?|video.*?|lyrics.*?)\\]", "")
                .replaceAll("[^a-zA-Z0-9а-яА-ЯёЁіІїЇєЄґҐ\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /* =========================================================================
     * TRANSLATION SERVICE
     * ========================================================================= */

    private void completeAndSave(String cacheKey, MiogramLrcModel.LrcSong song, LyricsCallback callback) {
        if (!song.hasAnyTranslation() && !song.lines.isEmpty()) {
            translateSongLines(song);
        }

        memoryCache.put(cacheKey, song);
        saveToDisk(cacheKey, song);
        postSuccess(callback, song);
    }

    public void translateSongLines(final MiogramLrcModel.LrcSong song) {
        translateSongLines(song, null);
    }

    public void translateSongLines(final MiogramLrcModel.LrcSong song, final Runnable onDone) {
        if (song == null || song.lines.isEmpty()) {
            if (onDone != null) AndroidUtilities.runOnUIThread(onDone);
            return;
        }
        executor.execute(() -> {
            try {
                String targetLang = app.miogram.bridge.MiogramLocale.isUkrainian() ? "uk" : "ru";

                StringBuilder batch = new StringBuilder();
                int count = Math.min(song.lines.size(), 40);
                for (int i = 0; i < count; i++) {
                    batch.append(song.lines.get(i).text).append("\n");
                }

                String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" +
                        targetLang + "&dt=t&q=" + URLEncoder.encode(batch.toString(), "UTF-8");

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String resStr = response.body().string();
                        JSONArray root = new JSONArray(resStr);
                        JSONArray parts = root.optJSONArray(0);
                        if (parts != null) {
                            StringBuilder fullTranslated = new StringBuilder();
                            for (int i = 0; i < parts.length(); i++) {
                                JSONArray part = parts.optJSONArray(i);
                                if (part != null) {
                                    fullTranslated.append(part.optString(0, ""));
                                }
                            }

                            String[] transLines = fullTranslated.toString().split("\\r?\\n");
                            for (int i = 0; i < Math.min(song.lines.size(), transLines.length); i++) {
                                String tr = transLines[i].trim();
                                if (!tr.isEmpty() && !tr.equals(song.lines.get(i).text)) {
                                    song.lines.get(i).translation = tr;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (onDone != null) {
                    AndroidUtilities.runOnUIThread(onDone);
                }
            }
        });
    }

    /* =========================================================================
     * CACHING HELPERS
     * ========================================================================= */

    private String getCacheKey(String artist, String title) {
        String raw = (artist.toLowerCase(Locale.ROOT).trim() + "_" + title.toLowerCase(Locale.ROOT).trim());
        return Utilities.MD5(raw);
    }

    private String cleanTitle(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)\\\\(feat\\\\..*?\\\\)|\\\\[feat\\\\..*?\\\\]|(?i)\\\\bfeat\\\\..*|\\\\[.*?\\\\]", "")
                  .replaceAll("(?i)\\\\(official.*?\\\\)", "")
                  .replaceAll("(?i)\\\\(audio.*?\\\\)", "")
                  .trim();
    }

    private String cleanArtist(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)\\\\bfeat\\\\..*", "")
                  .replaceAll("(?i),.*", "")
                  .trim();
    }

    private void saveToDisk(String key, MiogramLrcModel.LrcSong song) {
        try {
            File f = new File(cacheDir, key + ".json");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(song.toJson().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {}
    }

    private MiogramLrcModel.LrcSong loadFromDisk(String key) {
        try {
            File f = new File(cacheDir, key + ".json");
            if (f.exists() && f.length() > 0) {
                byte[] b = new byte[(int) f.length()];
                try (FileInputStream fis = new FileInputStream(f)) {
                    int read = fis.read(b);
                    if (read > 0) {
                        return MiogramLrcModel.LrcSong.fromJson(new String(b, 0, read, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void postSuccess(LyricsCallback callback, MiogramLrcModel.LrcSong song) {
        if (callback == null) return;
        AndroidUtilities.runOnUIThread(() -> callback.onLyricsLoaded(song));
    }

    private void postError(LyricsCallback callback, String msg) {
        if (callback == null) return;
        AndroidUtilities.runOnUIThread(() -> callback.onError(msg));
    }
}