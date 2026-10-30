package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.TablePreview;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Tabel pratinjau; kolom dibuat ulang dari header setiap kali data berubah. */
final class PreviewTable extends TableView<List<String>> {

    private static final double COLUMN_WIDTH = 140;

    PreviewTable(String placeholder) {
        setPlaceholder(ToolLayout.hint(placeholder));
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    void show(TablePreview table) {
        if (table == null) {
            getItems().clear();
            getColumns().clear();
            return;
        }
        List<TableColumn<List<String>, ?>> columns =
                new ArrayList<>(table.columns().size());
        for (int index = 0; index < table.columns().size(); index++) {
            columns.add(column(table.columns().get(index), index));
        }
        getColumns().setAll(columns);
        getItems().setAll(table.rows());
    }

    private static TableColumn<List<String>, String> column(String title, int index) {
        TableColumn<List<String>, String> column = new TableColumn<>(title);
        column.setPrefWidth(COLUMN_WIDTH);
        // Urutan baris mengikuti file; sort di pratinjau 200 baris bisa menyesatkan
        column.setSortable(false);
        column.setCellValueFactory(cell -> {
            List<String> row = cell.getValue();
            return new ReadOnlyStringWrapper(index < row.size() ? row.get(index) : "");
        });
        return column;
    }
}
