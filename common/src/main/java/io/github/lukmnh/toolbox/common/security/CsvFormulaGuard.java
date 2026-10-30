package io.github.lukmnh.toolbox.common.security;

import java.util.regex.Pattern;

/**
 * Netralisir formula injection di sel CSV (Security Requirements #18). Sel yang diawali {@code = + - @}, tab, atau
 * CR diberi prefix {@code '} supaya Excel/LibreOffice membacanya sebagai teks. Angka valid seperti {@code -12.5}
 * atau {@code +62812} dibiarkan apa adanya.
 */
public final class CsvFormulaGuard {

    static final String TEXT_PREFIX = "'";

    private static final String FORMULA_TRIGGERS = "=+-@\t\r";
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private CsvFormulaGuard() {}

    public static String neutralize(String cell) {
        if (cell.isEmpty() || FORMULA_TRIGGERS.indexOf(cell.charAt(0)) < 0) {
            return cell;
        }
        if (NUMBER.matcher(cell).matches()) {
            return cell;
        }
        return TEXT_PREFIX + cell;
    }
}
