package io.github.lukmnh.toolbox.devutils.qr.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.qr.domain.InvalidQrUrlException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrContentTooLargeException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrExportException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrToolException;
import javafx.stage.Window;

/** Memetakan error domain QR Code Generator ke pesan yang bisa dibaca user. */
final class QrErrorMessages {

    private QrErrorMessages() {}

    static String describe(QrToolException error) {
        return switch (error) {
            // Angka dikirim sebagai String supaya tidak diberi pemisah ribuan oleh MessageFormat
            case QrContentTooLargeException e ->
                Messages.format(
                        "devutils.qr.error.tooLarge",
                        String.valueOf(e.payloadBytes()),
                        e.errorCorrection().name(),
                        String.valueOf(e.errorCorrection().maxBytes()));
            case InvalidQrUrlException e -> Messages.get("devutils.qr.error.invalidUrl");
            case QrExportException e -> Messages.format("devutils.qr.error.io", e.fileName());
        };
    }

    static void showExportFailure(Window owner, Throwable error) {
        if (error instanceof QrToolException qrError) {
            ErrorDialogs.show(owner, Messages.get("devutils.qr.error.export"), describe(qrError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
