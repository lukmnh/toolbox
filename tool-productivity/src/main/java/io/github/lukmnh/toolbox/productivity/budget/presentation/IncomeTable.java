package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Tabel pemasukan yang bisa diedit langsung: nama sumber, nominal per bulan (2 desimal), catatan. */
final class IncomeTable {

    private static final double NAME_WIDTH = 180;
    private static final double AMOUNT_WIDTH = 150;

    private final BudgetEdits edits;
    private final Notifier notifier;
    private final TableView<Income> table = new TableView<>();
    private final TableNavigation<Income> navigation = new TableNavigation<>(table, Income::id);
    private final TableColumn<Income, String> nameColumn;

    IncomeTable(BudgetEdits edits, Notifier notifier) {
        this.edits = edits;
        this.notifier = notifier;
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setPlaceholder(ToolLayout.hint(Messages.get("productivity.budget.income.empty")));
        nameColumn = textColumn("productivity.budget.income.column.name", NAME_WIDTH, Income::name, Income::withName);
        table.getColumns()
                .addAll(List.of(
                        nameColumn,
                        amountColumn(),
                        textColumn(
                                "productivity.budget.income.column.note", NAME_WIDTH, Income::note, Income::withNote)));
    }

    TableView<Income> view() {
        return table;
    }

    void show(List<Income> incomes) {
        if (incomes.equals(table.getItems())) {
            return;
        }
        TableSelection selection = TableSelection.capture(table, Income::id);
        table.getItems().setAll(incomes);
        selection.restore(table, Income::id);
    }

    Optional<String> selectedId() {
        Income selected = table.getSelectionModel().getSelectedItem();
        return selected == null ? Optional.empty() : Optional.of(selected.id());
    }

    void editName(String incomeId) {
        navigation.edit(incomeId, nameColumn);
    }

    private TableColumn<Income, String> textColumn(
            String key, double width, Function<Income, String> getter, BiFunction<Income, String, Income> setter) {
        TableColumn<Income, String> column = new TableColumn<>(Messages.get(key));
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(getter.apply(data.getValue())));
        column.setCellFactory(view -> new EditingCell<>(
                new EditingCell.Spec<>(
                        Function.identity(),
                        Function.identity(),
                        text -> Optional.of(text.strip()),
                        () -> notifier.warning(Messages.get("productivity.budget.edit.invalid")),
                        (income, value) -> edits.updateIncome(income.id(), current -> setter.apply(current, value)),
                        Income::id),
                navigation));
        return column;
    }

    private TableColumn<Income, BigDecimal> amountColumn() {
        TableColumn<Income, BigDecimal> column =
                new TableColumn<>(Messages.get("productivity.budget.income.column.amount"));
        column.setPrefWidth(AMOUNT_WIDTH);
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().amount()));
        column.setCellFactory(view -> {
            EditingCell<Income, BigDecimal> cell = new EditingCell<>(
                    new EditingCell.Spec<>(
                            BudgetLabels::moneyCents,
                            BudgetLabels::plainNumber,
                            ItemTable::parseNonNegative,
                            () -> notifier.warning(Messages.get("productivity.budget.number.invalid")),
                            (income, value) -> edits.updateIncome(income.id(), current -> current.withAmount(value)),
                            Income::id),
                    navigation);
            cell.getStyleClass().add("budget-number-cell");
            return cell;
        });
        return column;
    }
}
