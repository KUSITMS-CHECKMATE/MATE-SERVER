package server.MATE.toss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TossUnlinkCallbackRequest(
        @NotNull Long userKey,
        @NotBlank String referrer
) {
}
