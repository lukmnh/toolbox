package io.github.lukmnh.toolbox.pdf.compress.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;

/** Port: kompres gambar di dalam PDF. */
public interface PdfCompressor {

    CompressionSession open(PdfSource source);
}
