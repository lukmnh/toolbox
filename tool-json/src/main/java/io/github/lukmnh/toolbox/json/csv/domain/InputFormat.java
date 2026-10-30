package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/** Cara membaca file CSV: hasil deteksi otomatis yang bisa di-override user. */
public record InputFormat(Delimiter delimiter, CsvEncoding encoding, boolean hasHeader) {

    public InputFormat {
        Objects.requireNonNull(delimiter, "delimiter");
        Objects.requireNonNull(encoding, "encoding");
    }
}
