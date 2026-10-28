package io.github.lukmnh.toolbox.pdf.testsupport;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.text.PDFTextStripper;

/** Membuat PDF contoh on-the-fly untuk test, tanpa file fixture di repo. */
public final class TestPdfs {

    public static final String USER_PASSWORD = "rahasia";
    public static final String OWNER_PASSWORD = "pemilik";
    private static final long RANDOM_SEED = 42;

    private TestPdfs() {}

    /** PDF A4 dengan teks "Halaman N" di tiap halaman, supaya urutan halaman bisa diverifikasi. */
    public static Path withNumberedPages(Path file, int pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int page = 1; page <= pages; page++) {
                PDPage pdPage = new PDPage(PDRectangle.A4);
                document.addPage(pdPage);
                writeText(document, pdPage, "Halaman " + page);
            }
            document.save(file.toFile());
        }
        return file;
    }

    /** Halaman kosong tanpa content stream: cepat dibuat walau ribuan halaman. */
    public static Path blankPages(Path file, int pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pages; page++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            document.save(file.toFile());
        }
        return file;
    }

    public static Path encrypted(Path file, int pages) throws IOException {
        return encrypted(file, pages, new AccessPermission());
    }

    public static Path encrypted(Path file, int pages, AccessPermission permission) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pages; page++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            StandardProtectionPolicy policy = new StandardProtectionPolicy(OWNER_PASSWORD, USER_PASSWORD, permission);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(file.toFile());
        }
        return file;
    }

    /**
     * Satu halaman dengan rotasi dan CropBox tertentu. MediaBox A4; CropBox diberi offset agar mapping koordinat
     * benar-benar diuji.
     */
    public static Path withGeometry(Path file, int rotation, PDRectangle cropBox) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            page.setCropBox(cropBox);
            page.setRotation(rotation);
            document.addPage(page);
            document.save(file.toFile());
        }
        return file;
    }

    /** Satu halaman berisi gambar noise 2400×2400 px yang ditampilkan 200×200 pt (~864 DPI). */
    public static Path withHighResolutionImage(Path file) throws IOException {
        return withImage(file, noiseImage(2400, 2400, BufferedImage.TYPE_INT_RGB));
    }

    /** Gambar ARGB: LosslessFactory menyimpannya dengan SMask, sehingga harus dilewati oleh kompresor. */
    public static Path withAlphaImage(Path file) throws IOException {
        return withImage(file, noiseImage(1200, 1200, BufferedImage.TYPE_INT_ARGB));
    }

    private static Path withImage(Path file, BufferedImage image) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject xObject = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(xObject, 100, 400, 200, 200);
            }
            document.save(file.toFile());
        }
        return file;
    }

    public static Path withTextField(Path file, String fieldName) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDAcroForm form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
            PDResources resources = new PDResources();
            resources.put(COSName.HELV, new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            form.setDefaultResources(resources);

            PDTextField field = new PDTextField(form);
            field.setPartialName(fieldName);
            field.setDefaultAppearance("/Helv 12 Tf 0 g");
            form.getFields().add(field);
            PDAnnotationWidget widget = field.getWidgets().getFirst();
            widget.setRectangle(new PDRectangle(50, 700, 300, 30));
            widget.setPage(page);
            widget.setPrinted(true);
            page.getAnnotations().add(widget);
            document.save(file.toFile());
        }
        return file;
    }

    public static String extractText(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    public static int pageCount(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return document.getNumberOfPages();
        }
    }

    /** {@code true} bila file tidak bisa dibuka tanpa password tetapi bisa dibuka dengan {@link #USER_PASSWORD}. */
    public static boolean needsUserPassword(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return false;
        } catch (InvalidPasswordException e) {
            try (PDDocument document = Loader.loadPDF(file.toFile(), USER_PASSWORD)) {
                return document.isEncrypted();
            }
        }
    }

    private static void writeText(PDDocument document, PDPage page, String text) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 24);
            content.newLineAtOffset(72, 700);
            content.showText(text);
            content.endText();
        }
    }

    /** Noise acak sulit dikompres Flate, sehingga JPEG + downscale pasti jauh lebih kecil. */
    private static BufferedImage noiseImage(int width, int height, int type) {
        Random random = new Random(RANDOM_SEED);
        BufferedImage image = new BufferedImage(width, height, type);
        boolean alpha = type == BufferedImage.TYPE_INT_ARGB;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = random.nextInt(0x1000000);
                image.setRGB(x, y, alpha ? (0x80 << 24) | rgb : rgb);
            }
        }
        return image;
    }
}
