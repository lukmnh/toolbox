package io.github.lukmnh.toolbox.devutils.qr.presentation;

/** Jenis isi QR. */
enum QrMode {
    TEXT("devutils.qr.mode.text", "devutils.qr.empty.text"),
    URL("devutils.qr.mode.url", "devutils.qr.empty.url"),
    VCARD("devutils.qr.mode.vcard", "devutils.qr.empty.vcard");

    private final String labelKey;
    private final String emptyKey;

    QrMode(String labelKey, String emptyKey) {
        this.labelKey = labelKey;
        this.emptyKey = emptyKey;
    }

    String labelKey() {
        return labelKey;
    }

    /** Petunjuk saat isi mode ini belum diisi. */
    String emptyKey() {
        return emptyKey;
    }
}
