package io.github.lukmnh.toolbox.devutils.regex.domain;

/**
 * Hitung grup capture di seluruh pola sebelum parse. JavaScript menentukan arti {@code \\1} dan {@code \\k} dari
 * jumlah grup di <em>seluruh</em> pola (termasuk yang muncul setelahnya), bukan hanya yang sudah lewat.
 */
record GroupPrescan(int captureCount, boolean hasNamedGroups) {

    static GroupPrescan scan(String pattern, boolean nestedClasses) {
        int count = 0;
        boolean named = false;
        int classDepth = 0;
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current == '\\') {
                index++;
                continue;
            }
            if (current == '[' && (classDepth == 0 || nestedClasses)) {
                classDepth++;
                continue;
            }
            if (current == ']' && classDepth > 0) {
                classDepth--;
                continue;
            }
            if (classDepth > 0 || current != '(') {
                continue;
            }
            if (!pattern.startsWith("?", index + 1)) {
                count++;
            } else if (isNamedGroupStart(pattern, index)) {
                count++;
                named = true;
            }
        }
        return new GroupPrescan(count, named);
    }

    private static boolean isNamedGroupStart(String pattern, int openParen) {
        boolean angle = pattern.startsWith("?<", openParen + 1)
                && !pattern.startsWith("?<=", openParen + 1)
                && !pattern.startsWith("?<!", openParen + 1);
        return angle || pattern.startsWith("?P<", openParen + 1) || pattern.startsWith("?'", openParen + 1);
    }
}
