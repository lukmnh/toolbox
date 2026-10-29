package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/** Pilihan Builder; tiap preset divalidasi di compact constructor. */
public sealed interface CronPreset {

    int MAX_MINUTE = 59;
    int MAX_DAY_OF_MONTH = 31;

    record EveryMinutes(int minutes) implements CronPreset {

        public EveryMinutes {
            requireRange(minutes, 1, MAX_MINUTE, "minutes");
        }
    }

    record Hourly(int minute) implements CronPreset {

        public Hourly {
            requireRange(minute, 0, MAX_MINUTE, "minute");
        }
    }

    record Daily(LocalTime time) implements CronPreset {

        public Daily {
            Objects.requireNonNull(time, "time");
        }
    }

    /** Senin–Jumat. */
    record Weekdays(LocalTime time) implements CronPreset {

        public Weekdays {
            Objects.requireNonNull(time, "time");
        }
    }

    record Weekly(DayOfWeek day, LocalTime time) implements CronPreset {

        public Weekly {
            Objects.requireNonNull(day, "day");
            Objects.requireNonNull(time, "time");
        }
    }

    record Monthly(int dayOfMonth, LocalTime time) implements CronPreset {

        public Monthly {
            requireRange(dayOfMonth, 1, MAX_DAY_OF_MONTH, "dayOfMonth");
            Objects.requireNonNull(time, "time");
        }
    }

    /** Isi tiap field bebas; kosong dianggap {@code *}. */
    record Custom(String minute, String hour, String dayOfMonth, String month, String dayOfWeek) implements CronPreset {

        public Custom {
            Objects.requireNonNull(minute, "minute");
            Objects.requireNonNull(hour, "hour");
            Objects.requireNonNull(dayOfMonth, "dayOfMonth");
            Objects.requireNonNull(month, "month");
            Objects.requireNonNull(dayOfWeek, "dayOfWeek");
        }
    }

    private static void requireRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " harus " + min + "–" + max);
        }
    }
}
