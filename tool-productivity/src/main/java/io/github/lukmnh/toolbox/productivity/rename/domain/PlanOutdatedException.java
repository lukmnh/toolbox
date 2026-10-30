package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.io.Serial;

/** Isi folder berubah setelah preview, sehingga rencana rename tidak lagi aman dijalankan. */
public final class PlanOutdatedException extends RenameException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PlanOutdatedException() {
        super("Preview sudah tidak sesuai isi folder", null);
    }
}
