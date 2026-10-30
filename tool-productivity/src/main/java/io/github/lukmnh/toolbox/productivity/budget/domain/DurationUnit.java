package io.github.lukmnh.toolbox.productivity.budget.domain;

/** Satuan durasi pemakaian barang. Konversinya ke bulan ada di {@link MonthlyCost}. */
public enum DurationUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS;

    public static final DurationUnit DEFAULT = MONTHS;
}
