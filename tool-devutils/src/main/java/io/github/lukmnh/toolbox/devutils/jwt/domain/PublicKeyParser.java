package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.security.PublicKey;

/** Port pembaca public key dari teks yang ditempel user: PEM public key, sertifikat X.509 PEM, atau JWK JSON. */
@FunctionalInterface
public interface PublicKeyParser {

    /** @throws UnreadableKeyException bila format tidak dikenali atau isinya rusak */
    PublicKey parse(String text);
}
