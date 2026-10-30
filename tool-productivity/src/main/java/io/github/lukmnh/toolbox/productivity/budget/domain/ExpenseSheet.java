package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Satu daftar pengeluaran (= satu sheet Excel), misalnya "Skincare" atau "Transport". Immutable: setiap perubahan
 * menghasilkan instance baru.
 */
public record ExpenseSheet(String id, String name, List<Category> categories, List<ExpenseItem> items) {

    public ExpenseSheet {
        id = Ids.require(id, "id daftar");
        name = Texts.requiredName(name, "nama daftar");
        categories = List.copyOf(categories);
        items = List.copyOf(items);
    }

    public static ExpenseSheet create(String name) {
        return new ExpenseSheet(Ids.next(), name, List.of(), List.of());
    }

    public ExpenseSheet withName(String newName) {
        return new ExpenseSheet(id, newName, categories, items);
    }

    public ExpenseSheet withItems(List<ExpenseItem> newItems) {
        return new ExpenseSheet(id, name, categories, newItems);
    }

    public Optional<Category> category(String categoryId) {
        return categories.stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst();
    }

    public Optional<ExpenseItem> item(String itemId) {
        return items.stream().filter(item -> item.id().equals(itemId)).findFirst();
    }

    public int indexOf(String itemId) {
        return ListEdits.indexOf(items, ExpenseItem::id, itemId);
    }

    public ExpenseSheet addCategory(Category category) {
        return new ExpenseSheet(id, name, ListEdits.append(categories, category), items);
    }

    public ExpenseSheet updateCategory(String categoryId, UnaryOperator<Category> change) {
        return new ExpenseSheet(id, name, ListEdits.replace(categories, Category::id, categoryId, change), items);
    }

    /** Item yang memakai kategori ini menjadi tanpa kategori, bukan ikut terhapus. */
    public ExpenseSheet removeCategory(String categoryId) {
        List<ExpenseItem> detached = items.stream()
                .map(item -> item.categoryId().equals(categoryId) ? item.withCategoryId(ExpenseItem.NO_CATEGORY) : item)
                .toList();
        return new ExpenseSheet(id, name, ListEdits.removeIf(categories, Category::id, categoryId), detached);
    }

    public ExpenseSheet addItem(ExpenseItem item) {
        return withItems(ListEdits.append(items, item));
    }

    public ExpenseSheet insertItem(int index, ExpenseItem item) {
        return withItems(ListEdits.insert(items, index, item));
    }

    public ExpenseSheet updateItem(String itemId, UnaryOperator<ExpenseItem> change) {
        return withItems(ListEdits.replace(items, ExpenseItem::id, itemId, change));
    }

    public ExpenseSheet removeItems(Set<String> itemIds) {
        return withItems(
                items.stream().filter(item -> !itemIds.contains(item.id())).toList());
    }

    /** Duplikat diletakkan tepat di bawah item asal. */
    public ExpenseSheet duplicateItem(String itemId) {
        int index = indexOf(itemId);
        return index < 0 ? this : insertItem(index + 1, items.get(index).copy());
    }

    public ExpenseSheet moveItem(String itemId, int delta) {
        return withItems(ListEdits.move(items, ExpenseItem::id, itemId, delta));
    }

    /** Referensi kategori yang tidak ada (misalnya file hasil edit manual) dianggap tanpa kategori. */
    public ExpenseSheet withValidCategoryReferences() {
        Set<String> known = categories.stream().map(Category::id).collect(Collectors.toSet());
        boolean allValid = items.stream().allMatch(item -> !item.hasCategory() || known.contains(item.categoryId()));
        if (allValid) {
            return this;
        }
        return withItems(items.stream()
                .map(item -> item.hasCategory() && !known.contains(item.categoryId())
                        ? item.withCategoryId(ExpenseItem.NO_CATEGORY)
                        : item)
                .toList());
    }
}
