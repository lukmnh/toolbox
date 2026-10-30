package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.nio.file.Path;

/** Port export/import JSON budget untuk backup atau pindah laptop. */
public interface BudgetTransfer {

    void exportJson(Budget budget, Path target);

    /**
     * Baca dan validasi file import: ukuran dicek sebelum dibaca, lalu skema, angka tidak negatif, dan jumlah item.
     *
     * @throws BudgetFileTooLargeException bila melebihi batas ukuran
     * @throws InvalidBudgetFileException bila skema tidak valid
     * @throws BudgetLimitExceededException bila jumlah item melebihi batas
     */
    Budget importJson(Path source);
}
