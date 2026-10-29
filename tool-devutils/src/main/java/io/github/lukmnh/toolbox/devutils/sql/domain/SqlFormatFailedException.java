package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.io.Serial;

/** Library formatter melempar error untuk sebuah statement; pesan library tidak diteruskan karena memuat isi SQL. */
public final class SqlFormatFailedException extends SqlToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int statementNumber;

    public SqlFormatFailedException(int statementNumber, Throwable cause) {
        super("Formatter gagal pada statement #" + statementNumber, cause);
        this.statementNumber = statementNumber;
    }

    /** Nomor statement, mulai dari 1. */
    public int statementNumber() {
        return statementNumber;
    }
}
