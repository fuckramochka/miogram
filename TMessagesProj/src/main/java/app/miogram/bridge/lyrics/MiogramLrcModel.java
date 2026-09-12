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

    /** Single word with start/end timings from enhanced STT. */
    public static class LrcWord {
        public final long startMs;
        public final long endMs;
        public final String text;

        public LrcWord(long startMs, long endMs, String text) {
            this.startMs = Math.max(0, startMs);
            this.endMs = Math.max(this.startMs, endMs);
            this.text = text != null ? text : "";
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("s", startMs);
                obj.put("e", endMs);
                obj.put("w", text);
            } catch (Throwable ignored) {}
            return obj;
        }

        public static LrcWord fromJson(JSONObject obj) {
            return new LrcWord(obj.optLong("s", 0L), obj.optLong("e", 0L), obj.optString("w", ""));
        }
    }

    public static class LrcLine implements Comparable<LrcLine> {
        public final long timeMs;
        public String text;
        public String translation;
        /** Word-level timings (enhanced transcription). Empty for line-level sources. */
        public final List<LrcWord> words = new ArrayList<>();

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

        public boolean hasWordTimings() {
            return !words.isEmpty();
        }

        /**
         * Fraction of this line already sung at currentMs, based on real word
         * start/end timings. Falls back to -1 when no word data.
         */
        public float wordFraction(long currentMs) {
            if (words.isEmpty() || text.isEmpty()) return -1f;
            if (currentMs < words.get(0).startMs) return 0f;
            int total = text.length();
            int sung = 0;
            int pos = 0;
            for (int i = 0; i < words.size(); i++) {
                LrcWord w = words.get(i);
                String wText = w.text;
                int at = text.indexOf(wText, pos);
                int start = at >= 0 ? at : pos;
                int end = Math.min(total, start + wText.length());
                if (currentMs >= w.endMs) {
                    sung = end;
                } else if (currentMs >= w.startMs && w.endMs > w.startMs) {
                    float f = (float) (currentMs - w.startMs) / (float) (w.endMs - w.startMs);
                    sung = start + Math.round((end - start) * Math.max(0f, Math.min(1f, f)));
                    break;
                } else {
                    break;
                }
                pos = end;
            }
            return Math.max(0f, Math.min(1f, (float) sung / (float) total));
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
                if (!words.isEmpty()) {
                    JSONArray arr = new JSONArray();
                    for (LrcWord w : words) arr.put(w.toJson());
                    obj.put("words", arr);
                }
            } catch (Throwable ignored) {}
            return obj;
        }

        public static LrcLine fromJson(JSONObject obj) {
            long t = obj.optLong("t", 0L);
            String s = obj.optString("s", "");
            String tr = obj.has("tr") ? obj.optString("tr", null) : null;
            LrcLine line = new LrcLine(t, s, tr);
            JSONArray arr = obj.optJSONArray("words");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject wo = arr.optJSONObject(i);
                    if (wo != null) line.words.add(LrcWord.fromJson(wo));
                }
            }
            return line;
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
     * Parses word-timed transcription: one word per timestamped line,
     * e.g. {@code [00:12.40] hello}. Words are grouped into readable lyric
     * lines (up to 8 words or a sentence break); every word keeps its own
     * start/end timings for precise karaoke. End = next word start.
     */
    public static LrcSong parseWordTimed(String content, String title, String artist, String source) {
        LrcSong song = new LrcSong(title, artist, source, true);
        if (content == null || content.trim().isEmpty()) return song;

        List<LrcWord> all = new ArrayList<>();
        String[] rawLines = content.split("\\r?\\n");
        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher matcher = TIME_TAG_PATTERN.matcher(line);
            List<Long> times = new ArrayList<>();
            int lastEnd = 0;
            while (matcher.find()) {
                try {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    String msStr = matcher.group(3);
                    long ms = 0;
                    if (msStr != null && !msStr.isEmpty()) {
                        ms = Long.parseLong(msStr);
                        if (msStr.length() == 1) ms *= 100;
                        else if (msStr.length() == 2) ms *= 10;
                    }
                    times.add(min * 60L * 1000L + sec * 1000L + ms);
                    lastEnd = matcher.end();
                } catch (Throwable ignored) {}
            }
            if (times.isEmpty()) continue;
            String word = line.substring(lastEnd).trim();
            if (word.isEmpty() || word.startsWith("[") && word.endsWith("]")) continue;
            // Skip whole-sentence lines: word mode expects single words.
            if (word.contains("  ") || word.split("\\s+").length > 4) continue;
            for (Long t : times) all.add(new LrcWord(t, t, word));
        }
        if (all.isEmpty()) return song;

        Collections.sort(all, (a, b) -> Long.compare(a.startMs, b.startMs));
        // Deduplicate identical timestamps, keep first word.
        List<LrcWord> dedup = new ArrayList<>();
        for (LrcWord w : all) {
            if (!dedup.isEmpty() && dedup.get(dedup.size() - 1).startMs == w.startMs) continue;
            dedup.add(w);
        }
        // End = next word start (clamped to +2.5s so pauses don't stretch words).
        List<LrcWord> timed = new ArrayList<>();
        for (int i = 0; i < dedup.size(); i++) {
            LrcWord w = dedup.get(i);
            long end = (i + 1 < dedup.size()) ? dedup.get(i + 1).startMs : w.startMs + 1200L;
            if (end - w.startMs > 2500L) end = w.startMs + 1200L;
            timed.add(new LrcWord(w.startMs, end, w.text));
        }

        // Group into readable lines: max 8 words, break on sentence punctuation
        // or gaps > 2.2s (new phrase).
        StringBuilder sb = new StringBuilder();
        List<LrcWord> cur = new ArrayList<>();
        long lineStart = -1;
        int wordCount = 0;
        for (int i = 0; i < timed.size(); i++) {
            LrcWord w = timed.get(i);
            if (lineStart < 0) lineStart = w.startMs;
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.text);
            cur.add(w);
            wordCount++;
            boolean sentenceEnd = w.text.matches(".*[.!?…]+[\"»\\)]*$");
            boolean gap = (i + 1 < timed.size()) && (timed.get(i + 1).startMs - w.endMs > 2200L);
            if (wordCount >= 8 || sentenceEnd || gap || i == timed.size() - 1) {
                LrcLine line = new LrcLine(lineStart, sb.toString());
                line.words.addAll(cur);
                song.lines.add(line);
                sb.setLength(0);
                cur.clear();
                lineStart = -1;
                wordCount = 0;
            }
        }
        song.isSynced = !song.lines.isEmpty();
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