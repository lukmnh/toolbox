package io.github.lukmnh.toolbox.json.viewer.domain;

/**
 * Filter view: quick search (teks mentah) dan filter query. Keduanya opsional; bila diisi semua, record harus
 * cocok dengan keduanya.
 */
public record ViewFilter(String quickSearch, String query) {

    public static final ViewFilter NONE = new ViewFilter("", "");

    public ViewFilter {
        quickSearch = quickSearch == null ? "" : quickSearch.strip();
        query = query == null ? "" : query.strip();
    }

    public boolean hasQuickSearch() {
        return !quickSearch.isEmpty();
    }

    public boolean hasQuery() {
        return !query.isEmpty();
    }

    public boolean isEmpty() {
        return !hasQuickSearch() && !hasQuery();
    }
}
