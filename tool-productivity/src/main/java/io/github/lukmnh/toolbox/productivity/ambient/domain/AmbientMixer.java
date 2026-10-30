package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Mixer stereo: channel noise → gain per channel → master/mute → fade → soft limiter. Output selalu di rentang [-1, 1].
 *
 * <p>Perubahan gain di-ramp linear sepanjang satu blok dan fade di-ramp per sampel, sehingga tidak ada lompatan amplitudo
 * (bunyi klik/pop). Tidak ada alokasi di {@link #render}. Satu instance per sesi pemutaran, hanya dipakai thread audio
 * (tidak thread-safe). Deterministik untuk seed yang sama.
 */
public final class AmbientMixer {

    public static final double FADE_SECONDS = 2.0;

    private static final NoiseChannel[] CHANNELS = NoiseChannel.values();
    private static final int WHITE = NoiseChannel.WHITE.ordinal();
    private static final int PINK = NoiseChannel.PINK.ordinal();
    private static final int BROWN = NoiseChannel.BROWN.ordinal();
    private static final int RAIN = NoiseChannel.RAIN.ordinal();

    private final WhiteNoise whiteLeft;
    private final WhiteNoise whiteRight;
    private final PinkNoise pinkLeft;
    private final PinkNoise pinkRight;
    private final BrownNoise brownLeft;
    private final BrownNoise brownRight;
    private final RainNoise rain;
    private final double fadeStep;

    private final double[] gains = new double[CHANNELS.length];
    private final double[] gainTargets = new double[CHANNELS.length];
    private final double[] gainSteps = new double[CHANNELS.length];
    private double master;
    private double masterTarget;
    private double masterStep;
    private double fade;

    /** Kiri dan kanan memakai generator terpisah supaya stereo terdengar lebar (tidak mono di tengah). */
    public AmbientMixer(int sampleRate, RandomGenerator.SplittableGenerator random) {
        Objects.requireNonNull(random, "random");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate harus positif");
        }
        this.whiteLeft = new WhiteNoise(random.split());
        this.whiteRight = new WhiteNoise(random.split());
        this.pinkLeft = new PinkNoise(random.split());
        this.pinkRight = new PinkNoise(random.split());
        this.brownLeft = new BrownNoise(random.split());
        this.brownRight = new BrownNoise(random.split());
        this.rain = new RainNoise(random.split(), sampleRate);
        this.fadeStep = 1.0 / (FADE_SECONDS * sampleRate);
    }

    /**
     * Isi {@code frames} frame stereo interleaved ke {@code output}.
     *
     * @param audible {@code true} = fade menuju volume penuh, {@code false} = fade menuju senyap
     */
    public void render(float[] output, int frames, MixLevels levels, boolean audible) {
        if (frames <= 0 || output.length < frames * AudioSpec.CHANNELS) {
            throw new IllegalArgumentException("Buffer output terlalu kecil untuk " + frames + " frame");
        }
        prepareRamps(levels, frames);
        if (isSilent() && !audible) {
            // Sedang dijeda/berhenti: generator dilewati supaya CPU nyaris nol selama senyap
            Arrays.fill(output, 0, frames * AudioSpec.CHANNELS, 0f);
            finishRamps();
            return;
        }
        double fadeTarget = audible ? 1.0 : 0.0;
        for (int frame = 0; frame < frames; frame++) {
            fade = approach(fade, fadeTarget, fadeStep);
            stepRamps();
            renderFrame(output, frame * AudioSpec.CHANNELS);
        }
        finishRamps();
    }

    /** {@code true} bila fade sudah mencapai nol (aman menutup line tanpa klik). */
    public boolean isSilent() {
        return fade == 0;
    }

    private void renderFrame(float[] output, int offset) {
        rain.advance();
        double left = gains[WHITE] * whiteLeft.next()
                + gains[PINK] * pinkLeft.next()
                + gains[BROWN] * brownLeft.next()
                + gains[RAIN] * rain.left();
        double right = gains[WHITE] * whiteRight.next()
                + gains[PINK] * pinkRight.next()
                + gains[BROWN] * brownRight.next()
                + gains[RAIN] * rain.right();
        // Fade dikuadratkan: penurunan loudness terdengar lebih rata daripada fade amplitudo linear
        double outputGain = master * fade * fade;
        output[offset] = softLimit(left * outputGain);
        output[offset + 1] = softLimit(right * outputGain);
    }

    private void prepareRamps(MixLevels levels, int frames) {
        for (NoiseChannel channel : CHANNELS) {
            int index = channel.ordinal();
            gainTargets[index] = loudness(levels.volume(channel));
            gainSteps[index] = (gainTargets[index] - gains[index]) / frames;
        }
        masterTarget = levels.muted() ? 0 : loudness(levels.master());
        masterStep = (masterTarget - master) / frames;
    }

    private void stepRamps() {
        for (int index = 0; index < gains.length; index++) {
            gains[index] += gainSteps[index];
        }
        master += masterStep;
    }

    /** Set tepat ke target supaya error pembulatan ramp tidak menumpuk antar blok. */
    private void finishRamps() {
        System.arraycopy(gainTargets, 0, gains, 0, gains.length);
        master = masterTarget;
    }

    /** Loudness terasa logaritmik; kuadrat adalah pendekatan murah supaya slider terasa linear di telinga. */
    private static double loudness(double sliderValue) {
        return sliderValue * sliderValue;
    }

    /** tanh: transparan di level rendah, menekan puncak secara halus sehingga tidak pernah clipping keras. */
    private static float softLimit(double sample) {
        return (float) Math.tanh(sample);
    }

    private static double approach(double current, double target, double step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }
}
