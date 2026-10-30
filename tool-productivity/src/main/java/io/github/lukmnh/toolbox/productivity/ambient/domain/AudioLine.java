package io.github.lukmnh.toolbox.productivity.ambient.domain;

/** Line output yang sedang terbuka. Hanya dipakai oleh satu thread (thread audio). */
public interface AudioLine extends AutoCloseable {

    /**
     * Kirim {@code frames} frame dari {@code samples} (stereo interleaved, nilai [-1, 1]). Blok sampai semua data masuk
     * buffer perangkat, sehingga method ini sekaligus mengatur tempo loop audio.
     *
     * @throws IllegalStateException bila line berhenti sebelum semua data tertulis
     */
    void write(float[] samples, int frames);

    /** Tunggu sampai buffer habis diputar supaya ekor fade out tidak terpotong (bunyi klik). */
    void drain();

    @Override
    void close();
}
