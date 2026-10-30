package io.github.lukmnh.toolbox.devutils.regex.domain;

public enum AnchorKind {
    /** {@code ^}. */
    LINE_START,
    /** {@code $}. */
    LINE_END,
    /** {@code \b}. */
    WORD_BOUNDARY,
    /** {@code \B}. */
    NON_WORD_BOUNDARY,
    /** {@code \A}. */
    INPUT_START,
    /** {@code \z}. */
    INPUT_END,
    /** {@code \Z}: akhir input atau sebelum newline terakhir. */
    INPUT_END_BEFORE_NEWLINE,
    /** {@code \G}. */
    PREVIOUS_MATCH_END
}
