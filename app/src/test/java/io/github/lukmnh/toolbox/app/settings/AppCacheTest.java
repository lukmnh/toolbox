package io.github.lukmnh.toolbox.app.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppCacheTest {

    @TempDir
    Path data;

    private Path tempFile;
    private Path activeLog;
    private Path rotatedLog;
    private Path jsonRecent;
    private Path toolUsage;
    private AppCache cache;

    @BeforeEach
    void setUp() throws Exception {
        Path temp = Files.createDirectories(data.resolve("tmp/nested"));
        Path logs = Files.createDirectories(data.resolve("logs"));
        tempFile = Files.write(temp.resolve("a.bin"), new byte[10]);
        activeLog = Files.write(logs.resolve("toolbox-0.log"), new byte[100]);
        rotatedLog = Files.write(logs.resolve("toolbox-1.log"), new byte[20]);
        jsonRecent = Files.write(data.resolve("recent.json"), new byte[5]);
        toolUsage = Files.write(data.resolve("tool-usage.properties"), new byte[7]);
        cache = new AppCache(data, data.resolve("tmp"), Optional.of(logs));
    }

    @Test
    void sizeCountsOnlyClearableFiles() {
        assertEquals(35, cache.size());
    }

    @Test
    void clearKeepsActiveLogAndToolUsage() {
        AppCache.Result result = cache.clear();

        assertEquals(new AppCache.Result(35, 0), result);
        assertFalse(Files.exists(tempFile));
        assertFalse(Files.exists(rotatedLog));
        assertFalse(Files.exists(jsonRecent));
        assertTrue(Files.exists(activeLog));
        assertTrue(Files.exists(toolUsage));
    }

    @Test
    void missingDirectoriesAreEmpty() {
        AppCache empty = new AppCache(data.resolve("none"), data.resolve("none/tmp"), Optional.empty());

        assertEquals(new AppCache.Result(0, 0), empty.clear());
    }
}
