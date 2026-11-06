package io.github.lukmnh.toolbox.devutils.sql.infrastructure;

import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataFiles;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException;
import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlFileTooLargeException;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlIoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Pilih reader dari ekstensi file; teks dibaca lewat {@link JdkSqlFiles} (UTF-8, BOM dibuang, ukuran dibatasi). */
public final class TabularDataFiles implements DataFiles {

    private final long maxFileBytes;
    private final long maxXlsxUncompressedBytes;
    private final int maxRows;

    public TabularDataFiles() {
        this(
                SecurityLimits.SQL_GENERATOR_MAX_FILE_BYTES,
                SecurityLimits.SQL_GENERATOR_XLSX_MAX_UNCOMPRESSED_BYTES,
                SecurityLimits.SQL_GENERATOR_MAX_ROWS);
    }

    /** Konstruktor untuk test. */
    TabularDataFiles(long maxFileBytes, long maxXlsxUncompressedBytes, int maxRows) {
        this.maxFileBytes = maxFileBytes;
        this.maxXlsxUncompressedBytes = maxXlsxUncompressedBytes;
        this.maxRows = maxRows;
    }

    @Override
    public DataTable read(Path file) {
        String name = file.getFileName().toString();
        String extension = FileExtensions.extensionOf(file);
        return switch (extension) {
            case "csv" -> DelimitedDataReader.csv(name, readText(file), maxRows);
            case "txt" -> DelimitedDataReader.txt(name, readText(file), maxRows);
            case "json" -> JsonDataReader.read(name, readText(file), maxRows);
            case "xlsx" -> {
                checkSize(file, name);
                yield XlsxDataReader.read(file, name, maxXlsxUncompressedBytes, maxRows);
            }
            default ->
                throw new InvalidDataException(
                        name, Problem.UNSUPPORTED_FORMAT, InvalidDataException.NO_LINE, extension);
        };
    }

    private String readText(Path file) {
        return new JdkSqlFiles(maxFileBytes).read(file).text();
    }

    private void checkSize(Path file, String name) {
        try {
            long size = Files.size(file);
            if (size > maxFileBytes) {
                throw new SqlFileTooLargeException(name, size, maxFileBytes);
            }
        } catch (IOException e) {
            throw new SqlIoException(name, e);
        }
    }
}
