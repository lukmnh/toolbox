package io.github.lukmnh.toolbox.productivity.rename.infrastructure;

import io.github.lukmnh.toolbox.common.security.SafePaths;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileEntry;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidSelectionException;
import io.github.lukmnh.toolbox.productivity.rename.domain.PathOutsideFolderException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameIoException;
import io.github.lukmnh.toolbox.productivity.rename.domain.TooManyFilesException;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** {@link RenameFileSystem} dengan NIO. Tidak pernah overwrite dan tidak pernah mengikuti symlink entry. */
public final class NioRenameFileSystem implements RenameFileSystem {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    @Override
    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public List<FileEntry> listFiles(Path folder, int maxFiles) {
        List<FileEntry> files = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder)) {
            for (Path entry : entries) {
                BasicFileAttributes attributes = attributes(entry);
                if (attributes.isDirectory()) {
                    continue;
                }
                // Berhenti begitu lewat batas supaya folder berisi jutaan file tidak dibaca habis
                if (files.size() == maxFiles) {
                    throw new TooManyFilesException(maxFiles);
                }
                files.add(toEntry(entry, attributes));
            }
        } catch (IOException | DirectoryIteratorException e) {
            throw new RenameIoException(name(folder), e);
        }
        return List.copyOf(files);
    }

    @Override
    public FileEntry describe(Path file) {
        try {
            BasicFileAttributes attributes = attributes(file);
            if (attributes.isDirectory()) {
                throw new InvalidSelectionException(InvalidSelectionException.Reason.CONTAINS_DIRECTORY, name(file));
            }
            return toEntry(file, attributes);
        } catch (NoSuchFileException e) {
            throw new InvalidSelectionException(InvalidSelectionException.Reason.NOT_FOUND, name(file));
        } catch (IOException e) {
            throw new RenameIoException(name(file), e);
        }
    }

    @Override
    public Set<String> entryNames(Path folder) {
        Set<String> names = new HashSet<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder)) {
            entries.forEach(entry -> names.add(entry.getFileName().toString()));
        } catch (IOException | DirectoryIteratorException e) {
            throw new RenameIoException(name(folder), e);
        }
        return Set.copyOf(names);
    }

    /** Default NTFS dan APFS tidak membedakan huruf besar-kecil; ext4 membedakan. */
    @Override
    public boolean caseInsensitiveNames() {
        return OS_NAME.startsWith("windows") || OS_NAME.startsWith("mac");
    }

    @Override
    public void move(Path folder, String fromName, String toName) {
        Path source = inside(folder, fromName);
        Path target = inside(folder, toName);
        try {
            // Tanpa REPLACE_EXISTING: target yang sudah ada → FileAlreadyExistsException, bukan overwrite
            Files.move(source, target, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new RenameIoException(fromName, e);
        }
    }

    /** Hanya entry langsung di folder: nama berisi separator (sub-folder) atau {@code ..} ditolak. */
    private static Path inside(Path folder, String name) {
        Path base = folder.toAbsolutePath().normalize();
        return SafePaths.resolveInside(base, name)
                .or(() -> linkInside(base, name))
                .filter(path -> base.equals(path.getParent()))
                .orElseThrow(() -> new PathOutsideFolderException(name));
    }

    /**
     * {@link SafePaths} mengikuti symlink lewat {@code toRealPath}, sehingga link yang menunjuk ke luar folder ditolak.
     * Yang di-rename adalah link-nya sendiri (bukan target), jadi cukup pastikan link berada langsung di folder.
     */
    private static Optional<Path> linkInside(Path base, String name) {
        try {
            Path candidate = base.resolve(name).normalize();
            boolean direct = base.equals(candidate.getParent()) && Files.isSymbolicLink(candidate);
            return direct ? Optional.of(candidate) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    private static FileEntry toEntry(Path path, BasicFileAttributes attributes) {
        return new FileEntry(
                name(path), attributes.size(), attributes.lastModifiedTime().toInstant(), attributes.isSymbolicLink());
    }

    private static String name(Path path) {
        return Objects.toString(path.getFileName(), path.toString());
    }
}
