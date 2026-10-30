package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.List;

/**
 * Port penyimpanan budget: satu file per budget, ditulis atomic dan owner-only (Security Requirements #19).
 * Semua error dilempar sebagai {@link BudgetException}. Implementasi harus aman dipanggil dari beberapa thread.
 */
public interface BudgetRepository {

    /** Semua budget yang tersimpan; file rusak dilewati (dicatat ke log tanpa isi). */
    List<BudgetEntry> list();

    Budget load(String id);

    /** Simpan sebagai file baru dengan nama file unik dari nama budget. */
    BudgetEntry create(Budget budget);

    /** Timpa file budget {@code id} secara atomic. */
    void save(String id, Budget budget);

    /** Tulis {@code renamed} ke file bernama baru lalu hapus file lama. */
    BudgetEntry rename(String id, Budget renamed);

    void delete(String id);
}
