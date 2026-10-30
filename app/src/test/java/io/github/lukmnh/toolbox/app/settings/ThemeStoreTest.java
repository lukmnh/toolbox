package io.github.lukmnh.toolbox.app.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.lukmnh.toolbox.common.settings.Theme;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThemeStoreTest {

    @TempDir
    Path directory;

    @Test
    void defaultsToLightWhenNothingSaved() {
        assertEquals(Theme.LIGHT, new ThemeStore(directory.resolve("settings.properties")).load());
    }

    @Test
    void savedThemeSurvivesNewInstance() throws Exception {
        Path file = directory.resolve("nested/settings.properties");
        new ThemeStore(file).save(Theme.DARK);

        assertEquals(Theme.DARK, new ThemeStore(file).load());
    }

    @Test
    void unknownValueFallsBackToLight() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.writeString(file, "theme=SEPIA\n");

        assertEquals(Theme.LIGHT, new ThemeStore(file).load());
    }
}
