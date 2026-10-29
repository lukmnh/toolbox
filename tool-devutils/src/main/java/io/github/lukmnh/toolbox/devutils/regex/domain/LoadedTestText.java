package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Objects;

public record LoadedTestText(String fileName, String text, long sizeBytes) {

    public LoadedTestText {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(text, "text");
    }
}
