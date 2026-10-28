package io.github.lukmnh.toolbox.devutils.encoder.infrastructure;

import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Files;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderIoException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileEncodeOptions;
import io.github.lukmnh.toolbox.devutils.encoder.domain.FileType;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidBase64Exception;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ProgressListener;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextPreview;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Adapter filesystem mode File: encode/decode streaming dengan buffer tetap, aman untuk file berukuran GB. */
public final class JdkBase64Files implements Base64Files {

    private static final System.Logger LOG = System.getLogger(JdkBase64Files.class.getName());
    private static final int BUFFER_SIZE = 64 * 1024;

    @Override
    public long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new EncoderIoException(file, e);
        }
    }

    @Override
    public EncodedFile encode(
            Path source, Path target, FileEncodeOptions options, String dataUriPrefix, ProgressListener progress) {
        long total = size(source);
        Base64.Encoder encoder = options.effectiveVariant().encoder(options.effectivePadding());
        try (InputStream input = Files.newInputStream(source);
                OutputStream file = new BufferedOutputStream(Files.newOutputStream(target), BUFFER_SIZE);
                OutputStream encoding = encoder.wrap(withPrefix(file, dataUriPrefix))) {
            copy(input, encoding, total, progress);
        } catch (IOException e) {
            throw new EncoderIoException(source, e);
        }
        return new EncodedFile(target, total, size(target));
    }

    private static OutputStream withPrefix(OutputStream file, String prefix) throws IOException {
        file.write(prefix.getBytes(StandardCharsets.US_ASCII));
        return file;
    }

    private static void copy(InputStream input, OutputStream output, long total, ProgressListener progress)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long done = 0;
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
            done += count;
            progress.advanced(done, total);
        }
    }

    @Override
    public DecodedFile decode(Path source, Path target, ProgressListener progress) {
        long total = size(source);
        try (Base64CleaningInputStream cleaned = new Base64CleaningInputStream(Files.newInputStream(source));
                InputStream decoded = Base64.getDecoder().wrap(cleaned);
                OutputStream output = new BufferedOutputStream(Files.newOutputStream(target), BUFFER_SIZE)) {
            byte[] header = new byte[FileType.HEADER_BYTES];
            int headerLength = 0;
            long written = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = readDecoded(decoded, buffer, cleaned)) >= 0) {
                int headerPart = Math.min(count, header.length - headerLength);
                System.arraycopy(buffer, 0, header, headerLength, headerPart);
                headerLength += headerPart;
                output.write(buffer, 0, count);
                written += count;
                progress.advanced(cleaned.position(), total);
            }
            return new DecodedFile(target, written, FileType.detect(Arrays.copyOf(header, headerLength)));
        } catch (IOException e) {
            throw new EncoderIoException(source, e);
        }
    }

    /** Decoder JDK melempar {@link IOException} generik untuk sisa 1 karakter; ubah ke error domain berposisi. */
    private static int readDecoded(InputStream decoded, byte[] buffer, Base64CleaningInputStream cleaned)
            throws IOException {
        try {
            return decoded.read(buffer);
        } catch (IOException e) {
            if (cleaned.endsWithDanglingCharacter()) {
                throw new InvalidBase64Exception(
                        InvalidBase64Exception.Reason.TRUNCATED, cleaned.lastDataPosition(), -1);
            }
            throw e;
        }
    }

    @Override
    public void writeBytes(Path target, byte[] content) {
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new EncoderIoException(target, e);
        }
    }

    @Override
    public TextPreview readPreview(Path file, int maxBytes) {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] head = input.readNBytes(maxBytes);
            boolean truncated = input.read() >= 0;
            return new TextPreview(new String(head, StandardCharsets.ISO_8859_1), truncated);
        } catch (IOException e) {
            throw new EncoderIoException(file, e);
        }
    }

    @Override
    public String readText(Path file) {
        try {
            return Files.readString(file, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new EncoderIoException(file, e);
        }
    }

    @Override
    public Optional<String> probeContentType(Path file) {
        try {
            return Optional.ofNullable(Files.probeContentType(file));
        } catch (IOException e) {
            // Deteksi OS hanya pelengkap; pemanggil punya fallback dari ekstensi
            LOG.log(Level.WARNING, "Gagal mendeteksi MIME type " + file.getFileName(), e);
            return Optional.empty();
        }
    }

    /** Hanya membaca header gambar; piksel tidak di-decode (cegah decompression bomb, #3). */
    @Override
    public Optional<ImageSize> imageSize(Path file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            return Optional.of(readSize(readers.next(), input));
        } catch (IOException | IllegalArgumentException e) {
            // Header rusak: preview dilewati, file hasil decode tetap bisa di-download
            LOG.log(Level.WARNING, "Gagal membaca dimensi gambar " + file.getFileName(), e);
            return Optional.empty();
        }
    }

    private static ImageSize readSize(ImageReader reader, ImageInputStream input) throws IOException {
        try {
            reader.setInput(input, true, true);
            return new ImageSize(reader.getWidth(0), reader.getHeight(0));
        } finally {
            reader.dispose();
        }
    }
}
