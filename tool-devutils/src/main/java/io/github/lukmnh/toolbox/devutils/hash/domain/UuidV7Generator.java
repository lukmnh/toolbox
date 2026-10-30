package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * UUID versi 7 (RFC 9562 §5.7) dengan counter monotonic (§6.2 metode 1).
 *
 * <p>Layout: 48 bit timestamp Unix ms | 4 bit versi {@code 0111} | 12 bit {@code rand_a} sebagai counter | 2 bit
 * varian {@code 10} | 62 bit acak. Counter diisi acak (bit teratas 0) tiap milidetik baru, lalu dinaikkan bila
 * dipanggil lagi di milidetik yang sama atau jam mundur, sehingga urutan generate = urutan sort. Saat counter
 * habis, timestamp dimajukan 1 ms seperti yang diizinkan RFC. Thread-safe.
 */
public final class UuidV7Generator {

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final int COUNTER_MAX = 0xFFF;
    /** Seed 11 bit (bit teratas counter 0) menyisakan minimal 2048 increment sebelum rollover. */
    private static final int COUNTER_SEED_BOUND = 0x800;

    private static final long VERSION_BITS = 0x7000L;
    private static final long VARIANT_BITS = 0x8000_0000_0000_0000L;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private final LongSupplier clockMillis;
    private final RandomGenerator random;
    private long lastMillis = -1;
    private int counter;

    /**
     * @param clockMillis sumber waktu Unix ms, biasanya {@code System::currentTimeMillis}
     * @param random sumber acak; produksi wajib {@code SecureRandom}
     */
    public UuidV7Generator(LongSupplier clockMillis, RandomGenerator random) {
        this.clockMillis = clockMillis;
        this.random = random;
    }

    public synchronized UUID next() {
        long now = clockMillis.getAsLong() & TIMESTAMP_MASK;
        if (now > lastMillis) {
            lastMillis = now;
            counter = random.nextInt(COUNTER_SEED_BOUND);
        } else if (counter < COUNTER_MAX) {
            counter++;
        } else {
            lastMillis = (lastMillis + 1) & TIMESTAMP_MASK;
            counter = random.nextInt(COUNTER_SEED_BOUND);
        }
        long mostSignificant = (lastMillis << 16) | VERSION_BITS | counter;
        long leastSignificant = VARIANT_BITS | (random.nextLong() & RAND_B_MASK);
        return new UUID(mostSignificant, leastSignificant);
    }
}
