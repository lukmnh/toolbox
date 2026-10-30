package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.regex.domain.Explanation;
import io.github.lukmnh.toolbox.devutils.regex.domain.QuantifierMode;
import io.github.lukmnh.toolbox.devutils.regex.domain.Quantity;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Menyusun teks bahasa Indonesia untuk satu baris penjelasan, contoh {@code \d{3,}} → "Digit 0–9, minimal 3 kali
 * (rakus)". Domain hanya mengirim jenis + argumen supaya tidak ada teks UI di luar {@code messages.properties}.
 */
final class ExplanationText {

    private static final String PREFIX = "devutils.regex.explain.";
    private static final String QUANTITY = "devutils.regex.quantity.";
    private static final String TRUE = "true";
    private static final int UNBOUNDED = -1;
    private static final String CODE_POINT_PREFIX = "U+";

    private ExplanationText() {}

    static String of(Explanation explanation) {
        String base = base(explanation);
        Quantity quantity = explanation.quantity();
        if (quantity.isOnce()) {
            return base;
        }
        return Messages.format(QUANTITY + "format", base, quantity(quantity), mode(quantity.mode()));
    }

    private static String base(Explanation explanation) {
        List<String> args = explanation.args();
        String kind = explanation.kind().name();
        return switch (explanation.kind()) {
            case ANY_CHAR -> Messages.get(PREFIX + kind + (TRUE.equals(arg(args, 0)) ? ".dotall" : ""));
            case ANCHOR -> anchor(args);
            case SHORTHAND, SPECIAL, CLASS_OPERATOR ->
                Messages.format(PREFIX + kind + "." + arg(args, 0), args.toArray());
            case UNICODE_PROPERTY, POSIX_CLASS ->
                Messages.format(PREFIX + kind + (TRUE.equals(arg(args, 1)) ? ".negated" : ""), arg(args, 0));
            case GROUP_FLAGS, INLINE_FLAGS -> flags(kind, arg(args, 0), arg(args, 1));
            case LITERAL_SPECIAL -> Messages.format(PREFIX + kind, specialCharacter(arg(args, 0)));
            default -> Messages.format(PREFIX + kind, args.toArray());
        };
    }

    /** {@code ^}/{@code $} berbeda arti saat multiline aktif. */
    private static String anchor(List<String> args) {
        String key = PREFIX + "ANCHOR." + arg(args, 0);
        return Messages.get(TRUE.equals(arg(args, 1)) && isLineAnchor(arg(args, 0)) ? key + ".multiline" : key);
    }

    private static boolean isLineAnchor(String anchor) {
        return "LINE_START".equals(anchor) || "LINE_END".equals(anchor);
    }

    private static String flags(String kind, String on, String off) {
        if (off.isEmpty()) {
            return Messages.format(PREFIX + kind + ".on", on);
        }
        if (on.isEmpty()) {
            return Messages.format(PREFIX + kind + ".off", off);
        }
        return Messages.format(PREFIX + kind, on, off);
    }

    /** Argumen berupa kode Unicode ({@code 0009} atau {@code U+0009}); nama umum diambil dari messages. */
    private static String specialCharacter(String code) {
        Optional<Integer> codePoint = parseCodePoint(code);
        if (codePoint.isEmpty()) {
            return code;
        }
        String hex = String.format(Locale.ROOT, "%04X", codePoint.get());
        String key = "devutils.regex.char." + hex;
        String name = Messages.get(key);
        return name.startsWith("!") ? Messages.format("devutils.regex.char.other", hex) : name;
    }

    private static Optional<Integer> parseCodePoint(String code) {
        String hex = code.toUpperCase(Locale.ROOT).startsWith(CODE_POINT_PREFIX)
                ? code.substring(CODE_POINT_PREFIX.length())
                : code;
        if (hex.isEmpty() || !hex.chars().allMatch(HexFormat::isHexDigit)) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(hex, 16));
    }

    private static String quantity(Quantity quantity) {
        int min = quantity.min();
        int max = quantity.max();
        if (min == 0 && max == 1) {
            return Messages.get(QUANTITY + "optional");
        }
        if (max == UNBOUNDED) {
            return switch (min) {
                case 0 -> Messages.get(QUANTITY + "zeroOrMore");
                case 1 -> Messages.get(QUANTITY + "oneOrMore");
                default -> Messages.format(QUANTITY + "atLeast", String.valueOf(min));
            };
        }
        if (min == max) {
            return Messages.format(QUANTITY + "exactly", String.valueOf(min));
        }
        return Messages.format(QUANTITY + "between", String.valueOf(min), String.valueOf(max));
    }

    private static String mode(QuantifierMode mode) {
        return Messages.get(QUANTITY + "mode." + mode.name());
    }

    private static String arg(List<String> args, int index) {
        return index < args.size() ? args.get(index) : "";
    }
}
