package io.github.lukmnh.toolbox.json.shared.infrastructure;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import io.github.lukmnh.toolbox.json.shared.domain.ErrorSnippet;
import io.github.lukmnh.toolbox.json.shared.domain.JsonIoException;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException.Kind;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Mengubah error Jackson menjadi {@link MalformedJsonException} lengkap dengan potongan konteks. */
public final class JacksonErrors {

    /** UTF-8 maksimal 4 byte per karakter, jadi jendela ini selalu cukup untuk radius karakter snippet. */
    private static final int WINDOW_BYTES = ErrorSnippet.DEFAULT_RADIUS * 4;

    private static final String EMPTY_DETAIL = "No content";
    private static final String TRAILING_DETAIL = "Unexpected content after root value";

    private JacksonErrors() {}

    public static MalformedJsonException fromFile(Path file, StreamReadException error) {
        JsonLocation location = error.getLocation();
        return new MalformedJsonException(
                Kind.SYNTAX,
                file.getFileName().toString(),
                line(location),
                column(location),
                fileSnippet(file, location == null ? 0 : location.getByteOffset()),
                error.getOriginalMessage());
    }

    public static MalformedJsonException fromText(String text, StreamReadException error) {
        JsonLocation location = error.getLocation();
        return new MalformedJsonException(
                Kind.SYNTAX,
                null,
                line(location),
                column(location),
                ErrorSnippet.around(text, location == null ? 0 : location.getCharOffset(), ErrorSnippet.DEFAULT_RADIUS),
                error.getOriginalMessage());
    }

    /** Untuk byte di memori, misalnya satu record; {@code sourceName} boleh {@code null}. */
    public static MalformedJsonException fromBytes(String sourceName, byte[] content, StreamReadException error) {
        JsonLocation location = error.getLocation();
        return new MalformedJsonException(
                Kind.SYNTAX,
                sourceName,
                line(location),
                column(location),
                bytesSnippet(content, location == null ? 0 : location.getByteOffset()),
                error.getOriginalMessage());
    }

    /** Pesan Jackson untuk pelanggaran batas hanya berisi angka batas, tanpa potongan isi file. */
    public static MalformedJsonException limitExceeded(String sourceName, StreamConstraintsException error) {
        JsonLocation location = error.getLocation();
        return new MalformedJsonException(
                Kind.LIMIT_EXCEEDED,
                sourceName,
                line(location),
                column(location),
                ErrorSnippet.NONE,
                error.getOriginalMessage());
    }

    public static MalformedJsonException empty(String sourceName) {
        return new MalformedJsonException(Kind.EMPTY, sourceName, 1, 1, ErrorSnippet.NONE, EMPTY_DETAIL);
    }

    /**
     * Konten tambahan setelah nilai root. {@code parser} harus berada di token tambahan tersebut.
     *
     * @param snippetSource menghasilkan snippet dari offset (byte atau char, sesuai jenis parser)
     */
    public static MalformedJsonException trailing(String sourceName, JsonParser parser, SnippetSource snippetSource) {
        JsonLocation location = parser.currentTokenLocation();
        long offset = location.getByteOffset() >= 0 ? location.getByteOffset() : location.getCharOffset();
        return new MalformedJsonException(
                Kind.TRAILING_CONTENT,
                sourceName,
                location.getLineNr(),
                location.getColumnNr(),
                snippetSource.snippetAt(offset),
                TRAILING_DETAIL);
    }

    public static ErrorSnippet fileSnippet(Path file, long byteOffset) {
        if (byteOffset < 0) {
            return ErrorSnippet.NONE;
        }
        long start = Math.max(0, byteOffset - WINDOW_BYTES);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) (byteOffset - start) + WINDOW_BYTES);
            int read = 0;
            // Baca sampai jendela penuh atau akhir file
            while (buffer.hasRemaining() && read >= 0) {
                read = channel.read(buffer, start + buffer.position());
            }
            byte[] window = new byte[buffer.position()];
            buffer.flip().get(window);
            return ErrorSnippet.aroundUtf8(window, (int) (byteOffset - start), ErrorSnippet.DEFAULT_RADIUS);
        } catch (IOException e) {
            throw new JsonIoException(file, e);
        }
    }

    public static ErrorSnippet bytesSnippet(byte[] content, long byteOffset) {
        if (byteOffset < 0) {
            return ErrorSnippet.NONE;
        }
        int marker = (int) Math.min(byteOffset, content.length);
        int start = Math.max(0, marker - WINDOW_BYTES);
        int end = Math.min(content.length, marker + WINDOW_BYTES);
        byte[] window = Arrays.copyOfRange(content, start, end);
        return ErrorSnippet.aroundUtf8(window, marker - start, ErrorSnippet.DEFAULT_RADIUS);
    }

    private static long line(JsonLocation location) {
        return location == null ? 1 : Math.max(1, location.getLineNr());
    }

    private static long column(JsonLocation location) {
        return location == null ? 1 : Math.max(1, location.getColumnNr());
    }

    @FunctionalInterface
    public interface SnippetSource {
        ErrorSnippet snippetAt(long offset);
    }
}
