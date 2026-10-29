package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Set;

/**
 * Aturan sintaks per flavor yang dibutuhkan parser. Satu tempat supaya perbedaan Java/JS/Go/PHP mudah ditelusuri.
 */
final class ParserDialect {

    private static final String JAVA_INLINE_FLAGS = "idmsuxU";
    private static final String GO_INLINE_FLAGS = "imsU";
    private static final String PHP_INLINE_FLAGS = "imsxnUJ^";
    private static final String JS_MODIFIER_FLAGS = "ims";
    private static final String JS_UNICODE_IDENTITY = "^$\\.*+?()[]{}|/";

    private final RegexFlavor flavor;
    private final boolean unicodeMode;
    private final boolean unicodeSets;
    private final boolean extended;

    private ParserDialect(RegexFlavor flavor, boolean unicodeMode, boolean unicodeSets, boolean extended) {
        this.flavor = flavor;
        this.unicodeMode = unicodeMode;
        this.unicodeSets = unicodeSets;
        this.extended = extended;
    }

    static ParserDialect of(RegexFlavor flavor, Set<RegexFlag> flags) {
        boolean sets = flags.contains(RegexFlag.JS_UNICODE_SETS);
        boolean unicode = sets || flags.contains(RegexFlag.JS_UNICODE);
        boolean extended = flags.contains(RegexFlag.JAVA_COMMENTS) || flags.contains(RegexFlag.PHP_EXTENDED);
        return new ParserDialect(flavor, unicode, sets, extended);
    }

    RegexFlavor flavor() {
        return flavor;
    }

    boolean is(RegexFlavor candidate) {
        return flavor == candidate;
    }

    /** JavaScript dengan flag {@code u}/{@code v}: escape ketat, {@code \\u{..}} dan {@code \\p{..}} aktif. */
    boolean unicodeMode() {
        return unicodeMode;
    }

    /** JavaScript flag {@code v}: class bersarang, {@code &&}, {@code --}. */
    boolean unicodeSets() {
        return unicodeSets;
    }

    /** JavaScript tanpa flag u/v: aturan Annex B yang longgar (identity escape, {@code {} literal). */
    boolean annexB() {
        return flavor == RegexFlavor.JAVASCRIPT && !unicodeMode;
    }

    boolean extendedInitially() {
        return extended;
    }

    String inlineFlagLetters() {
        return switch (flavor) {
            case JAVA -> JAVA_INLINE_FLAGS;
            case GO -> GO_INLINE_FLAGS;
            case PHP -> PHP_INLINE_FLAGS;
            case JAVASCRIPT -> JS_MODIFIER_FLAGS;
        };
    }

    /** {@code ]} tepat setelah {@code [} / {@code [^} adalah literal (semua flavor kecuali JavaScript). */
    boolean closingBracketFirstIsLiteral() {
        return flavor != RegexFlavor.JAVASCRIPT;
    }

    boolean nestedClasses() {
        return flavor == RegexFlavor.JAVA || unicodeSets;
    }

    boolean classIntersection() {
        return flavor == RegexFlavor.JAVA || unicodeSets;
    }

    boolean posixClasses() {
        return flavor == RegexFlavor.GO || flavor == RegexFlavor.PHP;
    }

    boolean possessive() {
        return flavor == RegexFlavor.JAVA || flavor == RegexFlavor.PHP;
    }

    /** {@code {,n}} = {0,n} (PCRE2 10.43+, PHP 8.4); flavor lain menganggapnya literal. */
    boolean braceWithoutMinimum() {
        return flavor == RegexFlavor.PHP;
    }

    /** {@code {} yang bukan quantifier valid dianggap literal (bukan error). */
    boolean literalBrace() {
        return flavor == RegexFlavor.GO || flavor == RegexFlavor.PHP || annexB();
    }

    /** Escape karakter non-alfanumerik yang diizinkan di mode Unicode JavaScript. */
    boolean jsUnicodeIdentityAllowed(int character, boolean inClass) {
        return JS_UNICODE_IDENTITY.indexOf(character) >= 0 || (inClass && character == '-');
    }
}
