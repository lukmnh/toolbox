package io.github.lukmnh.toolbox.devutils.regex.application;

import java.util.Objects;

public record ReplaceResult(String output, int replacements) {

    public ReplaceResult {
        Objects.requireNonNull(output, "output");
    }
}
