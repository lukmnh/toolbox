package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.UnsupportedTextException;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.CoordinateMapper;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.FontResolver;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PageBox;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditDefaults;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

/**
 * Menempel overlay teks dan gambar secara permanen ke content stream halaman.
 *
 * <p>Setiap overlay digambar di sistem koordinat lokal: origin di titik anchor, sumbu x = kanan tampilan,
 * sumbu y = atas tampilan. Dengan begitu teks dan tanda tangan selalu tegak di halaman yang diputar.
 */
final class OverlayBurner {

    private final FontResolver fontResolver;
    private final FontSpec fontSpec;

    OverlayBurner(FontResolver fontResolver, FontSpec fontSpec) {
        this.fontResolver = fontResolver;
        this.fontSpec = fontSpec;
    }

    static PageBox pageBox(PDPage page) {
        PDRectangle crop = page.getCropBox();
        return new PageBox(
                crop.getLowerLeftX(), crop.getLowerLeftY(), crop.getWidth(), crop.getHeight(), page.getRotation());
    }

    /** @throws io.github.lukmnh.toolbox.pdf.shared.domain.UnsupportedTextException bila ada karakter tanpa glyph */
    void burn(PDDocument document, List<Overlay> overlays) throws IOException {
        Map<Integer, List<Overlay>> byPage =
                overlays.stream().collect(Collectors.groupingBy(Overlay::pageIndex, TreeMap::new, Collectors.toList()));
        Resources resources = new Resources(document);
        for (Map.Entry<Integer, List<Overlay>> entry : byPage.entrySet()) {
            burnPage(document.getPage(entry.getKey()), entry.getValue(), resources);
        }
    }

    private void burnPage(PDPage page, List<Overlay> overlays, Resources resources) throws IOException {
        CoordinateMapper mapper = new CoordinateMapper(pageBox(page));
        // resetContext=true membungkus konten lama dengan q/Q, sehingga CTM-nya tidak bocor ke overlay
        try (PDPageContentStream content =
                new PDPageContentStream(resources.document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            for (Overlay overlay : overlays) {
                switch (overlay) {
                    case Overlay.Text text -> drawText(content, mapper, text, resources.font());
                    case Overlay.Image image -> drawImage(content, mapper, image, resources.image(image.image()));
                }
            }
        }
    }

    private static void drawText(PDPageContentStream content, CoordinateMapper mapper, Overlay.Text text, PDFont font)
            throws IOException {
        float fontSize = (float) text.fontSize();
        content.saveGraphicsState();
        content.transform(localFrame(mapper, text.x(), text.baselineY()));
        content.beginText();
        content.setFont(font, fontSize);
        content.setLeading((float) (text.fontSize() * EditDefaults.LINE_HEIGHT_FACTOR));
        content.newLineAtOffset(0, 0);
        String[] lines = text.lines();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                content.newLine();
            }
            showText(content, lines[index]);
        }
        content.endText();
        content.restoreGraphicsState();
    }

    private static void showText(PDPageContentStream content, String line) throws IOException {
        try {
            content.showText(line);
        } catch (IllegalArgumentException e) {
            // PDFBox melempar IAE bila ada glyph yang tidak ada di font
            throw new UnsupportedTextException(line, e);
        }
    }

    private static void drawImage(
            PDPageContentStream content, CoordinateMapper mapper, Overlay.Image image, PDImageXObject xObject)
            throws IOException {
        content.saveGraphicsState();
        // Anchor = pojok kiri-bawah gambar (di tampilan), karena gambar PDF digambar dari bawah ke atas
        content.transform(localFrame(mapper, image.x(), image.y() + image.height()));
        content.drawImage(xObject, 0, 0, (float) image.width(), (float) image.height());
        content.restoreGraphicsState();
    }

    private static Matrix localFrame(CoordinateMapper mapper, double displayX, double displayY) {
        CoordinateMapper.Point anchor = mapper.toPdf(displayX, displayY);
        return Matrix.getRotateInstance(
                Math.toRadians(mapper.contentRotationDegrees()), (float) anchor.x(), (float) anchor.y());
    }

    /** Font dan gambar dibuat sekali per dokumen, dipakai ulang di semua halaman. */
    private final class Resources {
        private final PDDocument document;
        private final Map<BufferedImage, PDImageXObject> images = new IdentityHashMap<>();
        private PDFont font;

        private Resources(PDDocument document) {
            this.document = document;
        }

        PDFont font() {
            if (font == null) {
                font = fontResolver.load(document, fontSpec);
            }
            return font;
        }

        PDImageXObject image(BufferedImage image) throws IOException {
            PDImageXObject existing = images.get(image);
            if (existing != null) {
                return existing;
            }
            // Lossless supaya alpha tanda tangan tetap (disimpan sebagai SMask)
            PDImageXObject created = LosslessFactory.createFromImage(document, image);
            images.put(image, created);
            return created;
        }
    }
}
