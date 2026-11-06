package io.github.lukmnh.toolbox.devutils.shared.infrastructure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;

/** {@link JsonFactory} untuk JSON tidak dipercaya (token/JWK, data SQL Generator). Thread-safe setelah dibuat. */
public final class SafeJsonFactory {

    /**
     * Batas dari {@link SecurityLimits} supaya token berbahaya ditolak rapi (Security #3). Duplicate key ditolak
     * karena key ganda (mis. claim {@code "exp"} dua kali) membuat isi dokumen ambigu antar library.
     */
    public static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(SecurityLimits.JSON_MAX_NESTING_DEPTH)
                    .maxStringLength(SecurityLimits.JSON_MAX_STRING_LENGTH)
                    .maxNumberLength(SecurityLimits.JSON_MAX_NUMBER_LENGTH)
                    .build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .build();

    private SafeJsonFactory() {}
}
