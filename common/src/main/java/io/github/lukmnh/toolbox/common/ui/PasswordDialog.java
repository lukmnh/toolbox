package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Meminta password untuk membuka file terenkripsi. */
public final class PasswordDialog {

    private static final double SPACING = 8;

    private PasswordDialog() {}

    /**
     * @param wrongPassword {@code true} bila password sebelumnya salah, untuk menampilkan peringatan
     * @return password, atau kosong bila user membatalkan
     */
    public static Optional<String> ask(Window owner, String fileName, boolean wrongPassword) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(Messages.get("password.title"));
        dialog.setHeaderText(Messages.format("password.header", fileName));

        PasswordField field = new PasswordField();
        VBox content = new VBox(SPACING);
        if (wrongPassword) {
            Label warning = new Label(Messages.get("password.wrong"));
            warning.getStyleClass().add("text-error");
            content.getChildren().add(warning);
        }
        content.getChildren().addAll(new Label(Messages.get("password.label")), field);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK ? field.getText() : null);
        Platform.runLater(field::requestFocus);
        return dialog.showAndWait();
    }
}
