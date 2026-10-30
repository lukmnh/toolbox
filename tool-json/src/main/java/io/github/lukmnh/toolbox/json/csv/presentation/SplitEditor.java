package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import io.github.lukmnh.toolbox.json.csv.domain.SplitStep;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Editor langkah Split kolom. Setiap perubahan langsung menjadi {@link SplitStep} baru. */
final class SplitEditor extends VBox {

    private static final String NAME_SEPARATOR = ",";

    private final boolean enabled;
    private final Consumer<PipelineStep> onChange;
    private final ComboBox<String> source = new ComboBox<>();
    private final ObjectProperty<SplitMode> mode;
    private final TextField rule = new TextField();
    private final Label ruleHint = ToolLayout.hint("");
    private final TextField names = new TextField();
    private final Spinner<Integer> maxColumns;
    private final CheckBox keepSource = new CheckBox(Messages.get("json.csv.split.keepSource"));

    SplitEditor(SplitStep step, List<String> columns, Consumer<PipelineStep> onChange) {
        super(ToolLayout.SPACING);
        this.enabled = step.enabled();
        this.onChange = onChange;
        this.mode = new SimpleObjectProperty<>(SplitMode.of(step.method()));
        source.getItems().setAll(columns);
        source.setValue(step.sourceColumn());
        source.setMaxWidth(Double.MAX_VALUE);
        rule.setText(SplitMode.ruleText(step.method()));
        ruleHint.setText(CsvLabels.splitRuleHint(mode.get()));
        names.setText(String.join(NAME_SEPARATOR + " ", step.resultNames()));
        names.setPromptText(Messages.get("json.csv.split.names.prompt"));
        maxColumns = new Spinner<>(SplitStep.MIN_COLUMNS, SplitStep.MAX_COLUMNS, step.maxColumns());
        keepSource.setSelected(step.keepSource());

        getChildren()
                .addAll(
                        ToolLayout.labeled(Messages.get("json.csv.split.source"), source),
                        ToolLayout.labeled(
                                Messages.get("json.csv.split.mode"),
                                ToolLayout.segmented(mode, List.of(SplitMode.values()), CsvLabels::splitMode)),
                        ToolLayout.labeled(Messages.get("json.csv.split.rule"), rule),
                        ruleHint,
                        ToolLayout.labeled(Messages.get("json.csv.split.names"), names),
                        ToolLayout.row(
                                ToolLayout.labeled(Messages.get("json.csv.split.maxColumns"), maxColumns), keepSource));
        listen();
    }

    private void listen() {
        source.valueProperty().addListener((observable, previous, current) -> emit());
        mode.addListener((observable, previous, current) -> {
            ruleHint.setText(CsvLabels.splitRuleHint(current));
            emit();
        });
        rule.textProperty().addListener((observable, previous, current) -> emit());
        names.textProperty().addListener((observable, previous, current) -> emit());
        maxColumns.valueProperty().addListener((observable, previous, current) -> emit());
        keepSource.selectedProperty().addListener((observable, previous, current) -> emit());
    }

    private void emit() {
        String column = source.getValue() == null ? "" : source.getValue();
        onChange.accept(new SplitStep(
                enabled,
                column,
                mode.get().toMethod(rule.getText()),
                parseNames(names.getText()),
                maxColumns.getValue(),
                keepSource.isSelected()));
    }

    /** Nama kosong di antara koma tetap dihitung, supaya {@code ", Belakang"} menamai kolom kedua saja. */
    private static List<String> parseNames(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(NAME_SEPARATOR, -1)).map(String::strip).toList();
    }
}
