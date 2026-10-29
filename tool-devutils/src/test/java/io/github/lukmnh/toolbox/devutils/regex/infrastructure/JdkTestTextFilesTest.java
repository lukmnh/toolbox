package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.regex.domain.LoadedTestText;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexIoException;
import io.github.lukmnh.toolbox.devutils.regex.domain.TestTextTooLargeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkTestTextFilesTest {

    private static final long SMALL_LIMIT = 32;

    @TempDir
    Path directory;

    private final JdkTestTextFiles files = new JdkTestTextFiles(SMALL_LIMIT);

    @Test
    void readsUtf8WithoutBom() throws IOException {
        byte[] body = "log é\n".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[body.length + 3];
        content[0] = (byte) 0xEF;
        content[1] = (byte) 0xBB;
        content[2] = (byte) 0xBF;
        System.arraycopy(body, 0, content, 3, body.length);
        Path file = Files.write(directory.resolve("app.log"), content);

        LoadedTestText loaded = files.read(file);
        assertEquals("app.log", loaded.fileName());
        assertEquals("log é\n", loaded.text());
        assertEquals(content.length, loaded.sizeBytes());
    }

    @Test
    void tooLargeFileIsRejected() throws IOException {
        Path file = Files.writeString(directory.resolve("big.txt"), "x".repeat((int) SMALL_LIMIT + 1));
        TestTextTooLargeException error = assertThrows(TestTextTooLargeException.class, () -> files.read(file));
        assertEquals("big.txt", error.fileName());
        assertEquals(SMALL_LIMIT, error.maxBytes());
    }

    @Test
    void missingFileIsIoError() {
        RegexIoException error =
                assertThrows(RegexIoException.class, () -> files.read(directory.resolve("hilang.txt")));
        assertEquals("hilang.txt", error.fileName());
    }
}
