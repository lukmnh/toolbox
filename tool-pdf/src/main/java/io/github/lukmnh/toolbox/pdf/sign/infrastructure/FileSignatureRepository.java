package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.security.OwnerOnlyFiles;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException.Limit;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Menyimpan tanda tangan terakhir sebagai PNG owner-only di folder data aplikasi (Security Requirements #1). */
public final class FileSignatureRepository implements SignatureRepository {

    private static final System.Logger LOG = System.getLogger(FileSignatureRepository.class.getName());
    private static final String FILE_NAME = "signature.png";
    private static final String PNG = "png";

    private final Path signatureFile;

    public FileSignatureRepository(Path dataDirectory) {
        this.signatureFile = dataDirectory.resolve(FILE_NAME);
    }

    @Override
    public Optional<BufferedImage> loadLast() {
        if (!Files.isRegularFile(signatureFile)) {
            return Optional.empty();
        }
        try {
            return Optional.of(decodeSafely(signatureFile));
        } catch (IOException e) {
            // File rusak tidak menghalangi tool; user cukup membuat tanda tangan baru
            LOG.log(Level.WARNING, "Gagal membaca tanda tangan tersimpan", e);
            return Optional.empty();
        }
    }

    @Override
    public void saveLast(BufferedImage signature) {
        try {
            AtomicFiles.write(signatureFile, file -> {
                writePng(signature, file);
                // Dibatasi sebelum di-move, supaya file final tidak pernah sempat terbaca user lain
                OwnerOnlyFiles.restrict(file);
            });
        } catch (IOException e) {
            throw new PdfIoException(signatureFile, e);
        }
    }

    @Override
    public void deleteLast() {
        try {
            Files.deleteIfExists(signatureFile);
        } catch (IOException e) {
            throw new PdfIoException(signatureFile, e);
        }
    }

    private static void writePng(BufferedImage image, Path file) throws IOException {
        if (!ImageIO.write(image, PNG, file.toFile())) {
            throw new IOException("Writer PNG tidak tersedia");
        }
    }

    @Override
    public BufferedImage readImage(Path file) {
        try {
            return decodeSafely(file);
        } catch (IOException e) {
            throw new PdfIoException(file, e);
        }
    }

    /**
     * Ukuran file dan dimensi dari header dicek <b>sebelum</b> decode, supaya gambar kecil berheader 50.000×50.000
     * px (decompression bomb) tidak sempat mengalokasikan pixel buffer raksasa (Security Requirements #3).
     */
    static BufferedImage decodeSafely(Path file) throws IOException {
        long size = Files.size(file);
        if (size > SecurityLimits.IMAGE_MAX_FILE_BYTES) {
            throw new PdfLimitExceededException(file, Limit.FILE_SIZE, size, SecurityLimits.IMAGE_MAX_FILE_BYTES);
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                throw new IOException("Gambar tidak bisa dibuka");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Format gambar tidak didukung");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int longestSide = Math.max(reader.getWidth(0), reader.getHeight(0));
                if (longestSide > SecurityLimits.IMAGE_MAX_DIMENSION) {
                    throw new PdfLimitExceededException(
                            file, Limit.IMAGE_DIMENSION, longestSide, SecurityLimits.IMAGE_MAX_DIMENSION);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
