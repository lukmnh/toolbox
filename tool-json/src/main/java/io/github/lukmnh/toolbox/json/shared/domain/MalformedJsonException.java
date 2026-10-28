package io.github.lukmnh.toolbox.json.shared.domain;

import java.io.Serial;
import java.util.Optional;

/** Input bukan JSON yang valid. Posisi {@link #line()}/{@link #column()} dimulai dari 1. */
public final class MalformedJsonException extends JsonToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Kind {
        /** Tidak ada nilai JSON sama sekali. */
        EMPTY,
        /** Masih ada konten setelah nilai root selesai. */
        TRAILING_CONTENT,
        /** Kesalahan sintaks; detail teknis ada di {@link #detail()}. */
        SYNTAX,
        /** Melebihi batas keamanan parser (nesting, panjang string/angka). */
        LIMIT_EXCEEDED
    }

    private final Kind kind;
    private final String sourceName;
    private final long line;
    private final long column;
    private final ErrorSnippet snippet;
    private final String detail;

    /** @param sourceName nama file, atau {@code null} untuk teks yang di-paste */
    public MalformedJsonException(
            Kind kind, String sourceName, long line, long column, ErrorSnippet snippet, String detail) {
        super("JSON tidak valid (" + kind + ") di " + line + ":" + column + ": " + detail, null);
        this.kind = kind;
        this.sourceName = sourceName;
        this.line = line;
        this.column = column;
        this.snippet = snippet;
        this.detail = detail;
    }

    public Kind kind() {
        return kind;
    }

    public Optional<String> sourceName() {
        return Optional.ofNullable(sourceName);
    }

    public long line() {
        return line;
    }

    public long column() {
        return column;
    }

    public ErrorSnippet snippet() {
        return snippet;
    }

    /** Pesan teknis dari parser (bahasa Inggris), ditampilkan sebagai info tambahan. */
    public String detail() {
        return detail;
    }
}
