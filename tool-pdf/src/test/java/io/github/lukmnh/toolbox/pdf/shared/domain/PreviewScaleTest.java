package io.github.lukmnh.toolbox.pdf.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PreviewScaleTest {

    @ParameterizedTest
    @CsvSource({"1, 144", "1.25, 180", "1.5, 216", "2, 288"})
    void renderDpiFollowsScreenScale(double outputScale, int expectedDpi) {
        assertEquals(expectedDpi, PreviewScale.renderDpi(outputScale));
    }

    @ParameterizedTest
    @CsvSource({"0.5, 144", "3, 288", "4, 288"})
    void renderDpiIsClampedBetweenBaseAndMaximum(double outputScale, int expectedDpi) {
        assertEquals(expectedDpi, PreviewScale.renderDpi(outputScale));
    }
}
