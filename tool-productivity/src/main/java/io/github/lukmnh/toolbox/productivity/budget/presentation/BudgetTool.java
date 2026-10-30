package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.util.Set;
import javafx.scene.Node;

/**
 * Budget pemasukan & pengeluaran: autosave JSON, ringkasan + grafik, export {@code .xlsx} ber-formula atau {@code .pdf},
 * dan import ulang dari file export tersebut.
 */
public final class BudgetTool implements Tool {

    private final BudgetUseCases useCases;
    private BudgetViewModel viewModel;

    public BudgetTool(BudgetUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return ProductivityToolIds.BUDGET;
    }

    @Override
    public String name() {
        return Messages.get("productivity.budget.name");
    }

    @Override
    public String description() {
        return Messages.get("productivity.budget.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PRODUCTIVITY;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-wallet-3-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new BudgetViewModel(useCases, context, new TempFiles());
        Node view = BudgetView.create(viewModel, context);
        viewModel.start();
        return view;
    }

    /** Selalu {@code true}: perubahan sudah/akan ter-autosave, dan {@link #dispose} menyimpan sisa yang tertunda. */
    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
