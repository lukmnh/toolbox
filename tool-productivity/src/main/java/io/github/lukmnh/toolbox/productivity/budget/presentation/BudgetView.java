package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.io.File;
import java.util.List;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Layout Budget: header + indikator autosave + import/export (Excel/PDF), panel daftar budget di kiri, editor di tengah. */
final class BudgetView {

    private static final double LIST_DIVIDER = 0.18;
    private static final List<String> IMPORT_PATTERNS = List.of("*.xlsx", "*.pdf");

    private BudgetView() {}

    static Node create(BudgetViewModel viewModel, ToolContext context) {
        BudgetEdits edits = new BudgetEdits(viewModel);
        BudgetListPane listPane = new BudgetListPane(viewModel, context.owner());
        BudgetEditor editor = new BudgetEditor(edits, context);
        editor.disableProperty().bind(viewModel.busyProperty());
        Node empty = emptyState(listPane);
        editor.visibleProperty().bind(viewModel.budgetProperty().isNotNull());
        empty.visibleProperty().bind(viewModel.budgetProperty().isNull());
        viewModel.budgetProperty().addListener((observable, previous, budget) -> {
            if (budget != null) {
                editor.show(budget);
            }
        });
        viewModel.currentEntryProperty().addListener((observable, previous, entry) -> editor.selectSummary());

        SplitPane split = new SplitPane(listPane, new StackPane(editor, empty));
        split.setDividerPositions(LIST_DIVIDER);
        SplitPane.setResizableWithParent(listPane, false);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox page = ToolLayout.page();
        page.getChildren().addAll(header(viewModel, context), split);
        return page;
    }

    private static HBox header(BudgetViewModel viewModel, ToolContext context) {
        BooleanBinding unavailable = viewModel.budgetProperty().isNull().or(viewModel.busyProperty());
        Button importButton =
                ToolLayout.button(Messages.get("productivity.budget.import"), () -> chooseImport(viewModel, context));
        importButton.disableProperty().bind(viewModel.busyProperty());
        MenuButton export = new MenuButton(Messages.get("productivity.budget.export"));
        export.getStyleClass().add("accent");
        export.getItems()
                .addAll(
                        menuItem(Messages.get("productivity.budget.export.xlsx"), viewModel::exportWorkbook),
                        menuItem(Messages.get("productivity.budget.export.pdf"), viewModel::exportPdf));
        export.disableProperty().bind(unavailable);
        HBox row = ToolLayout.row(
                ToolLayout.header(
                        ToolCategory.PRODUCTIVITY,
                        Messages.get("productivity.budget.name"),
                        Messages.get("productivity.budget.description")),
                ToolLayout.spacer(),
                saveBadge(viewModel),
                importButton,
                export);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private static MenuItem menuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }

    private static Label saveBadge(BudgetViewModel viewModel) {
        Label badge = ToolLayout.badge("", Tone.SUCCESS);
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(viewModel.saveErrorProperty());
        badge.setTooltip(tooltip);
        badge.visibleProperty().bind(viewModel.budgetProperty().isNotNull());
        ToolLayout.collapseWhenHidden(badge);
        updateBadge(badge, viewModel.saveStateProperty().get());
        viewModel.saveStateProperty().addListener((observable, previous, state) -> updateBadge(badge, state));
        return badge;
    }

    private static void updateBadge(Label badge, SaveState state) {
        switch (state) {
            case SAVED -> ToolLayout.update(badge, Messages.get("productivity.budget.save.saved"), Tone.SUCCESS);
            case SAVING -> ToolLayout.update(badge, Messages.get("productivity.budget.save.saving"), Tone.INFO);
            case FAILED -> ToolLayout.update(badge, Messages.get("productivity.budget.save.failed"), Tone.DANGER);
        }
    }

    private static Node emptyState(BudgetListPane listPane) {
        VBox empty = new VBox(
                ToolLayout.SPACING,
                ToolLayout.hint(Messages.get("productivity.budget.empty")),
                ToolLayout.accentButton(Messages.get("productivity.budget.list.create"), listPane::create));
        empty.setAlignment(Pos.CENTER);
        return empty;
    }

    private static void chooseImport(BudgetViewModel viewModel, ToolContext context) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("productivity.budget.import.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("productivity.budget.import.filter"), IMPORT_PATTERNS));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.importFile(chosen.toPath());
        }
    }
}
