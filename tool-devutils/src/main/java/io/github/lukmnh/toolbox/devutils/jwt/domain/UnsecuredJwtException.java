package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;

/** Token {@code alg: none} tidak punya signature, sehingga tidak pernah boleh dianggap valid (Security #20). */
public final class UnsecuredJwtException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnsecuredJwtException() {
        super("alg none is never valid");
    }
}
