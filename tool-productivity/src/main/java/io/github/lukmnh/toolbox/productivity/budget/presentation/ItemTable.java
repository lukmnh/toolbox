package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.DurationUnit;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import io.github.lukmnh.toolbox.productivity.budget.presentation.ItemRow.CategoryChoice;
import io.github.lukmnh.toolbox.productivity.shared.domain.IndonesianNumbers;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;

/**
 * Tabel item satu daftar: edit langsung, sort per kolom, filter, kolom No berwarna kategori, baris nonaktif merah +
 * coret. Item domain immutable, jadi setiap perubahan memuat ulang baris yang berubah saja ({@link #show}).
 */
final class ItemTable {

    private static final PseudoClass INACTIVE = PseudoClass.getPseudoClass("inactive");
    private static final double NUMBER_WIDTH = 56;
    private static final double TEXT_WIDTH = 160;
    private static final double SHORT_WIDTH = 96;
    private static final double MONEY_WIDTH = 130;

    private final String sheetId;
    private final BudgetEdits edits;
    private final Notifier notifier;
    private final ObservableList<ItemRow> rows = FXCollections.observableArrayList();
    private final FilteredList<ItemRow> filtered = new FilteredList<>(rows);
    private final ObservableList<CategoryChoice> categoryChoices = FXCollections.observableArrayList();
    private final TableView<ItemRow> table = new TableView<>();
    private final TableNavigation<ItemRow> navigation = new TableNavigation<>(table, ItemRow::id);
    private final TableColumn<ItemRow, String> nameColumn;

    ItemTable(String sheetId, BudgetEdits edits, Notifier notifier) {
        this.sheetId = sheetId;
        this.edits = edits;
        this.notifier = notifier;
        SortedList<ItemRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setEditable(true);
        table.getStyleClass().add("budget-table");
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setPlaceholder(ToolLayout.hint(Messages.get("productivity.budget.item.empty")));
        table.setRowFactory(view -> new ItemTableRow());
        nameColumn = textColumn("name", ExpenseItem::name, ExpenseItem::withName);
        table.getColumns()
                .addAll(List.of(
                        numberColumn(),
                        nameColumn,
                        textColumn("brand", ExpenseItem::brand, ExpenseItem::withBrand),
                        textColumn("netto", ExpenseItem::netto, ExpenseItem::withNetto),
                        durationColumn(),
                        unitColumn(),
                        priceColumn(),
                        monthlyColumn(),
                        categoryColumn(),
                        statusColumn(),
                        textColumn("note", ExpenseItem::note, ExpenseItem::withNote)));
    }

    TableView<ItemRow> view() {
        return table;
    }

    /** Muat ulang dari daftar terbaru dengan mempertahankan seleksi & fokus. */
    void show(ExpenseSheet sheet) {
        List<CategoryChoice> choices = CategoryChoice.all(sheet);
        if (!categoryChoices.equals(choices)) {
            categoryChoices.setAll(choices);
        }
        List<ItemRow> fresh = ItemRow.of(sheet);
        TableSelection selection = TableSelection.capture(table, ItemRow::id);
        if (sameIds(fresh)) {
            for (int index = 0; index < fresh.size(); index++) {
                if (!rows.get(index).equals(fresh.get(index))) {
                    rows.set(index, fresh.get(index));
                }
            }
        } else {
            rows.setAll(fresh);
        }
        selection.restore(table, ItemRow::id);
    }

    void setFilter(Predicate<ItemRow> filter) {
        filtered.setPredicate(filter);
    }

    List<String> selectedIds() {
        return table.getSelectionModel().getSelectedItems().stream()
                .map(ItemRow::id)
                .distinct()
                .toList();
    }

    Optional<String> focusedId() {
        TablePosition<?, ?> focused = table.getFocusModel().getFocusedCell();
        if (focused == null
                || focused.getRow() < 0
                || focused.getRow() >= table.getItems().size()) {
            return Optional.empty();
        }
        return Optional.of(table.getItems().get(focused.getRow()).id());
    }

    /** Mulai edit nama di baris baru. */
    void editName(String itemId) {
        navigation.edit(itemId, nameColumn);
    }

    /** Geser urutan hanya masuk akal saat tabel tampil sesuai urutan asli (tanpa sort kolom). */
    BooleanBinding sortedByColumn() {
        return Bindings.isNotEmpty(table.getSortOrder());
    }

    private boolean sameIds(List<ItemRow> fresh) {
        if (fresh.size() != rows.size()) {
            return false;
        }
        for (int index = 0; index < fresh.size(); index++) {
            if (!fresh.get(index).id().equals(rows.get(index).id())) {
                return false;
            }
        }
        return true;
    }

    private TableColumn<ItemRow, Integer> numberColumn() {
        TableColumn<ItemRow, Integer> column = column("number", NUMBER_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().number()));
        column.setCellFactory(view -> new NumberCell());
        column.setEditable(false);
        return column;
    }

    private TableColumn<ItemRow, String> textColumn(
            String key, Function<ExpenseItem, String> getter, BiFunction<ExpenseItem, String, ExpenseItem> setter) {
        TableColumn<ItemRow, String> column = column(key, TEXT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(getter.apply(data.getValue().item())));
        column.setCellFactory(view -> new EditingCell<>(
                new EditingCell.Spec<>(
                        Function.identity(),
                        Function.identity(),
                        text -> Optional.of(text.strip()),
                        () -> notifier.warning(Messages.get("productivity.budget.edit.invalid")),
                        (row, value) -> update(row, item -> setter.apply(item, value)),
                        ItemRow::id),
                navigation));
        return column;
    }

    private TableColumn<ItemRow, BigDecimal> durationColumn() {
        TableColumn<ItemRow, BigDecimal> column = column("duration", SHORT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().item().duration()));
        column.setCellFactory(view -> numberCell(BudgetLabels::duration, ExpenseItem::withDuration));
        return column;
    }

    private TableColumn<ItemRow, BigDecimal> priceColumn() {
        TableColumn<ItemRow, BigDecimal> column = column("price", MONEY_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().item().price()));
        column.setCellFactory(view -> numberCell(BudgetLabels::money, ExpenseItem::withPrice));
        return column;
    }

    private EditingCell<ItemRow, BigDecimal> numberCell(
            Function<BigDecimal, String> display, BiFunction<ExpenseItem, BigDecimal, ExpenseItem> setter) {
        EditingCell<ItemRow, BigDecimal> cell = new EditingCell<>(
                new EditingCell.Spec<>(
                        display,
                        BudgetLabels::plainNumber,
                        ItemTable::parseNonNegative,
                        () -> notifier.warning(Messages.get("productivity.budget.number.invalid")),
                        (row, value) -> update(row, item -> setter.apply(item, value)),
                        ItemRow::id),
                navigation);
        cell.getStyleClass().add("budget-number-cell");
        return cell;
    }

    private TableColumn<ItemRow, BigDecimal> monthlyColumn() {
        TableColumn<ItemRow, BigDecimal> column = column("monthly", MONEY_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().monthly()));
        column.setCellFactory(view -> new MonthlyCell());
        column.setEditable(false);
        return column;
    }

    private TableColumn<ItemRow, DurationUnit> unitColumn() {
        TableColumn<ItemRow, DurationUnit> column = column("unit", SHORT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().item().unit()));
        column.setCellFactory(ComboBoxTableCell.forTableColumn(
                ToolLayout.labels(BudgetLabels::unit), FXCollections.observableArrayList(DurationUnit.values())));
        column.setOnEditCommit(event -> update(event.getRowValue(), item -> item.withUnit(event.getNewValue())));
        return column;
    }

    private TableColumn<ItemRow, CategoryChoice> categoryColumn() {
        TableColumn<ItemRow, CategoryChoice> column = column("category", TEXT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().category()));
        column.setCellFactory(
                ComboBoxTableCell.forTableColumn(ToolLayout.labels(ItemTable::categoryLabel), categoryChoices));
        column.setComparator(Comparator.comparing(CategoryChoice::name, String.CASE_INSENSITIVE_ORDER));
        column.setOnEditCommit(event -> update(
                event.getRowValue(),
                item -> item.withCategoryId(event.getNewValue().id())));
        return column;
    }

    private TableColumn<ItemRow, ItemStatus> statusColumn() {
        TableColumn<ItemRow, ItemStatus> column = column("status", SHORT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().item().status()));
        column.setCellFactory(ComboBoxTableCell.forTableColumn(
                ToolLayout.labels(BudgetLabels::status), FXCollections.observableArrayList(ItemStatus.values())));
        column.setOnEditCommit(event -> update(event.getRowValue(), item -> item.withStatus(event.getNewValue())));
        return column;
    }

    private static <T> TableColumn<ItemRow, T> column(String key, double width) {
        TableColumn<ItemRow, T> column = new TableColumn<>(Messages.get("productivity.budget.column." + key));
        column.setPrefWidth(width);
        return column;
    }

    private void update(ItemRow row, UnaryOperator<ExpenseItem> change) {
        edits.updateItem(sheetId, row.id(), change);
    }

    private static String categoryLabel(CategoryChoice choice) {
        return choice.id().isEmpty() ? Messages.get("productivity.budget.category.none") : choice.name();
    }

    static Optional<BigDecimal> parseNonNegative(String text) {
        if (text.isBlank()) {
            return Optional.of(BigDecimal.ZERO);
        }
        return IndonesianNumbers.parse(text).filter(value -> value.signum() >= 0);
    }

    /** Kolom No berlatar warna kategori, seperti spreadsheet. Warna sudah tervalidasi #RRGGBB di domain. */
    private static final class NumberCell extends TableCell<ItemRow, Integer> {

        NumberCell() {
            getStyleClass().add("budget-no-cell");
        }

        @Override
        protected void updateItem(Integer number, boolean empty) {
            super.updateItem(number, empty);
            ItemRow row = empty ? null : getTableRow().getItem();
            setText(row == null ? null : Integer.toString(number));
            boolean colored = row != null && !row.color().isEmpty();
            setStyle(colored ? "-fx-background-color: " + row.color() + ";" : "");
            getStyleClass().remove("budget-category-cell");
            if (colored) {
                getStyleClass().add("budget-category-cell");
            }
        }
    }

    /** Rata-rata/bulan read-only; durasi kosong ditandai peringatan. */
    private static final class MonthlyCell extends TableCell<ItemRow, BigDecimal> {

        private final Tooltip warning = new Tooltip(Messages.get("productivity.budget.item.noDuration"));

        MonthlyCell() {
            getStyleClass().add("budget-number-cell");
        }

        @Override
        protected void updateItem(BigDecimal monthly, boolean empty) {
            super.updateItem(monthly, empty);
            ItemRow row = empty ? null : getTableRow().getItem();
            boolean missing = row != null && row.item().missingDuration();
            getStyleClass().remove("budget-warning-cell");
            if (row == null) {
                setText(null);
                setTooltip(null);
                return;
            }
            setText(
                    missing
                            ? Messages.format("productivity.budget.item.noDurationValue", BudgetLabels.money(monthly))
                            : BudgetLabels.money(monthly));
            setTooltip(missing ? warning : null);
            if (missing) {
                getStyleClass().add("budget-warning-cell");
            }
        }
    }

    /** Baris nonaktif: pseudo-class {@code :inactive} → merah + coret (lihat CSS). */
    private static final class ItemTableRow extends TableRow<ItemRow> {

        @Override
        protected void updateItem(ItemRow row, boolean empty) {
            super.updateItem(row, empty);
            pseudoClassStateChanged(
                    INACTIVE, !empty && row != null && !row.item().status().counts());
        }
    }
}
