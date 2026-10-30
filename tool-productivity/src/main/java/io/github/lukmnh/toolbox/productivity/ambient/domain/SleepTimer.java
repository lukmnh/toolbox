package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.time.Duration;
import java.util.Optional;

/** Pilihan sleep timer: setelah durasi habis suara di-fade out lalu berhenti. */
public enum SleepTimer {
    OFF(0),
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_45(45),
    MINUTES_60(60),
    MINUTES_90(90);

    private final int minutes;

    SleepTimer(int minutes) {
        this.minutes = minutes;
    }

    public int minutes() {
        return minutes;
    }

    /** @return kosong untuk {@link #OFF} */
    public Optional<Duration> duration() {
        return this == OFF ? Optional.empty() : Optional.of(Duration.ofMinutes(minutes));
    }
}
