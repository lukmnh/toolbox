package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Optional;

/** Port penyimpanan log undo (owner-only, ditulis atomic). Error I/O dilempar sebagai {@link RenameIoException}. */
public interface RenameHistory {

    /** Jumlah log yang disimpan; yang lebih lama dihapus. */
    int MAX_ENTRIES = 20;

    /** Simpan log lalu hapus log di luar {@link #MAX_ENTRIES} terbaru. @return id log */
    String save(RenameLog log);

    /** @throws CorruptHistoryException bila log terbaru tidak bisa dibaca */
    Optional<StoredRename> latest();

    void delete(String id);
}
