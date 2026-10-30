package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.io.Serial;

/** Urutan {@code %} yang rusak saat URL decode, misalnya {@code %G1} atau {@code %} di akhir teks. */
public final class InvalidPercentEncodingException extends EncoderException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int position;
    private final String sequence;

    /** @param position indeks 0-based karakter {@code %} di input */
    public InvalidPercentEncodingException(int position, String sequence) {
        super("Malformed percent-encoding at position " + position, null);
        this.position = position;
        this.sequence = sequence;
    }

    public int position() {
        return position;
    }

    /** Potongan input mulai dari {@code %}, maksimal 3 karakter. */
    public String sequence() {
        return sequence;
    }
}
