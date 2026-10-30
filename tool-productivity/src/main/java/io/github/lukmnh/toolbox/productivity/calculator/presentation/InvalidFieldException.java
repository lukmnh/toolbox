package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import java.io.Serial;

/** Teks field bukan angka yang valid. Pesan sudah berbahasa Indonesia dan ditampilkan inline. */
final class InvalidFieldException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    InvalidFieldException(String message) {
        super(message);
    }
}
