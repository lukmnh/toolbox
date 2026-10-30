package io.github.lukmnh.toolbox.devutils.regex.presentation;

import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor.GO;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor.JAVA;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor.JAVASCRIPT;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor.PHP;

import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Isi cheat sheet per flavor. Token adalah sintaks regex (bukan teks UI); deskripsinya dari
 * {@code devutils.regex.cheat.<id>}.
 */
final class CheatSheet {

    enum Category {
        CHARACTERS,
        ANCHORS,
        CLASSES,
        QUANTIFIERS,
        GROUPS,
        REFERENCES,
        REPLACEMENT
    }

    /** @param id akhiran key deskripsi */
    record Entry(Category category, String token, String id, Set<RegexFlavor> flavors) {
        Entry {
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(id, "id");
            flavors = Set.copyOf(flavors);
        }
    }

    private static final Set<RegexFlavor> ALL = EnumSet.allOf(RegexFlavor.class);
    private static final Set<RegexFlavor> NOT_GO = EnumSet.of(JAVA, JAVASCRIPT, PHP);
    private static final Set<RegexFlavor> JAVA_PHP = EnumSet.of(JAVA, PHP);
    private static final Set<RegexFlavor> JAVA_GO_PHP = EnumSet.of(JAVA, GO, PHP);
    private static final Set<RegexFlavor> GO_PHP = EnumSet.of(GO, PHP);

    private static final List<Entry> ENTRIES = List.of(
            new Entry(Category.CHARACTERS, ".", "dot", ALL),
            new Entry(Category.CHARACTERS, "\\d  \\D", "digit", ALL),
            new Entry(Category.CHARACTERS, "\\w  \\W", "word", ALL),
            new Entry(Category.CHARACTERS, "\\s  \\S", "space", ALL),
            new Entry(Category.CHARACTERS, "\\t  \\n  \\r", "controls", ALL),
            new Entry(Category.CHARACTERS, "\\x41  \\u0041", "hexEscape", NOT_GO),
            new Entry(Category.CHARACTERS, "\\x{1F600}", "hexBraces", JAVA_GO_PHP),
            new Entry(Category.CHARACTERS, "\\u{1F600}", "jsCodePoint", EnumSet.of(JAVASCRIPT)),
            new Entry(Category.CHARACTERS, "\\p{L}  \\P{L}", "property", ALL),
            new Entry(Category.CHARACTERS, "\\h  \\v  \\R", "hvr", JAVA_PHP),
            new Entry(Category.CHARACTERS, "\\X", "grapheme", JAVA_PHP),
            new Entry(Category.CHARACTERS, "\\Q...\\E", "quote", JAVA_GO_PHP),
            new Entry(Category.ANCHORS, "^  $", "lineAnchors", ALL),
            new Entry(Category.ANCHORS, "\\b  \\B", "wordBoundary", ALL),
            new Entry(Category.ANCHORS, "\\A  \\z", "inputAnchors", JAVA_GO_PHP),
            new Entry(Category.ANCHORS, "\\Z", "endBeforeNewline", JAVA_PHP),
            new Entry(Category.ANCHORS, "\\G", "previousMatch", JAVA_PHP),
            new Entry(Category.CLASSES, "[abc]  [^abc]", "set", ALL),
            new Entry(Category.CLASSES, "[a-z]", "range", ALL),
            new Entry(Category.CLASSES, "[[:alpha:]]", "posix", GO_PHP),
            new Entry(Category.CLASSES, "[a-z&&[^aeiou]]", "intersection", EnumSet.of(JAVA)),
            new Entry(Category.CLASSES, "[\\w--\\d]  [\\w&&\\p{L}]", "setOperations", EnumSet.of(JAVASCRIPT)),
            new Entry(Category.CLASSES, "[^]", "anyIncludingNewline", EnumSet.of(JAVASCRIPT)),
            new Entry(Category.QUANTIFIERS, "*  +  ?", "basicQuantifiers", ALL),
            new Entry(Category.QUANTIFIERS, "{n}  {n,}  {n,m}", "countQuantifiers", ALL),
            new Entry(Category.QUANTIFIERS, "*?  +?  ??", "lazy", ALL),
            new Entry(Category.QUANTIFIERS, "*+  ++  ?+", "possessive", JAVA_PHP),
            new Entry(Category.GROUPS, "(...)", "capture", ALL),
            new Entry(Category.GROUPS, "(?:...)", "nonCapture", ALL),
            new Entry(Category.GROUPS, "(?<name>...)", "named", ALL),
            new Entry(Category.GROUPS, "(?P<name>...)", "namedPython", GO_PHP),
            new Entry(Category.GROUPS, "(?'name'...)", "namedQuote", EnumSet.of(PHP)),
            new Entry(Category.GROUPS, "(?>...)", "atomic", JAVA_PHP),
            new Entry(Category.GROUPS, "(?=...)  (?!...)", "lookahead", NOT_GO),
            new Entry(Category.GROUPS, "(?<=...)  (?<!...)", "lookbehind", NOT_GO),
            new Entry(Category.GROUPS, "(?i)  (?-i)", "inlineFlags", JAVA_GO_PHP),
            new Entry(Category.GROUPS, "(?i:...)", "flagGroup", ALL),
            new Entry(Category.REFERENCES, "\\1", "backreference", NOT_GO),
            new Entry(Category.REFERENCES, "\\k<name>", "namedBackreference", NOT_GO),
            new Entry(Category.REFERENCES, "(?P=name)  \\g{-1}", "pcreBackreference", EnumSet.of(PHP)),
            new Entry(Category.REPLACEMENT, "$1  ${name}  \\$", "replaceJava", EnumSet.of(JAVA)),
            new Entry(Category.REPLACEMENT, "$1  $<name>  $&  $`  $'  $$", "replaceJs", EnumSet.of(JAVASCRIPT)),
            new Entry(Category.REPLACEMENT, "$1  ${1}  $name  ${name}  $$", "replaceGo", EnumSet.of(GO)),
            new Entry(Category.REPLACEMENT, "$1  \\1  ${1}  $0", "replacePhp", EnumSet.of(PHP)));

    private CheatSheet() {}

    static List<Entry> entries(RegexFlavor flavor, Category category) {
        return ENTRIES.stream()
                .filter(entry -> entry.category() == category && entry.flavors().contains(flavor))
                .toList();
    }
}
