package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JweNotSupportedException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtAlgorithm;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtPart;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtToolException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyMismatchException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.KeyType;
import io.github.lukmnh.toolbox.devutils.jwt.domain.MalformedJwtException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnreadableKeyException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsecuredJwtException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.UnsupportedAlgorithmException;
import java.util.Locale;

/** Memetakan error domain JWT ke pesan user bahasa Indonesia. */
final class JwtErrorMessages {

    private JwtErrorMessages() {}

    static String describe(JwtToolException error) {
        return switch (error) {
            case MalformedJwtException malformed ->
                Messages.format("devutils.jwt.error.malformed", partName(malformed.part()), reason(malformed));
            case JweNotSupportedException jwe -> Messages.get("devutils.jwt.error.jwe");
            case UnsecuredJwtException none -> Messages.get("devutils.jwt.error.none");
            case UnsupportedAlgorithmException unsupported ->
                unsupported.algorithmName().isEmpty()
                        ? Messages.get("devutils.jwt.error.noAlgorithm")
                        : Messages.format("devutils.jwt.error.unsupportedAlgorithm", unsupported.algorithmName());
            case KeyMismatchException mismatch ->
                Messages.format(
                        "devutils.jwt.error.keyMismatch",
                        mismatch.algorithm().headerName(),
                        expectedKey(mismatch.algorithm()),
                        givenKey(mismatch));
            case UnreadableKeyException unreadable ->
                Messages.get("devutils.jwt.error.key." + key(unreadable.reason()));
        };
    }

    static String partName(JwtPart part) {
        return Messages.get("devutils.jwt.part." + part.name().toLowerCase(Locale.ROOT));
    }

    private static String reason(MalformedJwtException error) {
        String base = "devutils.jwt.reason." + key(error.reason());
        if (error.detail().isEmpty()) {
            return Messages.get(base);
        }
        return Messages.format(base + ".detail", error.detail());
    }

    static String expectedKey(JwtAlgorithm algorithm) {
        return switch (algorithm.family()) {
            case HMAC -> Messages.get("devutils.jwt.keyType.hmac_secret");
            case RSA, RSA_PSS -> Messages.get("devutils.jwt.keyType.rsa");
            case EC -> Messages.format("devutils.jwt.keyType.ec.curve", algorithm.ecCurveBits());
            case EDDSA -> Messages.get("devutils.jwt.keyType.eddsa");
            case NONE -> Messages.get("devutils.jwt.keyType.none");
        };
    }

    private static String givenKey(KeyMismatchException error) {
        if (error.keyType() == KeyType.EC && error.curveBits() > 0) {
            return Messages.format("devutils.jwt.keyType.ec.curve", error.curveBits());
        }
        return Messages.get("devutils.jwt.keyType." + key(error.keyType()));
    }

    /** {@code INVALID_BASE64URL} → {@code invalid_base64url}, sesuai penamaan key di messages.properties. */
    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
