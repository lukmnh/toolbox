package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.budget.domain.DurationUnit;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Teks tampilan enum, format angka, dan palet warna Budget. */
final class BudgetLabels {

    private static final String PREFIX = "productivity.budget.";
    /** Durasi ditampilkan sampai 3 desimal (misalnya 1,5 atau 0,25). */
    static final int DURATION_DECIMALS = 3;
    /** Presisi penuh saat edit supaya nilai tidak berubah hanya karena sel dibuka lalu ditutup. */
    private static final int MAX_EDIT_DECIMALS = 6;

    /** Pastel: tetap terbaca di tema terang & gelap karena teks di atasnya selalu gelap. */
    static final List<String> PASTEL_PALETTE = List.of(
            "#A5F3FC", "#FED7AA", "#FBCFE8", "#BBF7D0", "#DDD6FE", "#FDE68A", "#BFDBFE", "#FECACA", "#C7D2FE",
            "#D9F99D");

    static final String UNCATEGORIZED_COLOR = "#CBD5E1";

    private BudgetLabels() {}

    static String unit(DurationUnit unit) {
        return Messages.get(PREFIX + "unit." + unit.name().toLowerCase(Locale.ROOT));
    }

    static String status(ItemStatus status) {
        return Messages.get(PREFIX + "status." + status.name().toLowerCase(Locale.ROOT));
    }

    static String money(BigDecimal amount) {
        return RupiahFormat.format(amount);
    }

    static String moneyCents(BigDecimal amount) {
        return RupiahFormat.format(amount, RupiahFormat.TWO_DECIMALS);
    }

    static String duration(BigDecimal duration) {
        return RupiahFormat.plain(duration, DURATION_DECIMALS);
    }

    /** Teks awal saat sel angka diedit; formatnya terbaca balik oleh {@code IndonesianNumbers}. */
    static String plainNumber(BigDecimal value) {
        return RupiahFormat.plain(value, MAX_EDIT_DECIMALS);
    }

    /** Warna palet berikutnya yang belum dipakai; berputar bila semua sudah dipakai. */
    static String nextPaletteColor(List<String> used) {
        return PASTEL_PALETTE.stream()
                .filter(color -> !used.contains(color))
                .findFirst()
                .orElse(PASTEL_PALETTE.get(used.size() % PASTEL_PALETTE.size()));
    }

    static String paletteColor(int index) {
        return PASTEL_PALETTE.get(index % PASTEL_PALETTE.size());
    }

    static WorkbookLabels workbook() {
        return WorkbookLabels.from(
                label -> Messages.get(PREFIX + "xlsx." + label.name().toLowerCase(Locale.ROOT)));
    }
}
