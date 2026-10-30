package io.github.lukmnh.toolbox.productivity.rename.domain;

import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;
import java.util.Objects;

/**
 * Satu aturan Batch Rename. Aturan diterapkan berurutan oleh {@link RenameEngine}; kecuali disebut lain, aturan
 * hanya mengubah nama tanpa ekstensi supaya tipe file tidak berubah tanpa sengaja.
 */
public sealed interface RenameRule {

    /** Cari & ganti teks biasa atau regex. {@code find} kosong berarti aturan tidak mengubah apa pun. */
    record Replace(String find, String replacement, boolean regex, boolean caseSensitive, boolean includeExtension)
            implements RenameRule {

        public Replace {
            Objects.requireNonNull(find, "find");
            Objects.requireNonNull(replacement, "replacement");
        }
    }

    record Affix(String prefix, String suffix) implements RenameRule {

        public Affix {
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(suffix, "suffix");
        }
    }

    /** Nomor urut sesuai posisi file di batch: {@code start + index * step}, diberi nol di depan sampai padding. */
    record Numbering(int start, int step, int padding, Placement placement, String separator) implements RenameRule {

        public static final int MAX_PADDING = 10;

        public Numbering {
            if (start < 0 || step < 1 || padding < 0 || padding > MAX_PADDING) {
                throw new IllegalArgumentException("Parameter penomoran tidak valid");
            }
            Objects.requireNonNull(placement, "placement");
            Objects.requireNonNull(separator, "separator");
        }

        public String format(int index) {
            String digits = Long.toString(start + (long) index * step);
            return "0".repeat(Math.max(0, padding - digits.length())) + digits;
        }
    }

    /** @param includeExtension ekstensi ikut diubah, tapi dikonversi terpisah supaya titiknya tidak hilang */
    record ChangeCase(CaseStyle style, boolean includeExtension) implements RenameRule {

        public ChangeCase {
            Objects.requireNonNull(style, "style");
        }
    }

    /** @param pattern pola {@link java.time.format.DateTimeFormatter}, misalnya {@code yyyy-MM-dd} */
    record InsertDate(String pattern, Placement placement, String separator) implements RenameRule {

        public InsertDate {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(placement, "placement");
            Objects.requireNonNull(separator, "separator");
        }
    }

    /**
     * @param count jumlah karakter untuk {@link RemoveMode#FIRST}/{@link RemoveMode#LAST}
     * @param from posisi awal (mulai 1, inklusif) untuk {@link RemoveMode#RANGE}
     * @param to posisi akhir (inklusif) untuk {@link RemoveMode#RANGE}
     */
    record Remove(RemoveMode mode, int count, int from, int to) implements RenameRule {

        public Remove {
            Objects.requireNonNull(mode, "mode");
            if (count < 0 || from < 1 || to < 1) {
                throw new IllegalArgumentException("Parameter hapus karakter tidak valid");
            }
        }

        /** Dihitung per code point supaya emoji/aksara non-Latin tidak terpotong di tengah. */
        public String removeFrom(String text) {
            int length = text.codePointCount(0, text.length());
            return switch (mode) {
                case FIRST -> slice(text, Math.min(count, length), length);
                case LAST -> slice(text, 0, Math.max(0, length - count));
                case RANGE -> removeRange(text, length);
            };
        }

        private String removeRange(String text, int length) {
            int start = Math.min(from, to) - 1;
            if (start >= length) {
                return text;
            }
            int end = Math.min(Math.max(from, to), length);
            return slice(text, 0, start) + slice(text, end, length);
        }

        private static String slice(String text, int startCodePoint, int endCodePoint) {
            return text.substring(text.offsetByCodePoints(0, startCodePoint), text.offsetByCodePoints(0, endCodePoint));
        }
    }

    /** @param extension ekstensi baru tanpa titik; kosong berarti ekstensi dihapus */
    record ChangeExtension(String extension) implements RenameRule {

        public ChangeExtension {
            Objects.requireNonNull(extension, "extension");
            extension = stripLeadingDots(extension.strip());
        }

        private static String stripLeadingDots(String value) {
            int index = 0;
            while (index < value.length() && value.charAt(index) == '.') {
                index++;
            }
            return value.substring(index);
        }
    }
}
