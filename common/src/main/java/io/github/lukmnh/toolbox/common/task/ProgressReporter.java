package io.github.lukmnh.toolbox.common.task;

/** Melaporkan kemajuan pekerjaan background. Aman dipanggil dari thread mana pun. */
@FunctionalInterface
public interface ProgressReporter {

    ProgressReporter NONE = (done, total) -> {};

    void report(long done, long total);
}
