package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Hasil preview untuk seluruh batch, urutan baris = urutan file di batch. */
public record RenamePreview(Path folder, List<PreviewRow> rows) {

    public RenamePreview {
        Objects.requireNonNull(folder, "folder");
        rows = List.copyOf(rows);
    }

    /**
     * @param newNames nama baru, indeks sama dengan {@code batch.files()}
     * @param folderNames semua entry di folder saat ini
     */
    public static RenamePreview of(
            FileBatch batch, List<String> newNames, Set<String> folderNames, boolean caseInsensitive) {
        List<FileEntry> files = batch.files();
        List<String> originals = files.stream().map(FileEntry::name).toList();
        List<NameCheck> checks = RenameChecker.check(originals, newNames, folderNames, caseInsensitive);
        List<PreviewRow> rows = new ArrayList<>(files.size());
        for (int index = 0; index < files.size(); index++) {
            NameCheck check = checks.get(index);
            rows.add(new PreviewRow(files.get(index), newNames.get(index), check.status(), check.issue()));
        }
        return new RenamePreview(batch.folder(), rows);
    }

    public long count(RenameStatus status) {
        return rows.stream().filter(row -> row.status() == status).count();
    }

    /** Minimal satu file berubah dan tidak ada KONFLIK / TIDAK VALID. */
    public boolean executable() {
        return count(RenameStatus.OK) > 0 && count(RenameStatus.CONFLICT) == 0 && count(RenameStatus.INVALID) == 0;
    }

    /** Pasangan rename untuk file berstatus OK saja; file yang tidak berubah tidak disentuh. */
    public List<RenamePair> changes() {
        return rows.stream()
                .filter(row -> row.status() == RenameStatus.OK)
                .map(row -> new RenamePair(row.file().name(), row.newName()))
                .toList();
    }
}
