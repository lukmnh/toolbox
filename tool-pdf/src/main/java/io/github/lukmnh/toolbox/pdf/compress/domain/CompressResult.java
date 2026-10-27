package io.github.lukmnh.toolbox.pdf.compress.domain;

import java.nio.file.Path;

/**
 * @param alreadyOptimal {@code true} bila hasil kompres tidak lebih kecil, sehingga output = salinan asli
 */
public record CompressResult(
        Path output, long originalSize, long resultSize, int imagesRecompressed, boolean alreadyOptimal) {

    private static final double PERCENT = 100.0;

    /** Persentase pengurangan ukuran, 0 bila tidak berkurang. */
    public double savingPercent() {
        if (originalSize == 0 || resultSize >= originalSize) {
            return 0;
        }
        return (originalSize - resultSize) * PERCENT / originalSize;
    }
}
