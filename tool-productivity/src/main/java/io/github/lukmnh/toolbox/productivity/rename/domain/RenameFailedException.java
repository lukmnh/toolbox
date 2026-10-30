package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;
import java.util.List;

/** Rename gagal di tengah dan sudah di-rollback; {@link #notRestored()} berisi file yang gagal dikembalikan. */
public final class RenameFailedException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final transient List<String> notRestored;

    /**
     * @param fileName nama asal file yang gagal di-rename
     * @param notRestored nama asal file yang tidak berhasil dikembalikan saat rollback
     */
    public RenameFailedException(String fileName, List<String> notRestored, Throwable cause) {
        super("Rename failed, " + notRestored.size() + " file(s) could not be rolled back", cause);
        this.fileName = fileName;
        this.notRestored = List.copyOf(notRestored);
    }

    public String fileName() {
        return fileName;
    }

    public List<String> notRestored() {
        return notRestored == null ? List.of() : notRestored;
    }
}
