package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/**
 * Pilihan file hasil.
 *
 * @param neutralizeFormulas netralisir formula injection (Security Requirements #18); default aktif
 */
public record OutputOptions(
        OutputDelimiter delimiter,
        CsvEncoding encoding,
        QuoteMode quote,
        LineEnding lineEnding,
        boolean includeHeader,
        boolean neutralizeFormulas) {

    public OutputOptions {
        Objects.requireNonNull(delimiter, "delimiter");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(lineEnding, "lineEnding");
        if (!CsvEncoding.OUTPUT_CHOICES.contains(encoding)) {
            throw new IllegalArgumentException("Encoding hasil harus UTF-8");
        }
    }

    /** Excel versi Indonesia (pemisah {@code ;}) salah membaca karakter non-ASCII pada UTF-8 tanpa BOM. */
    public static CsvEncoding defaultEncodingFor(Delimiter delimiter) {
        return delimiter == Delimiter.SEMICOLON ? CsvEncoding.UTF_8_BOM : CsvEncoding.UTF_8;
    }

    public OutputFormat resolve(Delimiter inputDelimiter) {
        return new OutputFormat(delimiter.resolve(inputDelimiter), encoding, quote, lineEnding, neutralizeFormulas);
    }
}
