package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@code @every <durasi>} robfig/cron. Durasi memakai format {@code time.ParseDuration} Go ({@code 1h30m}, {@code 90s},
 * {@code 1.5h}); seperti robfig, dibulatkan ke bawah ke detik dan minimal 1 detik.
 *
 * @param text durasi persis seperti ditulis user
 */
public record GoInterval(String text, Duration delay) implements CronSpec, CronMeaning {

    private static final Duration MIN_DELAY = Duration.ofSeconds(1);
    private static final BigDecimal MAX_NANOS = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final Map<String, Long> NANOS_PER_UNIT = Map.of(
            "ns", 1L,
            "us", 1_000L,
            "µs", 1_000L,
            "μs", 1_000L,
            "ms", 1_000_000L,
            "s", 1_000_000_000L,
            "m", 60_000_000_000L,
            "h", 3_600_000_000_000L);

    public GoInterval {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(delay, "delay");
    }

    /** @throws InvalidCronException bila bukan durasi Go yang valid */
    public static GoInterval parse(String text) {
        BigDecimal nanos = parseNanos(text);
        if (nanos.abs().compareTo(MAX_NANOS) > 0) {
            throw InvalidCronException.invalidInterval(text);
        }
        Duration parsed = Duration.ofNanos(nanos.longValue()).truncatedTo(ChronoUnit.SECONDS);
        return new GoInterval(text, parsed.compareTo(MIN_DELAY) < 0 ? MIN_DELAY : parsed);
    }

    /** robfig menjalankan pertama kali {@code delay} setelah scheduler start, lalu berulang tiap {@code delay}. */
    public List<ZonedDateTime> nextRuns(ZonedDateTime from, int count) {
        List<ZonedDateTime> runs = new ArrayList<>(count);
        ZonedDateTime cursor = from.truncatedTo(ChronoUnit.SECONDS);
        for (int index = 0; index < count; index++) {
            cursor = cursor.plus(delay);
            runs.add(cursor);
        }
        return List.copyOf(runs);
    }

    private static BigDecimal parseNanos(String text) {
        int index = 0;
        boolean negative = false;
        if (!text.isEmpty() && (text.charAt(0) == '-' || text.charAt(0) == '+')) {
            negative = text.charAt(0) == '-';
            index++;
        }
        // Go menerima "0" tanpa satuan
        if (text.substring(index).equals("0")) {
            return BigDecimal.ZERO;
        }
        if (index >= text.length()) {
            throw InvalidCronException.invalidInterval(text);
        }
        BigDecimal total = BigDecimal.ZERO;
        while (index < text.length()) {
            int numberEnd = scan(text, index, true);
            int unitEnd = scan(text, numberEnd, false);
            Long unit = NANOS_PER_UNIT.get(text.substring(numberEnd, unitEnd));
            if (numberEnd == index || unit == null) {
                throw InvalidCronException.invalidInterval(text);
            }
            total = total.add(number(text, index, numberEnd).multiply(BigDecimal.valueOf(unit)));
            index = unitEnd;
        }
        return negative ? total.negate() : total;
    }

    /** Maju selama karakter adalah bagian angka ({@code numeric}) atau bagian satuan. */
    private static int scan(String text, int from, boolean numeric) {
        int index = from;
        while (index < text.length() && isNumberChar(text.charAt(index)) == numeric) {
            index++;
        }
        return index;
    }

    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }

    private static BigDecimal number(String text, int from, int to) {
        try {
            return new BigDecimal(text.substring(from, to));
        } catch (NumberFormatException e) {
            throw InvalidCronException.invalidInterval(text);
        }
    }
}
