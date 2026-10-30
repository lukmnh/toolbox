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

/** Kerangka Kalkulator Cepat: header + dua tab (Diskon Ganda, Simulasi Cicilan). */
final class CalculatorView {

    private CalculatorView() {}

    static Node create(DiscountViewModel discount, InstallmentViewModel installment) {
        Tab discountTab =
                new Tab(Messages.get("productivity.calculator.tab.discount"), scroll(DiscountPane.create(discount)));
        Tab installmentTab = new Tab(
                Messages.get("productivity.calculator.tab.installment"), scroll(InstallmentPane.create(installment)));
        TabPane tabs = new TabPane(discountTab, installmentTab);
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

    /** Kotak angka besar + keterangan kecil, dipakai kedua tab. */
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
