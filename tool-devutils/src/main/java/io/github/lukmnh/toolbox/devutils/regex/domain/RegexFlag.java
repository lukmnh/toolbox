package io.github.lukmnh.toolbox.devutils.regex.domain;

/**
 * Flag per flavor. {@link #token()} adalah huruf flag (JS {@code /re/gi}, Go/PHP {@code (?i)} / modifier) atau nama
 * konstanta {@code java.util.regex.Pattern} untuk Java.
 */
public enum RegexFlag {
    JAVA_CASE_INSENSITIVE(RegexFlavor.JAVA, "CASE_INSENSITIVE"),
    JAVA_MULTILINE(RegexFlavor.JAVA, "MULTILINE"),
    JAVA_DOTALL(RegexFlavor.JAVA, "DOTALL"),
    JAVA_COMMENTS(RegexFlavor.JAVA, "COMMENTS"),
    JAVA_UNICODE_CASE(RegexFlavor.JAVA, "UNICODE_CASE"),
    JAVA_UNICODE_CHARACTER_CLASS(RegexFlavor.JAVA, "UNICODE_CHARACTER_CLASS"),

    JS_GLOBAL(RegexFlavor.JAVASCRIPT, "g"),
    JS_IGNORE_CASE(RegexFlavor.JAVASCRIPT, "i"),
    JS_MULTILINE(RegexFlavor.JAVASCRIPT, "m"),
    JS_DOT_ALL(RegexFlavor.JAVASCRIPT, "s"),
    JS_UNICODE(RegexFlavor.JAVASCRIPT, "u"),
    JS_UNICODE_SETS(RegexFlavor.JAVASCRIPT, "v"),
    JS_STICKY(RegexFlavor.JAVASCRIPT, "y"),

    GO_IGNORE_CASE(RegexFlavor.GO, "i"),
    GO_MULTILINE(RegexFlavor.GO, "m"),
    GO_DOT_NEWLINE(RegexFlavor.GO, "s"),
    GO_UNGREEDY(RegexFlavor.GO, "U"),

    PHP_IGNORE_CASE(RegexFlavor.PHP, "i"),
    PHP_MULTILINE(RegexFlavor.PHP, "m"),
    PHP_DOTALL(RegexFlavor.PHP, "s"),
    PHP_EXTENDED(RegexFlavor.PHP, "x"),
    PHP_UTF8(RegexFlavor.PHP, "u"),
    PHP_UNGREEDY(RegexFlavor.PHP, "U"),
    PHP_DOLLAR_END_ONLY(RegexFlavor.PHP, "D"),
    PHP_ANCHORED(RegexFlavor.PHP, "A");

    private final RegexFlavor flavor;
    private final String token;

    RegexFlag(RegexFlavor flavor, String token) {
        this.flavor = flavor;
        this.token = token;
    }

    public RegexFlavor flavor() {
        return flavor;
    }

    public String token() {
        return token;
    }
}
