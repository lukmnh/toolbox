package io.github.lukmnh.toolbox.devutils.sql.domain;

/**
 * Minify SQL: whitespace dipadatkan jadi satu spasi (atau dibuang di sekitar {@code , ( ) ;}), opsional hapus
 * komentar. String literal dan quoted identifier disalin byte per byte.
 */
public final class SqlMinifier {

    private static final int CHECKPOINT_INTERVAL = 4_096;
    private static final char NOT_PUNCTUATION = '\0';
    private static final String TIGHT_AFTER = "(,;";
    private static final String TIGHT_BEFORE = "),;";

    /** Pemisah yang harus ditulis sebelum token berikutnya; urutan = prioritas. */
    private enum Separator {
        NONE,
        SPACE,
        NEWLINE
    }

    private SqlMinifier() {}

    /**
     * @param removeComments komentar hint ({@code /*! ..*\/} MySQL, {@code /*+ ..*\/} optimizer) tetap dipertahankan
     *     karena mengubah perilaku query
     * @param checkpoint dipanggil berkala; boleh melempar exception untuk membatalkan
     */
    public static String minify(String sql, SqlDialect dialect, boolean removeComments, Runnable checkpoint) {
        StringBuilder out = new StringBuilder(sql.length());
        SqlTokenizer tokens = new SqlTokenizer(sql, dialect);
        Separator pending = Separator.NONE;
        char previous = NOT_PUNCTUATION;
        int count = 0;
        while (tokens.hasNext()) {
            if (++count % CHECKPOINT_INTERVAL == 0) {
                checkpoint.run();
            }
            SqlToken token = tokens.next();
            if (isDropped(token, sql, removeComments)) {
                pending = max(pending, Separator.SPACE);
                continue;
            }
            char current = punctuation(token, sql);
            appendSeparator(out, pending, previous, current, token.type());
            out.append(sql, token.start(), token.end());
            // Komentar baris dan GO harus diakhiri newline supaya token sesudahnya tidak ikut "termakan"
            pending = token.type() == SqlTokenType.LINE_COMMENT || token.type() == SqlTokenType.BATCH_SEPARATOR
                    ? Separator.NEWLINE
                    : Separator.NONE;
            previous = current;
        }
        return out.toString();
    }

    private static char punctuation(SqlToken token, String sql) {
        boolean punctuation = token.type() == SqlTokenType.PUNCTUATION || token.type() == SqlTokenType.SEMICOLON;
        return punctuation ? sql.charAt(token.start()) : NOT_PUNCTUATION;
    }

    private static boolean isDropped(SqlToken token, String sql, boolean removeComments) {
        return switch (token.type()) {
            case WHITESPACE -> true;
            case LINE_COMMENT -> removeComments;
            case BLOCK_COMMENT -> removeComments && !isHint(sql, token);
            default -> false;
        };
    }

    private static boolean isHint(String sql, SqlToken token) {
        return sql.startsWith("/*!", token.start()) || sql.startsWith("/*+", token.start());
    }

    /**
     * Spasi dibuang setelah {@code ( , ;} dan sebelum {@code ) , ;}: tidak pernah mengubah arti query. Spasi sebelum
     * {@code (} dipertahankan karena di MySQL {@code COUNT (*)} vs {@code COUNT(*)} bisa berbeda (IGNORE_SPACE).
     */
    private static void appendSeparator(
            StringBuilder out, Separator pending, char previous, char current, SqlTokenType currentType) {
        if (out.isEmpty()) {
            return;
        }
        if (pending == Separator.NEWLINE || currentType == SqlTokenType.BATCH_SEPARATOR) {
            out.append('\n');
        } else if (pending == Separator.SPACE
                && TIGHT_AFTER.indexOf(previous) < 0
                && TIGHT_BEFORE.indexOf(current) < 0) {
            out.append(' ');
        }
    }

    private static Separator max(Separator a, Separator b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
