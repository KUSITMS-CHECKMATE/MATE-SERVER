package server.MATE.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObjectiveOptionRequest(
        @NotBlank
        @Size(max = 17, message = "선택지명은 최대 17자까지 입력 가능합니다.")
        String content,

        String imageKey
) {
}
