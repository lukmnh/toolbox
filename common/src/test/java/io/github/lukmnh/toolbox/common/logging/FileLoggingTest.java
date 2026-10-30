package io.github.lukmnh.toolbox.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileLoggingTest {

    private static final Path FALLBACK = AppDirectories.dataDirectory().resolve("logs");

    @Test
    void linuxLauncherInBinResolvesToAppRoot() {
        Path root = Path.of("opt", "Toolbox").toAbsolutePath();

        assertEquals(root, FileLogging.appRoot(root.resolve("bin").resolve("Toolbox")));
    }

    @Test
    void windowsLauncherResolvesToItsFolder() {
        Path root = Path.of("Toolbox").toAbsolutePath();

        assertEquals(root, FileLogging.appRoot(root.resolve("Toolbox.exe")));
    }

    @Test
    void appFolderComesBeforeHomeFallback() {
        Path root = Path.of("Toolbox").toAbsolutePath();

        assertEquals(
                List.of(root.resolve("logs"), FALLBACK),
                FileLogging.candidates(root.resolve("Toolbox.exe").toString()));
    }

    @Test
    void withoutJpackageOnlyHomeFallback() {
        assertEquals(List.of(FALLBACK), FileLogging.candidates(null));
        assertEquals(List.of(FALLBACK), FileLogging.candidates(" "));
    }
}
