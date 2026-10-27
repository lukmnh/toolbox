package io.github.lukmnh.toolbox.common.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UniqueFileNamerTest {

    @TempDir
    Path folder;

    @Test
    void freeNameIsUsedAsIs() {
        assertEquals(folder.resolve("hasil.pdf"), UniqueFileNamer.uniqueFile(folder, "hasil.pdf"));
    }

    @Test
    void takenNamesGetIncrementingCounterBeforeExtension() throws IOException {
        Files.createFile(folder.resolve("hasil.pdf"));
        Files.createFile(folder.resolve("hasil (1).pdf"));

        assertEquals(folder.resolve("hasil (2).pdf"), UniqueFileNamer.uniqueFile(folder, "hasil.pdf"));
    }

    @Test
    void nameWithoutExtensionGetsCounterAtTheEnd() throws IOException {
        Files.createFile(folder.resolve("SHA256SUMS"));

        assertEquals(folder.resolve("SHA256SUMS (1)"), UniqueFileNamer.uniqueFile(folder, "SHA256SUMS"));
    }

    @Test
    void onlyLastDotIsTreatedAsExtension() throws IOException {
        Files.createFile(folder.resolve("data.pretty.json"));

        assertEquals(folder.resolve("data.pretty (1).json"), UniqueFileNamer.uniqueFile(folder, "data.pretty.json"));
    }

    @Test
    void directoryNamesKeepDotsAndGetCounter() throws IOException {
        Files.createDirectory(folder.resolve("laporan.v2_split"));

        assertEquals(
                folder.resolve("laporan.v2_split (1)"), UniqueFileNamer.uniqueDirectory(folder, "laporan.v2_split"));
    }
}
