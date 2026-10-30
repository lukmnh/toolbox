package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Pilihan file/folder tidak bisa dijadikan batch. */
public final class InvalidSelectionException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Reason {
        EMPTY,
        EMPTY_FOLDER,
        MIXED_FOLDERS,
        CONTAINS_DIRECTORY,
        NOT_FOUND
    }

    private final Reason reason;
    private final String fileName;

    /** @param fileName nama file/folder terkait untuk pesan user, atau kosong */
    public InvalidSelectionException(Reason reason, String fileName) {
        super("Invalid file selection: " + reason, null);
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
