package io.github.lukmnh.toolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.LINUX)
class OwnerOnlyFilesTest {

    @TempDir
    Path temp;

    @Test
    void fileBecomesReadableByOwnerOnly() throws IOException {
        Path file = Files.writeString(temp.resolve("signature.png"), "x");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));

        OwnerOnlyFiles.restrict(file);

        assertEquals("rw-------", permissions(file));
    }

    @Test
    void createdDirectoryIsOwnerOnly() throws IOException {
        Path directory =
                OwnerOnlyFiles.createDirectories(temp.resolve(".toolbox").resolve("tmp"));

        assertEquals("rwx------", permissions(directory));
    }

    private static String permissions(Path path) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
    }
}
