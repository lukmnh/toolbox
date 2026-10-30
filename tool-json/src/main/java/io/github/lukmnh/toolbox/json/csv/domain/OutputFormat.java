package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/** Format penulisan file hasil, pemisah sudah di-resolve terhadap input. */
public record OutputFormat(
        Delimiter delimiter, CsvEncoding encoding, QuoteMode quote, LineEnding lineEnding, boolean neutralizeFormulas) {

    public OutputFormat {
        Objects.requireNonNull(delimiter, "delimiter");
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(lineEnding, "lineEnding");
    }
}
