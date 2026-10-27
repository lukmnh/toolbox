package io.github.lukmnh.toolbox.pdf.sign.domain;

/** Nilai default tool Isi & TTD. Satuan point kecuali disebut lain. */
public final class EditDefaults {

    public static final double DEFAULT_FONT_SIZE = 12;
    public static final double MIN_FONT_SIZE = 4;
    public static final double MAX_FONT_SIZE = 144;
    public static final double FONT_SIZE_STEP = 0.5;

    /** Leading = 1.2 × fontSize, sama di preview dan PDF. */
    public static final double LINE_HEIGHT_FACTOR = 1.2;

    public static final double DEFAULT_SIGNATURE_WIDTH = 150;
    public static final double MIN_SIGNATURE_WIDTH = 20;

    private EditDefaults() {}
}
