package io.github.lukmnh.toolbox.pdf.compress.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressRequest;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressResult;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionSession;
import io.github.lukmnh.toolbox.pdf.compress.domain.PdfCompressor;

/**
 * Mengecilkan PDF dengan mengompres ulang gambar. Bila hasilnya tidak lebih kecil, output = salinan asli
 * dan hasil ditandai "sudah optimal".
 */
public final class CompressPdfUseCase {

    private final PdfCompressor compressor;

    public CompressPdfUseCase(PdfCompressor compressor) {
        this.compressor = compressor;
    }

    public CompressResult execute(CompressRequest request, ProgressReporter progress, CancellationToken cancellation) {
        try (CompressionSession session = compressor.open(request.source())) {
            int pages = session.pageCount();
            for (int page = 0; page < pages; page++) {
                cancellation.throwIfCancelled();
                session.analyzePage(page);
                // Analisis cepat; diberi bobot 1 unit per halaman sebelum jumlah gambar diketahui
                progress.report(page + 1L, pages + 1L);
            }
            int recompressed = recompressImages(session, request, progress, cancellation);
            cancellation.throwIfCancelled();
            return finish(session, request, recompressed);
        }
    }

    private static int recompressImages(
            CompressionSession session,
            CompressRequest request,
            ProgressReporter progress,
            CancellationToken cancellation) {
        int pages = session.pageCount();
        int images = session.imageCount();
        long totalSteps = (long) pages + images + 1;
        int recompressed = 0;
        for (int image = 0; image < images; image++) {
            cancellation.throwIfCancelled();
            if (session.recompressImage(image, request.preset())) {
                recompressed++;
            }
            progress.report(pages + image + 1L, totalSteps);
        }
        return recompressed;
    }

    private static CompressResult finish(CompressionSession session, CompressRequest request, int recompressed) {
        long originalSize = session.originalSize();
        long resultSize = recompressed == 0 ? originalSize : session.save(request.output(), request.protection());
        if (resultSize >= originalSize) {
            long copiedSize = session.copyOriginal(request.output(), request.protection());
            return new CompressResult(request.output(), originalSize, copiedSize, 0, true);
        }
        return new CompressResult(request.output(), originalSize, resultSize, recompressed, false);
    }
}
