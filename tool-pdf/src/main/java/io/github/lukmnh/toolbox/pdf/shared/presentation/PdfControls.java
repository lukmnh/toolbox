package io.github.lukmnh.toolbox.pdf.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.FileDropZone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Kontrol khusus tool PDF; layout umum ada di {@link ToolLayout}. */
public final class PdfControls {

    private PdfControls() {}

    /** Area drop + tombol pilih file, untuk tool yang belum punya file. */
    public static VBox dropZone(String hint, Runnable onChoose) {
        return ToolLayout.dropZone(hint, Messages.get("pdf.chooseFile"), onChoose);
    }

    public static void acceptPdfDrops(
            Node target, Consumer<List<Path>> onAccepted, Consumer<String> onRejectedMessage) {
        FileDropZone.install(
                target,
                PdfFileChooser.PDF_EXTENSIONS,
                onAccepted,
                rejected -> onRejectedMessage.accept(Messages.format("pdf.drop.rejected", ToolLayout.names(rejected))));
    }

    /**
     * Checkbox "Hapus password dari hasil" (Security Requirements #2). Default mati; saat dicentang user wajib
     * mengonfirmasi warning, kalau batal checkbox kembali mati.
     */
    public static CheckBox removeProtectionToggle(Window owner, BooleanProperty removeProtection) {
        CheckBox toggle = new CheckBox(Messages.get("pdf.protection.remove"));
        toggle.setTooltip(new Tooltip(Messages.get("pdf.protection.remove.tooltip")));
        toggle.selectedProperty().bindBidirectional(removeProtection);
        // onAction hanya terpicu oleh klik user, bukan oleh perubahan programatik
        toggle.setOnAction(event -> {
            if (toggle.isSelected()
                    && !ErrorDialogs.confirm(
                            owner,
                            Messages.get("pdf.protection.remove.header"),
                            Messages.get("pdf.protection.remove.detail"),
                            Messages.get("pdf.protection.remove.confirm"))) {
                toggle.setSelected(false);
            }
        });
        return toggle;
    }
}
