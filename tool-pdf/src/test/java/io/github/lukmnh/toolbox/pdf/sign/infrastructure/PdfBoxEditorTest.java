package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.pdf.shared.domain.FormFieldException;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.FontResolver;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.sign.application.ExportEditedPdfUseCase;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;
import io.github.lukmnh.toolbox.pdf.sign.domain.FormField;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import io.github.lukmnh.toolbox.pdf.sign.domain.PageGeometry;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Menguji "yang terlihat di preview = yang ada di PDF": overlay di-burn, lalu hasilnya dirender ulang dan pixel
 * di posisi tampilan yang sama diperiksa.
 */
class PdfBoxEditorTest {

    /** Render 72 DPI: 1 pixel = 1 point tampilan. */
    private static final int ONE_PIXEL_PER_POINT = 72;

    private static final int DARK_THRESHOLD = 100;
    private static final int LIGHT_THRESHOLD = 200;

    private final PdfBoxEditor editor = new PdfBoxEditor(new PdfLoader(), new FontResolver());
    private final ExportEditedPdfUseCase export = new ExportEditedPdfUseCase();

    @TempDir
    Path temp;

    static Stream<Arguments> geometries() {
        PDRectangle full = PDRectangle.A4;
        PDRectangle offsetCrop = new PDRectangle(50, 80, 400, 300);
        return Stream.of(0, 90, 180, 270)
                .flatMap(rotation -> Stream.of(Arguments.of(rotation, full), Arguments.of(rotation, offsetCrop)));
    }

    @ParameterizedTest(name = "rotasi {0}, crop {1}")
    @MethodSource("geometries")
    void signatureAppearsExactlyWherePlacedInPreview(int rotation, PDRectangle crop) throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), rotation, crop);
        Path output = temp.resolve("out.pdf");
        Overlay.Image signature = new Overlay.Image(0, 60, 50, 40, 30, solid(Color.BLACK));

        try (EditSession session = editor.open(PdfSource.of(source))) {
            export.execute(session, List.of(signature), output, OutputProtection.KEEP, CancellationToken.none());
        }

        BufferedImage page = render(output);
        // Di dalam kotak (60..100, 50..80) gelap, di luarnya terang
        assertDark(page, 62, 52);
        assertDark(page, 80, 65);
        assertDark(page, 98, 78);
        assertLight(page, 56, 65);
        assertLight(page, 104, 65);
        assertLight(page, 80, 46);
        assertLight(page, 80, 84);
    }

    @ParameterizedTest(name = "rotasi {0}, crop {1}")
    @MethodSource("geometries")
    void textBaselineSitsAtClickedPointAndReadsUpright(int rotation, PDRectangle crop) throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), rotation, crop);
        Path output = temp.resolve("out.pdf");
        Overlay.Text text = new Overlay.Text(0, 40, 100, "HHHH", 20);

        try (EditSession session = editor.open(PdfSource.of(source))) {
            export.execute(session, List.of(text), output, OutputProtection.KEEP, CancellationToken.none());
        }

        BufferedImage page = render(output);
        // Huruf kapital "H" berada tepat di atas baseline (cap height ~14 pt untuk 20 pt) dan mulai di x=40
        assertTrue(hasDarkPixel(page, 41, 88, 60, 99), "teks harus ada di atas baseline");
        assertFalse(hasDarkPixel(page, 30, 102, 100, 115), "tidak boleh ada tinta di bawah baseline");
        assertFalse(hasDarkPixel(page, 20, 80, 38, 99), "tidak boleh ada tinta sebelum titik klik");
        assertTrue(TestPdfs.extractText(output).contains("HHHH"));
    }

    @Test
    void multiLineTextUsesLeadingOf1Point2TimesFontSize() throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), 0, PDRectangle.A4);
        Path output = temp.resolve("out.pdf");

        try (EditSession session = editor.open(PdfSource.of(source))) {
            export.execute(
                    session,
                    List.of(new Overlay.Text(0, 40, 100, "HH\nHH", 20)),
                    output,
                    OutputProtection.KEEP,
                    CancellationToken.none());
        }

        BufferedImage page = render(output);
        // Baris kedua: baseline 100 + 1.2 × 20 = 124
        assertTrue(hasDarkPixel(page, 41, 112, 60, 123));
        assertFalse(hasDarkPixel(page, 30, 126, 100, 140));
    }

    @Test
    void geometryReflectsRotationAndCropBox() throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), 90, new PDRectangle(0, 0, 400, 300));

        try (EditSession session = editor.open(PdfSource.of(source))) {
            assertEquals(new PageGeometry(300, 400), session.geometry(0));
        }
    }

    @Test
    void formFieldValueIsAppliedAndKeptInExport() throws IOException {
        Path source = TestPdfs.withTextField(temp.resolve("form.pdf"), "nama");
        Path output = temp.resolve("out.pdf");

        try (EditSession session = editor.open(PdfSource.of(source))) {
            FormField field = session.formFields().getFirst();
            assertInstanceOf(FormField.Text.class, field);
            assertEquals("nama", field.name());

            session.setFieldValue("nama", new FieldValue.Text("Budi Santoso"));
            assertEquals(
                    new FormField.Text("nama", "Budi Santoso", false),
                    session.formFields().getFirst());
            export.execute(session, List.of(), output, OutputProtection.KEEP, CancellationToken.none());
        }

        try (PDDocument result = Loader.loadPDF(output.toFile())) {
            assertEquals(
                    "Budi Santoso",
                    result.getDocumentCatalog().getAcroForm().getField("nama").getValueAsString());
        }
    }

    @Test
    void mismatchedFieldValueIsReportedNotCrashing() throws IOException {
        Path source = TestPdfs.withTextField(temp.resolve("form.pdf"), "nama");

        try (EditSession session = editor.open(PdfSource.of(source))) {
            assertThrows(FormFieldException.class, () -> session.setFieldValue("nama", new FieldValue.Checked(true)));
            assertThrows(FormFieldException.class, () -> session.setFieldValue("tidak-ada", new FieldValue.Text("x")));
        }
    }

    @Test
    void exportDoesNotModifyTheOpenDocument() throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), 0, PDRectangle.A4);

        try (EditSession session = editor.open(PdfSource.of(source))) {
            Overlay.Image signature = new Overlay.Image(0, 60, 50, 40, 30, solid(Color.BLACK));
            export.execute(
                    session,
                    List.of(signature),
                    temp.resolve("out.pdf"),
                    OutputProtection.KEEP,
                    CancellationToken.none());

            BufferedImage preview = session.render(0, ONE_PIXEL_PER_POINT);
            assertLight(preview, 80, 65);
        }
    }

    @Test
    void exportOfEncryptedPdfKeepsPasswordUnlessRemovalChosen() throws IOException {
        Path source = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 1);
        Path kept = temp.resolve("tetap.pdf");
        Path removed = temp.resolve("tanpa-password.pdf");
        Overlay.Text text = new Overlay.Text(0, 40, 100, "Disetujui", 12);

        try (EditSession session = editor.open(new PdfSource(source, TestPdfs.USER_PASSWORD))) {
            export.execute(session, List.of(text), kept, OutputProtection.KEEP, CancellationToken.none());
            export.execute(session, List.of(text), removed, OutputProtection.REMOVE, CancellationToken.none());
        }

        assertTrue(TestPdfs.needsUserPassword(kept));
        assertTrue(TestPdfs.extractText(removed).contains("Disetujui"));
    }

    @Test
    void renderExceedingTimeoutIsRejectedAndSessionStaysLocked() throws IOException {
        Path source = TestPdfs.withGeometry(temp.resolve("in.pdf"), 0, PDRectangle.A4);
        // Timeout 0: render apa pun dianggap terlalu lama, mensimulasikan PDF yang membuat renderer macet
        PdfBoxEditor slowEditor = new PdfBoxEditor(new PdfLoader(), new FontResolver(), Duration.ZERO);

        try (EditSession session = slowEditor.open(PdfSource.of(source))) {
            PdfLimitExceededException error =
                    assertThrows(PdfLimitExceededException.class, () -> session.render(0, ONE_PIXEL_PER_POINT));
            assertEquals(PdfLimitExceededException.Limit.RENDER_TIME, error.limit());
            assertEquals(1, error.actual());

            assertThrows(PdfLimitExceededException.class, session::pageCount);
        }
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }

    private static BufferedImage render(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return new PDFRenderer(document).renderImageWithDPI(0, ONE_PIXEL_PER_POINT, ImageType.RGB);
        }
    }

    private static int brightness(BufferedImage image, int x, int y) {
        Color color = new Color(image.getRGB(x, y));
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    private static void assertDark(BufferedImage image, int x, int y) {
        assertTrue(brightness(image, x, y) < DARK_THRESHOLD, "pixel (" + x + "," + y + ") harus gelap");
    }

    private static void assertLight(BufferedImage image, int x, int y) {
        assertTrue(brightness(image, x, y) > LIGHT_THRESHOLD, "pixel (" + x + "," + y + ") harus terang");
    }

    private static boolean hasDarkPixel(BufferedImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (brightness(image, x, y) < DARK_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }
}
