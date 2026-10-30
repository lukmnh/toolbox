package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.nio.file.Path;
import java.util.List;

/** Port penulis CSV jadwal cicilan (library CSV ada di luar batas domain). */
public interface ScheduleCsvWriter {

    /**
     * Tulis tabel sebagai CSV UTF-8 dengan BOM dan baris CRLF supaya langsung terbaca benar di Excel. Setiap sel
     * dinetralisir dari formula injection (Security Requirements #18).
     *
     * @throws ScheduleExportException bila gagal ditulis
     */
    void write(Path target, List<String> header, List<List<String>> rows);
}
