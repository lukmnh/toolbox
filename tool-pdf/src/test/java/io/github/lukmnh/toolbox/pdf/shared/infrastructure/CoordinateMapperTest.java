package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CoordinateMapperTest {

    private static final double EPSILON = 1e-9;

    /** CropBox 200×100 dengan offset (10, 20) dari MediaBox. */
    private static PageBox box(int rotation) {
        return new PageBox(10, 20, 200, 100, rotation);
    }

    @ParameterizedTest(name = "rotasi {0}: tampilan ({1}, {2}) → PDF ({3}, {4})")
    @MethodSource("knownCorners")
    void mapsDisplayCornersToPdfUserSpace(int rotation, double dx, double dy, double px, double py) {
        CoordinateMapper.Point pdf = new CoordinateMapper(box(rotation)).toPdf(dx, dy);

        assertEquals(px, pdf.x(), EPSILON);
        assertEquals(py, pdf.y(), EPSILON);
    }

    static Stream<Arguments> knownCorners() {
        return Stream.of(
                // Rotasi 0: kiri-atas tampilan = kiri-atas CropBox
                Arguments.of(0, 0, 0, 10, 120),
                Arguments.of(0, 200, 100, 210, 20),
                // Rotasi 90: tampilan 100×200, kiri-atas tampilan = kiri-bawah CropBox
                Arguments.of(90, 0, 0, 10, 20),
                Arguments.of(90, 100, 200, 210, 120),
                // Rotasi 180: kiri-atas tampilan = kanan-bawah CropBox
                Arguments.of(180, 0, 0, 210, 20),
                Arguments.of(180, 200, 100, 10, 120),
                // Rotasi 270: kiri-atas tampilan = kanan-atas CropBox
                Arguments.of(270, 0, 0, 210, 120),
                Arguments.of(270, 100, 200, 10, 20));
    }

    @ParameterizedTest(name = "rotasi {0}")
    @MethodSource("rotations")
    void displayToPdfRoundTrips(int rotation) {
        CoordinateMapper mapper = new CoordinateMapper(box(rotation));
        double[][] samples = {{0, 0}, {12.5, 33.25}, {mapper.displayWidth(), mapper.displayHeight()}, {77, 5}};

        for (double[] sample : samples) {
            CoordinateMapper.Point pdf = mapper.toPdf(sample[0], sample[1]);
            CoordinateMapper.Point back = mapper.toDisplay(pdf.x(), pdf.y());
            assertEquals(sample[0], back.x(), EPSILON);
            assertEquals(sample[1], back.y(), EPSILON);
        }
    }

    @ParameterizedTest(name = "rotasi {0}")
    @MethodSource("rotations")
    void previewPixelsRoundTripAtDoubleScale(int rotation) {
        CoordinateMapper mapper = new CoordinateMapper(box(rotation));

        CoordinateMapper.Point pdf = mapper.previewPixelsToPdf(40, 60);
        CoordinateMapper.Point pixels = mapper.pdfToPreviewPixels(pdf.x(), pdf.y());

        assertEquals(40, pixels.x(), EPSILON);
        assertEquals(60, pixels.y(), EPSILON);
        assertEquals(mapper.toPdf(20, 30), pdf);
    }

    static Stream<Integer> rotations() {
        return Stream.of(0, 90, 180, 270);
    }

    @Test
    void quarterTurnSwapsDisplaySize() {
        assertEquals(200, new CoordinateMapper(box(0)).displayWidth(), EPSILON);
        assertEquals(100, new CoordinateMapper(box(90)).displayWidth(), EPSILON);
        assertEquals(200, new CoordinateMapper(box(270)).displayHeight(), EPSILON);
    }

    @Test
    void normalizesNegativeAndFullTurnRotation() {
        assertEquals(270, new PageBox(0, 0, 1, 1, -90).rotation());
        assertEquals(90, new PageBox(0, 0, 1, 1, 450).rotation());
    }
}
