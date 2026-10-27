package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

/** Membuka dan menyimpan PDF dengan aturan umum tool PDF. */
public final class PdfLoader {

    /**
     * Load dari bytes di memory (bukan dari file) supaya file asli tidak ter-lock di Windows selama
     * dokumen terbuka. Pemanggil wajib menutup dokumen (try-with-resources).
     */
    public PDDocument load(PdfSource source) {
        byte[] bytes = readBytes(source.file());
        try {
            return Loader.loadPDF(bytes, source.password());
        } catch (InvalidPasswordException e) {
            throw new PasswordRequiredException(
                    source.file(), !source.password().isEmpty(), e);
        } catch (IOException e) {
            throw new CorruptPdfException(source.file(), e);
        }
    }

    /** Simpan tanpa enkripsi: hasil dari PDF berpassword tidak lagi butuh password. */
    public void save(PDDocument document, Path output) {
        document.setAllSecurityToBeRemoved(true);
        try {
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
