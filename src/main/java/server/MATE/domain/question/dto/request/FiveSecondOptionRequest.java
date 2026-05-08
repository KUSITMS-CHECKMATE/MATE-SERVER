package server.MATE.domain.question.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FiveSecondOptionRequest(
        @Schema(example = "검색창")
        @NotBlank
        @Size(max = 50, message = "선택지 내용은 최대 50자까지 입력 가능합니다.")
        String content
) {
}
