package io.github.lukmnh.toolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SafePathsTest {

    @TempDir
    Path base;

    @Test
    void relativeNameInsideBaseIsResolved() {
        assertEquals(
                base.resolve("sub/a.txt"),
                SafePaths.resolveInside(base, "sub/a.txt").orElseThrow());
        assertEquals(
                base.resolve("b.txt"),
                SafePaths.resolveInside(base, "./x/../b.txt").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"../../etc/passwd", "../a.txt", "/etc/passwd", "\\windows\\system32", "C:\\x.txt", "", " ", "."})
    void escapingOrAbsoluteNamesAreRejected(String name) {
        assertTrue(SafePaths.resolveInside(base, name).isEmpty());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void symlinkPointingOutsideIsRejected(@TempDir Path outside) throws IOException {
        Path secret = Files.writeString(outside.resolve("secret.txt"), "x");
        Files.createSymbolicLink(base.resolve("link.txt"), secret);
        assertTrue(SafePaths.resolveInside(base, "link.txt").isEmpty());
    }
}
