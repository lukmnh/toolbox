package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Port ke filesystem untuk Batch Rename (Security Requirements #21). Entry dibaca dan dipindah tanpa mengikuti
 * symlink, dan semua nama diverifikasi tetap langsung di dalam folder. Error I/O dilempar sebagai
 * {@link RenameIoException}.
 */
public interface RenameFileSystem {

    /** Mengikuti symlink: folder yang dipilih user boleh berupa link ke folder. */
    boolean isDirectory(Path path);

    /**
     * Entry non-folder langsung di {@code folder} (tidak rekursif); symlink ikut sebagai entry.
     *
     * @throws TooManyFilesException bila lebih dari {@code maxFiles}
     */
    List<FileEntry> listFiles(Path folder, int maxFiles);

    /** @throws InvalidSelectionException bila path tidak ada atau berupa folder */
    FileEntry describe(Path file);

    /** Nama semua entry (termasuk sub-folder) langsung di {@code folder}, untuk cek bentrok nama. */
    Set<String> entryNames(Path folder);

    /** {@code true} bila file system tidak membedakan huruf besar-kecil nama file. */
    boolean caseInsensitiveNames();

    /**
     * Rename satu entry di dalam {@code folder}: tidak pernah overwrite dan tidak mengikuti symlink.
     *
     * @throws PathOutsideFolderException bila salah satu nama keluar dari folder
     * @throws RenameIoException bila gagal, termasuk bila {@code toName} sudah ada
     */
    void move(Path folder, String fromName, String toName);
}
