package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Installment Simulator: simulasi cicilan (+ export PDF/CSV), diskon ganda, pajak & PPN, dan invoice/kuitansi. */
public final class CalculatorTool implements Tool {

    private final CalculatorUseCases useCases;
    private InstallmentViewModel installmentViewModel;
    private TaxViewModel taxViewModel;
    private InvoiceViewModel invoiceViewModel;

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
        taxViewModel = new TaxViewModel(useCases.tax(), context.notifier());
        invoiceViewModel = new InvoiceViewModel(useCases, context, new TempFiles());
        return CalculatorView.create(installmentViewModel, discountViewModel, taxViewModel, invoiceViewModel);
    }

    /** Diskon tidak punya task, timer, atau temp file; tab lain perlu dibersihkan. */
    @Override
    public void dispose() {
        if (installmentViewModel != null) {
            installmentViewModel.dispose();
            installmentViewModel = null;
        }
        if (taxViewModel != null) {
            taxViewModel.dispose();
            taxViewModel = null;
        }
        if (invoiceViewModel != null) {
            invoiceViewModel.dispose();
            invoiceViewModel = null;
        }
    }
}
