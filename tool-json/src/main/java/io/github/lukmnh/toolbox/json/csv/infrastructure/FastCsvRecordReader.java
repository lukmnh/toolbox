package io.github.lukmnh.toolbox.json.csv.infrastructure;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncodingException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvIoException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordReader;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRow;
import io.github.lukmnh.toolbox.json.csv.domain.CsvToolException;
import io.github.lukmnh.toolbox.json.csv.domain.MalformedCsvException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.util.Optional;

/** Adapter FastCSV: record demi record, error library diterjemahkan ke exception domain. */
final class FastCsvRecordReader implements CsvRecordReader {

    private final String fileName;
    private final CsvEncoding encoding;
    private final CountingInputStream counter;
    private final CsvReader<de.siegmar.fastcsv.reader.CsvRecord> reader;
    private final CloseableIterator<de.siegmar.fastcsv.reader.CsvRecord> iterator;

    FastCsvRecordReader(
            String fileName,
            CsvEncoding encoding,
            CountingInputStream counter,
            CsvReader<de.siegmar.fastcsv.reader.CsvRecord> reader) {
        this.fileName = fileName;
        this.encoding = encoding;
        this.counter = counter;
        this.reader = reader;
        this.iterator = reader.iterator();
    }

    @Override
    public Optional<CsvRow> next() {
        try {
            if (!iterator.hasNext()) {
                return Optional.empty();
            }
            de.siegmar.fastcsv.reader.CsvRecord record = iterator.next();
            return Optional.of(new CsvRow(record.getStartingLineNumber(), record.getFields()));
        } catch (UncheckedIOException | CsvParseException e) {
            throw translate(e);
        }
    }

    @Override
    public long bytesRead() {
        return counter.count();
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new CsvIoException(fileName, CsvIoException.Operation.READ, e);
        }
    }

    /** FastCSV membungkus error decoder/I/O; cari penyebab aslinya supaya pesan user tepat. */
    private CsvToolException translate(RuntimeException error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof CharacterCodingException) {
                return new CsvEncodingException(fileName, encoding, error);
            }
            if (cause instanceof IOException) {
                return new CsvIoException(fileName, CsvIoException.Operation.READ, error);
            }
        }
        return new MalformedCsvException(fileName, MalformedCsvException.Kind.UNPARSEABLE, error);
    }
}
