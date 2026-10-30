package io.github.lukmnh.toolbox.devutils.sql.domain;

/** Aturan leksikal yang berbeda antar dialect; menentukan cara tokenizer mengenali string dan komentar. */
public enum SqlSyntax {
    /** {@code 'it\'s'}: backslash meng-escape karakter berikutnya di dalam string. */
    BACKSLASH_ESCAPES,
    /** {@code # komentar} sampai akhir baris. */
    HASH_COMMENTS,
    /** {@code --} baru dianggap komentar bila diikuti spasi/kontrol ({@code a--1} = a minus minus 1). */
    DASH_COMMENT_NEEDS_SPACE,
    /** {@code $$isi$$} / {@code $tag$isi$tag$}: body function PostgreSQL. */
    DOLLAR_QUOTES,
    /** {@code E'baris\n'}: string dengan escape backslash. */
    ESCAPE_STRING_PREFIX,
    /** {@code [nama kolom]} sebagai quoted identifier. */
    BRACKET_IDENTIFIERS,
    /** {@code /* luar /* dalam *\/ masih komentar *\/}. */
    NESTED_COMMENTS,
    /** {@code q'[it's]'}: alternative quoting Oracle. */
    ALTERNATIVE_QUOTING,
    /** Baris berisi {@code GO} saja memisahkan batch (SSMS/sqlcmd), bukan bagian SQL. */
    GO_BATCH_SEPARATOR
}
