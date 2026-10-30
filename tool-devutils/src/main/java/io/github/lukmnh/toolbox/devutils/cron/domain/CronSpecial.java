package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Karakter khusus di luar sintaks cron dasar; dukungannya berbeda per target. */
public enum CronSpecial {
    /** {@code ?}: tanpa nilai khusus (Quartz) atau alias {@code *}. */
    QUESTION_MARK,
    /** {@code L} di tanggal: hari terakhir bulan. */
    LAST_DAY,
    /** {@code L-3}: 3 hari sebelum hari terakhir bulan. */
    LAST_DAY_OFFSET,
    /** {@code LW}: hari kerja terakhir bulan. */
    LAST_WEEKDAY,
    /** {@code 15W}: hari kerja terdekat dari tanggal 15. */
    NEAREST_WEEKDAY,
    /** {@code 5L}: hari Jumat terakhir dalam bulan. */
    LAST_DAY_OF_WEEK,
    /** {@code 5#2}: hari Jumat ke-2 dalam bulan. */
    NTH_DAY_OF_WEEK;

    /** Karakter khusus yang dipakai satu field; nama hari seperti {@code WED} tidak terhitung sebagai {@code W}. */
    static Set<CronSpecial> detect(CronFieldKind field, String value) {
        Set<CronSpecial> found = EnumSet.noneOf(CronSpecial.class);
        for (String part : value.toUpperCase(Locale.ROOT).split(",", -1)) {
            if (part.equals("?")) {
                found.add(QUESTION_MARK);
            } else if (field == CronFieldKind.DAY_OF_MONTH) {
                detectDayOfMonth(part, found);
            } else if (field == CronFieldKind.DAY_OF_WEEK) {
                detectDayOfWeek(part, found);
            }
        }
        return found;
    }

    private static void detectDayOfMonth(String part, Set<CronSpecial> found) {
        if (part.equals("L")) {
            found.add(LAST_DAY);
        } else if (part.startsWith("L-")) {
            found.add(LAST_DAY_OFFSET);
        } else if (part.equals("LW")) {
            found.add(LAST_WEEKDAY);
        } else if (part.length() > 1 && part.endsWith("W")) {
            found.add(NEAREST_WEEKDAY);
        }
    }

    private static void detectDayOfWeek(String part, Set<CronSpecial> found) {
        if (part.contains("#")) {
            found.add(NTH_DAY_OF_WEEK);
        } else if (part.endsWith("L")) {
            found.add(LAST_DAY_OF_WEEK);
        }
    }
}
