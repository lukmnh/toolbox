package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;

/** Teks berisi karakter yang tidak didukung font yang tersedia. */
public final class UnsupportedTextException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String text;

    public UnsupportedTextException(String text, Throwable cause) {
        super("Characters not supported by the font: " + text, cause);
        this.text = text;
    }

    public String text() {
        return text;
    }
}
