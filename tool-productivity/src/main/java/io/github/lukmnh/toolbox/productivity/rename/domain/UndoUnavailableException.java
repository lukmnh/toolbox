package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Undo tidak bisa dijalankan; tidak ada file yang disentuh. */
public final class UndoUnavailableException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        NO_HISTORY,
        CORRUPT_HISTORY,
        FOLDER_MISSING,
        FILE_MISSING,
        NAME_TAKEN
    }

    private final Reason reason;
    private final String fileName;

    /** @param fileName nama file terkait untuk pesan user, atau kosong */
    public UndoUnavailableException(Reason reason, String fileName) {
        super("Undo cannot run: " + reason, null);
        this.reason = reason;
        this.fileName = fileName;
    }

    public Reason reason() {
        return reason;
    }

    public String fileName() {
        return fileName;
    }
}
