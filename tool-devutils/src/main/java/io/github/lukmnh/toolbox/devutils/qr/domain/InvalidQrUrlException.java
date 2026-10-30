package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.io.Serial;

/** URL bukan http/https absolut dengan host. URL user tidak dimasukkan ke pesan (bisa berisi token). */
public final class InvalidQrUrlException extends QrToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidQrUrlException(Throwable cause) {
        super("URL tidak valid", cause);
    }
}
