package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Preset → ekspresi sesuai target: detik {@code 0} di depan untuk Spring/Quartz/Go-dengan-detik, {@code ?} di Quartz,
 * nama hari ({@code MON-FRI}) untuk Java karena penomoran Quartz (1 = Minggu) mudah tertukar.
 */
public final class CronBuilder {

    private static final String ANY = "*";
    private static final String NO_SPECIFIC = "?";
    private static final String SECOND_ZERO = "0";
    private static final int UNIX_SUNDAY = 0;
    /** Posisi di daftar 5 field (sebelum detik ditambahkan). */
    private static final int DAY_OF_MONTH_INDEX = CronFieldKind.FIVE.indexOf(CronFieldKind.DAY_OF_MONTH);

    private static final int DAY_OF_WEEK_INDEX = CronFieldKind.FIVE.indexOf(CronFieldKind.DAY_OF_WEEK);

    private CronBuilder() {}

    public static String build(CronPreset preset, CronTarget target, boolean goWithSeconds) {
        List<String> fields = new ArrayList<>(fiveFields(preset, target));
        if (target == CronTarget.QUARKUS) {
            useNoSpecificDay(fields);
        }
        if (target.hasSecondsField(goWithSeconds)) {
            fields.addFirst(SECOND_ZERO);
        }
        return String.join(" ", fields);
    }

    /** Menit, jam, tanggal, bulan, hari-minggu. */
    private static List<String> fiveFields(CronPreset preset, CronTarget target) {
        return switch (preset) {
            case CronPreset.EveryMinutes p -> List.of(p.minutes() == 1 ? ANY : "*/" + p.minutes(), ANY, ANY, ANY, ANY);
            case CronPreset.Hourly p -> List.of(String.valueOf(p.minute()), ANY, ANY, ANY, ANY);
            case CronPreset.Daily p -> atTime(p.time(), ANY, ANY);
            case CronPreset.Weekdays p -> atTime(p.time(), ANY, weekdays(target));
            case CronPreset.Weekly p -> atTime(p.time(), ANY, day(p.day(), target));
            case CronPreset.Monthly p -> atTime(p.time(), String.valueOf(p.dayOfMonth()), ANY);
            case CronPreset.Custom p ->
                List.of(
                        orAny(p.minute()),
                        orAny(p.hour()),
                        orAny(p.dayOfMonth()),
                        orAny(p.month()),
                        orAny(p.dayOfWeek()));
        };
    }

    private static List<String> atTime(LocalTime time, String dayOfMonth, String dayOfWeek) {
        return List.of(String.valueOf(time.getMinute()), String.valueOf(time.getHour()), dayOfMonth, ANY, dayOfWeek);
    }

    /** Quartz menolak tanggal dan hari-minggu yang sama-sama terisi; salah satunya harus {@code ?}. */
    private static void useNoSpecificDay(List<String> fields) {
        if (fields.get(DAY_OF_WEEK_INDEX).equals(ANY)) {
            fields.set(DAY_OF_WEEK_INDEX, NO_SPECIFIC);
        } else if (fields.get(DAY_OF_MONTH_INDEX).equals(ANY)) {
            fields.set(DAY_OF_MONTH_INDEX, NO_SPECIFIC);
        }
    }

    private static String weekdays(CronTarget target) {
        return usesDayNames(target)
                ? name(DayOfWeek.MONDAY) + "-" + name(DayOfWeek.FRIDAY)
                : DayOfWeek.MONDAY.getValue() + "-" + DayOfWeek.FRIDAY.getValue();
    }

    private static String day(DayOfWeek day, CronTarget target) {
        if (usesDayNames(target)) {
            return name(day);
        }
        return String.valueOf(day == DayOfWeek.SUNDAY ? UNIX_SUNDAY : day.getValue());
    }

    private static boolean usesDayNames(CronTarget target) {
        return target == CronTarget.SPRING || target == CronTarget.QUARKUS;
    }

    private static String name(DayOfWeek day) {
        return day.getDisplayName(TextStyle.SHORT, Locale.ROOT).toUpperCase(Locale.ROOT);
    }

    private static String orAny(String value) {
        String trimmed = value.strip();
        return trimmed.isEmpty() ? ANY : trimmed;
    }
}
