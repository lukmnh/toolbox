package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.UUID;

/** Penghasil nilai satu kolom. Opsi (range, enum, pattern) di-parse sekali saat dibuat, bukan per baris. */
@FunctionalInterface
interface ColumnSource {

    String DIGITS = "0123456789";
    String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String ALPHANUMERIC = DIGITS + LETTERS;
    int MAX_HOUSE_NUMBER = 200;
    int MAX_STREET_NUMBER = 9999;
    int EMAIL_SUFFIX_BOUND = 100;

    /** @param row nomor baris mulai 1 */
    String next(SplittableRandom random, int row);

    static ColumnSource of(MockColumn column, MockLocale locale) {
        MockWords words = MockWords.of(locale);
        return switch (column.type()) {
            case SEQUENCE -> (random, row) -> String.valueOf(row);
            case UUID -> (random, row) -> uuid(random);
            case NAME -> (random, row) -> pick(random, words.firstNames()) + " " + pick(random, words.lastNames());
            case EMAIL -> (random, row) -> email(random, words);
            case PHONE -> (random, row) -> fill(words.phonePattern(), random);
            case ADDRESS -> (random, row) -> address(random, words, locale);
            case CITY -> (random, row) -> pick(random, words.cities());
            case INTEGER -> integer(MockSchemaValidator.longRange(column));
            case DECIMAL -> decimal(MockSchemaValidator.decimalRange(column));
            case BOOLEAN -> (random, row) -> String.valueOf(random.nextBoolean());
            case DATE -> date(MockSchemaValidator.dateRange(column));
            case ENUM -> {
                List<String> values = MockSchemaValidator.enumValues(column);
                yield (random, row) -> pick(random, values);
            }
            case PATTERN -> {
                String pattern = MockSchemaValidator.pattern(column);
                yield (random, row) -> fill(pattern, random);
            }
        };
    }

    private static ColumnSource integer(long[] range) {
        long min = range[0];
        long max = range[1];
        // nextLong(origin, bound) eksklusif di bound; max = Long.MAX_VALUE tidak bisa +1
        return (random, row) ->
                String.valueOf(max == Long.MAX_VALUE ? random.nextLong(min, max) : random.nextLong(min, max + 1));
    }

    private static ColumnSource decimal(double[] range) {
        double min = range[0];
        double max = range[1];
        return (random, row) -> String.format(Locale.ROOT, "%.2f", min == max ? min : random.nextDouble(min, max));
    }

    private static ColumnSource date(LocalDate[] range) {
        long from = range[0].toEpochDay();
        long to = range[1].toEpochDay();
        return (random, row) ->
                LocalDate.ofEpochDay(random.nextLong(from, to + 1)).toString();
    }

    private static String pick(SplittableRandom random, List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    /** UUID versi 4 dari random ber-seed, supaya ikut reproducible (UUID.randomUUID tidak bisa di-seed). */
    private static String uuid(SplittableRandom random) {
        long high = (random.nextLong() & ~0xF000L) | 0x4000L;
        long low = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(high, low).toString();
    }

    private static String email(SplittableRandom random, MockWords words) {
        String local = (pick(random, words.firstNames()) + "." + pick(random, words.lastNames()))
                .toLowerCase(Locale.ROOT)
                .replace(" ", "");
        int suffix = random.nextInt(EMAIL_SUFFIX_BOUND);
        return local + suffix + "@" + pick(random, MockWords.EMAIL_DOMAINS);
    }

    private static String address(SplittableRandom random, MockWords words, MockLocale locale) {
        String street = pick(random, words.streets());
        return switch (locale) {
            case ID -> "Jl. " + street + " No. " + (random.nextInt(MAX_HOUSE_NUMBER) + 1);
            case EN -> (random.nextInt(MAX_STREET_NUMBER) + 1) + " " + street + " St";
        };
    }

    /** {@code #} = digit, {@code ?} = huruf besar, {@code *} = alfanumerik; karakter lain ditulis apa adanya. */
    static String fill(String pattern, SplittableRandom random) {
        StringBuilder out = new StringBuilder(pattern.length());
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '#' -> out.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
                case '?' -> out.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
                case '*' -> out.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
