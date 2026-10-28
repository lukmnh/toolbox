package io.github.lukmnh.toolbox.pdf.merge.infrastructure;

import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeSession;
import io.github.lukmnh.toolbox.pdf.merge.domain.PdfMerger;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfProtection;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class PdfBoxMerger implements PdfMerger {

    private final PdfLoader loader;

    public PdfBoxMerger(PdfLoader loader) {
        this.loader = loader;
    }

    @Override
    public MergeSession openSession() {
        return new Session(loader);
    }

    private static final class Session implements MergeSession {

        private static final System.Logger LOG = System.getLogger(PdfBoxMerger.class.getName());

        private final PdfLoader loader;
        private final PDFMergerUtility mergerUtility = new PDFMergerUtility();
        // Stream cache di temp file (folder owner-only aplikasi) supaya hasil gabungan besar tidak menumpuk di heap
        private final PDDocument destination = new PDDocument(MemoryUsageSetting.setupTempFileOnly()
                .setTempDir(TempFiles.applicationDirectory().toFile())
                .streamCache);
        // Sumber harus tetap terbuka sampai destination disimpan karena resource-nya dipakai bersama
        private final List<PDDocument> openSources = new ArrayList<>();
        // Proteksi sumber terenkripsi pertama; dipakai untuk hasil bila user tidak memilih hapus password
        private Optional<PdfProtection> protection = Optional.empty();

        private Session(PdfLoader loader) {
            this.loader = loader;
        }

        @Override
        public void append(PdfSource source) {
            PDDocument document = loader.load(source);
            openSources.add(document);
            if (protection.isEmpty()) {
                protection = loader.protectionOf(document, source);
            }
            try {
                mergerUtility.appendDocument(destination, document);
            } catch (IOException e) {
                throw new CorruptPdfException(source.file(), e);
            }
        }

        @Override
        public int pageCount() {
            return destination.getNumberOfPages();
        }

        @Override
        public void save(Path output, OutputProtection mode) {
            loader.save(destination, output, protection, mode);
        }

        @Override
        public void close() {
            openSources.forEach(Session::closeQuietly);
            openSources.clear();
            closeQuietly(destination);
        }

        private static void closeQuietly(PDDocument document) {
            try {
                document.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Gagal menutup dokumen", e);
            }
        }
    }
}
