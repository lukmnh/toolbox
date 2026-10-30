package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlag;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import java.util.Set;

/**
 * Menyusun pola sebagai literal kode yang siap ditempel di bahasa terkait: string Java, literal {@code /re/flags} JS,
 * raw string Go, dan string {@code '/re/flags'} untuk {@code preg_*} PHP.
 */
final class CodeLiteral {

    private static final char DELIMITER = '/';
    private static final char BACKTICK = '`';
    private static final int FIRST_PRINTABLE = 0x20;
    private static final int DELETE = 0x7F;

    private CodeLiteral() {}

    static String of(RegexFlavor flavor, String pattern, Set<RegexFlag> flags) {
        return switch (flavor) {
            case JAVA -> javaString(pattern);
            case JAVASCRIPT -> javaScriptLiteral(pattern, flags);
            case GO -> goString(pattern);
            case PHP -> phpString(pattern, flags);
        };
    }

    /** Escape {@code \} dan {@code "} plus karakter kontrol, supaya string bisa langsung dipakai di kode Java. */
    static String javaString(String pattern) {
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < pattern.length(); index++) {
            appendEscaped(builder, pattern.charAt(index), true);
        }
        return builder.append('"').toString();
    }

    /** Literal JS tidak boleh kosong ({@code //} adalah komentar), jadi pola kosong ditulis {@code (?:)}. */
    static String javaScriptLiteral(String pattern, Set<RegexFlag> flags) {
        String body = pattern.isEmpty()
                ? "(?:)"
                : escapeDelimiter(pattern).replace("\n", "\\n").replace("\r", "\\r");
        return DELIMITER + body + DELIMITER + flagLetters(RegexFlavor.JAVASCRIPT, flags);
    }

    /** Raw string Go tidak bisa memuat backtick dan membuang CR; dua kasus itu memakai string biasa. */
    static String goString(String pattern) {
        if (pattern.indexOf(BACKTICK) < 0 && pattern.indexOf('\r') < 0) {
            return BACKTICK + pattern + BACKTICK;
        }
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < pattern.length(); index++) {
            appendEscaped(builder, pattern.charAt(index), false);
        }
        return builder.append('"').toString();
    }

    /**
     * Di string single-quote PHP hanya {@code \\} dan {@code \'} yang di-escape; menggandakan semua backslash tetap
     * menghasilkan pola yang sama dan aman untuk backslash di akhir.
     */
    static String phpString(String pattern, Set<RegexFlag> flags) {
        String regex = DELIMITER + escapeDelimiter(pattern) + DELIMITER + flagLetters(RegexFlavor.PHP, flags);
        return "'" + regex.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    static String flagLetters(RegexFlavor flavor, Set<RegexFlag> flags) {
        StringBuilder letters = new StringBuilder();
        for (RegexFlag flag : flavor.flags()) {
            if (flags.contains(flag)) {
                letters.append(flag.token());
            }
        }
        return letters.toString();
    }

    /** {@code /} yang belum di-escape (didahului backslash berjumlah genap) diberi backslash. */
    private static String escapeDelimiter(String pattern) {
        StringBuilder builder = new StringBuilder(pattern.length());
        int backslashes = 0;
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current == DELIMITER && backslashes % 2 == 0) {
                builder.append('\\');
            }
            builder.append(current);
            backslashes = current == '\\' ? backslashes + 1 : 0;
        }
        return builder.toString();
    }

    private static void appendEscaped(StringBuilder builder, char character, boolean javaUnicodeEscape) {
        switch (character) {
            case '\\' -> builder.append("\\\\");
            case '"' -> builder.append("\\\"");
            case '\n' -> builder.append("\\n");
            case '\r' -> builder.append("\\r");
            case '\t' -> builder.append("\\t");
            default -> {
                if (character < FIRST_PRINTABLE || character == DELETE) {
                    builder.append(
                            javaUnicodeEscape
                                    ? String.format("\\u%04x", (int) character)
                                    : String.format("\\x%02x", (int) character));
                } else {
                    builder.append(character);
                }
            }
        }
    }
}
