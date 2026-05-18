package server.MATE.toss.dto.request;

import jakarta.validation.constraints.NotNull;

public record TossUnlinkByUserKeyRequest(
        @NotNull Long userKey
) {
}
