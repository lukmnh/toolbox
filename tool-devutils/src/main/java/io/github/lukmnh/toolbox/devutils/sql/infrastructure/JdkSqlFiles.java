package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.devutils.sql.domain.LoadedSql;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFiles;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlIoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Baca/tulis file SQL UTF-8 dengan batas ukuran (Security Requirements #3). */
public final class JdkSqlFiles implements SqlFiles {

    private static final int BOM_LENGTH = 3;

    private final long maxBytes;

    public JdkSqlFiles() {
        this(SecurityLimits.SQL_MAX_FILE_BYTES);
    }

    /** @param maxBytes batas ukuran; konstruktor ini untuk test */
    JdkSqlFiles(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public LoadedSql read(Path file) {
        String name = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size > maxBytes) {
                throw new SqlFileTooLargeException(name, size, maxBytes);
            }
            byte[] bytes = readCapped(file, name, size);
            return new LoadedSql(name, decode(bytes), bytes.length);
        } catch (IOException e) {
            throw new SqlIoException(name, e);
        }
    }

    /** File bisa membesar setelah dicek; baca maksimal batas + 1 byte supaya tetap aman. */
    private byte[] readCapped(Path file, String name, long size) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int) maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new SqlFileTooLargeException(name, Math.max(size, bytes.length), maxBytes);
            }
            return bytes;
        }
    }

    /** Byte UTF-8 rusak diganti U+FFFD; dump lama berencoding campuran tetap bisa di-format. */
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
            throw new SqlIoException(target.getFileName().toString(), e);
        }
    }
}
