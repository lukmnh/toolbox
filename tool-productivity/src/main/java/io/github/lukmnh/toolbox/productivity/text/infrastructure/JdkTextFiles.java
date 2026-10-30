package io.github.lukmnh.toolbox.productivity.text.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.productivity.text.domain.LoadedText;
import io.github.lukmnh.toolbox.productivity.text.domain.TextFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.text.domain.TextFiles;
import io.github.lukmnh.toolbox.productivity.text.domain.TextIoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Baca/tulis file teks UTF-8 dengan batas ukuran (Security Requirements #3). */
public final class JdkTextFiles implements TextFiles {

    private static final int BOM_LENGTH = 3;
    private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n?");
    private static final String NEW_LINE = "\n";

    private final long maxBytes;

    public JdkTextFiles() {
        this(SecurityLimits.TEXT_TOOLS_MAX_BYTES);
    }

    /** @param maxBytes batas ukuran; konstruktor ini untuk test */
    JdkTextFiles(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public LoadedText read(Path file) {
        String name = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size > maxBytes) {
                throw new TextFileTooLargeException(name, size, maxBytes);
            }
            byte[] bytes = readCapped(file, name, size);
            return new LoadedText(name, LINE_BREAKS.matcher(decode(bytes)).replaceAll(NEW_LINE));
        } catch (IOException e) {
            throw new TextIoException(name, e);
        }
    }

    /** File bisa membesar setelah dicek; baca maksimal batas + 1 byte supaya tetap aman. */
    private byte[] readCapped(Path file, String name, long size) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int) maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new TextFileTooLargeException(name, Math.max(size, bytes.length), maxBytes);
            }
            return bytes;
        }
    }

    /** Byte UTF-8 rusak diganti U+FFFD supaya file berencoding campuran tetap bisa dibuka. */
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
            throw new TextIoException(target.getFileName().toString(), e);
        }
    }
}
