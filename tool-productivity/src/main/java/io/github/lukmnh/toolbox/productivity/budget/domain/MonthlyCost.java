package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rata-rata biaya per bulan = harga ÷ durasi dalam bulan.
 *
 * <p>Bulan dihitung dari tahun Julian (365,25 hari ÷ 12 = 30,4375 hari) supaya tahun kabisat ikut terwakili.
 * Hasil disimpan dengan presisi {@link #SCALE} desimal; pembulatan ke Rupiah hanya dilakukan saat tampil.
 */
public final class MonthlyCost {

    public static final int SCALE = 6;
    public static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    public static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365.25");
    public static final BigDecimal DAYS_PER_MONTH = DAYS_PER_YEAR.divide(MONTHS_PER_YEAR); // 30.4375, eksak
    public static final BigDecimal DAYS_PER_WEEK = BigDecimal.valueOf(7);
    /** 30,4375 ÷ 7; dipakai sebagai faktor tampilan, perhitungan memakai bentuk pecahan supaya eksak. */
    public static final BigDecimal WEEKS_PER_MONTH = DAYS_PER_MONTH.divide(DAYS_PER_WEEK, SCALE, RoundingMode.HALF_UP);

    private MonthlyCost() {}

    /** Durasi nol berarti belum diisi: hasil 0, dan pemanggil menandai baris dengan peringatan. */
    public static BigDecimal of(BigDecimal price, BigDecimal duration, DurationUnit unit) {
        if (duration.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return switch (unit) {
            case DAYS -> price.multiply(DAYS_PER_MONTH).divide(duration, SCALE, RoundingMode.HALF_UP);
            case WEEKS ->
                price.multiply(DAYS_PER_MONTH).divide(duration.multiply(DAYS_PER_WEEK), SCALE, RoundingMode.HALF_UP);
            case MONTHS -> price.divide(duration, SCALE, RoundingMode.HALF_UP);
            case YEARS -> price.divide(duration.multiply(MONTHS_PER_YEAR), SCALE, RoundingMode.HALF_UP);
        };
    }

    /** Pengeluaran per tahun dari pengeluaran per bulan. */
    public static BigDecimal perYear(BigDecimal monthly) {
        return monthly.multiply(MONTHS_PER_YEAR);
    }
}
