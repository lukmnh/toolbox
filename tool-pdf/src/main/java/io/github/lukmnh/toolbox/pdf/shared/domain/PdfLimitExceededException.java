package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;
import java.nio.file.Path;

/**
 * Input tool PDF melebihi batas keamanan; ditolak sebelum diproses lebih jauh. {@link #file()} adalah PDF, atau
 * gambar tanda tangan untuk {@code IMAGE_*}.
 */
public final class PdfLimitExceededException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Limit {
        /** actual/maximum dalam byte. */
        FILE_SIZE,
        PAGE_COUNT,
        /** actual = nomor halaman (1-based), maximum = batas detik. */
        RENDER_TIME,
        /** actual = sisi terpanjang gambar, maximum = batas px; dicek dari header sebelum decode. */
        IMAGE_DIMENSION
    }

    private final transient Path file;
    private final Limit limit;
    private final long actual;
    private final long maximum;

    public PdfLimitExceededException(Path file, Limit limit, long actual, long maximum) {
        super("Exceeds limit " + limit + ": " + file.getFileName(), null);
        this.file = file;
        this.limit = limit;
        this.actual = actual;
        this.maximum = maximum;
    }

    public Path file() {
        return file;
    }

    public Limit limit() {
        return limit;
    }

    public long actual() {
        return actual;
    }

    public long maximum() {
        return maximum;
    }
}
