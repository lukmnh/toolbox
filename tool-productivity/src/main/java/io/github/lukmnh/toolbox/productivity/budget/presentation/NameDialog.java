package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

/** Dialog input nama (budget/daftar). Tombol OK hanya aktif untuk nama yang valid. */
final class NameDialog {

    private NameDialog() {}

    /** @return nama yang sudah di-strip, kosong bila dibatalkan */
    static Optional<String> ask(Window owner, String header, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.initOwner(owner);
        dialog.setTitle(Messages.get("productivity.budget.name"));
        dialog.setHeaderText(header);
        dialog.setContentText(Messages.get("productivity.budget.dialog.name"));
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(dialog.getEditor().textProperty().map(NameDialog::invalid));
        return dialog.showAndWait().map(String::strip).filter(name -> !invalid(name));
    }

    private static boolean invalid(String name) {
        String stripped = name.strip();
        return stripped.isEmpty() || stripped.length() > Budget.MAX_NAME_LENGTH;
    }
}
