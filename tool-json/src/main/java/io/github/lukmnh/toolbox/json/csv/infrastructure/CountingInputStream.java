package io.github.lukmnh.toolbox.json.csv.infrastructure;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Menghitung byte yang sudah dibaca dari disk, untuk progress ekspor. */
final class CountingInputStream extends FilterInputStream {

    private volatile long count;

    CountingInputStream(InputStream in) {
        super(in);
    }

    long count() {
        return count;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value >= 0) {
            count++;
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            count += read;
        }
        return read;
    }

    @Override
    public long skip(long bytes) throws IOException {
        long skipped = super.skip(bytes);
        count += skipped;
        return skipped;
    }
}
