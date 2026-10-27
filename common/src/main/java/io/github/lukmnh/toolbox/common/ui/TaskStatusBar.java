package io.github.lukmnh.toolbox.common.ui;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Progress bar + tombol Batal yang hanya tampil selama task berjalan. */
public final class TaskStatusBar extends HBox {

    private static final double SPACING = 8;

    private final ProgressBar progressBar = new ProgressBar();
    private final Button cancelButton = new Button(Messages.get("common.cancel"));

    public TaskStatusBar() {
        super(SPACING);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("task-status-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        getChildren().addAll(progressBar, cancelButton);
        managedProperty().bind(visibleProperty());
        setVisible(false);
    }

    /** Tampilkan progress task ini; bar otomatis tersembunyi saat task selesai. */
    public void track(RunningTask task) {
        progressBar.progressProperty().bind(task.progressProperty());
        visibleProperty().bind(task.runningProperty());
        cancelButton.setOnAction(event -> task.cancel());
    }
}
