package io.github.lukmnh.toolbox.json.csv.infrastructure;

import de.siegmar.fastcsv.writer.CsvWriter;
import io.github.lukmnh.toolbox.common.security.CsvFormulaGuard;
import io.github.lukmnh.toolbox.json.csv.domain.CsvIoException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

/** Adapter FastCSV writer. {@link #close()} idempoten karena use case menutup lebih awal untuk membaca ukuran. */
final class FastCsvRecordWriter implements CsvRecordWriter {

    private final String displayName;
    private final Writer output;
    private final CsvWriter writer;
    private final boolean neutralizeFormulas;
    private boolean closed;

    FastCsvRecordWriter(String displayName, Writer output, CsvWriter writer, boolean neutralizeFormulas) {
        this.displayName = displayName;
        this.output = output;
        this.writer = writer;
        this.neutralizeFormulas = neutralizeFormulas;
    }

    @Override
    public void write(List<String> fields) {
        List<String> cells = neutralizeFormulas
                ? fields.stream().map(CsvFormulaGuard::neutralize).toList()
                : fields;
        try {
            writer.writeRecord(cells);
        } catch (UncheckedIOException e) {
            throw new CsvIoException(displayName, CsvIoException.Operation.WRITE, e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            writer.close();
        } catch (IOException e) {
            failure = e;
        }
        // FastCSV tidak menjamin menutup Writer milik pemanggil; tetap ditutup walau writer gagal flush
        try {
            output.close();
        } catch (IOException e) {
            failure = merge(failure, e);
        }
        if (failure != null) {
            throw new CsvIoException(displayName, CsvIoException.Operation.WRITE, failure);
        }
    }

    private static IOException merge(IOException first, IOException next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }
}
