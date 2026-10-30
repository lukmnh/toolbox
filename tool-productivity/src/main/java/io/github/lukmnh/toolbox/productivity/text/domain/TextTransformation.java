package io.github.lukmnh.toolbox.productivity.text.domain;

import io.github.lukmnh.toolbox.productivity.shared.domain.CaseConverter;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;
import java.util.Objects;

/** Transformasi teks yang bisa di-undo: ubah huruf atau rapikan baris. */
public sealed interface TextTransformation {

    String apply(String text);

    /** @param indonesianTitle kata tugas tetap huruf kecil; hanya berlaku untuk {@link CaseStyle#TITLE} */
    record ChangeCase(CaseStyle style, boolean indonesianTitle) implements TextTransformation {

        public ChangeCase {
            Objects.requireNonNull(style, "style");
        }

        @Override
        public String apply(String text) {
            return CaseConverter.convert(text, style, indonesianTitle);
        }
    }

    record Tidy(TidyOperation operation) implements TextTransformation {

        public Tidy {
            Objects.requireNonNull(operation, "operation");
        }

        @Override
        public String apply(String text) {
            return LineTidier.apply(text, operation);
        }
    }
}
