package io.github.lukmnh.toolbox.productivity.ambient.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.BackgroundService;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.productivity.ambient.application.AmbientPlayer;
import io.github.lukmnh.toolbox.productivity.ambient.domain.AudioDeviceUnavailableException;
import io.github.lukmnh.toolbox.productivity.ambient.domain.MixLevels;
import io.github.lukmnh.toolbox.productivity.ambient.domain.NoiseChannel;
import io.github.lukmnh.toolbox.productivity.ambient.domain.SleepTimer;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/**
 * Ambient Sound sebagai background service: dibuat sekali di composition root, dipakai bersama oleh tool view dan
 * mini-player header, dan tetap jalan saat user pindah tool. Semua state berupa JavaFX property yang hanya diubah di FX
 * thread; perubahan diteruskan ke {@link AmbientPlayer} (thread audio) lewat field volatile.
 */
public final class AmbientSoundService implements BackgroundService {

    private static final Logger LOG = System.getLogger(AmbientSoundService.class.getName());
    private static final javafx.util.Duration COUNTDOWN_TICK = javafx.util.Duration.seconds(1);

    private final AmbientPlayer player;
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper stopping = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper paused = new ReadOnlyBooleanWrapper(false);
    private final Map<NoiseChannel, DoubleProperty> volumes = new EnumMap<>(NoiseChannel.class);
    private final DoubleProperty masterVolume = new SimpleDoubleProperty(MixLevels.DEFAULT.master());
    private final BooleanProperty muted = new SimpleBooleanProperty(MixLevels.DEFAULT.muted());
    private final ObjectProperty<SleepTimer> sleepTimer = new SimpleObjectProperty<>(SleepTimer.OFF);
    private final ReadOnlyStringWrapper sleepRemaining = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper error = new ReadOnlyStringWrapper("");
    /** Dibuat lazy: Timeline butuh toolkit JavaFX dan baru perlu saat sleep timer aktif. */
    private Timeline countdown;
    /** Diambil dari context shell saat header dibangun; dipakai untuk toast error saat user sedang di tool lain. */
    private Notifier notifier;

    public AmbientSoundService(AmbientPlayer player) {
        this.player = player;
        for (NoiseChannel channel : NoiseChannel.values()) {
            DoubleProperty volume = new SimpleDoubleProperty(MixLevels.DEFAULT.volume(channel));
            volume.addListener((observable, previous, current) -> pushLevels());
            volumes.put(channel, volume);
        }
        masterVolume.addListener((observable, previous, current) -> pushLevels());
        muted.addListener((observable, previous, current) -> pushLevels());
        sleepTimer.addListener((observable, previous, current) -> onSleepTimerChanged());
        pushLevels();
    }

    @Override
    public String id() {
        return ProductivityToolIds.AMBIENT;
    }

    /** Putar (fade in). Juga melanjutkan sesi yang sedang fade out atau dijeda. */
    @Override
    public void start() {
        error.set("");
        player.start(new PlaybackListener());
        running.set(true);
        stopping.set(false);
        paused.set(false);
        refreshCountdown();
    }

    /** Fade out 2 detik lalu berhenti; {@link #runningProperty()} jadi {@code false} setelah thread audio selesai. */
    @Override
    public void stop() {
        player.stop();
        stopping.set(running.get());
    }

    /**
     * Jeda = fade out ke senyap tanpa menghentikan service. Dipilih (bukan stop) karena mini-player header hanya tampil
     * selama service jalan; dengan begini tombol lanjut tetap ada di header dan lanjut terasa instan tanpa membuka
     * perangkat audio lagi. Selama senyap mixer melewati generator, jadi CPU nyaris nol.
     */
    public void togglePause() {
        if (!running.get() || stopping.get()) {
            return;
        }
        paused.set(!paused.get());
        player.setPaused(paused.get());
    }

    @Override
    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    @Override
    public Node statusIndicator(ToolContext context) {
        notifier = context.notifier();
        return new AmbientMiniPlayer(this);
    }

    /** {@code true} selama fade out setelah stop (service masih jalan). */
    public ReadOnlyBooleanProperty stoppingProperty() {
        return stopping.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty pausedProperty() {
        return paused.getReadOnlyProperty();
    }

    /** Posisi slider [0, 1]. */
    public DoubleProperty volumeProperty(NoiseChannel channel) {
        return volumes.get(channel);
    }

    public DoubleProperty masterVolumeProperty() {
        return masterVolume;
    }

    public BooleanProperty mutedProperty() {
        return muted;
    }

    public ObjectProperty<SleepTimer> sleepTimerProperty() {
        return sleepTimer;
    }

    /** Teks sisa waktu sleep timer; kosong bila timer mati atau service berhenti. */
    public ReadOnlyStringProperty sleepRemainingProperty() {
        return sleepRemaining.getReadOnlyProperty();
    }

    /** Pesan error terakhir untuk user; kosong bila tidak ada. */
    public ReadOnlyStringProperty errorProperty() {
        return error.getReadOnlyProperty();
    }

    private void pushLevels() {
        player.setLevels(new MixLevels(
                level(volumes.get(NoiseChannel.WHITE)),
                level(volumes.get(NoiseChannel.PINK)),
                level(volumes.get(NoiseChannel.BROWN)),
                level(volumes.get(NoiseChannel.RAIN)),
                level(masterVolume),
                muted.get()));
    }

    /** Property publik bisa di-set di luar slider; nilai di luar rentang dijepit, bukan exception di listener. */
    private static double level(DoubleProperty property) {
        return Math.clamp(property.get(), 0.0, 1.0);
    }

    private SleepTimer currentTimer() {
        // ChoiceBox bisa mengosongkan value; dianggap timer mati
        return sleepTimer.get() == null ? SleepTimer.OFF : sleepTimer.get();
    }

    private void onSleepTimerChanged() {
        player.setSleepTimer(currentTimer());
        refreshCountdown();
    }

    private void refreshCountdown() {
        if (!running.get() || currentTimer() == SleepTimer.OFF) {
            if (countdown != null) {
                countdown.stop();
            }
            sleepRemaining.set("");
            return;
        }
        updateRemaining();
        countdown().play();
    }

    private Timeline countdown() {
        if (countdown == null) {
            countdown = new Timeline(new KeyFrame(COUNTDOWN_TICK, event -> updateRemaining()));
            countdown.setCycleCount(Animation.INDEFINITE);
        }
        return countdown;
    }

    private void updateRemaining() {
        sleepRemaining.set(player.sleepRemaining()
                .map(AmbientSoundService::formatRemaining)
                .orElse(""));
    }

    private static String formatRemaining(Duration remaining) {
        String clock = String.format(Locale.ROOT, "%d:%02d", remaining.toMinutes(), remaining.toSecondsPart());
        return Messages.format("productivity.ambient.timer.remaining", clock);
    }

    /** Callback dari sesi lama yang datang setelah sesi baru dimulai diabaikan lewat {@code player.isActive()}. */
    private void onSessionEnded() {
        if (player.isActive()) {
            return;
        }
        running.set(false);
        stopping.set(false);
        paused.set(false);
        player.setPaused(false);
        refreshCountdown();
    }

    private void onSessionFailed(RuntimeException failure) {
        // Hanya tipe error: tidak ada konten user di audio, tapi pesan library bisa berisi detail sistem (#5)
        LOG.log(
                Level.WARNING,
                "Ambient sound berhenti karena " + failure.getClass().getName());
        if (player.isActive()) {
            return;
        }
        String message = Messages.get(
                failure instanceof AudioDeviceUnavailableException
                        ? "productivity.ambient.error.device"
                        : "productivity.ambient.error.playback");
        error.set(message);
        onSessionEnded();
        if (notifier != null) {
            notifier.warning(message);
        }
    }

    /** Dipanggil dari thread audio; semua perubahan state dipindah ke FX thread. */
    private final class PlaybackListener implements AmbientPlayer.Listener {

        @Override
        public void onStopped() {
            Platform.runLater(AmbientSoundService.this::onSessionEnded);
        }

        @Override
        public void onFailed(RuntimeException error) {
            Platform.runLater(() -> onSessionFailed(error));
        }
    }
}
