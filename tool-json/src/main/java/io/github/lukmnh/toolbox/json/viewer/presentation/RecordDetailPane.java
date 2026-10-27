package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonLayout;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel kanan: record terpilih sebagai teks Pretty (maks 1 MB) dan Tree. */
final class RecordDetailPane extends VBox {

    private final ViewerViewModel viewModel;
    private final TextArea pretty = JsonLayout.monospace(new TextArea());
    private final Label truncated = new Label(Messages.get("json.viewer.detail.truncated"));
    private final JsonTreeView tree;

    RecordDetailPane(ViewerViewModel viewModel) {
        super(4);
        this.viewModel = viewModel;
        this.tree = new JsonTreeView(viewModel::loadChildren);
        pretty.setEditable(false);
        pretty.setPromptText(Messages.get("json.viewer.detail.empty"));
        truncated.getStyleClass().add("field-label");
        truncated.setWrapText(true);
        truncated.setVisible(false);
        JsonLayout.collapseWhenHidden(truncated);

        Tab prettyTab = new Tab(Messages.get("json.viewer.detail.pretty"), new VBox(4, truncated, pretty));
        VBox.setVgrow(pretty, Priority.ALWAYS);
        Tab treeTab = new Tab(Messages.get("json.viewer.detail.tree"), tree);
        TabPane tabs = new TabPane(prettyTab, treeTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);

        viewModel.selectedRecordProperty().addListener((observable, previous, record) -> show(record.intValue()));
    }

    private void show(int record) {
        if (record < 0) {
            pretty.clear();
            truncated.setVisible(false);
            tree.clear();
            return;
        }
        ViewerViewModel.Detail detail = viewModel.detail(record);
        pretty.setText(detail.text());
        truncated.setVisible(detail.truncated());
        viewModel.loadRecordNode(record, node -> {
            // Abaikan hasil lama bila user sudah memilih record lain
            if (viewModel.selectedRecordProperty().get() == record) {
                tree.show(node);
            }
        });
    }
}
