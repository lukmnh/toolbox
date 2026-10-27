package io.github.lukmnh.toolbox.common.ui;

import java.util.Objects;

/** Tombol di dalam toast, misalnya "Buka file". */
public record NotificationAction(String label, Runnable action) {

    public NotificationAction {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
    }
}
