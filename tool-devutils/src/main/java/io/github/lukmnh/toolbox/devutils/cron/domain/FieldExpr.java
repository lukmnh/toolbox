package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.List;
import java.util.Objects;

/**
 * Isi satu field cron hasil parse, bebas dari library. Nilai hari-minggu selalu ISO-8601 (1 = Senin … 7 = Minggu),
 * apa pun penomoran target aslinya (Unix 0 = Minggu, Quartz 1 = Minggu).
 */
public sealed interface FieldExpr {

    /** {@code *}, atau {@code ?} di Quartz. */
    record Any() implements FieldExpr {}

    record Value(int value) implements FieldExpr {}

    record Range(int from, int to) implements FieldExpr {}

    /** @param start {@link Any}, {@link Value}, atau {@link Range} */
    record Step(FieldExpr start, int step) implements FieldExpr {

        public Step {
            Objects.requireNonNull(start, "start");
        }
    }

    record ListOf(List<FieldExpr> items) implements FieldExpr {

        public ListOf {
            items = List.copyOf(items);
        }
    }

    /** {@code L} ({@code daysBefore} 0) atau {@code L-3}. */
    record LastDayOfMonth(int daysBefore) implements FieldExpr {}

    /** {@code LW}. */
    record LastWeekdayOfMonth() implements FieldExpr {}

    /** {@code 15W}. */
    record NearestWeekday(int dayOfMonth) implements FieldExpr {}

    /** {@code 5L}: hari tertentu terakhir dalam bulan. */
    record LastDayOfWeek(int isoDayOfWeek) implements FieldExpr {}

    /** {@code 5#2}: hari tertentu ke-n dalam bulan. */
    record NthDayOfWeek(int isoDayOfWeek, int nth) implements FieldExpr {}

    /** Konstruk yang tidak dikenali adapter; ditampilkan apa adanya, tidak pernah membuat crash. */
    record Raw(String text) implements FieldExpr {

        public Raw {
            Objects.requireNonNull(text, "text");
        }
    }
}
