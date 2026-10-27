package io.github.lukmnh.toolbox.json.viewer.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * InputStream untuk rentang byte {@code [start, end)} sebuah {@link FileChannel}. Memakai read posisional,
 * sehingga banyak stream bisa membaca channel yang sama secara bersamaan. {@link #close()} tidak menutup
 * channel.
 */
final class RangeInputStream extends InputStream {

    private final FileChannel channel;
    private final long end;
    private long position;

    RangeInputStream(FileChannel channel, long start, long end) {
        this.channel = channel;
        this.position = start;
        this.end = end;
    }

    @Override
    public int read() throws IOException {
        byte[] single = new byte[1];
        return read(single, 0, 1) < 0 ? -1 : single[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        if (position >= end) {
            return -1;
        }
        int wanted = (int) Math.min(length, end - position);
        int read = channel.read(ByteBuffer.wrap(buffer, offset, wanted), position);
        if (read > 0) {
            position += read;
        }
        return read;
    }
}
