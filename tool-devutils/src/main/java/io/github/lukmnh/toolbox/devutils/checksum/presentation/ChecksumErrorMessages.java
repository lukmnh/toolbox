package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumIoException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.InvalidChecksumException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.InvalidSumsFileException;
import io.github.lukmnh.toolbox.devutils.checksum.domain.SumsFileTooLargeException;
import javafx.stage.Window;

/** Memetakan error domain Checksum ke pesan berbahasa Indonesia yang menyebut nama file. */
final class ChecksumErrorMessages {

    private ChecksumErrorMessages() {}

    static String describe(ChecksumException error) {
        return switch (error) {
            case ChecksumIoException e -> Messages.format("devutils.checksum.error.io", e.fileName());
            case InvalidChecksumException e -> Messages.get("devutils.checksum.error.invalidChecksum");
            case InvalidSumsFileException e ->
                e.lineNumber() == InvalidSumsFileException.NO_ENTRIES
                        ? Messages.get("devutils.checksum.error.sums.empty")
                        : Messages.format("devutils.checksum.error.sums.line", e.lineNumber());
            case SumsFileTooLargeException e ->
                Messages.format(
                        "devutils.checksum.error.sums.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof ChecksumException checksumError) {
            ErrorDialogs.show(owner, Messages.get("devutils.checksum.error.header"), describe(checksumError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
