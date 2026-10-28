package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Objects;

/** Potongan awal teks hasil; {@code truncated} bila file lebih besar dari batas preview. */
public record TextPreview(String text, boolean truncated) {

    public TextPreview {
        Objects.requireNonNull(text, "text");
    }
}
