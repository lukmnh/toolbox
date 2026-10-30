package io.github.lukmnh.toolbox.productivity.rename.domain;

/** Alasan status KONFLIK / TIDAK VALID di preview. */
public enum NameIssue {
    NONE,
    EMPTY,
    FORBIDDEN_CHARACTER,
    RESERVED_NAME,
    TOO_LONG,
    TRAILING_DOT_OR_SPACE,
    DUPLICATE_IN_BATCH,
    EXISTS_IN_FOLDER
}
