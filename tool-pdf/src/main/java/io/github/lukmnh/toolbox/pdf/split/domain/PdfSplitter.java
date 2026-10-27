package io.github.lukmnh.toolbox.pdf.split.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;

/** Port: memisah PDF. */
public interface PdfSplitter {

    SplitSession open(PdfSource source);
}
