package io.github.lukmnh.toolbox.productivity.budget.presentation;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

/**
 * Sel teks yang bisa diedit langsung. Berbeda dari {@code TextFieldTableCell}: perubahan juga tersimpan saat fokus
 * pindah (klik sel lain), Tab/Shift+Tab pindah ke sel berikutnya, Enter turun satu baris, Esc membatalkan.
 *
 * <p>Semua jalur selesai-edit melewati {@link #cancelEdit()}, lalu nilai baru diserahkan ke {@code onCommit} setelah
 * mode edit berakhir, sehingga tabel boleh memuat ulang barisnya tanpa mengganggu sel yang sedang diedit.
 *
 * @param <S> tipe baris
 * @param <T> tipe nilai sel
 */
final class EditingCell<S, T> extends TableCell<S, T> {

    /** Cara menampilkan, mengedit, dan mem-parse nilai sel. */
    record Spec<S, T>(
            Function<T, String> display,
            Function<T, String> editText,
            Function<String, Optional<T>> parse,
            Runnable onInvalid,
            BiConsumer<S, T> onCommit,
            Function<S, String> rowId) {}

    private final Spec<S, T> spec;
    private final TableNavigation<S> navigation;
    private TextField field;
    private boolean discard;

    EditingCell(Spec<S, T> spec, TableNavigation<S> navigation) {
        this.spec = spec;
        this.navigation = navigation;
    }

    @Override
    public void startEdit() {
        if (isEmpty() || getTableRow() == null || getTableRow().getItem() == null) {
            return;
        }
        super.startEdit();
        if (!isEditing()) {
            return;
        }
        discard = false;
        field = new TextField(spec.editText().apply(getItem()));
        field.setOnKeyPressed(this::onKey);
        field.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused && isEditing()) {
                cancelEdit();
            }
        });
        setText(null);
        setGraphic(field);
        field.requestFocus();
        field.selectAll();
    }

    /** Selesai edit: simpan, kecuali dibatalkan dengan Esc. */
    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }
        S row = getTableRow().getItem();
        String text = field.getText();
        boolean commit = !discard;
        super.cancelEdit();
        showValue(getItem());
        if (commit && row != null) {
            commit(row, text);
        }
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        if (isEditing() && field != null) {
            setText(null);
            setGraphic(field);
            return;
        }
        showValue(item);
    }

    private void showValue(T item) {
        setGraphic(null);
        setText(item == null ? null : spec.display().apply(item));
    }

    private void commit(S row, String text) {
        if (text.equals(spec.editText().apply(getItem()))) {
            return;
        }
        Optional<T> value = spec.parse().apply(text);
        if (value.isEmpty()) {
            spec.onInvalid().run();
            return;
        }
        spec.onCommit().accept(row, value.get());
    }

    private void onKey(KeyEvent event) {
        S row = getTableRow().getItem();
        if (row == null) {
            return;
        }
        String rowId = spec.rowId().apply(row);
        switch (event.getCode()) {
            case ESCAPE -> {
                discard = true;
                cancelEdit();
            }
            case ENTER -> {
                cancelEdit();
                navigation.selectBelow(rowId, getTableColumn());
            }
            case TAB -> {
                cancelEdit();
                navigation.editNext(rowId, getTableColumn(), event.isShiftDown() ? -1 : 1);
            }
            default -> {
                return;
            }
        }
        event.consume();
    }
}
