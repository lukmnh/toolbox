package io.github.lukmnh.toolbox.json.viewer.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Filter query yang sudah di-parse oleh {@link QueryParser}.
 *
 * <p>Aturan perbandingan: angka dibandingkan secara numerik ({@code 1 = 1.0}), string secara leksikal. Tipe
 * berbeda tidak pernah cocok kecuali untuk {@code !=}. Field yang tidak ada hanya cocok dengan {@code !=}.
 * {@code contains} dan {@code startsWith} tidak peka huruf besar/kecil.
 */
public sealed interface Query {

    boolean matches(JsonValue record);

    record And(Query left, Query right) implements Query {
        @Override
        public boolean matches(JsonValue record) {
            return left.matches(record) && right.matches(record);
        }
    }

    record Or(Query left, Query right) implements Query {
        @Override
        public boolean matches(JsonValue record) {
            return left.matches(record) || right.matches(record);
        }
    }

    /** Field ada (nilainya boleh {@code null}). */
    record Exists(FieldPath path) implements Query {
        @Override
        public boolean matches(JsonValue record) {
            return path.resolve(record).isPresent();
        }
    }

    /** Field ada dan bernilai {@code null}. */
    record IsNull(FieldPath path) implements Query {
        @Override
        public boolean matches(JsonValue record) {
            return path.resolve(record)
                    .filter(JsonValue.JsonNull.class::isInstance)
                    .isPresent();
        }
    }

    /** @param literal string, angka, boolean, atau null */
    record Comparison(FieldPath path, QueryOperator operator, JsonValue literal) implements Query {

        public Comparison {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(operator, "operator");
            if (literal.isContainer()) {
                throw new IllegalArgumentException("Literal harus scalar");
            }
        }

        @Override
        public boolean matches(JsonValue record) {
            Optional<JsonValue> actual = path.resolve(record);
            if (actual.isEmpty()) {
                return operator == QueryOperator.NOT_EQUALS;
            }
            JsonValue value = actual.get();
            return switch (operator) {
                case EQUALS -> isEqual(value, literal);
                case NOT_EQUALS -> !isEqual(value, literal);
                case GREATER -> compare(value, literal).filter(c -> c > 0).isPresent();
                case LESS -> compare(value, literal).filter(c -> c < 0).isPresent();
                case GREATER_OR_EQUAL ->
                    compare(value, literal).filter(c -> c >= 0).isPresent();
                case LESS_OR_EQUAL ->
                    compare(value, literal).filter(c -> c <= 0).isPresent();
                case CONTAINS -> textMatch(value, literal, String::contains);
                case STARTS_WITH -> textMatch(value, literal, String::startsWith);
            };
        }

        private static boolean isEqual(JsonValue value, JsonValue literal) {
            if (value instanceof JsonValue.JsonNumber number && literal instanceof JsonValue.JsonNumber expected) {
                return number.decimal().compareTo(expected.decimal()) == 0;
            }
            return value.equals(literal);
        }

        private static Optional<Integer> compare(JsonValue value, JsonValue literal) {
            if (value instanceof JsonValue.JsonNumber number && literal instanceof JsonValue.JsonNumber expected) {
                BigDecimal actual = number.decimal();
                return Optional.of(actual.compareTo(expected.decimal()));
            }
            if (value instanceof JsonValue.JsonString string && literal instanceof JsonValue.JsonString expected) {
                return Optional.of(string.value().compareTo(expected.value()));
            }
            return Optional.empty();
        }

        private static boolean textMatch(JsonValue value, JsonValue literal, TextPredicate predicate) {
            Optional<String> actual = scalarText(value);
            Optional<String> expected = scalarText(literal);
            if (actual.isEmpty() || expected.isEmpty()) {
                return false;
            }
            return predicate.test(
                    actual.get().toLowerCase(Locale.ROOT), expected.get().toLowerCase(Locale.ROOT));
        }

        private static Optional<String> scalarText(JsonValue value) {
            return switch (value) {
                case JsonValue.JsonString string -> Optional.of(string.value());
                case JsonValue.JsonNumber number -> Optional.of(number.text());
                case JsonValue.JsonBoolean bool -> Optional.of(String.valueOf(bool.value()));
                default -> Optional.empty();
            };
        }

        @FunctionalInterface
        private interface TextPredicate {
            boolean test(String actual, String expected);
        }
    }
}
