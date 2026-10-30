package io.github.lukmnh.toolbox.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Versi aplikasi dari {@code app.properties} yang diisi Maven saat build, jadi tidak di-hard-code di kode. */
public final class AppVersion {

    static final String RESOURCE = "/app.properties";
    private static final String KEY = "version";

    private AppVersion() {}

    /**
     * @throws IllegalStateException bila resource hilang atau belum di-filter Maven (kesalahan build, bukan input user)
     */
    public static String current() {
        try (InputStream input = AppVersion.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Resource " + RESOURCE + " tidak ditemukan");
            }
            return parse(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal membaca " + RESOURCE, e);
        }
    }

    static String parse(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        String version = properties.getProperty(KEY, "").strip();
        // "${project.version}" berarti resource filtering tidak jalan
        if (version.isEmpty() || version.contains("${")) {
            throw new IllegalStateException("Versi aplikasi tidak valid di " + RESOURCE + ": '" + version + "'");
        }
        return version;
    }
}
