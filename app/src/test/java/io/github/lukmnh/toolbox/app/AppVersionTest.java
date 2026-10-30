package io.github.lukmnh.toolbox.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AppVersionTest {

    @Test
    void currentVersionComesFromFilteredResource() {
        String version = AppVersion.current();

        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"), version);
    }

    @Test
    void parseReadsVersionKey() throws Exception {
        assertEquals("2.3.4", AppVersion.parse(stream("version=2.3.4\n")));
    }

    @Test
    void unfilteredPlaceholderIsRejected() {
        assertThrows(IllegalStateException.class, () -> AppVersion.parse(stream("version=${project.version}\n")));
    }

    @Test
    void missingVersionIsRejected() {
        assertThrows(IllegalStateException.class, () -> AppVersion.parse(stream("name=toolbox\n")));
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
    }
}
