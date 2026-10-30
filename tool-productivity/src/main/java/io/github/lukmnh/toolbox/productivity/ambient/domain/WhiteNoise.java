package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** White noise uniform. Deterministik untuk seed {@link RandomGenerator} yang sama. Tidak thread-safe. */
public final class WhiteNoise {

    /**
     * Uniform [-1, 1] punya RMS ≈ 0,58. Diturunkan ke RMS ≈ 0,14 (setara channel lain) karena telinga paling peka di
     * frekuensi tinggi, tempat energi white noise paling besar.
     */
    private static final double OUTPUT_GAIN = 0.25;

    private final RandomGenerator random;

    public WhiteNoise(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public double next() {
        return random.nextDouble(-1.0, 1.0) * OUTPUT_GAIN;
    }
}
