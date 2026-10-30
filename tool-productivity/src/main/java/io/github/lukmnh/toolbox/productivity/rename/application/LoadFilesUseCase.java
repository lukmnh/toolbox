package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileBatch;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileEntry;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidSelectionException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.TooManyFilesException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mengubah pilihan user jadi batch: satu folder (hanya file di level itu, tidak rekursif) atau banyak file yang
 * semuanya berada di satu folder.
 */
public final class LoadFilesUseCase {

    private final RenameFileSystem fileSystem;
    private final int maxFiles;

    public LoadFilesUseCase(RenameFileSystem fileSystem, int maxFiles) {
        this.fileSystem = fileSystem;
        this.maxFiles = maxFiles;
    }

    /**
     * @param selection satu folder, atau file-file dari folder yang sama
     * @throws InvalidSelectionException bila kosong, campuran folder, atau berisi sub-folder
     * @throws TooManyFilesException bila melebihi batas
     */
    public FileBatch execute(List<Path> selection, CancellationToken cancellation) {
        List<Path> paths = selection.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .toList();
        if (paths.isEmpty()) {
            throw new InvalidSelectionException(InvalidSelectionException.Reason.EMPTY, "");
        }
        if (paths.size() == 1 && fileSystem.isDirectory(paths.getFirst())) {
            return loadFolder(paths.getFirst());
        }
        return loadFiles(paths, cancellation);
    }

    private FileBatch loadFolder(Path folder) {
        List<FileEntry> files = fileSystem.listFiles(folder, maxFiles);
        if (files.isEmpty()) {
            throw new InvalidSelectionException(InvalidSelectionException.Reason.EMPTY_FOLDER, displayName(folder));
        }
        return new FileBatch(folder, files);
    }

    private FileBatch loadFiles(List<Path> paths, CancellationToken cancellation) {
        if (paths.size() > maxFiles) {
            throw new TooManyFilesException(maxFiles);
        }
        Set<Path> parents = paths.stream().map(Path::getParent).collect(Collectors.toSet());
        if (parents.contains(null)) {
            throw new InvalidSelectionException(InvalidSelectionException.Reason.CONTAINS_DIRECTORY, "");
        }
        if (parents.size() > 1) {
            throw new InvalidSelectionException(InvalidSelectionException.Reason.MIXED_FOLDERS, "");
        }
        List<FileEntry> files = new ArrayList<>(paths.size());
        for (Path path : paths) {
            cancellation.throwIfCancelled();
            files.add(fileSystem.describe(path));
        }
        return new FileBatch(parents.iterator().next(), files);
    }

    private static String displayName(Path path) {
        return Objects.toString(path.getFileName(), path.toString());
    }
}
