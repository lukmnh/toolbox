package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import com.fasterxml.jackson.core.JsonParser;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.infrastructure.JsonFactories;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonWriter;
import io.github.lukmnh.toolbox.json.viewer.domain.RecentFiles;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Daftar file JSON terakhir di {@code ~/.toolbox/recent.json}, key {@code jsonFiles}. Key lain di file yang
 * sama (misalnya tool terakhir milik shell) dipertahankan saat menulis.
 */
public final class JsonRecentFiles implements RecentFiles {

    private static final System.Logger LOG = System.getLogger(JsonRecentFiles.class.getName());
    private static final String FILE_NAME = "recent.json";
    private static final String KEY = "jsonFiles";

    private final Path file;

    public JsonRecentFiles(Path dataDirectory) {
        this.file = dataDirectory.resolve(FILE_NAME);
    }

    @Override
    public List<Path> load() {
        Optional<JsonValue> entries = readRoot().map(root -> root.fields().get(KEY));
        if (entries.isEmpty() || !(entries.get() instanceof JsonValue.JsonArray array)) {
            return List.of();
        }
        List<Path> paths = new ArrayList<>();
        for (JsonValue item : array.items()) {
            if (item instanceof JsonValue.JsonString text) {
                toPath(text.value()).ifPresent(paths::add);
            }
        }
        return paths.stream().limit(MAX_FILES).toList();
    }

    @Override
    public void save(List<Path> files) {
        Map<String, JsonValue> fields =
                new LinkedHashMap<>(readRoot().map(JsonValue.JsonObject::fields).orElse(Map.of()));
        List<JsonValue> items = files.stream()
                .limit(MAX_FILES)
                .map(path -> (JsonValue)
                        new JsonValue.JsonString(path.toAbsolutePath().toString()))
                .toList();
        fields.put(KEY, new JsonValue.JsonArray(items));
        String json = JsonWriter.pretty(new JsonValue.JsonObject(fields)) + "\n";
        try {
            AtomicFiles.write(file, temp -> Files.writeString(temp, json));
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    /** File rusak tidak boleh menghalangi Viewer; isinya dianggap kosong dan ditimpa saat save berikutnya. */
    private Optional<JsonValue.JsonObject> readRoot() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (JsonParser parser = JsonFactories.FACTORY.createParser(Files.readAllBytes(file))) {
            if (parser.nextToken() == null) {
                return Optional.empty();
            }
            return JacksonValueReader.read(parser) instanceof JsonValue.JsonObject object
                    ? Optional.of(object)
                    : Optional.empty();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membaca " + file.getFileName() + ", daftar file terakhir dikosongkan", e);
            return Optional.empty();
        }
    }

    private static Optional<Path> toPath(String text) {
        try {
            return Optional.of(Path.of(text));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
