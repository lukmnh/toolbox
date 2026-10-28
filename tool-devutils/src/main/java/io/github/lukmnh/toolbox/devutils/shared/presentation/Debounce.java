package io.github.lukmnh.toolbox.devutils.shared.presentation;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/** Menunda aksi sampai input berhenti berubah selama {@code delay}. Dipakai di FX thread. */
public final class Debounce {

    private final PauseTransition pause;

    public Debounce(Duration delay, Runnable action) {
        pause = new PauseTransition(delay);
        pause.setOnFinished(event -> action.run());
    }

    public void trigger() {
        pause.playFromStart();
    }

    public void stop() {
        pause.stop();
    }
}
