package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;
import java.util.Objects;

/** Key/secret yang diberikan user tidak bisa dibaca atau dipakai. */
public final class UnreadableKeyException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Alasan, dipetakan presentation ke pesan bahasa Indonesia. */
    public enum Reason {
        EMPTY,
        UNRECOGNIZED_FORMAT,
        PRIVATE_KEY_GIVEN,
        UNSUPPORTED_KEY_TYPE,
        INVALID_PEM,
        INVALID_CERTIFICATE,
        INVALID_JWK,
        SYMMETRIC_JWK,
        INVALID_BASE64_SECRET,
        UNUSABLE_KEY
    }

    private final Reason reason;

    public UnreadableKeyException(Reason reason) {
        super("Key tidak bisa dibaca: " + reason);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public UnreadableKeyException(Reason reason, Throwable cause) {
        super("Key tidak bisa dibaca: " + reason, cause);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
