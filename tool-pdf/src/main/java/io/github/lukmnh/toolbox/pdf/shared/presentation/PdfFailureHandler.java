package io.github.lukmnh.toolbox.pdf.shared.presentation;

import io.github.lukmnh.toolbox.common.ui.PasswordDialog;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import javafx.stage.Window;

/**
 * Penanganan error standar untuk proses PDF: error password memunculkan dialog password lalu proses
 * diulang; error lain ditampilkan sebagai dialog.
 */
public final class PdfFailureHandler {

    private PdfFailureHandler() {}

    /**
     * @param retryWithPassword dipanggil dengan file dan password yang diisi user; pemanggil menyimpan password
     *     lalu menjalankan ulang prosesnya
     */
    public static void handle(Window owner, Throwable error, BiConsumer<Path, String> retryWithPassword) {
        if (error instanceof PasswordRequiredException passwordError) {
            PasswordDialog.ask(owner, passwordError.file().getFileName().toString(), passwordError.passwordWasGiven())
                    .ifPresent(password -> retryWithPassword.accept(passwordError.file(), password));
            return;
        }
        PdfErrorMessages.show(owner, error);
    }
}
