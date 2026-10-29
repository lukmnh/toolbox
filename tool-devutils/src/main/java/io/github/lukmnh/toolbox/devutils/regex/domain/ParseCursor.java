package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;

/** Posisi baca di pola + daftar masalah yang ditemukan. Dipakai bersama oleh bagian-bagian parser. */
final class ParseCursor {

    /** Nilai {@link #peek()} saat posisi sudah di akhir pola. */
    static final int END = -1;

    private final String pattern;
    private final List<RegexProblem> problems = new ArrayList<>();
    private int position;

    ParseCursor(String pattern) {
        this.pattern = pattern;
    }

    String pattern() {
        return pattern;
    }

    int position() {
        return position;
    }

    void reset(int newPosition) {
        position = newPosition;
    }

    boolean atEnd() {
        return position >= pattern.length();
    }

    /** Karakter ({@code char}) di posisi sekarang + {@code ahead}, atau {@link #END}. */
    int peek(int ahead) {
        int index = position + ahead;
        return index < pattern.length() ? pattern.charAt(index) : END;
    }

    int peek() {
        return peek(0);
    }

    boolean lookingAt(String text) {
        return pattern.startsWith(text, position);
    }

    /** Lewati {@code text} bila ada di posisi sekarang. */
    boolean consume(String text) {
        if (!lookingAt(text)) {
            return false;
        }
        position += text.length();
        return true;
    }

    boolean consume(char expected) {
        if (peek() != expected) {
            return false;
        }
        position++;
        return true;
    }

    void advance(int count) {
        position = Math.min(pattern.length(), position + count);
    }

    /** Ambil satu code point (pasangan surrogate dihitung satu). */
    int nextCodePoint() {
        int codePoint = pattern.codePointAt(position);
        position += Character.charCount(codePoint);
        return codePoint;
    }

    Span spanFrom(int start) {
        return new Span(start, Math.max(start, position));
    }

    String textFrom(int start) {
        return pattern.substring(start, Math.max(start, position));
    }

    void problem(Span span, ProblemCode code, String... args) {
        problems.add(RegexProblem.of(span, code, args));
    }

    void unsupported(Span span, Feature feature) {
        problem(span, ProblemCode.UNSUPPORTED_BY_FLAVOR, feature.name());
    }

    List<RegexProblem> problems() {
        return problems;
    }

    int problemCount() {
        return problems.size();
    }

    /** Buang masalah yang tercatat saat percobaan parse yang kemudian dibatalkan (backtrack). */
    void truncateProblems(int count) {
        while (problems.size() > count) {
            problems.removeLast();
        }
    }

    /** Baca deret digit desimal (maks {@code maxDigits}); kembalikan -1 bila tidak ada digit. */
    int readNumber(int maxDigits) {
        int start = position;
        long value = 0;
        while (position - start < maxDigits && isDigit(peek())) {
            value = Math.min(Integer.MAX_VALUE, value * 10 + (peek() - '0'));
            position++;
        }
        return position == start ? -1 : (int) value;
    }

    /** Baca tepat/maks {@code maxDigits} digit dengan basis tertentu; -1 bila kurang dari {@code minDigits}. */
    int readRadix(int radix, int minDigits, int maxDigits) {
        int start = position;
        long value = 0;
        while (position - start < maxDigits && Character.digit(peek(), radix) >= 0) {
            value = Math.min(Integer.MAX_VALUE, value * radix + Character.digit(peek(), radix));
            position++;
        }
        if (position - start < minDigits) {
            position = start;
            return -1;
        }
        return (int) value;
    }

    static boolean isDigit(int character) {
        return character >= '0' && character <= '9';
    }

    static boolean isOctal(int character) {
        return character >= '0' && character <= '7';
    }

    static boolean isAsciiLetter(int character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }
}
