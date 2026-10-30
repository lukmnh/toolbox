package io.github.lukmnh.toolbox.productivity.budget.domain;

/** Teks tetap di file export (Excel & PDF). Isinya disediakan presentation (i18n), bukan di-hard-code di infrastructure. */
public enum WorkbookLabel {
    SUMMARY_SHEET,
    DEFAULT_SHEET,
    LEGEND,
    HEADER_NO,
    HEADER_NAME,
    HEADER_BRAND,
    HEADER_NETTO,
    HEADER_DURATION,
    HEADER_UNIT,
    HEADER_PRICE,
    HEADER_MONTHLY,
    HEADER_STATUS,
    HEADER_NOTE,
    HEADER_CATEGORY,
    CATEGORY_SECTION,
    CATEGORY_NONE,
    UNIT_DAYS,
    UNIT_WEEKS,
    UNIT_MONTHS,
    UNIT_YEARS,
    STATUS_ACTIVE,
    STATUS_INACTIVE,
    TOTAL_ACTIVE,
    INCOME_SECTION,
    INCOME_SOURCE,
    INCOME_AMOUNT,
    INCOME_NOTE,
    INCOME_TOTAL,
    EXPENSE_SECTION,
    EXPENSE_SHEET,
    EXPENSE_PRICE,
    EXPENSE_MONTHLY,
    EXPENSE_TOTAL,
    METRIC_INCOME,
    METRIC_EXPENSE,
    METRIC_REMAINING,
    METRIC_RATIO,
    METRIC_PER_YEAR;

    public static WorkbookLabel unit(DurationUnit unit) {
        return switch (unit) {
            case DAYS -> UNIT_DAYS;
            case WEEKS -> UNIT_WEEKS;
            case MONTHS -> UNIT_MONTHS;
            case YEARS -> UNIT_YEARS;
        };
    }

    public static WorkbookLabel status(ItemStatus status) {
        return switch (status) {
            case ACTIVE -> STATUS_ACTIVE;
            case INACTIVE -> STATUS_INACTIVE;
        };
    }
}
