package io.github.lukmnh.toolbox.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFilesTest {

    @TempDir
    Path folder;

    @Test
    void copyKeepsSourceAndReplacesTarget() throws IOException {
        Path source = Files.writeString(folder.resolve("sumber.txt"), "baru");
        Path target = Files.writeString(folder.resolve("tujuan.txt"), "lama");

        AtomicFiles.copy(source, target);

        assertEquals("baru", Files.readString(target));
        assertTrue(Files.exists(source), "sumber tetap ada untuk download ulang");
        assertNoTempLeft();
    }

    @Test
    void copyCreatesMissingParentFolders() throws IOException {
        Path source = Files.writeString(folder.resolve("a.txt"), "isi");
        Path target = folder.resolve("x").resolve("y").resolve("a.txt");

        AtomicFiles.copy(source, target);

        assertEquals("isi", Files.readString(target));
    }

    @Test
    void failedWriteLeavesOriginalUntouchedAndCleansTemp() throws IOException {
        Path target = Files.writeString(folder.resolve("data.txt"), "asli");

        assertThrows(
                IOException.class,
                () -> AtomicFiles.write(target, file -> {
                    Files.writeString(file, "setengah");
                    throw new IOException("disk penuh");
                }));

        assertEquals("asli", Files.readString(target));
        assertNoTempLeft();
    }

    @Test
    void writeReplacesTargetAtomically() throws IOException {
        Path target = Files.writeString(folder.resolve("data.txt"), "lama");

        AtomicFiles.write(target, file -> Files.writeString(file, "baru"));

        assertEquals("baru", Files.readString(target));
        assertNoTempLeft();
    }

    private void assertNoTempLeft() throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
