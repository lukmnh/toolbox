package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.nio.file.Path;

/**
 * Port penulis {@code .xlsx}: sheet Ringkasan + satu sheet per daftar, dengan formula Excel. Teks dari user selalu
 * ditulis sebagai sel string, formula hanya dibangun dari referensi sel (Security Requirements #18).
 */
public interface BudgetWorkbookWriter {

    /** @throws BudgetStorageException bila file gagal ditulis */
    void write(Budget budget, WorkbookLabels labels, Path target);
}
