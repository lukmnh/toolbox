package io.github.lukmnh.toolbox.devutils.jwt.presentation;

/** Kolom key yang ditampilkan di card verifikasi, sesuai keluarga {@code alg}. */
enum KeyMode {
    /** HS*: secret lewat {@code PasswordField}. */
    SECRET,
    /** RS*, PS*, ES*, EdDSA: PEM/sertifikat/JWK. */
    PUBLIC_KEY,
    /** Belum ada token, {@code alg: none}, atau algoritma tidak dikenal. */
    UNAVAILABLE
}
