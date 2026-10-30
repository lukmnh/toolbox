package io.github.lukmnh.toolbox.app.settings;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Menyimpan favorit dan recent tool di {@code ~/.toolbox/tool-usage.properties}. */
public final class ToolUsageStore {

    private static final System.Logger LOG = System.getLogger(ToolUsageStore.class.getName());
    private static final String FILE_NAME = "tool-usage.properties";
    private static final String FAVORITES = "favorites";
    private static final String RECENT = "recent";
    private static final String SEPARATOR = ",";

    private final Path file;

    public ToolUsageStore(Path file) {
        this.file = file;
    }

    public static ToolUsageStore inDataDirectory() {
        return new ToolUsageStore(AppDirectories.dataDirectory().resolve(FILE_NAME));
    }

    /** File belum ada atau rusak tidak menggagalkan startup: dicatat ke log lalu mulai kosong. */
    public ToolUsage load() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return new ToolUsage(ids(properties.getProperty(FAVORITES)), ids(properties.getProperty(RECENT)));
        } catch (NoSuchFileException e) {
            return ToolUsage.EMPTY;
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Gagal membaca " + FILE_NAME + ", favorit dan recent dikosongkan", e);
            return ToolUsage.EMPTY;
        }
    }

    public void save(ToolUsage usage) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(FAVORITES, String.join(SEPARATOR, usage.favorites()));
        properties.setProperty(RECENT, String.join(SEPARATOR, usage.recent()));
        AtomicFiles.write(file, temp -> {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "Toolbox favorites and recent tools");
            }
        });
    }

    private static List<String> ids(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(SEPARATOR))
                .map(String::strip)
                .filter(id -> !id.isEmpty())
                .toList();
    }
}
