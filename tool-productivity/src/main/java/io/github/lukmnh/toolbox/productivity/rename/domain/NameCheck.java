package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Objects;

/** Hasil pemeriksaan satu nama baru. */
public record NameCheck(RenameStatus status, NameIssue issue) {

    public NameCheck {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(issue, "issue");
    }
}
