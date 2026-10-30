package io.github.lukmnh.toolbox.productivity.shared.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parser angka input user: format Indonesia ({@code 1.250.000,50}) maupun polos ({@code 1250000.50}). Prefix
 * {@code Rp}, spasi, dan {@code %} diabaikan.
 *
 * <p>Titik tunggal diikuti tepat 3 digit dianggap pemisah ribuan ({@code 1.250} = 1250, {@code 1.5} = 1,5), karena itu kebiasaan
 * penulisan nominal di Indonesia.
 */
public final class IndonesianNumbers {

    private static final Pattern NOISE = Pattern.compile("(?i)rp|\\s|%|\\u00A0");
    private static final Pattern PLAIN_NUMBER = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    private static final Pattern THOUSANDS_ONLY = Pattern.compile("[+-]?\\d{1,3}(\\.\\d{3})+");

    private IndonesianNumbers() {}

    /** @return kosong bila teks kosong atau bukan angka */
    public static Optional<BigDecimal> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String cleaned = NOISE.matcher(text).replaceAll("");
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        String normalized = normalize(cleaned);
        return PLAIN_NUMBER.matcher(normalized).matches() ? Optional.of(new BigDecimal(normalized)) : Optional.empty();
    }

    private static String normalize(String cleaned) {
        if (cleaned.indexOf(',') >= 0) {
            // Koma ada → format Indonesia: titik = ribuan, koma = desimal
            return cleaned.replace(".", "").replace(',', '.');
        }
        return THOUSANDS_ONLY.matcher(cleaned).matches() ? cleaned.replace(".", "") : cleaned;
    }
}
