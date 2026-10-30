package io.github.lukmnh.toolbox.productivity.budget.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

/**
 * Seleksi & fokus sel tabel berdasarkan id baris. Baris berisi record immutable yang diganti instance baru setiap
 * kali berubah, sehingga seleksi bawaan TableView (berbasis instance/index) perlu dipulihkan manual.
 *
 * @param focused sel yang difokus (0 atau 1 elemen)
 */
record TableSelection(List<Cell> selected, List<Cell> focused) {

    /** @param column index kolom di antara kolom yang terlihat */
    record Cell(String rowId, int column) {}

    static <S> TableSelection capture(TableView<S> table, Function<S, String> id) {
        List<Cell> selected = new ArrayList<>();
        for (TablePosition<?, ?> position : table.getSelectionModel().getSelectedCells()) {
            selected.addAll(cell(table, id, position));
        }
        TablePosition<?, ?> focus = table.getFocusModel().getFocusedCell();
        return new TableSelection(List.copyOf(selected), focus == null ? List.of() : cell(table, id, focus));
    }

    <S> void restore(TableView<S> table, Function<S, String> id) {
        if (table.getEditingCell() != null) {
            return;
        }
        table.getSelectionModel().clearSelection();
        for (Cell cell : selected) {
            int row = indexOf(table, id, cell.rowId());
            if (row >= 0) {
                table.getSelectionModel().select(row, table.getVisibleLeafColumn(cell.column()));
            }
        }
        for (Cell cell : focused) {
            int row = indexOf(table, id, cell.rowId());
            if (row >= 0) {
                table.getFocusModel().focus(row, table.getVisibleLeafColumn(cell.column()));
            }
        }
    }

    private static <S> List<Cell> cell(TableView<S> table, Function<S, String> id, TablePosition<?, ?> position) {
        int row = position.getRow();
        if (row < 0 || row >= table.getItems().size()) {
            return List.of();
        }
        return List.of(new Cell(id.apply(table.getItems().get(row)), position.getColumn()));
    }

    private static <S> int indexOf(TableView<S> table, Function<S, String> id, String rowId) {
        List<S> items = table.getItems();
        for (int index = 0; index < items.size(); index++) {
            if (id.apply(items.get(index)).equals(rowId)) {
                return index;
            }
        }
        return -1;
    }
}
