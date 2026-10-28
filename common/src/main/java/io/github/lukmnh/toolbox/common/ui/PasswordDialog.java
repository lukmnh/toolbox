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
     * @return password sebagai {@code char[]} (pemanggil wajib meng-wipe setelah dipakai), atau kosong bila
     *     user membatalkan
     */
    public static Optional<char[]> ask(Window owner, String fileName, boolean wrongPassword) {
        Dialog<char[]> dialog = new Dialog<>();
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
        dialog.setResultConverter(button -> button == ButtonType.OK ? toChars(field.getCharacters()) : null);
        Platform.runLater(field::requestFocus);
        Optional<char[]> password = dialog.showAndWait();
        // Kosongkan field UI supaya password tidak tertinggal di node yang masih direferensikan
        field.clear();
        return password;
    }

    /** Disalin per karakter supaya tidak membuat {@link String} yang tidak bisa di-wipe. */
    private static char[] toChars(CharSequence typed) {
        char[] chars = new char[typed.length()];
        for (int index = 0; index < chars.length; index++) {
            chars[index] = typed.charAt(index);
        }
        return chars;
    }
}
