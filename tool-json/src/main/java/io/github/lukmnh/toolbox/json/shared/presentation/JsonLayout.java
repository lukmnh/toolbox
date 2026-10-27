package io.github.lukmnh.toolbox.json.shared.presentation;

import io.github.lukmnh.toolbox.common.ui.FileDropZone;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Komponen layout yang dipakai bersama oleh view tool JSON. */
public final class JsonLayout {

    public static final double SPACING = 12;

    /** Font monospace logis JavaFX, tersedia di Windows dan Linux. */
    private static final String MONOSPACE_STYLE = "-fx-font-family: 'Monospaced';";

    private JsonLayout() {}

    public static VBox header(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tool-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("tool-description");
        descriptionLabel.setWrapText(true);
        return new VBox(4, titleLabel, descriptionLabel);
    }

    /** Area drop + tombol pilih file, untuk tool yang belum punya file. */
    public static VBox dropZone(String hint, String buttonLabel, Runnable onChoose) {
        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("drop-hint");
        hintLabel.setWrapText(true);
        Button chooseButton = new Button(buttonLabel);
        chooseButton.setOnAction(event -> onChoose.run());
        VBox zone = new VBox(SPACING, hintLabel, chooseButton);
        zone.setAlignment(Pos.CENTER);
        zone.getStyleClass().add("drop-zone");
        return zone;
    }

    /** @param onRejected menerima daftar nama file yang ditolak, dipisah koma */
    public static void acceptDrops(
            Node target, Set<String> extensions, Consumer<List<Path>> onAccepted, Consumer<String> onRejected) {
        FileDropZone.install(target, extensions, onAccepted, rejected -> onRejected.accept(names(rejected)));
    }

    public static String names(List<Path> files) {
        return files.stream().map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));
    }

    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** Baris tombol Download (utama) dan Simpan sebagai…. */
    public static HBox downloadButtons(Button download, Button saveAs) {
        download.getStyleClass().add("accent");
        HBox row = new HBox(8, download, saveAs);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    public static <T extends Control> T monospace(T control) {
        control.setStyle(MONOSPACE_STYLE);
        return control;
    }

    /** Node hanya memakan tempat saat terlihat. */
    public static void collapseWhenHidden(Node node) {
        node.managedProperty().bind(node.visibleProperty());
    }
}
