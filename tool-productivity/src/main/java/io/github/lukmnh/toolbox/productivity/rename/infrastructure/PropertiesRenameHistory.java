package io.github.lukmnh.toolbox.productivity.rename.infrastructure;

import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.security.OwnerOnlyFiles;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.productivity.rename.domain.CorruptHistoryException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameIoException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameLog;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePair;
import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Log undo sebagai satu file {@code .properties} per eksekusi di folder owner-only, ditulis atomic. Format
 * {@link Properties} dipilih karena escaping (unicode, baris baru, {@code =}) sudah ditangani JDK tanpa dependency
 * tambahan.
 */
public final class PropertiesRenameHistory implements RenameHistory {

    private static final String FILE_PREFIX = "rename-";
    private static final String FILE_SUFFIX = ".properties";
    /** Timestamp 19 digit (nol di depan) supaya urutan nama file = urutan waktu. */
    private static final String ID_FORMAT = FILE_PREFIX + "%019d-%s";

    private static final Pattern ID = Pattern.compile("rename-\\d{19}-[0-9a-f]{8}");
    private static final int RANDOM_SUFFIX_LENGTH = 8;

    private static final String KEY_FORMAT = "format";
    private static final String KEY_FOLDER = "folder";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_COUNT = "count";
    private static final String KEY_FROM = "from.";
    private static final String KEY_TO = "to.";
    private static final String FORMAT_VERSION = "1";

    /** 10.000 pasang nama × 2 × 255 byte (plus escaping) masih jauh di bawah batas ini. */
    private static final long MAX_FILE_BYTES = 32L * 1024 * 1024;

    private final Path directory;

    /** @param directory biasanya {@code ~/.toolbox/rename-history} */
    public PropertiesRenameHistory(Path directory) {
        this.directory = directory;
    }

    @Override
    public String save(RenameLog log) {
        String id = String.format(
                Locale.ROOT,
                ID_FORMAT,
                log.createdAt().toEpochMilli(),
                UUID.randomUUID().toString().substring(0, RANDOM_SUFFIX_LENGTH));
        try {
            OwnerOnlyFiles.createDirectories(directory);
            AtomicFiles.write(file(id), temp -> write(temp, log));
            prune();
            return id;
        } catch (IOException | UncheckedIOException e) {
            throw new RenameIoException(historyName(), e);
        }
    }

    @Override
    public Optional<StoredRename> latest() {
        List<String> ids = ids();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        String id = ids.getLast();
        return Optional.of(new StoredRename(id, read(id)));
    }

    @Override
    public void delete(String id) {
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Id log undo tidak valid");
        }
        try {
            Files.deleteIfExists(file(id));
        } catch (IOException e) {
            throw new RenameIoException(historyName(), e);
        }
    }

    /** Permission dibatasi sebelum file dipindah ke nama final, jadi tidak pernah ada jeda file terbaca user lain. */
    private static void write(Path temp, RenameLog log) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_FORMAT, FORMAT_VERSION);
        properties.setProperty(KEY_FOLDER, log.folder().toString());
        properties.setProperty(KEY_CREATED_AT, log.createdAt().toString());
        properties.setProperty(KEY_COUNT, Integer.toString(log.pairs().size()));
        for (int index = 0; index < log.pairs().size(); index++) {
            properties.setProperty(KEY_FROM + index, log.pairs().get(index).from());
            properties.setProperty(KEY_TO + index, log.pairs().get(index).to());
        }
        OwnerOnlyFiles.restrict(temp);
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }

    private RenameLog read(String id) {
        Path file = file(id);
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw new CorruptHistoryException(id, null);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return parse(id, properties);
        } catch (IOException e) {
            throw new RenameIoException(historyName(), e);
        } catch (IllegalArgumentException | DateTimeException e) {
            // Escape \\u rusak, angka/tanggal/path tidak valid, atau nama kosong
            throw new CorruptHistoryException(id, e);
        }
    }

    private static RenameLog parse(String id, Properties properties) {
        if (!FORMAT_VERSION.equals(properties.getProperty(KEY_FORMAT))) {
            throw new CorruptHistoryException(id, null);
        }
        Path folder = Path.of(required(id, properties, KEY_FOLDER));
        Instant createdAt = Instant.parse(required(id, properties, KEY_CREATED_AT));
        int count = Integer.parseInt(required(id, properties, KEY_COUNT));
        if (count < 1 || count > SecurityLimits.RENAME_MAX_FILES) {
            throw new CorruptHistoryException(id, null);
        }
        List<RenamePair> pairs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            pairs.add(new RenamePair(
                    required(id, properties, KEY_FROM + index), required(id, properties, KEY_TO + index)));
        }
        return new RenameLog(folder, createdAt, pairs);
    }

    private static String required(String id, Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new CorruptHistoryException(id, null);
        }
        return value;
    }

    /** Hapus log di luar {@link #MAX_ENTRIES} terbaru. */
    private void prune() throws IOException {
        List<String> ids = ids();
        for (int index = 0; index < ids.size() - MAX_ENTRIES; index++) {
            Files.deleteIfExists(file(ids.get(index)));
        }
    }

    /** Id terurut dari yang terlama. */
    private List<String> ids() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_SUFFIX))
                    .map(name -> name.substring(0, name.length() - FILE_SUFFIX.length()))
                    .filter(name -> ID.matcher(name).matches())
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            throw new RenameIoException(historyName(), e);
        }
    }

    private Path file(String id) {
        return directory.resolve(id + FILE_SUFFIX);
    }

    private String historyName() {
        return String.valueOf(directory.getFileName());
    }
}
