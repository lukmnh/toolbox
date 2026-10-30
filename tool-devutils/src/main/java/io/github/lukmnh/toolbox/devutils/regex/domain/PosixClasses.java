package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Map;

/** POSIX {@code [:alpha:]} (PCRE) → properti POSIX {@code java.util.regex}. */
final class PosixClasses {

    private static final Map<String, String> JAVA_NAMES = Map.ofEntries(
            Map.entry("alnum", "Alnum"),
            Map.entry("alpha", "Alpha"),
            Map.entry("ascii", "ASCII"),
            Map.entry("blank", "Blank"),
            Map.entry("cntrl", "Cntrl"),
            Map.entry("digit", "Digit"),
            Map.entry("graph", "Graph"),
            Map.entry("lower", "Lower"),
            Map.entry("print", "Print"),
            Map.entry("punct", "Punct"),
            Map.entry("space", "Space"),
            Map.entry("upper", "Upper"),
            Map.entry("xdigit", "XDigit"));

    private PosixClasses() {}

    static boolean isKnown(String name) {
        return JAVA_NAMES.containsKey(name) || name.equals("word");
    }

    static String toJava(String name, boolean negated) {
        if (name.equals("word")) {
            return negated ? "\\W" : "\\w";
        }
        String javaName = JAVA_NAMES.getOrDefault(name, name);
        return (negated ? "\\P{" : "\\p{") + javaName + "}";
    }
}
