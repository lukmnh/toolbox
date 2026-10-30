package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.devutils.hash.application.ExportPemUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateKeyPairUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateSecretUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.GenerateUuidsUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HashTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HmacTextUseCase;
import java.util.Objects;

/** Kumpulan use case Hash Generator (tab Hash/HMAC dan Generate), dirakit di composition root. */
public record HashUseCases(
        HashTextUseCase hash,
        HmacTextUseCase hmac,
        GenerateSecretUseCase secret,
        GenerateKeyPairUseCase keyPair,
        GenerateUuidsUseCase uuids,
        ExportPemUseCase exportPem) {

    public HashUseCases {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(hmac, "hmac");
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(keyPair, "keyPair");
        Objects.requireNonNull(uuids, "uuids");
        Objects.requireNonNull(exportPem, "exportPem");
    }
}
