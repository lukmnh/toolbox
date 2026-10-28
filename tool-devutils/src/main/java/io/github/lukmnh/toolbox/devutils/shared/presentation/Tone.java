package io.github.lukmnh.toolbox.devutils.shared.presentation;

/** Nada visual untuk badge dan pesan inline; warnanya dari token tema ({@code -tb-success}, dst). */
public enum Tone {
    NEUTRAL("neutral"),
    INFO("info"),
    SUCCESS("success"),
    WARNING("warning"),
    DANGER("error");

    private final String inlineSuffix;

    Tone(String inlineSuffix) {
        this.inlineSuffix = inlineSuffix;
    }

    String badgeClass() {
        return this == DANGER ? "badge-danger" : "badge-" + inlineSuffix;
    }

    String inlineClass() {
        return "inline-" + inlineSuffix;
    }
}
