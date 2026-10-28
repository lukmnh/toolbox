package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Isi satu sisi diff (Asli atau Baru). File kecil dimuat ke editor supaya bisa diedit; file besar disimpan apa adanya
 * karena {@code TextArea} berisi puluhan MB membuat UI tersendat.
 */
final class DiffInput {

    /** Batas teks yang masih nyaman diedit di TextArea. */
    static final int EDITOR_MAX_CHARS = 1_000_000;

    private final StringProperty text = new SimpleStringProperty("");
    private final ObjectProperty<LoadedText> largeFile = new SimpleObjectProperty<>();
    private final StringProperty fileName = new SimpleStringProperty("");

    StringProperty textProperty() {
        return text;
    }

    ReadOnlyObjectProperty<LoadedText> largeFileProperty() {
        return largeFile;
    }

    ReadOnlyStringProperty fileNameProperty() {
        return fileName;
    }

    String effectiveText() {
        return largeFile.get() != null ? largeFile.get().text() : text.get();
    }

    BooleanBinding isEmpty() {
        return Bindings.createBooleanBinding(
                () -> largeFile.get() == null && text.get().isEmpty(), text, largeFile);
    }

    void load(LoadedText loaded) {
        if (loaded.text().length() <= EDITOR_MAX_CHARS) {
            largeFile.set(null);
            text.set(loaded.text());
        } else {
            text.set("");
            largeFile.set(loaded);
        }
        fileName.set(loaded.fileName());
    }

    void clear() {
        largeFile.set(null);
        text.set("");
        fileName.set("");
    }

    void swapWith(DiffInput other) {
        String otherText = other.text.get();
        LoadedText otherFile = other.largeFile.get();
        String otherName = other.fileName.get();
        other.text.set(text.get());
        other.largeFile.set(largeFile.get());
        other.fileName.set(fileName.get());
        text.set(otherText);
        largeFile.set(otherFile);
        fileName.set(otherName);
    }
}
