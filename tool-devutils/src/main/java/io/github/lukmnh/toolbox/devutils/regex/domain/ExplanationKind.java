package io.github.lukmnh.toolbox.devutils.regex.domain;

/**
 * Jenis baris penjelasan. Presentation memetakan ke pesan {@code devutils.regex.explain.<KIND>} dengan argumen
 * {@link Explanation#args()}.
 */
public enum ExplanationKind {
    /** Pola kosong. */
    EMPTY,
    /** Pilihan; anak = tiap cabang. */
    ALTERNATION,
    /** Satu cabang dari ALTERNATION. args: [nomor cabang mulai 1]. */
    ALTERNATIVE,
    /** args: [nomor]. */
    GROUP_CAPTURING,
    /** args: [nomor, nama]. */
    GROUP_NAMED,
    GROUP_NON_CAPTURING,
    GROUP_ATOMIC,
    LOOKAHEAD,
    NEGATIVE_LOOKAHEAD,
    LOOKBEHIND,
    NEGATIVE_LOOKBEHIND,
    /** args: [flag aktif, flag mati]. */
    GROUP_FLAGS,
    /** args: [flag aktif, flag mati]. */
    INLINE_FLAGS,
    /** Literal berurutan digabung. args: [teks]. */
    LITERAL_TEXT,
    /** Satu karakter literal yang perlu dideskripsikan (spasi, tab, newline, karakter kontrol). args: [nama/kode]. */
    LITERAL_SPECIAL,
    /** args: [teks]. */
    QUOTED,
    /** args: ["true" bila dot juga cocok dengan newline (DOTALL/s), selain itu "false"]. */
    ANY_CHAR,
    /** args: [{@link AnchorKind#name()}, "true" bila mode multiline aktif]. */
    ANCHOR,
    /** args: [{@link ShorthandKind#name()}]. */
    SHORTHAND,
    /** args: [nama properti, "true" bila negasi]. */
    UNICODE_PROPERTY,
    /** args: [nama, "true" bila negasi]. */
    POSIX_CLASS,
    /** Anak = item class. */
    CHAR_CLASS,
    /** Anak = item class. */
    NEGATED_CHAR_CLASS,
    /** args: [dari, sampai]. */
    CLASS_RANGE,
    /** args: [karakter]. */
    CLASS_LITERAL,
    /** args: [{@link SetOperator#name()}]. */
    CLASS_OPERATOR,
    /** args: [nomor]. */
    BACKREFERENCE_NUMBER,
    /** args: [nama]. */
    BACKREFERENCE_NAME,
    /** args: [teks]. */
    COMMENT,
    /** args: [{@link SpecialKind#name()}, teks asli]. */
    SPECIAL,
    /** args: [teks asli]. */
    UNKNOWN
}
