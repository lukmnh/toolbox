package io.github.lukmnh.toolbox.productivity.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Format Rupiah gaya Indonesia: {@code Rp1.250.000} atau {@code Rp1.250.000,50}. Dipakai Kalkulator & Budget. */
public final class RupiahFormat {

    public static final int NO_DECIMALS = 0;
    public static final int TWO_DECIMALS = 2;
    private static final String PREFIX = "Rp";
    private static final DecimalFormatSymbols SYMBOLS = symbols();

    private RupiahFormat() {}

    /** Bulat HALF_UP ke {@code fractionDigits} desimal. Negatif ditulis {@code -Rp1.000}. */
    public static String format(BigDecimal amount, int fractionDigits) {
        BigDecimal rounded = amount.setScale(fractionDigits, RoundingMode.HALF_UP);
        String digits = number(fractionDigits).format(rounded.abs());
        return (rounded.signum() < 0 ? "-" : "") + PREFIX + digits;
    }

    public static String format(BigDecimal amount) {
        return format(amount, NO_DECIMALS);
    }

    /** Angka tanpa prefix {@code Rp}, misalnya untuk persen: {@code 12,5}. */
    public static String plain(BigDecimal value, int maxFractionDigits) {
        DecimalFormat format = new DecimalFormat("#,##0", SYMBOLS);
        format.setMaximumFractionDigits(maxFractionDigits);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    private static DecimalFormat number(int fractionDigits) {
        String pattern = fractionDigits == 0 ? "#,##0" : "#,##0." + "0".repeat(fractionDigits);
        DecimalFormat format = new DecimalFormat(pattern, SYMBOLS);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }

    /** Ditulis eksplisit supaya tidak tergantung data locale JDK (CLDR bisa berubah antar versi). */
    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        symbols.setMinusSign('-');
        return symbols;
    }
}
