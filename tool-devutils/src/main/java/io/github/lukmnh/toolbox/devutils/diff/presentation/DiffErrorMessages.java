package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.diff.domain.BinaryFileException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffIoException;
import javafx.stage.Window;

/** Memetakan error domain Text Diff ke pesan berbahasa Indonesia yang menyebut nama file. */
final class DiffErrorMessages {

    private DiffErrorMessages() {}

    static String describe(DiffException error) {
        return switch (error) {
            case BinaryFileException e -> Messages.format("devutils.diff.error.binary", e.fileName());
            case DiffFileTooLargeException e ->
                Messages.format(
                        "devutils.diff.error.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case DiffIoException e -> Messages.format("devutils.diff.error.io", e.fileName());
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof DiffException diffError) {
            ErrorDialogs.show(owner, Messages.get("devutils.diff.error.header"), describe(diffError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
