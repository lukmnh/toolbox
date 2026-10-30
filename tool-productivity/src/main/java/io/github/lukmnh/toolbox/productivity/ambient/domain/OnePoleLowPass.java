package io.github.lukmnh.toolbox.productivity.ambient.domain;

/** Filter low-pass one-pole (6 dB/oktaf). Cukup untuk membentuk warna noise; tidak thread-safe. */
public final class OnePoleLowPass {

    private final double coefficient;
    private double output;

    public OnePoleLowPass(double cutoffHz, int sampleRate) {
        if (cutoffHz <= 0 || sampleRate <= 0) {
            throw new IllegalArgumentException("cutoff dan sample rate harus positif");
        }
        this.coefficient = 1 - Math.exp(-2 * Math.PI * cutoffHz / sampleRate);
    }

    public double process(double input) {
        output += coefficient * (input - output);
        return output;
    }
}
