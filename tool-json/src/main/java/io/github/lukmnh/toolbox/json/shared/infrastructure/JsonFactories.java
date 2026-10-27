package io.github.lukmnh.toolbox.json.shared.infrastructure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

/** {@link JsonFactory} bersama. Thread-safe setelah dibuat, sehingga cukup satu instance. */
public final class JsonFactories {

    /** Dump sistem bisa berisi string sangat panjang (misalnya Base64); default Jackson 20 juta karakter. */
    private static final int MAX_STRING_LENGTH = Integer.MAX_VALUE;

    public static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxStringLength(MAX_STRING_LENGTH)
                    .build())
            // Emoji ditulis apa adanya dalam UTF-8, bukan escape surrogate \uD83D\uDE00
            .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    private JsonFactories() {}
}
