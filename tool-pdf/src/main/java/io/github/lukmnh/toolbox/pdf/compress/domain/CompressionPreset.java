package io.github.lukmnh.toolbox.pdf.compress.domain;

/** Target resolusi dan kualitas JPEG untuk gambar di dalam PDF. */
public enum CompressionPreset {
    BALANCED(150, 0.75f),
    HIGH_QUALITY(200, 0.85f),
    SMALL_SIZE(110, 0.60f);

    public static final CompressionPreset DEFAULT = BALANCED;

    private final int targetDpi;
    private final float jpegQuality;

    CompressionPreset(int targetDpi, float jpegQuality) {
        this.targetDpi = targetDpi;
        this.jpegQuality = jpegQuality;
    }

    public int targetDpi() {
        return targetDpi;
    }

    public float jpegQuality() {
        return jpegQuality;
    }
}
