package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlOutput;

/** Satu hasil Format/Minify beserta ukuran input → output (dihitung di background, bukan di FX thread). */
record SqlRun(SqlAction action, SqlDialect dialect, SqlOutput output, long inputBytes, long outputBytes) {

    static SqlRun of(SqlAction action, SqlDialect dialect, String input, SqlOutput output) {
        return new SqlRun(action, dialect, output, utf8Length(input), utf8Length(output.text()));
    }

    String text() {
        return output.text();
    }

    /** Tanpa {@code getBytes}: input 10 MB tidak perlu disalin hanya untuk dihitung. */
    private static long utf8Length(String text) {
        long bytes = 0;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c < 0x80) {
                bytes += 1;
            } else if (c < 0x800) {
                bytes += 2;
            } else if (Character.isHighSurrogate(c) && index + 1 < text.length()) {
                bytes += 4;
                index++;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }
}
