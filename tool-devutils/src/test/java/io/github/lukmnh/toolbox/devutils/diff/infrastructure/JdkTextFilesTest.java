package io.github.lukmnh.toolbox.devutils.diff.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.diff.domain.BinaryFileException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffIoException;
import io.github.lukmnh.toolbox.devutils.diff.domain.LoadedText;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkTextFilesTest {

    private static final long SMALL_LIMIT = 64;

    @TempDir
    Path directory;

    private final JdkTextFiles files = new JdkTextFiles(SMALL_LIMIT);

    @Test
    void readsUtf8AndStripsBom() throws IOException {
        Path file = directory.resolve("a.txt");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "héllo\n".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, content, 0, bom.length);
        System.arraycopy(body, 0, content, bom.length, body.length);
        Files.write(file, content);

        LoadedText loaded = files.read(file);
        assertEquals("a.txt", loaded.fileName());
        assertEquals("héllo\n", loaded.text());
    }

    @Test
    void binaryFileIsRejected() throws IOException {
        Path file = Files.write(directory.resolve("img.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 0});
        assertThrows(BinaryFileException.class, () -> files.read(file));
    }

    @Test
    void fileOverLimitIsRejected() throws IOException {
        Path file = Files.writeString(directory.resolve("big.txt"), "x".repeat((int) SMALL_LIMIT + 1));
        DiffFileTooLargeException error = assertThrows(DiffFileTooLargeException.class, () -> files.read(file));
        assertEquals(SMALL_LIMIT, error.maxBytes());
    }

    @Test
    void missingFileIsIoError() {
        assertThrows(DiffIoException.class, () -> files.read(directory.resolve("tidak-ada.txt")));
    }

    @Test
    void writesUtf8() throws IOException {
        Path target = directory.resolve("out.diff");
        files.write(target, "+é\n");
        assertEquals("+é\n", Files.readString(target, StandardCharsets.UTF_8));
    }
}
