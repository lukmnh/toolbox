package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.util.Objects;

/** Nilai baru untuk sebuah {@link FormField}. */
public sealed interface FieldValue permits FieldValue.Text, FieldValue.Checked, FieldValue.Choice {

    record Text(String value) implements FieldValue {
        public Text {
            Objects.requireNonNull(value, "value");
        }
    }

    record Checked(boolean checked) implements FieldValue {}

    record Choice(String value) implements FieldValue {
        public Choice {
            Objects.requireNonNull(value, "value");
        }
    }
}
