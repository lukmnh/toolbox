package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Daftar budget tersimpan, urut nama (tidak peka huruf besar/kecil & aksen). */
public final class ListBudgetsUseCase {

    private static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");

    private final BudgetRepository repository;

    public ListBudgetsUseCase(BudgetRepository repository) {
        this.repository = repository;
    }

    public List<BudgetEntry> execute() {
        Collator collator = Collator.getInstance(INDONESIAN);
        collator.setStrength(Collator.SECONDARY);
        return repository.list().stream()
                .sorted(Comparator.comparing(BudgetEntry::name, collator))
                .toList();
    }
}
