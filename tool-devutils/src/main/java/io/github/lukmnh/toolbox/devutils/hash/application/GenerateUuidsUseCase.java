package io.github.lukmnh.toolbox.devutils.hash.application;

import io.github.lukmnh.toolbox.devutils.hash.domain.UuidV7Generator;
import io.github.lukmnh.toolbox.devutils.hash.domain.UuidVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Generate sejumlah UUID v4 ({@link UUID#randomUUID()}, SecureRandom) atau v7 sekaligus. */
public final class GenerateUuidsUseCase {

    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 100;

    private final UuidV7Generator v7;

    public GenerateUuidsUseCase(UuidV7Generator v7) {
        this.v7 = v7;
    }

    /** @throws IllegalArgumentException bila {@code count} di luar {@value #MIN_COUNT}–{@value #MAX_COUNT} */
    public List<UUID> execute(UuidVersion version, int count) {
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new IllegalArgumentException("Jumlah UUID harus " + MIN_COUNT + "–" + MAX_COUNT + ": " + count);
        }
        List<UUID> uuids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            uuids.add(
                    switch (version) {
                        case V4 -> UUID.randomUUID();
                        case V7 -> v7.next();
                    });
        }
        return List.copyOf(uuids);
    }
}
