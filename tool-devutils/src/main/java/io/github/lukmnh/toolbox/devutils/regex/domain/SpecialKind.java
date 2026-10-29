package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Konstruk PCRE/JS yang dikenali parser tapi tidak bisa dijalankan engine terjemahan. */
public enum SpecialKind {
    /** {@code \K}: reset awal match. */
    KEEP_OUT,
    /** {@code (?R)}, {@code (?0)}: rekursi seluruh pola. */
    RECURSION,
    /** {@code (?1)}, {@code (?&name)}, {@code (?P>name)}, {@code \g<name>}: panggil subpola. */
    SUBROUTINE_CALL,
    /** {@code (?(cond)yes|no)}. */
    CONDITIONAL,
    /** {@code (?|...)}. */
    BRANCH_RESET,
    /** {@code (*SKIP)}, {@code (*FAIL)}, {@code (*UTF)}, dst. */
    BACKTRACK_VERB,
    /** {@code \C}: satu byte. */
    SINGLE_BYTE
}
