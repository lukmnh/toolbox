package io.github.lukmnh.toolbox.pdf.sign.domain;

/** Titik goresan tanda tangan, dalam pixel kanvas. */
public record StrokePoint(double x, double y) {

    public StrokePoint midpointTo(StrokePoint other) {
        return new StrokePoint((x + other.x) / 2, (y + other.y) / 2);
    }
}
