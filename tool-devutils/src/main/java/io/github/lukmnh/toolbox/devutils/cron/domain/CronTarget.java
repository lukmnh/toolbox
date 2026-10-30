package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Platform tempat ekspresi cron dipakai. Tiap platform punya jumlah field, karakter khusus, dan penomoran hari yang
 * berbeda, jadi validasi dan jadwal selalu mengikuti target yang dipilih.
 */
public enum CronTarget {
    /** cron/cronie Linux, Kubernetes CronJob, GitHub Actions. */
    UNIX("Unix crontab", EnumSet.noneOf(CronSpecial.class), DayMatching.EITHER),
    /** Spring 5.3+ {@code CronExpression} ({@code @Scheduled}). */
    SPRING("Java Spring", EnumSet.allOf(CronSpecial.class), DayMatching.BOTH),
    /** Quartz; juga default {@code quarkus.scheduler.cron-type}. */
    QUARKUS("Java Quarkus / Quartz", EnumSet.allOf(CronSpecial.class), DayMatching.BOTH),
    /** {@code github.com/robfig/cron/v3}, parser standar. */
    GO("Go (robfig/cron)", EnumSet.of(CronSpecial.QUESTION_MARK), DayMatching.EITHER),
    /** Laravel scheduler ({@code dragonmantank/cron-expression}). */
    LARAVEL(
            "PHP Laravel",
            EnumSet.of(
                    CronSpecial.QUESTION_MARK,
                    CronSpecial.LAST_DAY,
                    CronSpecial.NEAREST_WEEKDAY,
                    CronSpecial.LAST_DAY_OF_WEEK,
                    CronSpecial.NTH_DAY_OF_WEEK),
            DayMatching.EITHER);

    /** Arti bila tanggal dan hari-minggu sama-sama dibatasi. */
    public enum DayMatching {
        /** Jalan bila salah satu cocok (perilaku cron klasik). */
        EITHER,
        /** Jalan hanya bila keduanya cocok. */
        BOTH
    }

    private final String displayName;
    private final Set<CronSpecial> specials;
    private final DayMatching dayMatching;

    CronTarget(String displayName, Set<CronSpecial> specials, DayMatching dayMatching) {
        this.displayName = displayName;
        this.specials = specials;
        this.dayMatching = dayMatching;
    }

    /** Nama platform/library, tidak diterjemahkan. */
    public String displayName() {
        return displayName;
    }

    public DayMatching dayMatching() {
        return dayMatching;
    }

    public boolean supports(CronSpecial special) {
        return specials.contains(special);
    }

    public boolean supports(CronMacro macro) {
        return switch (this) {
            case QUARKUS -> false;
            case UNIX -> true;
            case SPRING, GO, LARAVEL -> macro != CronMacro.REBOOT;
        };
    }

    /** Quartz memakai {@code ?} sebagai "tanpa nilai khusus"; platform lain menganggapnya sama dengan {@code *}. */
    public boolean questionMarkIsWildcard() {
        return this != QUARKUS;
    }

    /** Jumlah field yang diterima. */
    public List<Integer> fieldCounts(boolean goWithSeconds) {
        return switch (this) {
            case UNIX, LARAVEL -> List.of(CronFieldKind.FIVE.size());
            case SPRING -> List.of(CronFieldKind.SIX.size());
            case QUARKUS -> List.of(CronFieldKind.SIX.size(), CronFieldKind.SEVEN.size());
            case GO -> List.of(goWithSeconds ? CronFieldKind.SIX.size() : CronFieldKind.FIVE.size());
        };
    }

    /** Field pertama detik? (Spring, Quartz, Go dengan {@code cron.WithSeconds()}). */
    public boolean hasSecondsField(boolean goWithSeconds) {
        return this == SPRING || this == QUARKUS || (this == GO && goWithSeconds);
    }

    /** @param fieldCount salah satu dari {@link #fieldCounts(boolean)} */
    List<CronFieldKind> layout(int fieldCount) {
        return switch (fieldCount) {
            case 5 -> CronFieldKind.FIVE;
            case 6 -> CronFieldKind.SIX;
            case 7 -> CronFieldKind.SEVEN;
            default -> throw new IllegalArgumentException("Unsupported field count: " + fieldCount);
        };
    }
}
