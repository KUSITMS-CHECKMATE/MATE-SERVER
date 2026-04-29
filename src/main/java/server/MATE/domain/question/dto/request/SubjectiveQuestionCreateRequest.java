package server.MATE.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectiveQuestionCreateRequest(
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Size(max = 55, message = "질문 설명은 최대 55자까지 입력 가능합니다.")
        String description,

        String imageKey
) {
}
