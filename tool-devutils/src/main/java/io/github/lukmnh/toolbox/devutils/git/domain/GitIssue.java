package io.github.lukmnh.toolbox.devutils.git.domain;

import java.util.List;

/** Masalah input: argumen wajib kosong, atau beberapa opsi eksklusif dipilih bersamaan. */
public record GitIssue(Kind kind, List<String> keys) {

    public enum Kind {
        MISSING_ARGUMENT,
        CONFLICT
    }

    public GitIssue {
        keys = List.copyOf(keys);
    }
}
