package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Invoice/kuitansi lengkap yang siap dicetak. Validasi lebih ketat dari {@link InvoicePricing}: nomor, nama penjual,
 * dan minimal satu baris wajib ada.
 */
public record Invoice(
        InvoiceType type,
        String number,
        LocalDate date,
        InvoiceParty seller,
        InvoiceParty customer,
        InvoicePricing pricing,
        String notes) {

    public static final int MAX_NUMBER_LENGTH = 40;
    public static final int MAX_NOTES_LENGTH = 1_000;

    public Invoice {
        Objects.requireNonNull(type, "type");
        number = Objects.requireNonNull(number, "number").strip();
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(seller, "seller");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(pricing, "pricing");
        notes = Objects.requireNonNull(notes, "notes").strip();
        if (number.isEmpty()) {
            throw new CalculatorInputException(InputProblem.DOCUMENT_NUMBER_REQUIRED);
        }
        requireMaxLength(number, MAX_NUMBER_LENGTH);
        if (seller.name().isEmpty()) {
            throw new CalculatorInputException(InputProblem.SELLER_REQUIRED);
        }
        if (pricing.lines().isEmpty()) {
            throw new CalculatorInputException(InputProblem.NO_LINES);
        }
        requireMaxLength(notes, MAX_NOTES_LENGTH);
    }

    /** Panjang dihitung per code point supaya emoji/aksara non-Latin tidak terhitung dobel. */
    static void requireMaxLength(String text, int maxLength) {
        if (text.codePointCount(0, text.length()) > maxLength) {
            throw new CalculatorInputException(InputProblem.TEXT_TOO_LONG);
        }
    }
}
