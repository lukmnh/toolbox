package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Hujan stereo: pink noise ter-filter band-pass (desis hujan jauh) + tetesan acak di dekat pendengar. Tetesan berupa
 * burst noise pendek dengan envelope attack/decay, waktu antar tetesan mengikuti proses Poisson (interval eksponensial)
 * dan posisi stereo acak. Tidak thread-safe.
 */
public final class RainNoise {

    private static final double BED_HIGH_PASS_HZ = 400;
    private static final double BED_LOW_PASS_HZ = 6_000;
    /** Band-pass membuang sebagian energi pink; dikompensasi supaya RMS total ≈ 0,13. */
    private static final double BED_GAIN = 2.0;

    private static final double DROPS_PER_SECOND = 12;
    /** Attack singkat (bukan langsung puncak) supaya tetesan terdengar "tik", bukan klik digital. */
    private static final double DROP_ATTACK_SECONDS = 0.0008;

    private static final double DROP_MIN_DECAY_SECONDS = 0.003;
    private static final double DROP_MAX_DECAY_SECONDS = 0.015;
    private static final double DROP_MIN_PEAK = 0.05;
    private static final double DROP_MAX_PEAK = 0.35;
    private static final double DROP_SILENCE = 1e-4;
    private static final double QUARTER_TURN = Math.PI / 2;

    private final RandomGenerator random;
    private final int sampleRate;
    private final PinkNoise bedLeft;
    private final PinkNoise bedRight;
    private final OnePoleLowPass rumbleLeft;
    private final OnePoleLowPass rumbleRight;
    private final OnePoleLowPass airLeft;
    private final OnePoleLowPass airRight;
    private final int attackSamples;
    private final double meanDropIntervalSamples;

    private long samplesUntilDrop;
    private boolean dropActive;
    private int attackRemaining;
    private double attackIncrement;
    private double envelope;
    private double decayFactor;
    private double panLeft;
    private double panRight;
    private double left;
    private double right;

    public RainNoise(RandomGenerator.SplittableGenerator random, int sampleRate) {
        this.random = Objects.requireNonNull(random, "random");
        this.sampleRate = sampleRate;
        this.bedLeft = new PinkNoise(random.split());
        this.bedRight = new PinkNoise(random.split());
        this.rumbleLeft = new OnePoleLowPass(BED_HIGH_PASS_HZ, sampleRate);
        this.rumbleRight = new OnePoleLowPass(BED_HIGH_PASS_HZ, sampleRate);
        this.airLeft = new OnePoleLowPass(BED_LOW_PASS_HZ, sampleRate);
        this.airRight = new OnePoleLowPass(BED_LOW_PASS_HZ, sampleRate);
        this.attackSamples = Math.max(1, (int) Math.round(DROP_ATTACK_SECONDS * sampleRate));
        this.meanDropIntervalSamples = sampleRate / DROPS_PER_SECOND;
        this.samplesUntilDrop = nextDropInterval();
    }

    /** Hitung satu frame; baca hasilnya lewat {@link #left()} dan {@link #right()}. */
    public void advance() {
        double drop = nextDropSample();
        left = bandPass(bedLeft.next(), rumbleLeft, airLeft) * BED_GAIN + drop * panLeft;
        right = bandPass(bedRight.next(), rumbleRight, airRight) * BED_GAIN + drop * panRight;
    }

    public double left() {
        return left;
    }

    public double right() {
        return right;
    }

    /** High-pass = input dikurangi komponen low-pass-nya; dipakai untuk membuang dengung rendah. */
    private static double bandPass(double input, OnePoleLowPass rumble, OnePoleLowPass air) {
        return air.process(input - rumble.process(input));
    }

    private double nextDropSample() {
        samplesUntilDrop--;
        if (samplesUntilDrop <= 0) {
            triggerDrop();
        }
        if (!dropActive) {
            return 0;
        }
        advanceEnvelope();
        return random.nextDouble(-1.0, 1.0) * envelope;
    }

    /** Satu voice saja: tetesan baru menimpa ekor tetesan lama, mulai dari envelope saat ini supaya tidak lompat. */
    private void triggerDrop() {
        samplesUntilDrop = nextDropInterval();
        double peak = random.nextDouble(DROP_MIN_PEAK, DROP_MAX_PEAK);
        attackRemaining = attackSamples;
        attackIncrement = (peak - envelope) / attackSamples;
        double decaySeconds = random.nextDouble(DROP_MIN_DECAY_SECONDS, DROP_MAX_DECAY_SECONDS);
        decayFactor = Math.exp(-1.0 / (decaySeconds * sampleRate));
        // Constant-power pan: loudness sama di posisi mana pun
        double angle = random.nextDouble() * QUARTER_TURN;
        panLeft = Math.cos(angle);
        panRight = Math.sin(angle);
        dropActive = true;
    }

    private void advanceEnvelope() {
        if (attackRemaining > 0) {
            attackRemaining--;
            envelope += attackIncrement;
            return;
        }
        envelope *= decayFactor;
        if (envelope < DROP_SILENCE) {
            envelope = 0;
            dropActive = false;
        }
    }

    private long nextDropInterval() {
        return Math.max(1, Math.round(random.nextExponential() * meanDropIntervalSamples));
    }
}
