package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.Base64;
import java.util.HexFormat;

/** Format tampilan digest. */
public enum DigestEncoding {
    HEX_LOWER,
    HEX_UPPER,
    BASE64;

    private static final HexFormat LOWER = HexFormat.of();
    private static final HexFormat UPPER = HexFormat.of().withUpperCase();

    public String encode(byte[] digest) {
        return switch (this) {
            case HEX_LOWER -> LOWER.formatHex(digest);
            case HEX_UPPER -> UPPER.formatHex(digest);
            case BASE64 -> Base64.getEncoder().encodeToString(digest);
        };
    }
}
