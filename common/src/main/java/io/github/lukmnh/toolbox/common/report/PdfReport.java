package io.github.lukmnh.toolbox.common.report;

import io.github.lukmnh.toolbox.common.io.AtomicFiles;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Dokumen PDF sederhana untuk fitur export: judul, heading, paragraf, pasangan label–nilai, tabel, dan lampiran
 * (embedded file) opsional. Gaya Swiss: hitam-putih, Helvetica, garis tipis, tanpa dekorasi.
 *
 * <p>Font Standard 14 hanya mendukung WinAnsi; karakter di luar itu diganti {@code ?} supaya export tidak gagal.
 */
public final class PdfReport {

    private static final float MARGIN = 48;
    private static final float BODY_SIZE = 10;
    private static final float TITLE_SIZE = 20;
    private static final float HEADING_SIZE = 12;
    private static final float LEADING = 1.45f;
    private static final float CELL_PADDING = 4;
    private static final float RULE_GRAY = 0.75f;
    /** Batas aman ukuran array di JVM. */
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private static final Pattern NUMERIC = Pattern.compile("^[-+]?(Rp\\s?)?[\\d.,\\s]+%?$");

    private final String title;
    private final List<Block> blocks = new ArrayList<>();
    private final Map<String, byte[]> attachments = new HashMap<>();

    private PdfReport(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public static PdfReport titled(String title) {
        return new PdfReport(title);
    }

    public PdfReport heading(String text) {
        blocks.add(new Heading(text));
        return this;
    }

    public PdfReport paragraph(String text) {
        blocks.add(new Paragraph(text));
        return this;
    }

    /** Pasangan label–nilai sesuai urutan map, misalnya ringkasan hasil hitung. */
    public PdfReport keyValues(SequencedMap<String, String> rows) {
        blocks.add(new KeyValues(List.copyOf(rows.sequencedEntrySet())));
        return this;
    }

    /** Sel yang berupa angka otomatis rata kanan. */
    public PdfReport table(List<String> headers, List<List<String>> rows) {
        blocks.add(
                new Table(List.copyOf(headers), rows.stream().map(List::copyOf).toList()));
        return this;
    }

    /** Lampirkan data mentah (misalnya JSON) supaya PDF bisa di-import ulang lewat {@link #readAttachment}. */
    public PdfReport attach(String fileName, byte[] content) {
        attachments.put(fileName, content.clone());
        return this;
    }

    public void writeTo(Path target) throws IOException {
        AtomicFiles.write(target, file -> {
            try (PDDocument document = new PDDocument()) {
                document.getDocumentInformation().setTitle(title);
                document.getDocumentInformation().setCreator("Toolbox");
                try (Renderer renderer = new Renderer(document)) {
                    renderer.render();
                }
                embedAttachments(document);
                document.save(file.toFile());
            }
        });
    }

    /**
     * Baca lampiran dari PDF hasil {@link #writeTo}. Kosong bila PDF tidak punya lampiran bernama {@code fileName}.
     *
     * @throws AttachmentTooLargeException bila isi lampiran (setelah decode) melebihi {@code maxBytes}
     * @throws IOException bila file bukan PDF valid
     */
    public static Optional<byte[]> readAttachment(Path pdf, String fileName, long maxBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDDocumentNameDictionary names = document.getDocumentCatalog().getNames();
            if (names == null || names.getEmbeddedFiles() == null) {
                return Optional.empty();
            }
            Map<String, PDComplexFileSpecification> files =
                    names.getEmbeddedFiles().getNames();
            PDComplexFileSpecification spec = files == null ? null : files.get(fileName);
            if (spec == null) {
                return Optional.empty();
            }
            PDEmbeddedFile embedded =
                    spec.getEmbeddedFileUnicode() != null ? spec.getEmbeddedFileUnicode() : spec.getEmbeddedFile();
            if (embedded == null) {
                return Optional.empty();
            }
            // Dibaca terbatas setelah decode: /Length hanya ukuran terkompresi, jadi tidak aman untuk cek batas
            try (InputStream content = embedded.createInputStream()) {
                byte[] bytes = content.readNBytes(Math.toIntExact(Math.min(maxBytes + 1, MAX_ARRAY_LENGTH)));
                if (bytes.length > maxBytes) {
                    throw new AttachmentTooLargeException(fileName, maxBytes);
                }
                return Optional.of(bytes);
            }
        }
    }

    /** Lampiran melebihi batas {@code maxBytes} pada {@link #readAttachment}. */
    public static final class AttachmentTooLargeException extends IOException {

        @Serial
        private static final long serialVersionUID = 1L;

        private final long maxBytes;

        AttachmentTooLargeException(String fileName, long maxBytes) {
            super("Attachment " + fileName + " exceeds the limit of " + maxBytes + " bytes");
            this.maxBytes = maxBytes;
        }

        public long maxBytes() {
            return maxBytes;
        }
    }

    private void embedAttachments(PDDocument document) throws IOException {
        if (attachments.isEmpty()) {
            return;
        }
        Map<String, PDComplexFileSpecification> specs = new HashMap<>();
        for (Map.Entry<String, byte[]> attachment : attachments.entrySet()) {
            PDEmbeddedFile embedded = new PDEmbeddedFile(document, new ByteArrayInputStream(attachment.getValue()));
            embedded.setSize(attachment.getValue().length);
            PDComplexFileSpecification spec = new PDComplexFileSpecification();
            spec.setFile(attachment.getKey());
            spec.setFileUnicode(attachment.getKey());
            spec.setEmbeddedFile(embedded);
            spec.setEmbeddedFileUnicode(embedded);
            specs.put(attachment.getKey(), spec);
        }
        PDEmbeddedFilesNameTreeNode tree = new PDEmbeddedFilesNameTreeNode();
        tree.setNames(specs);
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
        names.setEmbeddedFiles(tree);
        document.getDocumentCatalog().setNames(names);
    }

    private sealed interface Block permits Heading, Paragraph, KeyValues, Table {}

    private record Heading(String text) implements Block {}

    private record Paragraph(String text) implements Block {}

    private record KeyValues(List<Map.Entry<String, String>> rows) implements Block {}

    private record Table(List<String> headers, List<List<String>> rows) implements Block {}

    /** Menulis blok dari atas ke bawah; halaman baru otomatis saat ruang habis. */
    private final class Renderer implements AutoCloseable {

        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final Map<Integer, Boolean> encodable = new HashMap<>();
        private final float width = PDRectangle.A4.getWidth() - 2 * MARGIN;
        private PDPageContentStream stream;
        private float y;

        Renderer(PDDocument document) {
            this.document = document;
        }

        void render() throws IOException {
            newPage();
            text(title, bold, TITLE_SIZE, MARGIN, width);
            y -= BODY_SIZE;
            rule(MARGIN, MARGIN + width, 1f);
            y -= BODY_SIZE;
            for (Block block : blocks) {
                switch (block) {
                    case Heading heading -> {
                        y -= HEADING_SIZE * 0.6f;
                        text(heading.text(), bold, HEADING_SIZE, MARGIN, width);
                    }
                    case Paragraph paragraph -> text(paragraph.text(), regular, BODY_SIZE, MARGIN, width);
                    case KeyValues keyValues -> keyValues(keyValues.rows());
                    case Table table -> table(table);
                }
                y -= BODY_SIZE * 0.6f;
            }
        }

        private void keyValues(List<Map.Entry<String, String>> rows) throws IOException {
            float labelWidth = width * 0.4f;
            for (Map.Entry<String, String> row : rows) {
                List<String> labels = wrap(row.getKey(), regular, BODY_SIZE, labelWidth - CELL_PADDING);
                List<String> values = wrap(row.getValue(), bold, BODY_SIZE, width - labelWidth);
                float height = Math.max(labels.size(), values.size()) * lineHeight(BODY_SIZE);
                ensureSpace(height);
                float top = y;
                lines(labels, regular, BODY_SIZE, MARGIN, false, labelWidth);
                y = top;
                lines(values, bold, BODY_SIZE, MARGIN + labelWidth, false, width - labelWidth);
                y = top - height;
            }
        }

        private void table(Table table) throws IOException {
            float[] columns = columnWidths(table);
            row(table.headers(), columns, bold, true);
            for (List<String> cells : table.rows()) {
                row(cells, columns, regular, false);
            }
        }

        private void row(List<String> cells, float[] columns, PDType1Font font, boolean header) throws IOException {
            List<List<String>> wrapped = new ArrayList<>();
            int lineCount = 1;
            for (int index = 0; index < columns.length; index++) {
                String cell = index < cells.size() ? cells.get(index) : "";
                List<String> lines = wrap(cell, font, BODY_SIZE, columns[index] - 2 * CELL_PADDING);
                wrapped.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }
            float height = lineCount * lineHeight(BODY_SIZE) + CELL_PADDING;
            ensureSpace(height);
            float top = y;
            float x = MARGIN;
            for (int index = 0; index < columns.length; index++) {
                y = top;
                boolean right = !header && NUMERIC.matcher(cellAt(cells, index)).matches();
                lines(wrapped.get(index), font, BODY_SIZE, x + CELL_PADDING, right, columns[index] - 2 * CELL_PADDING);
                x += columns[index];
            }
            y = top - height;
            rule(MARGIN, MARGIN + width, header ? 1f : 0.4f);
            y -= CELL_PADDING;
        }

        /** Lebar kolom proporsional terhadap teks terpanjang, dengan batas bawah supaya kolom sempit tetap terbaca. */
        private float[] columnWidths(Table table) {
            int count = table.headers().size();
            float[] weights = new float[count];
            for (int index = 0; index < count; index++) {
                int longest = table.headers().get(index).length();
                for (List<String> row : table.rows()) {
                    longest = Math.max(longest, cellAt(row, index).length());
                }
                weights[index] = Math.max(6, Math.min(longest, 40));
            }
            float total = 0;
            for (float weight : weights) {
                total += weight;
            }
            float[] widths = new float[count];
            for (int index = 0; index < count; index++) {
                widths[index] = width * weights[index] / total;
            }
            return widths;
        }

        private static String cellAt(List<String> row, int index) {
            return index < row.size() && row.get(index) != null ? row.get(index) : "";
        }

        private void text(String value, PDType1Font font, float size, float x, float maxWidth) throws IOException {
            // Per baris (bukan per blok) supaya paragraf yang lebih panjang dari satu halaman tetap terpotong rapi
            for (String line : wrap(value, font, size, maxWidth)) {
                ensureSpace(lineHeight(size));
                lines(List.of(line), font, size, x, false, maxWidth);
            }
        }

        private void lines(List<String> lines, PDType1Font font, float size, float x, boolean right, float maxWidth)
                throws IOException {
            for (String line : lines) {
                y -= lineHeight(size);
                float offset = right ? maxWidth - font.getStringWidth(line) / 1000 * size : 0;
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(x + offset, y + size * (LEADING - 1));
                stream.showText(line);
                stream.endText();
            }
        }

        /** Word wrap per baris; kata yang lebih panjang dari kolom dipotong per karakter. */
        private List<String> wrap(String value, PDType1Font font, float size, float maxWidth) throws IOException {
            List<String> result = new ArrayList<>();
            for (String paragraph : sanitize(value == null ? "" : value, font).split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split(" ", -1)) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (width(candidate, font, size) <= maxWidth) {
                        line.setLength(0);
                        line.append(candidate);
                        continue;
                    }
                    if (!line.isEmpty()) {
                        result.add(line.toString());
                        line.setLength(0);
                    }
                    for (char character : word.toCharArray()) {
                        if (width(line.toString() + character, font, size) > maxWidth && !line.isEmpty()) {
                            result.add(line.toString());
                            line.setLength(0);
                        }
                        line.append(character);
                    }
                }
                result.add(line.toString());
            }
            return result;
        }

        private static float width(String text, PDType1Font font, float size) throws IOException {
            return font.getStringWidth(text) / 1000 * size;
        }

        private String sanitize(String value, PDType1Font font) {
            StringBuilder builder = new StringBuilder(value.length());
            value.replace("\t", "    ").replace("\r", "").codePoints().forEach(codePoint -> {
                boolean supported = codePoint == '\n'
                        || encodable.computeIfAbsent(codePoint, point -> {
                            try {
                                font.encode(Character.toString(point));
                                return true;
                            } catch (IOException | IllegalArgumentException e) {
                                return false;
                            }
                        });
                builder.appendCodePoint(supported ? codePoint : '?');
            });
            return builder.toString();
        }

        private void rule(float from, float to, float lineWidth) throws IOException {
            stream.setStrokingColor(RULE_GRAY, RULE_GRAY, RULE_GRAY);
            stream.setLineWidth(lineWidth);
            stream.moveTo(from, y);
            stream.lineTo(to, y);
            stream.stroke();
        }

        private static float lineHeight(float size) {
            return size * LEADING;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
