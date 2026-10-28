package io.github.lukmnh.toolbox.common.io;

import java.nio.file.Path;

/** Lokasi data aplikasi di folder home user. */
public final class AppDirectories {

    private static final String DATA_DIRECTORY_NAME = ".toolbox";
    private static final String TEMP_DIRECTORY_NAME = "tmp";

    private AppDirectories() {}

    public static Path userHome() {
        return Path.of(System.getProperty("user.home"));
    }

    /** {@code ~/.toolbox}: settings, recent, signature, dan log. */
    public static Path dataDirectory() {
        return userHome().resolve(DATA_DIRECTORY_NAME);
    }

    /** {@code ~/.toolbox/tmp}: semua temp file aplikasi (Security Requirements #4). */
    public static Path tempDirectory() {
        return dataDirectory().resolve(TEMP_DIRECTORY_NAME);
    }
}
