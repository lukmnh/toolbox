package io.github.lukmnh.toolbox.pdf;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import io.github.lukmnh.toolbox.pdf.compress.application.CompressPdfUseCase;
import io.github.lukmnh.toolbox.pdf.compress.infrastructure.PdfBoxCompressor;
import io.github.lukmnh.toolbox.pdf.compress.presentation.CompressTool;
import io.github.lukmnh.toolbox.pdf.merge.application.MergePdfUseCase;
import io.github.lukmnh.toolbox.pdf.merge.infrastructure.PdfBoxMerger;
import io.github.lukmnh.toolbox.pdf.merge.presentation.MergeTool;
import io.github.lukmnh.toolbox.pdf.shared.application.InspectPdfUseCase;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.FontResolver;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfBoxInspector;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.sign.application.ExportEditedPdfUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.ImportSignatureImageUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.LoadLastSignatureUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.OpenDocumentUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.RenderPageUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.SaveDrawnSignatureUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.UpdateFormFieldUseCase;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureImageCleaner;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRasterizer;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRepository;
import io.github.lukmnh.toolbox.pdf.sign.infrastructure.FileSignatureRepository;
import io.github.lukmnh.toolbox.pdf.sign.infrastructure.PdfBoxEditor;
import io.github.lukmnh.toolbox.pdf.sign.presentation.SignTool;
import io.github.lukmnh.toolbox.pdf.sign.presentation.SignUseCases;
import io.github.lukmnh.toolbox.pdf.split.application.SplitPdfUseCase;
import io.github.lukmnh.toolbox.pdf.split.infrastructure.PdfBoxSplitter;
import io.github.lukmnh.toolbox.pdf.split.presentation.SplitTool;
import java.util.List;

/**
 * Composition root module PDF: merakit infrastructure → use case → tool secara manual (tanpa DI framework).
 * Didaftarkan di {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 */
public final class PdfToolsProvider implements ToolProvider {

    @Override
    public List<Tool> tools() {
        PdfLoader loader = new PdfLoader();
        InspectPdfUseCase inspect = new InspectPdfUseCase(new PdfBoxInspector(loader));
        return List.of(
                signTool(loader),
                new MergeTool(new MergePdfUseCase(new PdfBoxMerger(loader))),
                new SplitTool(new SplitPdfUseCase(new PdfBoxSplitter(loader)), inspect),
                new CompressTool(new CompressPdfUseCase(new PdfBoxCompressor(loader)), inspect));
    }

    private static SignTool signTool(PdfLoader loader) {
        SignatureRepository signatures = new FileSignatureRepository(AppDirectories.dataDirectory());
        SignUseCases useCases = new SignUseCases(
                new OpenDocumentUseCase(new PdfBoxEditor(loader, new FontResolver())),
                new RenderPageUseCase(),
                new UpdateFormFieldUseCase(),
                new ExportEditedPdfUseCase(),
                new SaveDrawnSignatureUseCase(signatures, new SignatureRasterizer()),
                new ImportSignatureImageUseCase(signatures, new SignatureImageCleaner()),
                new LoadLastSignatureUseCase(signatures));
        return new SignTool(useCases);
    }
}
