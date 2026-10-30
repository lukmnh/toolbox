package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException.Problem;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.BudgetDocument.CategoryDocument;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.BudgetDocument.IncomeDocument;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.BudgetDocument.ItemDocument;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.BudgetDocument.SheetDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Konversi {@link BudgetDocument} ↔ domain, sekaligus validasi skema: field wajib, angka tidak negatif, id unik.
 * Setiap pelanggaran menyebut lokasi field (bukan isinya).
 */
final class BudgetDocumentMapper {

    private final String fileName;

    private BudgetDocumentMapper(String fileName) {
        this.fileName = fileName;
    }

    static BudgetDocument toDocument(Budget budget) {
        return new BudgetDocument(
                BudgetDocument.CURRENT_VERSION,
                budget.name(),
                budget.created().toString(),
                budget.modified().toString(),
                budget.incomes().stream()
                        .map(income -> new IncomeDocument(income.id(), income.name(), income.amount(), income.note()))
                        .toList(),
                budget.sheets().stream().map(BudgetDocumentMapper::toDocument).toList());
    }

    private static SheetDocument toDocument(ExpenseSheet sheet) {
        return new SheetDocument(
                sheet.id(),
                sheet.name(),
                sheet.categories().stream()
                        .map(category -> new CategoryDocument(category.id(), category.name(), category.color()))
                        .toList(),
                sheet.items().stream().map(BudgetDocumentMapper::toDocument).toList());
    }

    private static ItemDocument toDocument(ExpenseItem item) {
        return new ItemDocument(
                item.id(),
                item.name(),
                item.brand(),
                item.netto(),
                item.categoryId(),
                item.price(),
                item.duration(),
                item.unit(),
                item.status(),
                item.note());
    }

    /** @throws InvalidBudgetFileException bila dokumen tidak sesuai skema */
    static Budget toDomain(BudgetDocument document, String fileName) {
        return new BudgetDocumentMapper(fileName).budget(document);
    }

    private Budget budget(BudgetDocument document) {
        Integer version = require(document.version(), "version");
        if (version != BudgetDocument.CURRENT_VERSION) {
            throw invalid(Problem.UNSUPPORTED_VERSION, "version", null);
        }
        List<Income> incomes = mapAll(require(document.incomes(), "incomes"), "incomes", this::income);
        List<ExpenseSheet> sheets = mapAll(require(document.sheets(), "sheets"), "sheets", this::sheet);
        requireUniqueIds(incomes, Income::id, "incomes");
        requireUniqueIds(sheets, ExpenseSheet::id, "sheets");
        return construct(
                "",
                () -> new Budget(
                        require(document.name(), "name"),
                        instant(document.created(), "created"),
                        instant(document.modified(), "modified"),
                        incomes,
                        sheets));
    }

    private Income income(IncomeDocument document, String path) {
        BigDecimal amount = nonNegative(document.amount(), path + ".amount");
        return construct(
                path, () -> new Income(require(document.id(), path + ".id"), document.name(), amount, document.note()));
    }

    private ExpenseSheet sheet(SheetDocument document, String path) {
        List<Category> categories =
                mapAll(require(document.categories(), path + ".categories"), path + ".categories", this::category);
        List<ExpenseItem> items = mapAll(require(document.items(), path + ".items"), path + ".items", this::item);
        requireUniqueIds(categories, Category::id, path + ".categories");
        requireUniqueIds(items, ExpenseItem::id, path + ".items");
        return construct(
                        path,
                        () -> new ExpenseSheet(
                                require(document.id(), path + ".id"),
                                require(document.name(), path + ".name"),
                                categories,
                                items))
                .withValidCategoryReferences();
    }

    private Category category(CategoryDocument document, String path) {
        return construct(
                path,
                () -> new Category(
                        require(document.id(), path + ".id"),
                        require(document.name(), path + ".name"),
                        require(document.color(), path + ".color")));
    }

    private ExpenseItem item(ItemDocument document, String path) {
        BigDecimal price = nonNegative(document.price(), path + ".price");
        BigDecimal duration = nonNegative(document.duration(), path + ".duration");
        return construct(
                path,
                () -> new ExpenseItem(
                        require(document.id(), path + ".id"),
                        document.name(),
                        document.brand(),
                        document.netto(),
                        document.categoryId(),
                        price,
                        duration,
                        require(document.unit(), path + ".unit"),
                        require(document.status(), path + ".status"),
                        document.note()));
    }

    private <D, T> List<T> mapAll(List<D> documents, String path, BiFunction<D, String, T> mapper) {
        List<T> mapped = new ArrayList<>(documents.size());
        for (int index = 0; index < documents.size(); index++) {
            String elementPath = path + "[" + index + "]";
            mapped.add(mapper.apply(require(documents.get(index), elementPath), elementPath));
        }
        return mapped;
    }

    private <T> void requireUniqueIds(List<T> elements, Function<T, String> id, String path) {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < elements.size(); index++) {
            if (!seen.add(id.apply(elements.get(index)))) {
                throw invalid(Problem.INVALID_VALUE, path + "[" + index + "].id", null);
            }
        }
    }

    private BigDecimal nonNegative(BigDecimal value, String path) {
        BigDecimal number = require(value, path);
        if (number.signum() < 0) {
            throw invalid(Problem.NEGATIVE_NUMBER, path, null);
        }
        return number;
    }

    private Instant instant(String value, String path) {
        try {
            return Instant.parse(require(value, path));
        } catch (DateTimeParseException e) {
            throw invalid(Problem.INVALID_VALUE, path, e);
        }
    }

    private <T> T require(T value, String path) {
        if (value == null) {
            throw invalid(Problem.MISSING_VALUE, path, null);
        }
        return value;
    }

    /** Validasi compact constructor domain (nama kosong, warna bukan hex, …) dipetakan ke lokasi field. */
    private <T> T construct(String path, Supplier<T> constructor) {
        try {
            return constructor.get();
        } catch (IllegalArgumentException e) {
            throw invalid(Problem.INVALID_VALUE, path, e);
        }
    }

    private InvalidBudgetFileException invalid(Problem problem, String path, Throwable cause) {
        return new InvalidBudgetFileException(fileName, problem, path, cause);
    }
}
