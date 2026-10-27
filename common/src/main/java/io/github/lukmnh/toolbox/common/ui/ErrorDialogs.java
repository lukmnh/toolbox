package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.lang.System.Logger.Level;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Dialog error dan konfirmasi berbahasa Indonesia. Harus dipanggil dari FX thread. */
public final class ErrorDialogs {

    private static final System.Logger LOG = System.getLogger(ErrorDialogs.class.getName());

    private ErrorDialogs() {}

    public static void show(Window owner, String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(Messages.get("error.title"));
        alert.setHeaderText(header);
        alert.setContentText(detail);
        alert.showAndWait();
    }

    /** Untuk error yang tidak dipetakan ke pesan khusus: dicatat ke log lalu ditampilkan generik. */
    public static void showUnexpected(Window owner, Throwable error) {
        LOG.log(Level.ERROR, "Error tak terduga", error);
        show(owner, Messages.get("error.unexpected.header"), describe(error));
    }

    /** Ringkasan error yang bisa dibaca user. */
    public static String describe(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    /** Konfirmasi dengan tombol aksi dan Batal. {@code true} bila user memilih aksi. */
    public static boolean confirm(Window owner, String header, String detail, String confirmLabel) {
        ButtonType confirmButton = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(Messages.get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, detail, confirmButton, cancelButton);
        alert.initOwner(owner);
        alert.setTitle(Messages.get("confirm.title"));
        alert.setHeaderText(header);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.filter(confirmButton::equals).isPresent();
    }
}
