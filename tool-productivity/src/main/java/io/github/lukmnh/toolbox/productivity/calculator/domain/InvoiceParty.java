package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.util.Objects;

/**
 * Penjual atau pelanggan di invoice.
 *
 * @param details alamat, telepon, NPWP, dan sebagainya; boleh kosong dan multi-baris
 */
public record InvoiceParty(String name, String details) {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_DETAILS_LENGTH = 500;

    public InvoiceParty {
        name = Objects.requireNonNull(name, "name").strip();
        details = Objects.requireNonNull(details, "details").strip();
        Invoice.requireMaxLength(name, MAX_NAME_LENGTH);
        Invoice.requireMaxLength(details, MAX_DETAILS_LENGTH);
    }
}
