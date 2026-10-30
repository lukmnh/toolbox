package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetCalculator;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetSummary;
import io.github.lukmnh.toolbox.productivity.budget.domain.SheetTotals;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tab Ringkasan: kartu angka, tabel pemasukan (editable), total per daftar, dan grafik. */
final class SummaryPane extends ScrollPane {

    private static final double TABLE_HEIGHT = 240;
    private static final double SHEET_COLUMN_WIDTH = 180;
    private static final double MONEY_COLUMN_WIDTH = 140;
    private static final int RATIO_DECIMALS = 1;
    private static final String POSITIVE = "budget-positive";
    private static final String NEGATIVE = "budget-negative";

    private final BudgetEdits edits;
    private final IncomeTable incomes;
    private final TableView<SheetTotals> sheetTotals = new TableView<>();
    private final BudgetCharts charts = new BudgetCharts();
    private final Label incomeCard = statValue();
    private final Label expenseCard = statValue();
    private final Label remainingCard = statValue();
    private final Label ratioCard = statValue();
    private final Label perYearCard = statValue();
    private final Label incomeTotal = new Label();
    private final Label expenseTotal = new Label();

    SummaryPane(BudgetEdits edits, ToolContext context) {
        this.edits = edits;
        this.incomes = new IncomeTable(edits, context.notifier());
        setFitToWidth(true);
        getStyleClass().add("budget-summary");
        setupSheetTotals();
        VBox content = new VBox(
                ToolLayout.SPACING,
                cards(),
                tables(),
                ToolLayout.card(Messages.get("productivity.budget.chart.title"), List.of(), charts.view()));
        content.setPadding(new Insets(ToolLayout.SPACING, 0, ToolLayout.SPACING, 0));
        setContent(content);
    }

    void show(Budget budget) {
        BudgetSummary summary = BudgetCalculator.summarize(budget);
        incomes.show(budget.incomes());
        sheetTotals.getItems().setAll(summary.sheets());
        incomeCard.setText(BudgetLabels.moneyCents(summary.totalIncome()));
        expenseCard.setText(BudgetLabels.money(summary.totalMonthly()));
        remainingCard.setText(BudgetLabels.moneyCents(summary.remaining()));
        remainingCard.getStyleClass().removeAll(POSITIVE, NEGATIVE);
        remainingCard.getStyleClass().add(summary.remaining().signum() < 0 ? NEGATIVE : POSITIVE);
        ratioCard.setText(summary.expenseRatioPercent()
                .map(ratio -> Messages.format(
                        "productivity.budget.summary.percent", RupiahFormat.plain(ratio, RATIO_DECIMALS)))
                .orElse(Messages.get("productivity.budget.summary.noIncome")));
        perYearCard.setText(BudgetLabels.money(summary.perYear()));
        incomeTotal.setText(
                Messages.format("productivity.budget.income.total", BudgetLabels.moneyCents(summary.totalIncome())));
        expenseTotal.setText(Messages.format(
                "productivity.budget.summary.expenseTotal", BudgetLabels.money(summary.totalMonthly())));
        charts.show(budget, summary.sheets());
    }

    private FlowPane cards() {
        FlowPane cards = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        cards.getChildren()
                .addAll(
                        statCard("productivity.budget.summary.income", incomeCard),
                        statCard("productivity.budget.summary.expense", expenseCard),
                        statCard("productivity.budget.summary.remaining", remainingCard),
                        statCard("productivity.budget.summary.ratio", ratioCard),
                        statCard("productivity.budget.summary.perYear", perYearCard));
        return cards;
    }

    private static VBox statCard(String key, Label value) {
        Label caption = new Label(Messages.get(key));
        caption.getStyleClass().add("field-label");
        VBox card = ToolLayout.card(caption, value);
        card.getStyleClass().add("budget-stat-card");
        return card;
    }

    private static Label statValue() {
        Label value = new Label();
        value.getStyleClass().add("budget-stat-value");
        return value;
    }

    private HBox tables() {
        incomes.view().setPrefHeight(TABLE_HEIGHT);
        sheetTotals.setPrefHeight(TABLE_HEIGHT);
        Button add = ToolLayout.button(Messages.get("productivity.budget.income.add"), this::addIncome);
        Button delete = ToolLayout.button(Messages.get("productivity.budget.income.delete"), this::deleteIncome);
        delete.disableProperty()
                .bind(incomes.view().getSelectionModel().selectedItemProperty().isNull());
        incomeTotal.getStyleClass().add("budget-total-row");
        expenseTotal.getStyleClass().add("budget-total-row");
        VBox incomeCardBox = ToolLayout.card(
                Messages.get("productivity.budget.income.title"), List.of(add, delete), incomes.view(), incomeTotal);
        VBox sheetCardBox = ToolLayout.card(
                Messages.get("productivity.budget.summary.sheets"), List.of(), sheetTotals, expenseTotal);
        HBox.setHgrow(incomeCardBox, Priority.ALWAYS);
        HBox.setHgrow(sheetCardBox, Priority.ALWAYS);
        return new HBox(ToolLayout.SPACING, incomeCardBox, sheetCardBox);
    }

    private void setupSheetTotals() {
        sheetTotals.setPlaceholder(ToolLayout.hint(Messages.get("productivity.budget.summary.noSheets")));
        sheetTotals
                .getColumns()
                .addAll(List.of(
                        totalsColumn(
                                "productivity.budget.summary.column.sheet", SHEET_COLUMN_WIDTH, SheetTotals::sheetName),
                        totalsColumn(
                                "productivity.budget.summary.column.price",
                                MONEY_COLUMN_WIDTH,
                                totals -> BudgetLabels.money(totals.totalPrice())),
                        totalsColumn(
                                "productivity.budget.summary.column.monthly",
                                MONEY_COLUMN_WIDTH,
                                totals -> BudgetLabels.money(totals.totalMonthly()))));
    }

    private static TableColumn<SheetTotals, String> totalsColumn(
            String key, double width, Function<SheetTotals, String> value) {
        TableColumn<SheetTotals, String> column = new TableColumn<>(Messages.get(key));
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
        return column;
    }

    private void addIncome() {
        edits.addIncome().ifPresent(incomes::editName);
    }

    private void deleteIncome() {
        incomes.selectedId().ifPresent(edits::deleteIncome);
    }
}
