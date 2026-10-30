package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.ArrangeStep;
import io.github.lukmnh.toolbox.json.csv.domain.MergeStep;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import io.github.lukmnh.toolbox.json.csv.domain.SplitStep;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel kiri: daftar langkah pipeline (on/off, urutan) dan editor langkah terpilih. */
final class PipelinePane {

    private static final double LIST_HEIGHT = 150;
    private static final int UP = -1;
    private static final int DOWN = 1;

    private final CsvViewModel viewModel;
    private final ListView<PipelineStep> list;
    private final VBox editorHost = new VBox(ToolLayout.SPACING);

    private PipelinePane(CsvViewModel viewModel) {
        this.viewModel = viewModel;
        this.list = new ListView<>(viewModel.steps());
    }

    static VBox create(CsvViewModel viewModel) {
        return new PipelinePane(viewModel).build();
    }

    private VBox build() {
        list.setPrefHeight(LIST_HEIGHT);
        list.setMinHeight(LIST_HEIGHT);
        list.setPlaceholder(ToolLayout.hint(Messages.get("json.csv.steps.empty")));
        list.setCellFactory(view -> new StepCell());
        syncSelection();

        ScrollPane editorScroll = new ScrollPane(editorHost);
        editorScroll.setFitToWidth(true);
        VBox.setVgrow(editorScroll, Priority.ALWAYS);
        rebuildEditor();

        VBox card = ToolLayout.card(
                Messages.get("json.csv.steps.title"), List.of(), addButtons(), list, orderButtons(), editorScroll);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox addButtons() {
        return ToolLayout.row(
                ToolLayout.button(Messages.get("json.csv.steps.addSplit"), viewModel::addSplit),
                ToolLayout.button(Messages.get("json.csv.steps.addMerge"), viewModel::addMerge),
                ToolLayout.button(Messages.get("json.csv.steps.addArrange"), viewModel::addArrange));
    }

    private HBox orderButtons() {
        Button up = ToolLayout.button(Messages.get("json.csv.move.up"), () -> viewModel.moveSelected(UP));
        up.disableProperty().bind(viewModel.canMoveUp().not());
        Button down = ToolLayout.button(Messages.get("json.csv.move.down"), () -> viewModel.moveSelected(DOWN));
        down.disableProperty().bind(viewModel.canMoveDown().not());
        Button remove = ToolLayout.button(Messages.get("json.csv.steps.remove"), viewModel::removeSelected);
        remove.disableProperty().bind(viewModel.hasSelection().not());
        return ToolLayout.row(up, down, ToolLayout.spacer(), remove);
    }

    /**
     * Pilihan langkah disimpan di ViewModel, bukan di ListView: mengganti item (setiap ketikan di editor) bisa
     * mengosongkan seleksi ListView, dan itu tidak boleh membangun ulang editor yang sedang dipakai mengetik.
     */
    private void syncSelection() {
        list.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            if (current.intValue() >= 0) {
                viewModel.select(current.intValue());
            }
        });
        viewModel.selectedIndexProperty().addListener((observable, previous, current) -> selectInList());
        viewModel.steps().addListener((ListChangeListener<PipelineStep>) change -> selectInList());
        viewModel.editorRevisionProperty().addListener((observable, previous, current) -> rebuildEditor());
    }

    private void selectInList() {
        int index = viewModel.selectedIndexProperty().get();
        if (index < 0) {
            list.getSelectionModel().clearSelection();
        } else if (list.getSelectionModel().getSelectedIndex() != index) {
            list.getSelectionModel().select(index);
        }
    }

    private void rebuildEditor() {
        int index = viewModel.selectedIndexProperty().get();
        editorHost
                .getChildren()
                .setAll(viewModel
                        .selectedStep()
                        .map(step -> editor(index, step))
                        .orElseGet(() -> ToolLayout.hint(Messages.get("json.csv.editor.empty"))));
    }

    private Node editor(int index, PipelineStep step) {
        List<String> columns = viewModel.columnsBefore(index);
        Label title = new Label(Messages.format("json.csv.editor.title", index + 1, CsvLabels.stepTitle(step)));
        title.getStyleClass().add("section-title");
        Node body = switch (step) {
            case SplitStep split -> new SplitEditor(split, columns, viewModel::updateSelected);
            case MergeStep merge -> new MergeEditor(merge, columns, viewModel::updateSelected);
            case ArrangeStep arrange -> new ArrangeEditor(arrange, columns, viewModel::updateSelected);
        };
        return new VBox(ToolLayout.SPACING, title, body);
    }

    /** Baris daftar: centang aktif/nonaktif + ringkasan langkah. */
    private final class StepCell extends ListCell<PipelineStep> {

        private final CheckBox enabled = new CheckBox();
        private final Label summary = new Label();
        private final HBox content = ToolLayout.row(enabled, summary);

        private StepCell() {
            enabled.setOnAction(event -> {
                if (!isEmpty()) {
                    viewModel.setEnabled(getIndex(), enabled.isSelected());
                }
            });
        }

        @Override
        protected void updateItem(PipelineStep step, boolean empty) {
            super.updateItem(step, empty);
            if (empty || step == null) {
                setGraphic(null);
                return;
            }
            enabled.setSelected(step.enabled());
            summary.setText(CsvLabels.stepSummary(getIndex() + 1, step));
            summary.setDisable(!step.enabled());
            setGraphic(content);
        }
    }
}
