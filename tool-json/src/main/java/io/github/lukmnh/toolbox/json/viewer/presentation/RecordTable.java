package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.json.viewer.application.ViewerSession;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordColumn;
import io.github.lukmnh.toolbox.json.viewer.domain.SortSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * TableView virtual: item = nomor record, sel dirender dari record ter-cache. Klik header tidak mengurutkan
 * item secara lokal; urutan dihitung ulang di background lewat ViewModel.
 */
final class RecordTable extends TableView<Integer> {

    private static final double NUMBER_COLUMN_WIDTH = 80;
    private static final double DATA_COLUMN_WIDTH = 160;
    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    private final ViewerViewModel viewModel;
    private final TableColumn<Integer, String> numberColumn;
    private boolean rebuilding;

    RecordTable(ViewerViewModel viewModel) {
        this.viewModel = viewModel;
        this.numberColumn = column(new RecordColumn.RecordNumber(), NUMBER_COLUMN_WIDTH);
        setPlaceholder(new Label(Messages.get("json.viewer.table.empty")));
        getSelectionModel().setCellSelectionEnabled(true);
        setSortPolicy(table -> {
            if (!rebuilding) {
                viewModel.requestSort(currentSort());
            }
            return true;
        });
        getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, previous, record) -> viewModel.select(record == null ? -1 : record));
        setContextMenu(contextMenu());
        setOnKeyPressed(event -> {
            if (COPY.match(event)) {
                copyCell();
                event.consume();
            }
        });
        viewModel.sessionProperty().addListener((observable, previous, session) -> rebuildColumns(session));
        viewModel.rowsProperty().addListener((observable, previous, rows) -> showRows(rows));
    }

    /** Pilih dan tampilkan baris pada posisi view tertentu. */
    void reveal(int position) {
        scrollTo(position);
        getSelectionModel().clearAndSelect(position, numberColumn);
        requestFocus();
    }

    private void rebuildColumns(ViewerSession session) {
        rebuilding = true;
        try {
            getSortOrder().clear();
            List<TableColumn<Integer, ?>> columns = new ArrayList<>();
            columns.add(numberColumn);
            if (session != null) {
                session.columns().data().forEach(data -> columns.add(column(data, DATA_COLUMN_WIDTH)));
            }
            getColumns().setAll(columns);
        } finally {
            rebuilding = false;
        }
    }

    private void showRows(int[] rows) {
        rebuilding = true;
        try {
            setItems(
                    rows == null
                            ? FXCollections.emptyObservableList()
                            : FXCollections.observableList(new IntList(rows)));
        } finally {
            rebuilding = false;
        }
    }

    private TableColumn<Integer, String> column(RecordColumn recordColumn, double width) {
        TableColumn<Integer, String> column = new TableColumn<>(viewModel.columnTitle(recordColumn));
        column.setUserData(recordColumn);
        column.setPrefWidth(width);
        column.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(viewModel.cellText(cell.getValue(), recordColumn)));
        return column;
    }

    private SortSpec currentSort() {
        if (getSortOrder().isEmpty()) {
            return null;
        }
        TableColumn<Integer, ?> first = getSortOrder().getFirst();
        return new SortSpec((RecordColumn) first.getUserData(), first.getSortType() == TableColumn.SortType.ASCENDING);
    }

    private ContextMenu contextMenu() {
        MenuItem copyCell = new MenuItem(Messages.get("json.viewer.copyCell"));
        copyCell.setOnAction(event -> copyCell());
        MenuItem copyRow = new MenuItem(Messages.get("json.viewer.copyRow"));
        copyRow.setOnAction(event -> copyRow());
        return new ContextMenu(copyCell, copyRow);
    }

    private void copyCell() {
        TablePosition<Integer, ?> focused = getFocusModel().getFocusedCell();
        if (focused == null || focused.getTableColumn() == null || focused.getRow() < 0) {
            return;
        }
        Object value = focused.getTableColumn().getCellData(focused.getRow());
        putOnClipboard(value == null ? "" : value.toString());
    }

    private void copyRow() {
        Integer record = getSelectionModel().getSelectedItem();
        if (record != null) {
            putOnClipboard(viewModel.rowJson(record));
        }
    }

    private void putOnClipboard(String text) {
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, text));
        viewModel.notifyCopied();
    }
}
