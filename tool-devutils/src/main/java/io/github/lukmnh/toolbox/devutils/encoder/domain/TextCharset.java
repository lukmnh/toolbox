package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Charset untuk mengubah teks ke byte sebelum encode (dan sebaliknya setelah decode). */
public enum TextCharset {
    UTF_8(StandardCharsets.UTF_8),
    ISO_8859_1(StandardCharsets.ISO_8859_1),
    UTF_16(StandardCharsets.UTF_16);

    private final Charset charset;

    TextCharset(Charset charset) {
        this.charset = charset;
    }

    public Charset charset() {
        return charset;
    }

    public String displayName() {
        return charset.name();
    }
}
