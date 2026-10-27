package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.io.FileExtensions;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/** Memasang drag-drop file dari file manager ke sebuah node, dengan validasi ekstensi. */
public final class FileDropZone {

    private static final String ACTIVE_STYLE = "drop-active";

    private FileDropZone() {}

    /**
     * @param extensions ekstensi yang diterima (huruf kecil, tanpa titik)
     * @param onAccepted file yang ekstensinya cocok, sesuai urutan drop; tidak dipanggil bila kosong
     * @param onRejected file yang ditolak; tidak dipanggil bila kosong
     */
    public static void install(
            Node target, Set<String> extensions, Consumer<List<Path>> onAccepted, Consumer<List<Path>> onRejected) {
        target.setOnDragOver(event -> acceptFiles(event));
        target.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                target.getStyleClass().add(ACTIVE_STYLE);
            }
        });
        target.setOnDragExited(event -> target.getStyleClass().remove(ACTIVE_STYLE));
        target.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            if (!dragboard.hasFiles()) {
                event.setDropCompleted(false);
                return;
            }
            dispatch(dragboard.getFiles(), extensions, onAccepted, onRejected);
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private static void acceptFiles(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private static void dispatch(
            List<File> files,
            Set<String> extensions,
            Consumer<List<Path>> onAccepted,
            Consumer<List<Path>> onRejected) {
        Map<Boolean, List<Path>> partitioned = files.stream()
                .map(File::toPath)
                .collect(Collectors.partitioningBy(path -> FileExtensions.hasExtension(path, extensions)));
        if (!partitioned.get(true).isEmpty()) {
            onAccepted.accept(partitioned.get(true));
        }
        if (!partitioned.get(false).isEmpty()) {
            onRejected.accept(partitioned.get(false));
        }
    }
}
