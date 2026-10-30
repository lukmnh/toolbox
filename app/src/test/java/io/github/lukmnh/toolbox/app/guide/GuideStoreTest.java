package io.github.lukmnh.toolbox.app.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuideStoreTest {

    @TempDir
    Path directory;

    @Test
    void bundledGuideIsUsedUntilUserEdits() throws Exception {
        Path file = directory.resolve("guide.md");

        String guide = new GuideStore(file).load();

        assertTrue(guide.startsWith("# Toolbox Guide"), guide);
        assertFalse(Files.exists(file));
    }

    @Test
    void savedEditsAreLoadedBack() throws Exception {
        Path file = directory.resolve("guide.md");
        new GuideStore(file).save("# Mine\n\nÜnïcödé ✓");

        assertEquals("# Mine\n\nÜnïcödé ✓", new GuideStore(file).load());
    }

    @Test
    void resetDeletesEditsAndReturnsBundledGuide() throws Exception {
        Path file = directory.resolve("guide.md");
        GuideStore store = new GuideStore(file);
        store.save("# Mine");

        String guide = store.reset();

        assertEquals(GuideStore.defaultGuide(), guide);
        assertFalse(Files.exists(file));
        assertEquals(GuideStore.defaultGuide(), store.load());
    }
}
