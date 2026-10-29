package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

public record LoadedSql(String fileName, String text, long sizeBytes) {

    public LoadedSql {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(text, "text");
    }
}
