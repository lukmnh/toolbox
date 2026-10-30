package io.github.lukmnh.toolbox.devutils.sql.domain;

/** Port ke library formatter. Dipanggil per statement (lihat {@link SqlStatementSplitter}). */
public interface SqlStatementFormatter {

    /**
     * @return statement terformat, tanpa whitespace di awal/akhir
     * @throws RuntimeException error library apa adanya; use case membungkusnya jadi {@link SqlFormatFailedException}
     *     lengkap dengan nomor statement
     */
    String format(String statement, FormatOptions options);
}
