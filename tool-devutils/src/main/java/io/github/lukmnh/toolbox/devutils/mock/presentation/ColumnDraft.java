package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.devutils.mock.domain.MockColumn;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Satu baris editor kolom: nama, tipe, dan opsi yang sedang diketik user. */
final class ColumnDraft {

    private final StringProperty name;
    private final ObjectProperty<MockType> type;
    private final StringProperty options;

    ColumnDraft(String name, MockType type) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleObjectProperty<>(type);
        this.options = new SimpleStringProperty(type.defaultOptions());
        // Ganti tipe → opsi kembali ke default tipe baru, supaya "1..1000" tidak tertinggal di kolom ENUM
        this.type.addListener((observable, previous, current) -> options.set(current.defaultOptions()));
    }

    MockColumn toColumn() {
        return new MockColumn(name.get().strip(), type.get(), options.get());
    }

    /** Panggil {@code action} setiap nama, tipe, atau opsi berubah. */
    void onChange(Runnable action) {
        name.addListener((observable, previous, current) -> action.run());
        type.addListener((observable, previous, current) -> action.run());
        options.addListener((observable, previous, current) -> action.run());
    }

    StringProperty nameProperty() {
        return name;
    }

    ObjectProperty<MockType> typeProperty() {
        return type;
    }

    StringProperty optionsProperty() {
        return options;
    }
}
