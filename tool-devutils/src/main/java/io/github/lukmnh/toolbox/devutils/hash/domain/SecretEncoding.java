package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.Base64;
import java.util.HexFormat;

/** Format tampilan random secret. */
public enum SecretEncoding {
    HEX,
    BASE64,
    /** Base64 URL-safe tanpa padding, aman langsung dipakai di URL/env var. */
    BASE64_URL;

    public String encode(byte[] bytes) {
        return switch (this) {
            case HEX -> HexFormat.of().formatHex(bytes);
            case BASE64 -> Base64.getEncoder().encodeToString(bytes);
            case BASE64_URL -> Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        };
    }
}
