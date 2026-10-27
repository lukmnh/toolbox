package io.github.lukmnh.toolbox.common.io;

import java.nio.file.Path;

/** Lokasi data aplikasi di folder home user. */
public final class AppDirectories {

    private static final String DATA_DIRECTORY_NAME = ".toolbox";

    private AppDirectories() {}

    public static Path userHome() {
        return Path.of(System.getProperty("user.home"));
    }

    /** {@code ~/.toolbox}: settings, recent, signature, dan log. */
    public static Path dataDirectory() {
        return userHome().resolve(DATA_DIRECTORY_NAME);
    }
}
