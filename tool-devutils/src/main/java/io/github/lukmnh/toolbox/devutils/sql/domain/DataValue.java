package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Satu sel data input SQL Generator.
 *
 * @param kind {@link Kind#RAW} = teks tanpa tipe dari CSV/TXT; tipenya ditentukan per kolom saat generate
 * @param text nilai sebagai teks (angka apa adanya, JSON compact untuk {@link Kind#JSON}); kosong untuk NULL
 */
public record DataValue(Kind kind, String text) {

    public enum Kind {
        NULL,
        RAW,
        NUMBER,
        BOOLEAN,
        TEXT,
        /** Object/array JSON bersarang. */
        JSON
    }

    public static final DataValue NULL = new DataValue(Kind.NULL, "");

    private static final String NULL_WORD = "null";

    public DataValue {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    /** Sel kosong atau bertuliskan {@code NULL} (huruf apa pun) menjadi NULL. */
    public static DataValue raw(String text) {
        return isNullText(text) ? NULL : new DataValue(Kind.RAW, text);
    }

    /** Teks bertipe (string JSON, sel teks Excel): selalu di-quote, tidak ikut inferensi tipe kolom. */
    public static DataValue text(String text) {
        return new DataValue(Kind.TEXT, text);
    }

    public static DataValue number(String text) {
        return new DataValue(Kind.NUMBER, text);
    }

    public static DataValue bool(boolean value) {
        return new DataValue(Kind.BOOLEAN, Boolean.toString(value));
    }

    public static DataValue json(String compactJson) {
        return new DataValue(Kind.JSON, compactJson);
    }

    public boolean isNull() {
        return kind == Kind.NULL;
    }

    static boolean isNullText(String text) {
        return text.isBlank() || text.strip().toLowerCase(Locale.ROOT).equals(NULL_WORD);
    }
}
