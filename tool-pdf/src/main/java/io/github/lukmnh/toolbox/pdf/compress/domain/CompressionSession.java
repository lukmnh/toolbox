package io.github.lukmnh.toolbox.pdf.compress.domain;

import java.nio.file.Path;

/**
 * Proses kompres dua tahap: analisis semua halaman untuk mendapatkan resolusi efektif tiap gambar, lalu
 * kompres tiap gambar kandidat. Dibagi per langkah supaya use case bisa melaporkan progress dan membatalkan.
 */
public interface CompressionSession extends AutoCloseable {

    int pageCount();

    long originalSize();

    /** Catat gambar yang tampil di halaman ini beserta DPI efektifnya. */
    void analyzePage(int pageIndex);

    /** Jumlah gambar unik yang ditemukan setelah semua halaman dianalisis. */
    int imageCount();

    /** @return {@code true} bila gambar diganti dengan versi yang lebih kecil */
    boolean recompressImage(int imageIndex, CompressionPreset preset);

    /** @return ukuran file hasil dalam byte */
    long save(Path output);

    /** Salin file asli apa adanya, dipakai bila hasil kompres tidak lebih kecil. */
    void copyOriginal(Path output);

    @Override
    void close();
}
