package io.github.lukmnh.toolbox.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FileExtensionsTest {

    @Test
    void extensionIsLowercaseWithoutDot() {
        assertEquals("pdf", FileExtensions.extensionOf(Path.of("Laporan.PDF")));
        assertEquals("json", FileExtensions.extensionOf(Path.of("data.pretty.json")));
    }

    @Test
    void hiddenFilesAndNamesWithoutExtensionHaveNone() {
        assertEquals("", FileExtensions.extensionOf(Path.of(".bashrc")));
        assertEquals("", FileExtensions.extensionOf(Path.of("SHA256SUMS")));
        assertEquals("", FileExtensions.extensionOf(Path.of("akhir.")));
    }

    @Test
    void hasExtensionIsCaseInsensitive() {
        assertTrue(FileExtensions.hasExtension(Path.of("A.Pdf"), Set.of("pdf")));
        assertFalse(FileExtensions.hasExtension(Path.of("a.jsonl"), Set.of("json")));
    }

    @Test
    void baseNameDropsOnlyLastExtension() {
        assertEquals("laporan.final", FileExtensions.baseName(Path.of("/tmp/laporan.final.pdf")));
        assertEquals("README", FileExtensions.baseName(Path.of("README")));
    }
}
