package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;

/**
 * Sumber pemasukan bulanan.
 *
 * @param amount nominal per bulan (boleh 2 desimal)
 */
public record Income(String id, String name, BigDecimal amount, String note) {

    public Income {
        id = Ids.require(id, "id pemasukan");
        name = Texts.optional(name, "nama pemasukan");
        amount = Texts.nonNegative(amount, "nominal pemasukan");
        note = Texts.optional(note, "catatan pemasukan");
    }

    public static Income blank() {
        return new Income(Ids.next(), "", BigDecimal.ZERO, "");
    }

    public Income withName(String value) {
        return new Income(id, value, amount, note);
    }

    public Income withAmount(BigDecimal value) {
        return new Income(id, name, value, note);
    }

    public Income withNote(String value) {
        return new Income(id, name, amount, value);
    }
}
