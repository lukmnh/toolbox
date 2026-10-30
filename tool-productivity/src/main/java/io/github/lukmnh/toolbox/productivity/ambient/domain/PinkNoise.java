package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Pink noise (-3 dB/oktaf) dengan filter "refined" Paul Kellet: tujuh filter one-pole paralel atas white noise. Akurat
 * ±0,05 dB di atas 9,2 Hz untuk 44,1 kHz, jauh lebih murah daripada Voss-McCartney. Tidak thread-safe.
 */
public final class PinkNoise {

    private static final double[] POLES = {0.99886, 0.99332, 0.96900, 0.86650, 0.55000, -0.7616};
    private static final double[] INPUT_GAINS = {0.0555179, 0.0750759, 0.1538520, 0.3104856, 0.5329522, -0.0168980};
    private static final double DIRECT_GAIN = 0.5362;
    private static final double DELAYED_GAIN = 0.115926;
    /** Disetel empiris: RMS ≈ 0,12, setara loudness channel lain di posisi slider yang sama. */
    private static final double OUTPUT_GAIN = 0.07;

    private final RandomGenerator random;
    private final double[] poles = new double[POLES.length];
    private double delayed;

    public PinkNoise(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public double next() {
        double white = random.nextDouble(-1.0, 1.0);
        double sum = white * DIRECT_GAIN + delayed;
        for (int index = 0; index < POLES.length; index++) {
            poles[index] = POLES[index] * poles[index] + white * INPUT_GAINS[index];
            sum += poles[index];
        }
        delayed = white * DELAYED_GAIN;
        return sum * OUTPUT_GAIN;
    }
}
