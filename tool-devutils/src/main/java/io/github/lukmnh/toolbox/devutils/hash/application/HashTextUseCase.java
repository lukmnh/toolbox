package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HashAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.TextHasher;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Hash satu teks (UTF-8) dengan semua {@link HashAlgorithm} sekaligus. */
public final class HashTextUseCase {

    private final TextHasher hasher;

    public HashTextUseCase(TextHasher hasher) {
        this.hasher = hasher;
    }

    /** @return hasil sesuai urutan {@link HashAlgorithm#values()} */
    public List<DigestResult> execute(String text, CancellationToken cancellation) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        List<DigestResult> results = new ArrayList<>();
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            // Teks besar butuh waktu per algoritma; ketikan baru membatalkan perhitungan lama
            cancellation.throwIfCancelled();
            results.add(hasher.digest(algorithm, input));
        }
        return List.copyOf(results);
    }
}
