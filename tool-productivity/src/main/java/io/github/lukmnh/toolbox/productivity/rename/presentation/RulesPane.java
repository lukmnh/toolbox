package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Kolom kiri: daftar aturan (urutan = urutan penerapan, checkbox = on/off) dan editor aturan terpilih. */
final class RulesPane extends VBox {

    private static final double PANE_WIDTH = 360;
    private static final double LIST_HEIGHT = 220;

    private final RenameViewModel viewModel;
    private final ListView<RuleItem> list;

    RulesPane(RenameViewModel viewModel) {
        super(ToolLayout.SPACING);
        this.viewModel = viewModel;
        this.list = buildList();
        setPrefWidth(PANE_WIDTH);
        setMinWidth(PANE_WIDTH);
        VBox editor = buildEditor();
        VBox.setVgrow(editor, Priority.ALWAYS);
        getChildren().addAll(buildRules(), editor);
    }

    private VBox buildRules() {
        MenuButton add = new MenuButton(Messages.get("productivity.rename.rules.add"));
        for (RuleKind kind : RuleKind.values()) {
            MenuItem menuItem = new MenuItem(kind.label());
            menuItem.setOnAction(event -> list.getSelectionModel().select(viewModel.addRule(kind)));
            add.getItems().add(menuItem);
        }
        Button up = ToolLayout.button(Messages.get("productivity.rename.moveUp"), () -> moveSelected(-1));
        Button down = ToolLayout.button(Messages.get("productivity.rename.moveDown"), () -> moveSelected(1));
        Button remove = ToolLayout.button(Messages.get("productivity.rename.rules.remove"), this::removeSelected);
        for (Button button : List.of(up, down, remove)) {
            button.disableProperty()
                    .bind(list.getSelectionModel().selectedItemProperty().isNull());
        }
        return ToolLayout.card(
                Messages.get("productivity.rename.rules.title"),
                List.of(),
                list,
                ToolLayout.row(add, ToolLayout.spacer(), up, down, remove),
                ToolLayout.hint(Messages.get("productivity.rename.rules.hint")));
    }

    private ListView<RuleItem> buildList() {
        ListView<RuleItem> rules = new ListView<>(viewModel.rules());
        rules.setPrefHeight(LIST_HEIGHT);
        rules.setPlaceholder(ToolLayout.hint(Messages.get("productivity.rename.rules.empty")));
        rules.setCellFactory(CheckBoxListCell.forListView(
                RuleItem::enabledProperty, ToolLayout.labels(item -> RenameTexts.summary(item.rule()))));
        return rules;
    }

    private VBox buildEditor() {
        ScrollPane content = new ScrollPane();
        content.setFitToWidth(true);
        VBox.setVgrow(content, Priority.ALWAYS);
        // Editor dibuat ulang hanya saat aturan terpilih berganti, bukan saat isinya diubah
        list.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, previous, selected) -> content.setContent(
                        selected == null
                                ? ToolLayout.hint(Messages.get("productivity.rename.editor.none"))
                                : RuleEditors.create(selected)));
        content.setContent(ToolLayout.hint(Messages.get("productivity.rename.editor.none")));
        VBox card = ToolLayout.card(Messages.get("productivity.rename.editor.title"), List.of(), content);
        card.visibleProperty().bind(Bindings.isNotEmpty(viewModel.rules()));
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private void moveSelected(int delta) {
        int index = list.getSelectionModel().getSelectedIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= viewModel.rules().size()) {
            return;
        }
        RuleItem selected = viewModel.rules().get(index);
        viewModel.moveRule(index, target);
        list.getSelectionModel().select(selected);
    }

    private void removeSelected() {
        RuleItem selected = list.getSelectionModel().getSelectedItem();
        if (selected != null) {
            viewModel.removeRule(selected);
        }
    }
}
