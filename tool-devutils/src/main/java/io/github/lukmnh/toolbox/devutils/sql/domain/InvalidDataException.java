package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.io.Serial;
import java.util.Objects;

/** File data SQL Generator tidak bisa dipakai; {@link Problem} menentukan pesan untuk user. */
public final class InvalidDataException extends SqlToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Problem {
        /** {@code detail} = ekstensi file. */
        UNSUPPORTED_FORMAT,
        UNREADABLE,
        NO_HEADER,
        /** {@code detail} = nomor kolom (1-based). */
        BLANK_COLUMN,
        /** {@code detail} = nama kolom. */
        DUPLICATE_COLUMN,
        TOO_MANY_FIELDS,
        NOT_OBJECTS,
        /** {@code detail} = batas baris. */
        TOO_MANY_ROWS,
        NO_ROWS
    }

    /** Dipakai bila error tidak terkait baris tertentu. */
    public static final long NO_LINE = 0;

    private final String fileName;
    private final Problem problem;
    private final long line;
    private final String detail;

    public InvalidDataException(String fileName, Problem problem, long line, String detail, Throwable cause) {
        super("Invalid data in " + fileName + ": " + problem, cause);
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.problem = Objects.requireNonNull(problem, "problem");
        this.line = line;
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public InvalidDataException(String fileName, Problem problem, long line, String detail) {
        this(fileName, problem, line, detail, null);
    }

    public String fileName() {
        return fileName;
    }

    public Problem problem() {
        return problem;
    }

    /** Nomor baris di file (1-based), atau {@link #NO_LINE}. */
    public long line() {
        return line;
    }

    public String detail() {
        return detail;
    }
}
