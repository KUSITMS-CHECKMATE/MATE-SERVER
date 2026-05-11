package server.MATE.domain.answer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubjectiveAnswerCreateRequest(
        @NotNull Long questionId,
        @NotBlank String text
) {
}
