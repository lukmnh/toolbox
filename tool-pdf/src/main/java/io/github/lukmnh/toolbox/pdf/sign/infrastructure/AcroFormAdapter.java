package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.FormFieldException;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;
import io.github.lukmnh.toolbox.pdf.sign.domain.FormField;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

/** Menerjemahkan field AcroForm PDFBox ↔ model domain {@link FormField}. */
final class AcroFormAdapter {

    /** Nilai radio button saat tidak ada pilihan. */
    private static final String RADIO_OFF = "Off";

    List<FormField> read(PDDocument document) {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) {
            return List.of();
        }
        List<FormField> fields = new ArrayList<>();
        for (PDField field : form.getFieldTree()) {
            if (!field.isReadOnly()) {
                toDomain(field).ifPresent(fields::add);
            }
        }
        return List.copyOf(fields);
    }

    private static Optional<FormField> toDomain(PDField field) {
        String name = field.getFullyQualifiedName();
        return switch (field) {
            case PDTextField text ->
                Optional.of(new FormField.Text(name, orEmpty(text.getValue()), text.isMultiline()));
            case PDCheckBox checkBox -> Optional.of(new FormField.CheckBox(name, checkBox.isChecked()));
            case PDRadioButton radio -> Optional.of(radioField(name, radio));
            case PDChoice choice -> Optional.of(new FormField.Choice(name, choice.getOptions(), firstValue(choice)));
            default -> Optional.empty();
        };
    }

    private static FormField radioField(String name, PDRadioButton radio) {
        List<String> options = radio.getOnValues().stream().sorted().toList();
        String value = orEmpty(radio.getValue());
        return new FormField.Choice(name, options, RADIO_OFF.equals(value) ? "" : value);
    }

    private static String firstValue(PDChoice choice) {
        List<String> values = choice.getValue();
        return values.isEmpty() ? "" : values.getFirst();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    void apply(PDDocument document, String fieldName, FieldValue value) {
        PDField field = findField(document, fieldName);
        try {
            switch (value) {
                case FieldValue.Text text
                when field instanceof PDTextField textField -> textField.setValue(text.value());
                case FieldValue.Checked checked
                when field instanceof PDCheckBox checkBox -> setChecked(checkBox, checked);
                case FieldValue.Choice choice
                when field instanceof PDRadioButton radio -> radio.setValue(choice.value());
                case FieldValue.Choice choice when field instanceof PDChoice list -> list.setValue(choice.value());
                default ->
                    throw new FormFieldException(
                            fieldName, new IllegalArgumentException("Tipe nilai tidak cocok dengan field"));
            }
        } catch (IOException | IllegalArgumentException e) {
            // Biasanya appearance atau font /DA field rusak: tampilkan ke user, jangan crash
            throw new FormFieldException(fieldName, e);
        }
    }

    private static void setChecked(PDCheckBox checkBox, FieldValue.Checked checked) throws IOException {
        if (checked.checked()) {
            checkBox.check();
        } else {
            checkBox.unCheck();
        }
    }

    private static PDField findField(PDDocument document, String fieldName) {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        PDField field = form == null ? null : form.getField(fieldName);
        if (field == null) {
            throw new FormFieldException(fieldName, new IllegalArgumentException("Field tidak ditemukan"));
        }
        return field;
    }
}
