package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.nio.file.Path;
import java.util.Objects;

/** Font yang dipakai bersama oleh preview dan PDF, supaya ukuran dan posisi teks identik. */
public sealed interface FontSpec permits FontSpec.FileFont, FontSpec.BuiltinHelvetica {

    /** Nama family untuk preview JavaFX. */
    String family();

    record FileFont(Path file, String family) implements FontSpec {
        public FileFont {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(family, "family");
        }
    }

    /** Fallback: Helvetica Standard 14 (tanpa embed, hanya karakter WinAnsi). */
    record BuiltinHelvetica() implements FontSpec {
        @Override
        public String family() {
            return "Helvetica";
        }
    }
}
