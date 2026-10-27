package io.github.lukmnh.toolbox.pdf.sign.domain;

/** Warna tinta tanda tangan. */
public enum InkColor {
    BLACK(0x000000),
    BLUE(0x1A3FA8);

    private final int rgb;

    InkColor(int rgb) {
        this.rgb = rgb;
    }

    /** RGB 24-bit tanpa alpha. */
    public int rgb() {
        return rgb;
    }
}
