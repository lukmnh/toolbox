package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFormatFailedException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlIoException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlToolException;
import java.lang.System.Logger.Level;
import javafx.stage.Window;

/** Memetakan error domain SQL Formatter ke pesan berbahasa Indonesia. */
final class SqlErrorMessages {

    private static final System.Logger LOG = System.getLogger(SqlErrorMessages.class.getName());

    private SqlErrorMessages() {}

    static String describe(SqlToolException error) {
        return switch (error) {
            case SqlFileTooLargeException e ->
                Messages.format(
                        "devutils.sql.error.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case SqlIoException e -> Messages.format("devutils.sql.error.io", e.fileName());
            case SqlFormatFailedException e -> {
                // Hanya tipe error: pesan library bisa memuat potongan SQL user (Security Requirements #5)
                LOG.log(
                        Level.WARNING,
                        "Formatter SQL gagal: " + e.getCause().getClass().getName());
                yield Messages.format("devutils.sql.error.formatFailed", e.statementNumber());
            }
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof SqlToolException sqlError) {
            ErrorDialogs.show(owner, Messages.get("devutils.sql.error.header"), describe(sqlError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
