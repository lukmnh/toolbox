package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.devutils.regex.domain.LoadedTestText;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexIoException;
import io.github.lukmnh.toolbox.devutils.regex.domain.TestTextFiles;
import io.github.lukmnh.toolbox.devutils.regex.domain.TestTextTooLargeException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Baca teks uji UTF-8 dengan batas ukuran (Security Requirements #3). */
public final class JdkTestTextFiles implements TestTextFiles {

    private static final int BOM_LENGTH = 3;

    private final long maxBytes;

    public JdkTestTextFiles() {
        this(SecurityLimits.REGEX_TEST_MAX_FILE_BYTES);
    }

    /** @param maxBytes batas ukuran; konstruktor ini untuk test */
    JdkTestTextFiles(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public LoadedTestText read(Path file) {
        String name = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size > maxBytes) {
                throw new TestTextTooLargeException(name, size, maxBytes);
            }
            byte[] bytes = readCapped(file, name, size);
            return new LoadedTestText(name, decode(bytes), bytes.length);
        } catch (IOException e) {
            throw new RegexIoException(name, e);
        }
    }

    /** File bisa membesar setelah dicek; baca maksimal batas + 1 byte supaya tetap aman. */
    private byte[] readCapped(Path file, String name, long size) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int) maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new TestTextTooLargeException(name, Math.max(size, bytes.length), maxBytes);
            }
            return bytes;
        }
    }

    /** Byte UTF-8 rusak diganti U+FFFD supaya file log berencoding campuran tetap bisa diuji. */
    private static String decode(byte[] bytes) {
        boolean bom = bytes.length >= BOM_LENGTH
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
        int offset = bom ? BOM_LENGTH : 0;
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
    }
}
