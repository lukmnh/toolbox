package io.github.lukmnh.toolbox.devutils.hash.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.hash.domain.UuidV7Generator;
import io.github.lukmnh.toolbox.devutils.hash.domain.UuidVersion;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerateUuidsUseCaseTest {

    private final GenerateUuidsUseCase useCase =
            new GenerateUuidsUseCase(new UuidV7Generator(System::currentTimeMillis, new SecureRandom()));

    @Test
    void generatesRequestedCountAndVersion() {
        for (UuidVersion version : UuidVersion.values()) {
            List<UUID> uuids = useCase.execute(version, GenerateUuidsUseCase.MAX_COUNT);

            assertEquals(GenerateUuidsUseCase.MAX_COUNT, uuids.size());
            assertEquals(GenerateUuidsUseCase.MAX_COUNT, new HashSet<>(uuids).size());
            int expectedVersion = version == UuidVersion.V4 ? 4 : 7;
            uuids.forEach(uuid -> {
                assertEquals(expectedVersion, uuid.version());
                assertEquals(2, uuid.variant());
            });
        }
    }

    @Test
    void rejectsCountOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(UuidVersion.V4, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(UuidVersion.V7, GenerateUuidsUseCase.MAX_COUNT + 1));
    }
}
