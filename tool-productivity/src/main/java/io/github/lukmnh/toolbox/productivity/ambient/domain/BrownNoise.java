package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Brown noise (-6 dB/oktaf): random walk dengan leak. Tidak thread-safe. */
public final class BrownNoise {

    /** Langkah per sampel; makin kecil makin "gelap" dan makin lambat bergerak. */
    private static final double STEP = 0.02;
    /** Leak < 1 menarik walk kembali ke nol supaya tidak drift (DC) dan tidak keluar rentang. */
    private static final double LEAK = 1 / 1.02;
    /** Disetel empiris: RMS ≈ 0,15; sedikit lebih tinggi dari pink karena energi di bass terdengar lebih pelan. */
    private static final double OUTPUT_GAIN = 2.5;

    private final RandomGenerator random;
    private double level;

    public BrownNoise(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public double next() {
        level = (level + random.nextDouble(-1.0, 1.0) * STEP) * LEAK;
        return level * OUTPUT_GAIN;
    }
}
