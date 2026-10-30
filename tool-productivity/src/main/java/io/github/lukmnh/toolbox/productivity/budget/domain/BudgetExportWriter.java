package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.nio.file.Path;

/**
 * Port penulis file export ({@code .xlsx} atau {@code .pdf}). Setiap file menyimpan salinan data budget yang bisa
 * di-import ulang lewat {@link BudgetImporter}. Teks dari user selalu ditulis sebagai teks biasa, formula Excel hanya
 * dibangun dari referensi sel (Security Requirements #18).
 */
public interface BudgetExportWriter {

    /** @throws BudgetStorageException bila file gagal ditulis */
    void write(Budget budget, WorkbookLabels labels, Path target);
}
