package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import io.github.lukmnh.toolbox.common.security.ZipGuard;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTableBuilder;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlIoException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlToolException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;

/**
 * Sheet pertama {@code .xlsx}, dibaca streaming. Tipe sel dari Excel dipakai langsung; sel angka berformat tanggal
 * ditulis sebagai teks ISO ({@code 2026-11-06}, {@code 2026-11-06 13:45:00}, atau {@code 13:45:00}).
 */
final class XlsxDataReader {

    /** Workbook data biasa hanya berisi puluhan entry; batas ini menolak ZIP aneh lebih awal. */
    private static final int MAX_ZIP_ENTRIES = 10_000;

    /** Format tanggal/waktu bawaan Excel (ID 14–22 dan 45–47). */
    private static final int FIRST_DATE_FORMAT = 14;

    private static final int LAST_DATE_FORMAT = 22;
    private static final int FIRST_TIME_FORMAT = 45;
    private static final int LAST_TIME_FORMAT = 47;

    /** Teks berkutip, blok {@code [...]} (warna/locale/durasi), dan karakter ter-escape bukan token tanggal. */
    private static final Pattern FORMAT_LITERALS = Pattern.compile("\"[^\"]*\"|\\[[^]]*]|\\\\.");

    private static final Pattern DATE_TOKENS = Pattern.compile(".*[ymdhs].*");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private XlsxDataReader() {}

    static DataTable read(Path file, String fileName, long maxUncompressedBytes, int maxRows) {
        checkUncompressedSize(file, fileName, maxUncompressedBytes);
        DataTableBuilder builder = new DataTableBuilder(fileName, maxRows);
        try (ReadableWorkbook workbook = new ReadableWorkbook(file.toFile(), new ReadingOptions(true, false));
                Stream<Row> rows = workbook.getFirstSheet().openStream()) {
            Iterator<Row> iterator = rows.iterator();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                List<DataValue> values = values(row);
                if (builder.hasHeader()) {
                    builder.row(values, row.getRowNum());
                } else if (!values.stream().allMatch(DataValue::isNull)) {
                    builder.header(values.stream().map(DataValue::text).toList(), row.getRowNum());
                }
            }
        } catch (SqlToolException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // Reader melempar beragam runtime exception untuk XML/ZIP rusak; semuanya berarti file tidak terbaca
            throw new InvalidDataException(fileName, Problem.UNREADABLE, InvalidDataException.NO_LINE, "", e);
        }
        return builder.build();
    }

    private static void checkUncompressedSize(Path file, String fileName, long maxBytes) {
        try {
            long total = ZipGuard.uncompressedSize(file, maxBytes, MAX_ZIP_ENTRIES);
            if (total > maxBytes) {
                throw new SqlFileTooLargeException(fileName, total, maxBytes);
            }
        } catch (ZipException e) {
            throw new InvalidDataException(fileName, Problem.UNREADABLE, InvalidDataException.NO_LINE, "", e);
        } catch (IOException e) {
            throw new SqlIoException(fileName, e);
        }
    }

    private static List<DataValue> values(Row row) {
        List<DataValue> values = new ArrayList<>(row.getCellCount());
        for (int column = 0; column < row.getCellCount(); column++) {
            values.add(value(row.getCell(column)));
        }
        return values;
    }

    private static DataValue value(Cell cell) {
        if (cell == null) {
            return DataValue.NULL;
        }
        return switch (cell.getType()) {
            case EMPTY -> DataValue.NULL;
            case BOOLEAN -> DataValue.bool(cell.asBoolean());
            case NUMBER -> isDate(cell) ? DataValue.text(dateText(cell)) : DataValue.number(plain(cell.asNumber()));
            case STRING -> DataValue.raw(cell.asString()).isNull() ? DataValue.NULL : DataValue.text(cell.asString());
            // Nilai cache formula dan teks error (#N/A) tidak bertipe: ikut inferensi per kolom
            case FORMULA, ERROR -> DataValue.raw(cell.getRawValue() == null ? "" : cell.getRawValue());
        };
    }

    private static String plain(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    private static boolean isDate(Cell cell) {
        Integer id = cell.getDataFormatId();
        if (id != null
                && ((id >= FIRST_DATE_FORMAT && id <= LAST_DATE_FORMAT)
                        || (id >= FIRST_TIME_FORMAT && id <= LAST_TIME_FORMAT))) {
            return true;
        }
        String format = cell.getDataFormatString();
        if (format == null) {
            return false;
        }
        String tokens = FORMAT_LITERALS.matcher(format).replaceAll("").toLowerCase(Locale.ROOT);
        return !tokens.equals("general") && DATE_TOKENS.matcher(tokens).matches();
    }

    /** Serial < 1 = waktu saja; jam 00:00:00 = tanggal saja. */
    private static String dateText(Cell cell) {
        LocalDateTime value = cell.asDate();
        if (cell.asNumber().compareTo(BigDecimal.ONE) < 0) {
            return value.toLocalTime().format(TIME);
        }
        return value.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? value.toLocalDate().toString()
                : value.format(DATE_TIME);
    }
}
