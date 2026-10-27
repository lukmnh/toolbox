package io.github.lukmnh.toolbox.pdf.sign.presentation;

/** Mode klik di kanvas halaman. */
enum EditMode {
    SELECT("pdf.sign.mode.select"),
    TEXT("pdf.sign.mode.text"),
    SIGNATURE("pdf.sign.mode.signature");

    private final String messageKey;

    EditMode(String messageKey) {
        this.messageKey = messageKey;
    }

    String messageKey() {
        return messageKey;
    }
}
