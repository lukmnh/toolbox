package io.github.lukmnh.toolbox.productivity.ambient.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.ambient.domain.NoiseChannel;
import io.github.lukmnh.toolbox.productivity.ambient.domain.SleepTimer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Layout Ambient Sound: kartu pemutaran (play/stop, jeda, status, sleep timer) dan kartu mixer (volume per channel,
 * master, mute). Semua state ada di {@link AmbientSoundService}; view hanya binding dan dilepas di {@link #dispose()}
 * supaya service yang hidup sepanjang app tidak menahan node lama.
 */
final class AmbientView extends VBox {

    private static final double LABEL_COLUMN_WIDTH = 120;
    private static final double VALUE_COLUMN_WIDTH = 48;

    private static final Map<NoiseChannel, String> CHANNEL_LABELS = Map.of(
            NoiseChannel.RAIN, "productivity.ambient.channel.rain",
            NoiseChannel.WHITE, "productivity.ambient.channel.white",
            NoiseChannel.PINK, "productivity.ambient.channel.pink",
            NoiseChannel.BROWN, "productivity.ambient.channel.brown");

    private final AmbientSoundService service;
    private final List<Runnable> releases = new ArrayList<>();
    private final BooleanBinding playing;

    AmbientView(AmbientSoundService service) {
        this.service = service;
        this.playing = service.runningProperty().and(service.stoppingProperty().not());
        VBox page = ToolLayout.page();
        VBox.setVgrow(page, Priority.ALWAYS);
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.PRODUCTIVITY,
                                Messages.get("productivity.ambient.name"),
                                Messages.get("productivity.ambient.hint")),
                        buildPlaybackCard(),
                        buildMixerCard());
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getStyleClass().add("tool-view");
        getChildren().add(scroll);
    }

    void dispose() {
        releases.forEach(Runnable::run);
        releases.clear();
        playing.dispose();
    }

    private VBox buildPlaybackCard() {
        Button playStop = ToolLayout.accentButton("", this::onPlayStop);
        playStop.textProperty()
                .bind(Bindings.when(playing)
                        .then(Messages.get("productivity.ambient.stop"))
                        .otherwise(Messages.get("productivity.ambient.play")));
        Button pause = ToolLayout.button("", service::togglePause);
        pause.textProperty()
                .bind(Bindings.when(service.pausedProperty())
                        .then(Messages.get("productivity.ambient.resume"))
                        .otherwise(Messages.get("productivity.ambient.pause")));
        pause.visibleProperty().bind(playing);
        ToolLayout.collapseWhenHidden(pause);

        Label status = ToolLayout.badge("", Tone.NEUTRAL);
        observe(
                () -> renderStatus(status),
                service.runningProperty(),
                service.stoppingProperty(),
                service.pausedProperty());

        Label remaining = ToolLayout.statText();
        remaining.textProperty().bind(service.sleepRemainingProperty());
        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(service.errorProperty());

        HBox controls = ToolLayout.row(
                playStop,
                pause,
                status,
                ToolLayout.spacer(),
                caption("productivity.ambient.timer"),
                buildTimerChoice(),
                remaining);
        return ToolLayout.card(
                Messages.get("productivity.ambient.playback"),
                List.of(),
                controls,
                error,
                ToolLayout.hint(Messages.get("productivity.ambient.background.hint")));
    }

    private ChoiceBox<SleepTimer> buildTimerChoice() {
        ChoiceBox<SleepTimer> timer = new ChoiceBox<>(FXCollections.observableArrayList(SleepTimer.values()));
        timer.setConverter(ToolLayout.labels(AmbientView::timerLabel));
        link(timer.valueProperty(), service.sleepTimerProperty());
        return timer;
    }

    private VBox buildMixerCard() {
        VBox rows = new VBox(ToolLayout.GAP);
        for (NoiseChannel channel : NoiseChannel.values()) {
            rows.getChildren()
                    .add(volumeRow(Messages.get(CHANNEL_LABELS.get(channel)), service.volumeProperty(channel)));
        }
        CheckBox mute = new CheckBox(Messages.get("productivity.ambient.mute"));
        link(mute.selectedProperty(), service.mutedProperty());
        return ToolLayout.card(
                Messages.get("productivity.ambient.mixer"),
                List.of(mute),
                rows,
                new Separator(),
                volumeRow(Messages.get("productivity.ambient.master"), service.masterVolumeProperty()));
    }

    private HBox volumeRow(String label, DoubleProperty volume) {
        Label name = new Label(label);
        name.getStyleClass().add("field-label");
        name.setMinWidth(LABEL_COLUMN_WIDTH);
        Slider slider = new Slider(0, 1, volume.get());
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.setMaxWidth(Double.MAX_VALUE);
        link(slider.valueProperty(), volume);
        Label value = ToolLayout.statText();
        value.setMinWidth(VALUE_COLUMN_WIDTH);
        value.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.format("productivity.ambient.volume.value", slider.getValue()),
                        slider.valueProperty()));
        return ToolLayout.row(name, slider, value);
    }

    private void onPlayStop() {
        if (playing.get()) {
            service.stop();
        } else {
            service.start();
        }
    }

    private void renderStatus(Label status) {
        if (!service.runningProperty().get()) {
            ToolLayout.update(status, Messages.get("productivity.ambient.status.stopped"), Tone.NEUTRAL);
        } else if (service.stoppingProperty().get()) {
            ToolLayout.update(status, Messages.get("productivity.ambient.status.stopping"), Tone.INFO);
        } else if (service.pausedProperty().get()) {
            ToolLayout.update(status, Messages.get("productivity.ambient.status.paused"), Tone.WARNING);
        } else {
            ToolLayout.update(status, Messages.get("productivity.ambient.status.playing"), Tone.SUCCESS);
        }
    }

    private static String timerLabel(SleepTimer timer) {
        return timer == SleepTimer.OFF
                ? Messages.get("productivity.ambient.timer.off")
                : Messages.format("productivity.ambient.timer.minutes", timer.minutes());
    }

    private static Label caption(String key) {
        Label label = new Label(Messages.get(key));
        label.getStyleClass().add("field-label");
        return label;
    }

    /** Binding dua arah ke property service; dicatat supaya dilepas saat view dibuang. */
    private <T> void link(Property<T> control, Property<T> state) {
        control.bindBidirectional(state);
        releases.add(() -> control.unbindBidirectional(state));
    }

    /** Listener ke property service (hidup sepanjang app) dicatat supaya dilepas saat view dibuang. */
    private void observe(Runnable action, Observable... dependencies) {
        InvalidationListener listener = observable -> action.run();
        for (Observable dependency : dependencies) {
            dependency.addListener(listener);
            releases.add(() -> dependency.removeListener(listener));
        }
        action.run();
    }
}
