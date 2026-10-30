package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetCalculator;
import io.github.lukmnh.toolbox.productivity.budget.domain.CategoryTotal;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.SheetTotals;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Grafik Ringkasan: pie proporsi pengeluaran/bulan per daftar, bar per kategori untuk daftar terpilih. */
final class BudgetCharts {

    private static final double CHART_HEIGHT = 320;

    private final PieChart pie = new PieChart();
    private final CategoryAxis categoryAxis = new CategoryAxis();
    private final NumberAxis amountAxis = new NumberAxis();
    private final BarChart<String, Number> bar = new BarChart<>(categoryAxis, amountAxis);
    private final ComboBox<ExpenseSheet> sheetChooser = new ComboBox<>();
    private Budget current;

    BudgetCharts() {
        pie.setAnimated(false);
        pie.setLegendVisible(false);
        pie.setPrefHeight(CHART_HEIGHT);
        bar.setAnimated(false);
        bar.setLegendVisible(false);
        bar.setPrefHeight(CHART_HEIGHT);
        amountAxis.setTickLabelFormatter(new RupiahAxisFormatter());
        sheetChooser.setConverter(ToolLayout.labels(ExpenseSheet::name));
        sheetChooser.valueProperty().addListener((observable, previous, sheet) -> showBar(sheet));
    }

    Node view() {
        VBox pieBox = new VBox(ToolLayout.GAP, ToolLayout.hint(Messages.get("productivity.budget.chart.pie")), pie);
        VBox barBox = new VBox(
                ToolLayout.GAP,
                ToolLayout.row(ToolLayout.hint(Messages.get("productivity.budget.chart.bar")), sheetChooser),
                bar);
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        HBox.setHgrow(barBox, Priority.ALWAYS);
        return new HBox(ToolLayout.SPACING, pieBox, barBox);
    }

    void show(Budget budget, List<SheetTotals> totals) {
        current = budget;
        showPie(totals);
        String selectedId =
                sheetChooser.getValue() == null ? "" : sheetChooser.getValue().id();
        sheetChooser.getItems().setAll(budget.sheets());
        sheetChooser.setValue(budget.sheets().stream()
                .filter(sheet -> sheet.id().equals(selectedId))
                .findFirst()
                .orElse(budget.sheets().isEmpty() ? null : budget.sheets().getFirst()));
        showBar(sheetChooser.getValue());
    }

    /** Nilai chart bertipe double karena API JavaFX; perhitungan tetap BigDecimal, konversi hanya untuk gambar. */
    private void showPie(List<SheetTotals> totals) {
        pie.getData().clear();
        for (int index = 0; index < totals.size(); index++) {
            SheetTotals total = totals.get(index);
            if (total.totalMonthly().signum() == 0) {
                continue;
            }
            PieChart.Data slice =
                    new PieChart.Data(total.sheetName(), total.totalMonthly().doubleValue());
            pie.getData().add(slice);
            String color = BudgetLabels.paletteColor(index);
            String tooltip = total.sheetName() + ": " + BudgetLabels.money(total.totalMonthly());
            styleWhenReady(slice.getNode(), slice.nodeProperty(), "-fx-pie-color: " + color + ";", tooltip);
        }
    }

    private void showBar(ExpenseSheet sheet) {
        bar.getData().clear();
        if (sheet == null || current == null) {
            return;
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<CategoryTotal> totals = BudgetCalculator.categoryTotals(sheet, BudgetLabels.UNCATEGORIZED_COLOR);
        totals.forEach(total -> series.getData().add(new XYChart.Data<>(label(total), total.monthly())));
        bar.getData().add(series);
        for (int index = 0; index < totals.size(); index++) {
            XYChart.Data<String, Number> data = series.getData().get(index);
            CategoryTotal total = totals.get(index);
            String tooltip = label(total) + ": " + BudgetLabels.money(total.monthly());
            styleWhenReady(data.getNode(), data.nodeProperty(), "-fx-bar-fill: " + total.color() + ";", tooltip);
        }
    }

    private static String label(CategoryTotal total) {
        return total.uncategorized() ? Messages.get("productivity.budget.category.none") : total.name();
    }

    /** Node data dibuat chart saat layout; style dipasang begitu node tersedia. Warna sudah tervalidasi hex. */
    private static void styleWhenReady(Node node, ObservableValue<Node> nodeProperty, String style, String tooltip) {
        if (node != null) {
            decorate(node, style, tooltip);
            return;
        }
        nodeProperty.addListener((observable, previous, created) -> {
            if (created != null) {
                decorate(created, style, tooltip);
            }
        });
    }

    private static void decorate(Node node, String style, String tooltip) {
        node.setStyle(style);
        Tooltip.install(node, new Tooltip(tooltip));
    }

    /** Sumbu nilai dalam format Rupiah. */
    private static final class RupiahAxisFormatter extends StringConverter<Number> {

        @Override
        public String toString(Number value) {
            return value == null ? "" : BudgetLabels.money(new BigDecimal(Objects.toString(value)));
        }

        @Override
        public Number fromString(String text) {
            throw new UnsupportedOperationException("Axis labels cannot be typed");
        }
    }
}
