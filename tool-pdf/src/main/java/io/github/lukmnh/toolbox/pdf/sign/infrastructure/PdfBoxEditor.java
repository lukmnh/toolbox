package io.github.lukmnh.toolbox.pdf.sign.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException.Limit;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.CoordinateMapper;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.FontResolver;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfProtection;
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
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class PdfBoxEditor implements PdfEditor {

    private final PdfLoader loader;
    private final FontResolver fontResolver;
    private final Duration renderTimeout;

    public PdfBoxEditor(PdfLoader loader, FontResolver fontResolver) {
        this(loader, fontResolver, SecurityLimits.PDF_RENDER_TIMEOUT);
    }

    /** @param renderTimeout batas waktu render satu halaman; bisa diperkecil di test */
    PdfBoxEditor(PdfLoader loader, FontResolver fontResolver, Duration renderTimeout) {
        this.loader = loader;
        this.fontResolver = fontResolver;
        this.renderTimeout = renderTimeout;
    }

    @Override
    public EditSession open(PdfSource source) {
        PDDocument document = loader.load(source);
        return new Session(
                loader, source, document, loader.protectionOf(document, source), fontResolver, renderTimeout);
    }

    /** {@link PDDocument} tidak thread-safe, jadi semua akses disinkronkan. */
    private static final class Session implements EditSession {

        private static final System.Logger LOG = System.getLogger(PdfBoxEditor.class.getName());

        private final PdfLoader loader;
        private final PdfSource source;
        private final PDDocument document;
        private final Optional<PdfProtection> protection;
        private final PDFRenderer renderer;
        private final FontSpec fontSpec;
        private final OverlayBurner burner;
        private final AcroFormAdapter forms = new AcroFormAdapter();
        private final Duration renderTimeout;
        // Render di thread terpisah supaya bisa diberi timeout; PDF jahat bisa membuat render tidak pernah selesai
        private final ExecutorService renderThread = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().daemon().name("pdf-render").factory());

        /** Halaman (1-based) yang render-nya macet; sejak itu dokumen tidak boleh disentuh lagi. */
        private int stuckPage;

        private Session(
                PdfLoader loader,
                PdfSource source,
                PDDocument document,
                Optional<PdfProtection> protection,
                FontResolver fontResolver,
                Duration renderTimeout) {
            this.loader = loader;
            this.source = source;
            this.document = document;
            this.protection = protection;
            this.renderer = new PDFRenderer(document);
            this.fontSpec = fontResolver.resolve();
            this.burner = new OverlayBurner(fontResolver, fontSpec);
            this.renderTimeout = renderTimeout;
        }

        @Override
        public synchronized int pageCount() {
            requireResponsive();
            return document.getNumberOfPages();
        }

        @Override
        public synchronized PageGeometry geometry(int pageIndex) {
            requireResponsive();
            CoordinateMapper mapper = new CoordinateMapper(OverlayBurner.pageBox(document.getPage(pageIndex)));
            return new PageGeometry(mapper.displayWidth(), mapper.displayHeight());
        }

        @Override
        public synchronized BufferedImage render(int pageIndex, int dpi) {
            requireResponsive();
            Future<BufferedImage> rendering =
                    renderThread.submit(() -> renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB));
            return awaitRender(rendering, pageIndex + 1);
        }

        /**
         * Lock dokumen tetap dipegang sampai render selesai, termasuk bila task pemanggil dibatalkan (interrupt),
         * supaya thread render tidak pernah berjalan bersamaan dengan operasi lain pada dokumen.
         */
        private BufferedImage awaitRender(Future<BufferedImage> rendering, int pageNumber) {
            long deadline = System.nanoTime() + renderTimeout.toNanos();
            boolean interrupted = false;
            try {
                while (true) {
                    try {
                        return rendering.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } catch (TimeoutException e) {
                // Render PDFBox tidak bisa dihentikan paksa; tandai dokumen tidak aman dipakai lagi
                stuckPage = pageNumber;
                rendering.cancel(true);
                throw renderTimeout();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Error error) {
                    throw error;
                }
                throw renderFailure(e.getCause());
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private RuntimeException renderFailure(Throwable cause) {
            return switch (cause) {
                case IOException io -> new CorruptPdfException(source.file(), io);
                case RuntimeException runtime -> runtime;
                default -> new CorruptPdfException(source.file(), cause);
            };
        }

        private PdfLimitExceededException renderTimeout() {
            return new PdfLimitExceededException(
                    source.file(), Limit.RENDER_TIME, stuckPage, renderTimeout.toSeconds());
        }

        private void requireResponsive() {
            if (stuckPage > 0) {
                throw renderTimeout();
            }
        }

        @Override
        public synchronized List<FormField> formFields() {
            requireResponsive();
            return forms.read(document);
        }

        @Override
        public synchronized void setFieldValue(String fieldName, FieldValue value) {
            requireResponsive();
            forms.apply(document, fieldName, value);
        }

        @Override
        public FontSpec fontSpec() {
            return fontSpec;
        }

        @Override
        public synchronized void export(List<Overlay> overlays, Path output, OutputProtection mode) {
            requireResponsive();
            try (PDDocument copy = Loader.loadPDF(snapshot())) {
                burner.burn(copy, overlays);
                // Salinan tidak terenkripsi; proteksi sumber (dicatat saat open) di-apply ulang kecuali user menolak
                loader.save(copy, output, protection, mode);
            } catch (IOException e) {
                throw new PdfIoException(output, e);
            }
        }

        /** Salinan dokumen saat ini (termasuk isian form) tanpa enkripsi, hanya di memory. */
        private byte[] snapshot() throws IOException {
            document.setAllSecurityToBeRemoved(true);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            document.save(buffer);
            return buffer.toByteArray();
        }

        @Override
        public synchronized void close() {
            renderThread.shutdownNow();
            try {
                document.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Gagal menutup dokumen", e);
            }
        }
    }
}
