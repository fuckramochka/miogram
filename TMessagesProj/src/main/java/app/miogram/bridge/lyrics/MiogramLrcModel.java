package app.miogram.bridge.lyrics;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance LRC and synchronized lyrics data model and parser.
 * Supports standard [mm:ss.xx] timestamps, multiple timestamps per line,
 * offset tags, metadata tags, and bilingual translation merging.
 */
public class MiogramLrcModel {

    private static final Pattern TIME_TAG_PATTERN = Pattern.compile("(?:\\[|<)(\\d{1,2}):(\\d{1,2})(?:[\\.:](\\d{1,3}))?(?:\\]|>)");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE);

    public static class LrcLine implements Comparable<LrcLine> {
        public final long timeMs;
        public String text;
        public String translation;

        public LrcLine(long timeMs, String text) {
            this(timeMs, text, null);
        }

        public LrcLine(long timeMs, String text, String translation) {
            this.timeMs = timeMs;
            this.text = text != null ? text.trim() : "";
            this.translation = translation != null ? translation.trim() : null;
        }

        public boolean hasTranslation() {
            return translation != null && !translation.trim().isEmpty();
        }

        @Override
        public int compareTo(LrcLine o) {
            return Long.compare(this.timeMs, o.timeMs);
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("t", timeMs);
                obj.put("s", text);
                if (translation != null) {
                    obj.put("tr", translation);
                }
            } catch (Throwable ignored) {}
            return obj;
        }

        public static LrcLine fromJson(JSONObject obj) {
            long t = obj.optLong("t", 0L);
            String s = obj.optString("s", "");
            String tr = obj.has("tr") ? obj.optString("tr", null) : null;
            return new LrcLine(t, s, tr);
        }
    }

    public static class LrcSong {
        public final String title;
        public final String artist;
        public final String source;
        public boolean isSynced;
        public String plainLyrics;
        public final List<LrcLine> lines = new ArrayList<>();

        public LrcSong(String title, String artist, String source, boolean isSynced) {
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.source = source != null ? source : "Auto";
            this.isSynced = isSynced;
        }

        public boolean isEmpty() {
            return lines.isEmpty() && (plainLyrics == null || plainLyrics.trim().isEmpty());
        }

        public boolean hasAnyTranslation() {
            for (LrcLine line : lines) {
                if (line.hasTranslation()) return true;
            }
            return false;
        }

        /**
         * Finds active line index for current playback time using binary search.
         * Handles intros and long instrumental pauses accurately.
         */
        public int findLineIndex(long currentMs) {
            if (lines.isEmpty()) return -1;
            long firstTime = lines.get(0).timeMs;
            if (currentMs < firstTime) {
                // If we are within 1.2 seconds of the first line, highlight it, otherwise it's still intro
                return (firstTime - currentMs <= 1200L) ? 0 : -1;
            }

            int low = 0;
            int high = lines.size() - 1;
            int best = 0;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                long midTime = lines.get(mid).timeMs;
                if (midTime <= currentMs) {
                    best = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            long currentLineTime = lines.get(best).timeMs;
            long elapsedSinceLine = currentMs - currentLineTime;

            if (best < lines.size() - 1) {
                long nextLineTime = lines.get(best + 1).timeMs;
                long gapToNext = nextLineTime - currentLineTime;
                // If there's a long break (e.g. guitar solo / bridge > 6s) and we're 5s past current line, deactivate
                if (gapToNext > 6000L && elapsedSinceLine > 5000L) {
                    return -1;
                }
            } else {
                // Last line in song: deactivate if more than 7s passed
                if (elapsedSinceLine > 7000L) {
                    return -1;
                }
            }

            return best;
        }

        public String toJson() {
            JSONObject root = new JSONObject();
            try {
                root.put("title", title);
                root.put("artist", artist);
                root.put("source", source);
                root.put("isSynced", isSynced);
                if (plainLyrics != null) {
                    root.put("plainLyrics", plainLyrics);
                }
                JSONArray arr = new JSONArray();
                for (LrcLine line : lines) {
                    arr.put(line.toJson());
                }
                root.put("lines", arr);
            } catch (Throwable ignored) {}
            return root.toString();
        }

        public static LrcSong fromJson(String jsonStr) {
            if (jsonStr == null || jsonStr.trim().isEmpty()) return null;
            try {
                JSONObject root = new JSONObject(jsonStr);
                String title = root.optString("title", "");
                String artist = root.optString("artist", "");
                String source = root.optString("source", "Cache");
                boolean isSynced = root.optBoolean("isSynced", true);
                LrcSong song = new LrcSong(title, artist, source, isSynced);
                song.plainLyrics = root.optString("plainLyrics", null);

                JSONArray arr = root.optJSONArray("lines");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.optJSONObject(i);
                        if (obj != null) {
                            song.lines.add(LrcLine.fromJson(obj));
                        }
                    }
                }
                return song;
            } catch (Throwable e) {
                return null;
            }
        }
    }

    /**
     * Parses standard LRC text content into an LrcSong object.
     */
    public static LrcSong parseLrc(String content, String title, String artist, String source) {
        LrcSong song = new LrcSong(title, artist, source, true);
        if (content == null || content.trim().isEmpty()) {
            return song;
        }

        long offset = 0;
        Matcher offsetMatcher = OFFSET_PATTERN.matcher(content);
        if (offsetMatcher.find()) {
            try {
                offset = Long.parseLong(offsetMatcher.group(1));
            } catch (Throwable ignored) {}
        }

        String[] rawLines = content.split("\\r?\\n");
        List<LrcLine> parsedLines = new ArrayList<>();

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            // Ignore header tags like [ti:], [ar:], [al:], [by:], [re:], [ve:]
            if (line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:") ||
                line.startsWith("[by:") || line.startsWith("[offset:") || line.startsWith("[re:") ||
                line.startsWith("[ve:")) {
                continue;
            }

            Matcher matcher = TIME_TAG_PATTERN.matcher(line);
            List<Long> times = new ArrayList<>();
            int lastMatchEnd = 0;

            while (matcher.find()) {
                try {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    String msStr = matcher.group(3);
                    long ms = 0;
                    if (msStr != null && !msStr.isEmpty()) {
                        ms = Long.parseLong(msStr);
                        if (msStr.length() == 1) {
                            ms *= 100;
                        } else if (msStr.length() == 2) {
                            ms *= 10;
                        }
                    }
                    long totalMs = (min * 60L * 1000L) + (sec * 1000L) + ms;
                    times.add(totalMs);
                    lastMatchEnd = matcher.end();
                } catch (Throwable ignored) {}
            }

            if (!times.isEmpty()) {
                String text = line.substring(lastMatchEnd).trim();
                for (Long time : times) {
                    long adjusted = Math.max(0, time + offset);
                    parsedLines.add(new LrcLine(adjusted, text));
                }
            }
        }

        if (!parsedLines.isEmpty()) {
            Collections.sort(parsedLines);
            // Deduplicate same timestamp and empty text
            List<LrcLine> clean = new ArrayList<>();
            for (LrcLine l : parsedLines) {
                if (clean.isEmpty() || !clean.get(clean.size() - 1).text.equals(l.text) || clean.get(clean.size() - 1).timeMs != l.timeMs) {
                    clean.add(l);
                }
            }
            song.lines.addAll(clean);
            song.isSynced = true;
        } else {
            // Unsynced plain text lines
            song.isSynced = false;
            song.plainLyrics = content;
            for (String raw : rawLines) {
                String trimmed = raw.trim();
                if (!trimmed.isEmpty()) {
                    song.lines.add(new LrcLine(0L, trimmed));
                }
            }
        }

        return song;
    }

    /**
     * Merges synchronized translations into the song lines.
     */
    public static void mergeTranslation(LrcSong song, String translationLrc) {
        if (song == null || translationLrc == null || translationLrc.trim().isEmpty()) return;

        LrcSong transSong = parseLrc(translationLrc, song.title, song.artist, "Translation");
        if (transSong.lines.isEmpty()) return;

        for (LrcLine transLine : transSong.lines) {
            if (transLine.text.isEmpty()) continue;

            // Find best matching line in song within 800ms
            LrcLine bestMatch = null;
            long minDiff = Long.MAX_VALUE;

            for (LrcLine origLine : song.lines) {
                long diff = Math.abs(origLine.timeMs - transLine.timeMs);
                if (diff < minDiff && diff <= 1200) {
                    minDiff = diff;
                    bestMatch = origLine;
                }
            }

            if (bestMatch != null) {
                bestMatch.translation = transLine.text;
            }
        }
    }
}