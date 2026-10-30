package io.github.lukmnh.toolbox.devutils.regex.domain;

public enum GroupKind {
    CAPTURING,
    NAMED,
    NON_CAPTURING,
    ATOMIC,
    LOOKAHEAD,
    NEGATIVE_LOOKAHEAD,
    LOOKBEHIND,
    NEGATIVE_LOOKBEHIND,
    /** {@code (?i:...)} / {@code (?i-s:...)}: grup non-capture dengan flag lokal. */
    FLAG_GROUP
}
