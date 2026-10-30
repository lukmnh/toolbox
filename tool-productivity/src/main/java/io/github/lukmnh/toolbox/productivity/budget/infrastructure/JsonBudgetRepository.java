package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.common.download.UniqueFileNamer;
import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import io.github.lukmnh.toolbox.common.security.FileNameSanitizer;
import io.github.lukmnh.toolbox.common.security.OwnerOnlyFiles;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Satu file JSON per budget di folder budgets (owner-only). Nama file = nama budget yang disanitasi + unik; id budget
 * = nama file tanpa {@code .json}. Semua method {@code synchronized} supaya autosave dari beberapa virtual thread
 * tidak saling tumpang.
 */
public final class JsonBudgetRepository implements BudgetRepository {

    private static final System.Logger LOG = System.getLogger(JsonBudgetRepository.class.getName());
    private static final String EXTENSION = ".json";

    private final Path directory;
    private final BudgetJsonCodec codec;

    /** @param directory folder budget, dibuat owner-only saat pertama dipakai */
    public JsonBudgetRepository(Path directory, BudgetJsonCodec codec) {
        this.directory = directory;
        this.codec = codec;
    }

    @Override
    public synchronized List<BudgetEntry> list() {
        List<BudgetEntry> entries = new ArrayList<>();
        for (Path file : budgetFiles()) {
            try {
                Budget budget = codec.read(file);
                entries.add(new BudgetEntry(idOf(file), budget.name(), budget.modified()));
            } catch (BudgetException e) {
                // Satu file rusak tidak boleh membuat semua budget hilang dari daftar; isi file tidak dicatat
                LOG.log(
                        Level.WARNING,
                        "File budget dilewati: " + file.getFileName() + " ("
                                + e.getClass().getSimpleName() + ")");
            }
        }
        return List.copyOf(entries);
    }

    @Override
    public synchronized Budget load(String id) {
        return codec.read(fileOf(id));
    }

    @Override
    public synchronized BudgetEntry create(Budget budget) {
        Path target = newFileFor(budget.name());
        write(target, budget);
        return new BudgetEntry(idOf(target), budget.name(), budget.modified());
    }

    @Override
    public synchronized void save(String id, Budget budget) {
        write(fileOf(id), budget);
    }

    @Override
    public synchronized BudgetEntry rename(String id, Budget renamed) {
        Path current = fileOf(id);
        if (FileNameSanitizer.sanitize(renamed.name()).equals(id)) {
            write(current, renamed);
            return new BudgetEntry(id, renamed.name(), renamed.modified());
        }
        // File baru ditulis dulu; file lama baru dihapus setelah itu, sehingga data tidak pernah hilang di tengah
        Path target = newFileFor(renamed.name());
        write(target, renamed);
        deleteFile(current);
        return new BudgetEntry(idOf(target), renamed.name(), renamed.modified());
    }

    @Override
    public synchronized void delete(String id) {
        deleteFile(fileOf(id));
    }

    private List<Path> budgetFiles() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new BudgetStorageException(directory.getFileName().toString(), e);
        }
    }

    private Path newFileFor(String budgetName) {
        ensureDirectory();
        return UniqueFileNamer.uniqueFile(directory, FileNameSanitizer.sanitize(budgetName) + EXTENSION);
    }

    /** Id berasal dari daftar file sendiri, tapi tetap dicek supaya tidak pernah keluar folder budget. */
    private Path fileOf(String id) {
        if (id.isBlank() || !FileNameSanitizer.sanitize(id).equals(id)) {
            throw new IllegalArgumentException("Id budget tidak valid");
        }
        return directory.resolve(id + EXTENSION);
    }

    private static String idOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    /** Permission owner-only dipasang ke temp sebelum isi ditulis, lalu dipindah atomic ke tujuan. */
    private void write(Path target, Budget budget) {
        ensureDirectory();
        try {
            AtomicFiles.write(target, temp -> {
                OwnerOnlyFiles.restrict(temp);
                codec.write(budget, temp);
            });
        } catch (IOException e) {
            throw new BudgetStorageException(target.getFileName().toString(), e);
        }
    }

    private void ensureDirectory() {
        try {
            OwnerOnlyFiles.createDirectories(directory);
        } catch (IOException e) {
            throw new BudgetStorageException(directory.getFileName().toString(), e);
        }
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BudgetStorageException(file.getFileName().toString(), e);
        }
    }
}
