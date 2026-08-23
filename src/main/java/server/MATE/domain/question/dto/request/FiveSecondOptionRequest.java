package server.MATE.domain.question.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FiveSecondOptionRequest(
        @Schema(example = "검색창")
        @NotBlank
        String content
) {
}
