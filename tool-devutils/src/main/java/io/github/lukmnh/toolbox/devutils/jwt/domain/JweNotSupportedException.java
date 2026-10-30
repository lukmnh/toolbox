package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;

/** Token 5 bagian adalah JWE (terenkripsi); isinya tidak bisa dibaca tanpa private key, jadi tidak didukung. */
public final class JweNotSupportedException extends JwtToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public JweNotSupportedException() {
        super("JWE is not supported");
    }
}
