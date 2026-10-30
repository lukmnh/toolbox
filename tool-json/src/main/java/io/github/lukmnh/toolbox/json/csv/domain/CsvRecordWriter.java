package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/** Port: penulis record streaming. Formula injection dinetralisir di sini bila diminta format. */
public interface CsvRecordWriter extends AutoCloseable {

    /** @throws CsvIoException bila gagal menulis */
    void write(List<String> fields);

    @Override
    void close();
}
