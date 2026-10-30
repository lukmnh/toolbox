package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/**
 * Semua error domain Batch Rename. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive. Pesan exception tidak memuat nama/path file (Security Requirements #5).
 */
public abstract sealed class RenameException extends RuntimeException
        permits TooManyFilesException,
                InvalidSelectionException,
                InvalidRuleException,
                RenameIoException,
                PathOutsideFolderException,
                RenameFailedException,
                PlanOutdatedException,
                UndoUnavailableException,
                CorruptHistoryException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected RenameException(String message, Throwable cause) {
        super(message, cause);
    }
}
