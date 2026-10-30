package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** File log undo rusak atau tidak dikenali. */
public final class CorruptHistoryException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;

    public CorruptHistoryException(String id, Throwable cause) {
        super("Undo log is damaged", cause);
        this.id = id;
    }

    public String id() {
        return id;
    }
}
