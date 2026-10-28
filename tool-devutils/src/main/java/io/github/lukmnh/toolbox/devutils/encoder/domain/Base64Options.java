package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Objects;

/** Opsi encode/decode Base64 mode teks. */
public record Base64Options(Base64Variant variant, boolean padding, TextCharset charset) {

    public static final Base64Options DEFAULT = new Base64Options(Base64Variant.STANDARD, true, TextCharset.UTF_8);

    public Base64Options {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(charset, "charset");
    }
}
