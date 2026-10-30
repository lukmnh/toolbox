package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Satu baris item yang sedang diketik. {@code amount} diisi view model setelah total dihitung. */
final class InvoiceLineInput {

    private static final String DEFAULT_QUANTITY = "1";

    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty quantity = new SimpleStringProperty(DEFAULT_QUANTITY);
    private final StringProperty unitPrice = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper amount = new ReadOnlyStringWrapper("");

    StringProperty descriptionProperty() {
        return description;
    }

    StringProperty quantityProperty() {
        return quantity;
    }

    StringProperty unitPriceProperty() {
        return unitPrice;
    }

    ReadOnlyStringProperty amountProperty() {
        return amount.getReadOnlyProperty();
    }

    void setAmount(String text) {
        amount.set(text);
    }

    /** Baris yang belum diisi apa pun (qty bawaan tidak dihitung) dilewati, bukan error. */
    boolean isBlank() {
        return description.get().isBlank()
                && unitPrice.get().isBlank()
                && (quantity.get().isBlank()
                        || DEFAULT_QUANTITY.equals(quantity.get().strip()));
    }

    /** Dipakai sebagai extractor list: perubahan teks di baris mana pun memicu hitung ulang. */
    Observable[] inputs() {
        return new Observable[] {description, quantity, unitPrice};
    }
}
