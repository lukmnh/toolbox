package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/**
 * Satu statement hasil pemecahan (termasuk {@code ;} penutupnya).
 *
 * @param batchSeparator {@code true} untuk baris {@code GO}: ditulis apa adanya, tidak di-format
 */
public record SqlStatement(String text, boolean batchSeparator) {

    public SqlStatement {
        Objects.requireNonNull(text, "text");
    }
}
