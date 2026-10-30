package io.github.lukmnh.toolbox.productivity.ambient.domain;

/**
 * Snapshot pengaturan mixer. Immutable supaya bisa diganti utuh dari thread UI lewat satu field {@code volatile} dan
 * dibaca thread audio tanpa lock. Volume adalah posisi slider [0, 1] (gain audio, bukan persen uang), kurva
 * loudness diterapkan {@link AmbientMixer}.
 */
public record MixLevels(double white, double pink, double brown, double rain, double master, boolean muted) {

    private static final double DEFAULT_RAIN = 0.7;
    private static final double DEFAULT_BROWN = 0.3;
    private static final double DEFAULT_MASTER = 0.7;

    public static final MixLevels DEFAULT = new MixLevels(0, 0, DEFAULT_BROWN, DEFAULT_RAIN, DEFAULT_MASTER, false);

    public MixLevels {
        requireLevel(white, "white");
        requireLevel(pink, "pink");
        requireLevel(brown, "brown");
        requireLevel(rain, "rain");
        requireLevel(master, "master");
    }

    public double volume(NoiseChannel channel) {
        return switch (channel) {
            case WHITE -> white;
            case PINK -> pink;
            case BROWN -> brown;
            case RAIN -> rain;
        };
    }

    /** Ditulis terbalik supaya {@code NaN} juga ditolak. */
    private static void requireLevel(double value, String name) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(name + " harus di rentang [0, 1]: " + value);
        }
    }
}
