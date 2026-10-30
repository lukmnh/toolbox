package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.shared.domain.IndonesianNumbers;
import java.math.BigDecimal;
import java.util.Optional;

/** Parse field angka kalkulator (format Indonesia maupun polos). Field kosong = belum diisi, bukan error. */
final class FieldParser {

    private FieldParser() {}

    /** @throws InvalidFieldException bila terisi tapi bukan angka */
    static Optional<BigDecimal> decimal(String input, String fieldLabelKey) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(IndonesianNumbers.parse(input)
                .orElseThrow(() -> new InvalidFieldException(
                        Messages.format("productivity.calculator.error.notNumber", Messages.get(fieldLabelKey)))));
    }

    /** @throws InvalidFieldException bila terisi tapi bukan bilangan bulat */
    static Optional<Integer> wholeNumber(String input, String fieldLabelKey) {
        Optional<BigDecimal> value = decimal(input, fieldLabelKey);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(value.get().intValueExact());
        } catch (ArithmeticException e) {
            // Pecahan atau di luar int: tampilkan sebagai input salah, bukan error sistem
            throw new InvalidFieldException(
                    Messages.format("productivity.calculator.error.notWhole", Messages.get(fieldLabelKey)));
        }
    }
}
