package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.text.domain.TextFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.text.domain.TextIoException;
import io.github.lukmnh.toolbox.productivity.text.domain.TextToolException;
import javafx.stage.Window;

/** Memetakan error domain Text Tools ke pesan berbahasa Indonesia. */
final class TextErrorMessages {

    private TextErrorMessages() {}

    static String describe(TextToolException error) {
        return switch (error) {
            case TextFileTooLargeException e ->
                Messages.format(
                        "productivity.text.error.fileTooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case TextIoException e -> Messages.format("productivity.text.error.io", e.fileName());
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof TextToolException textError) {
            ErrorDialogs.show(owner, Messages.get("productivity.text.error.header"), describe(textError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
