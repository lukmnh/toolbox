package io.github.lukmnh.toolbox.devutils.regex.domain;

/**
 * Jenis masalah pola. Presentation memetakan ke pesan {@code devutils.regex.problem.<KODE>} dengan argumen
 * {@link RegexProblem#args()}.
 */
public enum ProblemCode {
    UNMATCHED_OPEN_PAREN,
    UNMATCHED_CLOSE_PAREN,
    UNTERMINATED_CLASS,
    NOTHING_TO_REPEAT,
    /** args: [dari, sampai] karakter. */
    INVALID_CLASS_RANGE,
    /** args: [min, max]. */
    INVALID_QUANTIFIER_RANGE,
    TRAILING_BACKSLASH,
    /** args: [teks escape]. */
    INVALID_ESCAPE,
    /** args: [nama]. */
    INVALID_GROUP_NAME,
    /** args: [nama]. */
    DUPLICATE_GROUP_NAME,
    /** args: [nomor atau nama]. */
    UNKNOWN_GROUP_REFERENCE,
    /** args: [teks setelah "(?"]. */
    INVALID_GROUP_SYNTAX,
    /** args: [teks escape]. */
    INVALID_UNICODE_ESCAPE,
    /** args: [flag]. */
    UNKNOWN_FLAG,
    /** Fitur tidak ada di flavor ini. args: [{@link Feature#name()}]. */
    UNSUPPORTED_BY_FLAVOR,
    /** Fitur ada di flavor ini tapi tidak bisa dijalankan engine terjemahan. args: [{@link Feature#name()}]. */
    UNSUPPORTED_BY_ENGINE
}
