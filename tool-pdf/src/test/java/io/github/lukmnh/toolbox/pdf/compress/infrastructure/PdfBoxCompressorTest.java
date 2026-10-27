package io.github.lukmnh.toolbox.pdf.compress.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.compress.application.CompressPdfUseCase;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressRequest;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressResult;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxCompressorTest {

    private final CompressPdfUseCase useCase = new CompressPdfUseCase(new PdfBoxCompressor(new PdfLoader()));

    @TempDir
    Path temp;

    @Test
    void highResolutionImageIsDownscaledToTargetDpi() throws IOException {
        Path source = TestPdfs.withHighResolutionImage(temp.resolve("scan.pdf"));
        Path output = temp.resolve("out.pdf");

        CompressResult result = compress(source, output, CompressionPreset.BALANCED);

        assertFalse(result.alreadyOptimal());
        assertEquals(1, result.imagesRecompressed());
        assertTrue(Files.size(output) < Files.size(source) / 4, "hasil harus jauh lebih kecil");
        ImageInfo image = onlyImage(output);
        // 200 pt × 150 DPI / 72 ≈ 417 px
        assertEquals(417, image.width(), 1);
        assertEquals(COSName.DCT_DECODE, image.filter());
    }

    @Test
    void imageWithAlphaIsLeftUntouched() throws IOException {
        Path source = TestPdfs.withAlphaImage(temp.resolve("alpha.pdf"));
        Path output = temp.resolve("out.pdf");

        CompressResult result = compress(source, output, CompressionPreset.SMALL_SIZE);

        assertTrue(result.alreadyOptimal());
        assertEquals(Files.size(source), Files.size(output));
        assertTrue(onlyImage(output).hasSoftMask());
    }

    @Test
    void textOnlyPdfIsAlreadyOptimal() throws IOException {
        Path source = TestPdfs.withNumberedPages(temp.resolve("teks.pdf"), 3);
        Path output = temp.resolve("out.pdf");

        CompressResult result = compress(source, output, CompressionPreset.DEFAULT);

        assertTrue(result.alreadyOptimal());
        assertEquals(0, result.savingPercent());
        assertEquals(3, TestPdfs.pageCount(output));
    }

    private CompressResult compress(Path source, Path output, CompressionPreset preset) {
        return useCase.execute(
                new CompressRequest(PdfSource.of(source), preset, output),
                ProgressReporter.NONE,
                CancellationToken.none());
    }

    private record ImageInfo(int width, COSName filter, boolean hasSoftMask) {}

    /** Properti dibaca selagi dokumen masih terbuka. */
    private static ImageInfo onlyImage(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDResources resources = document.getPage(0).getResources();
            COSName name = resources.getXObjectNames().iterator().next();
            PDImageXObject image = (PDImageXObject) resources.getXObject(name);
            return new ImageInfo(
                    image.getWidth(),
                    image.getCOSObject().getCOSName(COSName.FILTER),
                    image.getCOSObject().getDictionaryObject(COSName.SMASK) != null);
        }
    }
}
