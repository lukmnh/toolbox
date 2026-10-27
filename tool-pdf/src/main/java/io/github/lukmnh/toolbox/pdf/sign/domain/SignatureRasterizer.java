package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * Mengubah goresan tanda tangan menjadi gambar ARGB transparan beresolusi 3× kanvas, dipotong ke bounding
 * box goresan + padding.
 */
public final class SignatureRasterizer {

    public static final int EXPORT_SCALE = 3;
    public static final double PADDING = 8;

    /** @return kosong bila tidak ada goresan */
    public Optional<BufferedImage> rasterize(List<SignatureStroke> strokes) {
        if (strokes.isEmpty()) {
            return Optional.empty();
        }
        Bounds bounds = Bounds.of(strokes);
        int width = (int) Math.ceil(bounds.width() * EXPORT_SCALE);
        int height = (int) Math.ceil(bounds.height() * EXPORT_SCALE);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.scale(EXPORT_SCALE, EXPORT_SCALE);
            graphics.translate(-bounds.minX(), -bounds.minY());
            strokes.forEach(stroke -> draw(graphics, stroke));
        } finally {
            graphics.dispose();
        }
        return Optional.of(image);
    }

    private static void draw(Graphics2D graphics, SignatureStroke stroke) {
        graphics.setColor(new Color(stroke.color().rgb()));
        List<StrokePoint> points = stroke.points();
        if (points.size() == 1) {
            StrokePoint dot = points.getFirst();
            double radius = stroke.width() / 2;
            graphics.fill(new Ellipse2D.Double(dot.x() - radius, dot.y() - radius, stroke.width(), stroke.width()));
            return;
        }
        graphics.setStroke(new BasicStroke((float) stroke.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(smoothPath(points));
    }

    /**
     * Kurva quadratic melalui titik tengah antar titik: tiap titik asli menjadi control point, sehingga
     * sudut tajam akibat sampling mouse menjadi halus.
     */
    static Path2D smoothPath(List<StrokePoint> points) {
        Path2D.Double path = new Path2D.Double();
        StrokePoint first = points.getFirst();
        path.moveTo(first.x(), first.y());
        for (int index = 1; index < points.size() - 1; index++) {
            StrokePoint control = points.get(index);
            StrokePoint end = control.midpointTo(points.get(index + 1));
            path.quadTo(control.x(), control.y(), end.x(), end.y());
        }
        StrokePoint last = points.getLast();
        path.lineTo(last.x(), last.y());
        return path;
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {

        static Bounds of(List<SignatureStroke> strokes) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (SignatureStroke stroke : strokes) {
                double margin = stroke.width() / 2 + PADDING;
                for (StrokePoint point : stroke.points()) {
                    minX = Math.min(minX, point.x() - margin);
                    minY = Math.min(minY, point.y() - margin);
                    maxX = Math.max(maxX, point.x() + margin);
                    maxY = Math.max(maxY, point.y() + margin);
                }
            }
            return new Bounds(minX, minY, maxX, maxY);
        }

        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }
    }
}
