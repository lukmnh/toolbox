package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Panel kiri: daftar budget + buat, ganti nama, duplikat, hapus. */
final class BudgetListPane extends VBox {

    private static final double MIN_WIDTH = 200;

    private final BudgetViewModel viewModel;
    private final Window owner;
    private final ListView<BudgetEntry> list = new ListView<>();
    /** Mencegah seleksi programatik (dari ViewModel) memicu {@code select} lagi. */
    private boolean syncing;

    BudgetListPane(BudgetViewModel viewModel, Window owner) {
        super(ToolLayout.GAP);
        this.viewModel = viewModel;
        this.owner = owner;
        setMinWidth(MIN_WIDTH);
        list.setCellFactory(view -> new EntryCell());
        list.setPlaceholder(ToolLayout.hint(Messages.get("productivity.budget.list.empty")));
        list.getSelectionModel().selectedItemProperty().addListener((observable, previous, entry) -> {
            if (!syncing && entry != null) {
                viewModel.select(entry);
            }
        });
        viewModel.currentEntryProperty().addListener((observable, previous, entry) -> syncSelection(entry));
        // Salinan sendiri (bukan setItems langsung) supaya penggantian daftar tidak memicu select budget lain
        viewModel.entries().addListener((ListChangeListener<BudgetEntry>) change -> syncEntries());
        VBox.setVgrow(list, Priority.ALWAYS);
        Label title = new Label(Messages.get("productivity.budget.list.title"));
        title.getStyleClass().add("card-title");
        getChildren().addAll(title, list, actions());
        disableProperty().bind(viewModel.busyProperty());
    }

    private FlowPane actions() {
        Button create = ToolLayout.accentButton(Messages.get("productivity.budget.list.create"), this::create);
        Button rename = ToolLayout.button(Messages.get("productivity.budget.list.rename"), this::rename);
        Button duplicate =
                ToolLayout.button(Messages.get("productivity.budget.list.duplicate"), viewModel::duplicateCurrent);
        Button delete = ToolLayout.button(Messages.get("productivity.budget.list.delete"), this::delete);
        for (Button button : new Button[] {rename, duplicate, delete}) {
            button.disableProperty().bind(viewModel.currentEntryProperty().isNull());
        }
        FlowPane actions = new FlowPane(ToolLayout.GAP, ToolLayout.GAP, create, rename, duplicate, delete);
        actions.setPrefWrapLength(MIN_WIDTH);
        return actions;
    }

    private void syncEntries() {
        syncing = true;
        try {
            list.getItems().setAll(viewModel.entries());
        } finally {
            syncing = false;
        }
        syncSelection(viewModel.currentEntryProperty().get());
    }

    private void syncSelection(BudgetEntry current) {
        syncing = true;
        try {
            if (current == null) {
                list.getSelectionModel().clearSelection();
                return;
            }
            list.getItems().stream()
                    .filter(entry -> entry.id().equals(current.id()))
                    .findFirst()
                    .ifPresent(list.getSelectionModel()::select);
        } finally {
            syncing = false;
        }
    }

    void create() {
        NameDialog.ask(
                        owner,
                        Messages.get("productivity.budget.list.create.header"),
                        Messages.get("productivity.budget.defaultName"))
                .ifPresent(viewModel::create);
    }

    private void rename() {
        BudgetEntry current = viewModel.currentEntryProperty().get();
        if (current != null) {
            NameDialog.ask(owner, Messages.get("productivity.budget.list.rename.header"), current.name())
                    .ifPresent(viewModel::renameCurrent);
        }
    }

    private void delete() {
        BudgetEntry current = viewModel.currentEntryProperty().get();
        if (current == null) {
            return;
        }
        boolean confirmed = ErrorDialogs.confirm(
                owner,
                Messages.format("productivity.budget.list.delete.header", current.name()),
                Messages.get("productivity.budget.list.delete.detail"),
                Messages.get("productivity.budget.delete"));
        if (confirmed) {
            viewModel.deleteCurrent();
        }
    }

    private static final class EntryCell extends ListCell<BudgetEntry> {
        @Override
        protected void updateItem(BudgetEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            setText(empty || entry == null ? null : entry.name());
        }
    }
}
