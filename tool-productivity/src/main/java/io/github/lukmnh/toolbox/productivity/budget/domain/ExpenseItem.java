package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Satu barang/pengeluaran rutin. Nomor urut tidak disimpan: posisinya di {@link ExpenseSheet#items()} adalah
 * urutannya, sehingga geser/hapus baris tidak perlu menomori ulang.
 *
 * @param categoryId id {@link Category} di daftar yang sama, atau kosong bila tanpa kategori
 * @param price harga sekali beli
 * @param duration lama pemakaian dalam {@code unit}; 0 berarti belum diisi
 */
public record ExpenseItem(
        String id,
        String name,
        String brand,
        String netto,
        String categoryId,
        BigDecimal price,
        BigDecimal duration,
        DurationUnit unit,
        ItemStatus status,
        String note) {

    public static final String NO_CATEGORY = "";

    public ExpenseItem {
        id = Ids.require(id, "id item");
        name = Texts.optional(name, "nama item");
        brand = Texts.optional(brand, "brand");
        netto = Texts.optional(netto, "netto");
        categoryId = Texts.optional(categoryId, "kategori");
        price = Texts.nonNegative(price, "harga");
        duration = Texts.nonNegative(duration, "durasi");
        Objects.requireNonNull(unit, "satuan");
        Objects.requireNonNull(status, "status");
        note = Texts.optional(note, "catatan");
    }

    public static ExpenseItem blank(String categoryId) {
        return new ExpenseItem(
                Ids.next(),
                "",
                "",
                "",
                categoryId,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                DurationUnit.DEFAULT,
                ItemStatus.ACTIVE,
                "");
    }

    /** Salinan dengan id baru, untuk duplikat baris. */
    public ExpenseItem copy() {
        return new ExpenseItem(Ids.next(), name, brand, netto, categoryId, price, duration, unit, status, note);
    }

    public BigDecimal monthlyCost() {
        return MonthlyCost.of(price, duration, unit);
    }

    /** Durasi kosong/0: rata-rata tidak bisa dihitung, baris diberi tanda peringatan. */
    public boolean missingDuration() {
        return duration.signum() == 0;
    }

    public boolean hasCategory() {
        return !categoryId.isEmpty();
    }

    public ExpenseItem withName(String value) {
        return new ExpenseItem(id, value, brand, netto, categoryId, price, duration, unit, status, note);
    }

    public ExpenseItem withBrand(String value) {
        return new ExpenseItem(id, name, value, netto, categoryId, price, duration, unit, status, note);
    }

    public ExpenseItem withNetto(String value) {
        return new ExpenseItem(id, name, brand, value, categoryId, price, duration, unit, status, note);
    }

    public ExpenseItem withCategoryId(String value) {
        return new ExpenseItem(id, name, brand, netto, value, price, duration, unit, status, note);
    }

    public ExpenseItem withPrice(BigDecimal value) {
        return new ExpenseItem(id, name, brand, netto, categoryId, value, duration, unit, status, note);
    }

    public ExpenseItem withDuration(BigDecimal value) {
        return new ExpenseItem(id, name, brand, netto, categoryId, price, value, unit, status, note);
    }

    public ExpenseItem withUnit(DurationUnit value) {
        return new ExpenseItem(id, name, brand, netto, categoryId, price, duration, value, status, note);
    }

    public ExpenseItem withStatus(ItemStatus value) {
        return new ExpenseItem(id, name, brand, netto, categoryId, price, duration, unit, value, note);
    }

    public ExpenseItem withNote(String value) {
        return new ExpenseItem(id, name, brand, netto, categoryId, price, duration, unit, status, value);
    }
}
