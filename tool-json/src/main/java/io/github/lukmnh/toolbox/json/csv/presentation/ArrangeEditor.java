package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.csv.domain.ArrangeStep;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnRule;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Editor langkah Atur kolom: centang = pertahankan, kolom teks = nama baru, Naik/Turun = urutan. */
final class ArrangeEditor extends VBox {

    private static final double CHECK_WIDTH = 180;

    private final boolean enabled;
    private final Consumer<PipelineStep> onChange;
    private final List<ColumnRule> rules;
    private final VBox rows = new VBox(ToolLayout.GAP);

    ArrangeEditor(ArrangeStep step, List<String> columns, Consumer<PipelineStep> onChange) {
        super(ToolLayout.SPACING);
        this.enabled = step.enabled();
        this.onChange = onChange;
        this.rules = reconcile(step.rules(), columns);
        getChildren()
                .addAll(
                        ToolLayout.hint(Messages.get("json.csv.arrange.hint")),
                        ToolLayout.row(
                                ToolLayout.button(Messages.get("json.csv.arrange.keepAll"), () -> setAll(true)),
                                ToolLayout.button(Messages.get("json.csv.arrange.dropAll"), () -> setAll(false))),
                        rows);
        rebuildRows();
    }

    /** Aturan untuk kolom yang sudah tidak ada dibuang; kolom baru ditambah di akhir tanpa perubahan. */
    private static List<ColumnRule> reconcile(List<ColumnRule> existing, List<String> columns) {
        List<ColumnRule> reconciled = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (ColumnRule rule : existing) {
            if (columns.contains(rule.source()) && covered.add(rule.source())) {
                reconciled.add(rule);
            }
        }
        columns.stream()
                .filter(column -> !covered.contains(column))
                .map(ColumnRule::unchanged)
                .forEach(reconciled::add);
        return reconciled;
    }

    private void rebuildRows() {
        rows.getChildren().clear();
        for (int index = 0; index < rules.size(); index++) {
            rows.getChildren().add(row(index));
        }
        if (rules.isEmpty()) {
            rows.getChildren().add(ToolLayout.hint(Messages.get("json.csv.arrange.empty")));
        }
    }

    private HBox row(int index) {
        ColumnRule rule = rules.get(index);
        CheckBox keep = new CheckBox(rule.source());
        keep.setSelected(rule.keep());
        keep.setPrefWidth(CHECK_WIDTH);
        keep.setMinWidth(CHECK_WIDTH);
        TextField name = new TextField(rule.name());
        name.setPromptText(rule.source());
        name.disableProperty().bind(keep.selectedProperty().not());
        HBox.setHgrow(name, Priority.ALWAYS);
        keep.selectedProperty().addListener((observable, previous, current) -> update(index, keep, name));
        name.textProperty().addListener((observable, previous, current) -> update(index, keep, name));
        Button up = ToolLayout.button(Messages.get("json.csv.move.up"), () -> move(index, -1));
        up.setDisable(index == 0);
        Button down = ToolLayout.button(Messages.get("json.csv.move.down"), () -> move(index, 1));
        down.setDisable(index == rules.size() - 1);
        return ToolLayout.row(keep, name, up, down);
    }

    private void update(int index, CheckBox keep, TextField name) {
        rules.set(index, new ColumnRule(rules.get(index).source(), name.getText(), keep.isSelected()));
        emit();
    }

    private void move(int index, int offset) {
        rules.add(index + offset, rules.remove(index));
        rebuildRows();
        emit();
    }

    private void setAll(boolean keep) {
        rules.replaceAll(rule -> new ColumnRule(rule.source(), rule.name(), keep));
        rebuildRows();
        emit();
    }

    private void emit() {
        onChange.accept(new ArrangeStep(enabled, rules));
    }
}
