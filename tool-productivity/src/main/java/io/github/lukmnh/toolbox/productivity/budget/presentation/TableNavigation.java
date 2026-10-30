package io.github.lukmnh.toolbox.productivity.budget.presentation;

import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Navigasi ala spreadsheet setelah commit sel: Tab → sel editable berikutnya, Enter → baris di bawahnya. Baris
 * dicari lewat id karena setelah commit item tabel diganti instance baru (record immutable) dan bisa berpindah posisi.
 */
final class TableNavigation<S> {

    private final TableView<S> table;
    private final Function<S, String> id;

    TableNavigation(TableView<S> table, Function<S, String> id) {
        this.table = table;
        this.id = id;
    }

    /** Dijalankan setelah pulse berikutnya, saat tabel sudah memuat baris hasil commit. */
    void editNext(String rowId, TableColumn<S, ?> from, int direction) {
        Platform.runLater(() -> {
            int row = rowIndex(rowId);
            List<TableColumn<S, ?>> columns = table.getVisibleLeafColumns();
            int index = columns.indexOf(from) + direction;
            while (row >= 0
                    && index >= 0
                    && index < columns.size()
                    && !columns.get(index).isEditable()) {
                index += direction;
            }
            if (row < 0 || index < 0 || index >= columns.size()) {
                return;
            }
            focus(row, columns.get(index));
            table.edit(row, columns.get(index));
        });
    }

    void selectBelow(String rowId, TableColumn<S, ?> column) {
        Platform.runLater(() -> {
            int row = rowIndex(rowId);
            if (row >= 0) {
                focus(Math.min(row + 1, table.getItems().size() - 1), column);
            }
        });
    }

    /** Fokus + mulai edit sel di baris {@code rowId} (misalnya baris yang baru ditambahkan). */
    void edit(String rowId, TableColumn<S, ?> column) {
        Platform.runLater(() -> {
            int row = rowIndex(rowId);
            if (row >= 0) {
                table.scrollTo(row);
                focus(row, column);
                table.edit(row, column);
            }
        });
    }

    private void focus(int row, TableColumn<S, ?> column) {
        table.getSelectionModel().clearAndSelect(row, column);
        table.getFocusModel().focus(row, column);
        table.requestFocus();
    }

    private int rowIndex(String rowId) {
        List<S> items = table.getItems();
        for (int index = 0; index < items.size(); index++) {
            if (id.apply(items.get(index)).equals(rowId)) {
                return index;
            }
        }
        return -1;
    }
}
