package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Objects;

/** Satu parameter query string dalam bentuk ter-decode. Key duplikat diperbolehkan (urutan dipertahankan). */
public record QueryParam(String key, String value) {

    public QueryParam {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }
}
