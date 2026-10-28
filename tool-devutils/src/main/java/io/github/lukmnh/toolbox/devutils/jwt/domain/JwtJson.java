package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.util.List;
import java.util.Optional;

/** Port parsing JSON header/payload (diimplementasikan dengan Jackson streaming di infrastructure). */
public interface JwtJson {

    /**
     * @param part bagian token asal JSON, untuk pesan error
     * @return JSON terformat dengan indentasi
     * @throws MalformedJwtException bila bukan JSON object yang valid
     */
    String prettyPrint(String json, JwtPart part);

    /**
     * @return field top-level payload sesuai urutan
     * @throws MalformedJwtException bila bukan JSON object yang valid
     */
    List<Claim> claims(String payloadJson);

    /** Nilai string {@code alg} di header; kosong bila tidak ada atau bukan string. */
    Optional<String> algorithm(String headerJson);
}
