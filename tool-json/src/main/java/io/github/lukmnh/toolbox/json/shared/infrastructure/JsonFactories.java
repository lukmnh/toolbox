package io.github.lukmnh.toolbox.json.shared.infrastructure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;

/** {@link JsonFactory} bersama. Thread-safe setelah dibuat, sehingga cukup satu instance. */
public final class JsonFactories {

    /** Batas dari {@link SecurityLimits}: file berbahaya ditolak rapi, bukan bikin hang/OOM. */
    public static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(SecurityLimits.JSON_MAX_NESTING_DEPTH)
                    .maxStringLength(SecurityLimits.JSON_MAX_STRING_LENGTH)
                    .maxNumberLength(SecurityLimits.JSON_MAX_NUMBER_LENGTH)
                    .build())
            // Emoji ditulis apa adanya dalam UTF-8, bukan escape surrogate \uD83D\uDE00
            .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    private JsonFactories() {}
}
