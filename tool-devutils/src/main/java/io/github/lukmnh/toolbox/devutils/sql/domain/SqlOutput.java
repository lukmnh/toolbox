package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/**
 * Hasil Format/Minify.
 *
 * @param unformattedStatements statement yang terlalu panjang untuk di-format dan ditulis apa adanya
 */
public record SqlOutput(String text, int unformattedStatements) {

    public SqlOutput {
        Objects.requireNonNull(text, "text");
        if (unformattedStatements < 0) {
            throw new IllegalArgumentException("unformattedStatements is negative");
        }
    }
}
