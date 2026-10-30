package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableStringValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Kerangka Installment Simulator: header + tab Cicilan, Diskon, Pajak & PPN, Invoice. */
final class CalculatorView {

    private CalculatorView() {}

    static Node create(
            InstallmentViewModel installment, DiscountViewModel discount, TaxViewModel tax, InvoiceViewModel invoice) {
        TabPane tabs = new TabPane(
                tab("productivity.calculator.tab.installment", InstallmentPane.create(installment)),
                tab("productivity.calculator.tab.discount", DiscountPane.create(discount)),
                tab("productivity.calculator.tab.tax", TaxPane.create(tax)),
                tab("productivity.calculator.tab.invoice", InvoicePane.create(invoice)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("underline-tabs");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.PRODUCTIVITY,
                                Messages.get("productivity.calculator.name"),
                                Messages.get("productivity.calculator.hint")),
                        tabs);
        return page;
    }

    /** Kotak angka besar + keterangan kecil, dipakai semua tab. */
    static VBox tile(String caption, ObservableStringValue value) {
        return tile(Bindings.createStringBinding(() -> caption), value);
    }

    static VBox tile(ObservableStringValue caption, ObservableStringValue value) {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.textProperty().bind(value);
        Label captionLabel = new Label();
        captionLabel.getStyleClass().add("field-label");
        captionLabel.textProperty().bind(caption);
        VBox tile = new VBox(2, valueLabel, captionLabel);
        tile.getStyleClass().add("stat-tile");
        return tile;
    }

    private static Tab tab(String titleKey, Node content) {
        return new Tab(Messages.get(titleKey), scroll(content));
    }

    private static ScrollPane scroll(Node content) {
        VBox wrapper = new VBox(content);
        wrapper.getStyleClass().add("tool-view");
        wrapper.setPadding(new Insets(ToolLayout.SPACING, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }
}
