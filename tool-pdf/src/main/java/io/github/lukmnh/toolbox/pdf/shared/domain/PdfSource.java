package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.nio.file.Path;
import java.util.Objects;

/** File PDF input beserta password-nya (string kosong bila tidak terenkripsi). */
public record PdfSource(Path file, String password) {

    public PdfSource {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(password, "password");
    }

    public static PdfSource of(Path file) {
        return new PdfSource(file, "");
    }

    public PdfSource withPassword(String newPassword) {
        return new PdfSource(file, newPassword);
    }

    public String fileName() {
        return file.getFileName().toString();
    }

    @Override
    public String toString() {
        // Password tidak boleh ikut tercetak ke log
        return "PdfSource[" + file + "]";
    }
}
