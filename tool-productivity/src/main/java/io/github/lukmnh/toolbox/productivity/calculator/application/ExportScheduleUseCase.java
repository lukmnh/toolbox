package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.AmortizationRow;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentSchedule;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ScheduleCsvWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

/**
 * Ekspor jadwal cicilan ke CSV: kolom bulan, cicilan, pokok, bunga, sisa pokok. Nominal ditulis sebagai angka polos
 * (tanpa {@code Rp} dan pemisah ribuan) supaya bisa langsung dihitung di spreadsheet.
 */
public final class ExportScheduleUseCase {

    public static final int COLUMN_COUNT = 5;

    private final ScheduleCsvWriter writer;

    public ExportScheduleUseCase(ScheduleCsvWriter writer) {
        this.writer = writer;
    }

    /** @param header judul {@link #COLUMN_COUNT} kolom sesuai urutan di atas */
    public void execute(InstallmentSchedule schedule, List<String> header, Path target) {
        if (header.size() != COLUMN_COUNT) {
            throw new IllegalArgumentException("Schedule header must have " + COLUMN_COUNT + " columns");
        }
        List<List<String>> rows =
                schedule.rows().stream().map(ExportScheduleUseCase::cells).toList();
        writer.write(target, header, rows);
    }

    private static List<String> cells(AmortizationRow row) {
        return List.of(
                String.valueOf(row.month()),
                plain(row.payment()),
                plain(row.principalPart()),
                plain(row.interest()),
                plain(row.remaining()));
    }

    private static String plain(BigDecimal value) {
        return value.toPlainString();
    }
}
