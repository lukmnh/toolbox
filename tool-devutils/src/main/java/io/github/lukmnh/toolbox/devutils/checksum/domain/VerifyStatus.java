package io.github.lukmnh.toolbox.devutils.checksum.domain;

/** Status verifikasi satu entry file daftar checksum. */
public enum VerifyStatus {
    OK,
    FAILED,
    NOT_FOUND,
    /** Path absolut atau keluar folder ({@code ../}); file tidak pernah dibaca (Security Requirements #6). */
    REJECTED
}
