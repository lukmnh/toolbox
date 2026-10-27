package io.github.lukmnh.toolbox.pdf.sign.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;

/** Port: membuka PDF untuk diisi dan ditandatangani. */
public interface PdfEditor {

    EditSession open(PdfSource source);
}
