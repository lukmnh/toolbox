package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.io.Serial;

public final class ScheduleExportException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public ScheduleExportException(String fileName, Throwable cause) {
        super("Gagal menulis jadwal cicilan " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
