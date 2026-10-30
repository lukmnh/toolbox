package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Validasi field teks & angka bersama untuk record budget. */
final class Texts {

    /** Batas panjang teks bebas; menjaga file dan sel Excel tetap wajar (sel Excel maks 32.767 karakter). */
    static final int MAX_TEXT_LENGTH = 1_000;

    static final int MAX_NAME_LENGTH = 100;

    private Texts() {}

    /** Teks opsional: null dianggap kosong. */
    static String optional(String text, String field) {
        String value = text == null ? "" : text;
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(field + " terlalu panjang");
        }
        return value;
    }

    static String requiredName(String name, String field) {
        Objects.requireNonNull(name, field);
        String stripped = name.strip();
        if (stripped.isEmpty() || stripped.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(field + " kosong atau terlalu panjang");
        }
        return stripped;
    }

    static BigDecimal nonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " tidak boleh negatif");
        }
        return value;
    }
}
