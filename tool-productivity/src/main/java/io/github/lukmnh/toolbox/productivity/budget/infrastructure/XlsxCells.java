package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import org.dhatim.fastexcel.Worksheet;

/**
 * Referensi sel A1 dan penulisan formula.
 *
 * <p>FastExcel 0.20.2 menulis teks formula ke XML apa adanya (tanpa escape), sehingga nama sheet berisi {@code &}
 * akan merusak file. Karena itu semua formula di-escape di sini. Formula hanya dibangun dari referensi sel dan
 * konstanta kode, tidak pernah dari teks user (Security Requirements #18).
 */
final class XlsxCells {

    private static final int ALPHABET = 26;

    private XlsxCells() {}

    /** @param row index 0-based, @param column index 0-based → misalnya {@code G5} */
    static String ref(int row, int column) {
        return columnName(column) + (row + 1);
    }

    static String range(int firstRow, int lastRow, int column) {
        return ref(firstRow, column) + ":" + ref(lastRow, column);
    }

    /** {@code 'Make Up''s'!H30}: nama sheet selalu dikutip, kutip tunggal di dalamnya digandakan. */
    static String sheetRef(String sheetName, String cellRef) {
        return "'" + sheetName.replace("'", "''") + "'!" + cellRef;
    }

    /** Literal teks formula (hanya untuk label dari kode, misalnya kriteria SUMIF). */
    static String text(String literal) {
        return "\"" + literal.replace("\"", "\"\"") + "\"";
    }

    static void formula(Worksheet sheet, int row, int column, String expression) {
        sheet.formula(row, column, xmlEscape(expression));
    }

    /** Ekspresi conditional formatting juga ditulis mentah oleh FastExcel. */
    static String xmlEscape(String expression) {
        return expression.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String columnName(int column) {
        StringBuilder name = new StringBuilder();
        for (int index = column + 1; index > 0; index = (index - 1) / ALPHABET) {
            name.insert(0, (char) ('A' + (index - 1) % ALPHABET));
        }
        return name.toString();
    }
}
