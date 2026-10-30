package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Satu budget: pemasukan bulanan + beberapa daftar pengeluaran. Disimpan sebagai satu file JSON.
 *
 * @param created waktu dibuat
 * @param modified waktu terakhir disimpan
 */
public record Budget(String name, Instant created, Instant modified, List<Income> incomes, List<ExpenseSheet> sheets) {

    public static final int MAX_NAME_LENGTH = Texts.MAX_NAME_LENGTH;

    public Budget {
        name = Texts.requiredName(name, "nama budget");
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(modified, "modified");
        incomes = List.copyOf(incomes);
        sheets = List.copyOf(sheets);
    }

    /** Budget baru berisi satu daftar kosong supaya user langsung bisa mengisi. */
    public static Budget create(String name, String firstSheetName, Instant now) {
        return new Budget(name, now, now, List.of(), List.of(ExpenseSheet.create(firstSheetName)));
    }

    public Budget withName(String newName) {
        return new Budget(newName, created, modified, incomes, sheets);
    }

    public Budget withModified(Instant time) {
        return new Budget(name, created, time, incomes, sheets);
    }

    /** Untuk duplikat/import: dianggap budget baru. */
    public Budget withCreated(Instant time) {
        return new Budget(name, time, time, incomes, sheets);
    }

    public int itemCount() {
        return sheets.stream().mapToInt(sheet -> sheet.items().size()).sum();
    }

    public Optional<ExpenseSheet> sheet(String sheetId) {
        return sheets.stream().filter(sheet -> sheet.id().equals(sheetId)).findFirst();
    }

    public Budget addSheet(ExpenseSheet sheet) {
        return new Budget(name, created, modified, incomes, ListEdits.append(sheets, sheet));
    }

    public Budget updateSheet(String sheetId, UnaryOperator<ExpenseSheet> change) {
        List<ExpenseSheet> changed = ListEdits.replace(sheets, ExpenseSheet::id, sheetId, change);
        return new Budget(name, created, modified, incomes, changed);
    }

    public Budget removeSheet(String sheetId) {
        return new Budget(name, created, modified, incomes, ListEdits.removeIf(sheets, ExpenseSheet::id, sheetId));
    }

    public Budget moveSheet(String sheetId, int delta) {
        return new Budget(name, created, modified, incomes, ListEdits.move(sheets, ExpenseSheet::id, sheetId, delta));
    }

    public Budget addIncome(Income income) {
        return new Budget(name, created, modified, ListEdits.append(incomes, income), sheets);
    }

    public Budget insertIncome(int index, Income income) {
        return new Budget(name, created, modified, ListEdits.insert(incomes, index, income), sheets);
    }

    public Budget updateIncome(String incomeId, UnaryOperator<Income> change) {
        return new Budget(name, created, modified, ListEdits.replace(incomes, Income::id, incomeId, change), sheets);
    }

    public Budget removeIncome(String incomeId) {
        return new Budget(name, created, modified, ListEdits.removeIf(incomes, Income::id, incomeId), sheets);
    }

    public int incomeIndex(String incomeId) {
        return ListEdits.indexOf(incomes, Income::id, incomeId);
    }
}
