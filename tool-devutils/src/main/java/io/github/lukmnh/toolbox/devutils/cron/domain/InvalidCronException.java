package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

/** Ekspresi tidak valid untuk target terpilih; menyimpan cukup detail untuk pesan yang menunjuk field yang salah. */
public final class InvalidCronException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        EMPTY,
        FIELD_COUNT,
        INVALID_FIELD,
        UNSUPPORTED_SPECIAL,
        UNSUPPORTED_MACRO,
        /** {@code @reboot}: valid di crontab, tapi tidak punya jadwal waktu. */
        NO_SCHEDULE,
        /** Quartz: salah satu dari tanggal/hari-minggu wajib {@code ?}. */
        DAY_FIELDS_CONFLICT,
        INVALID_INTERVAL,
        INVALID_TIME_ZONE,
        INVALID_EXPRESSION
    }

    private final Reason reason;
    private final CronFieldKind field;
    private final String token;
    private final int min;
    private final int max;
    private final List<Integer> expectedCounts;
    private final int actualCount;

    private InvalidCronException(
            Reason reason,
            CronFieldKind field,
            String token,
            int min,
            int max,
            List<Integer> expectedCounts,
            int actualCount) {
        super("Ekspresi cron tidak valid: " + reason + (field == null ? "" : " di " + field));
        this.reason = reason;
        this.field = field;
        this.token = token;
        this.min = min;
        this.max = max;
        this.expectedCounts = List.copyOf(expectedCounts);
        this.actualCount = actualCount;
    }

    private static InvalidCronException of(Reason reason, CronFieldKind field, String token) {
        return new InvalidCronException(reason, field, token, 0, 0, List.of(), 0);
    }

    public static InvalidCronException empty() {
        return of(Reason.EMPTY, null, "");
    }

    public static InvalidCronException fieldCount(List<Integer> expected, int actual) {
        return new InvalidCronException(Reason.FIELD_COUNT, null, "", 0, 0, expected, actual);
    }

    /** @param min batas bawah nilai field menurut target, @param max batas atas */
    public static InvalidCronException invalidField(CronFieldKind field, String token, int min, int max) {
        return new InvalidCronException(Reason.INVALID_FIELD, field, token, min, max, List.of(), 0);
    }

    public static InvalidCronException unsupportedSpecial(CronFieldKind field, String token) {
        return of(Reason.UNSUPPORTED_SPECIAL, field, token);
    }

    public static InvalidCronException unsupportedMacro(String macro) {
        return of(Reason.UNSUPPORTED_MACRO, null, macro);
    }

    public static InvalidCronException noSchedule(String macro) {
        return of(Reason.NO_SCHEDULE, null, macro);
    }

    public static InvalidCronException dayFieldsConflict() {
        return of(Reason.DAY_FIELDS_CONFLICT, CronFieldKind.DAY_OF_WEEK, "");
    }

    public static InvalidCronException invalidInterval(String duration) {
        return of(Reason.INVALID_INTERVAL, null, duration);
    }

    public static InvalidCronException invalidTimeZone(String zone) {
        return of(Reason.INVALID_TIME_ZONE, null, zone);
    }

    public static InvalidCronException invalidExpression() {
        return of(Reason.INVALID_EXPRESSION, null, "");
    }

    public Reason reason() {
        return reason;
    }

    public Optional<CronFieldKind> field() {
        return Optional.ofNullable(field);
    }

    /** Potongan input yang bermasalah (field, macro, durasi, atau zona). */
    public String token() {
        return token;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public List<Integer> expectedCounts() {
        return expectedCounts;
    }

    public int actualCount() {
        return actualCount;
    }
}
