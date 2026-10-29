package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.sql.domain.LoadedSql;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlIoException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkSqlFilesTest {

    private static final long SMALL_LIMIT = 32;

    @TempDir
    Path directory;

    private final JdkSqlFiles files = new JdkSqlFiles(SMALL_LIMIT);

    @Test
    void readsUtf8WithoutBom() throws IOException {
        byte[] body = "select 'é';".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[body.length + 3];
        content[0] = (byte) 0xEF;
        content[1] = (byte) 0xBB;
        content[2] = (byte) 0xBF;
        System.arraycopy(body, 0, content, 3, body.length);
        Path file = Files.write(directory.resolve("query.sql"), content);

        LoadedSql loaded = files.read(file);
        assertEquals("query.sql", loaded.fileName());
        assertEquals("select 'é';", loaded.text());
        assertEquals(content.length, loaded.sizeBytes());
    }

    @Test
    void tooLargeFileIsRejected() throws IOException {
        Path file = Files.writeString(directory.resolve("dump.sql"), "x".repeat((int) SMALL_LIMIT + 1));
        SqlFileTooLargeException error = assertThrows(SqlFileTooLargeException.class, () -> files.read(file));
        assertEquals("dump.sql", error.fileName());
        assertEquals(SMALL_LIMIT, error.maxBytes());
    }

    @Test
    void missingFileIsIoError() {
        SqlIoException error = assertThrows(SqlIoException.class, () -> files.read(directory.resolve("hilang.sql")));
        assertEquals("hilang.sql", error.fileName());
    }

    @Test
    void writesUtf8() throws IOException {
        Path target = directory.resolve("out.sql");
        files.write(target, "select 'ü';");
        assertEquals("select 'ü';", Files.readString(target, StandardCharsets.UTF_8));
    }
}
