package io.github.lukmnh.toolbox.pdf.split.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PageRange;
import java.nio.file.Path;

/** Dokumen sumber yang sedang dipisah. Wajib ditutup. */
public interface SplitSession extends AutoCloseable {

    int pageCount();

    /** Tulis halaman dalam {@code range} ke file {@code output}. */
    void extract(PageRange range, Path output);

    @Override
    void close();
}
