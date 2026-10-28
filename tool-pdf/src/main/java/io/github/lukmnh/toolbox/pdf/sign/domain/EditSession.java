package io.github.lukmnh.toolbox.pdf.sign.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Dokumen yang sedang diedit. Implementasi wajib thread-safe karena render dan update field berjalan di
 * background thread. Wajib ditutup saat tool di-dispose.
 */
public interface EditSession extends AutoCloseable {

    int pageCount();

    PageGeometry geometry(int pageIndex);

    BufferedImage render(int pageIndex, int dpi);

    List<FormField> formFields();

    /** Nilai langsung diterapkan ke dokumen; render ulang halaman untuk melihat hasilnya. */
    void setFieldValue(String fieldName, FieldValue value);

    /** Font yang dipakai untuk teks overlay, agar preview memakai font yang sama. */
    FontSpec fontSpec();

    /**
     * Tulis salinan dokumen dengan overlay di-burn ke {@code output}. Dokumen yang sedang diedit tidak
     * berubah, sehingga user bisa lanjut mengedit dan download lagi.
     */
    void export(List<Overlay> overlays, Path output, OutputProtection protection);

    @Override
    void close();
}
