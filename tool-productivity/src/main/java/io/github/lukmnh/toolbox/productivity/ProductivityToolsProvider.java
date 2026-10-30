package io.github.lukmnh.toolbox.productivity;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import io.github.lukmnh.toolbox.productivity.budget.application.CreateBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.DeleteBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.DuplicateBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ExportBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ImportBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ListBudgetsUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.LoadBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.RenameBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.SaveBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.BudgetJsonCodec;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.ExportedBudgetImporter;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.FastExcelBudgetWorkbook;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.JsonBudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.PdfBudgetReport;
import io.github.lukmnh.toolbox.productivity.budget.presentation.BudgetTool;
import io.github.lukmnh.toolbox.productivity.budget.presentation.BudgetUseCases;
import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateDiscountUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.ExportScheduleUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.SimulateInstallmentUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.infrastructure.FastCsvScheduleWriter;
import io.github.lukmnh.toolbox.productivity.calculator.presentation.CalculatorTool;
import io.github.lukmnh.toolbox.productivity.calculator.presentation.CalculatorUseCases;
import io.github.lukmnh.toolbox.productivity.rename.application.ExecuteRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.FindLastRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.LoadFilesUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.PreviewRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.TwoPhaseRenamer;
import io.github.lukmnh.toolbox.productivity.rename.application.UndoLastRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.infrastructure.NioRenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.infrastructure.PropertiesRenameHistory;
import io.github.lukmnh.toolbox.productivity.rename.infrastructure.TimedPatternReplacer;
import io.github.lukmnh.toolbox.productivity.rename.presentation.RenameTool;
import io.github.lukmnh.toolbox.productivity.rename.presentation.RenameUseCases;
import io.github.lukmnh.toolbox.productivity.text.application.AnalyzeTextUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.ExportTextUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.LoadTextFileUseCase;
import io.github.lukmnh.toolbox.productivity.text.application.TransformTextUseCase;
import io.github.lukmnh.toolbox.productivity.text.domain.TextFiles;
import io.github.lukmnh.toolbox.productivity.text.infrastructure.JdkTextFiles;
import io.github.lukmnh.toolbox.productivity.text.presentation.TextTool;
import io.github.lukmnh.toolbox.productivity.text.presentation.TextUseCases;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

/**
 * Composition root module Produktivitas: merakit infrastructure → use case → tool secara manual (tanpa DI framework).
 * Didaftarkan di {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 */
public final class ProductivityToolsProvider implements ToolProvider {

    private static final String BUDGETS_DIRECTORY = "budgets";
    private static final String RENAME_HISTORY_DIRECTORY = "rename-history";

    @Override
    public List<Tool> tools() {
        return List.of(textTool(), renameTool(), calculatorTool(), budgetTool());
    }

    private static TextTool textTool() {
        TextFiles files = new JdkTextFiles();
        return new TextTool(new TextUseCases(
                new AnalyzeTextUseCase(),
                new TransformTextUseCase(),
                new LoadTextFileUseCase(files),
                new ExportTextUseCase(files)));
    }

    private static RenameTool renameTool() {
        RenameFileSystem fileSystem = new NioRenameFileSystem();
        RenameHistory history = new PropertiesRenameHistory(dataDirectory(RENAME_HISTORY_DIRECTORY));
        TwoPhaseRenamer renamer = new TwoPhaseRenamer(fileSystem);
        FindLastRenameUseCase findLast = new FindLastRenameUseCase(history);
        return new RenameTool(new RenameUseCases(
                new LoadFilesUseCase(fileSystem, SecurityLimits.RENAME_MAX_FILES),
                new PreviewRenameUseCase(fileSystem, new TimedPatternReplacer(), ZoneId.systemDefault()),
                new ExecuteRenameUseCase(
                        fileSystem, renamer, history, Clock.systemUTC(), SecurityLimits.RENAME_MAX_FILES),
                findLast,
                new UndoLastRenameUseCase(findLast, history, fileSystem, renamer)));
    }

    private static CalculatorTool calculatorTool() {
        return new CalculatorTool(new CalculatorUseCases(
                new CalculateDiscountUseCase(),
                new SimulateInstallmentUseCase(),
                new ExportScheduleUseCase(new FastCsvScheduleWriter())));
    }

    private static BudgetTool budgetTool() {
        BudgetJsonCodec codec = new BudgetJsonCodec();
        BudgetRepository repository = new JsonBudgetRepository(dataDirectory(BUDGETS_DIRECTORY), codec);
        Clock clock = Clock.systemUTC();
        return new BudgetTool(new BudgetUseCases(
                new ListBudgetsUseCase(repository),
                new LoadBudgetUseCase(repository),
                new CreateBudgetUseCase(repository, clock),
                new SaveBudgetUseCase(repository, clock),
                new RenameBudgetUseCase(repository, clock),
                new DuplicateBudgetUseCase(repository, clock),
                new DeleteBudgetUseCase(repository),
                new ImportBudgetUseCase(new ExportedBudgetImporter(codec), repository, clock),
                new ExportBudgetUseCase(new FastExcelBudgetWorkbook(codec)),
                new ExportBudgetUseCase(new PdfBudgetReport(codec))));
    }

    private static Path dataDirectory(String name) {
        return AppDirectories.dataDirectory().resolve(name);
    }
}
