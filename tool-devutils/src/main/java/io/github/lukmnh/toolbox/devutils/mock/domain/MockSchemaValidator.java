package io.github.lukmnh.toolbox.devutils.mock.domain;

import io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException.Reason;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validasi sebelum generate supaya error muncul sekali di UI, bukan di tengah 100k baris. Parser range di sini juga
 * dipakai generator, jadi aturannya hanya satu.
 */
public final class MockSchemaValidator {

    public static final int MAX_ROWS = 100_000;
    public static final int MAX_COLUMNS = 30;
    public static final int MAX_PATTERN_LENGTH = 100;
    // Identifier aman untuk SQL tanpa quoting (mencegah injection lewat nama tabel/kolom)
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private static final String RANGE_SEPARATOR = "\\.\\.";

    private MockSchemaValidator() {}

    public static void validate(MockSchema schema) {
        if (schema.rows() < 1 || schema.rows() > MAX_ROWS) {
            throw new InvalidMockSchemaException(Reason.ROWS_OUT_OF_RANGE, String.valueOf(schema.rows()));
        }
        if (schema.columns().isEmpty()) {
            throw new InvalidMockSchemaException(Reason.NO_COLUMNS, "");
        }
        if (schema.columns().size() > MAX_COLUMNS) {
            throw new InvalidMockSchemaException(
                    Reason.TOO_MANY_COLUMNS, String.valueOf(schema.columns().size()));
        }
        if (!IDENTIFIER.matcher(schema.table()).matches()) {
            throw new InvalidMockSchemaException(Reason.INVALID_TABLE, schema.table());
        }
        Set<String> names = new HashSet<>();
        for (int i = 0; i < schema.columns().size(); i++) {
            MockColumn column = schema.columns().get(i);
            if (column.name().isBlank()) {
                throw new InvalidMockSchemaException(Reason.BLANK_NAME, String.valueOf(i + 1));
            }
            if (!IDENTIFIER.matcher(column.name()).matches()) {
                throw new InvalidMockSchemaException(Reason.INVALID_NAME, column.name());
            }
            if (!names.add(column.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidMockSchemaException(Reason.DUPLICATE_NAME, column.name());
            }
            validateOptions(column);
        }
    }

    private static void validateOptions(MockColumn column) {
        switch (column.type()) {
            case INTEGER -> longRange(column);
            case DECIMAL -> decimalRange(column);
            case DATE -> dateRange(column);
            case ENUM -> enumValues(column);
            case PATTERN -> pattern(column);
            default -> {
                // Tipe tanpa opsi
            }
        }
    }

    static long[] longRange(MockColumn column) {
        String[] bounds = bounds(column);
        try {
            long min = Long.parseLong(bounds[0]);
            long max = Long.parseLong(bounds[1]);
            if (min > max) {
                throw invalidRange(column);
            }
            return new long[] {min, max};
        } catch (NumberFormatException e) {
            throw invalidRange(column);
        }
    }

    static double[] decimalRange(MockColumn column) {
        String[] bounds = bounds(column);
        try {
            double min = Double.parseDouble(bounds[0]);
            double max = Double.parseDouble(bounds[1]);
            if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
                throw invalidRange(column);
            }
            return new double[] {min, max};
        } catch (NumberFormatException e) {
            throw invalidRange(column);
        }
    }

    static LocalDate[] dateRange(MockColumn column) {
        String[] bounds = bounds(column);
        try {
            LocalDate from = LocalDate.parse(bounds[0]);
            LocalDate to = LocalDate.parse(bounds[1]);
            if (from.isAfter(to)) {
                throw invalidRange(column);
            }
            return new LocalDate[] {from, to};
        } catch (DateTimeParseException e) {
            throw invalidRange(column);
        }
    }

    static List<String> enumValues(MockColumn column) {
        List<String> values = Arrays.stream(column.effectiveOptions().split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .toList();
        if (values.isEmpty()) {
            throw new InvalidMockSchemaException(Reason.EMPTY_ENUM, column.name());
        }
        return values;
    }

    static String pattern(MockColumn column) {
        String pattern = column.effectiveOptions();
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw new InvalidMockSchemaException(Reason.INVALID_PATTERN, column.name());
        }
        return pattern;
    }

    private static String[] bounds(MockColumn column) {
        String[] parts = column.effectiveOptions().split(RANGE_SEPARATOR, -1);
        if (parts.length != 2) {
            throw invalidRange(column);
        }
        return new String[] {parts[0].strip(), parts[1].strip()};
    }

    private static InvalidMockSchemaException invalidRange(MockColumn column) {
        return new InvalidMockSchemaException(Reason.INVALID_RANGE, column.name());
    }
}
