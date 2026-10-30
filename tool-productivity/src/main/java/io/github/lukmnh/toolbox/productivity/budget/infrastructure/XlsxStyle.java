package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Worksheet;

/**
 * Gaya satu sel. Field kosong ({@link #NONE}) berarti tidak diatur. Warna dalam hex RGB tanpa {@code #}.
 *
 * @param border {@code true} untuk border tipis abu-abu di keempat sisi
 */
record XlsxStyle(String fill, String fontColor, boolean bold, String format, String align, boolean border) {

    static final String NONE = "";

    static final String HEADER_FILL = "2F6B57";
    static final String HEADER_FONT = "FFFFFF";
    static final String ZEBRA_FILL = "F2F2F2";
    static final String INACTIVE_FILL = "FFC7CE";
    static final String INACTIVE_FONT = "9C0006";
    static final String TOTAL_FILL = "D9EAD3";
    static final String BORDER_COLOR = "BFBFBF";
    static final String RUPIAH = "\"Rp\"#,##0";
    static final String RUPIAH_CENTS = "\"Rp\"#,##0.00";
    static final String PERCENT = "0.00%";
    static final String CENTER = "center";
    static final int TITLE_FONT_SIZE = 14;

    static final XlsxStyle PLAIN = new XlsxStyle(NONE, NONE, false, NONE, NONE, false);
    static final XlsxStyle BORDERED = PLAIN.withBorder();
    static final XlsxStyle BOLD = PLAIN.withBold();
    static final XlsxStyle HEADER = new XlsxStyle(HEADER_FILL, HEADER_FONT, true, NONE, CENTER, true);
    static final XlsxStyle TOTAL = new XlsxStyle(TOTAL_FILL, NONE, true, NONE, NONE, true);

    XlsxStyle withFill(String color) {
        return new XlsxStyle(color, fontColor, bold, format, align, border);
    }

    XlsxStyle withFontColor(String color) {
        return new XlsxStyle(fill, color, bold, format, align, border);
    }

    XlsxStyle withBold() {
        return new XlsxStyle(fill, fontColor, true, format, align, border);
    }

    XlsxStyle withFormat(String numberFormat) {
        return new XlsxStyle(fill, fontColor, bold, numberFormat, align, border);
    }

    XlsxStyle withAlign(String alignment) {
        return new XlsxStyle(fill, fontColor, bold, format, alignment, border);
    }

    XlsxStyle withBorder() {
        return new XlsxStyle(fill, fontColor, bold, format, align, true);
    }

    void apply(Worksheet sheet, int row, int column) {
        StyleSetter setter = sheet.style(row, column);
        if (!fill.isEmpty()) {
            setter = setter.fillColor(fill);
        }
        if (!fontColor.isEmpty()) {
            setter = setter.fontColor(fontColor);
        }
        if (bold) {
            setter = setter.bold();
        }
        if (!format.isEmpty()) {
            setter = setter.format(format);
        }
        if (!align.isEmpty()) {
            setter = setter.horizontalAlignment(align);
        }
        if (border) {
            setter = setter.borderStyle(BorderStyle.THIN).borderColor(BORDER_COLOR);
        }
        setter.set();
    }

    /** Warna domain {@code #RRGGBB} → {@code RRGGBB}. */
    static String excelColor(String hexColor) {
        return hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
    }
}
