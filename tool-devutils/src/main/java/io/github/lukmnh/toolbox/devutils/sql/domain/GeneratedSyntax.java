package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/**
 * @param rows baris yang menjadi statement/dokumen
 * @param skippedRows baris UPDATE/DELETE yang dilewati karena key-nya kosong
 */
public record GeneratedSyntax(String text, int rows, int skippedRows) {

    public GeneratedSyntax {
        Objects.requireNonNull(text, "text");
    }
}
