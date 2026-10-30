package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aturan bersama hitungan uang kalkulator: pembulatan HALF_UP dan batas skala. */
public final class Money {

    /** 0 = rupiah bulat, 2 = sampai sen. */
    public static final int MAX_SCALE = 2;

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {}

    public static BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    static int requireScale(int scale) {
        if (scale < 0 || scale > MAX_SCALE) {
            throw new IllegalArgumentException("Skala uang harus 0–" + MAX_SCALE);
        }
        return scale;
    }

    /** @return {@code true} bila {@code 0 ≤ value ≤ 100} */
    static boolean isPercent(BigDecimal value) {
        return value.signum() >= 0 && value.compareTo(HUNDRED) <= 0;
    }
}
