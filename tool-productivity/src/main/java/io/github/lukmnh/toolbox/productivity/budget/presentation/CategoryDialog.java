package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import java.util.Locale;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/** Dialog tambah/ubah kategori: nama + warna ({@link ColorPicker}), plus tombol Hapus saat mengubah. */
final class CategoryDialog {

    /** Hasil dialog. */
    sealed interface Result {
        record Save(String name, String color) implements Result {}

        record Delete() implements Result {}
    }

    private static final int RGB_MAX = 255;

    private CategoryDialog() {}

    /** @param deletable tampilkan tombol Hapus (kategori yang sudah ada) */
    static Optional<Result> show(Window owner, String header, String name, String color, boolean deletable) {
        TextField nameField = new TextField(name);
        ColorPicker picker = new ColorPicker(Color.web(color));
        picker.getCustomColors()
                .setAll(BudgetLabels.PASTEL_PALETTE.stream().map(Color::web).toList());
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(Messages.get("productivity.budget.category.title"));
        dialog.setHeaderText(header);
        ButtonType save = new ButtonType(Messages.get("productivity.budget.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType delete = new ButtonType(Messages.get("productivity.budget.delete"), ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        if (deletable) {
            dialog.getDialogPane().getButtonTypes().add(delete);
        }
        dialog.getDialogPane()
                .lookupButton(save)
                .disableProperty()
                .bind(nameField.textProperty().map(CategoryDialog::invalid));
        dialog.getDialogPane()
                .setContent(new VBox(
                        ToolLayout.SPACING,
                        ToolLayout.labeled(Messages.get("productivity.budget.dialog.name"), nameField),
                        ToolLayout.labeled(Messages.get("productivity.budget.category.color"), picker)));
        dialog.setResultConverter(button -> {
            if (button == save) {
                return new Result.Save(nameField.getText().strip(), hex(picker.getValue()));
            }
            return button == delete ? new Result.Delete() : null;
        });
        nameField.requestFocus();
        return dialog.showAndWait();
    }

    private static boolean invalid(String name) {
        String stripped = name.strip();
        return stripped.isEmpty() || stripped.length() > Budget.MAX_NAME_LENGTH;
    }

    /** {@code #RRGGBB}; transparansi diabaikan karena warna dipakai sebagai latar sel. */
    static String hex(Color color) {
        return String.format(
                Locale.ROOT,
                "#%02X%02X%02X",
                Math.round(color.getRed() * RGB_MAX),
                Math.round(color.getGreen() * RGB_MAX),
                Math.round(color.getBlue() * RGB_MAX));
    }
}
