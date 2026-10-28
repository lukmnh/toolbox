package io.github.lukmnh.toolbox.devutils.jwt.infrastructure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;

/** {@link JsonFactory} untuk JSON di dalam token dan JWK. Thread-safe setelah dibuat. */
final class JwtJsonFactory {

    /**
     * Batas dari {@link SecurityLimits} supaya token berbahaya ditolak rapi (Security #3). Duplicate key ditolak
     * karena claim ganda ({@code "exp"} dua kali) membuat isi token ambigu antar library.
     */
    static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(SecurityLimits.JSON_MAX_NESTING_DEPTH)
                    .maxStringLength(SecurityLimits.JSON_MAX_STRING_LENGTH)
                    .maxNumberLength(SecurityLimits.JSON_MAX_NUMBER_LENGTH)
                    .build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    private JwtJsonFactory() {}
}
