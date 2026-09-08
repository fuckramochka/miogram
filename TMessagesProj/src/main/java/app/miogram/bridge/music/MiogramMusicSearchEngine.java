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
        final AtomicInteger pendingEngines = new AtomicInteger(4);

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
