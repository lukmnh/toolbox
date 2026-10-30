package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.MergeStep;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Editor langkah Merge kolom: daftar kolom terpilih (urutan = urutan gabung) + pemisah + nama baru. */
final class MergeEditor extends VBox {

    private final boolean enabled;
    private final Consumer<PipelineStep> onChange;
    private final List<String> columns;
    private final List<String> selected;
    private final VBox selectedRows = new VBox(ToolLayout.GAP);
    private final ComboBox<String> addChoice = new ComboBox<>();
    private final TextField separator = new TextField();
    private final TextField newName = new TextField();
    private final CheckBox skipEmpty = new CheckBox(Messages.get("json.csv.merge.skipEmpty"));
    private final CheckBox removeSources = new CheckBox(Messages.get("json.csv.merge.removeSources"));

    MergeEditor(MergeStep step, List<String> columns, Consumer<PipelineStep> onChange) {
        super(ToolLayout.SPACING);
        this.enabled = step.enabled();
        this.onChange = onChange;
        this.columns = List.copyOf(columns);
        this.selected = new ArrayList<>(step.sourceColumns());
        separator.setText(step.separator());
        separator.setPromptText(Messages.get("json.csv.merge.separator.prompt"));
        newName.setText(step.newName());
        skipEmpty.setSelected(step.skipEmpty());
        removeSources.setSelected(step.removeSources());
        addChoice.setPromptText(Messages.get("json.csv.merge.add.prompt"));
        addChoice.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addChoice, Priority.ALWAYS);
        Button add = ToolLayout.button(Messages.get("json.csv.merge.add"), this::addColumn);
        add.disableProperty().bind(addChoice.valueProperty().isNull());

        getChildren()
                .addAll(
                        ToolLayout.labeled(Messages.get("json.csv.merge.columns"), selectedRows),
                        ToolLayout.row(addChoice, add),
                        ToolLayout.hint(Messages.get("json.csv.merge.columns.hint")),
                        ToolLayout.labeled(Messages.get("json.csv.merge.separator"), separator),
                        ToolLayout.labeled(Messages.get("json.csv.merge.newName"), newName),
                        skipEmpty,
                        removeSources);
        rebuildRows();
        separator.textProperty().addListener((observable, previous, current) -> emit());
        newName.textProperty().addListener((observable, previous, current) -> emit());
        skipEmpty.selectedProperty().addListener((observable, previous, current) -> emit());
        removeSources.selectedProperty().addListener((observable, previous, current) -> emit());
    }

    private void rebuildRows() {
        selectedRows.getChildren().clear();
        for (int index = 0; index < selected.size(); index++) {
            selectedRows.getChildren().add(row(index));
        }
        if (selected.isEmpty()) {
            selectedRows.getChildren().add(ToolLayout.hint(Messages.get("json.csv.merge.columns.empty")));
        }
        addChoice
                .getItems()
                .setAll(columns.stream()
                        .filter(column -> !selected.contains(column))
                        .toList());
        addChoice.setValue(null);
    }

    private HBox row(int index) {
        String column = selected.get(index);
        Label name = new Label(columns.contains(column) ? column : Messages.format("json.csv.column.missing", column));
        Button up = ToolLayout.button(Messages.get("json.csv.move.up"), () -> move(index, -1));
        up.setDisable(index == 0);
        Button down = ToolLayout.button(Messages.get("json.csv.move.down"), () -> move(index, 1));
        down.setDisable(index == selected.size() - 1);
        Button remove = ToolLayout.button(Messages.get("json.csv.merge.remove"), () -> {
            selected.remove(index);
            changed();
        });
        return ToolLayout.row(name, ToolLayout.spacer(), up, down, remove);
    }

    private void addColumn() {
        String column = addChoice.getValue();
        if (column != null && !selected.contains(column)) {
            selected.add(column);
            changed();
        }
    }

    private void move(int index, int offset) {
        selected.add(index + offset, selected.remove(index));
        changed();
    }

    private void changed() {
        rebuildRows();
        emit();
    }

    private void emit() {
        onChange.accept(new MergeStep(
                enabled,
                selected,
                separator.getText(),
                newName.getText(),
                skipEmpty.isSelected(),
                removeSources.isSelected()));
    }
}
