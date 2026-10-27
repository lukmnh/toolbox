package io.github.lukmnh.toolbox.pdf.split.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.PageRange;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.split.domain.PdfSplitter;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitSession;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class PdfBoxSplitter implements PdfSplitter {

    private final PdfLoader loader;

    public PdfBoxSplitter(PdfLoader loader) {
        this.loader = loader;
    }

    @Override
    public SplitSession open(PdfSource source) {
        return new Session(loader, source, loader.load(source));
    }

    private static final class Session implements SplitSession {

        private static final System.Logger LOG = System.getLogger(PdfBoxSplitter.class.getName());

        private final PdfLoader loader;
        private final PdfSource source;
        private final PDDocument document;

        private Session(PdfLoader loader, PdfSource source, PDDocument document) {
            this.loader = loader;
            this.source = source;
            this.document = document;
        }

        @Override
        public int pageCount() {
            return document.getNumberOfPages();
        }

        @Override
        public void extract(PageRange range, Path output) {
            List<PDDocument> parts = splitRange(range);
            try {
                loader.save(parts.getFirst(), output);
            } finally {
                parts.forEach(Session::closeQuietly);
            }
        }

        private List<PDDocument> splitRange(PageRange range) {
            Splitter splitter = new Splitter();
            splitter.setStartPage(range.first());
            splitter.setEndPage(range.last());
            // Satu potongan berisi seluruh range
            splitter.setSplitAtPage(range.pageCount());
            try {
                return splitter.split(document);
            } catch (IOException e) {
                throw new PdfIoException(source.file(), e);
            }
        }

        @Override
        public void close() {
            closeQuietly(document);
        }

        private static void closeQuietly(PDDocument part) {
            try {
                part.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Gagal menutup dokumen", e);
            }
        }
    }
}
