package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Objects;

/** Satu baris preview: nama lama → nama baru + status. */
public record PreviewRow(FileEntry file, String newName, RenameStatus status, NameIssue issue) {

    public PreviewRow {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(newName, "newName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(issue, "issue");
    }
}
