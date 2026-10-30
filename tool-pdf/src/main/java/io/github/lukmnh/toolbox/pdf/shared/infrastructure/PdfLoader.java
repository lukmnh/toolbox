package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException.Limit;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

/** Membuka dan menyimpan PDF dengan aturan umum tool PDF. */
public final class PdfLoader {

    private static final System.Logger LOG = System.getLogger(PdfLoader.class.getName());

    /**
     * Load dari bytes di memory (bukan dari file) supaya file asli tidak ter-lock di Windows selama
     * dokumen terbuka. Pemanggil wajib menutup dokumen (try-with-resources).
     */
    public PDDocument load(PdfSource source) {
        long size = sizeOf(source.file());
        // Cek sebelum readAllBytes supaya file raksasa tidak menghabiskan heap
        if (size > SecurityLimits.PDF_MAX_FILE_BYTES) {
            throw new PdfLimitExceededException(
                    source.file(), Limit.FILE_SIZE, size, SecurityLimits.PDF_MAX_FILE_BYTES);
        }
        PDDocument document = parse(source, readBytes(source.file()));
        int pages = document.getNumberOfPages();
        if (pages > SecurityLimits.PDF_MAX_PAGES) {
            closeAfterRejection(document);
            throw new PdfLimitExceededException(source.file(), Limit.PAGE_COUNT, pages, SecurityLimits.PDF_MAX_PAGES);
        }
        return document;
    }

    private static PDDocument parse(PdfSource source, byte[] bytes) {
        try {
            return Loader.loadPDF(bytes, source.password());
        } catch (InvalidPasswordException e) {
            throw new PasswordRequiredException(
                    source.file(), !source.password().isEmpty(), e);
        } catch (IOException e) {
            throw new CorruptPdfException(source.file(), e);
        }
    }

    private static void closeAfterRejection(PDDocument document) {
        try {
            document.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal menutup dokumen yang ditolak", e);
        }
    }

    /** Proteksi asli dokumen; panggil tepat setelah {@link #load(PdfSource)}. */
    public Optional<PdfProtection> protectionOf(PDDocument document, PdfSource source) {
        return PdfProtection.of(document, source);
    }

    /**
     * Simpan dokumen. {@link OutputProtection#KEEP} meng-apply ulang {@code original}; {@link OutputProtection#REMOVE}
     * menyimpan tanpa password (hanya setelah user konfirmasi di UI).
     */
    public void save(PDDocument document, Path output, Optional<PdfProtection> original, OutputProtection mode) {
        Optional<PdfProtection> protection = mode == OutputProtection.KEEP ? original : Optional.empty();
        try {
            if (protection.isPresent()) {
                protection.get().applyTo(document);
            } else {
                document.setAllSecurityToBeRemoved(true);
            }
            document.save(output.toFile());
        } catch (IOException e) {
            throw new PdfIoException(output, e);
        }
    }

    /** Ukuran file dalam byte. */
    public long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new PdfIoException(file, e);
        }
    }

    private static byte[] readBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new PdfIoException(file, e);
        }
    }
}
