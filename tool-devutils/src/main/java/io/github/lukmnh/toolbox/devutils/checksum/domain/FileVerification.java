package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.util.Objects;

/** Hasil verifikasi satu file terhadap checksum yang ditempel user. */
public record FileVerification(String fileName, ChecksumAlgorithm algorithm, String expected, String actual) {

    public FileVerification {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
    }

    public boolean matches() {
        return expected.equals(actual);
    }
}
