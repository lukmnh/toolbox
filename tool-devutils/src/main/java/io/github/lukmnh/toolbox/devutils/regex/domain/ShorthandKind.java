package io.github.lukmnh.toolbox.devutils.regex.domain;

public enum ShorthandKind {
    DIGIT,
    NOT_DIGIT,
    WORD,
    NOT_WORD,
    SPACE,
    NOT_SPACE,
    HORIZONTAL_SPACE,
    NOT_HORIZONTAL_SPACE,
    VERTICAL_SPACE,
    NOT_VERTICAL_SPACE,
    /** {@code \R}: urutan line break apa pun. */
    LINEBREAK,
    /** {@code \X}: satu grapheme cluster. */
    GRAPHEME,
    /** {@code \N} (PCRE): karakter apa pun selain newline. */
    NOT_NEWLINE
}
