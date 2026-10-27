package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;

/** Menyimpan tanda tangan terakhir sebagai PNG di folder data aplikasi. */
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
            return Optional.ofNullable(ImageIO.read(signatureFile.toFile()));
        } catch (IOException e) {
            // File rusak tidak menghalangi tool; user cukup membuat tanda tangan baru
            LOG.log(Level.WARNING, "Gagal membaca " + signatureFile, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveLast(BufferedImage signature) {
        try {
            AtomicFiles.write(signatureFile, file -> writePng(signature, file));
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
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                throw new IOException("Format gambar tidak didukung");
            }
            return image;
        } catch (IOException e) {
            throw new PdfIoException(file, e);
        }
    }
}
