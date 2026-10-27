package io.github.lukmnh.toolbox.json.shared.domain;

/**
 * Callback progress untuk proses streaming di infrastructure. Pemanggil boleh melempar exception dari sini
 * untuk membatalkan proses (dipakai use case untuk meneruskan pembatalan).
 */
@FunctionalInterface
public interface ScanProgress {

    ScanProgress NONE = (done, total) -> {};

    void update(long done, long total);
}
