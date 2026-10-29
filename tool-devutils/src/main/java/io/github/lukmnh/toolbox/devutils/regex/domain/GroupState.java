package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Hitungan grup yang dibutuhkan untuk menafsirkan escape angka ({@code \\1} backreference vs octal). */
final class GroupState {

    private final GroupPrescan prescan;
    private int opened;

    GroupState(GroupPrescan prescan) {
        this.prescan = prescan;
    }

    /** Nomor untuk grup capture berikutnya. */
    int openNext() {
        return ++opened;
    }

    /** Grup capture yang sudah dibuka sampai posisi sekarang (Java, PCRE). */
    int openedSoFar() {
        return opened;
    }

    /** Grup capture di seluruh pola (JavaScript). */
    int total() {
        return prescan.captureCount();
    }

    boolean hasNamedGroups() {
        return prescan.hasNamedGroups();
    }
}
