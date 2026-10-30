package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;

/**
 * Isi dokumen PDF yang sudah berupa teks siap tampil (label dan nominal sudah diformat oleh presentation). Domain
 * tidak tahu library PDF; {@link ReportPdfWriter} yang merendernya.
 */
public record ReportDocument(String title, List<Block> blocks) {

    public ReportDocument {
        Objects.requireNonNull(title, "title");
        blocks = List.copyOf(blocks);
    }

    public sealed interface Block {}

    public record Heading(String text) implements Block {

        public Heading {
            Objects.requireNonNull(text, "text");
        }
    }

    public record Paragraph(String text) implements Block {

        public Paragraph {
            Objects.requireNonNull(text, "text");
        }
    }

    /** Pasangan label–nilai sesuai urutan map. */
    public record KeyValues(SequencedMap<String, String> rows) implements Block {

        public KeyValues {
            rows = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(rows));
        }
    }

    public record Table(List<String> headers, List<List<String>> rows) implements Block {

        public Table {
            headers = List.copyOf(headers);
            rows = rows.stream().map(List::copyOf).toList();
            int width = headers.size();
            if (rows.stream().anyMatch(row -> row.size() != width)) {
                throw new IllegalArgumentException("Jumlah sel tiap baris harus sama dengan jumlah header");
            }
        }
    }

    /** Penyusun berurutan supaya pemanggil tidak perlu merakit list block sendiri. */
    public static Builder titled(String title) {
        return new Builder(title);
    }

    public static final class Builder {

        private final String title;
        private final List<Block> blocks = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder heading(String text) {
            blocks.add(new Heading(text));
            return this;
        }

        public Builder paragraph(String text) {
            blocks.add(new Paragraph(text));
            return this;
        }

        public Builder keyValues(SequencedMap<String, String> rows) {
            blocks.add(new KeyValues(rows));
            return this;
        }

        public Builder table(List<String> headers, List<List<String>> rows) {
            blocks.add(new Table(headers, rows));
            return this;
        }

        public ReportDocument build() {
            return new ReportDocument(title, blocks);
        }
    }
}
