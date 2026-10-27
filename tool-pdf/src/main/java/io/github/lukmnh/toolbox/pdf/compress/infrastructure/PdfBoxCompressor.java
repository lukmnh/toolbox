package io.github.lukmnh.toolbox.pdf.compress.infrastructure;

import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionSession;
import io.github.lukmnh.toolbox.pdf.compress.domain.PdfCompressor;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class PdfBoxCompressor implements PdfCompressor {

    private final PdfLoader loader;

    public PdfBoxCompressor(PdfLoader loader) {
        this.loader = loader;
    }

    @Override
    public CompressionSession open(PdfSource source) {
        return new Session(loader, source, loader.load(source));
    }

    private static final class Session implements CompressionSession {

        private static final System.Logger LOG = System.getLogger(PdfBoxCompressor.class.getName());

        private final PdfLoader loader;
        private final PdfSource source;
        private final PDDocument document;
        private final ImageUsageCollector collector = new ImageUsageCollector();
        private final ImageRecompressor recompressor;
        private List<ImageUsageCollector.ImageUsage> images = List.of();

        private Session(PdfLoader loader, PdfSource source, PDDocument document) {
            this.loader = loader;
            this.source = source;
            this.document = document;
            this.recompressor = new ImageRecompressor(document);
        }

        @Override
        public int pageCount() {
            return document.getNumberOfPages();
        }

        @Override
        public long originalSize() {
            return loader.sizeOf(source.file());
        }

        @Override
        public void analyzePage(int pageIndex) {
            try {
                collector.collect(document.getPage(pageIndex));
            } catch (IOException e) {
                throw new CorruptPdfException(source.file(), e);
            }
            images = collector.usages();
        }

        @Override
        public int imageCount() {
            return images.size();
        }

        @Override
        public boolean recompressImage(int imageIndex, CompressionPreset preset) {
            ImageUsageCollector.ImageUsage usage = images.get(imageIndex);
            try {
                return recompressor.recompress(usage.image(), usage.maxEffectiveDpi(), preset);
            } catch (IOException e) {
                // Satu gambar rusak tidak boleh menggagalkan seluruh dokumen; gambar itu dibiarkan apa adanya
                LOG.log(Level.WARNING, "Gambar #" + imageIndex + " dilewati di " + source.file(), e);
                return false;
            }
        }

        @Override
        public long save(Path output) {
            loader.save(document, output);
            return loader.sizeOf(output);
        }

        @Override
        public void copyOriginal(Path output) {
            try {
                Files.copy(source.file(), output, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new PdfIoException(output, e);
            }
        }

        @Override
        public void close() {
            try {
                document.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Gagal menutup dokumen", e);
            }
        }
    }
}
