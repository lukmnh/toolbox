package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;
import java.util.Objects;

/** Token tidak bisa di-decode; {@link #part()} menunjuk bagian yang rusak supaya user tahu harus cek di mana. */
public final class MalformedJwtException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Alasan kerusakan, dipetakan presentation ke pesan bahasa Indonesia. */
    public enum Reason {
        EMPTY_TOKEN,
        WRONG_PART_COUNT,
        EMPTY_PART,
        INVALID_BASE64URL,
        NOT_UTF8,
        INVALID_JSON,
        NOT_JSON_OBJECT
    }

    private final JwtPart part;
    private final Reason reason;
    private final String detail;

    /** @param detail info tambahan untuk user (misalnya jumlah bagian atau posisi error JSON); boleh kosong */
    public MalformedJwtException(JwtPart part, Reason reason, String detail) {
        super(part + ": " + reason);
        this.part = Objects.requireNonNull(part, "part");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public MalformedJwtException(JwtPart part, Reason reason) {
        this(part, reason, "");
    }

    public JwtPart part() {
        return part;
    }

    public Reason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }
}
