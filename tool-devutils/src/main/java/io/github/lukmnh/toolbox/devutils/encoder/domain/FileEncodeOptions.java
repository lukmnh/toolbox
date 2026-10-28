package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Objects;

/**
 * Opsi encode file. Data URI selalu memakai alfabet standar dengan padding (syarat RFC 2397), jadi varian
 * diabaikan saat {@code dataUri} aktif.
 */
public record FileEncodeOptions(Base64Variant variant, boolean padding, boolean dataUri) {

    public FileEncodeOptions {
        Objects.requireNonNull(variant, "variant");
    }

    public Base64Variant effectiveVariant() {
        return dataUri ? Base64Variant.STANDARD : variant;
    }

    public boolean effectivePadding() {
        return dataUri || padding;
    }
}
