package io.github.lukmnh.toolbox.pdf.merge.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;

/** Satu proses penggabungan. Wajib ditutup untuk melepas semua dokumen sumber. */
public interface MergeSession extends AutoCloseable {

    /** Tambahkan semua halaman {@code source} di akhir dokumen hasil. */
    void append(PdfSource source);

    int pageCount();

    void save(Path output);

    @Override
    void close();
}
