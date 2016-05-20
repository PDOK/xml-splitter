package nl.pdok;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Raymond Kroon <raymond@k3n.nl>
 */
public class BoyerMoorePatternMatcher {

    public final static int NO_OF_CHARS = 256;
    public final static int MAX_BUFFER_SIZE = 1024;

    private InputStream src;
    private HashMap<String, PatternCache> cache = new HashMap<>();

    private boolean isAtMatch = false;
    private int bufferSize = 0;
    private byte[] buffer = new byte[MAX_BUFFER_SIZE];

    private int bufferPosition = 0;

    private String previousPatternId = null;
    private int suggestedBufferPosition = 0;

    public BoyerMoorePatternMatcher(InputStream src) {
        this.src = src;
    }

    public boolean currentPositionIsMatch() {
        return isAtMatch;
    }

    public byte[] flushToNextMatch(String patternId) throws IOException {

//        een stukje buffer lezen, en de buffer aanvullen mits nodig
//        fuck de copy pasta onzin....
        if (previousPatternId != null && previousPatternId.equals(patternId)) {
            bufferPosition = suggestedBufferPosition;
        }

        previousPatternId = patternId;

        byte[] flushResult = new byte[0];

        isAtMatch = false;
        PatternCache pc = cache.get(patternId);

        while (true) {

            SearchResult result = search(buffer, bufferSize, pc.pattern, pc.patternLength, pc.badchars, bufferPosition);

            bufferPosition = result.offset;
            suggestedBufferPosition = result.suggestedNewOffset;

            flushResult = concat(flushResult, flushBuffer());

            if (result.matched) {

                isAtMatch = true;
                return flushResult;
            } else {
                if (!fillBuffer()) {
                    isAtMatch = false;
                    flushResult = concat(flushResult, flushBuffer());
                    return flushResult;
                }
            }
        }
    }

    private byte[] flushBuffer() {
//        System.out.println("buffer");
//        System.out.println(new String(buffer));
//        System.out.println("/buffer/" + bufferPosition);
        byte[] flushed = Arrays.copyOfRange(buffer, 0, bufferPosition);
        // move currentPosition to front;
        buffer = Arrays.copyOfRange(buffer, bufferPosition, MAX_BUFFER_SIZE + bufferPosition);
        bufferSize = bufferSize - bufferPosition;
        suggestedBufferPosition = suggestedBufferPosition - bufferPosition;
        bufferPosition = 0;

        return flushed;
    }

    private boolean fillBuffer() throws IOException {
        if (bufferSize < MAX_BUFFER_SIZE) {
            int read = src.read(buffer, bufferSize, MAX_BUFFER_SIZE - bufferSize);
            bufferSize += read;

            return read > 0;
        }

        return true;
    }

    public byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private class PatternCache {

        public final byte[] pattern;
        public final int patternLength;
        public final int[] badchars;

        public PatternCache(byte[] pattern) {
            this.pattern = pattern;
            this.patternLength = pattern.length;
            this.badchars = badCharHeuristic(pattern);
        }
    }

    public void setPattern(String id, byte[] pattern) {
        cache.put(id, new PatternCache(pattern));
    }

    public static int[] badCharHeuristic(byte[] pattern) {
        int[] result = new int[NO_OF_CHARS];

        int patternSize = pattern.length;

        // default = -1
        Arrays.fill(result, -1);

        for (int i = 0; i < patternSize; ++i) {
            result[byteValue(pattern[i])] = i;
        }

        return result;
    }

    public static class SearchResult {

        public final boolean matched;
        public final int offset;
        public final int suggestedNewOffset;

        public SearchResult(boolean matched, int offset, int suggestedNewOffset) {
            this.matched = matched;
            this.offset = offset;
            this.suggestedNewOffset = suggestedNewOffset;
        }
    }

    public static SearchResult search(byte[] src, byte[] pattern, int offset) {
        /* Fill the bad character array by calling the preprocessing
         function badCharHeuristic() for given pattern */
        return search(src, src.length, pattern, pattern.length, badCharHeuristic(pattern), offset);
    }

    /* A pattern searching function that uses Bad Character Heuristic of
     Boyer Moore Algorithm */
    public static SearchResult search(byte[] src, int srcLength, byte[] pattern, int patternLength, int[] badchars, int offset) {

        int s = offset; // s is shift of the pattern with respect to text
        while (s <= (srcLength - patternLength)) {
            int j = patternLength - 1;

            /* Keep reducing index j of pattern while characters of
             pattern and text are matching at this shift s */
            while (j >= 0 && pattern[j] == src[s + j]) {
                j--;
            }

            /* If the pattern is present at current shift, then index j
             will become -1 after the above loop */
            if (j < 0) {
                //System.out.println("pattern found at index = " + s);

                /* Shift the pattern so that the next character in src
                 aligns with the last occurrence of it in pattern.
                 The condition s+m < n is necessary for the case when
                 pattern occurs at the end of text */
                //s += (s+patternLength < srcLength) ? patternLength - badchars[src[s+patternLength]] : 1;
                return new SearchResult(true, s, (s + patternLength < srcLength) ? s + patternLength - badchars[src[s + patternLength]] : s + 1);
            } else {
                /* Shift the pattern so that the bad character in src
                 aligns with the last occurrence of it in pattern. The
                 max function is used to make sure that we get a positive
                 shift. We may get a negative shift if the last occurrence
                 of bad character in pattern is on the right side of the
                 current character. */
                s += Math.max(1, j - badchars[byteValue(src[s + j])]);
            }
        }

        return new SearchResult(false, srcLength, s);
    }

    private static int byteValue(byte b) {
        return (int) b & 0xFF;
    }
}
