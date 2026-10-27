package io.github.lukmnh.toolbox.pdf.split.presentation;

/** Pilihan mode di UI; diterjemahkan ke {@link io.github.lukmnh.toolbox.pdf.split.domain.SplitMode}. */
enum SplitChoice {
    EACH_PAGE("pdf.split.mode.each"),
    RANGES("pdf.split.mode.ranges");

    private final String messageKey;

    SplitChoice(String messageKey) {
        this.messageKey = messageKey;
    }

    String messageKey() {
        return messageKey;
    }
}
