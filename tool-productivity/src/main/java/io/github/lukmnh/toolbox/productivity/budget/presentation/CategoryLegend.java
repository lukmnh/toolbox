package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/** Legend kategori di atas tabel: kotak warna + nama. Klik untuk ubah/hapus, tombol + untuk menambah. */
final class CategoryLegend extends FlowPane {

    private static final double SWATCH_SIZE = 14;

    private final String sheetId;
    private final BudgetEdits edits;
    private final Window owner;
    private final Button addButton;
    private List<Category> shown = List.of();

    CategoryLegend(String sheetId, BudgetEdits edits, Window owner) {
        super(ToolLayout.GAP, ToolLayout.GAP);
        this.sheetId = sheetId;
        this.edits = edits;
        this.owner = owner;
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("budget-legend");
        addButton = ToolLayout.button(Messages.get("productivity.budget.category.add"), this::addCategory);
        getChildren().add(addButton);
    }

    void show(ExpenseSheet sheet) {
        if (sheet.categories().equals(shown)) {
            return;
        }
        shown = sheet.categories();
        Label title = new Label(Messages.get("productivity.budget.category.legend"));
        title.getStyleClass().add("field-label");
        getChildren().setAll(title);
        sheet.categories().forEach(category -> getChildren().add(chip(category)));
        getChildren().add(addButton);
    }

    private Button chip(Category category) {
        Region swatch = new Region();
        swatch.setMinSize(SWATCH_SIZE, SWATCH_SIZE);
        swatch.setMaxSize(SWATCH_SIZE, SWATCH_SIZE);
        swatch.getStyleClass().add("budget-swatch");
        // Warna sudah tervalidasi #RRGGBB di domain, aman dipakai di style inline
        swatch.setStyle("-fx-background-color: " + category.color() + ";");
        Button chip = new Button(category.name(), swatch);
        chip.getStyleClass().add("budget-legend-chip");
        chip.setTooltip(new Tooltip(Messages.get("productivity.budget.category.edit")));
        chip.setOnAction(event -> editCategory(category));
        return chip;
    }

    private void addCategory() {
        String color = BudgetLabels.nextPaletteColor(
                shown.stream().map(Category::color).toList());
        CategoryDialog.show(owner, Messages.get("productivity.budget.category.add"), "", color, false)
                .ifPresent(result -> {
                    if (result instanceof CategoryDialog.Result.Save save) {
                        edits.addCategory(sheetId, save.name(), save.color());
                    }
                });
    }

    private void editCategory(Category category) {
        CategoryDialog.show(
                        owner,
                        Messages.get("productivity.budget.category.edit"),
                        category.name(),
                        category.color(),
                        true)
                .ifPresent(result -> {
                    switch (result) {
                        case CategoryDialog.Result.Save save ->
                            edits.updateCategory(sheetId, category.id(), save.name(), save.color());
                        case CategoryDialog.Result.Delete delete -> confirmDelete(category);
                    }
                });
    }

    private void confirmDelete(Category category) {
        boolean confirmed = ErrorDialogs.confirm(
                owner,
                Messages.format("productivity.budget.category.delete.header", category.name()),
                Messages.get("productivity.budget.category.delete.detail"),
                Messages.get("productivity.budget.delete"));
        if (confirmed) {
            edits.deleteCategory(sheetId, category.id());
        }
    }
}
