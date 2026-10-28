package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Format waktu claim: {@code Sen, 2 Nov 2026 08:00 WIB} + ISO 8601 + waktu relatif dalam bahasa Indonesia. */
final class JwtTimeFormat {

    private static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm z", Messages.LOCALE);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 3_600;
    private static final long SECONDS_PER_DAY = 86_400;
    private static final long DAYS_PER_YEAR = 365;

    private JwtTimeFormat() {}

    /** @param claimName {@code exp}, {@code nbf}, atau {@code iat}; menentukan kalimat relatifnya */
    static String describe(String claimName, Instant time, Instant now, ZoneId zone) {
        ZonedDateTime zoned = time.atZone(zone).truncatedTo(ChronoUnit.SECONDS);
        return Messages.format(
                "devutils.jwt.time.value", HUMAN.format(zoned), relative(claimName, time, now), ISO.format(zoned));
    }

    private static String relative(String claimName, Instant time, Instant now) {
        Duration distance = Duration.between(now, time);
        String direction = distance.isNegative() ? "past" : "future";
        return Messages.format("devutils.jwt.relative." + claimName + "." + direction, amount(distance.abs()));
    }

    /** Satu satuan terbesar yang masuk akal, dibulatkan ke bawah: 90 menit → "1 jam". */
    private static String amount(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < SECONDS_PER_MINUTE) {
            return Messages.format("devutils.jwt.duration.seconds", seconds);
        }
        if (seconds < SECONDS_PER_HOUR) {
            return Messages.format("devutils.jwt.duration.minutes", seconds / SECONDS_PER_MINUTE);
        }
        if (seconds < SECONDS_PER_DAY) {
            return Messages.format("devutils.jwt.duration.hours", seconds / SECONDS_PER_HOUR);
        }
        long days = seconds / SECONDS_PER_DAY;
        if (days < DAYS_PER_YEAR) {
            return Messages.format("devutils.jwt.duration.days", days);
        }
        return Messages.format("devutils.jwt.duration.years", days / DAYS_PER_YEAR);
    }
}
