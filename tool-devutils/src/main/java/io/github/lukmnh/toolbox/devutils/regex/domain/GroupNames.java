package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Aturan nama grup per flavor. */
final class GroupNames {

    private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private GroupNames() {}

    static boolean isValid(RegexFlavor flavor, String name) {
        if (name.isEmpty()) {
            return false;
        }
        return switch (flavor) {
            case JAVA -> javaName(name);
            case GO -> name.chars().allMatch(c -> c == '_' || isAsciiAlnum(c));
            case PHP -> phpName(name);
            case JAVASCRIPT -> jsName(name);
        };
    }

    /** {@code java.util.regex}: huruf ASCII lalu huruf/digit ASCII. */
    private static boolean javaName(String name) {
        return ParseCursor.isAsciiLetter(name.charAt(0)) && name.chars().allMatch(GroupNames::isAsciiAlnum);
    }

    /** PCRE2: diawali huruf atau underscore, lalu karakter kata. */
    private static boolean phpName(String name) {
        char first = name.charAt(0);
        boolean startOk = first == '_' || ParseCursor.isAsciiLetter(first);
        return startOk && name.chars().allMatch(c -> c == '_' || isAsciiAlnum(c));
    }

    /** ECMAScript IdentifierName: boleh {@code $}, {@code _}, dan huruf Unicode. */
    private static boolean jsName(String name) {
        int first = name.codePointAt(0);
        if (!(first == '$' || first == '_' || Character.isUnicodeIdentifierStart(first))) {
            return false;
        }
        return name.codePoints()
                .skip(1)
                .allMatch(c -> c == '$'
                        || c == ZERO_WIDTH_NON_JOINER
                        || c == ZERO_WIDTH_JOINER
                        || (Character.isUnicodeIdentifierPart(c) && !Character.isIdentifierIgnorable(c)));
    }

    private static boolean isAsciiAlnum(int c) {
        return ParseCursor.isAsciiLetter(c) || ParseCursor.isDigit(c);
    }
}
