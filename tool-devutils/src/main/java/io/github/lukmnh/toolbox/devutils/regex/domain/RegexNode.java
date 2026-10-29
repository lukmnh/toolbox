package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Objects;

/**
 * Pohon sintaks pola hasil {@link RegexParser}. Dipakai bersama oleh explainer (penjelasan per token) dan translator
 * (JavaScript/PHP → sintaks Java). Setiap node menyimpan {@link Span} di pola asli untuk highlight dan posisi error.
 */
public sealed interface RegexNode {

    Span span();

    /** {@code a|b|c}. */
    record Alternation(Span span, List<RegexNode> branches) implements RegexNode {
        public Alternation {
            branches = List.copyOf(branches);
        }
    }

    /** Urutan atom; boleh kosong (misalnya cabang kosong di {@code a|}). */
    record Sequence(Span span, List<RegexNode> items) implements RegexNode {
        public Sequence {
            items = List.copyOf(items);
        }
    }

    /**
     * @param number nomor grup capture (mulai 1), atau 0 untuk grup non-capture
     * @param name nama grup seperti tertulis di pola, atau string kosong
     * @param flagsOn flag yang diaktifkan untuk {@link GroupKind#FLAG_GROUP} (misalnya {@code "i"}), selain itu kosong
     * @param flagsOff flag yang dimatikan untuk {@link GroupKind#FLAG_GROUP}, selain itu kosong
     */
    record Group(Span span, GroupKind kind, int number, String name, String flagsOn, String flagsOff, RegexNode body)
            implements RegexNode {
        public Group {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(flagsOn, "flagsOn");
            Objects.requireNonNull(flagsOff, "flagsOff");
            Objects.requireNonNull(body, "body");
        }
    }

    /** @param max batas atas, atau {@link #UNBOUNDED} */
    record Quantified(Span span, RegexNode target, int min, int max, QuantifierMode mode) implements RegexNode {
        public static final int UNBOUNDED = -1;

        public Quantified {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(mode, "mode");
            if (min < 0 || (max != UNBOUNDED && max < min)) {
                throw new IllegalArgumentException("Quantifier tidak valid");
            }
        }
    }

    /** Satu karakter literal, termasuk hasil escape ({@code \t}, {@code \x41}, {@code \u0041}, {@code \.}). */
    record Literal(Span span, int codePoint) implements RegexNode {}

    /** {@code \Q...\E}: semua karakter di dalamnya literal. */
    record QuotedLiteral(Span span, String text) implements RegexNode {}

    /** {@code .}. */
    record AnyChar(Span span) implements RegexNode {}

    record Anchor(Span span, AnchorKind kind) implements RegexNode {}

    /** {@code \d \w \s \h \v \R \X \N} dan negasinya. */
    record Shorthand(Span span, ShorthandKind kind) implements RegexNode {}

    /** {@code \p{L}}, {@code \pL}, {@code \P{Greek}}, {@code \p{Script=Latin}}. Nama apa adanya dari pola. */
    record UnicodeProperty(Span span, String name, boolean negated) implements RegexNode {}

    /** {@code [:alpha:]} di dalam character class (Go, PHP). */
    record PosixClass(Span span, String name, boolean negated) implements RegexNode {}

    /**
     * {@code [...]}. Item: {@link Literal}, {@link ClassRange}, {@link Shorthand}, {@link UnicodeProperty},
     * {@link PosixClass}, {@link CharClass} (nested), dan {@link ClassOperator}.
     */
    record CharClass(Span span, boolean negated, List<RegexNode> items) implements RegexNode {
        public CharClass {
            items = List.copyOf(items);
        }
    }

    /** {@code a-z} di dalam character class. */
    record ClassRange(Span span, int from, int to) implements RegexNode {}

    /** {@code &&} (irisan) atau {@code --} (selisih, JS flag v) di dalam character class. */
    record ClassOperator(Span span, SetOperator operator) implements RegexNode {}

    /**
     * Backreference. Tepat satu dari {@code number}/{@code name} terisi; referensi relatif PHP ({@code \g{-1}})
     * sudah di-resolve parser ke nomor absolut.
     */
    record Backreference(Span span, int number, String name) implements RegexNode {
        public Backreference {
            Objects.requireNonNull(name, "name");
        }
    }

    /** Flag inline yang berlaku sampai akhir grup: {@code (?i)}, {@code (?-s)}. */
    record InlineFlags(Span span, String flagsOn, String flagsOff) implements RegexNode {}

    /** {@code (?#...)} atau komentar mode extended ({@code # ...}). */
    record Comment(Span span, String text) implements RegexNode {}

    /**
     * Konstruk yang dikenali tapi tidak punya padanan di engine terjemahan (misalnya {@code \K}, rekursi PCRE).
     * {@code raw} = teks asli di pola.
     */
    record SpecialConstruct(Span span, SpecialKind kind, String raw) implements RegexNode {}

    /** Bagian pola yang tidak dikenali parser; ditampilkan sebagai "Konstruk lanjutan", tidak pernah crash. */
    record Unknown(Span span, String raw) implements RegexNode {}
}
