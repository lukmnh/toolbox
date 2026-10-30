package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.nio.file.Path;

/** Port import budget dari file export Toolbox ({@code .xlsx} atau {@code .pdf}) yang membawa data budget. */
public interface BudgetImporter {

    /**
     * Baca dan validasi data budget yang tertanam di file export: ukuran file dan data dicek sebelum/selama dibaca,
     * lalu skema, angka tidak negatif, dan jumlah item.
     *
     * @throws BudgetFileTooLargeException bila file atau data di dalamnya melebihi batas ukuran
     * @throws InvalidBudgetFileException bila file bukan export Toolbox, rusak, atau skemanya tidak valid
     * @throws BudgetLimitExceededException bila jumlah item melebihi batas
     * @throws BudgetStorageException bila file gagal dibaca
     */
    Budget read(Path source);
}
