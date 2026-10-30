package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

/**
 * CropBox halaman (koordinat user space PDF) dan rotasinya.
 *
 * @param rotation rotasi tampilan searah jarum jam; dinormalisasi ke 0, 90, 180, atau 270
 */
public record PageBox(double lowerLeftX, double lowerLeftY, double width, double height, int rotation) {

    private static final int FULL_TURN = 360;
    private static final int QUARTER_TURN = 90;

    public PageBox {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Page size must be positive: " + width + "x" + height);
        }
        rotation = normalize(rotation);
    }

    private static int normalize(int rotation) {
        int normalized = Math.floorMod(rotation, FULL_TURN);
        if (normalized % QUARTER_TURN != 0) {
            throw new IllegalArgumentException("Rotation must be a multiple of 90: " + rotation);
        }
        return normalized;
    }

    public boolean isQuarterTurned() {
        return rotation == 90 || rotation == 270;
    }
}
