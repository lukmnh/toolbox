package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Total satu daftar, hanya dari item aktif.
 *
 * @param totalPrice jumlah harga sekali beli
 * @param totalMonthly jumlah rata-rata per bulan (presisi penuh)
 * @param missingDurations jumlah item aktif yang durasinya belum diisi
 */
public record SheetTotals(
        String sheetId, String sheetName, BigDecimal totalPrice, BigDecimal totalMonthly, int missingDurations) {

    public SheetTotals {
        Objects.requireNonNull(sheetId, "sheetId");
        Objects.requireNonNull(sheetName, "sheetName");
        Objects.requireNonNull(totalPrice, "totalPrice");
        Objects.requireNonNull(totalMonthly, "totalMonthly");
    }
}
