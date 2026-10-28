package io.github.lukmnh.toolbox.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempFilesTest {

    @TempDir
    Path temp;

    @Test
    void tempFilesAreCreatedInsideGivenRootAndDeletedOnClose() {
        Path root = temp.resolve("tmp");
        Path file;
        Path directory;
        try (TempFiles tempFiles = new TempFiles(root)) {
            file = tempFiles.createFile(".pdf");
            directory = tempFiles.createDirectory();
            assertEquals(root, file.getParent());
            assertEquals(root, directory.getParent());
        }

        assertFalse(Files.exists(file));
        assertFalse(Files.exists(directory));
    }

    @Test
    void purgeAtStartupRemovesLeftoversFromCrash() throws IOException {
        Path root = Files.createDirectory(temp.resolve("tmp"));
        Files.writeString(root.resolve("toolbox-sisa.pdf"), "sisa crash");
        Path leftoverDirectory = Files.createDirectory(root.resolve("toolbox-split"));
        Files.writeString(leftoverDirectory.resolve("hal1.pdf"), "x");

        TempFiles.purge(root);

        assertTrue(Files.isDirectory(root));
        try (Stream<Path> entries = Files.list(root)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void purgeOfMissingDirectoryDoesNothing() {
        TempFiles.purge(temp.resolve("tidak-ada"));

        assertFalse(Files.exists(temp.resolve("tidak-ada")));
    }
}
