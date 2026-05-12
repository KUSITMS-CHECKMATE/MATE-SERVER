package server.MATE.domain.answer.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ObjectiveAnswerCreateRequest(
        @NotNull Long questionId,
        @NotNull List<Long> selectedOptionIds,
        String otherText
) {
}
