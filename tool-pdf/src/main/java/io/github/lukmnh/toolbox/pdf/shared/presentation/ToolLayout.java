package io.github.lukmnh.toolbox.pdf.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.FileDropZone;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Komponen layout yang dipakai bersama oleh view tool PDF. */
public final class ToolLayout {

    public static final double SPACING = 12;

    private ToolLayout() {}

    public static VBox header(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tool-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("tool-description");
        descriptionLabel.setWrapText(true);
        return new VBox(4, titleLabel, descriptionLabel);
    }

    /** Area drop + tombol pilih file, untuk tool yang belum punya file. */
    public static VBox dropZone(String hint, Runnable onChoose) {
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("drop-hint");
        hintLabel.setWrapText(true);
        Button chooseButton = new Button(Messages.get("pdf.chooseFile"));
        chooseButton.setOnAction(event -> onChoose.run());
        VBox zone = new VBox(SPACING, hintLabel, chooseButton);
        zone.setAlignment(Pos.CENTER);
        zone.getStyleClass().add("drop-zone");
        return zone;
    }

    public static void acceptPdfDrops(
            Node target, Consumer<List<Path>> onAccepted, Consumer<String> onRejectedMessage) {
        FileDropZone.install(
                target,
                PdfFileChooser.PDF_EXTENSIONS,
                onAccepted,
                rejected -> onRejectedMessage.accept(Messages.format("pdf.drop.rejected", names(rejected))));
    }

    public static Set<String> names(List<Path> files) {
        return files.stream().map(path -> path.getFileName().toString()).collect(Collectors.toSet());
    }

    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
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

    /** Baris tombol Download (utama) dan Simpan sebagai…. */
    public static HBox downloadButtons(Button download, Button saveAs) {
        download.getStyleClass().add("accent");
        HBox row = new HBox(8, download, saveAs);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }
}
