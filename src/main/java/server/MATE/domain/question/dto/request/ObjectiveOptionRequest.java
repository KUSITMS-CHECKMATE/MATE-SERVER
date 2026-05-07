package server.MATE.domain.question.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjectiveOptionRequest(
        @Schema(example = "검색")
        @NotBlank
        @Size(max = 17, message = "선택지명은 최대 17자까지 입력 가능합니다.")
        String content,

        @Schema(example = "objective-option-image-key", nullable = true)
        String imageKey
) {
}
