package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronFieldKind;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronMeaning;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronSchedule;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.FieldExpr;
import io.github.lukmnh.toolbox.devutils.cron.domain.GoInterval;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Deskripsi bahasa Indonesia dari model hasil parse (bukan dari string ekspresi), contoh {@code *}{@code /15 8-17 * * 1-5}
 * → "Setiap 15 menit, dari jam 08:00 sampai 17:59, hari Senin sampai Jumat".
 *
 * <p>Semua angka dikirim ke {@link Messages#format} sebagai String supaya tahun 2027 tidak menjadi "2.027".
 */
final class CronDescriber {

    /** Lebih dari ini, daftar jam ditulis per field ("jam 8, 9, … pada menit 0") bukan "pukul 08:00, …". */
    private static final int MAX_LISTED_TIMES = 6;

    private static final String SEPARATOR = ", ";
    private static final int LAST_MINUTE = 59;

    private CronDescriber() {}

    static String describe(CronMeaning meaning) {
        return switch (meaning) {
            case CronSchedule schedule -> capitalize(schedule(schedule));
            case GoInterval interval ->
                capitalize(Messages.format("devutils.cron.desc.interval", duration(interval.delay())));
        };
    }

    /** Arti satu field untuk tabel rincian, contoh "Setiap 15 menit". */
    static String field(CronFieldKind kind, FieldExpr expr) {
        return capitalize(phrase(kind, expr));
    }

    private static String schedule(CronSchedule schedule) {
        DayText days = days(schedule);
        List<String> parts = new ArrayList<>();
        Optional<String> times = fixedTimes(schedule);
        if (times.isPresent()) {
            String when = days.text().isEmpty()
                    ? Messages.get("devutils.cron.desc.everyDay")
                    : days.recurring() ? days.text() : Messages.format("devutils.cron.desc.onDays", days.text());
            parts.add(Messages.format("devutils.cron.desc.atTimes", when, times.get()));
        } else {
            parts.add(time(schedule));
            if (!days.text().isEmpty()) {
                parts.add(days.text());
            }
        }
        restriction(CronFieldKind.MONTH, schedule.field(CronFieldKind.MONTH)).ifPresent(parts::add);
        restriction(CronFieldKind.YEAR, schedule.field(CronFieldKind.YEAR)).ifPresent(parts::add);
        return String.join(SEPARATOR, parts);
    }

    // ---------- Waktu ----------

    /** "pukul 08:00, 12:00, dan 17:00" bila menit & jam semuanya nilai tetap (dan detik tetap). */
    private static Optional<String> fixedTimes(CronSchedule schedule) {
        FieldExpr second = schedule.field(CronFieldKind.SECOND);
        boolean secondsShown = secondsMatter(schedule);
        Optional<List<Integer>> seconds = secondsShown ? values(second) : Optional.of(List.of(0));
        Optional<List<Integer>> minutes = values(schedule.field(CronFieldKind.MINUTE));
        Optional<List<Integer>> hours = values(schedule.field(CronFieldKind.HOUR));
        if (seconds.isEmpty()
                || minutes.isEmpty()
                || hours.isEmpty()
                || seconds.get().size() != 1) {
            return Optional.empty();
        }
        if (minutes.get().size() * hours.get().size() > MAX_LISTED_TIMES) {
            return Optional.empty();
        }
        List<String> times = new ArrayList<>();
        for (int hour : hours.get()) {
            for (int minute : minutes.get()) {
                times.add(
                        secondsShown
                                ? "%02d:%02d:%02d"
                                        .formatted(hour, minute, seconds.get().getFirst())
                                : "%02d:%02d".formatted(hour, minute));
            }
        }
        return Optional.of(joinAnd(times));
    }

    private static String time(CronSchedule schedule) {
        FieldExpr minute = schedule.field(CronFieldKind.MINUTE);
        FieldExpr hour = schedule.field(CronFieldKind.HOUR);
        String core = minuteAndHour(minute, hour);
        if (!secondsMatter(schedule)) {
            return core;
        }
        String seconds = seconds(schedule.field(CronFieldKind.SECOND));
        boolean everyMinuteOfEveryHour = minute instanceof FieldExpr.Any && hour instanceof FieldExpr.Any;
        return everyMinuteOfEveryHour ? seconds : seconds + SEPARATOR + core;
    }

    /** Detik 0 (atau tidak ada field detik) tidak perlu disebut. */
    private static boolean secondsMatter(CronSchedule schedule) {
        return schedule.hasSeconds() && !schedule.field(CronFieldKind.SECOND).equals(new FieldExpr.Value(0));
    }

    private static String seconds(FieldExpr second) {
        if (second instanceof FieldExpr.Any) {
            return Messages.get("devutils.cron.desc.everySecond");
        }
        if (second instanceof FieldExpr.Value value) {
            return Messages.format("devutils.cron.desc.atSecond", String.valueOf(value.value()));
        }
        return every(second)
                .map(step -> Messages.format("devutils.cron.desc.everyNSeconds", step))
                .orElseGet(() -> phrase(CronFieldKind.SECOND, second));
    }

    private static String minuteAndHour(FieldExpr minute, FieldExpr hour) {
        if (hour instanceof FieldExpr.Any) {
            return minutesOfEveryHour(minute);
        }
        Optional<String> hourStep = every(hour);
        if (hourStep.isPresent() && minute instanceof FieldExpr.Value value) {
            return value.value() == 0
                    ? Messages.format("devutils.cron.desc.everyNHoursSharp", hourStep.get())
                    : Messages.format(
                            "devutils.cron.desc.everyNHoursAt", hourStep.get(), String.valueOf(value.value()));
        }
        Optional<int[]> window = window(hour);
        if (window.isPresent()) {
            return minutesInWindow(minute, window.get());
        }
        return phrase(CronFieldKind.MINUTE, minute) + SEPARATOR + phrase(CronFieldKind.HOUR, hour);
    }

    private static String minutesOfEveryHour(FieldExpr minute) {
        if (minute instanceof FieldExpr.Any) {
            return Messages.get("devutils.cron.desc.everyMinute");
        }
        if (minute instanceof FieldExpr.Value value) {
            return value.value() == 0
                    ? Messages.get("devutils.cron.desc.everyHourSharp")
                    : Messages.format("devutils.cron.desc.everyHourAt", String.valueOf(value.value()));
        }
        return every(minute)
                .map(step -> Messages.format("devutils.cron.desc.everyNMinutes", step))
                .orElseGet(() -> phrase(CronFieldKind.MINUTE, minute)
                        + SEPARATOR
                        + Messages.get("devutils.cron.desc.everyHour"));
    }

    /** Jam berupa satu nilai atau rentang: "dari jam 08:00 sampai 17:59". */
    private static String minutesInWindow(FieldExpr minute, int[] window) {
        if (minute instanceof FieldExpr.Value value) {
            return Messages.format(
                    "devutils.cron.desc.hourlyWindow",
                    String.valueOf(value.value()),
                    clock(window[0], value.value()),
                    clock(window[1], value.value()));
        }
        String range = Messages.format("devutils.cron.desc.window", clock(window[0], 0), clock(window[1], LAST_MINUTE));
        if (minute instanceof FieldExpr.Any) {
            return Messages.get("devutils.cron.desc.everyMinute") + SEPARATOR + range;
        }
        return every(minute)
                .map(step -> Messages.format("devutils.cron.desc.everyNMinutes", step) + SEPARATOR + range)
                .orElseGet(() -> phrase(CronFieldKind.MINUTE, minute) + SEPARATOR + range);
    }

    private static Optional<int[]> window(FieldExpr hour) {
        return switch (hour) {
            case FieldExpr.Value value -> Optional.of(new int[] {value.value(), value.value()});
            case FieldExpr.Range range -> Optional.of(new int[] {range.from(), range.to()});
            default -> Optional.empty();
        };
    }

    /** {@code *}{@code /n} atau {@code 0/n}: langkah n dari awal. */
    private static Optional<String> every(FieldExpr expr) {
        if (expr instanceof FieldExpr.Step step
                && (step.start() instanceof FieldExpr.Any || step.start().equals(new FieldExpr.Value(0)))) {
            return Optional.of(String.valueOf(step.step()));
        }
        return Optional.empty();
    }

    /** Semua nilai tetap (satu nilai atau daftar nilai), urut seperti ditulis. */
    private static Optional<List<Integer>> values(FieldExpr expr) {
        if (expr instanceof FieldExpr.Value value) {
            return Optional.of(List.of(value.value()));
        }
        if (expr instanceof FieldExpr.ListOf list
                && list.items().stream().allMatch(FieldExpr.Value.class::isInstance)) {
            return Optional.of(list.items().stream()
                    .map(item -> ((FieldExpr.Value) item).value())
                    .toList());
        }
        return Optional.empty();
    }

    // ---------- Hari, bulan, tahun ----------

    /** @param recurring teks sudah diawali "setiap" (langkah), jadi tidak perlu ditambah lagi */
    private record DayText(String text, boolean recurring) {}

    private static DayText days(CronSchedule schedule) {
        FieldExpr dayOfMonth = schedule.field(CronFieldKind.DAY_OF_MONTH);
        FieldExpr dayOfWeek = schedule.field(CronFieldKind.DAY_OF_WEEK);
        boolean monthDayRestricted = !(dayOfMonth instanceof FieldExpr.Any);
        boolean weekDayRestricted = !(dayOfWeek instanceof FieldExpr.Any);
        if (monthDayRestricted && weekDayRestricted) {
            String key = schedule.target().dayMatching() == CronTarget.DayMatching.EITHER
                    ? "devutils.cron.desc.daysEither"
                    : "devutils.cron.desc.daysBoth";
            return new DayText(
                    Messages.format(
                            key,
                            phrase(CronFieldKind.DAY_OF_MONTH, dayOfMonth),
                            phrase(CronFieldKind.DAY_OF_WEEK, dayOfWeek)),
                    false);
        }
        if (monthDayRestricted) {
            return new DayText(phrase(CronFieldKind.DAY_OF_MONTH, dayOfMonth), dayOfMonth instanceof FieldExpr.Step);
        }
        if (weekDayRestricted) {
            return new DayText(phrase(CronFieldKind.DAY_OF_WEEK, dayOfWeek), dayOfWeek instanceof FieldExpr.Step);
        }
        return new DayText("", false);
    }

    /** Bulan/tahun: satu nilai ditulis "hanya bulan Januari", selain itu frasa biasa. */
    private static Optional<String> restriction(CronFieldKind kind, FieldExpr expr) {
        if (expr instanceof FieldExpr.Any) {
            return Optional.empty();
        }
        String text = phrase(kind, expr);
        return Optional.of(expr instanceof FieldExpr.Value ? Messages.format("devutils.cron.desc.only", text) : text);
    }

    // ---------- Frasa umum per field ----------

    private static String phrase(CronFieldKind kind, FieldExpr expr) {
        return switch (expr) {
            case FieldExpr.Any any -> Messages.format("devutils.cron.desc.every", unit(kind));
            case FieldExpr.Value value -> label(kind) + " " + format(kind, value.value());
            case FieldExpr.Range range ->
                Messages.format(
                        "devutils.cron.desc.range", label(kind), format(kind, range.from()), format(kind, range.to()));
            case FieldExpr.Step step -> step(kind, step);
            case FieldExpr.ListOf list -> list(kind, list);
            case FieldExpr.LastDayOfMonth last ->
                last.daysBefore() == 0
                        ? Messages.get("devutils.cron.desc.lastDay")
                        : Messages.format("devutils.cron.desc.lastDayOffset", String.valueOf(last.daysBefore()));
            case FieldExpr.LastWeekdayOfMonth last -> Messages.get("devutils.cron.desc.lastWeekday");
            case FieldExpr.NearestWeekday nearest ->
                Messages.format("devutils.cron.desc.nearestWeekday", String.valueOf(nearest.dayOfMonth()));
            case FieldExpr.LastDayOfWeek last ->
                Messages.format("devutils.cron.desc.lastDayOfWeek", dayName(last.isoDayOfWeek()));
            case FieldExpr.NthDayOfWeek nth ->
                Messages.format(
                        "devutils.cron.desc.nthDayOfWeek", dayName(nth.isoDayOfWeek()), String.valueOf(nth.nth()));
            case FieldExpr.Raw raw -> Messages.format("devutils.cron.desc.raw", raw.text());
        };
    }

    private static String step(CronFieldKind kind, FieldExpr.Step step) {
        String every = String.valueOf(step.step());
        return switch (step.start()) {
            case FieldExpr.Any any -> Messages.format("devutils.cron.desc.step", every, units(kind));
            case FieldExpr.Value value ->
                Messages.format(
                        "devutils.cron.desc.stepFrom", every, units(kind), label(kind), format(kind, value.value()));
            case FieldExpr.Range range ->
                Messages.format(
                        "devutils.cron.desc.stepRange",
                        every,
                        units(kind),
                        label(kind),
                        format(kind, range.from()),
                        format(kind, range.to()));
            default -> Messages.format("devutils.cron.desc.step", every, units(kind));
        };
    }

    private static String list(CronFieldKind kind, FieldExpr.ListOf list) {
        Optional<List<Integer>> values = values(list);
        if (values.isPresent()) {
            return label(kind) + " "
                    + joinAnd(values.get().stream()
                            .map(value -> format(kind, value))
                            .toList());
        }
        return joinAnd(list.items().stream().map(item -> phrase(kind, item)).toList());
    }

    private static String unit(CronFieldKind kind) {
        return Messages.get("devutils.cron.unit." + kind.name());
    }

    /** Bentuk jamak untuk langkah, contoh "every 15 minutes". */
    private static String units(CronFieldKind kind) {
        return Messages.get("devutils.cron.units." + kind.name());
    }

    private static String label(CronFieldKind kind) {
        return Messages.get("devutils.cron.label." + kind.name());
    }

    private static String format(CronFieldKind kind, int value) {
        return switch (kind) {
            case HOUR -> "%02d".formatted(value);
            case MONTH -> monthName(value);
            case DAY_OF_WEEK -> dayName(value);
            default -> String.valueOf(value);
        };
    }

    private static String monthName(int value) {
        return value >= 1 && value <= Month.values().length
                ? Month.of(value).getDisplayName(TextStyle.FULL, Messages.LOCALE)
                : String.valueOf(value);
    }

    private static String dayName(int isoDay) {
        return isoDay >= 1 && isoDay <= DayOfWeek.values().length
                ? DayOfWeek.of(isoDay).getDisplayName(TextStyle.FULL, Messages.LOCALE)
                : String.valueOf(isoDay);
    }

    private static String clock(int hour, int minute) {
        return "%02d:%02d".formatted(hour, minute);
    }

    /** "1 jam 30 menit"; satuan bernilai nol dilewati. */
    static String duration(Duration duration) {
        List<String> parts = new ArrayList<>();
        addPart(parts, "devutils.cron.duration.days", duration.toDays());
        addPart(parts, "devutils.cron.duration.hours", duration.toHoursPart());
        addPart(parts, "devutils.cron.duration.minutes", duration.toMinutesPart());
        addPart(parts, "devutils.cron.duration.seconds", duration.toSecondsPart());
        return String.join(" ", parts);
    }

    private static void addPart(List<String> parts, String key, long amount) {
        if (amount > 0) {
            // Angka (bukan String) supaya choice format bisa memilih bentuk tunggal/jamak
            parts.add(Messages.format(key, amount));
        }
    }

    /** "a", "a dan b", "a, b, dan c". */
    private static String joinAnd(List<String> items) {
        if (items.size() <= 1) {
            return items.isEmpty() ? "" : items.getFirst();
        }
        String head = String.join(SEPARATOR, items.subList(0, items.size() - 1));
        String key = items.size() == 2 ? "devutils.cron.list.two" : "devutils.cron.list.many";
        return Messages.format(key, head, items.getLast());
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
