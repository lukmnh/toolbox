package io.github.lukmnh.toolbox.pdf.sign.domain;

import java.util.List;
import java.util.Objects;

/** Field AcroForm yang bisa diisi user. Field read-only, tombol, dan field tanda tangan digital tidak disertakan. */
public sealed interface FormField permits FormField.Text, FormField.CheckBox, FormField.Choice {

    /** Nama lengkap (fully qualified) field, unik dalam dokumen. */
    String name();

    record Text(String name, String value, boolean multiline) implements FormField {
        public Text {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
        }
    }

    record CheckBox(String name, boolean checked) implements FormField {
        public CheckBox {
            Objects.requireNonNull(name, "name");
        }
    }

    /** Combo box, list box, atau grup radio button. {@code value} kosong bila belum dipilih. */
    record Choice(String name, List<String> options, String value) implements FormField {
        public Choice {
            Objects.requireNonNull(name, "name");
            options = List.copyOf(options);
            Objects.requireNonNull(value, "value");
        }
    }
}
