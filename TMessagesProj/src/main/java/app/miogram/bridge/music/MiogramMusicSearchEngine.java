package app.miogram.bridge.music;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MiogramMusicSearchEngine {

    public interface SearchCallback {
        void onResults(List<MiogramMusicTrack> tracks, boolean isFinal);
        void onError(String error);
    }

    public interface InstallCallback {
        void onProgress(float progress);
        void onSuccess(File localFile);
        void onError(String error);
    }

    /**
     * Parallel search across Telegram Cloud Audio, Deezer HQ, and iTunes.
     */
    public static void searchAll(String query, int currentAccount, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            if (callback != null) callback.onResults(Collections.emptyList(), true);
            return;
        }

        final String q = query.trim();
        final List<MiogramMusicTrack> aggregatedResults = Collections.synchronizedList(new ArrayList<>());
        final Set<String> seenSignatures = Collections.synchronizedSet(new HashSet<>());
        final AtomicInteger pendingEngines = new AtomicInteger(6);

        // 1. Search Telegram Global Cloud
        searchTelegram(q, currentAccount, new SearchCallback() {
            @Override
            public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                if (tracks != null) {
                    for (MiogramMusicTrack t : tracks) {
                        String sig = normalize(t.artist) + "|" + normalize(t.title);
                        if (seenSignatures.add(sig)) {
                            aggregatedResults.add(t);
                        }
                    }
                }
                checkFinal();
            }

            @Override
            public void onError(String error) {
                checkFinal();
            }

            private void checkFinal() {
                if (pendingEngines.decrementAndGet() == 0) {
                    AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                }
            }
        });

        // 2. Search Deezer API
        Utilities.globalQueue.postRunnable(() -> {
            searchDeezer(q, new SearchCallback() {
                @Override
                public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                    if (tracks != null) {
                        for (MiogramMusicTrack t : tracks) {
                            String sig = normalize(t.artist) + "|" + normalize(t.title);
                            if (seenSignatures.add(sig)) {
                                aggregatedResults.add(t);
                            }
                        }
                    }
                    checkFinal();
                }

                @Override
                public void onError(String error) {
                    checkFinal();
                }

                private void checkFinal() {
                    if (pendingEngines.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                    }
                }
            });
        });

        // 3. Search iTunes Store API
        Utilities.globalQueue.postRunnable(() -> {
            searchItunes(q, new SearchCallback() {
                @Override
                public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                    if (tracks != null) {
                        for (MiogramMusicTrack t : tracks) {
                            String sig = normalize(t.artist) + "|" + normalize(t.title);
                            if (seenSignatures.add(sig)) {
                                aggregatedResults.add(t);
                            }
                        }
                    }
                    checkFinal();
                }

                @Override
                public void onError(String error) {
                    checkFinal();
                }

                private void checkFinal() {
                    if (pendingEngines.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                    }
                }
            });
        });

        // 4. Search Jamendo API
        Utilities.globalQueue.postRunnable(() -> {
            searchJamendo(q, new SearchCallback() {
                @Override
                public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                    if (tracks != null) {
                        for (MiogramMusicTrack t : tracks) {
                            String sig = normalize(t.artist) + "|" + normalize(t.title);
                            if (seenSignatures.add(sig)) {
                                aggregatedResults.add(t);
                            }
                        }
                    }
                    checkFinal();
                }

                @Override
                public void onError(String error) {
                    checkFinal();
                }

                private void checkFinal() {
                    if (pendingEngines.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                    }
                }
            });
        });

        // 5. Search Audius Hi-Fi API
        Utilities.globalQueue.postRunnable(() -> {
            searchAudius(q, new SearchCallback() {
                @Override
                public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                    if (tracks != null) {
                        for (MiogramMusicTrack t : tracks) {
                            String sig = normalize(t.artist) + "|" + normalize(t.title);
                            if (seenSignatures.add(sig)) {
                                aggregatedResults.add(t);
                            }
                        }
                    }
                    checkFinal();
                }

                @Override
                public void onError(String error) {
                    checkFinal();
                }

                private void checkFinal() {
                    if (pendingEngines.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                    }
                }
            });
        });

        // 6. Search YouTube Music InnerTube API
        Utilities.globalQueue.postRunnable(() -> {
            searchYouTubeMusic(q, new SearchCallback() {
                @Override
                public void onResults(List<MiogramMusicTrack> tracks, boolean isFinal) {
                    if (tracks != null) {
                        for (MiogramMusicTrack t : tracks) {
                            String sig = normalize(t.artist) + "|" + normalize(t.title);
                            if (seenSignatures.add(sig)) {
                                aggregatedResults.add(t);
                            }
                        }
                    }
                    checkFinal();
                }

                @Override
                public void onError(String error) {
                    checkFinal();
                }

                private void checkFinal() {
                    if (pendingEngines.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), true));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> callback.onResults(new ArrayList<>(aggregatedResults), false));
                    }
                }
            });
        });
    }

    /**
     * Telegram Global Music Search.
     */
    public static void searchTelegram(String query, int currentAccount, SearchCallback callback) {
        final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
        req.filter = new TLRPC.TL_inputMessagesFilterMusic();
        req.q = query;
        req.limit = 35;
        req.offset_rate = 0;
        req.offset_id = 0;
        req.offset_peer = new TLRPC.TL_inputPeerEmpty();

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
            if (err != null || res == null) {
                AndroidUtilities.runOnUIThread(() -> callback.onError(err != null ? err.text : "Telegram search failed"));
                return;
            }

            final List<MiogramMusicTrack> results = new ArrayList<>();
            if (res instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages msgs = (TLRPC.messages_Messages) res;
                MessagesController.getInstance(currentAccount).putUsers(msgs.users, false);
                MessagesController.getInstance(currentAccount).putChats(msgs.chats, false);

                for (TLRPC.Message message : msgs.messages) {
                    MessageObject msgObj = new MessageObject(currentAccount, message, false, true);
                    TLRPC.Document doc = msgObj.getDocument();
                    if (doc == null) continue;

                    TLRPC.TL_documentAttributeAudio attr = null;
                    for (int i = 0; i < doc.attributes.size(); i++) {
                        if (doc.attributes.get(i) instanceof TLRPC.TL_documentAttributeAudio) {
                            attr = (TLRPC.TL_documentAttributeAudio) doc.attributes.get(i);
                            break;
                        }
                    }

                    if (attr != null) {
                        MiogramMusicTrack track = new MiogramMusicTrack();
                        track.id = "tg_" + msgObj.getId() + "_" + msgObj.getDialogId();
                        track.title = attr.title != null ? attr.title : msgObj.getDocumentName();
                        track.artist = attr.performer != null ? attr.performer : "Telegram Audio";
                        track.durationSeconds = (int) attr.duration;
                        track.fileSize = doc.size;
                        track.source = MiogramMusicTrack.Source.TELEGRAM;
                        track.telegramMessage = msgObj;
                        results.add(track);
                    }
                }
            }

            AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
        });
    }

    /**
     * Deezer Direct API Search.
     */
    public static void searchDeezer(String query, SearchCallback callback) {
        HttpURLConnection conn = null;
        try {
            String urlStr = "https://api.deezer.com/search?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=25";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray data = root.optJSONArray("data");
                List<MiogramMusicTrack> results = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        MiogramMusicTrack track = new MiogramMusicTrack();
                        track.id = "deezer_" + item.optLong("id");
                        track.title = item.optString("title");
                        JSONObject artistObj = item.optJSONObject("artist");
                        track.artist = artistObj != null ? artistObj.optString("name") : "";
                        JSONObject albumObj = item.optJSONObject("album");
                        if (albumObj != null) {
                            track.album = albumObj.optString("title");
                            track.coverUrl = albumObj.optString("cover_medium");
                        }
                        track.durationSeconds = item.optInt("duration");
                        track.streamUrl = item.optString("preview");
                        track.downloadUrl = track.streamUrl;
                        track.source = MiogramMusicTrack.Source.DEEZER;
                        results.add(track);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.onError("Deezer HTTP " + code));
            }
        } catch (Throwable t) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(t.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * iTunes Search API.
     */
    public static void searchItunes(String query, SearchCallback callback) {
        HttpURLConnection conn = null;
        try {
            String urlStr = "https://itunes.apple.com/search?term=" + URLEncoder.encode(query, "UTF-8") + "&entity=song&limit=25";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray resultsArr = root.optJSONArray("results");
                List<MiogramMusicTrack> results = new ArrayList<>();
                if (resultsArr != null) {
                    for (int i = 0; i < resultsArr.length(); i++) {
                        JSONObject item = resultsArr.getJSONObject(i);
                        MiogramMusicTrack track = new MiogramMusicTrack();
                        track.id = "itunes_" + item.optLong("trackId");
                        track.title = item.optString("trackName");
                        track.artist = item.optString("artistName");
                        track.album = item.optString("collectionName");
                        track.durationSeconds = (int) (item.optLong("trackTimeMillis") / 1000L);
                        String cover = item.optString("artworkUrl100");
                        if (cover != null && cover.contains("100x100")) {
                            cover = cover.replace("100x100", "600x600");
                        }
                        track.coverUrl = cover;
                        track.streamUrl = item.optString("previewUrl");
                        track.downloadUrl = track.streamUrl;
                        track.source = MiogramMusicTrack.Source.ITUNES;
                        results.add(track);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.onError("iTunes HTTP " + code));
            }
        } catch (Throwable t) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(t.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Jamendo API Search.
     */
    public static void searchJamendo(String query, SearchCallback callback) {
        HttpURLConnection conn = null;
        try {
            String urlStr = "https://api.jamendo.com/v3.0/tracks/?client_id=56d30c95&format=json&limit=25&search=" + URLEncoder.encode(query, "UTF-8") + "&include=musicinfo";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray resultsArr = root.optJSONArray("results");
                List<MiogramMusicTrack> results = new ArrayList<>();
                if (resultsArr != null) {
                    for (int i = 0; i < resultsArr.length(); i++) {
                        JSONObject item = resultsArr.getJSONObject(i);
                        MiogramMusicTrack track = new MiogramMusicTrack();
                        track.id = "jamendo_" + item.optString("id");
                        track.title = item.optString("name");
                        track.artist = item.optString("artist_name");
                        track.album = item.optString("album_name");
                        track.durationSeconds = item.optInt("duration");
                        track.coverUrl = item.optString("image");
                        track.streamUrl = item.optString("audio");
                        String dl = item.optString("audiodownload");
                        track.downloadUrl = (dl != null && !dl.isEmpty()) ? dl : track.streamUrl;
                        track.source = MiogramMusicTrack.Source.JAMENDO;
                        results.add(track);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.onError("Jamendo HTTP " + code));
            }
        } catch (Throwable t) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(t.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Audius Direct API Search (Decentralized Hi-Fi 320kbps streams).
     */
    public static void searchAudius(String query, SearchCallback callback) {
        HttpURLConnection conn = null;
        try {
            String urlStr = "https://discoveryprovider.audius.co/v1/tracks/search?query=" + URLEncoder.encode(query, "UTF-8") + "&app_name=miogram";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray data = root.optJSONArray("data");
                List<MiogramMusicTrack> results = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        String trackId = item.optString("id");
                        if (trackId == null || trackId.isEmpty()) continue;

                        MiogramMusicTrack track = new MiogramMusicTrack();
                        track.id = "audius_" + trackId;
                        track.title = item.optString("title");
                        JSONObject userObj = item.optJSONObject("user");
                        track.artist = userObj != null ? userObj.optString("name") : "Audius Artist";
                        track.durationSeconds = item.optInt("duration");
                        JSONObject artwork = item.optJSONObject("artwork");
                        if (artwork != null) {
                            track.coverUrl = artwork.optString("150x150");
                            if (track.coverUrl == null || track.coverUrl.isEmpty()) {
                                track.coverUrl = artwork.optString("480x480");
                            }
                        }
                        track.streamUrl = "https://discoveryprovider.audius.co/v1/tracks/" + trackId + "/stream?app_name=miogram";
                        track.downloadUrl = track.streamUrl;
                        track.source = MiogramMusicTrack.Source.AUDIUS;
                        results.add(track);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.onError("Audius HTTP " + code));
            }
        } catch (Throwable t) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(t.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * YouTube Music Search (InnerTube API via WEB_REMIX client).
     */
    public static void searchYouTubeMusic(String query, SearchCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://music.youtube.com/youtubei/v1/search");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://music.youtube.com/");

            JSONObject contextObj = new JSONObject();
            JSONObject clientObj = new JSONObject();
            clientObj.put("clientName", "WEB_REMIX");
            clientObj.put("clientVersion", "1.20230101.01.00");
            clientObj.put("hl", "uk");
            clientObj.put("gl", "UA");
            contextObj.put("client", clientObj);

            JSONObject payload = new JSONObject();
            payload.put("context", contextObj);
            payload.put("query", query);
            payload.put("params", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D");

            byte[] postData = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(postData);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                List<MiogramMusicTrack> results = new ArrayList<>();

                JSONObject contents = root.optJSONObject("contents");
                if (contents != null) {
                    JSONObject tabbed = contents.optJSONObject("tabbedSearchResultsRenderer");
                    if (tabbed != null) {
                        JSONArray tabs = tabbed.optJSONArray("tabs");
                        if (tabs != null && tabs.length() > 0) {
                            JSONObject tab = tabs.getJSONObject(0);
                            JSONObject tabRenderer = tab.optJSONObject("tabRenderer");
                            if (tabRenderer != null) {
                                JSONObject tabContent = tabRenderer.optJSONObject("content");
                                if (tabContent != null) {
                                    JSONObject sectionList = tabContent.optJSONObject("sectionListRenderer");
                                    if (sectionList != null) {
                                        JSONArray secContents = sectionList.optJSONArray("contents");
                                        if (secContents != null) {
                                            for (int s = 0; s < secContents.length(); s++) {
                                                JSONObject sec = secContents.getJSONObject(s);
                                                JSONObject shelf = sec.optJSONObject("musicShelfRenderer");
                                                if (shelf == null) continue;
                                                JSONArray items = shelf.optJSONArray("contents");
                                                if (items == null) continue;

                                                for (int i = 0; i < items.length(); i++) {
                                                    JSONObject item = items.getJSONObject(i);
                                                    JSONObject r = item.optJSONObject("musicResponsiveListItemRenderer");
                                                    if (r == null) continue;

                                                    String videoId = null;
                                                    JSONObject playlistItemData = r.optJSONObject("playlistItemData");
                                                    if (playlistItemData != null) {
                                                        videoId = playlistItemData.optString("videoId", null);
                                                    }
                                                    if (videoId == null || videoId.isEmpty()) {
                                                        JSONObject overlay = r.optJSONObject("overlay");
                                                        if (overlay != null) {
                                                            JSONObject thumbOverlay = overlay.optJSONObject("musicItemThumbnailOverlayRenderer");
                                                            if (thumbOverlay != null) {
                                                                JSONObject content = thumbOverlay.optJSONObject("content");
                                                                if (content != null) {
                                                                    JSONObject playBtn = content.optJSONObject("musicPlayButtonRenderer");
                                                                    if (playBtn != null) {
                                                                        JSONObject nav = playBtn.optJSONObject("playNavigationEndpoint");
                                                                        if (nav != null) {
                                                                            JSONObject watch = nav.optJSONObject("watchEndpoint");
                                                                            if (watch != null) {
                                                                                videoId = watch.optString("videoId", null);
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    JSONArray flexCols = r.optJSONArray("flexColumns");
                                                    if (flexCols == null || flexCols.length() == 0) continue;

                                                    // Column 0: Title
                                                    String trackTitle = "";
                                                    JSONObject c0 = flexCols.getJSONObject(0);
                                                    JSONObject col0Renderer = c0.optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                                                    if (col0Renderer != null) {
                                                        JSONObject textObj = col0Renderer.optJSONObject("text");
                                                        if (textObj != null) {
                                                            JSONArray runs = textObj.optJSONArray("runs");
                                                            if (runs != null && runs.length() > 0) {
                                                                JSONObject run0 = runs.getJSONObject(0);
                                                                trackTitle = run0.optString("text", "");
                                                                if ((videoId == null || videoId.isEmpty()) && run0.has("navigationEndpoint")) {
                                                                    JSONObject nav = run0.optJSONObject("navigationEndpoint");
                                                                    if (nav != null) {
                                                                        JSONObject watch = nav.optJSONObject("watchEndpoint");
                                                                        if (watch != null) {
                                                                            videoId = watch.optString("videoId", null);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (videoId == null || videoId.isEmpty()) continue;
                                                    if (trackTitle.isEmpty()) trackTitle = "YouTube Track";

                                                    // Column 1: Artist, Album, Duration
                                                    String artistName = "YouTube Music";
                                                    String albumName = "";
                                                    int durationSec = 0;

                                                    if (flexCols.length() > 1) {
                                                        JSONObject c1 = flexCols.getJSONObject(1);
                                                        JSONObject col1Renderer = c1.optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                                                        if (col1Renderer != null) {
                                                            JSONObject textObj = col1Renderer.optJSONObject("text");
                                                            if (textObj != null) {
                                                                JSONArray runs = textObj.optJSONArray("runs");
                                                                if (runs != null) {
                                                                    List<String> textParts = new ArrayList<>();
                                                                    for (int runIdx = 0; runIdx < runs.length(); runIdx++) {
                                                                        String part = runs.getJSONObject(runIdx).optString("text", "").trim();
                                                                        if (!part.isEmpty() && !part.equals("•")) {
                                                                            textParts.add(part);
                                                                        }
                                                                    }
                                                                    if (!textParts.isEmpty()) {
                                                                        artistName = textParts.get(0);
                                                                        String lastPart = textParts.get(textParts.size() - 1);
                                                                        if (lastPart.contains(":")) {
                                                                            String[] timeParts = lastPart.split(":");
                                                                            try {
                                                                                if (timeParts.length == 2) {
                                                                                    durationSec = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);
                                                                                } else if (timeParts.length == 3) {
                                                                                    durationSec = Integer.parseInt(timeParts[0]) * 3600 + Integer.parseInt(timeParts[1]) * 60 + Integer.parseInt(timeParts[2]);
                                                                                }
                                                                                textParts.remove(textParts.size() - 1);
                                                                            } catch (Throwable ignore) {}
                                                                        }
                                                                        if (textParts.size() > 1) {
                                                                            albumName = textParts.get(1);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Cover Art
                                                    String coverUrl = null;
                                                    JSONObject thumbObj = r.optJSONObject("thumbnail");
                                                    if (thumbObj != null) {
                                                        JSONObject musicThumb = thumbObj.optJSONObject("musicThumbnailRenderer");
                                                        if (musicThumb != null) {
                                                            JSONObject thumbSub = musicThumb.optJSONObject("thumbnail");
                                                            if (thumbSub != null) {
                                                                JSONArray thumbs = thumbSub.optJSONArray("thumbnails");
                                                                if (thumbs != null && thumbs.length() > 0) {
                                                                    coverUrl = thumbs.getJSONObject(thumbs.length() - 1).optString("url");
                                                                }
                                                            }
                                                        }
                                                    }

                                                    MiogramMusicTrack track = new MiogramMusicTrack();
                                                    track.id = "ytm_" + videoId;
                                                    track.title = trackTitle;
                                                    track.artist = artistName;
                                                    track.album = albumName;
                                                    track.durationSeconds = durationSec;
                                                    track.coverUrl = coverUrl;
                                                    track.streamUrl = "https://music.youtube.com/watch?v=" + videoId;
                                                    track.downloadUrl = track.streamUrl;
                                                    track.source = MiogramMusicTrack.Source.YOUTUBE_MUSIC;
                                                    results.add(track);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResults(results, true));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.onError("YouTube Music HTTP " + code));
            }
        } catch (Throwable t) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(t.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static File getTargetMusicDir(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Miogram");
        if (!dir.exists()) dir.mkdirs();
        try {
            File pub = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Miogram");
            if (pub.exists() || pub.mkdirs()) {
                dir = pub;
            }
        } catch (Throwable ignore) {}
        return dir;
    }

    /**
     * High-Speed Quick Install / Download:
     * 1. Downloads file into Music/Miogram on device.
     * 2. Automatically saves to user's Telegram Saved Messages so it's in their permanent cloud library.
     */
    public static void fastInstallTrack(Context context, MiogramMusicTrack track, int currentAccount, InstallCallback callback) {
        if (track == null) {
            if (callback != null) callback.onError("Empty track");
            return;
        }

        track.isDownloading = true;

        if (track.telegramMessage != null) {
            // Telegram high-speed CDN download
            TLRPC.Document doc = track.telegramMessage.getDocument();
            if (doc != null) {
                final File targetDir = getTargetMusicDir(context);

                FileLoader.getInstance(currentAccount).loadFile(doc, track.telegramMessage, FileLoader.PRIORITY_HIGH, 0);

                // Auto-save to user's Saved Messages (permanent cloud sync)
                long myUserId = UserConfig.getInstance(currentAccount).getClientUserId();
                ArrayList<MessageObject> forwardList = new ArrayList<>();
                forwardList.add(track.telegramMessage);
                SendMessagesHelper.getInstance(currentAccount).sendMessage(forwardList, myUserId, false, false, true, 0, 0L);

                Utilities.globalQueue.postRunnable(() -> {
                    File attachFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, true);
                    int timeout = 0;
                    while ((attachFile == null || !attachFile.exists()) && timeout < 120) {
                        try {
                            Thread.sleep(500);
                            timeout++;
                        } catch (Exception ignored) {
                        }
                        attachFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, true);
                    }

                    if (attachFile != null && attachFile.exists()) {
                        String cleanName = sanitizeFilename(track.getDisplayArtist() + " - " + track.getDisplayTitle() + ".mp3");
                        File destFile = new File(targetDir, cleanName);
                        try {
                            AndroidUtilities.copyFile(attachFile, destFile);

                            // Scan into Android MediaStore
                            MediaScannerConnection.scanFile(context, new String[]{destFile.getAbsolutePath()}, new String[]{"audio/mpeg"}, null);

                            AndroidUtilities.runOnUIThread(() -> {
                                track.isDownloading = false;
                                track.isInstalled = true;
                                track.localFile = destFile;
                                if (callback != null) callback.onSuccess(destFile);
                            });
                        } catch (Throwable t) {
                            AndroidUtilities.runOnUIThread(() -> {
                                track.isDownloading = false;
                                if (callback != null) callback.onError(t.getMessage());
                            });
                        }
                    } else {
                        AndroidUtilities.runOnUIThread(() -> {
                            track.isDownloading = false;
                            if (callback != null) callback.onError("Download timed out");
                        });
                    }
                });
                return;
            }
        }

        // Direct / Deezer / iTunes URL download
        if (track.downloadUrl != null && !track.downloadUrl.isEmpty()) {
            Utilities.globalQueue.postRunnable(() -> {
                HttpURLConnection conn = null;
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    final File targetDir = getTargetMusicDir(context);

                    String cleanName = sanitizeFilename(track.getDisplayArtist() + " - " + track.getDisplayTitle() + ".mp3");
                    File destFile = new File(targetDir, cleanName);

                    URL url = new URL(track.downloadUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(12000);
                    conn.connect();

                    int total = conn.getContentLength();
                    is = conn.getInputStream();
                    fos = new FileOutputStream(destFile);

                    byte[] buf = new byte[8192];
                    int read;
                    int downloaded = 0;
                    while ((read = is.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                        downloaded += read;
                        if (total > 0 && callback != null) {
                            float progress = (float) downloaded / total;
                            AndroidUtilities.runOnUIThread(() -> callback.onProgress(progress));
                        }
                    }
                    fos.flush();

                    // Scan MediaStore
                    MediaScannerConnection.scanFile(context, new String[]{destFile.getAbsolutePath()}, new String[]{"audio/mpeg"}, null);

                    AndroidUtilities.runOnUIThread(() -> {
                        track.isDownloading = false;
                        track.isInstalled = true;
                        track.localFile = destFile;
                        if (callback != null) callback.onSuccess(destFile);
                    });
                } catch (Throwable t) {
                    AndroidUtilities.runOnUIThread(() -> {
                        track.isDownloading = false;
                        if (callback != null) callback.onError(t.getMessage());
                    });
                } finally {
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                    try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                    if (conn != null) conn.disconnect();
                }
            });
            return;
        }

        track.isDownloading = false;
        if (callback != null) callback.onError("No stream or download link available");
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9а-яіїєґ]", "").trim();
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
