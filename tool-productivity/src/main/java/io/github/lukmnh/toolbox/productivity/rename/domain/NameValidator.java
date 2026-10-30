package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nama file yang aman di Windows dan Linux. Berbeda dengan {@code FileNameSanitizer} yang diam-diam membersihkan nama
 * output, di sini nama hasil aturan user tidak diubah: masalahnya dilaporkan supaya user memperbaiki aturan.
 */
public final class NameValidator {

    /** Sama dengan batas {@code FileNameSanitizer.MAX_LENGTH}. */
    public static final int MAX_CHARACTERS = 200;

    /** Batas nama file ext4/NTFS/APFS dalam byte. */
    public static final int MAX_UTF8_BYTES = 255;

    /** Karakter terlarang di Windows plus karakter kontrol; {@code /} dan {@code \} juga mencegah path traversal. */
    private static final Pattern FORBIDDEN = Pattern.compile("[/\\\\:*?\"<>|\\p{Cntrl}]");

    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1",
            "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private NameValidator() {}

    public static NameIssue check(String name) {
        if (name.isBlank()) {
            return NameIssue.EMPTY;
        }
        if (FORBIDDEN.matcher(name).find()) {
            return NameIssue.FORBIDDEN_CHARACTER;
        }
        // Windows membuang titik/spasi di akhir, sehingga nama berubah diam-diam (dan "." / ".." ikut tertolak)
        if (name.endsWith(".") || name.endsWith(" ")) {
            return NameIssue.TRAILING_DOT_OR_SPACE;
        }
        if (isWindowsReserved(name)) {
            return NameIssue.RESERVED_NAME;
        }
        boolean tooLong = name.codePointCount(0, name.length()) > MAX_CHARACTERS
                || name.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES;
        return tooLong ? NameIssue.TOO_LONG : NameIssue.NONE;
    }

    /** Windows menolak nama reserved walau diberi ekstensi, misalnya {@code CON.txt}. */
    private static boolean isWindowsReserved(String name) {
        int dot = name.indexOf('.');
        String stem = (dot < 0 ? name : name.substring(0, dot)).strip();
        return WINDOWS_RESERVED.contains(stem.toUpperCase(Locale.ROOT));
    }
}
