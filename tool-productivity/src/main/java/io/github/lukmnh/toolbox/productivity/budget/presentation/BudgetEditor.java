package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Area tengah: tab Ringkasan + satu tab per daftar pengeluaran, dengan toolbar tambah/ganti nama/hapus/geser daftar.
 * Tab dipertahankan per id daftar sehingga edit isi tidak membuat ulang tabel (scroll & seleksi tetap).
 */
final class BudgetEditor extends VBox {

    private final BudgetEdits edits;
    private final ToolContext context;
    private final TabPane tabs = new TabPane();
    private final Tab summaryTab;
    private final SummaryPane summary;
    private final Map<String, Tab> sheetTabs = new HashMap<>();
    private final Map<String, SheetPane> sheetPanes = new HashMap<>();

    BudgetEditor(BudgetEdits edits, ToolContext context) {
        super(ToolLayout.GAP);
        this.edits = edits;
        this.context = context;
        this.summary = new SummaryPane(edits, context);
        summaryTab = new Tab(Messages.get("productivity.budget.tab.summary"), summary);
        summaryTab.setClosable(false);
        tabs.getTabs().add(summaryTab);
        tabs.getStyleClass().add("underline-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(toolbar(), tabs);
    }

    void show(Budget budget) {
        summary.show(budget);
        syncSheetTabs(budget.sheets());
    }

    void selectSummary() {
        tabs.getSelectionModel().select(summaryTab);
    }

    private HBox toolbar() {
        BooleanBinding summarySelected =
                Bindings.equal(summaryTab, tabs.getSelectionModel().selectedItemProperty());
        Button add = ToolLayout.button(Messages.get("productivity.budget.sheet.add"), this::addSheet);
        Button rename = ToolLayout.button(Messages.get("productivity.budget.sheet.rename"), this::renameSheet);
        Button delete = ToolLayout.button(Messages.get("productivity.budget.sheet.delete"), this::deleteSheet);
        Button left = ToolLayout.button(Messages.get("productivity.budget.sheet.left"), () -> moveSheet(-1));
        Button right = ToolLayout.button(Messages.get("productivity.budget.sheet.right"), () -> moveSheet(1));
        for (Button button : List.of(rename, delete, left, right)) {
            button.disableProperty().bind(summarySelected);
        }
        return ToolLayout.row(add, rename, delete, left, right);
    }

    /** Tambah/hapus/urutkan tab sesuai daftar terbaru tanpa membuat ulang tab yang sudah ada. */
    private void syncSheetTabs(List<ExpenseSheet> sheets) {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        List<String> ids = sheets.stream().map(ExpenseSheet::id).toList();
        sheetTabs.keySet().removeIf(id -> !ids.contains(id));
        sheetPanes.keySet().removeIf(id -> !ids.contains(id));
        List<Tab> ordered = new ArrayList<>(List.of(summaryTab));
        for (ExpenseSheet sheet : sheets) {
            Tab tab = sheetTabs.computeIfAbsent(sheet.id(), this::createTab);
            tab.setText(sheet.name());
            sheetPanes.get(sheet.id()).show(sheet);
            ordered.add(tab);
        }
        if (!ordered.equals(tabs.getTabs())) {
            tabs.getTabs().setAll(ordered);
            tabs.getSelectionModel().select(ordered.contains(selected) ? selected : summaryTab);
        }
    }

    private Tab createTab(String sheetId) {
        SheetPane pane = new SheetPane(sheetId, edits, context);
        sheetPanes.put(sheetId, pane);
        Tab tab = new Tab("", pane);
        tab.setClosable(false);
        tab.setUserData(sheetId);
        return tab;
    }

    private Optional<String> selectedSheetId() {
        Tab tab = tabs.getSelectionModel().getSelectedItem();
        return tab == null || tab == summaryTab ? Optional.empty() : Optional.of((String) tab.getUserData());
    }

    private void addSheet() {
        NameDialog.ask(context.owner(), Messages.get("productivity.budget.sheet.add.header"), edits.nextSheetName())
                .flatMap(edits::addSheet)
                .map(sheetTabs::get)
                .ifPresent(tabs.getSelectionModel()::select);
    }

    private void renameSheet() {
        selectedSheetId()
                .ifPresent(sheetId -> NameDialog.ask(
                                context.owner(),
                                Messages.get("productivity.budget.sheet.rename.header"),
                                sheetTabs.get(sheetId).getText())
                        .ifPresent(name -> edits.renameSheet(sheetId, name)));
    }

    private void deleteSheet() {
        selectedSheetId().ifPresent(sheetId -> {
            boolean confirmed = ErrorDialogs.confirm(
                    context.owner(),
                    Messages.format(
                            "productivity.budget.sheet.delete.header",
                            sheetTabs.get(sheetId).getText()),
                    Messages.get("productivity.budget.sheet.delete.detail"),
                    Messages.get("productivity.budget.delete"));
            if (confirmed) {
                edits.deleteSheet(sheetId);
            }
        });
    }

    private void moveSheet(int delta) {
        selectedSheetId().ifPresent(sheetId -> edits.moveSheet(sheetId, delta));
    }
}
