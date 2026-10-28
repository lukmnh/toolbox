package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import java.util.Optional;

/** Pilihan algoritma di form verifikasi: Otomatis (deteksi dari panjang checksum) atau algoritma tertentu. */
enum AlgorithmChoice {
    AUTO(null),
    SHA_256(ChecksumAlgorithm.SHA_256),
    SHA_1(ChecksumAlgorithm.SHA_1),
    SHA_512(ChecksumAlgorithm.SHA_512),
    MD5(ChecksumAlgorithm.MD5),
    CRC32(ChecksumAlgorithm.CRC32);

    private final ChecksumAlgorithm algorithm;

    AlgorithmChoice(ChecksumAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /** Kosong untuk {@link #AUTO}. */
    Optional<ChecksumAlgorithm> algorithm() {
        return Optional.ofNullable(algorithm);
    }

    String label() {
        return algorithm()
                .map(ChecksumAlgorithm::label)
                .orElseGet(() -> Messages.get("devutils.checksum.algorithm.auto"));
    }
}
