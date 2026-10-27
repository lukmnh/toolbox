package io.github.lukmnh.toolbox.common.settings;

import java.nio.file.Path;
import java.util.Objects;

/** Pengaturan user. Immutable; ubah dengan method {@code with...}. */
public record Settings(Path downloadDirectory, boolean alwaysAskLocation, Theme theme) {

    public Settings {
        Objects.requireNonNull(downloadDirectory, "downloadDirectory");
        Objects.requireNonNull(theme, "theme");
    }

    public static Settings defaults(Path downloadDirectory) {
        return new Settings(downloadDirectory, false, Theme.SYSTEM);
    }

    public Settings withDownloadDirectory(Path directory) {
        return new Settings(directory, alwaysAskLocation, theme);
    }

    public Settings withAlwaysAskLocation(boolean alwaysAsk) {
        return new Settings(downloadDirectory, alwaysAsk, theme);
    }

    public Settings withTheme(Theme newTheme) {
        return new Settings(downloadDirectory, alwaysAskLocation, newTheme);
    }
}
