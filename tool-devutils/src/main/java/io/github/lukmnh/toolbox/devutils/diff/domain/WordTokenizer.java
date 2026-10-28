package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Memecah satu baris menjadi token kata, deret whitespace, dan tanda baca (satu karakter per token), supaya highlight
 * per kata tidak menandai seluruh baris hanya karena satu kata berubah.
 */
public final class WordTokenizer {

    private WordTokenizer() {}

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = tokenEnd(text, index);
            tokens.add(text.substring(index, end));
            index = end;
        }
        return tokens;
    }

    private static int tokenEnd(String text, int start) {
        int codePoint = text.codePointAt(start);
        int end = start + Character.charCount(codePoint);
        if (isWord(codePoint)) {
            return runEnd(text, end, true);
        }
        if (Character.isWhitespace(codePoint)) {
            return runEnd(text, end, false);
        }
        return end;
    }

    /** Lanjutkan selama karakter sejenis (kata atau whitespace). */
    private static int runEnd(String text, int from, boolean word) {
        int end = from;
        while (end < text.length()) {
            int codePoint = text.codePointAt(end);
            boolean sameKind = word ? isWord(codePoint) : Character.isWhitespace(codePoint);
            if (!sameKind) {
                return end;
            }
            end += Character.charCount(codePoint);
        }
        return end;
    }

    private static boolean isWord(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }
}
