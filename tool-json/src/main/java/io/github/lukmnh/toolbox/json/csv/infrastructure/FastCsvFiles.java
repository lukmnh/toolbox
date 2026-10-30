package io.github.lukmnh.toolbox.json.csv.infrastructure;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategies;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFiles;
import io.github.lukmnh.toolbox.json.csv.domain.CsvIoException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordReader;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordWriter;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSource;
import io.github.lukmnh.toolbox.json.csv.domain.Delimiter;
import io.github.lukmnh.toolbox.json.csv.domain.EncodingDetector;
import io.github.lukmnh.toolbox.json.csv.domain.InputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.LineEnding;
import io.github.lukmnh.toolbox.json.csv.domain.OutputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.QuoteMode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Adapter file system + FastCSV 4.4.0. */
public final class FastCsvFiles implements CsvFiles {

    private static final int BUFFER_BYTES = 64 * 1024;
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new CsvIoException(file.getFileName().toString(), CsvIoException.Operation.READ, e);
        }
    }

    @Override
    public byte[] head(Path file, int maxBytes) {
        try (InputStream in = Files.newInputStream(file)) {
            return in.readNBytes(maxBytes);
        } catch (IOException e) {
            throw new CsvIoException(file.getFileName().toString(), CsvIoException.Operation.READ, e);
        }
    }

    @Override
    public List<List<String>> parseSample(String text, Delimiter delimiter, int maxRecords) {
        List<List<String>> records = new ArrayList<>();
        try (CsvReader<de.siegmar.fastcsv.reader.CsvRecord> reader =
                        readerBuilder(delimiter).ofCsvRecord(text);
                CloseableIterator<de.siegmar.fastcsv.reader.CsvRecord> iterator = reader.iterator()) {
            while (records.size() < maxRecords && iterator.hasNext()) {
                records.add(iterator.next().getFields());
            }
        } catch (CsvParseException | IOException | UncheckedIOException e) {
            // Sampel terpotong bisa berakhir di tengah quote; record yang sudah terurai tetap cukup untuk deteksi
            return records;
        }
        return records;
    }

    @Override
    public CsvRecordReader openReader(CsvSource source, InputFormat format) {
        String name = source.fileName();
        InputStream raw = null;
        try {
            raw = Files.newInputStream(source.file());
            CountingInputStream counter = new CountingInputStream(raw);
            BufferedInputStream buffered = new BufferedInputStream(counter, BUFFER_BYTES);
            if (format.encoding().isUtf8()) {
                skipBom(buffered);
            }
            InputStreamReader text = new InputStreamReader(buffered, decoder(format.encoding()));
            return new FastCsvRecordReader(
                    name,
                    format.encoding(),
                    counter,
                    readerBuilder(format.delimiter()).ofCsvRecord(text));
        } catch (IOException e) {
            closeQuietly(raw, e);
            throw new CsvIoException(name, CsvIoException.Operation.READ, e);
        }
    }

    @Override
    public CsvRecordWriter openWriter(Path target, String displayName, OutputFormat format) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(Files.newOutputStream(target), BUFFER_BYTES);
            if (format.encoding().hasBom()) {
                out.write(UTF8_BOM);
            }
            Writer output = new OutputStreamWriter(out, format.encoding().charset());
            CsvWriter writer = CsvWriter.builder()
                    .fieldSeparator(format.delimiter().symbol())
                    .quoteStrategy(format.quote() == QuoteMode.ALL ? QuoteStrategies.ALWAYS : QuoteStrategies.REQUIRED)
                    .lineDelimiter(format.lineEnding() == LineEnding.CRLF ? LineDelimiter.CRLF : LineDelimiter.LF)
                    .build(output);
            return new FastCsvRecordWriter(displayName, output, writer, format.neutralizeFormulas());
        } catch (IOException e) {
            closeQuietly(out, e);
            throw new CsvIoException(displayName, CsvIoException.Operation.WRITE, e);
        }
    }

    /**
     * Jumlah kolom tidak konsisten ditangani domain (laporan + pilihan isi/lewati), bukan dilempar parser. Baris
     * kosong dilewati supaya baris kosong di akhir file tidak dilaporkan sebagai baris rusak.
     */
    private static CsvReader.CsvReaderBuilder readerBuilder(Delimiter delimiter) {
        return CsvReader.builder()
                .fieldSeparator(delimiter.symbol())
                .skipEmptyLines(true)
                .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
                .missingFieldStrategy(FieldMismatchStrategy.IGNORE);
    }

    /**
     * UTF-8 wajib valid supaya file Windows-1252 yang salah terdeteksi ketahuan, bukan diam-diam jadi karakter rusak.
     * Windows-1252 dipakai sebagai fallback, jadi lima byte yang tidak terdefinisi diganti, bukan ditolak.
     */
    private static CharsetDecoder decoder(CsvEncoding encoding) {
        CodingErrorAction action = encoding.isUtf8() ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
        return encoding.charset().newDecoder().onMalformedInput(action).onUnmappableCharacter(action);
    }

    /** BOM dibuang juga saat user memilih "UTF-8" tanpa BOM, supaya tidak menempel di nama kolom pertama. */
    private static void skipBom(BufferedInputStream in) throws IOException {
        in.mark(UTF8_BOM.length);
        byte[] start = in.readNBytes(UTF8_BOM.length);
        if (!EncodingDetector.startsWithBom(start)) {
            in.reset();
        }
    }

    private static void closeQuietly(Closeable resource, IOException original) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (IOException e) {
            original.addSuppressed(e);
        }
    }
}
