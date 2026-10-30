package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.hash.domain.KeyExportException;
import javafx.stage.Window;

/** Memetakan error tab Generate ke pesan yang bisa dibaca user. */
final class GenerateErrorMessages {

    private GenerateErrorMessages() {}

    static void showKeyPairFailure(Window owner, Throwable error) {
        // Pesan JCA hanya berisi nama algoritma/parameter, tidak pernah materi key
        ErrorDialogs.show(owner, Messages.get("devutils.hash.generate.error.keyPair"), ErrorDialogs.describe(error));
    }

    static void showExportFailure(Window owner, Throwable error) {
        if (error instanceof KeyExportException exportError) {
            ErrorDialogs.show(
                    owner,
                    Messages.get("devutils.hash.generate.error.export"),
                    Messages.format("devutils.hash.generate.error.io", exportError.fileName()));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
