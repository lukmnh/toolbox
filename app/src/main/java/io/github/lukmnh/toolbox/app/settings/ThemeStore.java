package io.github.lukmnh.toolbox.app.settings;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.settings.Theme;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Menyimpan pilihan tema di {@code ~/.toolbox/settings.properties} supaya bertahan setelah restart. */
public final class ThemeStore {

    private static final System.Logger LOG = System.getLogger(ThemeStore.class.getName());
    private static final String FILE_NAME = "settings.properties";
    private static final String KEY = "theme";

    private final Path file;

    public ThemeStore(Path file) {
        this.file = file;
    }

    public static ThemeStore inDataDirectory() {
        return new ThemeStore(AppDirectories.dataDirectory().resolve(FILE_NAME));
    }

    /**
     * Tema tersimpan, atau {@link Theme#LIGHT} bila belum ada. File rusak/tidak terbaca tidak boleh menggagalkan
     * startup: dicatat ke log lalu pakai default.
     */
    public Theme load() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty(KEY, Theme.LIGHT.name());
            return Theme.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (NoSuchFileException e) {
            return Theme.LIGHT;
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Gagal membaca tema dari " + FILE_NAME + ", memakai tema terang", e);
            return Theme.LIGHT;
        }
    }

    public void save(Theme theme) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY, theme.name());
        AtomicFiles.write(file, temp -> {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "Toolbox settings");
            }
        });
    }
}
