package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Satu field top-level payload.
 *
 * @param value isi string untuk {@link ValueType#STRING}, literal angka untuk {@link ValueType#NUMBER}, JSON kompak
 *     untuk array/object, dan {@code true}/{@code false}/{@code null} untuk sisanya
 */
public record Claim(String name, String value, ValueType type) {

    /** Claim terdaftar RFC 7519 yang diberi penjelasan. */
    public static final Set<String> REGISTERED = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");

    /** Claim bertipe NumericDate (detik sejak epoch). */
    public static final Set<String> TIME_CLAIMS = Set.of("exp", "nbf", "iat");

    /** Tipe nilai JSON. */
    public enum ValueType {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        ARRAY,
        OBJECT
    }

    public Claim {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(type, "type");
    }

    public boolean registered() {
        return REGISTERED.contains(name);
    }

    /** Waktu untuk {@code exp}/{@code nbf}/{@code iat} bernilai angka; pecahan detik dibulatkan ke bawah. */
    public Optional<Instant> instant() {
        if (!TIME_CLAIMS.contains(name) || type != ValueType.NUMBER) {
            return Optional.empty();
        }
        try {
            long seconds = new BigDecimal(value).setScale(0, RoundingMode.FLOOR).longValueExact();
            return Optional.of(Instant.ofEpochSecond(seconds));
        } catch (NumberFormatException | ArithmeticException | DateTimeException e) {
            // Angka di luar rentang Instant tetap ditampilkan apa adanya, hanya tanpa konversi waktu
            return Optional.empty();
        }
    }
}
