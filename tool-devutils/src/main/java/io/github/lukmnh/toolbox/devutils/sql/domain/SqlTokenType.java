package io.github.lukmnh.toolbox.devutils.sql.domain;

public enum SqlTokenType {
    /** Keyword, identifier, angka, operator: semua yang bukan jenis di bawah. */
    WORD,
    WHITESPACE,
    /** String literal; isinya tidak boleh diubah. */
    STRING,
    /** {@code "x"}, {@code `x`}, {@code [x]}; isinya tidak boleh diubah. */
    QUOTED_IDENTIFIER,
    LINE_COMMENT,
    BLOCK_COMMENT,
    /** {@code , ( )}: spasi di sekitarnya aman dibuang. */
    PUNCTUATION,
    SEMICOLON,
    /** Baris {@code GO} di T-SQL. */
    BATCH_SEPARATOR
}
