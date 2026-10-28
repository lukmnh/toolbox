package io.github.lukmnh.toolbox.common.security;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanitasi nama file output (Security Requirements #6). Hasilnya selalu satu nama file (bukan path) yang valid
 * di Windows dan Linux.
 */
public final class FileNameSanitizer {

    public static final int MAX_LENGTH = 200;
    static final String FALLBACK_NAME = "hasil";

    /** Karakter terlarang di Windows plus karakter kontrol; {@code /} dan {@code \} mencegah path traversal. */
    private static final Pattern FORBIDDEN = Pattern.compile("[/\\\\:*?\"<>|\\p{Cntrl}]");

    /** Titik dan spasi di awal/akhir: dibuang Windows, dan titik di awal membuat file tersembunyi di Linux. */
    private static final Pattern EDGE_DOTS_AND_SPACES = Pattern.compile("^[. ]+|[. ]+$");

    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1",
            "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    /** Ekstensi lebih panjang dari ini dianggap bagian nama saat dipotong. */
    private static final int MAX_EXTENSION_LENGTH = 16;

    private FileNameSanitizer() {}

    public static String sanitize(String name) {
        String cleaned = FORBIDDEN.matcher(name).replaceAll("");
        cleaned = cleaned.replace("..", "");
        cleaned = EDGE_DOTS_AND_SPACES.matcher(cleaned).replaceAll("");
        if (cleaned.isEmpty()) {
            return FALLBACK_NAME;
        }
        if (isWindowsReserved(cleaned)) {
            cleaned = "_" + cleaned;
        }
        return truncate(cleaned);
    }

    /** Windows menolak nama reserved walau diberi ekstensi, misalnya {@code CON.pdf} atau {@code nul.tar.gz}. */
    private static boolean isWindowsReserved(String name) {
        int dot = name.indexOf('.');
        String stem = (dot < 0 ? name : name.substring(0, dot)).strip();
        return WINDOWS_RESERVED.contains(stem.toUpperCase(Locale.ROOT));
    }

    /** Potong ke {@link #MAX_LENGTH} karakter dengan mempertahankan ekstensi. */
    private static String truncate(String name) {
        if (name.length() <= MAX_LENGTH) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        boolean keepExtension = dot > 0 && name.length() - dot <= MAX_EXTENSION_LENGTH;
        String extension = keepExtension ? name.substring(dot) : "";
        String stem = keepExtension ? name.substring(0, dot) : name;
        int cut = MAX_LENGTH - extension.length();
        // Jangan memotong di tengah pasangan surrogate (emoji)
        if (Character.isHighSurrogate(stem.charAt(cut - 1))) {
            cut--;
        }
        return stem.substring(0, cut) + extension;
    }
}
