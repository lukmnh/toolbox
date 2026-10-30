package io.github.lukmnh.toolbox.productivity.ambient.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Mini-player di header shell: ♪, play/pause, volume master, sisa sleep timer, dan stop. Shell menampilkannya hanya
 * selama service jalan. Tidak ada tombol "buka tool" karena {@code ToolOpener} saat ini wajib menerima file.
 */
final class AmbientMiniPlayer extends HBox {

    private static final double VOLUME_WIDTH = 80;
    private static final double SPACING = 6;

    AmbientMiniPlayer(AmbientSoundService service) {
        super(SPACING);
        Label icon = new Label(Messages.get("productivity.ambient.indicator.icon"));
        icon.setTooltip(new Tooltip(Messages.get("productivity.ambient.name")));

        Button pause = ToolLayout.button("", service::togglePause);
        pause.textProperty()
                .bind(Bindings.when(service.pausedProperty())
                        .then(Messages.get("productivity.ambient.indicator.play"))
                        .otherwise(Messages.get("productivity.ambient.indicator.pause")));
        Tooltip pauseTip = new Tooltip();
        pauseTip.textProperty()
                .bind(Bindings.when(service.pausedProperty())
                        .then(Messages.get("productivity.ambient.resume"))
                        .otherwise(Messages.get("productivity.ambient.pause")));
        pause.setTooltip(pauseTip);
        pause.disableProperty().bind(service.stoppingProperty());

        Slider volume = new Slider(0, 1, service.masterVolumeProperty().get());
        volume.setPrefWidth(VOLUME_WIDTH);
        volume.valueProperty().bindBidirectional(service.masterVolumeProperty());
        volume.setTooltip(new Tooltip(Messages.get("productivity.ambient.master")));

        Label remaining = ToolLayout.statText();
        remaining.textProperty().bind(service.sleepRemainingProperty());
        remaining.visibleProperty().bind(remaining.textProperty().isNotEmpty());
        ToolLayout.collapseWhenHidden(remaining);

        Button stop = ToolLayout.button(Messages.get("productivity.ambient.indicator.stop"), service::stop);
        stop.setTooltip(new Tooltip(Messages.get("productivity.ambient.stop")));
        stop.disableProperty().bind(service.stoppingProperty());

        getChildren().addAll(icon, pause, volume, remaining, stop);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("ambient-indicator");
    }
}
