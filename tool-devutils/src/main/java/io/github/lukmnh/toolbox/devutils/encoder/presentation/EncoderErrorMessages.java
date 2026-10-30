package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncoderIoException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidBase64Exception;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidPercentEncodingException;
import java.util.Locale;

/** Memetakan exception domain Encoder ke pesan bahasa Indonesia. Posisi ditampilkan 1-based. */
final class EncoderErrorMessages {

    private EncoderErrorMessages() {}

    static String describe(EncoderException error) {
        return switch (error) {
            case InvalidBase64Exception base64 -> base64Message(base64);
            case InvalidPercentEncodingException percent ->
                Messages.format("devutils.encoder.error.percent", percent.position() + 1, percent.sequence());
            case EncoderIoException io ->
                Messages.format(
                        "devutils.encoder.error.io", io.file().getFileName(), ErrorDialogs.describe(io.getCause()));
        };
    }

    private static String base64Message(InvalidBase64Exception error) {
        long position = error.position() + 1;
        return switch (error.reason()) {
            case INVALID_CHARACTER ->
                Messages.format("devutils.encoder.error.base64.character", position, printable(error.character()));
            case MISPLACED_PADDING -> Messages.format("devutils.encoder.error.base64.padding", position);
            case TRUNCATED -> Messages.format("devutils.encoder.error.base64.truncated", position);
        };
    }

    /** Karakter kontrol/tak terlihat ditampilkan sebagai kode Unicode supaya user tahu apa yang salah. */
    private static String printable(int codePoint) {
        if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint) || !Character.isDefined(codePoint)) {
            return String.format(Locale.ROOT, "U+%04X", codePoint);
        }
        return "\"" + Character.toString(codePoint) + "\"";
    }
}
