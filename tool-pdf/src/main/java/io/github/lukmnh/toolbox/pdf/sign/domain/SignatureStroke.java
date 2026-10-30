package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.util.List;
import java.util.Objects;

/** Satu goresan (mouse ditekan sampai dilepas). */
public record SignatureStroke(List<StrokePoint> points, InkColor color, double width) {

    public SignatureStroke {
        points = List.copyOf(points);
        Objects.requireNonNull(color, "color");
        if (points.isEmpty()) {
            throw new IllegalArgumentException("A stroke needs at least 1 point");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Pen width must be positive: " + width);
        }
    }
}
