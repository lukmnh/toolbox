package io.github.lukmnh.toolbox.devutils.diff.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.devutils.diff.domain.BinaryDetector;
import io.github.lukmnh.toolbox.devutils.diff.domain.BinaryFileException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffIoException;
import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Baca/tulis file teks UTF-8 dengan batas ukuran dan deteksi biner. */
public final class JdkTextFiles implements TextFiles {

    private static final int BOM_LENGTH = 3;

    private final long maxBytes;

    public JdkTextFiles() {
        this(SecurityLimits.DIFF_MAX_FILE_BYTES);
    }

    /** @param maxBytes batas ukuran per file; konstruktor ini untuk test */
    JdkTextFiles(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public LoadedText read(Path file) {
        String name = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size > maxBytes) {
                throw new DiffFileTooLargeException(name, size, maxBytes);
            }
            byte[] bytes = readCapped(file, name, size);
            if (BinaryDetector.isBinary(bytes, bytes.length)) {
                throw new BinaryFileException(name);
            }
            return new LoadedText(name, decode(bytes), bytes.length);
        } catch (IOException e) {
            throw new DiffIoException(name, e);
        }
    }

    /** File bisa membesar setelah dicek ukurannya; baca maksimal batas + 1 byte supaya tetap aman. */
    private byte[] readCapped(Path file, String name, long size) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int) maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new DiffFileTooLargeException(name, Math.max(size, bytes.length), maxBytes);
            }
            return bytes;
        }
    }

    /** Byte UTF-8 yang rusak diganti U+FFFD; Text Diff tetap jalan untuk file berencoding campuran. */
    private static String decode(byte[] bytes) {
        boolean bom = bytes.length >= BOM_LENGTH
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
        int offset = bom ? BOM_LENGTH : 0;
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
    }

    @Override
    public void write(Path target, String content) {
        try {
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DiffIoException(target.getFileName().toString(), e);
        }
    }
}
