package io.github.lukmnh.toolbox.app.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolUsageStoreTest {

    @TempDir
    Path directory;

    @Test
    void emptyWhenNothingSaved() {
        assertEquals(ToolUsage.EMPTY, new ToolUsageStore(directory.resolve("tool-usage.properties")).load());
    }

    @Test
    void savedUsageSurvivesNewInstance() throws Exception {
        Path file = directory.resolve("nested/tool-usage.properties");
        ToolUsage usage = new ToolUsage(List.of("pdf.sign", "json.viewer"), List.of("devutils.jwt"));
        new ToolUsageStore(file).save(usage);

        assertEquals(usage, new ToolUsageStore(file).load());
    }

    @Test
    void blankEntriesAreIgnored() throws Exception {
        Path file = directory.resolve("tool-usage.properties");
        Files.writeString(file, "favorites= , ,a\nrecent=\n");

        assertEquals(new ToolUsage(List.of("a"), List.of()), new ToolUsageStore(file).load());
    }
}
