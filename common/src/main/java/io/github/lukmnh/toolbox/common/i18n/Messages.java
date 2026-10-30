package io.github.lukmnh.toolbox.common.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Akses teks UI dari {@code i18n/messages.properties}. Tidak ada teks UI yang di-hard-code di kode.
 *
 * <p>Untuk {@link #format}, tanda kutip tunggal di properties harus ditulis ganda ({@code ''}) sesuai aturan
 * {@link MessageFormat}.
 */
public final class Messages {

    public static final Locale LOCALE = Locale.US;

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(
            "i18n.messages",
            LOCALE,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private Messages() {}

    public static String get(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            // Key yang hilang tampil apa adanya supaya mudah dikenali saat development, bukan crash
            return "!" + key + "!";
        }
    }

    public static String format(String key, Object... arguments) {
        return new MessageFormat(get(key), LOCALE).format(arguments);
    }
}
