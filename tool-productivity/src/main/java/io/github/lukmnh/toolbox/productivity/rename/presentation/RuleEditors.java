package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.rename.domain.Placement;
import io.github.lukmnh.toolbox.productivity.rename.domain.RemoveMode;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Editor per jenis aturan. Setiap perubahan kontrol membuat record aturan baru dan menggantinya di {@link RuleItem},
 * sehingga preview selalu dihitung dari record immutable.
 */
final class RuleEditors {

    private static final String PREFIX = "productivity.rename.editor.";
    /** Maksimal 9 digit supaya selalu muat di {@code int}. */
    private static final Pattern DIGITS = Pattern.compile("\\d{0,9}");

    private static final int MAX_NUMBER = 999_999_999;
    private static final int MAX_POSITION = 255;
    private static final double SPINNER_WIDTH = 120;

    private RuleEditors() {}

    static Node create(RuleItem item) {
        return switch (item.rule()) {
            case RenameRule.Replace rule -> replaceEditor(item, rule);
            case RenameRule.Affix rule -> affixEditor(item, rule);
            case RenameRule.Numbering rule -> numberingEditor(item, rule);
            case RenameRule.ChangeCase rule -> caseEditor(item, rule);
            case RenameRule.InsertDate rule -> dateEditor(item, rule);
            case RenameRule.Remove rule -> removeEditor(item, rule);
            case RenameRule.ChangeExtension rule -> extensionEditor(item, rule);
        };
    }

    private static Node replaceEditor(RuleItem item, RenameRule.Replace initial) {
        TextField find = new TextField(initial.find());
        TextField replacement = new TextField(initial.replacement());
        CheckBox regex = checkBox("replace.regex", initial.regex());
        CheckBox caseSensitive = checkBox("replace.caseSensitive", initial.caseSensitive());
        CheckBox includeExtension = checkBox("includeExtension", initial.includeExtension());
        onChange(
                () -> item.ruleProperty()
                        .set(new RenameRule.Replace(
                                find.getText(),
                                replacement.getText(),
                                regex.isSelected(),
                                caseSensitive.isSelected(),
                                includeExtension.isSelected())),
                find.textProperty(),
                replacement.textProperty(),
                regex.selectedProperty(),
                caseSensitive.selectedProperty(),
                includeExtension.selectedProperty());
        return editor(
                labeled("replace.find", find),
                labeled("replace.with", replacement),
                regex,
                caseSensitive,
                includeExtension,
                ToolLayout.hint(Messages.get(PREFIX + "replace.hint")));
    }

    private static Node affixEditor(RuleItem item, RenameRule.Affix initial) {
        TextField prefix = new TextField(initial.prefix());
        TextField suffix = new TextField(initial.suffix());
        onChange(
                () -> item.ruleProperty().set(new RenameRule.Affix(prefix.getText(), suffix.getText())),
                prefix.textProperty(),
                suffix.textProperty());
        return editor(labeled("affix.prefix", prefix), labeled("affix.suffix", suffix));
    }

    private static Node numberingEditor(RuleItem item, RenameRule.Numbering initial) {
        Spinner<Integer> start = intSpinner(0, MAX_NUMBER, initial.start());
        Spinner<Integer> step = intSpinner(1, MAX_NUMBER, initial.step());
        Spinner<Integer> padding = intSpinner(0, RenameRule.Numbering.MAX_PADDING, initial.padding());
        ChoiceBox<Placement> placement =
                choice(List.of(Placement.values()), initial.placement(), RenameTexts::placement);
        TextField separator = new TextField(initial.separator());
        onChange(
                () -> item.ruleProperty()
                        .set(new RenameRule.Numbering(
                                start.getValue(),
                                step.getValue(),
                                padding.getValue(),
                                placement.getValue(),
                                separator.getText())),
                start.valueProperty(),
                step.valueProperty(),
                padding.valueProperty(),
                placement.valueProperty(),
                separator.textProperty());
        return editor(
                ToolLayout.row(
                        labeled("numbering.start", start),
                        labeled("numbering.step", step),
                        labeled("numbering.padding", padding)),
                labeled("placement", placement),
                labeled("separator", separator));
    }

    private static Node caseEditor(RuleItem item, RenameRule.ChangeCase initial) {
        ChoiceBox<CaseStyle> style = choice(List.of(CaseStyle.values()), initial.style(), RenameTexts::caseStyle);
        CheckBox includeExtension = checkBox("includeExtension", initial.includeExtension());
        onChange(
                () -> item.ruleProperty()
                        .set(new RenameRule.ChangeCase(style.getValue(), includeExtension.isSelected())),
                style.valueProperty(),
                includeExtension.selectedProperty());
        return editor(labeled("case.style", style), includeExtension);
    }

    private static Node dateEditor(RuleItem item, RenameRule.InsertDate initial) {
        TextField pattern = new TextField(initial.pattern());
        // Tanggal disisipkan, bukan pengganti nama: banyak file bisa bertanggal sama
        ChoiceBox<Placement> placement =
                choice(List.of(Placement.START, Placement.END), initial.placement(), RenameTexts::placement);
        TextField separator = new TextField(initial.separator());
        onChange(
                () -> item.ruleProperty()
                        .set(new RenameRule.InsertDate(pattern.getText(), placement.getValue(), separator.getText())),
                pattern.textProperty(),
                placement.valueProperty(),
                separator.textProperty());
        return editor(
                labeled("date.pattern", pattern),
                ToolLayout.hint(Messages.get(PREFIX + "date.hint")),
                labeled("placement", placement),
                labeled("separator", separator));
    }

    private static Node removeEditor(RuleItem item, RenameRule.Remove initial) {
        ChoiceBox<RemoveMode> mode = choice(List.of(RemoveMode.values()), initial.mode(), RenameTexts::removeMode);
        Spinner<Integer> count = intSpinner(0, MAX_POSITION, initial.count());
        Spinner<Integer> from = intSpinner(1, MAX_POSITION, initial.from());
        Spinner<Integer> to = intSpinner(1, MAX_POSITION, initial.to());
        VBox countField = labeled("remove.count", count);
        HBox rangeFields = ToolLayout.row(labeled("remove.from", from), labeled("remove.to", to));
        countField.visibleProperty().bind(mode.valueProperty().isNotEqualTo(RemoveMode.RANGE));
        rangeFields.visibleProperty().bind(mode.valueProperty().isEqualTo(RemoveMode.RANGE));
        ToolLayout.collapseWhenHidden(countField);
        ToolLayout.collapseWhenHidden(rangeFields);
        onChange(
                () -> item.ruleProperty()
                        .set(new RenameRule.Remove(mode.getValue(), count.getValue(), from.getValue(), to.getValue())),
                mode.valueProperty(),
                count.valueProperty(),
                from.valueProperty(),
                to.valueProperty());
        return editor(
                labeled("remove.mode", mode),
                countField,
                rangeFields,
                ToolLayout.hint(Messages.get(PREFIX + "remove.hint")));
    }

    private static Node extensionEditor(RuleItem item, RenameRule.ChangeExtension initial) {
        TextField extension = new TextField(initial.extension());
        onChange(
                () -> item.ruleProperty().set(new RenameRule.ChangeExtension(extension.getText())),
                extension.textProperty());
        Label warning = ToolLayout.inlineMessage(Tone.WARNING);
        warning.setText(Messages.get("productivity.rename.extension.warning"));
        return editor(
                labeled("extension.value", extension),
                ToolLayout.hint(Messages.get(PREFIX + "extension.hint")),
                warning);
    }

    private static void onChange(Runnable update, Observable... sources) {
        InvalidationListener listener = observable -> update.run();
        for (Observable source : sources) {
            source.addListener(listener);
        }
    }

    private static VBox editor(Node... children) {
        return new VBox(ToolLayout.SPACING, children);
    }

    private static VBox labeled(String key, Node control) {
        return ToolLayout.labeled(Messages.get(PREFIX + key), control);
    }

    private static CheckBox checkBox(String key, boolean selected) {
        CheckBox checkBox = new CheckBox(Messages.get(PREFIX + key));
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static <T> ChoiceBox<T> choice(List<T> options, T selected, Function<T, String> label) {
        ChoiceBox<T> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().setAll(options);
        choiceBox.setConverter(ToolLayout.labels(label));
        choiceBox.setValue(selected);
        return choiceBox;
    }

    /**
     * Spinner angka yang bisa diketik. Input dibatasi digit supaya konversi tidak pernah gagal; teks kosong dianggap
     * nilai minimum, bukan {@code null}.
     */
    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                return text == null || text.isBlank() ? min : Integer.parseInt(text.strip());
            }
        });
        Spinner<Integer> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(SPINNER_WIDTH);
        spinner.getEditor()
                .setTextFormatter(new TextFormatter<>(
                        change -> DIGITS.matcher(change.getControlNewText()).matches() ? change : null));
        return spinner;
    }
}
