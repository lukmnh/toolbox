package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.CoordinateMapper;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.FontResolver;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;
import io.github.lukmnh.toolbox.pdf.sign.domain.FormField;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import io.github.lukmnh.toolbox.pdf.sign.domain.PageGeometry;
import io.github.lukmnh.toolbox.pdf.sign.domain.PdfEditor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class PdfBoxEditor implements PdfEditor {

    private final PdfLoader loader;
    private final FontResolver fontResolver;

    public PdfBoxEditor(PdfLoader loader, FontResolver fontResolver) {
        this.loader = loader;
        this.fontResolver = fontResolver;
    }

    @Override
    public EditSession open(PdfSource source) {
        return new Session(loader, source, loader.load(source), fontResolver);
    }

    /** {@link PDDocument} tidak thread-safe, jadi semua akses disinkronkan. */
    private static final class Session implements EditSession {

        private static final System.Logger LOG = System.getLogger(PdfBoxEditor.class.getName());

        private final PdfLoader loader;
        private final PdfSource source;
        private final PDDocument document;
        private final PDFRenderer renderer;
        private final FontSpec fontSpec;
        private final OverlayBurner burner;
        private final AcroFormAdapter forms = new AcroFormAdapter();

        private Session(PdfLoader loader, PdfSource source, PDDocument document, FontResolver fontResolver) {
            this.loader = loader;
            this.source = source;
            this.document = document;
            this.renderer = new PDFRenderer(document);
            this.fontSpec = fontResolver.resolve();
            this.burner = new OverlayBurner(fontResolver, fontSpec);
        }

        @Override
        public synchronized int pageCount() {
            return document.getNumberOfPages();
        }

        @Override
        public synchronized PageGeometry geometry(int pageIndex) {
            CoordinateMapper mapper = new CoordinateMapper(OverlayBurner.pageBox(document.getPage(pageIndex)));
            return new PageGeometry(mapper.displayWidth(), mapper.displayHeight());
        }

        @Override
        public synchronized BufferedImage render(int pageIndex, int dpi) {
            try {
                return renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
            } catch (IOException e) {
                throw new CorruptPdfException(source.file(), e);
            }
        }

        @Override
        public synchronized List<FormField> formFields() {
            return forms.read(document);
        }

        @Override
        public synchronized void setFieldValue(String fieldName, FieldValue value) {
            forms.apply(document, fieldName, value);
        }

        @Override
        public FontSpec fontSpec() {
            return fontSpec;
        }

        @Override
        public synchronized void export(List<Overlay> overlays, Path output) {
            try (PDDocument copy = Loader.loadPDF(snapshot())) {
                burner.burn(copy, overlays);
                loader.save(copy, output);
            } catch (IOException e) {
                throw new PdfIoException(output, e);
            }
        }

        /** Salinan dokumen saat ini (termasuk isian form) tanpa enkripsi. */
        private byte[] snapshot() throws IOException {
            document.setAllSecurityToBeRemoved(true);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            document.save(buffer);
            return buffer.toByteArray();
        }

        @Override
        public synchronized void close() {
            try {
                document.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Gagal menutup dokumen", e);
            }
        }
    }
}
