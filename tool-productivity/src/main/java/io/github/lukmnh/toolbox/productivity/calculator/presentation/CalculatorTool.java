package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Kalkulator Cepat: Diskon Ganda dan Simulasi Cicilan (+ export jadwal CSV). */
public final class CalculatorTool implements Tool {

    private final CalculatorUseCases useCases;
    private InstallmentViewModel installmentViewModel;

    public CalculatorTool(CalculatorUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return ProductivityToolIds.CALCULATOR;
    }

    @Override
    public String name() {
        return Messages.get("productivity.calculator.name");
    }

    @Override
    public String description() {
        return Messages.get("productivity.calculator.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PRODUCTIVITY;
    }

    /** Input berupa angka yang diketik, bukan file. */
    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        DiscountViewModel discountViewModel = new DiscountViewModel(useCases.discount(), context.notifier());
        installmentViewModel = new InstallmentViewModel(useCases, context, new TempFiles());
        return CalculatorView.create(discountViewModel, installmentViewModel);
    }

    /** Diskon tidak punya task atau temp file; hanya tab cicilan yang perlu dibersihkan. */
    @Override
    public void dispose() {
        if (installmentViewModel != null) {
            installmentViewModel.dispose();
            installmentViewModel = null;
        }
    }
}
