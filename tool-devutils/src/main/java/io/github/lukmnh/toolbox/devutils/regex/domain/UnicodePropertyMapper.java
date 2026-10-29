package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Nama properti Unicode JavaScript/PCRE → sintaks {@code java.util.regex}. Java hanya mengenal nama pendek general
 * category ({@code Lu}) dan script dengan prefix {@code Is}.
 */
final class UnicodePropertyMapper {

    private static final String ANY_CHARACTER = "\\x{0}-\\x{10FFFF}";
    private static final String CASED_LETTER = "\\p{Lu}\\p{Ll}\\p{Lt}";

    private static final Set<String> SHORT_CATEGORIES = Set.of(
            "L", "Lu", "Ll", "Lt", "Lm", "Lo", "M", "Mn", "Mc", "Me", "N", "Nd", "Nl", "No", "P", "Pc", "Pd", "Ps",
            "Pe", "Pi", "Pf", "Po", "S", "Sm", "Sc", "Sk", "So", "Z", "Zs", "Zl", "Zp", "C", "Cc", "Cf", "Cs", "Co",
            "Cn");

    private static final Map<String, String> LONG_CATEGORIES = Map.ofEntries(
            Map.entry("Letter", "L"),
            Map.entry("Uppercase_Letter", "Lu"),
            Map.entry("Lowercase_Letter", "Ll"),
            Map.entry("Titlecase_Letter", "Lt"),
            Map.entry("Modifier_Letter", "Lm"),
            Map.entry("Other_Letter", "Lo"),
            Map.entry("Mark", "M"),
            Map.entry("Combining_Mark", "M"),
            Map.entry("Nonspacing_Mark", "Mn"),
            Map.entry("Spacing_Mark", "Mc"),
            Map.entry("Enclosing_Mark", "Me"),
            Map.entry("Number", "N"),
            Map.entry("Decimal_Number", "Nd"),
            Map.entry("digit", "Nd"),
            Map.entry("Letter_Number", "Nl"),
            Map.entry("Other_Number", "No"),
            Map.entry("Punctuation", "P"),
            Map.entry("punct", "P"),
            Map.entry("Connector_Punctuation", "Pc"),
            Map.entry("Dash_Punctuation", "Pd"),
            Map.entry("Open_Punctuation", "Ps"),
            Map.entry("Close_Punctuation", "Pe"),
            Map.entry("Initial_Punctuation", "Pi"),
            Map.entry("Final_Punctuation", "Pf"),
            Map.entry("Other_Punctuation", "Po"),
            Map.entry("Symbol", "S"),
            Map.entry("Math_Symbol", "Sm"),
            Map.entry("Currency_Symbol", "Sc"),
            Map.entry("Modifier_Symbol", "Sk"),
            Map.entry("Other_Symbol", "So"),
            Map.entry("Separator", "Z"),
            Map.entry("Space_Separator", "Zs"),
            Map.entry("Line_Separator", "Zl"),
            Map.entry("Paragraph_Separator", "Zp"),
            Map.entry("Other", "C"),
            Map.entry("Control", "Cc"),
            Map.entry("cntrl", "Cc"),
            Map.entry("Format", "Cf"),
            Map.entry("Surrogate", "Cs"),
            Map.entry("Private_Use", "Co"),
            Map.entry("Unassigned", "Cn"));

    /** Properti khusus PCRE ({@code \\p{Xan}} dst) tanpa padanan Java. */
    private static final Set<String> PCRE_SPECIALS = Set.of("Xan", "Xps", "Xsp", "Xwd", "Xuc");

    private UnicodePropertyMapper() {}

    /** @return sintaks Java, atau kosong bila tidak bisa diterjemahkan */
    static Optional<String> javaScript(String name, boolean negated) {
        int equals = name.indexOf('=');
        if (equals < 0) {
            return Optional.of(lone(name, negated));
        }
        String key = name.substring(0, equals);
        String value = name.substring(equals + 1);
        return switch (key) {
            case "General_Category", "gc" -> Optional.of(category(LONG_CATEGORIES.getOrDefault(value, value), negated));
            case "Script", "sc" -> Optional.of(property("Is" + value, negated));
            default -> Optional.empty();
        };
    }

    static Optional<String> php(String name, boolean negated) {
        if (PCRE_SPECIALS.contains(name)) {
            return Optional.empty();
        }
        if (name.equals("L&")) {
            return Optional.of(category("LC", negated));
        }
        if (name.equals("Any")) {
            return Optional.of(any(negated));
        }
        if (SHORT_CATEGORIES.contains(name)) {
            return Optional.of(property(name, negated));
        }
        return Optional.of(property("Is" + name, negated));
    }

    /** Tanpa {@code =}: general category, atau binary property ({@code Alphabetic}, {@code Emoji}, ...). */
    private static String lone(String name, boolean negated) {
        if (SHORT_CATEGORIES.contains(name) || name.equals("LC") || LONG_CATEGORIES.containsKey(name)) {
            return category(LONG_CATEGORIES.getOrDefault(name, name), negated);
        }
        return switch (name) {
            case "ASCII" -> property("ASCII", negated);
            case "Any" -> any(negated);
            case "Assigned" -> property("Cn", !negated);
            case "Cased_Letter" -> category("LC", negated);
            case "ASCII_Hex_Digit" -> negated ? "[^0-9A-Fa-f]" : "[0-9A-Fa-f]";
            default -> property("Is" + name, negated);
        };
    }

    private static String category(String shortName, boolean negated) {
        if (shortName.equals("LC") || shortName.equals("Cased_Letter")) {
            return (negated ? "[^" : "[") + CASED_LETTER + "]";
        }
        return property(shortName, negated);
    }

    private static String any(boolean negated) {
        return (negated ? "[^" : "[") + ANY_CHARACTER + "]";
    }

    private static String property(String javaName, boolean negated) {
        return (negated ? "\\P{" : "\\p{") + javaName + "}";
    }
}
