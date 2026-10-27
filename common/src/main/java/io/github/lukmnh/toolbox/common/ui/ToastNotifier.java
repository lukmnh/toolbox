package io.github.lukmnh.toolbox.common.ui;

import java.util.List;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Menampilkan toast di pojok kanan bawah sebuah {@link StackPane} host. */
public final class ToastNotifier implements Notifier {

    private static final Duration SHORT_DURATION = Duration.seconds(4);
    private static final Duration LONG_DURATION = Duration.seconds(8);
    private static final double TOAST_MAX_WIDTH = 420;
    private static final double SPACING = 8;

    private final VBox container = new VBox(SPACING);

    public ToastNotifier(StackPane host) {
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.setPadding(new Insets(16));
        container.setPickOnBounds(false);
        container.setMaxSize(TOAST_MAX_WIDTH + 32, Double.MAX_VALUE);
        StackPane.setAlignment(container, Pos.BOTTOM_RIGHT);
        host.getChildren().add(container);
    }

    @Override
    public void info(String message) {
        show(message, "toast-info", List.of(), SHORT_DURATION);
    }

    @Override
    public void warning(String message) {
        show(message, "toast-warning", List.of(), LONG_DURATION);
    }

    @Override
    public void success(String message, List<NotificationAction> actions) {
        show(message, "toast-success", actions, LONG_DURATION);
    }

    private void show(String message, String styleClass, List<NotificationAction> actions, Duration duration) {
        VBox toast = new VBox(SPACING);
        toast.getStyleClass().addAll("toast", styleClass);
        toast.setMaxWidth(TOAST_MAX_WIDTH);

        Label text = new Label(message);
        text.setWrapText(true);
        toast.getChildren().add(text);
        if (!actions.isEmpty()) {
            toast.getChildren().add(actionRow(actions, toast));
        }
        container.getChildren().add(toast);

        PauseTransition autoHide = new PauseTransition(duration);
        autoHide.setOnFinished(event -> container.getChildren().remove(toast));
        // Jangan hilang saat user sedang mengarahkan mouse ke tombol
        toast.setOnMouseEntered(event -> autoHide.pause());
        toast.setOnMouseExited(event -> autoHide.playFromStart());
        autoHide.play();
    }

    private HBox actionRow(List<NotificationAction> actions, VBox toast) {
        HBox row = new HBox(SPACING);
        row.setAlignment(Pos.CENTER_RIGHT);
        for (NotificationAction action : actions) {
            Button button = new Button(action.label());
            button.getStyleClass().add("toast-action");
            button.setOnAction(event -> {
                container.getChildren().remove(toast);
                action.action().run();
            });
            row.getChildren().add(button);
        }
        return row;
    }
}
