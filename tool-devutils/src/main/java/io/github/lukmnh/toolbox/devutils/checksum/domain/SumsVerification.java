package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.util.Objects;

/**
 * Hasil verifikasi satu entry. {@code actual} kosong bila file tidak dihitung (ditolak, tidak ditemukan, atau gagal
 * dibaca).
 */
public record SumsVerification(String fileName, VerifyStatus status, String expected, String actual) {

    public SumsVerification {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
    }
}
