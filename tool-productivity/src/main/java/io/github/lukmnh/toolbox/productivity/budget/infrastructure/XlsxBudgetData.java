package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.security.ZipGuard;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException.Problem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

/**
 * Data budget (JSON dari {@link BudgetJsonCodec}) di sheet {@value #SHEET_NAME} yang disembunyikan (very hidden),
 * supaya {@code .xlsx} hasil export bisa di-import ulang.
 *
 * <p>Baris 1: penanda format + versi. Baris berikutnya: JSON di-Base64 lalu dipotong per sel (batas Excel 32.767
 * karakter). Base64 dipilih karena tahan normalisasi spasi/baris baru saat file disimpan ulang di Excel, dan potongan
 * tidak pernah membelah karakter.
 */
final class XlsxBudgetData {

    static final String SHEET_NAME = "_toolbox_data";
    static final String FORMAT = "toolbox-budget";
    static final String VERSION = "1";
    /** Di bawah batas 32.767 karakter per sel Excel. */
    static final int CHUNK_LENGTH = 32_000;

    private static final int FORMAT_COLUMN = 0;
    private static final int VERSION_COLUMN = 1;
    private static final int DATA_COLUMN = 0;
    /** File export Toolbox hanya berisi belasan entry; batas ini menolak ZIP aneh lebih awal. */
    private static final int MAX_ZIP_ENTRIES = 10_000;

    private static final String NO_LOCATION = "";

    private XlsxBudgetData() {}

    static void write(Workbook workbook, byte[] json) throws IOException {
        Worksheet sheet = workbook.newWorksheet(SHEET_NAME);
        sheet.setVisibilityState(VisibilityState.VERY_HIDDEN);
        sheet.inlineString(0, FORMAT_COLUMN, FORMAT);
        sheet.inlineString(0, VERSION_COLUMN, VERSION);
        String encoded = Base64.getEncoder().encodeToString(json);
        int row = 1;
        for (int start = 0; start < encoded.length(); start += CHUNK_LENGTH) {
            sheet.inlineString(
                    row++, DATA_COLUMN, encoded.substring(start, Math.min(encoded.length(), start + CHUNK_LENGTH)));
        }
        sheet.finish();
    }

    /**
     * Ambil JSON dari sheet data. Isi ZIP dicek dulu (zip bomb), lalu sheet dibaca streaming dengan batas ukuran.
     *
     * @throws InvalidBudgetFileException bila file rusak atau tidak punya sheet data Toolbox
     * @throws BudgetFileTooLargeException bila isi ZIP atau data budget melebihi batas
     */
    static byte[] read(Path file, String fileName) {
        checkUncompressedSize(file, fileName);
        try (ReadableWorkbook workbook = new ReadableWorkbook(file.toFile())) {
            Sheet sheet = workbook.findSheet(SHEET_NAME)
                    .orElseThrow(() -> invalid(fileName, Problem.NOT_TOOLBOX_EXPORT, null));
            try (Stream<Row> rows = sheet.openStream()) {
                return decode(rows.iterator(), fileName);
            }
        } catch (BudgetException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // Reader melempar beragam runtime exception untuk XML/ZIP rusak; semuanya berarti file tidak terbaca
            throw invalid(fileName, Problem.UNREADABLE_FILE, e);
        }
    }

    private static byte[] decode(Iterator<Row> rows, String fileName) {
        if (!rows.hasNext()) {
            throw invalid(fileName, Problem.NOT_TOOLBOX_EXPORT, null);
        }
        Row header = rows.next();
        if (!FORMAT.equals(text(header, FORMAT_COLUMN))) {
            throw invalid(fileName, Problem.NOT_TOOLBOX_EXPORT, null);
        }
        if (!VERSION.equals(text(header, VERSION_COLUMN))) {
            throw invalid(fileName, Problem.UNSUPPORTED_VERSION, null);
        }
        long maxBytes = SecurityLimits.BUDGET_IMPORT_MAX_FILE_BYTES;
        long maxEncoded = 4 * ((maxBytes + 2) / 3);
        StringBuilder encoded = new StringBuilder();
        while (rows.hasNext()) {
            String chunk = text(rows.next(), DATA_COLUMN);
            if (encoded.length() + (long) chunk.length() > maxEncoded) {
                long decodedSize = (encoded.length() + (long) chunk.length()) * 3 / 4;
                throw new BudgetFileTooLargeException(fileName, decodedSize, maxBytes);
            }
            encoded.append(chunk);
        }
        try {
            return Base64.getDecoder().decode(encoded.toString());
        } catch (IllegalArgumentException e) {
            throw invalid(fileName, Problem.UNREADABLE_FILE, e);
        }
    }

    private static String text(Row row, int column) {
        return row.getOptionalCell(column).map(Cell::getText).orElse("");
    }

    /** Isi ZIP dicek lewat {@link ZipGuard} sebelum fastexcel men-decompress apa pun. */
    private static void checkUncompressedSize(Path file, String fileName) {
        long max = SecurityLimits.BUDGET_XLSX_MAX_UNCOMPRESSED_BYTES;
        try {
            long total = ZipGuard.uncompressedSize(file, max, MAX_ZIP_ENTRIES);
            if (total > max) {
                throw new BudgetFileTooLargeException(fileName, total, max);
            }
        } catch (ZipException e) {
            throw invalid(fileName, Problem.UNREADABLE_FILE, e);
        } catch (IOException e) {
            throw new BudgetStorageException(fileName, e);
        }
    }

    private static InvalidBudgetFileException invalid(String fileName, Problem problem, Throwable cause) {
        return new InvalidBudgetFileException(fileName, problem, NO_LOCATION, cause);
    }
}
