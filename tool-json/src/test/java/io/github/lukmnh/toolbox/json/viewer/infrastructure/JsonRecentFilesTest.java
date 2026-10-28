package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class JsonRecentFilesTest {

    @TempDir
    Path dataDirectory;

    @Test
    void missingFileMeansEmptyList() {
        assertTrue(new JsonRecentFiles(dataDirectory).load().isEmpty());
    }

    @Test
    void savesAndLoadsAbsolutePaths() {
        JsonRecentFiles recent = new JsonRecentFiles(dataDirectory);
        List<Path> files = List.of(dataDirectory.resolve("a.json"), dataDirectory.resolve("b.jsonl"));

        recent.save(files);

        assertEquals(files, recent.load());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void savedRecentFileIsOwnerOnly() throws IOException {
        new JsonRecentFiles(dataDirectory).save(List.of(dataDirectory.resolve("a.json")));

        Path saved = dataDirectory.resolve("recent.json");
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(saved)));
    }

    @Test
    void otherKeysInRecentJsonArePreserved() throws IOException {
        Path file = dataDirectory.resolve("recent.json");
        Files.writeString(file, "{\"tools\": [\"pdf.sign\"], \"jsonFiles\": []}");

        new JsonRecentFiles(dataDirectory).save(List.of(dataDirectory.resolve("a.json")));

        String content = Files.readString(file);
        assertTrue(content.contains("\"tools\": [\n    \"pdf.sign\"\n  ]"), content);
        assertTrue(content.contains("a.json"), content);
    }

    @Test
    void corruptFileIsIgnoredAndOverwritten() throws IOException {
        Files.writeString(dataDirectory.resolve("recent.json"), "{rusak");
        JsonRecentFiles recent = new JsonRecentFiles(dataDirectory);

        assertTrue(recent.load().isEmpty());
        recent.save(List.of(dataDirectory.resolve("a.json")));
        assertEquals(1, recent.load().size());
    }
}
