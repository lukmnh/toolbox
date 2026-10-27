package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;
import io.github.lukmnh.toolbox.pdf.sign.domain.FormField;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

/** Panel samping berisi semua field form PDF yang bisa diisi. */
final class FormFieldPanel extends ScrollPane {

    private static final double PANEL_WIDTH = 280;
    private static final double SPACING = 6;
    private static final int TEXT_AREA_ROWS = 3;

    private final SignViewModel viewModel;
    private final VBox fields = new VBox(SPACING);

    FormFieldPanel(SignViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("form-panel");
        setFitToWidth(true);
        setPrefWidth(PANEL_WIDTH);
        fields.setPadding(new Insets(SPACING * 2));
        setContent(fields);
        viewModel.formFields().addListener((ListChangeListener<FormField>) change -> rebuild());
        visibleProperty().bind(Bindings.isNotEmpty(viewModel.formFields()));
        managedProperty().bind(visibleProperty());
        rebuild();
    }

    private void rebuild() {
        fields.getChildren().clear();
        Label title = new Label(Messages.get("pdf.sign.form.title"));
        title.getStyleClass().add("section-title");
        fields.getChildren().add(title);
        viewModel.formFields().forEach(field -> fields.getChildren().add(editorFor(field)));
    }

    private Node editorFor(FormField field) {
        return switch (field) {
            case FormField.Text text -> labeled(text.name(), textEditor(text));
            case FormField.CheckBox checkBox -> checkBoxEditor(checkBox);
            case FormField.Choice choice -> labeled(choice.name(), choiceEditor(choice));
        };
    }

    private static VBox labeled(String name, Node editor) {
        Label label = new Label(name);
        label.getStyleClass().add("field-label");
        return new VBox(2, label, editor);
    }

    private Node textEditor(FormField.Text field) {
        TextInputControl input;
        if (field.multiline()) {
            TextArea area = new TextArea(field.value());
            area.setPrefRowCount(TEXT_AREA_ROWS);
            area.setWrapText(true);
            input = area;
        } else {
            TextField line = new TextField(field.value());
            line.setOnAction(event -> commitText(field.name(), line));
            input = line;
        }
        // Simpan saat fokus pindah, supaya halaman tidak dirender ulang tiap ketikan
        input.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused) {
                commitText(field.name(), input);
            }
        });
        input.setUserData(field.value());
        return input;
    }

    private void commitText(String name, TextInputControl input) {
        String committed = (String) input.getUserData();
        if (input.getText().equals(committed)) {
            return;
        }
        input.setUserData(input.getText());
        viewModel.updateField(name, new FieldValue.Text(input.getText()));
    }

    private Node checkBoxEditor(FormField.CheckBox field) {
        CheckBox box = new CheckBox(field.name());
        box.setSelected(field.checked());
        box.setOnAction(event -> viewModel.updateField(field.name(), new FieldValue.Checked(box.isSelected())));
        return box;
    }

    private Node choiceEditor(FormField.Choice field) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(field.options());
        combo.setMaxWidth(Double.MAX_VALUE);
        if (!field.value().isEmpty()) {
            combo.setValue(field.value());
        }
        combo.setOnAction(event -> {
            if (combo.getValue() != null) {
                viewModel.updateField(field.name(), new FieldValue.Choice(combo.getValue()));
            }
        });
        return combo;
    }
}
