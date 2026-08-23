package server.MATE.domain.question.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ObjectiveOptionRequest(
        @Schema(example = "검색")
        @NotBlank
        String content,

        @Schema(example = "objective-option-image-key", nullable = true)
        String imageKey
) {
}
