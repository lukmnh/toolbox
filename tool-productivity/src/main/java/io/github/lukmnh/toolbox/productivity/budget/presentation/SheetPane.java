package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetCalculator;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import io.github.lukmnh.toolbox.productivity.budget.domain.SheetTotals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Isi tab satu daftar pengeluaran: legend kategori, toolbar + filter, tabel item, dan baris total. */
final class SheetPane extends VBox {

    private static final KeyCombination ADD_ROW = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination DUPLICATE_ROW = new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination MOVE_UP = new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN);
    private static final KeyCombination MOVE_DOWN = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN);

    private final String sheetId;
    private final BudgetEdits edits;
    private final ItemTable table;
    private final CategoryLegend legend;
    private final ComboBox<RowFilter> categoryFilter = new ComboBox<>();
    private final ComboBox<RowFilter> statusFilter = new ComboBox<>();
    private final RowFilter allCategories = new RowFilter.All(Messages.get("productivity.budget.filter.allCategories"));
    private final RowFilter allStatuses = new RowFilter.All(Messages.get("productivity.budget.filter.allStatuses"));
    private final Label totals = new Label();
    private final Label warning = ToolLayout.inlineMessage(Tone.WARNING);

    SheetPane(String sheetId, BudgetEdits edits, ToolContext context) {
        super(ToolLayout.SPACING);
        this.sheetId = sheetId;
        this.edits = edits;
        this.table = new ItemTable(sheetId, edits, context.notifier());
        this.legend = new CategoryLegend(sheetId, edits, context.owner());
        setPadding(new Insets(ToolLayout.SPACING, 0, 0, 0));
        setupFilters();
        table.view().addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        VBox.setVgrow(table.view(), Priority.ALWAYS);
        totals.getStyleClass().add("budget-total-row");
        totals.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(legend, toolbar(), table.view(), totals, warning);
    }

    void show(ExpenseSheet sheet) {
        legend.show(sheet);
        refreshCategoryFilter(sheet.categories());
        table.show(sheet);
        SheetTotals sheetTotals = BudgetCalculator.totals(sheet);
        totals.setText(Messages.format(
                "productivity.budget.sheet.total",
                BudgetLabels.money(sheetTotals.totalPrice()),
                BudgetLabels.money(sheetTotals.totalMonthly())));
        warning.setText(
                sheetTotals.missingDurations() == 0
                        ? ""
                        : Messages.format(
                                "productivity.budget.sheet.missingDurations", sheetTotals.missingDurations()));
    }

    private HBox toolbar() {
        BooleanBinding noSelection =
                Bindings.isEmpty(table.view().getSelectionModel().getSelectedItems());
        BooleanBinding cannotMove = noSelection.or(table.sortedByColumn());
        Button add = ToolLayout.accentButton(Messages.get("productivity.budget.item.add"), this::addRow);
        add.setTooltip(new Tooltip(Messages.get("productivity.budget.item.add.tooltip")));
        Button duplicate = disabledWhen(button("productivity.budget.item.duplicate", this::duplicateRow), noSelection);
        Button up = disabledWhen(button("productivity.budget.item.up", () -> moveRow(-1)), cannotMove);
        Button down = disabledWhen(button("productivity.budget.item.down", () -> moveRow(1)), cannotMove);
        Button delete = disabledWhen(button("productivity.budget.item.delete", this::deleteRows), noSelection);
        up.setTooltip(new Tooltip(Messages.get("productivity.budget.item.move.tooltip")));
        down.setTooltip(new Tooltip(Messages.get("productivity.budget.item.move.tooltip")));
        return ToolLayout.row(add, duplicate, up, down, delete, ToolLayout.spacer(), categoryFilter, statusFilter);
    }

    private static Button button(String key, Runnable action) {
        return ToolLayout.button(Messages.get(key), action);
    }

    private static Button disabledWhen(Button button, BooleanBinding condition) {
        button.disableProperty().bind(condition);
        return button;
    }

    private void setupFilters() {
        categoryFilter.setConverter(ToolLayout.labels(RowFilter::label));
        statusFilter.setConverter(ToolLayout.labels(RowFilter::label));
        categoryFilter.getItems().setAll(allCategories, new RowFilter.Uncategorized());
        categoryFilter.setValue(allCategories);
        List<RowFilter> statuses = new ArrayList<>(List.of(allStatuses));
        for (ItemStatus status : ItemStatus.values()) {
            statuses.add(new RowFilter.WithStatus(status));
        }
        statusFilter.getItems().setAll(statuses);
        statusFilter.setValue(allStatuses);
        categoryFilter.valueProperty().addListener((observable, previous, current) -> applyFilter());
        statusFilter.valueProperty().addListener((observable, previous, current) -> applyFilter());
    }

    /** Pilihan filter ikut daftar kategori terbaru; pilihan aktif dipertahankan berdasarkan id kategori. */
    private void refreshCategoryFilter(List<Category> categories) {
        List<RowFilter> options = new ArrayList<>(List.of(allCategories, new RowFilter.Uncategorized()));
        categories.forEach(category -> options.add(new RowFilter.InCategory(category)));
        if (options.equals(categoryFilter.getItems())) {
            return;
        }
        RowFilter selected = categoryFilter.getValue();
        categoryFilter.getItems().setAll(options);
        categoryFilter.setValue(options.stream()
                .filter(option -> sameFilter(option, selected))
                .findFirst()
                .orElse(allCategories));
    }

    private static boolean sameFilter(RowFilter option, RowFilter selected) {
        if (option instanceof RowFilter.InCategory candidate && selected instanceof RowFilter.InCategory current) {
            return candidate.category().id().equals(current.category().id());
        }
        return Objects.equals(option, selected);
    }

    private void applyFilter() {
        RowFilter category = Objects.requireNonNullElse(categoryFilter.getValue(), allCategories);
        RowFilter status = Objects.requireNonNullElse(statusFilter.getValue(), allStatuses);
        table.setFilter(row -> category.test(row) && status.test(row));
    }

    private void onKey(KeyEvent event) {
        if (table.view().getEditingCell() != null) {
            return;
        }
        if (ADD_ROW.match(event)) {
            addRow();
        } else if (DUPLICATE_ROW.match(event)) {
            duplicateRow();
        } else if (MOVE_UP.match(event)) {
            moveRow(-1);
        } else if (MOVE_DOWN.match(event)) {
            moveRow(1);
        } else if (event.getCode() == KeyCode.DELETE) {
            deleteRows();
        } else if (event.getCode() == KeyCode.F2) {
            editFocused();
        } else {
            return;
        }
        event.consume();
    }

    /** Filter direset supaya baris baru (aktif, tanpa kategori) pasti terlihat. */
    private void addRow() {
        categoryFilter.setValue(allCategories);
        statusFilter.setValue(allStatuses);
        edits.addItem(sheetId).ifPresent(table::editName);
    }

    private void duplicateRow() {
        table.focusedId().ifPresent(itemId -> edits.duplicateItem(sheetId, itemId));
    }

    private void moveRow(int delta) {
        if (!table.sortedByColumn().get()) {
            table.focusedId().ifPresent(itemId -> edits.moveItem(sheetId, itemId, delta));
        }
    }

    private void deleteRows() {
        edits.deleteItems(sheetId, table.selectedIds());
    }

    private void editFocused() {
        var focused = table.view().getFocusModel().getFocusedCell();
        if (focused != null && focused.getRow() >= 0) {
            table.view().edit(focused.getRow(), table.view().getVisibleLeafColumn(focused.getColumn()));
        }
    }
}
