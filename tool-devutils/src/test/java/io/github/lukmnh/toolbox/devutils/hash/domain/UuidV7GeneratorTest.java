package io.github.lukmnh.toolbox.devutils.hash.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

    private static final long NOW = 1_717_000_000_123L;

    @Test
    void setsVersionVariantAndTimestamp() {
        UuidV7Generator generator = new UuidV7Generator(() -> NOW, new SecureRandom());

        UUID uuid = generator.next();

        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
        assertEquals(NOW, uuid.getMostSignificantBits() >>> 16);
        // Format teks: karakter ke-15 = versi, karakter ke-20 = varian 8/9/a/b
        String text = uuid.toString();
        assertEquals('7', text.charAt(14));
        assertTrue("89ab".indexOf(text.charAt(19)) >= 0);
    }

    @Test
    void uuidsInSameMillisecondStayStrictlyOrdered() {
        UuidV7Generator generator = new UuidV7Generator(() -> NOW, new SecureRandom());

        // Lebih dari 2048 panggilan memaksa counter rollover dan timestamp maju 1 ms
        List<UUID> uuids = generate(generator, 5000);

        assertStrictlyAscending(uuids);
        assertTrue(timestamp(uuids.getLast()) > NOW);
    }

    @Test
    void timestampsAreMonotonicEvenWhenClockGoesBackwards() {
        AtomicLong clock = new AtomicLong(NOW);
        UuidV7Generator generator = new UuidV7Generator(clock::get, new SecureRandom());

        UUID first = generator.next();
        clock.set(NOW - 10_000);
        UUID afterRewind = generator.next();
        clock.set(NOW + 1);
        UUID later = generator.next();

        assertStrictlyAscending(List.of(first, afterRewind, later));
        assertEquals(NOW, timestamp(afterRewind));
        assertEquals(NOW + 1, timestamp(later));
    }

    @Test
    void timestampFollowsRealClock() {
        long before = System.currentTimeMillis();
        UUID uuid = new UuidV7Generator(System::currentTimeMillis, new SecureRandom()).next();
        long after = System.currentTimeMillis();

        assertTrue(timestamp(uuid) >= before && timestamp(uuid) <= after);
    }

    private static List<UUID> generate(UuidV7Generator generator, int count) {
        List<UUID> uuids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            uuids.add(generator.next());
        }
        return uuids;
    }

    private static long timestamp(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }

    /** Urutan sort UUID v7 = urutan byte/teks; {@link UUID#compareTo} membandingkan signed, jadi pakai teks. */
    private static void assertStrictlyAscending(List<UUID> uuids) {
        for (int index = 1; index < uuids.size(); index++) {
            String previous = uuids.get(index - 1).toString();
            String current = uuids.get(index).toString();
            assertTrue(previous.compareTo(current) < 0, previous + " >= " + current);
            assertEquals(7, uuids.get(index).version());
            assertEquals(2, uuids.get(index).variant());
        }
    }
}
